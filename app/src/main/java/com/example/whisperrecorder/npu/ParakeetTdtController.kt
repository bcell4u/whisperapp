package com.example.whisperrecorder.npu

internal data class ParakeetTdtConfig(
    val blankTokenId: Int,
    val decoderStartTokenId: Int,
    val durations: IntArray,
    val maxSymbolsPerStep: Int
) {
    fun requireValidatedContract() {
        require(blankTokenId == 8192) { "Unexpected blank token: $blankTokenId" }
        require(decoderStartTokenId == 8192) { "Unexpected decoder-start token: $decoderStartTokenId" }
        require(durations.contentEquals(intArrayOf(0, 1, 2, 3, 4))) { "Unexpected TDT durations: ${durations.contentToString()}" }
        require(maxSymbolsPerStep == 10) { "Unexpected max_symbols_per_step: $maxSymbolsPerStep" }
    }
}

internal data class TdtControllerSummary(
    val steps: Int,
    val finalFrameIndex: Int,
    val encoderExhausted: Boolean
)

/** Exact CPU controller semantics validated against Transformers 5.9 by the Builder. */
internal object ParakeetTdtController {
    interface Runtime {
        /** Executes and commits predictor output/recurrent state for this token. */
        fun predict(tokenId: Int)

        /** Executes joint for the current projected frame and cached predictor output. */
        fun joint(frameIndex: Int): FloatArray
    }

    fun run(
        validFrames: Int,
        config: ParakeetTdtConfig,
        runtime: Runtime,
        onTokenEmitted: (Int, Int) -> Unit = { _, _ -> }
    ): TdtControllerSummary {
        config.requireValidatedContract()
        require(validFrames > 0) { "TDT requires at least one valid encoder frame" }
        val generatedDecisionCap = config.maxSymbolsPerStep * (validFrames + 1) - 1
        require(generatedDecisionCap > 0) { "Invalid TDT generated decision cap" }

        runtime.predict(config.decoderStartTokenId)
        var frameIndex = 0
        var steps = 0

        while (frameIndex < validFrames && steps < generatedDecisionCap) {
            val logits = runtime.joint(frameIndex)
            require(logits.size == 8198) { "Joint output width ${logits.size}, expected 8198" }
            val token = argmax(logits, 0, 8193)
            val durationIndex = argmax(logits, 8193, 8198) - 8193
            val duration = config.durations[durationIndex]
            val effective = if (token == config.blankTokenId && duration == 0) 1 else duration

            if (token != config.blankTokenId) onTokenEmitted(token, frameIndex)
            frameIndex += effective
            steps++
            if (frameIndex >= validFrames) break

            // Critical TDT fast path: blank reuses predictor output/state.
            if (token != config.blankTokenId) runtime.predict(token)
        }

        return TdtControllerSummary(
            steps = steps,
            finalFrameIndex = frameIndex,
            encoderExhausted = frameIndex >= validFrames
        )
    }

    private fun argmax(values: FloatArray, from: Int, to: Int): Int {
        var best = from
        var bestValue = values[from]
        require(bestValue.isFinite()) { "Non-finite joint logit at index $from" }
        for (i in from + 1 until to) {
            val value = values[i]
            require(value.isFinite()) { "Non-finite joint logit at index $i" }
            if (value > bestValue) {
                bestValue = value
                best = i
            }
        }
        return best
    }
}
