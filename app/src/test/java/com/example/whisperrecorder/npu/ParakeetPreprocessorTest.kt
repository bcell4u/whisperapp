package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class ParakeetPreprocessorTest {
    @Test fun transformerCompatibleFixtureShapeMasksAndNormalization() {
        val preprocessor = ParakeetPreprocessor(
            ParakeetPreprocessor.Config(16_000, 128, 160, 512, 400, 0.97f)
        )
        val result = preprocessor.extract(syntheticFixture())
        assertEquals(100, result.validFeatureFrames)
        assertEquals(13, result.validEncodedFrames)
        assertEquals(128 * 4001, result.audioSignal.size)
        assertEquals(2001, result.subsampleMask1.size)
        assertEquals(1001, result.subsampleMask2.size)
        assertEquals(501, result.subsampleMask3.size)
        assertEquals(50f, result.subsampleMask1.sum(), 0f)
        assertEquals(25f, result.subsampleMask2.sum(), 0f)
        assertEquals(13f, result.subsampleMask3.sum(), 0f)

        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        var total = 0.0
        var totalSq = 0.0
        var count = 0
        repeat(128) { mel ->
            val base = mel * 4001
            repeat(result.validFeatureFrames) { frame ->
                val value = result.audioSignal[base + frame]
                min = minOf(min, value)
                max = maxOf(max, value)
                total += value
                totalSq += value.toDouble() * value
                count++
            }
        }
        assertEquals(-9.898734f, min, 1e-4f)
        assertEquals(9.89161f, max, 1e-4f)
        assertTrue(kotlin.math.abs(total / count) < 1e-6)
        assertEquals(0.9949574, sqrt(totalSq / count), 1e-5)
    }
    private fun syntheticFixture(sampleRate: Int = 16_000, seconds: Int = 1): FloatArray {
        val count = sampleRate * seconds
        return FloatArray(count) { i ->
            val t = i.toDouble() / sampleRate
            (0.08 * sin(2.0 * PI * 220.0 * t) +
                0.04 * sin(2.0 * PI * 440.0 * t + 0.23) +
                0.02 * sin(2.0 * PI * 880.0 * t + 0.61)).toFloat()
        }
    }

}
