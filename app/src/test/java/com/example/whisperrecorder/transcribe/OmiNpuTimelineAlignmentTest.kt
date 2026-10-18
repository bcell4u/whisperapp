package com.example.whisperrecorder.transcribe

import org.junit.Assert.*
import org.junit.Test

class OmiNpuTimelineAlignmentTest {
    @Test fun alignRawSuffixBeforeOverlapTimeClipping() {
        // The removed prefix falls outside the non-overlap window. Older code clipped first,
        // accidentally erasing a required alignment token before it could find the suffix.
        val words = listOf(
            TimedTextSegment(120, 220, "start"),
            TimedTextSegment(360, 530, "for"),
            TimedTextSegment(630, 760, "72"),
            TimedTextSegment(800, 930, "hours")
        )
        val result = TranscriptTimelineBuilder.cuesForChunkDetailed("for 72 hours", 11_000, 12_000,
            nativeSegments = words, wordByWord = true, minimumStartMs = 11_600)
        assertEquals("native-partial-overlap", result.mode)
        assertEquals("prefix-removed", result.reason)
        assertEquals(listOf("72", "hours"), result.cues.map { it.text })
        assertEquals(4, result.nativeProvided)
        assertEquals(3, result.nativeAligned)
        assertEquals(1, result.overlapDiscarded)
        assertEquals(1, result.droppedForCleanup)
    }

    @Test fun preserveWordTimingForInteriorRepetitionRemoval() {
        val words = listOf("for", "72", "hours", "72", "hours", "vancomycin")
            .mapIndexed { i, text -> TimedTextSegment((i * 100).toLong(), (i * 100 + 90).toLong(), text) }
        val result = TranscriptTimelineBuilder.cuesForChunkDetailed(
            "for 72 hours vancomycin", 0, 1200, words, wordByWord = true)
        assertEquals("native", result.mode)
        assertEquals("exact-token-subsequence", result.reason)
        assertEquals(listOf("for", "72", "hours", "vancomycin"), result.cues.map { it.text })
        assertEquals(2, result.droppedForCleanup)
    }

    @Test fun doNotForceMismatchForClinicallyImportantUnit() {
        val words = listOf(TimedTextSegment(0, 100, "72"), TimedTextSegment(120, 300, "dollars"))
        val result = TranscriptTimelineBuilder.cuesForChunkDetailed(
            "72 hours", 0, 2000, words, wordByWord = true)
        assertEquals("estimated-uniform", result.mode)
        assertTrue(result.reason.startsWith("text-token-mismatch"))
        assertEquals(0, result.nativeAligned)
    }

    @Test fun distinguishPartialOverlapFromMissingTimestamps() {
        val words = listOf(TimedTextSegment(0, 120, "the"), TimedTextSegment(500, 700, "lecture"))
        val result = TranscriptTimelineBuilder.cuesForChunkDetailed(
            "the lecture", 0, 1000, words, wordByWord = true, minimumStartMs = 200)
        assertEquals("native-partial-overlap", result.mode)
        assertEquals("exact", result.reason)
        assertEquals(listOf("lecture"), result.cues.map { it.text })
    }
}
