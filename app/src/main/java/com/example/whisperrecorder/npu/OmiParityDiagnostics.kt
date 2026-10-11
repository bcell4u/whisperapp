package com.example.whisperrecorder.npu

import kotlin.math.sqrt

/** Privacy-preserving diagnostics for comparing Android features with an external NeMo reference.
 *  Aggregates are NOT numeric parity evidence; parity still requires identical per-bin references.
 */
internal object OmiParityDiagnostics {
    const val FRONTEND_CONTRACT = "16k-mono;128mel;fft512;win400;hop160;hann-periodic-false;preemphasis0.97;" +
        "slaney-mel;log-guard-2^-24;per-channel-variance-unbiased;eps1e-5;center-zero-pad;1201f"

    data class Summary(
        val pcmRms: Double, val pcmPeak: Double, val pcmClipPct: Double,
        val featureRms: Double, val featurePeak: Double, val featureMean: Double,
        val featureNonFinite: Int, val paddedNonZero: Int,
        val validFrames: Int, val encodedFrames: Int, val maskCounts: String
    )

    fun summarize(pcm: FloatArray, features: ParakeetPreprocessor.Result): Summary {
        var pcmPower = 0.0
        var peak = 0.0
        var clipped = 0
        pcm.forEach { x ->
            val abs = kotlin.math.abs(x.toDouble())
            if (abs > peak) peak = abs
            if (abs >= 0.999) clipped++
            pcmPower += x.toDouble() * x
        }
        val physical = 1201
        val valid = features.validFeatureFrames
        require(features.audioSignal.size == 128 * physical && valid in 1..1200)
        var featureSum = 0.0
        var featurePower = 0.0
        var featurePeak = 0.0
        var nonFinite = 0
        var nonZeroPad = 0
        for (channel in 0 until 128) {
            val base = channel * physical
            for (i in 0 until physical) {
                val x = features.audioSignal[base + i]
                if (!x.isFinite()) { nonFinite++; continue }
                if (i >= valid) { if (x != 0f) nonZeroPad++; continue }
                featureSum += x
                featurePower += x.toDouble() * x
                featurePeak = maxOf(featurePeak, kotlin.math.abs(x.toDouble()))
            }
        }
        val count = (128 * valid).coerceAtLeast(1)
        fun countMask(m: FloatArray) = m.count { it != 0f }
        return Summary(
            sqrt(pcmPower / pcm.size.coerceAtLeast(1)), peak,
            clipped * 100.0 / pcm.size.coerceAtLeast(1),
            sqrt(featurePower / count), featurePeak, featureSum / count,
            nonFinite, nonZeroPad, valid, features.validEncodedFrames,
            "${countMask(features.subsampleMask1)}/${countMask(features.subsampleMask2)}/${countMask(features.subsampleMask3)}"
        )
    }
}
