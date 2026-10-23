package com.example.whisperrecorder.qwen

/** Strips only Qwen's leading ASR protocol header from each separately decoded segment.
 * Spoken content and token generation are not modified. Unexpected prefixes remain visible.
 */
internal object QwenTranscriptCleaner {
    // Example from the first S26 transcript: "language English<asr_text>So it's..."
    private val header = Regex("^\\s*language\\s+[A-Za-z][A-Za-z_-]*\\s*<asr_text>\\s*", RegexOption.IGNORE_CASE)

    fun cleanSegment(decoded: String): String = decoded.replaceFirst(header, "").trim()
}
