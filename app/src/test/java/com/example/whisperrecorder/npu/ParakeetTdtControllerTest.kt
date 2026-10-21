package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParakeetTdtControllerTest {
    @Test fun pinnedControllerSemanticsStayExact() {
        data class Decision(val token: Int, val duration: Int)
        val decisions = listOf(
            Decision(7, 0),
            Decision(8192, 0),
            Decision(8, 2),
            Decision(8192, 1),
            Decision(8192, 4)
        )
        val predicted = mutableListOf<Int>()
        val jointFrames = mutableListOf<Int>()
        val emitted = mutableListOf<Int>()
        val emittedFrames = mutableListOf<Int>()
        var cursor = 0
        val runtime = object : ParakeetTdtController.Runtime {
            override fun predict(tokenId: Int) { predicted += tokenId }
            override fun joint(frameIndex: Int): FloatArray {
                jointFrames += frameIndex
                val decision = decisions[cursor++]
                return FloatArray(8198) { -10f }.also {
                    it[decision.token] = 10f
                    it[8193 + decision.duration] = 10f
                }
            }
        }
        val summary = ParakeetTdtController.run(
            validFrames = 6,
            config = ParakeetTdtConfig(8192, 8192, intArrayOf(0, 1, 2, 3, 4), 10),
            runtime = runtime,
            onTokenEmitted = { token, frame -> emitted += token; emittedFrames += frame }
        )

        assertTrue(summary.encoderExhausted)
        assertEquals(5, summary.steps)
        assertEquals(8, summary.finalFrameIndex)
        assertEquals(listOf(0, 0, 1, 3, 4), jointFrames)
        assertEquals(listOf(8192, 7, 8), predicted)
        assertEquals(listOf(7, 8), emitted)
        assertEquals(listOf(0, 1), emittedFrames)
    }
}
