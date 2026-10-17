package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Repository migration guard: incremental GitHub updates may retain the historical
 * Qwen3HybridPolicyTest.kt from v6.5.32. This clean diagnostic baseline intentionally
 * does not ship the experimental qwen3_hybrid backend. Do not reference that enum.
 *
 * Preserve and test the existing Qwen3 CPU backend, Parakeet and Omi instead.
 */
class Qwen3HybridPolicyTest {
    @Test fun qwenCpuBackendRemainsAvailable() {
        assertEquals(AsrEngine.QWEN3, AsrEngine.fromId("qwen3"))
        assertEquals("auto", AsrEngine.QWEN3.resolveLanguage("en"))
        assertEquals(AccelerationTier.CPU, AccelerationPolicy.tier(AsrEngine.QWEN3))
    }

    @Test fun existingNpuSegmentationPoliciesRemainUnchanged() {
        assertEquals(40, SegmentationPolicy.plan(AsrEngine.PARAKEET_NPU, 90_000, 16_000, 1).maxSegmentSeconds)
        assertEquals(12, SegmentationPolicy.plan(AsrEngine.OMI_NPU, 90_000, 16_000, 1).maxSegmentSeconds)
    }
}
