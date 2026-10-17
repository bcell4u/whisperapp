package com.example.whisperrecorder.audio

import android.content.Context
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import com.example.whisperrecorder.data.AppPrefs

data class MicrophoneProcessingConfig(
    val preset: String,
    val source: String,
    val noiseSuppressor: String,
    val aec: String
)

data class MicrophoneProcessingCapabilities(
    val noiseSuppressor: Boolean,
    val aec: Boolean
)

object MicrophoneProcessing {
    const val PRESET_DEFAULT = "default"
    const val PRESET_LECTURE = "lecture"
    const val PRESET_MEETING = "meeting"
    const val PRESET_CLOSE = "close"
    const val PRESET_SPEAKER = "speaker"
    const val PRESET_CUSTOM = "custom"

    const val SOURCE_MIC = "mic"
    const val SOURCE_VOICE_RECOGNITION = "voice_recognition"
    const val SOURCE_CAMCORDER = "camcorder"

    const val MODE_AUTO = "auto"
    const val MODE_ON = "on"
    const val MODE_OFF = "off"

    val presetIds = setOf(PRESET_DEFAULT, PRESET_LECTURE, PRESET_MEETING, PRESET_CLOSE, PRESET_SPEAKER, PRESET_CUSTOM)
    val sourceIds = setOf(SOURCE_MIC, SOURCE_VOICE_RECOGNITION, SOURCE_CAMCORDER)
    val effectModes = setOf(MODE_AUTO, MODE_ON, MODE_OFF)

    fun presetConfig(id: String): MicrophoneProcessingConfig = when (id) {
        PRESET_LECTURE -> MicrophoneProcessingConfig(id, SOURCE_VOICE_RECOGNITION, MODE_ON, MODE_OFF)
        PRESET_MEETING -> MicrophoneProcessingConfig(id, SOURCE_VOICE_RECOGNITION, MODE_ON, MODE_OFF)
        PRESET_CLOSE -> MicrophoneProcessingConfig(id, SOURCE_MIC, MODE_AUTO, MODE_OFF)
        PRESET_SPEAKER -> MicrophoneProcessingConfig(id, SOURCE_VOICE_RECOGNITION, MODE_ON, MODE_ON)
        else -> MicrophoneProcessingConfig(PRESET_DEFAULT, SOURCE_MIC, MODE_AUTO, MODE_OFF)
    }

    fun resolve(context: Context): MicrophoneProcessingConfig {
        val preset = AppPrefs.getMicrophonePreset(context)
        if (preset != PRESET_CUSTOM) return presetConfig(preset)
        return MicrophoneProcessingConfig(
            PRESET_CUSTOM,
            AppPrefs.getCustomMicrophoneSource(context),
            AppPrefs.getCustomNoiseSuppressorMode(context),
            AppPrefs.getCustomAecMode(context)
        )
    }

    fun sourceValue(source: String): Int = when (source) {
        SOURCE_VOICE_RECOGNITION -> MediaRecorder.AudioSource.VOICE_RECOGNITION
        SOURCE_CAMCORDER -> MediaRecorder.AudioSource.CAMCORDER
        else -> MediaRecorder.AudioSource.MIC
    }

    fun sourceLabel(source: String): String = when (source) {
        SOURCE_VOICE_RECOGNITION -> "Voice recognition"
        SOURCE_CAMCORDER -> "Camcorder"
        else -> "Microphone"
    }

    fun presetLabel(preset: String): String = when (preset) {
        PRESET_LECTURE -> "Lecture / Distance"
        PRESET_MEETING -> "Meeting / Room"
        PRESET_CLOSE -> "Close Speech"
        PRESET_SPEAKER -> "Phone Speaker / Playback"
        PRESET_CUSTOM -> "Custom"
        else -> "Default"
    }

    fun capabilities(): MicrophoneProcessingCapabilities = MicrophoneProcessingCapabilities(
        noiseSuppressor = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false),
        aec = runCatching { AcousticEchoCanceler.isAvailable() }.getOrDefault(false)
    )
}
