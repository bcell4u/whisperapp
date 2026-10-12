package com.example.whisperrecorder.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import org.json.JSONObject
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private const val DIR = "diagnostics"
    private const val FILE = "transcription.log"
    private const val MAX_BYTES = 2L * 1024L * 1024L
    private const val KEEP_BYTES = 1024L * 1024L
    private const val COPY_BUFFER_BYTES = 64 * 1024
    private val lock = Any()
    // All formatting happens under [lock], so one formatter can be reused safely instead of
    // constructing a SimpleDateFormat for every diagnostic line and heartbeat.
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun logFile(context: Context): File =
        File(File(context.filesDir, DIR).apply { mkdirs() }, FILE)

    fun append(context: Context, message: String) {
        synchronized(lock) {
            val file = logFile(context)
            rotateIfNeeded(file)
            val stamp = timestampFormat.format(Date())
            file.appendText("$stamp  $message\n", Charsets.UTF_8)
        }
    }

    fun appendError(context: Context, label: String, throwable: Throwable) {
        append(context, "$label: ${throwable::class.java.simpleName}: ${throwable.message ?: "(no message)"}")
        val trace = Log.getStackTraceString(throwable)
        synchronized(lock) {
            val file = logFile(context)
            rotateIfNeeded(file)
            file.appendText(trace + "\n", Charsets.UTF_8)
        }
        mirrorErrorToWorkspace(context, label, throwable, trace)
        // An error automatically creates a bounded local ZIP without attaching recordings or weights.
        runCatching { createSnapshot(context, "automatic-error", label, throwable) }
            .onFailure { Log.w("WhisperRecorderDiag", "Automatic diagnostic ZIP failed", it) }
    }

    private fun mirrorErrorToWorkspace(context: Context, label: String, throwable: Throwable, trace: String) {
        val tree = AppPrefs.getSaveTree(context) ?: return
        runCatching {
            val logsDir = WorkspaceStorage.logsDirectory(context, tree)
            val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS", Locale.US).format(Date())
            val safeLabel = label.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9._-]+"), "-")
                .trim('-')
                .take(48)
                .ifBlank { "error" }
            val body = buildString {
                append("Whisper Recorder error log\n")
                append("Time: ").append(stamp).append('\n')
                append("Label: ").append(label).append('\n')
                append("Error: ").append(throwable::class.java.name).append(": ")
                    .append(throwable.message ?: "(no message)").append("\n\n")
                append(trace)
                if (!trace.endsWith('\n')) append('\n')
            }
            SafStorage.writeTextToDirectory(context, logsDir, "error-$stamp-$safeLabel.txt", body)
        }
    }

    fun readAll(context: Context): String = synchronized(lock) {
        val file = logFile(context)
        val main = if (file.isFile) file.readText(Charsets.UTF_8) else ""
        val worker = File(context.filesDir, "npu-diagnostics/runtime-worker.log")
        if (!worker.isFile) main else {
            // QNN session/driver failures are often only in the isolated worker process.
            // Include its bounded recent log in diagnostics exports without reading gigabytes.
            val tail = RandomAccessFile(worker, "r").use { input ->
                val from = (input.length() - 128L * 1024L).coerceAtLeast(0L)
                input.seek(from)
                ByteArray((input.length() - from).toInt()).also(input::readFully)
                    .toString(Charsets.UTF_8)
            }
            "$main\n\n--- NPU worker log (last 128 KiB) ---\n$tail"
        }
    }

    /** Read a bounded preview without materializing the full diagnostic log. */
    fun readTail(context: Context, maxChars: Int): String = synchronized(lock) {
        if (maxChars <= 0) return@synchronized ""
        val file = logFile(context)
        if (!file.isFile || file.length() <= 0L) return@synchronized ""

        // UTF-8 uses at most four bytes per code point. Reading up to 4x the requested character
        // count guarantees enough data for the preview while keeping memory bounded.
        val maxBytes = maxChars.toLong() * 4L
        val start = (file.length() - maxBytes).coerceAtLeast(0L)
        val bytes = ByteArray((file.length() - start).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        RandomAccessFile(file, "r").use { input ->
            input.seek(start)
            input.readFully(bytes)
        }
        var text = bytes.toString(Charsets.UTF_8)
        if (start > 0L) text = text.substringAfter('\n', "")
        text.takeLast(maxChars)
    }

    fun clear(context: Context) {
        synchronized(lock) {
            val file = logFile(context)
            if (file.exists()) file.writeText("", Charsets.UTF_8)
        }
    }

    private const val BUNDLE_LIMIT = 5
    private const val BUNDLE_LOG_BYTES = 384 * 1024
    private const val BUNDLE_WORKER_BYTES = 128 * 1024

    private fun bundleFolder(context: Context) =
        File(File(context.filesDir, DIR), "bundles").apply { mkdirs() }

    /** Only app-owned diagnostic ZIPs; excludes recordings, models, and workspace exports. */
    fun bundles(context: Context): List<File> = synchronized(lock) {
        bundleFolder(context).listFiles()?.filter {
            it.isFile && it.name.matches(Regex("^WhisperRecorder-diagnostics-[0-9-]+\\.zip$"))
        }?.sortedByDescending { it.name } ?: emptyList()
    }

    /** Clear only these ZIPs; doesn't touch worker logs, models, user recordings or the raw logger. */
    fun deleteBundles(context: Context): Int = synchronized(lock) {
        val existing = bundles(context)
        var deleted = 0
        for (file in existing) if (file.delete()) deleted++
        deleted
    }

    private fun boundedLog(file: File, bytes: Int): String {
        if (!file.isFile || file.length() == 0L) return "(not available)"
        val buffer = ByteArray(minOf(file.length(), bytes.toLong()).toInt())
        RandomAccessFile(file, "r").use {
            it.seek((it.length() - buffer.size).coerceAtLeast(0))
            it.readFully(buffer)
        }
        val text = buffer.toString(Charsets.UTF_8)
        return if (file.length() > buffer.size) text.substringAfter('\n', "") else text
    }

    /** Defense in depth: scrub common credentials, filesystem identity and transcript-bearing lines. */
    internal fun sanitizeSupportText(input: String): String = input
        .replace(Regex("hf_[A-Za-z0-9]{12,}"), "[REDACTED_TOKEN]")
        .replace(Regex("(?i)Bearer\\s+[^\\s]+"), "Bearer [REDACTED]")
        .replace(Regex("(?i)(token|password|secret|authorization|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+"), "$1=[REDACTED]")
        .replace(Regex("(?i)content://[^\\s]+"), "[CONTENT_URI]")
        .replace(Regex("(?i)/storage/emulated/[0-9]+/[^\\s]+"), "[USER_STORAGE_PATH]")
        .lineSequence()
        .filterNot { it.contains(Regex("(?i)(transcript\\s*[:=]|recognized_text|transcription_text|raw_audio|waveform_data)")) }
        .joinToString("\n")

    private fun addZipText(zip: ZipOutputStream, name: String, body: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(sanitizeSupportText(body).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /** Auto-called by appendError; manually callable for performance/storage snapshots. */
    fun createSnapshot(
        context: Context,
        reason: String = "manual-snapshot",
        failureLabel: String = "",
        failure: Throwable? = null
    ): File = synchronized(lock) {
        val folder = bundleFolder(context)
        val file = File(folder, "WhisperRecorder-diagnostics-${System.currentTimeMillis()}-${android.os.Process.myPid()}-${System.nanoTime()}.zip")
        val pending = File(folder, file.name + ".partial")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memory = android.app.ActivityManager.MemoryInfo()
        runCatching { am?.getMemoryInfo(memory) }
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { context.getSystemService(PowerManager::class.java)?.currentThermalStatus }.getOrNull()
        } else null
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        val summary = JSONObject()
            .put("format", "whisperrecorder-diagnostic-v1")
            .put("createdUtcMs", System.currentTimeMillis())
            .put("reason", reason)
            .put("failureStage", failureLabel)
            .put("exceptionType", failure?.javaClass?.simpleName ?: "")
            .put("appVersion", version)
            .put("androidApi", Build.VERSION.SDK_INT)
            .put("hardwareModel", Build.MODEL)
            .put("deviceSoc", if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "unknown")
            .put("processPssKb", Debug.getPss())
            .put("availableMemoryBytes", memory.availMem)
            .put("lowMemory", memory.lowMemory)
            .put("thermalStatus", thermal ?: -1)
            .put("containsAudioOrModelWeights", false)
        try {
            ZipOutputStream(FileOutputStream(pending).buffered()).use { zip ->
                addZipText(zip, "failure-and-device.json", summary.toString(2))
                addZipText(zip, "transcription-tail.log", boundedLog(logFile(context), BUNDLE_LOG_BYTES))
                addZipText(zip, "npu-worker-tail.log", boundedLog(File(context.filesDir, "npu-diagnostics/runtime-worker.log"), BUNDLE_WORKER_BYTES))
                addZipText(zip, "failure-stack.txt", failure?.let { Log.getStackTraceString(it).takeLast(16_384) } ?: "No exception; manual snapshot")
                addZipText(zip, "README.txt", "Diagnostic snapshot. Review before sharing. No audio, transcripts, source models, SAF archive, tokens or recordings intentionally included. Logs are bounded and best-effort redacted. Original files were not modified.")
            }
            if (!pending.renameTo(file)) error("Unable to finalize diagnostic ZIP")
        } catch (error: Throwable) {
            pending.delete()
            throw error
        }
        bundles(context).drop(BUNDLE_LIMIT).forEach { it.delete() }
        file
    }

    fun sizeBytes(context: Context): Long = synchronized(lock) { logFile(context).length() }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_BYTES) return

        // Keep only the tail without materializing the entire 2+ MiB log in memory. Rotation is
        // rare, but this prevents an avoidable multi-megabyte allocation while transcription may
        // already have large model/audio buffers resident.
        val temp = File(file.parentFile, file.name + ".trim")
        RandomAccessFile(file, "r").use { input ->
            val start = (input.length() - KEEP_BYTES).coerceAtLeast(0L)
            input.seek(start)
            FileOutputStream(temp, false).buffered(COPY_BUFFER_BYTES).use { output ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                var firstBlock = start > 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    var offset = 0
                    if (firstBlock) {
                        val newline = (0 until count).firstOrNull { buffer[it] == '\n'.code.toByte() }
                        offset = if (newline == null) count else newline + 1
                        firstBlock = newline == null
                    }
                    if (offset < count) output.write(buffer, offset, count - offset)
                }
                output.write("\n--- diagnostic log trimmed to most recent 1 MiB ---\n".toByteArray(Charsets.UTF_8))
            }
        }
        if (file.exists() && !file.delete()) {
            temp.delete()
            return
        }
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }
}
