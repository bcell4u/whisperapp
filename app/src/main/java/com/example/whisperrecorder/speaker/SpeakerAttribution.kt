package com.example.whisperrecorder.speaker

/**
 * Align diarization evidence to ASR cues without double-counting overlapping windows.
 * A cluster confidence is a silhouette score (-1..1), NOT a calibrated probability.
 */
internal object SpeakerAttribution {
    private const val UNAVAILABLE_CONFIDENCE = -2.0f
    private const val MIN_EVIDENCE_MS = 150L
    private const val MIN_CUE_COVERAGE = 0.20
    private const val MIN_SPEECH_DOMINANCE = 0.45
    private const val MIN_WEIGHTED_DOMINANCE = 0.65

    fun bestSpeaker(cue: SpeakerTextCue, intervals: List<SpeakerInterval>): Int? {
        val relevant = intervals.filter {
            it.speaker >= 0 && it.startMs >= 0 && it.endMs > it.startMs &&
                it.startMs < cue.endMs && it.endMs > cue.startMs
        }
        if (relevant.isEmpty()) return null
        // Walk distinct time slices so multiple overlapping pyannote windows for the
        // SAME voice contribute only once. Two different voices may overlap legitimately.
        val boundaries = (listOf(cue.startMs, cue.endMs) + relevant.flatMap {
            listOf(maxOf(cue.startMs, it.startMs), minOf(cue.endMs, it.endMs))
        }).distinct().sorted()
        val raw = mutableMapOf<Int, Long>()
        val weighted = mutableMapOf<Int, Double>()
        var speechMs = 0L
        for (i in 0 until boundaries.lastIndex) {
            val start = boundaries[i]
            val end = boundaries[i + 1]
            if (end <= start) continue
            val active = relevant.filter { it.startMs < end && it.endMs > start }
            if (active.isEmpty()) continue
            val length = end - start
            speechMs += length
            active.groupBy { it.speaker }.forEach { (speaker, windows) ->
                // Overlapping windows from a single speaker cannot multiply the vote.
                raw[speaker] = (raw[speaker] ?: 0L) + length
                val bestWeight = windows.maxOf { confidenceWeight(it.confidence) }
                weighted[speaker] = (weighted[speaker] ?: 0.0) + length * bestWeight
            }
        }
        val winner = weighted.maxWithOrNull(compareBy<Map.Entry<Int, Double>> { it.value }.thenBy { -it.key })
            ?: return null
        val winnerSpeechMs = raw[winner.key] ?: return null
        val cueMs = cue.endMs - cue.startMs
        val scoreTotal = weighted.values.sum()
        // A native Parakeet word can be 40-120 ms; the legacy 150 ms minimum
        // would incorrectly mark nearly every word as Unknown speaker.
        val minEvidence = minOf(MIN_EVIDENCE_MS, maxOf(40L, cueMs / 2L))
        if (winnerSpeechMs < minEvidence || cueMs <= 0L || speechMs <= 0L || scoreTotal <= 0.0) return null
        // Do not assign a long cue from a tiny, possibly incidental speech fragment.
        if (winnerSpeechMs.toDouble() / cueMs < MIN_CUE_COVERAGE) return null
        // When two speakers share even a word-length cue, report uncertainty.
        if (winnerSpeechMs.toDouble() / speechMs < MIN_SPEECH_DOMINANCE) return null
        if (winner.value / scoreTotal < MIN_WEIGHTED_DOMINANCE) return null
        return winner.key
    }

    private fun confidenceWeight(confidence: Float): Double =
        if (confidence.isFinite() && confidence >= -1f && confidence <= 1f)
            (0.75 + confidence.toDouble() * 0.25) // 0.5..1; never a probability
        else 1.0 // Older/unavailable confidence: do not discard valid speech.
}
