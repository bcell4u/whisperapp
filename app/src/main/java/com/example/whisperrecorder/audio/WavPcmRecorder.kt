package com.example.whisperrecorder.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Minimal 16 kHz mono PCM WAV recorder used only when the user explicitly selects WAV.
 * AudioRecord gives this path an audio-session ID, so supported Android audio effects can
 * be explicitly enabled/disabled for WAV capture. Compressed formats use MediaRecorder and
 * rely on the selected audio source/OEM processing path instead.
 */
class WavPcmRecorder(
    private val context: Context,
    private val output: File,
    private val processing: MicrophoneProcessingConfig = MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_DEFAULT)
) {
    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }

    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val amplitude = AtomicInteger(0)
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    private val effects = mutableListOf<AudioEffect>()
    @Volatile private var failure: Throwable? = null
    @Volatile var actualSource: String = processing.source
        private set
    @Volatile var effectSummary: String = "NS=system AEC=system"
        private set

    val maxAmplitude: Int get() = amplitude.get()
    val error: Throwable? get() = failure

    fun start() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Microphone permission is required for WAV recording")
        }
        check(!running.get()) { "WAV recorder is already running" }
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBuffer > 0) { "16 kHz PCM recording is not supported on this device" }
        val bufferSize = maxOf(minBuffer * 2, SAMPLE_RATE * 2)

        val requestedSource = MicrophoneProcessing.sourceValue(processing.source)
        var recorder = createRecorder(requestedSource, bufferSize)
        if (recorder == null && requestedSource != MediaRecorder.AudioSource.MIC) {
            actualSource = MicrophoneProcessing.SOURCE_MIC
            recorder = createRecorder(MediaRecorder.AudioSource.MIC, bufferSize)
        }
        requireNotNull(recorder) { "Unable to initialize PCM recorder" }
        audioRecord = recorder

        try {
            configureEffects(recorder.audioSessionId)
            writeEmptyHeader(output)
            running.set(true)
            recorder.startRecording()
            worker = Thread({ captureLoop(recorder, bufferSize) }, "wav-pcm-recorder").apply { start() }
        } catch (t: Throwable) {
            running.set(false)
            releaseEffects()
            runCatching { recorder.release() }
            audioRecord = null
            output.delete()
            throw t
        }
    }

    fun pause() { paused.set(true) }
    fun resume() { paused.set(false) }

    fun stop() {
        running.set(false)
        val writer = worker
        runCatching { audioRecord?.stop() }
        try {
            writer?.join(2_000)
            if (writer?.isAlive == true) {
                // Some audio drivers need release(), not stop(), to unblock a pending read.
                release()
                writer.join(2_000)
            }
            check(writer?.isAlive != true) { "WAV writer did not finish; refusing to finalize a file still being written" }
            failure?.let { throw IllegalStateException("WAV recording failed", it) }
            patchHeader(output)
        } finally {
            release()
        }
    }

    fun release() {
        running.set(false)
        releaseEffects()
        runCatching { audioRecord?.release() }
        audioRecord = null
        worker = null
    }

    private fun createRecorder(source: Int, bufferSize: Int): AudioRecord? = runCatching {
        // Recheck at the AudioRecord constructor boundary, independently of start().
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Microphone permission is required for WAV recording")
        }
        AudioRecord(
            source,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).also {
            if (it.state != AudioRecord.STATE_INITIALIZED) {
                it.release()
                error("AudioRecord was not initialized")
            }
        }
    }.getOrNull()

    private fun configureEffects(sessionId: Int) {
        val ns = configureEffect(processing.noiseSuppressor, NoiseSuppressor.isAvailable()) { NoiseSuppressor.create(sessionId) }
        val aec = configureEffect(processing.aec, AcousticEchoCanceler.isAvailable()) { AcousticEchoCanceler.create(sessionId) }
        effectSummary = "NS=$ns AEC=$aec"
        if (processing.preset == MicrophoneProcessing.PRESET_CUSTOM) {
            require(processing.noiseSuppressor != MicrophoneProcessing.MODE_ON || ns == "on") {
                "Noise suppression could not be enabled on this recording session"
            }
            require(processing.aec != MicrophoneProcessing.MODE_ON || aec == "on") {
                "Echo cancellation could not be enabled on this recording session"
            }
        }
    }

    private fun configureEffect(mode: String, available: Boolean, factory: () -> AudioEffect?): String {
        if (mode == MicrophoneProcessing.MODE_AUTO) return "auto"
        if (!available) return "unsupported"
        val effect = runCatching { factory() }.getOrNull() ?: return "unavailable"
        effects += effect
        val requestedEnabled = mode == MicrophoneProcessing.MODE_ON
        return runCatching {
            effect.enabled = requestedEnabled
            if (effect.enabled) "on" else "off"
        }.getOrElse { "error" }
    }

    private fun releaseEffects() {
        effects.forEach { runCatching { it.release() } }
        effects.clear()
    }

    private fun captureLoop(recorder: AudioRecord, bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        val bytes = ByteArray(buffer.size * 2)
        try {
            RandomAccessFile(output, "rw").use { raf ->
                raf.seek(44)
                while (running.get()) {
                    val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) throw IllegalStateException("AudioRecord read failed: $count")
                    if (count == 0) continue
                    var peak = 0
                    for (i in 0 until count) peak = maxOf(peak, abs(buffer[i].toInt()))
                    amplitude.set(peak)
                    if (!paused.get()) {
                        var j = 0
                        for (i in 0 until count) {
                            val value = buffer[i].toInt()
                            bytes[j++] = (value and 0xff).toByte()
                            bytes[j++] = ((value ushr 8) and 0xff).toByte()
                        }
                        raf.write(bytes, 0, count * 2)
                    }
                }
            }
        } catch (t: Throwable) {
            if (running.get()) failure = t
        }
    }

    private fun writeEmptyHeader(file: File) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(ByteArray(44))
        }
    }

    private fun patchHeader(file: File) {
        val dataSize = (file.length() - 44L).coerceAtLeast(0L)
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            writeLeInt(raf, (36L + dataSize).coerceAtMost(0xffffffffL).toInt())
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            writeLeInt(raf, 16)
            writeLeShort(raf, 1)
            writeLeShort(raf, CHANNELS)
            writeLeInt(raf, SAMPLE_RATE)
            writeLeInt(raf, byteRate)
            writeLeShort(raf, CHANNELS * BITS_PER_SAMPLE / 8)
            writeLeShort(raf, BITS_PER_SAMPLE)
            raf.writeBytes("data")
            writeLeInt(raf, dataSize.coerceAtMost(0xffffffffL).toInt())
        }
    }

    private fun writeLeInt(raf: RandomAccessFile, value: Int) {
        raf.write(value and 0xff)
        raf.write((value ushr 8) and 0xff)
        raf.write((value ushr 16) and 0xff)
        raf.write((value ushr 24) and 0xff)
    }

    private fun writeLeShort(raf: RandomAccessFile, value: Int) {
        raf.write(value and 0xff)
        raf.write((value ushr 8) and 0xff)
    }
}
