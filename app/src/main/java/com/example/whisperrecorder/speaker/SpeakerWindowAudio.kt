package com.example.whisperrecorder.speaker

/** Builds a short clean utterance for cross-window speaker matching, without saving PCM. */
internal object SpeakerWindowAudio {
    private const val SAMPLE_RATE = 16_000
    private const val MIN_MS = 1500L
    private const val MAX_MS = 9000L

    fun representative(samples: FloatArray, windowStartMs: Long, segments: List<SpeakerInterval>, speaker: Int): FloatArray? {
        val valid = segments.filter { it.speaker == speaker && it.endMs - it.startMs >= MIN_MS }
            .sortedByDescending { it.endMs - it.startMs }
        // One uninterrupted high-quality speech region is preferable to concatenating
        // unrelated phonemes across pauses and speaker transitions.
        for (part in valid) {
            val from = ((part.startMs - windowStartMs) * SAMPLE_RATE / 1000L).toInt().coerceAtLeast(0)
            val to = ((part.endMs - windowStartMs) * SAMPLE_RATE / 1000L).toInt().coerceAtMost(samples.size)
            if (to - from >= MIN_MS * SAMPLE_RATE / 1000L) {
                return samples.copyOfRange(from, minOf(to, from + (MAX_MS * SAMPLE_RATE / 1000L).toInt()))
            }
        }
        return null
    }
}
