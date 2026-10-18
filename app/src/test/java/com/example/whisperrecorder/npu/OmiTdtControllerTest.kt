package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmiTdtControllerTest {
    @Test fun emitsTokenAndAdvancesByTdtDurationWithoutPredictingOnBlank() {
        val predictions = ArrayList<Int>()
        var jointCalls = 0
        val runtime = object : OmiTdtController.Runtime {
            override fun predict(tokenId: Int) { predictions += tokenId }
            override fun joint(frameIndex: Int): FloatArray {
                val logits = FloatArray(1030) { -1f }
                val token = if (jointCalls++ == 0) 23 else 1024
                logits[token] = 9f
                logits[1025 + 1] = 10f // duration 1
                return logits
            }
        }
        val result = OmiTdtController.decode(3, runtime)
        assertEquals(listOf(23), result.tokens)
        assertEquals(listOf(0), result.frames)
        assertEquals(listOf(1024, 23), predictions)
        assertEquals(3, result.endingFrame)
        assertEquals(3, result.steps)
        assertEquals("frames-exhausted", result.stopReason)
        assertEquals(0, result.symbolLimitAdvances)
    }

    @Test fun blankZeroDurationCannotStallDecoder() {
        val result = OmiTdtController.decode(3, object : OmiTdtController.Runtime {
            override fun predict(tokenId: Int) { }
            override fun joint(frameIndex: Int): FloatArray = FloatArray(1030) { i ->
                if (i == 1024 || i == 1025) 3f else 0f
            }
        })
        assertTrue(result.tokens.isEmpty())
        assertEquals(3, result.endingFrame)
        assertEquals("frames-exhausted", result.stopReason)
    }
    @Test fun positiveDurationResetsPerFrameBudgetAndDoesNotSkipFrames() {
        val framesSeen = mutableListOf<Int>()
        val result = OmiTdtController.decode(35, object : OmiTdtController.Runtime {
            override fun predict(tokenId: Int) { }
            override fun joint(frameIndex: Int): FloatArray {
                framesSeen += frameIndex
                return FloatArray(1030) { i ->
                    if (i == 7 || i == 1026) 5f else -5f // token 7, duration 1
                }
            }
        })
        assertEquals((0 until 35).toList(), framesSeen)
        assertEquals(35, result.tokens.size)
        assertEquals((0 until 35).toList(), result.frames)
        assertEquals(0, result.symbolLimitAdvances)
        assertEquals("frames-exhausted", result.stopReason)
    }

    @Test fun zeroDurationTokensTriggerGuardExactlyOncePerTenEmissions() {
        val framesSeen = mutableListOf<Int>()
        val result = OmiTdtController.decode(2, object : OmiTdtController.Runtime {
            override fun predict(tokenId: Int) { }
            override fun joint(frameIndex: Int): FloatArray {
                framesSeen += frameIndex
                return FloatArray(1030) { i ->
                    if (i == 8 || i == 1025) 5f else -5f // token 8, duration 0
                }
            }
        })
        assertEquals(List(10) { 0 } + List(10) { 1 }, framesSeen)
        assertEquals(20, result.tokens.size)
        assertEquals(2, result.symbolLimitAdvances)
        assertEquals("frames-exhausted", result.stopReason)
    }

    @Test fun blankZeroDurationAdvancesAndCannotCauseSymbolGuard() {
        val framesSeen = mutableListOf<Int>()
        val result = OmiTdtController.decode(14, object : OmiTdtController.Runtime {
            override fun predict(tokenId: Int) { }
            override fun joint(frameIndex: Int): FloatArray {
                framesSeen += frameIndex
                return FloatArray(1030) { i ->
                    if (i == 1024 || i == 1025) 5f else -5f // blank + duration 0
                }
            }
        })
        assertEquals((0 until 14).toList(), framesSeen)
        assertEquals(0, result.symbolLimitAdvances)
        assertEquals(14, result.zeroDurations)
    }
}
