package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmiNpuSegmentationTest {
    @Test fun newExperimentalEngineUsesTwelveSecondWindowsNotGpuForty() {
        val plan = SegmentationPolicy.plan(AsrEngine.OMI_NPU, 180_000L, 48000, 2)
        assertEquals(12, plan.segmentSeconds)
        assertEquals(1, plan.overlapSeconds)
        assertEquals("experimental-omi-npu-fixed12-overlap1", plan.strategy)
        assertEquals(AccelerationTier.NPU, AccelerationPolicy.tier(AsrEngine.OMI_NPU))
        assertTrue(AsrEngine.OMI_NPU in AsrEngine.entries)
    }
}
