package com.example.whisperrecorder.data

import org.junit.Assert.*
import org.junit.Test

class RecordingDeletionPolicyTest {
    private fun policy(
        media: Boolean = true, transcript: Boolean = true, speaker: Boolean = true,
        removeMedia: Boolean = true, removeTranscripts: Boolean = true,
        deletedMedia: Boolean = true, deletedTranscript: Boolean = true, deletedSpeaker: Boolean = true
    ) = RecordingDeletionPolicy(media, transcript, speaker, removeMedia, removeTranscripts,
        deletedMedia, deletedTranscript, deletedSpeaker)

    @Test fun successfulMultiDeleteRemovesAllReferences() {
        val result = policy()
        assertFalse(result.entryRemaining)
        assertTrue(result.failures.isEmpty())
        assertFalse(result.partial)
    }

    @Test fun failedRegularTranscriptRemainsReachableAfterAudioDelete() {
        val result = policy(deletedTranscript = false)
        assertFalse(result.mediaRemaining)
        assertTrue(result.transcriptRemaining)
        assertFalse(result.speakerRemaining)
        assertEquals(listOf("transcript"), result.failures)
        assertTrue(result.partial)
    }

    @Test fun failedSpeakerDeleteDoesNotBlockRegularTranscript() {
        val result = policy(deletedSpeaker = false)
        assertFalse(result.mediaRemaining)
        assertFalse(result.transcriptRemaining)
        assertTrue(result.speakerRemaining)
        assertEquals(listOf("speaker transcript"), result.failures)
    }

    @Test fun previouslyPartialEntryCanDeleteRegularTranscriptIndependently() {
        val result = policy(media = false, deletedSpeaker = false)
        assertFalse(result.transcriptRemaining)
        assertTrue(result.speakerRemaining)
        assertEquals(listOf("speaker transcript"), result.failures)
    }

    @Test fun orphanedSpeakerOnlyEntryIsNotDiscarded() {
        val result = policy(media = false, transcript = false, speaker = true, deletedSpeaker = false)
        assertTrue(result.entryRemaining)
        assertEquals(listOf("speaker transcript"), result.failures)
    }

    @Test fun speakerOnlyRetryRemovesLastDocument() {
        val result = policy(media = false, transcript = false, speaker = true)
        assertFalse(result.entryRemaining)
    }

    @Test fun keepTranscriptsLeavesBothKindsOfTranscriptInLibrary() {
        val result = policy(removeTranscripts = false)
        assertFalse(result.mediaRemaining)
        assertTrue(result.transcriptRemaining)
        assertTrue(result.speakerRemaining)
        assertTrue(result.failures.isEmpty())
    }

    @Test fun mediaFailureDoesNotPreventDeletingTwoTranscripts() {
        val result = policy(deletedMedia = false)
        assertTrue(result.mediaRemaining)
        assertFalse(result.transcriptRemaining)
        assertFalse(result.speakerRemaining)
        assertEquals(listOf("audio"), result.failures)
    }

    @Test fun allFailuresAreReportedNotMaskedAsSuccess() {
        val result = policy(deletedMedia = false, deletedTranscript = false, deletedSpeaker = false)
        assertTrue(result.entryRemaining)
        assertEquals(3, result.failures.size)
        assertFalse(result.partial)
    }

    @Test fun transcriptOnlyDeletionDoesNotTouchExistingAudio() {
        val result = policy(removeMedia = false)
        assertTrue(result.mediaRemaining)
        assertFalse(result.transcriptRemaining)
        assertFalse(result.speakerRemaining)
    }
}
