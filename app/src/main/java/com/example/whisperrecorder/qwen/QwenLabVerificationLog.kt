package com.example.whisperrecorder.qwen

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.example.whisperrecorder.BuildConfig
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Separate numerical-only test report: never emits audio, transcript or token IDs. */
internal object QwenLabVerificationLog {
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
    private val permittedNumbers = listOf("first_divergent_token_index", "generated_tokens",
        "native_loads", "native_threads", "elapsed_ms")
    private val permittedBooleans = listOf("internal_repeat_identical", "qnn_feature_repeat_identical",
        "eos_both", "decision_policy_selftest", "mrope_parity_verified", "numerical_logits_parity_verified")
    private val permittedHashes = listOf("model_sha256", "feature_sha256", "native_sha256")
    private val permittedStatuses = setOf("PASS_INTERNAL_ONLY", "PASS_REFERENCE_TOKENS",
        "FAIL_REFERENCE_TOKENS", "FAIL_INTERNAL_REPLAY", "FAIL_NATIVE_RECEIPT",
        "INCONCLUSIVE_NO_EOS", "INCONCLUSIVE_BAD_REFERENCE",
        "INCONCLUSIVE_REFERENCE_INPUT_MISMATCH", "INCONCLUSIVE_RUNTIME_ERROR",
        "FAIL_QNN_FEATURE_REPLAY")
    private val phases = setOf("MODEL_IMPORT", "PREPARE_QNN", "VERIFY_FIXTURE", "COMPLETE")
    fun render(startedMs: Long, result: JSONObject?, errorClass: String?, lastPhase: String): String = buildString {
        appendLine("WhisperRecorder Qwen GGUF Lab - decoder fixture verification")
        appendLine("schema=1")
        appendLine("app_version=${BuildConfig.VERSION_NAME}")
        appendLine("version_code=${BuildConfig.VERSION_CODE}")
        appendLine("started_utc=${Instant.ofEpochMilli(startedMs)}")
        val status = result?.optString("verification_status")?.takeIf { it in permittedStatuses }
            ?: "INCONCLUSIVE_RUNTIME_ERROR"
        appendLine("verification_status=$status")
        appendLine("last_phase=" + (lastPhase.takeIf { it in phases } ?: "UNKNOWN"))
        appendLine("reference_status=" + (result?.optString("reference_status")
            ?.takeIf { it in setOf("MISSING", "MATCH", "DIVERGED", "INVALID",
                "MODEL_OR_FEATURE_SHA_MISMATCH", "NOT_EVALUATED") } ?: "NOT_EVALUATED"))
        if (errorClass != null) {
            appendLine("error_class=" + errorClass.take(64).filter { it.isLetterOrDigit() || it == '_' })
        }
        result?.let { data ->
            for (key in permittedNumbers) {
                if (data.has(key) && data.opt(key) is Number) appendLine("$key=${data.optLong(key)}")
            }
            for (key in permittedBooleans) {
                if (data.has(key) && data.opt(key) is Boolean) appendLine("$key=${data.optBoolean(key)}")
            }
            for (key in permittedHashes) {
                val sha = data.optString(key)
                if (sha.matches(Regex("[a-f0-9]{64}"))) appendLine("$key=$sha")
            }
        }
        appendLine("synthetic_export_status=" +
            (result?.optString("synthetic_export_status")?.takeIf { it in setOf("SAVED", "FAILED") }
                ?: "NOT_AVAILABLE"))
        appendLine("reference_authenticity_verified=false")
        appendLine("official_logit_parity_verified=false")
        appendLine("mrope_equivalence=UNQUALIFIED")
        appendLine("contains_transcript=false")
        appendLine("contains_raw_tokens=false")
        appendLine("contains_private_paths=false")
    }

    fun save(context: Context, startedMs: Long, result: JSONObject?, errorClass: String?, lastPhase: String): String {
        val filename = "WhisperRecorder-QwenVerify-${stamp.format(Instant.ofEpochMilli(startedMs))}-" +
            UUID.randomUUID().toString().take(8) + ".log"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, QwenLabDiagnosticLog.RELATIVE_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Cannot create verification log")
        var committed = false
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(render(startedMs, result, errorClass, lastPhase).toByteArray()) }
                ?: error("Verification log stream unavailable")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, done, null, null) == 1)
            committed = true
            return filename
        } finally {
            if (!committed) runCatching { resolver.delete(uri, null, null) }
        }
    }
}
