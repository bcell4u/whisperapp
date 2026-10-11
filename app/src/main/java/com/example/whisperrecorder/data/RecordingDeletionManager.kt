package com.example.whisperrecorder.data

import android.content.Context
import android.net.Uri
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import java.util.concurrent.ConcurrentHashMap

/** Delete each requested document independently; preserve references to every failed deletion. */
object RecordingDeletionManager {
    data class Result(val success: Boolean, val partial: Boolean = false, val message: String? = null)

    private val deletingIds = ConcurrentHashMap.newKeySet<String>()

    private fun log(context: Context, message: String) {
        runCatching { DiagnosticLogger.append(context, message) }
    }

    fun markDeleting(id: String) { deletingIds.add(id) }
    fun isDeleting(id: String): Boolean = deletingIds.contains(id)

    fun deleteRecording(context: Context, item: RecordingItem, deleteTranscript: Boolean): Result =
        delete(context, item, removeMedia = true, removeTranscripts = deleteTranscript)

    fun deleteTranscriptOnly(context: Context, item: RecordingItem): Result =
        delete(context, item, removeMedia = false, removeTranscripts = true)

    private fun delete(context: Context, item: RecordingItem, removeMedia: Boolean, removeTranscripts: Boolean): Result {
        deletingIds.add(item.id)
        try {
            fun attempt(uri: String?, label: String): Boolean {
                if (uri == null) return true
                val outcome = SafStorage.deleteRecordingDocument(context, Uri.parse(uri))
                log(context, "library delete id=${item.id} target=$label status=${outcome.status}")
                return outcome.removed
            }

            // Imported media is never deleted. Only its Library reference is detached.
            val mediaRemoved = !removeMedia || !item.audioOwnedByApp || attempt(item.mediaUri, "audio")
            // A missing/denied speaker file must never prevent deleting the regular transcript.
            val transcriptRemoved = !removeTranscripts || attempt(item.transcriptUri, "transcript")
            val speakerRemoved = !removeTranscripts || attempt(item.speakerTranscriptUri, "speaker")
            val decision = RecordingDeletionPolicy(
                hadMedia = item.mediaUri != null, hadTranscript = item.transcriptUri != null,
                hadSpeakerTranscript = item.speakerTranscriptUri != null,
                removeMedia = removeMedia, removeTranscripts = removeTranscripts,
                mediaRemoved = mediaRemoved, transcriptRemoved = transcriptRemoved, speakerRemoved = speakerRemoved
            )
            val latest = RecordingStore.applyDocumentDeletion(
                context, item,
                clearMedia = removeMedia && mediaRemoved,
                clearTranscript = removeTranscripts && transcriptRemoved,
                clearSpeaker = removeTranscripts && speakerRemoved
            )
            if (removeTranscripts && transcriptRemoved && item.transcriptUri != null && latest?.transcriptUri != item.transcriptUri) {
                TranscriptTimelineStore.delete(item.transcriptTimingPath)
            }
            if (removeTranscripts && speakerRemoved && item.speakerTranscriptUri != null && latest?.speakerTranscriptUri != item.speakerTranscriptUri) {
                TranscriptTimelineStore.delete(TranscriptTimelineStore.speakerPath(context, item.id))
            }
            if (removeMedia && mediaRemoved) releaseExternalMediaPermissionIfUnused(context, item)
            log(context,
                "library delete id=${item.id} result=${if (decision.failures.isEmpty()) "complete" else "incomplete"}" +
                    " partial=${decision.partial} failed=${decision.failures.joinToString(",").ifBlank { "none" }}" +
                    " retained=${if (latest == null) "none" else "library-entry"}")
            return if (decision.failures.isEmpty()) {
                Result(true, message = when {
                    latest != null -> "Remaining media or transcripts kept in Library."
                    removeMedia && !item.audioOwnedByApp -> "Removed from Library. External source media kept."
                    else -> null
                })
            } else {
                Result(false, partial = decision.partial,
                    message = "Could not delete ${decision.failures.joinToString(", ")}." +
                        " Remaining files are still listed in Library; retry deletion or check workspace access.")
            }
        } catch (e: Exception) {
            runCatching { DiagnosticLogger.appendError(context, "library-delete", e) }
            return Result(false, message = "Deletion interrupted: ${e.javaClass.simpleName}. Check diagnostics and retry remaining files.")
        } finally {
            deletingIds.remove(item.id)
        }
    }

    /** Removes only Library metadata. User-visible media/transcript documents are never deleted. */
    fun removeFromLibrary(context: Context, item: RecordingItem): Result {
        deletingIds.add(item.id)
        try {
            RecordingStore.remove(context, item.id)
            releaseExternalMediaPermissionIfUnused(context, item)
            log(context, "library remove-only id=${item.id} filesKept=true")
            return Result(true, message = "Removed from Library. Recording and transcript files were kept.")
        } finally {
            deletingIds.remove(item.id)
        }
    }

    private fun releaseExternalMediaPermissionIfUnused(context: Context, item: RecordingItem) {
        if (item.audioOwnedByApp) return
        val raw = item.mediaUri ?: return
        if (RecordingStore.list(context).any { it.mediaUri == raw }) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(raw), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
