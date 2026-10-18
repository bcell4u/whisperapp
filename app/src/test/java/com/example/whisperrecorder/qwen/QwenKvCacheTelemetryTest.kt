package com.example.whisperrecorder.qwen

import org.junit.Assert.*
import org.junit.Test

class QwenKvCacheTelemetryTest {
    @Test fun monotonicCacheCountsBytesButMakesNoCopyClaim() {
        val stats = QwenKvCacheTelemetry()
        for (size in listOf(40L, 44L, 48L, 48L, 52L)) stats.observe(size)
        assertEquals(40L, stats.firstBytes)
        assertEquals(52L, stats.lastBytes)
        assertEquals(232L, stats.logicalBytesSum)
        assertEquals(5, stats.samples)
        assertEquals(3, stats.growthSteps)
        assertEquals(0, stats.shrinkSteps)
    }
    @Test fun tracksShrinkAndRejectsInvalidPayload() {
        val stats = QwenKvCacheTelemetry()
        stats.observe(52)
        stats.observe(48)
        assertEquals(1, stats.shrinkSteps)
        try { stats.observe(-1); fail("negative payload should be rejected") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun accumulationSaturatesInsteadOfOverflowing() {
        val stats = QwenKvCacheTelemetry()
        stats.observe(Long.MAX_VALUE - 1)
        stats.observe(20)
        assertEquals(Long.MAX_VALUE, stats.logicalBytesSum)
    }
}
