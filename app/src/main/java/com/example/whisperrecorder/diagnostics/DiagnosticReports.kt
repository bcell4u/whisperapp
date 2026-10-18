package com.example.whisperrecorder.diagnostics

import android.net.Uri
import androidx.core.content.FileProvider
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Bounded diagnostic archives published in the existing user-selected Whisperapp/Logs folder.
 * Private storage is used only for atomic staging, the interruption marker, and a fallback
 * when SAF storage is unavailable. Existing logging remains intact. Never reads recordings,
 * transcripts, model weights, token values, or arbitrary files into the ZIP.
 */
object DiagnosticReports {
    private const val MAX_REPORTS = 8
    private const val MAX_TOTAL_BYTES = 32L * 1024 * 1024
    private const val MAX_SAMPLES = 2048
    private val fileLock = Any()
    private const val PREFIX = "WhisperRecorder-Diagnostics-"
    // Restrict listing, retention and deletion to files produced by this component.
    private val legacyName = Regex("^WhisperRecorder-Diagnostics-[0-9]{8}-[0-9]{6}-[0-9]{3}-[a-f0-9]{8}\\.zip$")
    private val devName = Regex("^WhisperRecorder-Diagnostics-dev-[0-9]{8}-[0-9]{6}-[0-9]{3}-[a-f0-9]{8}\\.zip$")
    private fun ownedName(context: Context, name: String): Boolean =
        if (context.packageName.endsWith(".dev")) devName.matches(name) else legacyName.matches(name)
    private fun privateName(context: Context, name: String): Boolean =
        ownedName(context, name) || (context.packageName.endsWith(".dev") && legacyName.matches(name))

    enum class Location { WORKSPACE_LOGS, PRIVATE_FALLBACK }

    data class Report internal constructor(
        val name: String,
        val sizeBytes: Long,
        val location: Location,
        internal val uri: Uri? = null,
        internal val file: File? = null
    )

    private fun directory(context: Context): File =
        File(context.filesDir, "diagnostics/reports").apply { mkdirs() }

    private fun workspaceReports(context: Context): List<Report> {
        val tree = AppPrefs.getSaveTree(context) ?: return emptyList()
        val logs = WorkspaceStorage.logsDirectory(context, tree)
        return SafStorage.listDirectory(context, tree, logs)
            .filter { ownedName(context, it.name) }
            .map { Report(it.name, it.size, Location.WORKSPACE_LOGS, uri = it.uri) }
    }

    fun list(context: Context): List<Report> = synchronized(fileLock) {
        // A revoked/unavailable SAF grant must never hide an earlier private report.
        val workspace = runCatching { workspaceReports(context) }.getOrDefault(emptyList())
        val names = workspace.mapTo(HashSet()) { it.name }
        val privateReports = directory(context).listFiles().orEmpty()
            .filter { it.isFile && privateName(context, it.name) && it.name !in names }
            .map { Report(it.name, it.length(), Location.PRIVATE_FALLBACK, file = it) }
        (workspace + privateReports).sortedByDescending { it.name }
    }

    fun shareUri(context: Context, report: Report): Uri = when (report.location) {
        Location.WORKSPACE_LOGS -> requireNotNull(report.uri)
        Location.PRIVATE_FALLBACK -> FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", requireNotNull(report.file))
    }

    fun delete(context: Context, name: String): Boolean = synchronized(fileLock) {
        // No arbitrary file deletion from the user's Logs folder, even with a forged name.
        if (!privateName(context, name)) return@synchronized false
        val report = list(context).firstOrNull { it.name == name } ?: return@synchronized false
        deleteReport(context, report)
    }

    fun deleteAll(context: Context): Int = synchronized(fileLock) {
        list(context).count { deleteReport(context, it) }
    }

    private fun deleteReport(context: Context, report: Report): Boolean = when (report.location) {
        Location.WORKSPACE_LOGS -> report.uri?.let { SafStorage.deleteDocument(context, it) } ?: false
        Location.PRIVATE_FALLBACK -> report.file?.delete() == true
    }

    /** Shareable location for UI; the user-selected workspace takes priority. */
    fun storageDescription(context: Context): String =
        if (AppPrefs.getSaveTree(context) == null) "Private fallback: select a workspace folder to use Whisperapp/Logs"
        else "Whisperapp/Logs in your selected workspace (private fallback if unavailable)"

    private fun sha256(input: java.io.InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest()
    }

