package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test

class QwenDecoderRunConfigTest {
    @Test fun configRoundTripAndVersionedHandshake() {
        for (threads in listOf(2, 4)) for (arenaOff in listOf(false, true))
            for (prepackOff in listOf(false, true)) {
            val config = QwenDecoderRunConfig(threads, false, arenaOff, prepackOff)
            assertEquals(config, QwenDecoderRunConfig.fromJson(config.toJson()))
        }
    }

    @Test fun rejectsUnsupportedParametersBeforeWorkerAllocation() {
        for (invalid in listOf(0, 1, 3, 6, 8)) {
            try { QwenDecoderRunConfig(invalid, false, false, false); fail("invalid accepted") }
            catch (_: IllegalArgumentException) { }
        }
        try { QwenDecoderRunConfig.fromJson("{\"schema\":1,\"threads\":4,\"fp16_embedding_trial\":false,\"cpu_arena_disabled\":false,\"prepacking_disabled\":false}"); fail("invalid schema accepted") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun doesNotPermitMissingFieldsOrTextBooleans() {
        for (payload in listOf(
            "{\"schema\":2,\"threads\":4,\"fp16_embedding_trial\":false,\"cpu_arena_disabled\":false}",
            "{\"schema\":2,\"threads\":4,\"fp16_embedding_trial\":\"false\",\"cpu_arena_disabled\":false,\"prepacking_disabled\":false}",
            "{\"schema\":2,\"threads\":4,\"fp16_embedding_trial\":false,\"cpu_arena_disabled\":false,\"prepacking_disabled\":\"true\"}"
        )) {
            try { QwenDecoderRunConfig.fromJson(payload); fail("invalid config accepted") }
            catch (_: Exception) { }
        }
    }
}
