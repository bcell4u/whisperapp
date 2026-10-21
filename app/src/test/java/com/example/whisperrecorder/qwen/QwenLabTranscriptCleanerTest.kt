package com.example.whisperrecorder.qwen

import org.junit.Assert.assertEquals
import org.junit.Test

class QwenLabTranscriptCleanerTest {
    @Test fun removesRealS26ProtocolPrefix() {
        assertEquals("So it's describing like a set.",
            QwenLabTranscriptCleaner.cleanSegment("language English<asr_text>So it's describing like a set."))
    }

    @Test fun worksForEachSegment() {
        assertEquals("And then we once we.",
            QwenLabTranscriptCleaner.cleanSegment("language English <asr_text> And then we once we."))
    }

    @Test fun preservesSpeechAndUnknownTags() {
        assertEquals("language English is commonly spoken.",
            QwenLabTranscriptCleaner.cleanSegment("language English is commonly spoken."))
        assertEquals("<asr_text>Unrecognized header",
            QwenLabTranscriptCleaner.cleanSegment("<asr_text>Unrecognized header"))
    }
}
