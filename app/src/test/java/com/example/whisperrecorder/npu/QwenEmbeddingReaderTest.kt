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
}
