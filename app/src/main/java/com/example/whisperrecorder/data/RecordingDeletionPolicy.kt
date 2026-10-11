package com.example.whisperrecorder.data

/** Pure result calculation shared by single and multi-select deletion. */
internal data class RecordingDeletionPolicy(
    val hadMedia: Boolean,
    val hadTranscript: Boolean,
    val hadSpeakerTranscript: Boolean,
    val removeMedia: Boolean,
    val removeTranscripts: Boolean,
    val mediaRemoved: Boolean,
    val transcriptRemoved: Boolean,
    val speakerRemoved: Boolean
) {
    val mediaRemaining: Boolean get() = hadMedia && !(removeMedia && mediaRemoved)
    val transcriptRemaining: Boolean get() = hadTranscript && !(removeTranscripts && transcriptRemoved)
    val speakerRemaining: Boolean get() = hadSpeakerTranscript && !(removeTranscripts && speakerRemoved)
    val entryRemaining: Boolean get() = mediaRemaining || transcriptRemaining || speakerRemaining
    val failures: List<String> get() = buildList {
        if (hadMedia && removeMedia && !mediaRemoved) add("audio")
        if (hadTranscript && removeTranscripts && !transcriptRemoved) add("transcript")
        if (hadSpeakerTranscript && removeTranscripts && !speakerRemoved) add("speaker transcript")
    }
    val partial: Boolean get() = failures.isNotEmpty() && (
        (hadMedia && removeMedia && mediaRemoved) ||
        (hadTranscript && removeTranscripts && transcriptRemoved) ||
        (hadSpeakerTranscript && removeTranscripts && speakerRemoved)
    )
}
