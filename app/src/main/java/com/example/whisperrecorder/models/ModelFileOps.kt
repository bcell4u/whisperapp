package com.example.whisperrecorder.models

import android.content.Context
import android.net.Uri
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Shared high-throughput file operations used by every downloadable model family. */
internal data class ModelArchiveSnapshot(
    val treeUri: Uri,
    val directoryUri: Uri,
    val documents: List<SafStorage.TreeDocument>
)

internal object ModelFileOps {
    private const val BUFFER_BYTES = 1024 * 1024

    fun persistentModelsDirectory(context: Context): Pair<Uri, Uri>? {
        val tree = AppPrefs.getSaveTree(context) ?: return null
        return tree to WorkspaceStorage.modelsDirectory(context, tree)
    }

    /**
     * v5.36 stores model archives under Whisperapp/Models. The legacy root-level Models folder is
     * still read so upgrades never strand previously archived models or force a multi-gigabyte
     * migration during app startup.
     */
    private fun persistentModelDirectories(context: Context): List<Pair<Uri, Uri>> {
        val tree = AppPrefs.getSaveTree(context) ?: return emptyList()
        val primary = runCatching { WorkspaceStorage.modelsDirectory(context, tree) }.getOrNull()
        val legacy = runCatching { SafStorage.findDirectory(context, tree, "Models") }.getOrNull()
        return listOfNotNull(primary, legacy)
            .distinctBy { it.toString() }
            .map { tree to it }
    }

    fun persistentDocuments(context: Context): List<SafStorage.TreeDocument> {
        val merged = linkedMapOf<String, SafStorage.TreeDocument>()
        persistentModelDirectories(context).forEach { (tree, directory) ->
            runCatching { SafStorage.listDirectory(context, tree, directory) }
                .getOrDefault(emptyList())
                .forEach { doc -> if (doc.name !in merged) merged[doc.name] = doc }
        }
        return merged.values.toList()
    }

    fun findPersistentDocument(context: Context, displayName: String): SafStorage.TreeDocument? =
        persistentDocuments(context).firstOrNull { it.name == displayName }

    fun deletePersistentDocuments(context: Context, displayName: String): Boolean {
        var ok = true
        persistentModelDirectories(context).forEach { (tree, directory) ->
            runCatching { SafStorage.listDirectory(context, tree, directory) }
                .getOrDefault(emptyList())
                .filter { it.name == displayName }
                .forEach { if (!SafStorage.deleteDocument(context, it.uri)) ok = false }
        }
        return ok
    }

    fun snapshot(context: Context): ModelArchiveSnapshot? {
        val (tree, directory) = persistentModelsDirectory(context) ?: return null
        return ModelArchiveSnapshot(tree, directory, persistentDocuments(context))
    }

    fun digest(file: File, algorithm: String, isCancelled: () -> Boolean = { false }): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().buffered(BUFFER_BYTES).use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                if (isCancelled()) throw CancellationException("Model verification cancelled")
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val bytes = digest.digest()
        val hex = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        bytes.forEachIndexed { index, value ->
            val unsigned = value.toInt() and 0xff
            hex[index * 2] = digits[unsigned ushr 4]
            hex[index * 2 + 1] = digits[unsigned and 0x0f]
        }
        return String(hex)
    }

    fun verify(file: File, expectedBytes: Long? = null, algorithm: String, expectedDigest: String): Boolean {
        if (!file.isFile) return false
        if (expectedBytes != null && file.length() != expectedBytes) return false
        return digest(file, algorithm).equals(expectedDigest, ignoreCase = true)
    }

    /** Copy a persisted SAF document into app-private storage using one atomic replacement path. */
    fun restoreDocument(
        context: Context,
        sourceUri: Uri,
        destination: File,
        validate: (File) -> Unit = {}
    ) {
        val restoring = File(destination.parentFile, destination.name + ".restoring")
        restoring.delete()
        try {
            requireNotNull(context.contentResolver.openInputStream(sourceUri)) {
                "Unable to read saved ${destination.name}"
            }.use { input ->
                FileOutputStream(restoring, false).buffered(BUFFER_BYTES).use { output ->
                    input.copyTo(output, BUFFER_BYTES)
                }
            }
            validate(restoring)
            // Never unlink the previously verified model before replacing it: a crash
            // between delete() and renameTo() would destroy a working local copy.
            try {
                Files.move(restoring.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(restoring.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (t: Throwable) {
            restoring.delete()
            throw t
        }
    }

    /**
     * Resumable HTTP download shared by Whisper and GGUF models.
     * Returns the resolved total byte count when the server provides one.
     */
    fun downloadResumable(
        url: String,
        partial: File,
        expectedBytes: Long? = null,
        isCancelled: () -> Boolean,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Long {
        var existing = partial.takeIf { it.isFile }?.length() ?: 0L
        if (expectedBytes != null && existing > expectedBytes) {
            require(partial.delete()) { "Unable to discard oversized partial download" }
            existing = 0L
        }
        if (isCancelled()) throw CancellationException("Model download cancelled")
        // A completed .part file may survive process death before its checksum/rename step.
        if (expectedBytes != null && existing == expectedBytes) {
            onProgress(existing, existing)
            return existing // The caller still verifies the full checksum before installation.
        }

        var connection = connection(url, existing.takeIf { it > 0L })
        try {
            connection.connect()
            if (existing > 0L && connection.responseCode == 416) {
                // Unknown expected size: the server says the locally complete partial is at EOF.
                val remoteLength = connection.getHeaderField("Content-Range")
                    ?.takeIf { it.startsWith("bytes */") }
                    ?.removePrefix("bytes */")?.toLongOrNull()
                if (remoteLength == existing && expectedBytes == null) {
                    if (isCancelled()) throw CancellationException("Model download cancelled")
                    onProgress(existing, existing)
                    return existing // The caller's checksum validation remains mandatory.
                }
            }
            require(connection.responseCode in 200..299) { "Download failed: HTTP ${connection.responseCode}" }
            var resumed = existing > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (resumed) {
                val contentRange = connection.getHeaderField("Content-Range").orEmpty()
                require(contentRange.startsWith("bytes $existing-")) {
                    "Resume offset mismatch: expected $existing; Content-Range=$contentRange"
                }
            }
            if (existing > 0L && !resumed) {
                connection.disconnect()
                require(partial.delete() || !partial.exists()) { "Unable to restart partial download" }
                existing = 0L
                connection = connection(url, null)
                connection.connect()
                require(connection.responseCode in 200..299) { "Download failed: HTTP ${connection.responseCode}" }
                resumed = false
            }

            val remaining = connection.contentLengthLong.coerceAtLeast(0L)
            val total = expectedBytes ?: if (resumed && remaining > 0L) existing + remaining else remaining
            var done = existing
            onProgress(done, total)
            connection.inputStream.use { input ->
                FileOutputStream(partial, resumed).buffered(BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        if (isCancelled()) throw CancellationException("Model download cancelled")
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        done += count
                        onProgress(done, total)
                    }
                }
            }
            if (isCancelled()) throw CancellationException("Model download cancelled")
            if (expectedBytes != null) {
                require(partial.length() == expectedBytes) {
                    "Downloaded model has unexpected size ${partial.length()} (expected $expectedBytes)"
                }
            }
            return total
        } finally {
            connection.disconnect()
        }
    }

    private fun connection(url: String, rangeStart: Long?): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (rangeStart != null) setRequestProperty("Range", "bytes=$rangeStart-")
        }
}
