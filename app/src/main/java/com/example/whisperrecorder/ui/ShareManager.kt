package com.example.whisperrecorder.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.whisperrecorder.data.RecordingItem
import java.io.File
import java.util.ArrayList

object ShareManager {
    private val INVALID_FILENAME_CHARS = Regex("[\\/:*?\"<>|]")

    /**
     * Shares the transcript as a real .txt attachment. We first copy the saved transcript into the
     * app's cache and expose that copy through FileProvider. This avoids receiver-specific problems
     * with Storage Access Framework document URIs while preserving reliable URI permission grants.
     */
    fun shareTranscriptFile(context: Context, title: String, transcriptUri: Uri) {
        val safeBase = title
            .replace(INVALID_FILENAME_CHARS, "_")
            .trim()
            .ifBlank { "Transcript" }
            .take(120)
        val shareDir = File(context.cacheDir, "shared_transcripts").apply { mkdirs() }
        val cached = File(shareDir, "$safeBase.txt")

        requireNotNull(context.contentResolver.openInputStream(transcriptUri)) {
            "Transcript file is not readable"
        }.use { input ->
            cached.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
        require(cached.isFile && cached.length() > 0L) { "Transcript file is empty" }

        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cached)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, cached.name)
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData.newUri(context.contentResolver, cached.name, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share transcript"))
    }

    fun share(context: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "Shared recording", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    fun shareMany(context: Context, items: List<RecordingItem>) {
        val pairs = items.mapNotNull { item -> (item.mediaUri ?: item.transcriptUri)?.let { Uri.parse(it) to item } }
        if (pairs.isEmpty()) return
        if (pairs.size == 1) {
            val (uri, item) = pairs.first()
            share(context, uri, if (item.mediaUri != null) item.mime else "text/plain")
            return
        }
        val uris = ArrayList(pairs.map { it.first })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = ClipData.newUri(context.contentResolver, "Shared recordings", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share selected recordings"))
    }
}
