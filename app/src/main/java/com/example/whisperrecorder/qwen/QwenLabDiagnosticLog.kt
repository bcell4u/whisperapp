package com.example.whisperrecorder.qwen

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.whisperrecorder.BuildConfig
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Writes a single privacy-safe report per attempted Lab transcription to public Downloads.
 * Uses scoped MediaStore Downloads (minSdk 29); no storage permission or raw path access.
 * No transcript, audio, tokens, model paths, user file names, or untrusted error messages.
 */
internal object QwenLabDiagnosticLog {
    const val RELATIVE_DIR = "Download/SFSU/Whisperapp/Logs/"
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

    internal data class RunReport(
        val startedEpochMs: Long,
        val elapsedMs: Long,
        val status: Status,
        val phase: Phase,
        val encodedSegments: Int,
        val errorClass: String? = null,
        val result: JSONObject? = null
    )

    internal enum class Status { SUCCESS, FAILED, CANCELLED }
    internal enum class Phase { IMPORT_MODEL, CONNECT, PREPARE, DECODE_AUDIO, ENCODE_QNN, DECODE_GGUF, COMPLETE }

    // This is an explicit numerical allowlist, not arbitrary JSON serialisation.
    private val numericMetrics = listOf(
        "segments", "model_loads", "preprocess_ms", "encoder_ms",
        "model_load_ms", "decoder_ms", "decoder_segments_wall_ms",
        "generated_tokens", "eos_segments", "native_rss_peak_kb", "native_cpu_ms"
    )

    internal fun render(report: RunReport): String = buildString {
        appendLine("WhisperRecorder Qwen GGUF Lab - on-device experimental transcription")
        appendLine("schema=1")
        appendLine("app_version=${BuildConfig.VERSION_NAME}")
        appendLine("version_code=${BuildConfig.VERSION_CODE}")
        appendLine("started_utc=${Instant.ofEpochMilli(report.startedEpochMs)}")
        appendLine("status=${report.status.name}")
        appendLine("last_phase=${report.phase.name}")
        appendLine("attempt_elapsed_ms=${report.elapsedMs.coerceAtLeast(0)}")
        appendLine("encoded_segments=${report.encodedSegments.coerceIn(0, 20)}")
        report.errorClass?.let {
            appendLine("error_class=${it.take(64).filter { c -> c.isLetterOrDigit() || c == '_' }}")
        }
        appendLine("encoder_required=QNNExecutionProvider_no_cpu_fallback")
        appendLine("decoder=llama.cpp_ARM64_dotprod_Q8_0_four_threads")
        appendLine("mrope_equivalence=UNQUALIFIED")
        report.result?.let { result ->
            for (key in numericMetrics) {
                if (!result.isNull(key) && result.has(key)) {
                    // Ignore strings/floats/object fields; only metrics with integral JSON values.
                    val number = result.opt(key)
                    if (number is Number && number.toDouble().isFinite() &&
                        number.toDouble() == number.toLong().toDouble()) {
                        appendLine("$key=${number.toLong()}")
                    }
                }
            }
            appendLine("npu_reported=${result.optString("encoder_execution_provider") == "QNNExecutionProvider"}")
            appendLine("model_parity_verified=${result.optBoolean("mrope_parity_verified", false)}")
        }
        appendLine("contains_transcript=false")
        appendLine("contains_private_paths=false")
    }

    /** Returns the user-visible filename (not a private filesystem path). */
    fun save(context: Context, report: RunReport): String {
        val name = "WhisperRecorder-QwenGGUF-${stamp.format(Instant.ofEpochMilli(report.startedEpochMs))}-${UUID.randomUUID().toString().take(8)}.log"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                RELATIVE_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Cannot create Downloads log")
        var published = false
        try {
            resolver.openOutputStream(uri, "w")?.use { stream ->
                stream.write(render(report).toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Cannot write Downloads log")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, done, null, null) == 1) { "Cannot publish Downloads log" }
            published = true
            return name
        } finally {
            // Delete only the pending MediaStore item created by THIS run on write failure.
            if (!published) runCatching { resolver.delete(uri, null, null) }
        }
    }
}
