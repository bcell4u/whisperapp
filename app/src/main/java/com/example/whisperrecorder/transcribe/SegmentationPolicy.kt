package com.example.whisperrecorder.transcribe

/**
 * Runtime-aware audio segmentation policy.
 *
 * Segment limits are mobile safety ceilings, not model context requirements. For recordings
 * longer than an engine's ceiling, the policy balances work across the minimum number of
 * segments needed so the final segment is not an extremely short tail.
 *
 * Whisper remains compatible with the existing user setting: the configured 1-5 minute value
 * is treated as a maximum segment length and Whisper keeps zero overlap. Other local engines
 * use a small overlap so TranscriptCleaner can remove words repeated across boundaries.
 * Production NVIDIA Parakeet and Omi Med use the verified 40-second OpenCL segment size with 2-second overlap.
 * NVIDIA v3 established the stable mobile GPU policy; Omi shares the same bounded worker path.
 */
object SegmentationPolicy {
    data class Plan(
        val segmentSeconds: Int,
        val overlapSeconds: Int,
        val maxSegmentSeconds: Int,
        val runtimeMaxSegmentSeconds: Int,
        val pcmSafetyMaxSeconds: Int,
        val strategy: String
    )

    private const val NON_WHISPER_OVERLAP_SECONDS = 2
    private const val TARGET_SAMPLE_RATE = 16_000
    private const val PCM_WORKING_SET_BUDGET_BYTES = 48L * 1024L * 1024L
    private const val MIN_PCM_SAFETY_SECONDS = 30

    private const val PARAKEET_MAX_SECONDS = 40
    private const val OMI_MAX_SECONDS = 40
    private const val OMI_NPU_MAX_SECONDS = 12
    private const val QWEN3_MAX_SECONDS = 60

    fun plan(
        engine: AsrEngine,
        totalDurationMs: Long,
        sourceSampleRate: Int,
        whisperChunkMinutes: Int
    ): Plan {
        val totalSeconds = ((totalDurationMs.coerceAtLeast(1L) + 999L) / 1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
            .coerceAtLeast(1)

        require(sourceSampleRate > 0) { "Invalid source sample rate" }

        val runtimeMaxSegmentSeconds = when (engine) {
            AsrEngine.WHISPER -> whisperChunkMinutes.coerceIn(1, 5) * 60
            AsrEngine.PARAKEET, AsrEngine.PARAKEET_NPU -> PARAKEET_MAX_SECONDS
            AsrEngine.OMI -> OMI_MAX_SECONDS
            AsrEngine.OMI_NPU -> OMI_NPU_MAX_SECONDS
            AsrEngine.QWEN3 -> QWEN3_MAX_SECONDS
        }
        val pcmSafetyMaxSeconds = pcmSafetyMaxSeconds(sourceSampleRate)
        val maxSegmentSeconds = minOf(runtimeMaxSegmentSeconds, pcmSafetyMaxSeconds)
        val overlapSeconds = when (engine) {
            AsrEngine.OMI_NPU -> 1
            else -> if (engine.usesBoundaryOverlap) NON_WHISPER_OVERLAP_SECONDS else 0
        }
        val fixedParakeet = engine == AsrEngine.PARAKEET || engine == AsrEngine.PARAKEET_NPU || engine == AsrEngine.OMI || engine == AsrEngine.OMI_NPU
        val segmentSeconds = if (fixedParakeet) {
            minOf(totalSeconds, maxSegmentSeconds)
        } else {
            balancedSegmentSeconds(
                totalSeconds = totalSeconds,
                maxSegmentSeconds = maxSegmentSeconds,
                overlapSeconds = overlapSeconds
            )
        }

        return Plan(
            segmentSeconds = segmentSeconds,
            overlapSeconds = overlapSeconds.coerceAtMost((segmentSeconds - 1).coerceAtLeast(0)),
            maxSegmentSeconds = maxSegmentSeconds,
            runtimeMaxSegmentSeconds = runtimeMaxSegmentSeconds,
            pcmSafetyMaxSeconds = pcmSafetyMaxSeconds,
            strategy = when {
                totalSeconds <= maxSegmentSeconds -> "single-pass"
                engine == AsrEngine.PARAKEET_NPU -> "production-npu-fixed40"
                engine == AsrEngine.OMI_NPU -> "experimental-omi-npu-fixed12-overlap1"
                fixedParakeet -> "production-opencl-fixed40"
                else -> "balanced"
            }
        )
    }


    /**
     * PcmDecoder now streams source-rate decoder output directly into a bounded 16 kHz buffer,
     * so the Java-side working set no longer scales with a video's 44.1/48 kHz source rate.
     * Budget for both the accumulation buffer and the FloatArray handed to native inference.
     */
    internal fun pcmSafetyMaxSeconds(sourceSampleRate: Int): Int {
        require(sourceSampleRate > 0) { "Invalid source sample rate" }
        val estimatedBytesPerSecond = TARGET_SAMPLE_RATE.toLong() * 4L * 2L
        return (PCM_WORKING_SET_BUDGET_BYTES / estimatedBytesPerSecond)
            .coerceAtLeast(MIN_PCM_SAFETY_SECONDS.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    /**
     * Choose the shortest equal-sized segment that still uses the minimum number of segments
     * implied by maxSegmentSeconds. This avoids tiny final tails without exceeding the ceiling.
     */
    internal fun balancedSegmentSeconds(
        totalSeconds: Int,
        maxSegmentSeconds: Int,
        overlapSeconds: Int
    ): Int {
        require(totalSeconds > 0) { "Total duration must be positive" }
        require(maxSegmentSeconds > 0) { "Maximum segment duration must be positive" }
        require(overlapSeconds >= 0 && overlapSeconds < maxSegmentSeconds) { "Invalid segment overlap" }
        if (totalSeconds <= maxSegmentSeconds) return totalSeconds

        val stride = maxSegmentSeconds - overlapSeconds
        val remainingAfterFirst = totalSeconds - maxSegmentSeconds
        val segmentCount = 1L +
            (remainingAfterFirst.toLong() + stride.toLong() - 1L) / stride.toLong()
        val coverageNeeded = totalSeconds.toLong() + (segmentCount - 1L) * overlapSeconds.toLong()
        val balanced = (coverageNeeded + segmentCount - 1L) / segmentCount
        return balanced.coerceIn(1L, maxSegmentSeconds.toLong()).toInt()
    }
}
