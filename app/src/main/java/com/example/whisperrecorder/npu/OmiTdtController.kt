package com.example.whisperrecorder.npu

/** Omi Med v1 / Parakeet TDT v2 greedy decoding, independent of Parakeet v3's 8192-token contract. */
internal object OmiTdtController {
    const val BLANK = 1024
    val DURATIONS = intArrayOf(0, 1, 2, 3, 4)
    const val LOGITS = 1030
    const val MAX_SYMBOLS_PER_FRAME = 10

    interface Runtime {
        /** Run the predictor and commit recurrent states only when called (initial blank or emitted token). */
        fun predict(tokenId: Int)
        fun joint(frameIndex: Int): FloatArray
    }

    data class Result(val tokens: List<Int>, val frames: List<Int>, val steps: Int, val endingFrame: Int,
        val blanks: Int, val zeroDurations: Int, val symbolLimitAdvances: Int,
        val stopReason: String)

    fun decode(validFrames: Int, runtime: Runtime, isCancelled: () -> Boolean = { false }): Result {
        require(validFrames in 1..150) { "Omi encoder valid frames outside 1..150: $validFrames" }
        runtime.predict(BLANK)
        val tokens = ArrayList<Int>()
        val frames = ArrayList<Int>()
        var t = 0
        var steps = 0
        var symbolCount = 0
        var blankCount = 0
        var zeroDurationCount = 0
        var symbolLimitAdvances = 0
        val maxSteps = MAX_SYMBOLS_PER_FRAME * (validFrames + 1) - 1
        while (t < validFrames && steps < maxSteps) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Omi NPU decoding cancelled")
            val logits = runtime.joint(t)
            require(logits.size == LOGITS) { "Omi joint width mismatch: ${logits.size}" }
            val token = argmax(logits, 0, BLANK + 1)
            val durationIndex = argmax(logits, BLANK + 1, LOGITS) - (BLANK + 1)
            val duration = DURATIONS[durationIndex]
            val advance = if (token == BLANK && duration == 0) 1 else duration
            if (duration == 0) zeroDurationCount++
            if (token == BLANK) blankCount++
            steps++
            if (token != BLANK) {
                tokens += token
                frames += t
                // NeMo's symbol budget applies to ONE encoder frame, not the whole chunk.
                // Non-zero durations already move to a new frame and reset the counter.
                symbolCount = if (advance == 0) symbolCount + 1 else 0
            } else {
                symbolCount = 0
            }
            t += advance
            if (t >= validFrames) break
            if (token != BLANK) runtime.predict(token)
            // Only force an advance after 10 nonblank ZERO-duration emissions on
            // the SAME frame. Never skip an extra frame after a positive duration.
            if (advance == 0 && symbolCount >= MAX_SYMBOLS_PER_FRAME) {
                t++
                symbolLimitAdvances++
                symbolCount = 0
            }
        }
        return Result(tokens, frames, steps, t, blankCount, zeroDurationCount,
            symbolLimitAdvances, if (t >= validFrames) "frames-exhausted" else "max-steps")
    }

    private fun argmax(logits: FloatArray, start: Int, end: Int): Int {
        var best = start
        require(logits[start].isFinite()) { "Omi joint nonfinite at $start" }
        for (i in start + 1 until end) {
            val x = logits[i]
            require(x.isFinite()) { "Omi joint nonfinite at $i" }
            if (x > logits[best]) best = i
        }
        return best
    }
}
