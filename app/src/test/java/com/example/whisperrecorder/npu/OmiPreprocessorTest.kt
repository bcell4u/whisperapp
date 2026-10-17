package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Test

class OmiPreprocessorTest {
    @Test fun twelveSecondContractHasCorrectMasksAndEncoderFrames() {
        val preprocessor = ParakeetPreprocessor(ParakeetPreprocessor.Config(
            16_000, 128, 160, 512, 400, 0.97f, 192_000, 1201))
        val input = FloatArray(16_000) { i -> (i % 101) / 101f }
        val out = preprocessor.extract(input)
        assertEquals(128 * 1201, out.audioSignal.size)
        assertEquals(601, out.subsampleMask1.size)
        assertEquals(301, out.subsampleMask2.size)
        assertEquals(151, out.subsampleMask3.size)
        assertEquals(13, out.validEncodedFrames)
    }
}
