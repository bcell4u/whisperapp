package com.example.whisperrecorder.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

enum class MediaSourceKind(val id: String) {
    AUDIO("audio"),
    VIDEO("video");

    companion object {
        fun fromId(value: String?, mime: String = ""): MediaSourceKind = when {
            value == VIDEO.id -> VIDEO
            value == AUDIO.id -> AUDIO
            mime.startsWith("video/") -> VIDEO
            else -> AUDIO
        }
    }
}

data class MediaSourceInfo(
    val displayName: String,
    val mime: String,
    val kind: MediaSourceKind,
    val durationMs: Long,
    val audioSampleRate: Int,
    val audioChannelCount: Int,
    val audioTrackMime: String
)

/**
 * Inspects a user-selected media URI without copying it. Audio and video imports share this
 * path; a video is accepted only when Android can expose a readable audio track for ASR.
 */
object MediaSourceInspector {
    fun inspect(context: Context, uri: Uri): MediaSourceInfo {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "Media_${System.currentTimeMillis()}"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val declaredMime = resolver.getType(uri)?.lowercase().orEmpty()

        val extractor = MediaExtractor()
        val pfd = resolver.openFileDescriptor(uri, "r") ?: error("Cannot open selected media")
        var audioFormat: MediaFormat? = null
        var hasVideoTrack = false
        try {
            extractor.setDataSource(pfd.fileDescriptor)
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                when {
                    trackMime.startsWith("audio/") && audioFormat == null -> audioFormat = format
                    trackMime.startsWith("video/") -> hasVideoTrack = true
                }
            }
        } catch (t: Throwable) {
            throw IllegalArgumentException("Android cannot read this media file: ${t.message ?: "unsupported format"}", t)
        } finally {
            extractor.release()
            pfd.close()
        }

        val audio = requireNotNull(audioFormat) { "The selected media has no readable audio track" }
        val sampleRate = audio.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
        val channels = audio.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
        require(sampleRate > 0 && channels > 0) { "The selected media has an invalid audio track" }
        val audioTrackMime = audio.getString(MediaFormat.KEY_MIME).orEmpty()

        val audioDurationMs = audio.getLongOrNull(MediaFormat.KEY_DURATION)?.div(1000L) ?: 0L
        var retrieverDurationMs = 0L
        var retrieverMime = ""
        if (hasVideoTrack || audioDurationMs <= 0L || declaredMime.isBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retrieverDurationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retrieverMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE).orEmpty().lowercase()
            } catch (_: Throwable) {
                // MediaExtractor already confirmed the audio track. Duration validation below
                // still prevents unusable sources from entering the library.
            } finally {
                retriever.release()
            }
        }

        val kind = if (hasVideoTrack || declaredMime.startsWith("video/") || retrieverMime.startsWith("video/")) {
            MediaSourceKind.VIDEO
        } else {
            MediaSourceKind.AUDIO
        }
        // Video playback uses the original container, so retain its full duration in Library.
        // ASR separately reads the audio-track duration from PcmDecoder.
        val durationMs = when {
            kind == MediaSourceKind.VIDEO && retrieverDurationMs > 0L -> retrieverDurationMs
            audioDurationMs > 0L -> audioDurationMs
            else -> retrieverDurationMs
        }
        require(durationMs > 0L) { "The selected media has no readable duration" }
        val mime = when {
            declaredMime.startsWith("audio/") || declaredMime.startsWith("video/") -> declaredMime
            retrieverMime.startsWith("audio/") || retrieverMime.startsWith("video/") -> retrieverMime
            else -> fallbackContainerMime(extension, kind)
        }

        return MediaSourceInfo(
            displayName = displayName,
            mime = mime,
            kind = kind,
            durationMs = durationMs,
            audioSampleRate = sampleRate,
            audioChannelCount = channels,
            audioTrackMime = audioTrackMime
        )
    }

    private fun fallbackContainerMime(extension: String, kind: MediaSourceKind): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg", "opus" -> "audio/ogg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "mp4", "m4v" -> if (kind == MediaSourceKind.VIDEO) "video/mp4" else "audio/mp4"
        "mov" -> "video/quicktime"
        "webm" -> if (kind == MediaSourceKind.VIDEO) "video/webm" else "audio/webm"
        "mkv" -> "video/x-matroska"
        "3gp", "3gpp" -> if (kind == MediaSourceKind.VIDEO) "video/3gpp" else "audio/3gpp"
        else -> if (kind == MediaSourceKind.VIDEO) "video/*" else "audio/*"
    }

    private fun MediaFormat.getIntegerOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun MediaFormat.getLongOrNull(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null
}
