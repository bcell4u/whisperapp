package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Test

/** The displayed QNN window count must match the 12-second/11-second-step encoder loop. */
class QwenWindowProgressTest {
    @Test fun windowsFollowActualEncodeStride() {
        assertEquals(1, Qwen06Session.estimatedWindowCount(1))
        assertEquals(1, Qwen06Session.estimatedWindowCount(192_000))
        assertEquals(2, Qwen06Session.estimatedWindowCount(192_001))
        assertEquals(2, Qwen06Session.estimatedWindowCount(368_000))
        assertEquals(3, Qwen06Session.estimatedWindowCount(368_001))
        assertEquals(11, Qwen06Session.estimatedWindowCount(120 * 16_000))
        assertEquals(22, Qwen06Session.estimatedWindowCount(240 * 16_000))
    }
}
