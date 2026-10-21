package com.example.whisperrecorder.qwen

import org.junit.Assert.assertEquals
import org.junit.Test

class QwenLabTranscriptStitcherTest {
    @Test fun overlappingWindowsDeduplicateExactBoundaryPhrases() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "Start broad spectrum antibiotics for seventy two hours",
            "seventy two hours until the blood cultures return"
        ), 1)
        assertEquals("Start broad spectrum antibiotics for seventy two hours until the blood cultures return", result.transcript)
        assertEquals(3, result.removedDuplicateWords)
        assertEquals(1, result.stitchedBoundaries)
    }

    @Test fun canDisableOverlapWithoutChangingBaselineTranscript() {
        val result = QwenLabTranscriptStitcher.stitch(listOf("Hello hello", "hello again"), 0)
        assertEquals("Hello hello hello again", result.transcript)
        assertEquals(0, result.removedDuplicateWords)
        assertEquals(0, result.repetitionCopiesRemoved)
    }

    @Test fun oneWordMatchIsNotSilentlyRemoved() {
        val result = QwenLabTranscriptStitcher.stitch(listOf("This is the plan", "plan for antibiotics"), 1)
        assertEquals("This is the plan plan for antibiotics", result.transcript)
        assertEquals(1, result.unmatchedBoundaries)
    }

    @Test fun punctuationAndCaseMayVaryButMedicalTermsMustMatch() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "Start Vancomycin and Meropenem.",
            "AND meropenem, pending cultures"
        ), 1)
        assertEquals("Start Vancomycin and Meropenem. pending cultures", result.transcript)
        val guarded = QwenLabTranscriptStitcher.stitch(listOf(
            "MRSA treated with meropenem", "treated with mirapenem until cultures"
        ), 1)
        assertEquals("MRSA treated with meropenem treated with mirapenem until cultures", guarded.transcript)
    }

    @Test fun twoShortWordsAreNotEnoughEvidence() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "Please start it now", "it now after drawing cultures"
        ), 1)
        assertEquals("Please start it now it now after drawing cultures", result.transcript)
        assertEquals(0, result.removedDuplicateWords)
    }

    @Test fun twoLongWordsCanBeDeduplicated() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "We discussed broad antibiotics", "broad antibiotics after cultures"
        ), 1)
        assertEquals("We discussed broad antibiotics after cultures", result.transcript)
        assertEquals(2, result.removedDuplicateWords)
    }

    @Test fun supportsTwentyFourWordBoundaryMatches() {
        val overlap = (1..24).joinToString(" ") { "word$it" }
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "A discussion about $overlap", "$overlap then a new point"
        ), 1)
        assertEquals("A discussion about $overlap then a new point", result.transcript)
        assertEquals(24, result.removedDuplicateWords)
    }

    @Test fun repeatedPhraseWithinNewContextIsNotRemovedIfNotAtBoundary() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "The plan includes antibiotics and cultures today",
            "The plan includes antibiotics but check your blood cultures"
        ), 1)
        assertEquals(0, result.stitchedBoundaries)
    }

    @Test fun overlappingBoundaryCanConsumeWholeRightSegment() {
        val result = QwenLabTranscriptStitcher.stitch(listOf("Take blood cultures now", "blood cultures now"), 1)
        assertEquals("Take blood cultures now", result.transcript)
        assertEquals(3, result.removedDuplicateWords)
    }

    @Test fun ordinarySpokenRepetitionsArePreserved() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "Oxygen. Oxygen, respiratory treatments.",
            "Now discuss infection prevention."
        ), 1)
        assertEquals("Oxygen. Oxygen, respiratory treatments. Now discuss infection prevention.", result.transcript)
        assertEquals(0, result.repetitionCopiesRemoved)
    }

    @Test fun productionCleanupRequiresThreeSubstantialRepeatedPhrases() {
        val loop = "Start antibiotics and draw cultures."
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "$loop $loop $loop", "Then review results and adjust therapy."
        ), 1)
        assertEquals("Start antibiotics and draw cultures. Then review results and adjust therapy.", result.transcript)
        assertEquals(2, result.repetitionCopiesRemoved)
    }

    @Test fun repetitionOfTwoSentencesIsNotChanged() {
        val result = QwenLabTranscriptStitcher.stitch(listOf(
            "Start antibiotics and draw cultures. Start antibiotics and draw cultures.",
            "Then review the results."
        ), 1)
        assertEquals("Start antibiotics and draw cultures. Start antibiotics and draw cultures. Then review the results.", result.transcript)
    }

    @Test fun rejectsUnqualifiedOverlapModes() {
        try {
            QwenLabTranscriptStitcher.stitch(listOf("one", "two"), 2)
            throw AssertionError("expected error")
        } catch (_: IllegalArgumentException) { }
    }
}
