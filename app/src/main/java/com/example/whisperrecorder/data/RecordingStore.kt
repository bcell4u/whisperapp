package com.example.whisperrecorder.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.whisperrecorder.media.MediaSourceKind
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import org.json.JSONArray
import org.json.JSONObject

data class RecordingItem(
    val id: String,
    val title: String,
    val audioUri: String?,
    val treeUri: String,
    val mime: String,
    val durationMs: Long,
    val createdAt: Long,
    val audioOwnedByApp: Boolean = true,
    val sourceKind: String = MediaSourceKind.AUDIO.id,
    val transcriptUri: String? = null,
    val transcriptTimingPath: String? = null,
    val transcriptSyncProfileId: String? = null,
    val transcriptSyncProfileLabel: String? = null,
    val transcriptionElapsedMs: Long? = null,
    val hasEverBeenTranscribed: Boolean = false,
    val speakerTranscriptUri: String? = null
) {
    val mediaUri: String? get() = audioUri
    val isVideo: Boolean get() = MediaSourceKind.fromId(sourceKind, mime) == MediaSourceKind.VIDEO
    val transcriptMissing: Boolean get() = hasEverBeenTranscribed && transcriptUri == null && mediaUri != null
}

object RecordingStore {
    private const val PREFS = "recording_store"
    private const val KEY = "items"

    data class ReconcileResult(
        val removedEntries: Int = 0,
        val missingMedia: Int = 0,
        val missingTranscripts: Int = 0
    ) {
        val changed: Boolean get() = removedEntries > 0 || missingMedia > 0 || missingTranscripts > 0
    }

    // The UI and transcription service query the store frequently. Cache the decoded
    // representation and invalidate it whenever the backing JSON changes so those reads
    // do not repeatedly parse/sort the same SharedPreferences payload.
    @Volatile private var initialized = false
    @Volatile private var cachedItems: List<RecordingItem> = emptyList()
    @Volatile private var cachedById: Map<String, RecordingItem> = emptyMap()

