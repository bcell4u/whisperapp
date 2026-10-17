package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Test

class NpuEncoderPaddingTest {
    @Test fun ignoresValidFramesAndReadsChannelMajorPadding() {
        val data = FloatArray(1024 * 501)
        data[0] = 9f
        data[500] = -0.2f
        data[501 + 500] = 0.7f
        assertEquals(0.7f, encoderPaddedMaxAbs(data, 500), 0f)
    }

    @Test fun noPaddingReturnsZero() {
        val data = FloatArray(1024 * 501)
        data[data.lastIndex] = 2f
        assertEquals(0f, encoderPaddedMaxAbs(data, 501), 0f)
    }

    @Test fun zeroFilledPaddingReturnsZero() {
        val data = FloatArray(1024 * 501)
        data[501 * 1023] = 2f
        assertEquals(0f, encoderPaddedMaxAbs(data, 1), 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongTensorShape() {
        encoderPaddedMaxAbs(FloatArray(5), 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeValidFrames() {
        encoderPaddedMaxAbs(FloatArray(1024 * 501), 502)
    }
}
