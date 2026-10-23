package com.example.whisperrecorder.data

import android.content.Context
import android.net.Uri
import android.content.SharedPreferences
import com.example.whisperrecorder.transcribe.AsrEngine
import com.example.whisperrecorder.transcribe.AccelerationPolicy
import com.example.whisperrecorder.transcribe.AccelerationTier
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.speaker.SpeakerClusteringPolicy

object AppPrefs {
    private const val PREFS = "whisper_recorder"
    private const val KEY_TREE = "save_tree"
    private const val KEY_FORMAT = "audio_format"
    private const val KEY_MODEL = "model_path"
    private const val KEY_TRANSCRIPTION_ENGINE = "transcription_engine"
    private const val KEY_AUTOMATIC_ENGINE = "transcription_engine_auto"
    private const val KEY_GPU_NOTICE_SHOWN = "acceleration_notice_gpu_shown"
    private const val KEY_CPU_NOTICE_SHOWN = "acceleration_notice_cpu_shown"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_PENDING_RENAME = "pending_rename_recording_id"
    private const val KEY_VERBOSE_DIAGNOSTICS = "verbose_diagnostics"
    private const val KEY_CONFIRM_EXIT = "confirm_exit"
    private const val KEY_PERFORMANCE_PRESET = "performance_preset"
    private const val KEY_HIGHLIGHT_OFFSET_PREFIX = "transcript_highlight_offset_ms:"
    private const val KEY_HIGHLIGHT_LEAD_PREFIX = "transcript_highlight_lead_ms:"
    private const val KEY_CUSTOM_THREADS = "custom_threads"
    private const val KEY_CHUNK_MINUTES = "chunk_minutes"
    private const val KEY_TRANSCRIPTION_LANGUAGE = "transcription_language"
    private const val KEY_MIC_PRESET = "microphone_preset"
    private const val KEY_MIC_SOURCE = "microphone_source"
    private const val KEY_NS_MODE = "noise_suppressor_mode"
    private const val KEY_AEC_MODE = "aec_mode"
    private const val KEY_SPEAKER_TIMESTAMPS = "speaker_timestamps"
    private const val KEY_SPEAKER_COUNT = "speaker_expected_count"

    private val audioFormats = setOf("m4a", "opus", "wav")
    private val microphonePresets = setOf("default", "lecture", "meeting", "close", "speaker", "custom")
    private val microphoneSources = setOf("mic", "voice_recognition", "camcorder")
    private val effectModes = setOf("auto", "on", "off")
    private val performancePresets = setOf("balanced", "high", "max", "custom")

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSaveTree(context: Context): Uri? =
        prefs(context)
            .getString(KEY_TREE, null)?.let(Uri::parse)

    fun setSaveTree(context: Context, uri: Uri) {
        prefs(context).edit()
            .putString(KEY_TREE, uri.toString()).apply()
    }

    fun getAudioFormat(context: Context): String {
        val value = prefs(context)
            .getString(KEY_FORMAT, "opus") ?: "opus"
        return if (value in audioFormats) value else "opus"
    }

    fun setAudioFormat(context: Context, value: String) {
        val normalized = if (value in audioFormats) value else "opus"
        prefs(context).edit()
            .putString(KEY_FORMAT, normalized).apply()
    }

    fun speakerTimestamps(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SPEAKER_TIMESTAMPS, false)