    @Synchronized
    fun list(context: Context): List<RecordingItem> {
        if (initialized) return cachedItems
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val decoded = (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val mime = o.getString("mime")
                val transcriptUri = o.optString("transcriptUri").takeIf { it.isNotBlank() }
                val transcriptTimingPath = o.optString("transcriptTimingPath").takeIf { it.isNotBlank() }
                val transcriptionElapsedMs = o.optLong("transcriptionElapsedMs", -1L).takeIf { it >= 0L }
                RecordingItem(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    audioUri = o.optString("audioUri").takeIf { it.isNotBlank() },
                    treeUri = o.getString("treeUri"),
                    mime = mime,
                    durationMs = o.getLong("durationMs"),
                    createdAt = o.getLong("createdAt"),
                    audioOwnedByApp = o.optBoolean("audioOwnedByApp", true),
                    sourceKind = o.optString("sourceKind").takeIf { it.isNotBlank() }
                        ?: MediaSourceKind.fromId(null, mime).id,
                    transcriptUri = transcriptUri,
                    speakerTranscriptUri = o.optString("speakerTranscriptUri").takeIf { it.isNotBlank() },
                    transcriptTimingPath = transcriptTimingPath,
                    transcriptSyncProfileId = o.optString("transcriptSyncProfileId").takeIf { it.isNotBlank() },
                    transcriptSyncProfileLabel = o.optString("transcriptSyncProfileLabel").takeIf { it.isNotBlank() },
                    transcriptionElapsedMs = transcriptionElapsedMs,
                    hasEverBeenTranscribed = if (o.has("hasEverBeenTranscribed")) {
                        o.optBoolean("hasEverBeenTranscribed", false)
                    } else {
                        transcriptUri != null || transcriptTimingPath != null || transcriptionElapsedMs != null
                    }
                )
            }.getOrNull()
        }.sortedByDescending { it.createdAt }
        cachedItems = decoded
        cachedById = decoded.associateBy(RecordingItem::id)
        initialized = true
        return decoded
    }

    @Synchronized
    fun find(context: Context, id: String): RecordingItem? {
        list(context)
        return cachedById[id]
    }

    fun hasMedia(context: Context, id: String): Boolean = find(context, id)?.mediaUri != null

    @Synchronized
    fun add(context: Context, item: RecordingItem) {
        val items = list(context).toMutableList().apply { add(0, item) }
        save(context, items)
    }

    @Synchronized
    fun setTranscript(
        context: Context,
        id: String,
        transcriptUri: String,
        transcriptTimingPath: String?,
        transcriptSyncProfileId: String?,
        transcriptSyncProfileLabel: String?,
        transcriptionElapsedMs: Long? = null
    ) {
        update(context, id) {
            it.copy(
                transcriptUri = transcriptUri,
                transcriptTimingPath = transcriptTimingPath,
                transcriptSyncProfileId = transcriptSyncProfileId,
                transcriptSyncProfileLabel = transcriptSyncProfileLabel,
                transcriptionElapsedMs = transcriptionElapsedMs ?: it.transcriptionElapsedMs,
                hasEverBeenTranscribed = true
            )
        }
    }

    @Synchronized
    fun setSpeakerTranscript(context: Context, id: String, uri: String) {
        update(context, id) { it.copy(speakerTranscriptUri = uri) }
    }

    @Synchronized
    fun rename(context: Context, id: String, title: String, audioUri: String? = null, transcriptUri: String? = null) {
        update(context, id) { current ->
            current.copy(
                title = title,
                audioUri = audioUri ?: current.audioUri,
                transcriptUri = transcriptUri ?: current.transcriptUri
            )
        }
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        save(context, list(context).filterNot { it.id == id })
    }

    /** Apply confirmed per-document deletions without dropping references to documents that remain. */
    @Synchronized
    fun applyDocumentDeletion(
        context: Context, snapshot: RecordingItem,
        clearMedia: Boolean, clearTranscript: Boolean, clearSpeaker: Boolean
    ): RecordingItem? {
        val current = find(context, snapshot.id) ?: return null
        val media = if (clearMedia && current.mediaUri == snapshot.mediaUri) null else current.mediaUri
        val transcript = if (clearTranscript && current.transcriptUri == snapshot.transcriptUri) null else current.transcriptUri
        val speaker = if (clearSpeaker && current.speakerTranscriptUri == snapshot.speakerTranscriptUri) null else current.speakerTranscriptUri
        if (media == null && transcript == null && speaker == null) {
            remove(context, snapshot.id)
            return null
        }
        val transcriptCleared = current.transcriptUri != null && transcript == null
        val updated = current.copy(
            audioUri = media, transcriptUri = transcript, speakerTranscriptUri = speaker,
            transcriptTimingPath = if (transcriptCleared) null else current.transcriptTimingPath,
            transcriptSyncProfileId = if (transcriptCleared) null else current.transcriptSyncProfileId,
            transcriptSyncProfileLabel = if (transcriptCleared) null else current.transcriptSyncProfileLabel,
            transcriptionElapsedMs = if (transcriptCleared) null else current.transcriptionElapsedMs
        )
        update(context, snapshot.id) { updated }
        return updated
    }

    /**
     * Reconciles persisted Library metadata with the documents that still exist.
     *
     * Availability probes run outside the store lock so a slow document provider cannot block
     * transcription metadata writes. Before applying a probe, the current URIs are compared with
     * the snapshot that was checked; a transcript/media URI changed concurrently is left untouched.
     */
    fun reconcileAvailability(context: Context): ReconcileResult {
        data class Availability(
            val mediaUri: String?,
            val mediaExists: Boolean,
            val transcriptUri: String?,
            val transcriptExists: Boolean,
            val speakerUri: String?,
            val speakerExists: Boolean
        )

        val snapshot = list(context)
        if (snapshot.isEmpty()) return ReconcileResult()

        data class WorkspaceFiles(val recordings: Set<String>, val transcripts: Set<String>)
        val activeTree = AppPrefs.getSaveTree(context)
        val workspaceFiles = if (activeTree != null && snapshot.any { it.treeUri == activeTree.toString() }) {
            mapOf(activeTree.toString() to runCatching {
                val layout = WorkspaceStorage.ensure(context, activeTree)
                WorkspaceFiles(
                    recordings = SafStorage.listDirectory(context, activeTree, layout.recordingsUri)
                        .asSequence().map { it.uri.toString() }.toSet(),
                    transcripts = SafStorage.listDirectory(context, activeTree, layout.transcriptsUri)
                        .asSequence().map { it.uri.toString() }.toSet()
                )
            }.getOrNull())
        } else emptyMap()

        fun exists(raw: String?, indexed: Set<String>?): Boolean {
            if (raw == null) return false
            if (indexed?.contains(raw) == true) return true
            return SafStorage.documentExists(context, Uri.parse(raw))
        }

        val availability = snapshot.associate { item ->
            val indexed = workspaceFiles[item.treeUri]
            item.id to Availability(
                mediaUri = item.mediaUri,
                mediaExists = exists(item.mediaUri, if (item.audioOwnedByApp) indexed?.recordings else null),
                transcriptUri = item.transcriptUri,
                transcriptExists = exists(item.transcriptUri, indexed?.transcripts),
                speakerUri = item.speakerTranscriptUri,
                speakerExists = exists(item.speakerTranscriptUri, indexed?.transcripts)
            )
        }

        var removed = 0
        var mediaMissing = 0
        var transcriptMissing = 0
        val abandonedExternalUris = mutableSetOf<String>()
        val obsoleteTimelinePaths = mutableSetOf<String>()
        val next = synchronized(this) {
            val latest = list(context)
            val reconciled = ArrayList<RecordingItem>(latest.size)
            latest.forEach { item ->
                val observed = availability[item.id]
                if (observed == null || observed.mediaUri != item.mediaUri || observed.transcriptUri != item.transcriptUri || observed.speakerUri != item.speakerTranscriptUri) {
                    reconciled += item
                    return@forEach
                }

                val hadTranscript = item.hasEverBeenTranscribed || item.transcriptUri != null
                val media = item.mediaUri?.takeIf { observed.mediaExists }
                val transcript = item.transcriptUri?.takeIf { observed.transcriptExists }
                val speaker = item.speakerTranscriptUri?.takeIf { observed.speakerExists }
                if (media == null && transcript == null && speaker == null) {
                    removed++
                    item.transcriptTimingPath?.let(obsoleteTimelinePaths::add)
                    if (!item.audioOwnedByApp) item.mediaUri?.let(abandonedExternalUris::add)
                } else {
                    if (media == null && item.mediaUri != null) {
                        mediaMissing++
                        if (!item.audioOwnedByApp) item.mediaUri?.let(abandonedExternalUris::add)
                    }
                    if (transcript == null && item.transcriptUri != null) {
                        transcriptMissing++
                        item.transcriptTimingPath?.let(obsoleteTimelinePaths::add)
                    }
                    val transcriptCleared = item.transcriptUri != null && transcript == null
                    reconciled += item.copy(
                        audioUri = media, transcriptUri = transcript, speakerTranscriptUri = speaker,
                        transcriptTimingPath = if (transcriptCleared) null else item.transcriptTimingPath,
                        transcriptSyncProfileId = if (transcriptCleared) null else item.transcriptSyncProfileId,
                        transcriptSyncProfileLabel = if (transcriptCleared) null else item.transcriptSyncProfileLabel,
                        transcriptionElapsedMs = if (transcriptCleared) null else item.transcriptionElapsedMs,
                        hasEverBeenTranscribed = hadTranscript
                    )
                }
            }
            val reconciledItems = reconciled.sortedByDescending { it.createdAt }
            if (reconciledItems != latest) save(context, reconciledItems)
            reconciledItems
        }

        obsoleteTimelinePaths.forEach { TranscriptTimelineStore.delete(it) }
        if (abandonedExternalUris.isNotEmpty()) {
            val stillReferenced = next.mapNotNull { it.mediaUri }.toHashSet()
            abandonedExternalUris.filterNot(stillReferenced::contains).forEach { raw ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(raw),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
        return ReconcileResult(removed, mediaMissing, transcriptMissing)
    }

    private fun update(context: Context, id: String, transform: (RecordingItem) -> RecordingItem) {
        save(context, list(context).map { if (it.id == id) transform(it) else it })
    }

    private fun save(context: Context, items: List<RecordingItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("audioUri", item.audioUri ?: "")
                put("treeUri", item.treeUri)
                put("mime", item.mime)
                put("durationMs", item.durationMs)
                put("createdAt", item.createdAt)
                put("audioOwnedByApp", item.audioOwnedByApp)
                put("sourceKind", item.sourceKind)
                put("transcriptUri", item.transcriptUri ?: "")
                put("speakerTranscriptUri", item.speakerTranscriptUri ?: "")
                put("transcriptTimingPath", item.transcriptTimingPath ?: "")
                put("transcriptSyncProfileId", item.transcriptSyncProfileId ?: "")
                put("transcriptSyncProfileLabel", item.transcriptSyncProfileLabel ?: "")
                put("transcriptionElapsedMs", item.transcriptionElapsedMs ?: -1L)
                put("hasEverBeenTranscribed", item.hasEverBeenTranscribed)
            })
        }
        val raw = arr.toString()
        // All RecordingStore writes flow through this object, so the process-local cache can serve
        // subsequent UI/service reads without another SharedPreferences lookup or JSON comparison.
        cachedItems = items.sortedByDescending { it.createdAt }
        cachedById = cachedItems.associateBy(RecordingItem::id)
        initialized = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, raw).apply()
    }
}
