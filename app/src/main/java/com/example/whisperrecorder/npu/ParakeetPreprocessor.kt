package com.example.whisperrecorder.npu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.sin

/** CPU front-end matching Transformers 5.9 ParakeetFeatureExtractor for the pinned model contract. */
internal class ParakeetPreprocessor(private val config: Config) {
    data class Config(
        val sampleRate: Int,
        val featureSize: Int,
        val hopLength: Int,
        val nFft: Int,
        val winLength: Int,
        val preemphasis: Float,
        val maxSamples: Int = 40 * 16_000,
        val physicalFrames: Int = 4001
    ) {
        fun requireValidatedContract() {
            require(sampleRate == 16_000) { "Unsupported Parakeet sample rate: $sampleRate" }
            require(featureSize == 128) { "Unsupported Parakeet feature size: $featureSize" }
            require(hopLength == 160) { "Unsupported Parakeet hop length: $hopLength" }
            require(nFft == 512) { "Unsupported Parakeet FFT size: $nFft" }
            require(winLength == 400) { "Unsupported Parakeet window length: $winLength" }
            require(kotlin.math.abs(preemphasis - 0.97f) <= 1e-6f) { "Unsupported Parakeet preemphasis: $preemphasis" }
            require((maxSamples == 640_000 && physicalFrames == 4001) ||
                (maxSamples == 192_000 && physicalFrames == 1201)) {
                "Unsupported fixed feature contract: samples=$maxSamples frames=$physicalFrames"
            }
        }
    }

    data class Result(
        val audioSignal: FloatArray,
        val subsampleMask1: FloatArray,
        val subsampleMask2: FloatArray,
        val subsampleMask3: FloatArray,
        val validFeatureFrames: Int,
        val validEncodedFrames: Int
    )

