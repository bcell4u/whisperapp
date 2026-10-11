package com.example.whisperrecorder.transcribe

/** All backends: already processed audio must not rewind on overlapping chunk boundaries.
 * Progress is credited only after a chunk has finished, never for audio still in inference.
 */
internal object TranscriptionProgressPolicy {
    fun monotonicProcessed(previousCompletedMs: Long, candidateMs: Long, totalMs: Long): Long {
        if (totalMs <= 0L) return 0L
        return maxOf(previousCompletedMs, candidateMs).coerceIn(0L, totalMs)
    }

    fun activePercent(completedMs: Long, totalMs: Long): Int {
        if (totalMs <= 0L) return 0
        // Convert after division to avoid overflow on long recordings.
        return ((completedMs.coerceIn(0L, totalMs).toDouble() / totalMs) * 100.0)
            .toInt().coerceIn(0, 99)
    }
}
