package com.example.whisperrecorder.transcribe

import org.junit.Assert.*
import org.junit.Test

class Qwen3HybridPolicyTest {
    @Test fun qwenIsIndependentNeverAnAutomaticDefault() {
        assertEquals(AsrEngine.QWEN3_HYBRID, AsrEngine.fromId("qwen3_hybrid"))
        assertEquals("auto", AsrEngine.QWEN3_HYBRID.resolveLanguage("en"))
        assertEquals(AccelerationTier.NPU, AccelerationPolicy.tier(AsrEngine.QWEN3_HYBRID))
        assertNotEquals(AsrEngine.QWEN3_HYBRID, AccelerationPolicy.preferredAvailable(
            setOf(AsrEngine.QWEN3_HYBRID, AsrEngine.WHISPER)))
    }

    @Test fun qwenUsesOnlyProvenFixed12SecondTensor() {
        val plan = SegmentationPolicy.plan(AsrEngine.QWEN3_HYBRID, 90_000, 16_000, 1)
        assertEquals(12, plan.maxSegmentSeconds)
        assertEquals(1, plan.overlapSeconds)
        assertEquals("experimental-qwen3-hybrid-fixed12-overlap1", plan.strategy)
    }

    @Test fun parakeetAndOmiSegmentationRemainUnchanged() {
        assertEquals(40, SegmentationPolicy.plan(AsrEngine.PARAKEET_NPU, 90_000, 16_000, 1).maxSegmentSeconds)
        assertEquals(12, SegmentationPolicy.plan(AsrEngine.OMI_NPU, 90_000, 16_000, 1).maxSegmentSeconds)
    }
}
