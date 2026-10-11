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

/**
 * A one-time, user-triggered preflight before saving a custom audio option.
 * A fresh recording can still fail if permissions, routing, or device availability change.
 */
internal object MicrophoneCapabilityProbe {
    data class Result(val supported: Boolean, val message: String)

    fun check(
        context: Context,
        source: String,
        format: String,
        noiseSuppressor: String,
        aec: String
    ): Result {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return Result(false, "Grant microphone permission before testing this option.")
        }
        if (RecordingRuntime.state.value.active) {
            return Result(false, "Stop the current recording before testing audio settings.")
        }
        if (format != "wav" && (noiseSuppressor == MicrophoneProcessing.MODE_ON || aec == MicrophoneProcessing.MODE_ON)) {
            return Result(false, "Explicit noise suppression and echo cancellation can only be verified and enabled for WAV. M4A/Opus effects are controlled by Android; choose Auto or switch to WAV.")
        }
        return runCatching {
            if (format == "wav") checkPcm(source, noiseSuppressor, aec)
            else checkCompressed(context, source, format)
            Result(true, "${MicrophoneProcessing.sourceLabel(source)} passed the $format input check. Device routing can change when recording starts.")
        }.getOrElse { error ->
            Result(false, "${MicrophoneProcessing.sourceLabel(source)} could not be enabled: ${error.message ?: error.javaClass.simpleName}. Your previous selection was kept.")
        }
    }

    private fun checkPcm(source: String, noiseSuppressor: String, aec: String) {
        val minBuffer = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minBuffer > 0) { "16 kHz WAV capture is unavailable" }
        val recorder = AudioRecord(
            MicrophoneProcessing.sourceValue(source), 16_000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, 16_000 * 2)
        )
        val effects = mutableListOf<AudioEffect>()
        try {
            require(recorder.state == AudioRecord.STATE_INITIALIZED) { "The audio source cannot initialize" }
            if (noiseSuppressor == MicrophoneProcessing.MODE_ON) {
                require(NoiseSuppressor.isAvailable()) { "Noise suppression is not supported" }
                val effect = requireNotNull(NoiseSuppressor.create(recorder.audioSessionId)) { "Noise suppression cannot be created for this session" }
                effects.add(effect)
                effect.enabled = true
                require(effect.enabled) { "Noise suppression did not enable" }
            }
            if (aec == MicrophoneProcessing.MODE_ON) {
                require(AcousticEchoCanceler.isAvailable()) { "Echo cancellation is not supported" }
                val effect = requireNotNull(AcousticEchoCanceler.create(recorder.audioSessionId)) { "Echo cancellation cannot be created for this session" }
                effects.add(effect)
                effect.enabled = true
                require(effect.enabled) { "Echo cancellation did not enable" }
            }
            recorder.startRecording()
            require(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "The microphone did not start recording" }
        } finally {
            runCatching { recorder.stop() }
            effects.forEach { runCatching { it.release() } }
            recorder.release()
        }
    }

    private fun checkCompressed(context: Context, source: String, format: String) {
        val file = File.createTempFile("mic-source-probe-", if (format == "opus") ".ogg" else ".m4a", context.cacheDir)
        val recorder = try {
            if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context)
            else @Suppress("DEPRECATION") MediaRecorder()
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        try {
            recorder.setAudioSource(MicrophoneProcessing.sourceValue(source))
            if (format == "opus") {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                recorder.setAudioEncodingBitRate(64_000)
                recorder.setAudioSamplingRate(48_000)
            } else {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(96_000)
                recorder.setAudioSamplingRate(44_100)
            }
            recorder.setAudioChannels(1)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            // The actual recorder uses the same source/encoder configuration.
            // A short-lived probe checks startup, not sustained capture quality.
        } finally {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            file.delete()
        }
    }
}
