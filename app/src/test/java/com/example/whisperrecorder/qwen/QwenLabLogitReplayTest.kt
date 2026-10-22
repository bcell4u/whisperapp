package com.example.whisperrecorder.qwen

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenLabLogitReplayTest {
    private fun fixture(): ByteArray = ByteBuffer.allocate(QwenLabLogitReplay.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN).apply {
            for (i in 0 until QwenLabLogitReplay.VOCAB) putFloat(i % 31 * 0.125f)
        }.array()

    @Test fun exactlyIdenticalVectorsPass() {
        val a = fixture()
        val result = QwenLabLogitReplay.compare(a, a.clone())
        assertTrue(result.identical)
        assertEquals(-1, result.firstDifferentFloat)
        assertEquals(0.0, result.maxAbsoluteDelta, 0.0)
        assertEquals(64, result.sha256.length)
    }
    @Test fun singleLogitDifferenceFailsWithIndex() {
        val a = fixture()
        val b = a.clone()
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putFloat(17 * 4, 99.5f)
        val result = QwenLabLogitReplay.compare(a, b)
        assertFalse(result.identical)
        assertEquals(17, result.firstDifferentFloat)
        assertTrue(result.maxAbsoluteDelta > 90)
    }
    @Test(expected = IllegalArgumentException::class)
    fun truncatedLogitsMustNotPass() {
        QwenLabLogitReplay.compare(ByteArray(128), fixture())
    }
    @Test(expected = IllegalArgumentException::class)
    fun nonfiniteLogitsMustNotPass() {
        val a = fixture()
        ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).putFloat(0, Float.NaN)
        QwenLabLogitReplay.compare(a, a.clone())
    }
    @Test fun negativeZeroBitMismatchIsReported() {
        val a = fixture()
        val b = a.clone()
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putFloat(0, -0.0f)
        val result = QwenLabLogitReplay.compare(a, b)
        assertFalse(result.identical)
        assertEquals(0, result.firstDifferentFloat)
    }
}
