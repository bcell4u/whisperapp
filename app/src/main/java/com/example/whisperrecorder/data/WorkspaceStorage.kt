package com.example.whisperrecorder.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.concurrent.ConcurrentHashMap

/**
 * Canonical user-visible storage layout beneath the directory selected through SAF.
 *
 * The selected tree is treated as a parent only. Whisper Recorder owns a single `Whisperapp`
 * directory beneath it so recordings, transcripts, persistent model archives, and logs
 * never spill into the user's chosen directory root. Prompt backups continue to use the system
 * document picker and are therefore not assigned a managed workspace directory.
 */
object WorkspaceStorage {
    const val ROOT_DIR = "Whisperapp"
    const val RECORDINGS_DIR = "Recordings"
    const val TRANSCRIPTS_DIR = "Transcripts"
    const val MODELS_DIR = "Models"
    const val LOGS_DIR = "Logs"

    data class Layout(
        val treeUri: Uri,
        val rootUri: Uri,
        val recordingsUri: Uri,
        val transcriptsUri: Uri,
        val modelsUri: Uri,
        val logsUri: Uri
    )

    private val cache = ConcurrentHashMap<String, Layout>()
    private val lock = Any()

    fun ensure(context: Context, treeUri: Uri): Layout {
        val key = treeUri.toString()
        cache[key]?.let { return it }
        synchronized(lock) {
            cache[key]?.let { return it }
            val root = SafStorage.getOrCreateDirectory(context, treeUri, ROOT_DIR)
            val layout = Layout(
                treeUri = treeUri,
                rootUri = root,
                recordingsUri = SafStorage.getOrCreateChildDirectory(context, treeUri, root, RECORDINGS_DIR),
                transcriptsUri = SafStorage.getOrCreateChildDirectory(context, treeUri, root, TRANSCRIPTS_DIR),
                modelsUri = SafStorage.getOrCreateChildDirectory(context, treeUri, root, MODELS_DIR),
                logsUri = SafStorage.getOrCreateChildDirectory(context, treeUri, root, LOGS_DIR)
            )
            cache[key] = layout
            return layout
        }
    }

    /** Revalidate the workspace with one root lookup and one child inventory. */
    fun refresh(context: Context, treeUri: Uri): Layout {
        val key = treeUri.toString()
        synchronized(lock) {
            val root = SafStorage.findDirectory(context, treeUri, ROOT_DIR)
                ?: SafStorage.getOrCreateDirectory(context, treeUri, ROOT_DIR)
            val children = SafStorage.listDirectory(context, treeUri, root)
                .filter { it.mime == DocumentsContract.Document.MIME_TYPE_DIR }
                .associateBy { it.name }
            fun directory(name: String): Uri = children[name]?.uri
                ?: SafStorage.createDirectoryInDirectory(context, root, name)
            return Layout(
                treeUri = treeUri, rootUri = root,
                recordingsUri = directory(RECORDINGS_DIR),
                transcriptsUri = directory(TRANSCRIPTS_DIR),
                modelsUri = directory(MODELS_DIR),
                logsUri = directory(LOGS_DIR)
            ).also { cache[key] = it }
        }
    }

    fun recordingsDirectory(context: Context, treeUri: Uri): Uri = ensure(context, treeUri).recordingsUri
    fun transcriptsDirectory(context: Context, treeUri: Uri): Uri = ensure(context, treeUri).transcriptsUri
    fun modelsDirectory(context: Context, treeUri: Uri): Uri = ensure(context, treeUri).modelsUri
    fun logsDirectory(context: Context, treeUri: Uri): Uri = ensure(context, treeUri).logsUri
}
