package com.example.whisperrecorder.diagnostics

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
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Private, bounded, local diagnostic archives. Never reads recordings, transcripts, model
 * weights, token values, SAF URLs, or arbitrary files. Existing logging remains intact.
 * The ZIP contains only explicitly assembled performance and failure metadata.
 */
object DiagnosticReports {
    private const val MAX_REPORTS = 8
    private const val MAX_TOTAL_BYTES = 32L * 1024 * 1024
    private const val MAX_SAMPLES = 2048
    private val fileLock = Any()
    private const val PREFIX = "WhisperRecorder-Diagnostics-"

    private fun directory(context: Context): File =
        File(context.filesDir, "diagnostics/reports").apply { mkdirs() }

    fun list(context: Context): List<File> = synchronized(fileLock) {
        directory(context).listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(PREFIX) && it.name.endsWith(".zip") }
            .sortedByDescending { it.lastModified() }
    }

    fun delete(context: Context, name: String): Boolean = synchronized(fileLock) {
        // File deletion is deliberately scoped to a fixed app-private directory.
        val item = list(context).firstOrNull { it.name == name } ?: return@synchronized false
        item.delete()
    }

    fun deleteAll(context: Context): Int = synchronized(fileLock) {
        list(context).count { it.delete() }
    }

    fun begin(context: Context, engine: String, audioDurationMs: Long): Session {
        return Session(context.applicationContext,
            engine.replace(Regex("[^a-zA-Z0-9_.-]"), "_").take(48),
            audioDurationMs.coerceAtLeast(0))
    }

    fun manualSnapshot(context: Context): File? =
        Session(context.applicationContext, "manual", 0L, false).apply {
            event("manual_diagnostic_snapshot")
        }.finish("manual")

    /** Identifies interrupted work on a subsequent app launch without storing recording IDs. */
    fun recoverAbandoned(context: Context): File? = synchronized(fileLock) {
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

        @Synchronized fun finish(
            outcome: String,
            failureStage: String? = null,
            error: Throwable? = null,
            totals: Map<String, Long> = emptyMap()
        ): File? {
            if (closed) return null
            closed = true
            val endElapsed = SystemClock.elapsedRealtime()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
            val name = "$PREFIX$stamp-${sessionId.take(8)}.zip"
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
                        add("audit/collection-status.json", JSONObject()
                            .put("status", "complete")
                            .put("bounded_events", true)
                            .put("raw_user_content_collected", false).toString(2))
                    }
                }
                check(temporary.renameTo(final)) { "Report publication failed" }
                synchronized(fileLock) {
                    // Only delete our own active marker; never touch model or workspace files.
                    val registeredAt = runCatching { JSONObject(activeMarker.readText())
                        .optLong("started_epoch_ms") }.getOrNull()
                    if (registeredAt == startEpoch) activeMarker.delete()
                    prune(context)
                }
                final
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
        val files = list(context).sortedBy { it.lastModified() }
        var bytes = files.sumOf { it.length() }
        var count = files.size
        for (file in files) {
            if (count <= MAX_REPORTS && bytes <= MAX_TOTAL_BYTES) break
            val size = file.length()
            if (file.delete()) { bytes -= size; count-- }
        }
    }
}
