package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccelerationPolicyTest {
    @Test fun classifiesRealAsrBackends() {
        assertEquals(AccelerationTier.NPU, AccelerationPolicy.tier(AsrEngine.PARAKEET_NPU))
        assertEquals(AccelerationTier.GPU, AccelerationPolicy.tier(AsrEngine.PARAKEET))
        assertEquals(AccelerationTier.GPU, AccelerationPolicy.tier(AsrEngine.OMI))
        assertEquals(AccelerationTier.NPU, AccelerationPolicy.tier(AsrEngine.QWEN06))
        assertEquals(AccelerationTier.CPU, AccelerationPolicy.tier(AsrEngine.WHISPER))
    }

    @Test fun prioritizesOnlyAvailableModels() {
        val all = AsrEngine.entries.toSet()
        assertEquals(AsrEngine.PARAKEET_NPU, AccelerationPolicy.preferredAvailable(all))
        assertEquals(AsrEngine.PARAKEET, AccelerationPolicy.preferredAvailable(all - AsrEngine.PARAKEET_NPU))
        assertEquals(AsrEngine.OMI, AccelerationPolicy.preferredAvailable(setOf(AsrEngine.OMI, AsrEngine.WHISPER)))
        assertEquals(AsrEngine.WHISPER, AccelerationPolicy.preferredAvailable(setOf(AsrEngine.WHISPER)))
        assertNull(AccelerationPolicy.preferredAvailable(setOf(AsrEngine.QWEN06)))
        assertNull(AccelerationPolicy.preferredAvailable(emptySet()))
        assertEquals(AsrEngine.WHISPER, AsrEngine.fromId("qwen3"))
    }
}