    fun setSpeakerTimestamps(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SPEAKER_TIMESTAMPS, enabled).apply()
    }

    fun speakerExpectedCount(context: Context): Int =
        SpeakerClusteringPolicy.normalize(prefs(context).getInt(KEY_SPEAKER_COUNT, SpeakerClusteringPolicy.AUTO))

    fun setSpeakerExpectedCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_SPEAKER_COUNT, SpeakerClusteringPolicy.normalize(count)).apply()
    }

    fun getModelPath(context: Context): String? =
        prefs(context).getString(KEY_MODEL, null)

    fun setModelPath(context: Context, value: String?) {
        prefs(context).edit()
            .putString(KEY_MODEL, value).apply()
    }

    fun getTranscriptionEngine(context: Context): String {
        val saved = prefs(context).getString(KEY_TRANSCRIPTION_ENGINE, AsrEngine.WHISPER.id)
        // Upgrade compatibility: the retired Parakeet 1.1B engine used this persisted ID.
        // Map only the preference; model availability is still resolved independently.
        if (saved == "parakeet11") return AsrEngine.PARAKEET.id
        if (saved == "qwen3") {
            // v6.7 removes the 1.7B transcribe backend. Never hand its stale
            // GGUF path to Whisper; let inventory choose an installed backend.
            prefs(context).edit()
                .remove(KEY_TRANSCRIPTION_ENGINE)
                .remove(KEY_MODEL)
                .putBoolean(KEY_AUTOMATIC_ENGINE, true)
                .apply()
            return AsrEngine.WHISPER.id
        }
        return AsrEngine.fromId(saved).id
    }

    /** An automatic choice may be upgraded when a better backend becomes available. */
    fun hasExplicitTranscriptionEngine(context: Context): Boolean {
        val store = prefs(context)
        return store.contains(KEY_TRANSCRIPTION_ENGINE) && !store.getBoolean(KEY_AUTOMATIC_ENGINE, false)
    }

    fun setAutomaticTranscriptionSelection(context: Context, engineId: String, modelPath: String?) {
        prefs(context).edit()
            .putString(KEY_TRANSCRIPTION_ENGINE, AsrEngine.fromId(engineId).id)
            .putString(KEY_MODEL, modelPath)
            .putBoolean(KEY_AUTOMATIC_ENGINE, true)
            .apply()
    }

    /** Let the next inventory refresh reselect an installed backend by priority. */
    fun resetAutomaticTranscriptionSelection(context: Context) {
        prefs(context).edit()
            .remove(KEY_TRANSCRIPTION_ENGINE)
            .remove(KEY_MODEL)
            .putBoolean(KEY_AUTOMATIC_ENGINE, true)
            .apply()
    }

    /** Claim a one-time in-app notice after a usable model has been selected. */
    @Synchronized
    fun claimAccelerationNotice(context: Context, engineId: String): AccelerationTier? {
        val tier = AccelerationPolicy.tier(AsrEngine.fromId(engineId))
        val key = when (tier) {
            AccelerationTier.NPU -> return null
            AccelerationTier.GPU -> KEY_GPU_NOTICE_SHOWN
            AccelerationTier.CPU -> KEY_CPU_NOTICE_SHOWN
        }
        val store = prefs(context)
        if (store.getBoolean(key, false)) return null
        if (!store.edit().putBoolean(key, true).commit()) return null
        runCatching { DiagnosticLogger.append(context, "ACCELERATION notice tier=$tier engine=$engineId") }
        return tier
    }

    fun setTranscriptionEngine(context: Context, value: String) {
        prefs(context).edit()
            .putString(KEY_TRANSCRIPTION_ENGINE, AsrEngine.fromId(value).id)
            .putBoolean(KEY_AUTOMATIC_ENGINE, false).apply()
    }

    fun getPendingRenameRecordingId(context: Context): String? =
        prefs(context).getString(KEY_PENDING_RENAME, null)

    fun setPendingRenameRecordingId(context: Context, id: String?) {
        prefs(context).edit()
            .putString(KEY_PENDING_RENAME, id).apply()
    }

    fun isVerboseDiagnostics(context: Context): Boolean =
        prefs(context)
            .getBoolean(KEY_VERBOSE_DIAGNOSTICS, false)

    fun setVerboseDiagnostics(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_VERBOSE_DIAGNOSTICS, enabled).apply()
    }


    fun getPerformancePreset(context: Context): String {
        val value = prefs(context).getString(KEY_PERFORMANCE_PRESET, "max") ?: "max"
        return if (value in performancePresets) value else "max"
    }

    fun setPerformancePreset(context: Context, value: String) {
        val normalized = when (value) {
            "high", "max", "custom" -> value
            "balanced" -> value
            else -> "max"
        }
        prefs(context).edit()
            .putString(KEY_PERFORMANCE_PRESET, normalized).apply()
    }

    fun getTranscriptHighlightOffsetMs(context: Context, profileId: String?): Int {
        val preferences = prefs(context)
        val key = highlightOffsetKey(profileId)
        if (preferences.contains(key)) {
            return preferences.getInt(key, 0).coerceIn(-1_000, 1_000)
        }

        // v5.32 reverses the user-facing direction so moving right advances the highlight.
        // Negate the legacy value once so existing users keep the same effective sync.
        val legacyKey = legacyHighlightOffsetKey(profileId)
        if (preferences.contains(legacyKey)) {
            val migrated = -preferences.getInt(legacyKey, 0).coerceIn(-1_000, 1_000)
            preferences.edit().putInt(key, migrated).remove(legacyKey).apply()
            return migrated
        }
        return 0
    }

    fun setTranscriptHighlightOffsetMs(context: Context, profileId: String?, valueMs: Int) {
        val clamped = valueMs.coerceIn(-1_000, 1_000)
        val snapped = kotlin.math.round(clamped / 50.0).toInt() * 50
        prefs(context).edit()
            .putInt(highlightOffsetKey(profileId), snapped.coerceIn(-1_000, 1_000)).apply()
    }

    private fun highlightOffsetKey(profileId: String?): String =
        KEY_HIGHLIGHT_LEAD_PREFIX + profileId.orEmpty().ifBlank { "legacy" }

    private fun legacyHighlightOffsetKey(profileId: String?): String =
        KEY_HIGHLIGHT_OFFSET_PREFIX + profileId.orEmpty().ifBlank { "legacy" }

    fun getCustomThreads(context: Context, detectedCores: Int = Runtime.getRuntime().availableProcessors()): Int {
        val max = detectedCores.coerceAtLeast(1)
        return prefs(context)
            .getInt(KEY_CUSTOM_THREADS, minOf(4, max)).coerceIn(1, max)
    }

    fun setCustomThreads(context: Context, value: Int, detectedCores: Int = Runtime.getRuntime().availableProcessors()) {
        val max = detectedCores.coerceAtLeast(1)
        prefs(context).edit()
            .putInt(KEY_CUSTOM_THREADS, value.coerceIn(1, max)).apply()
    }


    fun getTranscriptionLanguage(context: Context): String =
        prefs(context)
            .getString(KEY_TRANSCRIPTION_LANGUAGE, "en") ?: "en"

    fun setTranscriptionLanguage(context: Context, value: String) {
        prefs(context).edit()
            .putString(KEY_TRANSCRIPTION_LANGUAGE, value.ifBlank { "en" }).apply()
    }

    fun getChunkMinutes(context: Context): Int =
        prefs(context)
            .getInt(KEY_CHUNK_MINUTES, 2).coerceIn(1, 5)

    fun setChunkMinutes(context: Context, value: Int) {
        prefs(context).edit()
            .putInt(KEY_CHUNK_MINUTES, value.coerceIn(1, 5)).apply()
    }

    fun effectiveTranscriptionThreads(context: Context, detectedCores: Int = Runtime.getRuntime().availableProcessors()): Int {
        val cores = detectedCores.coerceAtLeast(1)
        return when (getPerformancePreset(context)) {
            "high" -> minOf(6, cores)
            "max" -> cores
            "custom" -> getCustomThreads(context, cores)
            else -> minOf(4, cores)
        }.coerceAtLeast(1)
    }


    fun getMicrophonePreset(context: Context): String {
        val value = prefs(context)
            .getString(KEY_MIC_PRESET, "default") ?: "default"
        return if (value in microphonePresets) value else "default"
    }

    fun setMicrophonePreset(context: Context, value: String) {
        val normalized = if (value in microphonePresets) value else "default"
        prefs(context).edit().putString(KEY_MIC_PRESET, normalized).apply()
    }

    fun getCustomMicrophoneSource(context: Context): String {
        val value = prefs(context).getString(KEY_MIC_SOURCE, "mic") ?: "mic"
        return if (value in microphoneSources) value else "mic"
    }

    fun setCustomMicrophoneSource(context: Context, value: String) {
        val normalized = if (value in microphoneSources) value else "mic"
        prefs(context).edit().putString(KEY_MIC_SOURCE, normalized).apply()
    }

    private fun getEffectMode(context: Context, key: String): String {
        val value = prefs(context).getString(key, "auto") ?: "auto"
        return if (value in effectModes) value else "auto"
    }

    private fun setEffectMode(context: Context, key: String, value: String) {
        val normalized = if (value in effectModes) value else "auto"
        prefs(context).edit().putString(key, normalized).apply()
    }

    fun getCustomNoiseSuppressorMode(context: Context): String = getEffectMode(context, KEY_NS_MODE)
    fun setCustomNoiseSuppressorMode(context: Context, value: String) = setEffectMode(context, KEY_NS_MODE, value)
    fun getCustomAecMode(context: Context): String = getEffectMode(context, KEY_AEC_MODE)
    fun setCustomAecMode(context: Context, value: String) = setEffectMode(context, KEY_AEC_MODE, value)

    fun isConfirmExitEnabled(context: Context): Boolean =
        prefs(context)
            .getBoolean(KEY_CONFIRM_EXIT, true)

    fun setConfirmExitEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_CONFIRM_EXIT, enabled).apply()
    }

    fun isSetupComplete(context: Context): Boolean =
        prefs(context)
            .getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context, complete: Boolean = true) {
        prefs(context).edit()
            .putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }
}
