package com.example.whisperrecorder.transcribe

import org.junit.Assert.*
import org.junit.Test

class SpeakerWordSyncTest {
    private val speakerText = "Speaker 1:\n[00:00:01] Hello there, how are you?\n\nSpeaker 2:\nI'm very well, thank you. Goodbye.\n"
    private val words = listOf("Hello", "there,", "how", "are", "you?", "I'm", "very", "well,", "thank", "you.", "Goodbye.")
        .mapIndexed { i, word -> TranscriptCue(1000L + i * 200L, 1120L + i * 200L, word) }

    @Test fun replacesStaleSpeakerPhrasesWithMatchingRegularWordTiming() {
        val old = listOf(TranscriptCue(1000, 2000, "Hello there, how are you?"),
            TranscriptCue(2200, 3600, "I'm very well, thank you. Goodbye."))
        val choice = SpeakerWordSync.choose(speakerText, old, words)
        assertTrue(choice.useRegular)
        assertEquals(11, choice.regularWordMapped)
        assertEquals(2, choice.speakerMapped)
        assertEquals(11, choice.textWords)
    }

    @Test fun keepsSpeakerWordTimingWhenAlreadyCorrect() {
        assertFalse(SpeakerWordSync.choose(speakerText, words, words).useRegular)
    }

    @Test fun rejectsMismatchedRegularTranscription() {
        val unrelated = (0..12).map { i -> TranscriptCue(1000L + i * 200, 1100L + i * 200, "other") }
        assertFalse(SpeakerWordSync.choose(speakerText, emptyList(), unrelated).useRegular)
    }

    @Test fun ignoresRenamedSpeakerHeadingsAndTimestampDigits() {
        val renamed = speakerText.replace("Speaker 1:", "Doctor:").replace("Speaker 2:", "Patient:")
        assertEquals(11, TranscriptCueMapper.playableWordCount(renamed))
        assertEquals(11, TranscriptCueMapper.map(renamed, words, skipSpeakerHeadings = true).size)
    }
}
