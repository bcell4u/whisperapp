package com.example.whisperrecorder.speaker

import org.junit.Assert.*
import org.junit.Test

class SpeakerTranscriptTest {
    @Test fun alignsUsingMajorityOverlap() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(100, 900, "Hello"), SpeakerTextCue(1000, 2000, "There")),
            listOf(SpeakerInterval(0, 950, 0), SpeakerInterval(950, 2100, 1))
        )
        assertEquals(listOf(0, 1), result.map { it.speaker })
    }

    @Test fun unlabeledWhenNoSufficientCoverage() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(1000, 3000, "Hi")), listOf(SpeakerInterval(1000, 1200, 0))
        )
        assertNull(result.single().speaker)
    }


    @Test fun overlappingWindowsOfSameVoiceDoNotDoubleCount() {
        // Old code summed both A windows (2 * 800ms), outvoting B's genuine 900ms.
        // New code counts A's timeline coverage once and selects B.
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Hello")),
            listOf(SpeakerInterval(0, 800, 0), SpeakerInterval(0, 800, 0),
                SpeakerInterval(100, 1000, 1))
        )
        // Both voices cover most of this cue: the result is ambiguous, not A.
        assertNull(result.single().speaker)
    }

    @Test fun clearMajorityWithDuplicateWindowsKeepsOneSpeaker() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Hello")),
            listOf(SpeakerInterval(0, 850, 0), SpeakerInterval(50, 800, 0),
                SpeakerInterval(850, 1000, 1))
        )
        assertEquals(0, result.single().speaker)
    }

    @Test fun strongCompetingSpeakersAreMarkedUnknownInsteadOfGuessed() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Yes, but...")),
            listOf(SpeakerInterval(0, 520, 0), SpeakerInterval(520, 1000, 1))
        )
        assertNull(result.single().speaker)
    }

    @Test fun confidenceOnlyBreaksRealCompetition() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Hello")),
            listOf(SpeakerInterval(0, 500, 0, -1.0f),
                SpeakerInterval(500, 1000, 1, 1.0f))
        )
        assertEquals(0, result.single().speaker) // first encountered winner renumbers to 0
        assertEquals(1, result.mapNotNull { it.speaker }.distinct().size)
    }

    @Test fun unavailableConfidencePreservesSingleSpeaker() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Hello")),
            listOf(SpeakerInterval(0, 850, 3, -2.0f))
        )
        assertEquals(0, result.single().speaker)
    }

    @Test fun tinySpeakerFragmentDoesNotLabelWholeCue() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1500, "Long utterance")),
            listOf(SpeakerInterval(0, 150, 0))
        )
        assertNull(result.single().speaker)
    }

    @Test fun briefPauseCanStillBeLabeledWhenSpeechEvidenceIsClear() {
        val result = SpeakerTranscript.align(
            listOf(SpeakerTextCue(0, 1000, "Hello")),
            listOf(SpeakerInterval(350, 800, 0))
        )
        assertEquals(0, result.single().speaker)
    }

    @Test fun timestampsDefaultOffAndCanBeEnabled() {
        val lines = listOf(SpeakerTextLine(0, 1234, "Hello"), SpeakerTextLine(1, 61000, "Hey"))
        val plain = SpeakerTranscript.render(lines, timestamps = false)
        assertFalse(plain.contains("[00:"))
        assertTrue(plain.contains("Speaker 1:\nHello"))
        val timed = SpeakerTranscript.render(lines, timestamps = true)
        assertTrue(timed.contains("[00:00:01] Hello"))
        assertTrue(timed.contains("[00:01:01] Hey"))
    }

    @Test fun speakerNamesCanBeProvidedByCaller() {
        val result = SpeakerTranscript.render(listOf(SpeakerTextLine(0, 0, "Hi")), false, mapOf(0 to "Doctor"))
        assertTrue(result.startsWith("Doctor:\nHi"))
    }

    @Test fun renamesOnlySpeakerHeadings() {
        val input = "Speaker 1:\nHi\n\nSpeaker 2:\nHello Speaker 1: body"
        val output = SpeakerTranscript.renameLabels(input, mapOf(0 to "Doctor", 1 to "Patient"))
        assertTrue(output.contains("Doctor:\nHi"))
        assertTrue(output.contains("Patient:\nHello Speaker 1: body"))
    }

    @Test fun shortTimedWordsCanBeAttributedToDifferentSpeakers() {
        val words = listOf(SpeakerTextCue(100, 180, "Yes"), SpeakerTextCue(200, 280, "but"))
        val intervals = listOf(SpeakerInterval(80, 190, 4), SpeakerInterval(190, 320, 2))
        val labeled = SpeakerTranscript.align(words, intervals)
        assertEquals(listOf(0, 1), labeled.map { it.speaker })
        val output = SpeakerTranscript.render(SpeakerTranscript.groupWords(labeled), false)
        assertTrue(output.contains("Speaker 1:\nYes"))
        assertTrue(output.contains("Speaker 2:\nbut"))
    }

    @Test fun wordGroupingPreservesSpeakerTransitions() {
        val words = listOf(SpeakerTextLine(0, 0, "We"), SpeakerTextLine(0, 150, "agree."),
            SpeakerTextLine(1, 300, "But"), SpeakerTextLine(1, 420, "no."))
        assertEquals(listOf("We agree.", "But no."), SpeakerTranscript.groupWords(words).map { it.text })
    }

    @Test fun mixedWordAndPhraseCuesGroupWithoutLosingSpeakerBoundaries() {
        val cues = listOf(
            SpeakerTextLine(0, 0, "I"), SpeakerTextLine(0, 160, "think"),
            SpeakerTextLine(0, 350, "we should"), SpeakerTextLine(0, 720, "begin."),
            SpeakerTextLine(1, 1100, "Yes"), SpeakerTextLine(1, 1300, "please."),
            SpeakerTextLine(0, 1900, "All right.")
        )
        val grouped = SpeakerTranscript.groupWords(cues)
        assertEquals(listOf("I think we should begin.", "Yes please.", "All right."), grouped.map { it.text })
        val text = SpeakerTranscript.render(grouped, false)
        assertTrue(text.contains("Speaker 1:\nI think we should begin."))
        assertTrue(text.contains("Speaker 2:\nYes please."))
        assertFalse(text.contains("I\nthink"))
        assertEquals(cues.size, 7) // source word/phrase cues were not mutated
    }

    @Test fun timestampedWordsStayInReadableGroups() {
        val words = listOf(SpeakerTextLine(0, 1000, "This"), SpeakerTextLine(0, 1150, "works."),
            SpeakerTextLine(0, 1500, "Another"), SpeakerTextLine(0, 1750, "sentence."))
        val output = SpeakerTranscript.render(SpeakerTranscript.groupWords(words), timestamps = true)
        assertTrue(output.contains("[00:00:01] This works."))
        assertTrue(output.contains("[00:00:01] Another sentence."))
        assertFalse(output.contains("This\n"))
    }

    @Test fun preservesSpeakerSuffixOnCollision() {
        assertEquals("meeting-speaker.txt", SpeakerTranscript.filename("meeting.txt"))
        assertEquals("meeting (2)-speaker.txt", SpeakerTranscript.filename("meeting.txt", 2))
    }
}
