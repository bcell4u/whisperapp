package com.example.whisperrecorder.transcribe

/** Two real completion receipts per window: encoder and decoder. This measures
 * window-work units, not elapsed time or predicted time remaining. Model loading
 * does not advance the bar until a window actually completes. */
internal object QwenWorkProgress {
    fun fraction(encoded: Int, decoded: Int, total: Int, chunk: Int = 1, totalChunks: Int = 1): Float {
        if (total <= 0) return 0f
        val done = encoded.coerceIn(0, total).toLong() + decoded.coerceIn(0, total).toLong()
        val sliceFraction = done.toFloat() / (2L * total).toFloat()
        // Keep the bar monotonic across the 240-second outer recording slices.
        // Each slice owns one slot; this is counted work, NOT remaining-time prediction.
        val slots = totalChunks.coerceAtLeast(1)
        return ((chunk.coerceIn(1, slots) - 1) + sliceFraction) / slots
    }
    fun percent(encoded: Int, decoded: Int, total: Int, chunk: Int = 1, totalChunks: Int = 1): Int =
        (fraction(encoded, decoded, total, chunk, totalChunks) * 100f).toInt().coerceIn(0, 99)
}
