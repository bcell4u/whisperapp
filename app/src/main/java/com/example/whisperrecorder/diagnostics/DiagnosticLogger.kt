package com.example.whisperrecorder.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
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
