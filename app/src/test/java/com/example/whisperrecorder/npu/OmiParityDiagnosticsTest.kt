package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test

class OmiParityDiagnosticsTest {
    @Test fun allZeroPaddingAndFixedMaskContractAreReported() {
        val pcm = FloatArray(16_000) { if (it % 2 == 0) 0.1f else -0.1f }
        val features = ParakeetPreprocessor(ParakeetPreprocessor.Config(
            sampleRate = 16_000, featureSize = 128, hopLength = 160,
            nFft = 512, winLength = 400, preemphasis = 0.97f,
            maxSamples = 192_000, physicalFrames = 1201
        )).extract(pcm)
        val summary = OmiParityDiagnostics.summarize(pcm, features)
        assertEquals(100, summary.validFrames)
        assertEquals(0, summary.featureNonFinite)
        assertEquals(0, summary.paddedNonZero)
        assertTrue(summary.featureRms.isFinite())
        assertTrue(summary.maskCounts.startsWith("50/25/"))
        assertTrue(OmiParityDiagnostics.FRONTEND_CONTRACT.contains("preemphasis0.97"))
    }
}
