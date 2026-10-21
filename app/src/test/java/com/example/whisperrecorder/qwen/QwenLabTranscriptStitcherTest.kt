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

    @Test fun rejectsUnqualifiedOverlapModes() {
        try {
            QwenLabTranscriptStitcher.stitch(listOf("one", "two"), 2)
            throw AssertionError("expected error")
        } catch (_: IllegalArgumentException) { }
    }
}
