package com.example.whisperrecorder.transcribe

/** Real backend capabilities; model selection must never silently switch model families. */
enum class AccelerationTier { NPU, GPU, CPU }

object AccelerationPolicy {
    fun tier(engine: AsrEngine): AccelerationTier = when (engine) {
        AsrEngine.PARAKEET_NPU, AsrEngine.OMI_NPU -> AccelerationTier.NPU
        AsrEngine.PARAKEET, AsrEngine.OMI -> AccelerationTier.GPU
        AsrEngine.QWEN3, AsrEngine.WHISPER -> AccelerationTier.CPU
    }

    /** First-run default only. An explicitly selected model is always kept. */
    fun preferredAvailable(available: Set<AsrEngine>): AsrEngine? =
        listOf(AsrEngine.PARAKEET_NPU, AsrEngine.PARAKEET, AsrEngine.OMI, AsrEngine.QWEN3, AsrEngine.WHISPER)
            .firstOrNull { it in available }
}
