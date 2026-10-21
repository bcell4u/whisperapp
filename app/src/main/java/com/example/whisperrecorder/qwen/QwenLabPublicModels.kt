package com.example.whisperrecorder.qwen

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Uninstall-resistant, user-visible model archive. This is NOT app-specific external storage:
 * MediaStore.Downloads creates files in Internal storage/Download/SFSU/Whisperapp/Models.
 * After reinstall, Android can treat the old MediaStore items as another app's downloads;
 * the user restores through the Storage Access Framework directory picker.
 *
 * Internal private copies remain the only models used by the QNN/llama.cpp workers.
 * Never delete files belonging to users or previous installations.
 */
internal object QwenLabPublicModels {
    const val RELATIVE_DIR = "Download/SFSU/Whisperapp/Models/"
    private const val PUBLIC_DECODER = "Qwen3-ASR-0.6B-Q8_0.gguf"
    private val ENCODER_FILES = listOf(
        "encoder-qnn-fp16.bin", "tokenizer.json",
        "golden-synthetic-pcm-12s-f32.raw", "golden-synthetic-mel-12s-f32.raw",
        "model-manifest.json"
    )

    private fun digest(file: File) = QwenLabModelDownloader.sha256(file)

    /**
     * Back up an already verified private encoder + official Q8 GGUF. Reuses public files
     * only when their content matches; commits each new public file after hashing the copy.
     * Writes are initiated only by the user's Download or Back up button.
     */
    fun backupVerified(
        context: Context, cancelled: () -> Boolean, status: (String) -> Unit
    ) {
        require(QwenLabEncoderAssets.valid(context)) { "Private QNN package is not verified" }
        require(QwenLabModelDownloader.officialDecoderPresent(context)) {
            "Backups require the verified official Q8_0 decoder"
        }
        val encoder = QwenLabEncoderAssets.directory(context)
        require(File(encoder, "model-manifest.json").isFile) {
            "A model manifest is required for a portable, hash-verified backup"
        }
        val assets = ENCODER_FILES.map { it to File(encoder, it) } +
            (PUBLIC_DECODER to QwenLabModelDownloader.decoder(context))
        for ((name, file) in assets) {
            QwenLabModelDownloader.ensureActive(cancelled)
            require(file.isFile) { "Missing private model component: $name" }
            val expected = digest(file)
            if (matchingOwnedPublicFile(context, name, file.length(), expected, cancelled)) {
                status("Saved copy already verified: $name")
                continue
            }
            status("Saving uninstall-resistant copy: $name")
            publishNew(context, name, file, expected, cancelled)
        }
        status("Models saved to Internal storage/$RELATIVE_DIR")
    }

