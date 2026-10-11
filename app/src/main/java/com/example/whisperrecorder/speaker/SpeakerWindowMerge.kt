package com.example.whisperrecorder.speaker

/** Emits each overlapped timestamp once, splitting the shared region at its midpoint. */
internal class SpeakerWindowMerge {
    private var pending = emptyList<SpeakerInterval>()
    private var pendingWindowEndMs = Long.MIN_VALUE

    fun push(windowStartMs: Long, windowEndMs: Long, labeled: List<SpeakerInterval>): List<SpeakerInterval> {
        if (pendingWindowEndMs == Long.MIN_VALUE) {
            pending = labeled
            pendingWindowEndMs = windowEndMs
            return emptyList()
        }
        require(windowStartMs >= 0 && windowEndMs > windowStartMs)
        val crossover = windowStartMs + (pendingWindowEndMs - windowStartMs).coerceAtLeast(0L) / 2L
        val completed = clip(pending, Long.MIN_VALUE, crossover)
        pending = clip(labeled, crossover, Long.MAX_VALUE)
        pendingWindowEndMs = windowEndMs
        return completed
    }

    fun finish(): List<SpeakerInterval> = pending.also {
        pending = emptyList()
        pendingWindowEndMs = Long.MIN_VALUE
    }

    private fun clip(items: List<SpeakerInterval>, from: Long, to: Long): List<SpeakerInterval> =
        items.mapNotNull { item ->
            val start = maxOf(item.startMs, from)
            val end = minOf(item.endMs, to)
            if (end > start) item.copy(startMs = start, endMs = end) else null
        }
}
