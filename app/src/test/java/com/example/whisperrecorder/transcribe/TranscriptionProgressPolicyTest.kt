package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionProgressPolicyTest {
    @Test fun overlappingSegmentsNeverMoveProgressBackward() {
        val total = 220_534L
        var completed = 0L
        var priorPercent = 0
        for (chunk in 0 until 20) {
            val start = chunk * 11_000L
            val end = (start + 12_000L).coerceAtMost(total)
            val startProcessed = TranscriptionProgressPolicy.monotonicProcessed(completed, start, total)
            assertTrue(startProcessed >= completed)
            val before = TranscriptionProgressPolicy.activePercent(startProcessed, total)
            assertTrue(before >= priorPercent)
            completed = TranscriptionProgressPolicy.monotonicProcessed(startProcessed, end, total)
            val after = TranscriptionProgressPolicy.activePercent(completed, total)
            assertTrue(after >= before)
            priorPercent = after
        }
        assertEquals(total, completed)
        assertEquals(99, priorPercent)
    }

    @Test fun clampsMissingAndCorruptDurationWithoutClaimingCompletion() {
        assertEquals(0L, TranscriptionProgressPolicy.monotonicProcessed(0L, 500L, 0L))
        assertEquals(0, TranscriptionProgressPolicy.activePercent(500L, 0L))
        assertEquals(0L, TranscriptionProgressPolicy.monotonicProcessed(-1L, -1L, 100L))
        assertEquals(100L, TranscriptionProgressPolicy.monotonicProcessed(50L, 500L, 100L))
        assertEquals(99, TranscriptionProgressPolicy.activePercent(100L, 100L))
    }
}
