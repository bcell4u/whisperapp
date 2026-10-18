package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class QwenEmbeddingReaderTest {
    @Test fun halfBitsHandleZeroSubnormalNormalAndInfinity() {
        assertEquals(0.0f, QwenEmbeddingReader.halfToFloat(0x0000), 0f)
        assertEquals(0.0f, QwenEmbeddingReader.halfToFloat(0x8000), 0f)
        assertEquals((-0f).toRawBits(), QwenEmbeddingReader.halfToFloat(0x8000).toRawBits())
        assertEquals(1f, QwenEmbeddingReader.halfToFloat(0x3c00), 0f)
        assertEquals(-2f, QwenEmbeddingReader.halfToFloat(0xc000), 0f)
        assertEquals(0.000000059604645f, QwenEmbeddingReader.halfToFloat(0x0001), 0f)
        assertEquals(Float.POSITIVE_INFINITY, QwenEmbeddingReader.halfToFloat(0x7c00), 0f)
        assertTrue(QwenEmbeddingReader.halfToFloat(0x7e00).isNaN())
    }

    @Test fun fp16ReaderSeeksTheCorrectRowWithoutLoadingTheTable() {
        val sample = File.createTempFile("k2a-embedding", ".bin")
        try {
            RandomAccessFile(sample, "rw").use { f ->
                val row = ByteBuffer.allocate(QwenEmbeddingReader.WIDTH * 2).order(ByteOrder.LITTLE_ENDIAN)
                repeat(QwenEmbeddingReader.WIDTH) { row.putShort(0x3c00.toShort()) }
                f.write(row.array())
                row.clear()
                repeat(QwenEmbeddingReader.WIDTH) { row.putShort(0xc000.toShort()) }
                f.write(row.array())
            }
            RandomAccessFile(sample, "r").use { f ->
                val second = QwenEmbeddingReader.readRow(f, 1, true)
                assertEquals(QwenEmbeddingReader.WIDTH, second.size)
                assertTrue(second.all { it == -2f })
            }
        } finally { sample.delete() }
    }

    @Test fun existingFp32ReaderRemainsUnchanged() {
        val sample = File.createTempFile("fp32-embedding", ".bin")
        try {
            RandomAccessFile(sample, "rw").use { f ->
                val row = ByteBuffer.allocate(QwenEmbeddingReader.WIDTH * 4).order(ByteOrder.LITTLE_ENDIAN)
                repeat(QwenEmbeddingReader.WIDTH) { row.putFloat(-0.25f) }
                f.write(row.array())
            }
            RandomAccessFile(sample, "r").use { f ->
                assertTrue(QwenEmbeddingReader.readRow(f, 0, false).all { it == -0.25f })
            }
        } finally { sample.delete() }
    }
    @Test fun reusableScratchReaderMatchesOriginalForFp32AndFp16() {
        for (fp16 in listOf(false, true)) {
            val sample = File.createTempFile("qwen-row-compare", ".bin")
            try {
                val values = listOf(0.0f, -0.0f, 1.0f, -2.0f, 0.5f)
                RandomAccessFile(sample, "rw").use { f ->
                    val row = ByteBuffer.allocate(QwenEmbeddingReader.WIDTH * (if (fp16) 2 else 4))
                        .order(ByteOrder.LITTLE_ENDIAN)
                    repeat(QwenEmbeddingReader.WIDTH) { i ->
                        val value = values[i % values.size]
                        if (fp16) {
                            val bits = when (value) {
                                0f -> if (value.toRawBits() < 0) 0x8000 else 0x0000
                                1f -> 0x3c00
                                -2f -> 0xc000
                                else -> 0x3800
                            }
                            row.putShort(bits.toShort())
                        } else row.putFloat(value)
                    }
                    f.write(row.array())
                }
                RandomAccessFile(sample, "r").use { f ->
                    val original = QwenEmbeddingReader.readRow(f, 0, fp16)
                    val reused = FloatArray(QwenEmbeddingReader.WIDTH)
                    val scratch = ByteArray(QwenEmbeddingReader.WIDTH * 4)
                    QwenEmbeddingReader.readRowInto(f, 0, fp16, reused, scratch)
                    for (i in original.indices) {
                        assertEquals(original[i].toRawBits(), reused[i].toRawBits())
                    }
                    // Run again with the same scratch: there must be no offset drift.
                    QwenEmbeddingReader.readRowInto(f, 0, fp16, reused, scratch)
                    assertArrayEquals(original, reused, 0f)
                }
            } finally { sample.delete() }
        }
    }

}