    private fun publish(context: Context, source: File): Report {
        // A valid ZIP is prepared locally before any SAF I/O. If access is revoked,
        // retain the complete report privately rather than losing failure diagnostics.
        ZipFile(source).use { require(it.getEntry("manifest.json") != null) }
        val tree = AppPrefs.getSaveTree(context) ?: return Report(
            source.name, source.length(), Location.PRIVATE_FALLBACK, file = source)
        val published = runCatching {
            val logs = WorkspaceStorage.logsDirectory(context, tree)
            val uri = SafStorage.copyFileToDirectory(
                context, source, tree, logs, "application/zip", source.name)
            try {
                check(SafStorage.displayName(context, uri) == source.name) {
                    "Provider changed the diagnostic ZIP filename"
                }
                val verified = requireNotNull(context.contentResolver.openInputStream(uri))
                    .let { sha256(it) }.contentEquals(sha256(source.inputStream()))
                check(verified) { "Copied diagnostic ZIP failed checksum verification" }
            } catch (error: Exception) {
                SafStorage.deleteDocument(context, uri)
                throw error
            }
            Report(source.name, source.length(), Location.WORKSPACE_LOGS, uri = uri)
        }.getOrNull()
        if (published != null) {
            source.delete() // If deletion fails, the duplicate is excluded from listings by name.
            return published
        }
        return Report(source.name, source.length(), Location.PRIVATE_FALLBACK, file = source)
    }

    fun begin(context: Context, engine: String, audioDurationMs: Long): Session {
        return Session(context.applicationContext,
            engine.replace(Regex("[^a-zA-Z0-9_.-]"), "_").take(48),
            audioDurationMs.coerceAtLeast(0))
    }

    fun manualSnapshot(context: Context): Report? =
        Session(context.applicationContext, "manual", 0L, false).apply {
            event("manual_diagnostic_snapshot")
        }.finish("manual")

    /** Identifies interrupted work on a subsequent app launch without storing recording IDs. */
    fun recoverAbandoned(context: Context): Report? = synchronized(fileLock) {
        val marker = File(directory(context), "active-session.json")
        if (!marker.isFile) return@synchronized null
        val metadata = runCatching { JSONObject(marker.readText()) }.getOrNull()
        val ageMs = System.currentTimeMillis() - (metadata?.optLong("started_epoch_ms") ?: 0L)
        // Do not treat a still-running service as a crash: only recover stale markers.
        if (ageMs < 5 * 60_000L) return@synchronized null
        val session = Session(context.applicationContext, metadata?.optString("engine") ?: "unknown", 0L, false)
        session.event("previous_session_interrupted")
        val report = session.finish("interrupted", "prior worker stopped before finalization", null)
        marker.delete()
        report
    }

