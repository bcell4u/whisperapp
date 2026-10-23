package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Test

class QwenWorkProgressTest {
    @Test fun progressUsesOnlyCompletedWindowReceipts() {
        assertEquals(0f, QwenWorkProgress.fraction(0, 0, 20), 0.0001f)
        assertEquals(0.25f, QwenWorkProgress.fraction(10, 0, 20), 0.0001f)
        assertEquals(0.5f, QwenWorkProgress.fraction(20, 0, 20), 0.0001f)
        assertEquals(0.75f, QwenWorkProgress.fraction(20, 10, 20), 0.0001f)
        assertEquals(1f, QwenWorkProgress.fraction(20, 20, 20), 0.0001f)
        assertEquals(1f, QwenWorkProgress.fraction(999, 999, 20), 0.0001f)
        assertEquals(0f, QwenWorkProgress.fraction(0, 0, 0), 0.0001f)
        assertEquals(0.5f, QwenWorkProgress.fraction(20, 20, 20, 1, 2), 0.0001f)
        assertEquals(0.5f, QwenWorkProgress.fraction(0, 0, 20, 2, 2), 0.0001f)
        assertEquals(0.75f, QwenWorkProgress.fraction(20, 0, 20, 2, 2), 0.0001f)
    }
}
