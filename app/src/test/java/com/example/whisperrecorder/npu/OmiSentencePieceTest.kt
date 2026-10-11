package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class OmiSentencePieceTest {
    @Test fun parsesDecodeOnlyModelProtoAndFrameTimedWords() {
        val model = ByteArrayOutputStream()
        repeat(1024) { index ->
            val piece = when (index) {
                0 -> "<unk>"
                1 -> "▁heart"
                2 -> "▁rate"
                else -> "piece$index"
            }
            val inner = field(1, piece.toByteArray(Charsets.UTF_8))
            model.write(field(1, inner))
        }
        val tokenizer = OmiSentencePiece.fromModel(model.toByteArray())
        assertEquals("heart rate", tokenizer.decode(listOf(1, 2)))
        val words = tokenizer.timedWords(listOf(1, 2), listOf(0, 6), 1000)
        assertEquals(listOf("heart", "rate"), words.map { it.text })
        assertEquals(0L, words[0].startMs)
        assertEquals(480L, words[1].startMs)
    }

    private fun field(number: Int, content: ByteArray): ByteArray =
        byteArrayOf((number shl 3 or 2).toByte()) + varint(content.size) + content

    private fun varint(input: Int): ByteArray {
        var value = input
        val output = ByteArrayOutputStream()
        do {
            val bits = value and 127
            value = value ushr 7
            output.write(bits or (if (value > 0) 128 else 0))
        } while (value > 0)
        return output.toByteArray()
    }
}
