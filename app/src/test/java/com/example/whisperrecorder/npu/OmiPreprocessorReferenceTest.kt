package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Offline goldens generated independently with torch.stft and librosa Slaney Mels.
 *  These guard against regressions, but do NOT certify parity with Omi's .nemo checkpoint.
 *  See scripts/omi-parity/reference_features.py for reproducible reference algorithm.
 */
class OmiPreprocessorReferenceTest {
    private val processor = ParakeetPreprocessor(ParakeetPreprocessor.Config(
        16_000, 128, 160, 512, 400, 0.97f, 192_000, 1201))

    private data class Probe(val mel: Int, val frame: Int, val expected: Float)

    private fun verify(input: FloatArray, expectedValid: Int, probes: List<Probe>) {
        val result = processor.extract(input)
        assertEquals(expectedValid, result.validFeatureFrames)
        assertEquals((expectedValid + 7) / 8, result.validEncodedFrames)
        val sizes = intArrayOf(601, 301, 151)
        val masks = arrayOf(result.subsampleMask1, result.subsampleMask2, result.subsampleMask3)
        var count = expectedValid
        for (stage in 0..2) {
            count = (count + 1) / 2
            assertEquals(sizes[stage], masks[stage].size)
            for (i in masks[stage].indices) {
                assertEquals("mask stage=$stage index=$i", if (i < count) 1f else 0f, masks[stage][i], 0f)
            }
        }
        for (mel in 0 until 128) {
            for (f in 0 until 1201) {
                val x = result.audioSignal[mel * 1201 + f]
                assertTrue("nonfinite mel=$mel frame=$f", x.isFinite())
                if (f >= expectedValid) assertEquals("padded mel=$mel frame=$f", 0f, x, 0f)
            }
        }
        for (probe in probes) {
            assertEquals("independent torch/librosa probe mel=${probe.mel} frame=${probe.frame}",
                probe.expected, result.audioSignal[probe.mel * 1201 + probe.frame], 0.0003f)
        }
    }

    @Test fun impulseMatchesIndependentNumericProbesAndMasks() {
        val pcm = FloatArray(16_000) { if (it == 8_123) 0.8f else 0f }
        verify(pcm, 100, listOf(
            Probe(0, 0, -0.14232635f), Probe(2, 50, 6.15080881f),
            Probe(20, 50, 5.99672174f), Probe(50, 50, 6.16764832f),
            Probe(127, 50, 6.34297514f), Probe(70, 80, -0.142435f)))
    }

    @Test fun fullTwelveSecondToneMatchesIndependentNumericProbes() {
        val pcm = FloatArray(192_000) { i -> (0.1 * sin(2 * PI * 440 * i / 16_000)).toFloat() }
        verify(pcm, 1200, listOf(
            Probe(0, 0, 29.16229248f), Probe(2, 0, 26.66949081f),
            Probe(10, 0, 30.54165459f), Probe(20, 20, -0.02739978f),
            Probe(40, 600, -0.04135656f), Probe(127, 1199, 2.50514960f)))
    }

    @Test fun partialNoiseMatchesIndependentNumericProbesAndMasks() {
        val random = Random(20261010)
        val pcm = FloatArray(51_237) { (random.nextDouble() * 0.02 - 0.01).toFloat() }
        verify(pcm, 320, listOf(
            Probe(0, 0, 0.27915433f), Probe(2, 187, 0.45782152f),
            Probe(20, 150, 0.90767711f), Probe(40, 10, 1.81923294f),
            Probe(80, 200, -0.64902782f), Probe(127, 319, 0.72715986f)))
    }

    @Test fun silenceRemainsFiniteAndExactlyPadded() {
        val pcm = FloatArray(16_000)
        val result = processor.extract(pcm)
        assertEquals(100, result.validFeatureFrames)
        for (x in result.audioSignal) assertTrue(x.isFinite() && kotlin.math.abs(x) < 0.0003f)
    }
}