    class Session internal constructor(
        private val context: Context,
        private val engine: String,
        private val audioDurationMs: Long,
        registerActive: Boolean = true
    ) {
        private val startElapsed = SystemClock.elapsedRealtime()
        private val startEpoch = System.currentTimeMillis()
        private val samples = ArrayList<JSONObject>()
        private val workerMetrics = ArrayList<JSONObject>()
        private var closed = false
        private var dropped = 0
        private val sessionId = UUID.randomUUID().toString()
        private val activeMarker = File(directory(context), "active-session.json")

        init {
            if (registerActive) runCatching {
                synchronized(fileLock) {
                    activeMarker.writeText(JSONObject().put("engine", engine)
                        .put("started_epoch_ms", startEpoch).toString())
                }
            }
        }

        @Synchronized fun event(stage: String, elapsedMs: Long? = null, index: Int? = null) {
            if (closed) return
            if (samples.size >= MAX_SAMPLES) { dropped++; return }
            val item = JSONObject()
                .put("stage", stage.replace(Regex("[^a-zA-Z0-9_.-]"), "_").take(64))
                .put("elapsed_since_start_ms", SystemClock.elapsedRealtime() - startElapsed)
            if (elapsedMs != null) item.put("duration_ms", elapsedMs.coerceAtLeast(0))
            if (index != null) item.put("segment_index", index.coerceAtLeast(0))
            samples.add(item)
        }

        /** Numeric-only cross-process profiling. Never accepts filenames, transcript text or token IDs. */
        @Synchronized fun workerMetric(name: String, value: Long, segmentIndex: Int) {
            if (closed) return
            if (workerMetrics.size >= MAX_SAMPLES) { dropped++; return }
            if (name !in setOf("preprocess_ms", "encoder_ms", "decoder_load_ms", "decoder_ms",
                    "prefill_ms", "step_ms", "decoder_steps", "generated_token_count",
                    "embedding_io_ms", "argmax_ms", "decoder_threads", "worker_cpu_ms",
                    "worker_wall_ms", "worker_pss_kb", "thermal_start", "thermal_end",
                    "encoder_graph_calls", "decoder_npu_graph_calls", "decoder_kv_payload_bytes_max",
                    "decoder_step_p50_ms", "decoder_step_p95_ms", "decoder_step_max_ms",
                    "decoder_first_step_ms", "decoder_slow_steps_over_250ms",
                    "decoder_prefill_pss_kb", "decoder_pss_before_load_kb",
                    "decoder_pss_after_init_kb", "decoder_pss_after_step_kb",
                    "decoder_reusable_step_inputs",
                    "qwen_cleanup_verified")) return
            workerMetrics.add(JSONObject().put("name", name).put("value", value.coerceAtLeast(0))
                .put("segment_index", segmentIndex.coerceAtLeast(0)))
        }

        @Synchronized fun finish(
            outcome: String,
            failureStage: String? = null,
            error: Throwable? = null,
            totals: Map<String, Long> = emptyMap()
        ): Report? {
            if (closed) return null
            closed = true
            val endElapsed = SystemClock.elapsedRealtime()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
            val variant = if (context.packageName.endsWith(".dev")) "dev-" else ""
            val name = "$PREFIX$variant$stamp-${sessionId.take(8)}.zip"
            val reports = directory(context)
            val temporary = File(reports, ".$name.part")
            val final = File(reports, name)
            return try {
                val am = context.getSystemService(ActivityManager::class.java)
                val memory = runCatching { am.getProcessMemoryInfo(intArrayOf(Process.myPid())).firstOrNull() }.getOrNull()
                val power = context.getSystemService(PowerManager::class.java)
                val device = JSONObject()
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("model", Build.MODEL)
                    .put("soc", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "unavailable")
                    .put("sdk", Build.VERSION.SDK_INT)
                    .put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                    .put("thermal_status", runCatching {
                        if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else -1
                    }.getOrDefault(-1))
                    .put("process_pss_kb", memory?.totalPss ?: -1)
                    .put("java_heap_used_bytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                val version = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "unknown"
                val metadata = JSONObject()
                    .put("schema_version", 1)
                    .put("app_version", version)
                    .put("session_id", sessionId)
                    .put("engine", engine)
                    .put("outcome", outcome)
                    .put("started_epoch_ms", startEpoch)
                    .put("wall_ms", endElapsed - startElapsed)
                    .put("audio_duration_ms", audioDurationMs)
                    .put("sample_count", samples.size)
                    .put("dropped_samples", dropped)
                    .put("privacy", "No audio, transcript text, tokens, model data or file paths are included")
                // Only fixed, predefined stage labels are exportable; runtime stage strings
                // can sometimes contain user-supplied recording titles or provider messages.
                val safeStage = when (failureStage?.lowercase(Locale.US)) {
                    "preparing media" -> "preparing_media"
                    "loading model" -> "loading_model"
                    "transcribing" -> "transcribing"
                    "saving transcript" -> "saving_transcript"
                    "user_or_service_cancellation" -> "cancellation"
                    "prior worker stopped before finalization" -> "interrupted"
                    null -> null
                    else -> "other_stage"
                }
                val failure = JSONObject().put("stage", safeStage ?: JSONObject.NULL)
                    .put("exception_class", error?.javaClass?.name ?: JSONObject.NULL)
                    .put("error_code", classify(error))
                // Throwable messages often include sensitive URIs or spoken text. Stack frame
                // symbols and line numbers are sufficient to pinpoint the failing operation.
                val frames = JSONArray()
                error?.stackTrace?.take(32)?.forEach { frame ->
                    frames.put(JSONObject().put("class", frame.className)
                        .put("method", frame.methodName)
                        .put("line", frame.lineNumber))
                }
                failure.put("stack_frames", frames)
                val counts = JSONObject()
                totals.forEach { (k, v) ->
                    if (k.matches(Regex("[a-zA-Z0-9_]{1,48}"))) counts.put(k, v)
                }
                FileOutputStream(temporary).buffered().use { stream ->
                    ZipOutputStream(stream).use { zip ->
                        fun add(path: String, data: String) {
                            zip.putNextEntry(ZipEntry(path))
                            zip.write(data.toByteArray(Charsets.UTF_8))
                            zip.closeEntry()
                        }
                        add("manifest.json", metadata.toString(2))
                        add("device/runtime.json", device.toString(2))
                        add("performance/totals.json", counts.toString(2))
                        add("errors/failure.json", failure.toString(2))
                        add("performance/stage-events.json", JSONArray(samples).toString(2))
                        add("performance/qwen-worker-metrics.json", JSONArray(workerMetrics).toString(2))
                        add("audit/collection-status.json", JSONObject()
                            .put("status", "complete")
                            .put("bounded_events", true)
                            .put("raw_user_content_collected", false).toString(2))
                    }
                }
                check(temporary.renameTo(final)) { "Report publication failed" }
                val published = synchronized(fileLock) { publish(context, final) }
                synchronized(fileLock) {
                    // Only delete our own active marker; never touch model or workspace files.
                    val registeredAt = runCatching { JSONObject(activeMarker.readText())
                        .optLong("started_epoch_ms") }.getOrNull()
                    if (registeredAt == startEpoch) activeMarker.delete()
                    prune(context)
                }
                published
            } catch (_: Exception) {
                temporary.delete()
                null
            }
        }
    }

    private fun classify(error: Throwable?): String = when (error) {
        null -> "none"
        is OutOfMemoryError -> "out_of_memory"
        is java.io.IOException -> "io_error"
        is IllegalArgumentException -> "invalid_argument"
        is IllegalStateException -> "invalid_state"
        else -> error.javaClass.simpleName.take(64)
    }

    private fun prune(context: Context) {
        val files = list(context).sortedBy { it.name }
        var bytes = files.sumOf { it.sizeBytes }
        var count = files.size
        for (file in files) {
            if (count <= MAX_REPORTS && bytes <= MAX_TOTAL_BYTES) break
            val size = file.sizeBytes
            if (deleteReport(context, file)) { bytes -= size; count-- }
        }
    }
}
