package com.example.whisperrecorder.npu

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** CPU preprocessing port of pinned exporter src/mel.py at 4aee2ae0.
 * 16 kHz mono, reflect-centered torch.stft 400/160, periodic Hann, Slaney 128 mel,
 * float32 power -> log10 clamp -> -8 floor -> (x+4)/4, final frame dropped.
 * The package's synthetic golden vectors MUST pass before invoking the NPU.
 */
internal object Qwen3Mel {
    const val SAMPLES = 192000
    const val FRAMES = 1200
    const val BINS = 128
    private const val FFT = 400
    private const val FREQUENCIES = 201
    private val window: FloatArray = FloatArray(FFT) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / FFT)).toFloat()
    }
    private val cosine: Array<FloatArray> = Array(FREQUENCIES) { k ->
        FloatArray(FFT) { n -> cos(2.0 * PI * k * n / FFT).toFloat() }
    }
    private val sine: Array<FloatArray> = Array(FREQUENCIES) { k ->
        FloatArray(FFT) { n -> sin(2.0 * PI * k * n / FFT).toFloat() }
    }
    private fun hzToMel(hz: Double): Double = if (hz < 1000.0) hz / (200.0 / 3.0)
        else 15.0 + ln(hz / 1000.0) / (ln(6.4) / 27.0)
    private fun melToHz(mel: Double): Double = if (mel < 15.0) mel * (200.0 / 3.0)
        else 1000.0 * exp((mel - 15.0) * ln(6.4) / 27.0)
    private val filters: Array<FloatArray> = run {
        val maxMel = hzToMel(8000.0)
        val edges = DoubleArray(BINS + 2) { melToHz(it * maxMel / (BINS + 1)) }
        Array(BINS) { band ->
            val low = edges[band]
            val mid = edges[band + 1]
            val high = edges[band + 2]
            val normalization = 2.0 / (high - low)
            FloatArray(FREQUENCIES) { k ->
                val frequency = k * 16000.0 / FFT
                (max(0.0, min((frequency - low) / (mid - low),
                    (high - frequency) / (high - mid))) * normalization).toFloat()
            }
        }
    }

    fun compute(pcm: FloatArray, cancelled: () -> Boolean = { false }): FloatArray {
        require(pcm.size in 1..SAMPLES) { "Qwen mel accepts at most 12 seconds of 16k mono PCM" }
        require(pcm.all { it.isFinite() }) { "Nonfinite Qwen audio" }
        val audio = FloatArray(SAMPLES)
        pcm.copyInto(audio)
        // Channel-major contiguous [1, 128, 1200]. Last centered frame excluded.
        val mel = FloatArray(BINS * FRAMES)
        val frame = FloatArray(FFT)
        val powers = FloatArray(FREQUENCIES)
        for (t in 0 until FRAMES) {
            if (t % 16 == 0 && cancelled()) throw java.util.concurrent.CancellationException("Qwen mel cancelled")
            val center = t * 160
            for (n in 0 until FFT) {
                var index = center + n - FFT / 2
                if (index < 0) index = -index
                if (index >= SAMPLES) index = 2 * SAMPLES - 2 - index
                frame[n] = audio[index] * window[n]
            }
            for (k in 0 until FREQUENCIES) {
                val cosRow = cosine[k]
                val sinRow = sine[k]
                var real = 0f
                var imaginary = 0f
                for (n in 0 until FFT) {
                    val value = frame[n]
                    real += value * cosRow[n]
                    imaginary -= value * sinRow[n]
                }
                powers[k] = real * real + imaginary * imaginary
            }
            for (band in 0 until BINS) {
                val weights = filters[band]
                var energy = 0f
                for (k in 0 until FREQUENCIES) energy += weights[k] * powers[k]
                mel[band * FRAMES + t] = log10(max(energy, 1.0e-10f))
            }
        }
        val floor = mel.maxOrNull()!! - 8f
        for (i in mel.indices) mel[i] = (max(mel[i], floor) + 4f) / 4f
        return mel
    }

    data class Parity(val maxAbs: Double, val rmse: Double, val pass: Boolean)

    /** Compare against public deterministic fixture; never logs source PCM or transcripts. */
    fun verifyGolden(folder: File, cancelled: () -> Boolean = { false }): Parity {
        val pcm = readFloat(File(folder, "golden-synthetic-pcm-12s-f32.raw"), SAMPLES)
        val reference = readFloat(File(folder, "golden-synthetic-mel-12s-f32.raw"), BINS * FRAMES)
        val actual = compute(pcm, cancelled)
        var peak = 0.0
        var sumSq = 0.0
        for (i in reference.indices) {
            val difference = abs(actual[i].toDouble() - reference[i].toDouble())
            peak = max(peak, difference)
            sumSq += difference * difference
        }
        val rmse = sqrt(sumSq / reference.size)
        return Parity(peak, rmse, peak <= 3e-4 && rmse <= 3e-5)
    }

    private fun readFloat(file: File, count: Int): FloatArray {
        require(file.isFile && file.length() == count * 4L) { "Missing Qwen golden fixture: ${file.name}" }
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val result = FloatArray(count)
        buffer.get(result)
        return result
    }
}