    private val fft = Radix2Fft(config.nFft)
    private val window = FloatArray(config.winLength) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / (config.winLength - 1))).toFloat()
    }
    private val melFilters = buildSlaneyMelFilters(config.sampleRate, config.nFft, config.featureSize)
    private val windowOffset = (config.nFft - config.winLength) / 2

    init { config.requireValidatedContract() }

    fun extract(inputPcm: FloatArray): Result {
        require(inputPcm.isNotEmpty()) { "PCM input is empty" }
        val sampleCount = min(inputPcm.size, config.maxSamples)
        val validFrames = min(config.physicalFrames - 1, sampleCount / config.hopLength)
        require(validFrames > 1) { "Parakeet preprocessing requires at least two valid feature frames" }

        val output = FloatArray(config.featureSize * config.physicalFrames)
        val sums = DoubleArray(config.featureSize)
        val sumsSq = DoubleArray(config.featureSize)
        val re = DoubleArray(config.nFft)
        val im = DoubleArray(config.nFft)
        val power = DoubleArray(config.nFft / 2 + 1)

        for (frame in 0 until validFrames) {
            java.util.Arrays.fill(re, 0.0)
            java.util.Arrays.fill(im, 0.0)
            val center = frame * config.hopLength
            for (w in 0 until config.winLength) {
                val sampleIndex = center + w - config.winLength / 2
                val x = preemphasized(inputPcm, sampleIndex, sampleCount)
                re[windowOffset + w] = x.toDouble() * window[w].toDouble()
            }
            fft.forward(re, im)
            for (k in power.indices) power[k] = re[k] * re[k] + im[k] * im[k]

            for (mel in 0 until config.featureSize) {
                val filterOffset = mel * power.size
                var energy = 0.0
                for (k in power.indices) energy += melFilters[filterOffset + k].toDouble() * power[k]
                val value = ln(energy + LOG_ZERO_GUARD).toFloat()
                output[mel * config.physicalFrames + frame] = value
                sums[mel] += value.toDouble()
                sumsSq[mel] += value.toDouble() * value.toDouble()
            }
        }

        for (mel in 0 until config.featureSize) {
            val mean = sums[mel] / validFrames
            val centeredSq = max(0.0, sumsSq[mel] - validFrames * mean * mean)
            val variance = centeredSq / (validFrames - 1)
            val std = sqrt(variance)
            val denom = std + EPSILON
            val base = mel * config.physicalFrames
            for (frame in 0 until validFrames) {
                val normalized = ((output[base + frame] - mean) / denom).toFloat()
                require(normalized.isFinite()) { "Non-finite normalized feature mel=$mel frame=$frame" }
                output[base + frame] = normalized
            }
            // Padded physical frames remain exact zero, matching attention-mask multiplication.
        }

        val stage1 = subsampleMask(validFrames, config.physicalFrames, 3, 2, 1)
        val stage2 = subsampleMask(stage1.valid, stage1.physical, 3, 2, 1)
        val stage3 = subsampleMask(stage2.valid, stage2.physical, 3, 2, 1)
        require(stage1.physical == (config.physicalFrames + 1) / 2 &&
            stage2.physical == (stage1.physical + 1) / 2 &&
            stage3.physical == (stage2.physical + 1) / 2) {
            "Unexpected host-mask physical shapes: ${stage1.physical}/${stage2.physical}/${stage3.physical}"
        }

        return Result(
            audioSignal = output,
            subsampleMask1 = stage1.mask,
            subsampleMask2 = stage2.mask,
            subsampleMask3 = stage3.mask,
            validFeatureFrames = validFrames,
            validEncodedFrames = stage3.valid
        )
    }

    private fun preemphasized(pcm: FloatArray, index: Int, sampleCount: Int): Float {
        if (index < 0 || index >= sampleCount) return 0f
        val current = pcm[index]
        if (index == 0) return current
        return current - config.preemphasis * pcm[index - 1]
    }

    private data class MaskResult(val mask: FloatArray, val valid: Int, val physical: Int)

    private fun subsampleMask(validIn: Int, physicalIn: Int, kernel: Int, stride: Int, padding: Int): MaskResult {
        val physical = (physicalIn + 2 * padding - kernel) / stride + 1
        val valid = ((validIn + 2 * padding - kernel) / stride + 1).coerceIn(0, physical)
        return MaskResult(FloatArray(physical) { if (it < valid) 1f else 0f }, valid, physical)
    }

    private class Radix2Fft(private val n: Int) {
        private val bitReverse: IntArray
        private val cosTable = DoubleArray(n / 2)
        private val sinTable = DoubleArray(n / 2)

        init {
            require(n > 1 && n and (n - 1) == 0) { "FFT size must be a power of two" }
            val bits = Integer.numberOfTrailingZeros(n)
            bitReverse = IntArray(n) { Integer.reverse(it) ushr (32 - bits) }
            for (k in 0 until n / 2) {
                val angle = -2.0 * PI * k / n
                cosTable[k] = cos(angle)
                sinTable[k] = sin(angle)
            }
        }

        fun forward(real: DoubleArray, imag: DoubleArray) {
            for (i in 0 until n) {
                val j = bitReverse[i]
                if (j > i) {
                    val tr = real[i]; real[i] = real[j]; real[j] = tr
                    val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
                }
            }
            var size = 2
            while (size <= n) {
                val half = size / 2
                val tableStep = n / size
                var start = 0
                while (start < n) {
                    for (j in 0 until half) {
                        val k = j * tableStep
                        val even = start + j
                        val odd = even + half
                        val or = real[odd]
                        val oi = imag[odd]
                        val wr = cosTable[k]
                        val wi = sinTable[k]
                        val tr = wr * or - wi * oi
                        val ti = wr * oi + wi * or
                        real[odd] = real[even] - tr
                        imag[odd] = imag[even] - ti
                        real[even] += tr
                        imag[even] += ti
                    }
                    start += size
                }
                size = size shl 1
            }
        }
    }

    companion object {
        private const val EPSILON = 1e-5
        private const val LOG_ZERO_GUARD = 5.960464477539063e-8 // 2^-24
        private const val F_SP = 200.0 / 3.0
        private const val MIN_LOG_HZ = 1000.0
        private const val MIN_LOG_MEL = MIN_LOG_HZ / F_SP
        private val LOGSTEP = ln(6.4) / 27.0

        private fun hzToMel(hz: Double): Double = if (hz < MIN_LOG_HZ) {
            hz / F_SP
        } else {
            MIN_LOG_MEL + ln(hz / MIN_LOG_HZ) / LOGSTEP
        }

        private fun melToHz(mel: Double): Double = if (mel < MIN_LOG_MEL) {
            F_SP * mel
        } else {
            MIN_LOG_HZ * exp(LOGSTEP * (mel - MIN_LOG_MEL))
        }

        private fun buildSlaneyMelFilters(sampleRate: Int, nFft: Int, nMels: Int): FloatArray {
            val bins = nFft / 2 + 1
            val minMel = hzToMel(0.0)
            val maxMel = hzToMel(sampleRate / 2.0)
            val edges = DoubleArray(nMels + 2) { i ->
                melToHz(minMel + (maxMel - minMel) * i / (nMels + 1))
            }
            val result = FloatArray(nMels * bins)
            for (m in 0 until nMels) {
                val left = edges[m]
                val center = edges[m + 1]
                val right = edges[m + 2]
                val norm = 2.0 / (right - left)
                for (k in 0 until bins) {
                    val hz = k.toDouble() * sampleRate / nFft
                    val lower = (hz - left) / (center - left)
                    val upper = (right - hz) / (right - center)
                    result[m * bins + k] = (max(0.0, min(lower, upper)) * norm).toFloat()
                }
            }
            return result
        }
    }
}
