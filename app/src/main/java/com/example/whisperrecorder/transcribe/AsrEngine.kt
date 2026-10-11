package com.example.whisperrecorder.transcribe

/** Single source of truth for supported transcription engines. */
enum class AsrEngine(val id: String) {
    WHISPER("whisper"),
    PARAKEET("parakeet"),
    PARAKEET_NPU("parakeet_npu"),
    OMI_NPU("omi_npu"),
    OMI("omi"),
    QWEN3("qwen3"),
    QWEN3_HYBRID("qwen3_hybrid");

    val usesBoundaryOverlap: Boolean get() = this != WHISPER
    val supportsConfigurableThreads: Boolean get() = this != OMI && this != PARAKEET && this != PARAKEET_NPU && this != OMI_NPU && this != QWEN3_HYBRID

    fun resolveLanguage(requestedLanguage: String, whisperEnglishOnly: Boolean = false): String = when (this) {
        PARAKEET, PARAKEET_NPU -> requestedLanguage.takeUnless { it == "auto" }.orEmpty().ifBlank { "en" }
        QWEN3, QWEN3_HYBRID -> "auto"
        OMI, OMI_NPU -> "en"
        WHISPER -> if (whisperEnglishOnly) "en" else requestedLanguage
    }

    companion object {
        private val byId = entries.associateBy(AsrEngine::id)
        fun fromId(value: String?): AsrEngine = byId[value] ?: WHISPER
        val ids: Set<String> = byId.keys
    }
}
