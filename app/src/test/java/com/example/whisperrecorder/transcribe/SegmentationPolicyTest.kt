package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentationPolicyTest {
    @Test
    fun parakeetUsesMeasuredOpenClProductionSegments() {
        val plan = SegmentationPolicy.plan(
            engine = AsrEngine.PARAKEET,
            totalDurationMs = 185_000,
            sourceSampleRate = 48_000,
            whisperChunkMinutes = 2
        )
        assertEquals(40, plan.segmentSeconds)
        assertEquals(2, plan.overlapSeconds)
        assertEquals("production-opencl-fixed40", plan.strategy)
    }


    @Test
    fun parakeetNpuUsesSameFortySecondOverlapContract() {
        val plan = SegmentationPolicy.plan(
            engine = AsrEngine.PARAKEET_NPU,
            totalDurationMs = 185_000,
            sourceSampleRate = 48_000,
            whisperChunkMinutes = 2
        )
        assertEquals(40, plan.segmentSeconds)
        assertEquals(2, plan.overlapSeconds)
        assertEquals("production-npu-fixed40", plan.strategy)
    }

    @Test
    fun omiUsesSharedOpenClProductionSegments() {
        val plan = SegmentationPolicy.plan(
            engine = AsrEngine.OMI,
            totalDurationMs = 185_000,
            sourceSampleRate = 48_000,
            whisperChunkMinutes = 2
        )
        assertEquals(40, plan.segmentSeconds)
        assertEquals(2, plan.overlapSeconds)
        assertEquals("production-opencl-fixed40", plan.strategy)
    }

    @Test
    fun whisperKeepsZeroOverlap() {
        val plan = SegmentationPolicy.plan(
            engine = AsrEngine.WHISPER,
            totalDurationMs = 610_000,
            sourceSampleRate = 48_000,
            whisperChunkMinutes = 2
        )
        assertEquals(0, plan.overlapSeconds)
        assertTrue(plan.segmentSeconds <= 120)
    }
}