    /** Only read files we can open; after an uninstall old entries may not be app-owned. */
    private fun matchingOwnedPublicFile(
        context: Context, name: String, bytes: Long, hash: String, cancelled: () -> Boolean
    ): Boolean {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val existing = mutableListOf<Uri>()
        // A prior installation may own files that the freshly installed app cannot
        // enumerate. SAF restore remains available; never let a query denial block backup.
        runCatching {
            resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(RELATIVE_DIR, name), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    existing += ContentUris.withAppendedId(collection, cursor.getLong(0))
                }
            }
        }
        for (uri in existing) {
            QwenLabModelDownloader.ensureActive(cancelled)
            val good = runCatching {
                val md = MessageDigest.getInstance("SHA-256")
                var count = 0L
                resolver.openInputStream(uri)?.buffered()?.use { input ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        QwenLabModelDownloader.ensureActive(cancelled)
                        val n = input.read(buffer)
                        if (n < 0) break
                        count += n
                        if (count > bytes) return@use
                        md.update(buffer, 0, n)
                    }
                } ?: return@runCatching false
                count == bytes && md.digest().joinToString("") { "%02x".format(it) }
                    .equals(hash, ignoreCase = true)
            }.getOrDefault(false)
            if (good) return true
        }
        return false
    }

    private fun publishNew(
        context: Context, name: String, source: File, expectedSha: String,
        cancelled: () -> Boolean
    ) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.MediaColumns.MIME_TYPE,
                if (name.endsWith(".json")) "application/json" else "application/octet-stream")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
        ) ?: error("Cannot create model backup in Downloads")
        var published = false
        try {
            val md = MessageDigest.getInstance("SHA-256")
            var written = 0L
            source.inputStream().buffered().use { input ->
                requireNotNull(resolver.openOutputStream(uri, "w")) {
                    "Cannot open model backup destination"
                }.use { out ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        QwenLabModelDownloader.ensureActive(cancelled)
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        written += n
                        md.update(buffer, 0, n)
                    }
                    out.flush()
                }
            }
            QwenLabModelDownloader.ensureActive(cancelled)
            require(written == source.length() &&
                md.digest().joinToString("") { "%02x".format(it) }.equals(expectedSha, true)) {
                "Saved model copy failed checksum verification"
            }
            val commit = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, commit, null, null) == 1) {
                "Cannot publish verified model backup"
            }
            published = true
        } finally {
            // Only remove this specific pending entry if THIS attempted copy failed.
            if (!published) runCatching { resolver.delete(uri, null, null) }
        }
    }

    /**
     * Restore a saved archive after reinstall by explicit SAF folder selection.
     * QNN is validated by its manifest and decoder by the pinned official SHA-256.
     */
    fun restore(
        context: Context, folderUri: Uri, cancelled: () -> Boolean, status: (String) -> Unit
    ) {
        val folder = requireNotNull(DocumentFile.fromTreeUri(context, folderUri)) {
            "Select the Models folder in Downloads"
        }
        require(folder.isDirectory) { "The selected item is not a folder" }
        val files = folder.listFiles().filter { it.isFile }.associateBy { it.name }
        require((ENCODER_FILES + PUBLIC_DECODER).all { files[it] != null }) {
            "Choose Download/SFSU/Whisperapp/Models (all six files required)"
        }
        QwenLabModelDownloader.ensureActive(cancelled)
        status("Restoring and verifying QNN encoder from saved Downloads files...")
        // This function stages the entire encoder, verifies hashes, then swaps only private files.
        QwenLabEncoderAssets.importFolder(context, folderUri)
        QwenLabModelDownloader.ensureActive(cancelled)
        status("Restoring and verifying official Q8 decoder from Downloads...")
        val target = QwenLabModelDownloader.decoder(context)
        if (!QwenLabModelDownloader.officialDecoderPresent(context)) {
            require(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
            val pending = File(target.parentFile, "decoder-q8.gguf.restore-part")
            require(!pending.exists() || pending.delete())
            try {
                val doc = requireNotNull(files[PUBLIC_DECODER])
                val md = MessageDigest.getInstance("SHA-256")
                var count = 0L
                requireNotNull(context.contentResolver.openInputStream(doc.uri)).buffered().use { input ->
                    FileOutputStream(pending).use { out ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            QwenLabModelDownloader.ensureActive(cancelled)
                            val n = input.read(buffer)
                            if (n < 0) break
                            count += n
                            require(count <= QwenLabModelDownloader.DECODER_BYTES) {
                                "Saved Q8 file exceeds verified model size"
                            }
                            out.write(buffer, 0, n)
                            md.update(buffer, 0, n)
                        }
                        out.fd.sync()
                    }
                }
                QwenLabModelDownloader.ensureActive(cancelled)
                require(count == QwenLabModelDownloader.DECODER_BYTES &&
                    md.digest().joinToString("") { "%02x".format(it) }.equals(
                        QwenLabModelDownloader.DECODER_SHA256, true)) {
                    "Saved Q8 file does not match the verified official decoder"
                }
                require(pending.renameTo(target)) { "Cannot activate restored private Q8 model" }
            } finally {
                if (pending.exists()) pending.delete()
            }
        }
        require(QwenLabEncoderAssets.valid(context) &&
            QwenLabModelDownloader.officialDecoderPresent(context)) {
            "The restored model files did not pass final verification"
        }
        status("Models restored and verified; no Hugging Face download required.")
    }
}
