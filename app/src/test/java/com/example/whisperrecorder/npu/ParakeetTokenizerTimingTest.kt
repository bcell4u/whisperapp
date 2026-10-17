package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test

class ParakeetTokenizerTimingTest {
    // Pure JVM fixture: Android's JSONObject is intentionally not used in local unit tests.
    private fun tokenizer(): ParakeetTokenizer {
        val vocab = arrayOfNulls<String>(8193)
        vocab[3] = "▁Hello"
        vocab[4] = "▁world"
        vocab[5] = "!"
        vocab[6] = "▁caf"
        vocab[7] = "<0xC3>"
        vocab[8] = "<0xA9>"
        val ctor = ParakeetTokenizer::class.java.declaredConstructors.first { it.parameterCount == 4 }
            .apply { isAccessible = true }
        return ctor.newInstance(vocab, setOf(8192), "▁", 2) as ParakeetTokenizer
    }

    @Test fun decoderFramesProduceMonotonicWordTimes() {
        val result = tokenizer().decodeTimedWords(listOf(3, 4, 5), listOf(4, 10, 13), 2500)
        assertEquals(listOf("Hello", "world!"), result.map { it.text })
        assertEquals(320L, result[0].startMs)
        assertEquals(800L, result[1].startMs)
        assertTrue(result[0].endMs <= result[1].startMs)
    }

    @Test fun byteFallbackDoesNotChangeTranscript() {
        val ids = listOf(6, 7, 8)
        assertEquals("café", tokenizer().decode(ids))
        assertEquals("café", tokenizer().decodeTimedWords(ids, listOf(0, 1, 1), 1000).single().text)
    }
}
