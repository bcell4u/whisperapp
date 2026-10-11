package com.example.whisperrecorder.speaker

import kotlin.math.sqrt

/**
 * Resolves the independently numbered speakers returned by each bounded diarization window.
 * Prototypes contain voice embeddings only, never the spoken words or audio itself.
 * Ambiguous cross-window identities remain unknown; we must not silently swap voices.
 */
internal class SpeakerWindowStitcher(expectedSpeakers: Int) {
    private val fixedCount = SpeakerClusteringPolicy.normalize(expectedSpeakers).takeIf { it > 0 }
    private val maxIdentities = fixedCount ?: 12
    private val prototypes = mutableMapOf<Int, FloatArray>()
    private val updates = mutableMapOf<Int, Int>()
    private var previousWindow: List<SpeakerInterval> = emptyList()
    private var nextIdentity = 0

    /** Inputs use absolute recording time. Unknown speakers are assigned -1. */
    fun map(intervals: List<SpeakerInterval>, localEmbeddings: Map<Int, FloatArray>): List<SpeakerInterval> {
        val firstWindow = previousWindow.isEmpty() && nextIdentity == 0
        val labels = intervals.filter { it.speaker >= 0 }.map { it.speaker }.distinct()
        val embeddingsByLocal = localEmbeddings.mapNotNull { (id, e) -> normalized(e)?.let { id to it } }.toMap()
        data class Match(val local: Int, val global: Int, val score: Double, val reliable: Boolean)
        val candidates = mutableListOf<Match>()
        for (local in labels) {
            val overlaps = mutableMapOf<Int, Long>()
            for (segment in intervals.filter { it.speaker == local }) {
                for (prior in previousWindow.filter { it.speaker >= 0 }) {
                    val ms = minOf(segment.endMs, prior.endMs) - maxOf(segment.startMs, prior.startMs)
                    if (ms > 0) overlaps[prior.speaker] = (overlaps[prior.speaker] ?: 0) + ms
                }
            }
            val overlapTop = overlaps.entries.sortedByDescending { it.value }
            val topMs = overlapTop.firstOrNull()?.value ?: 0L
            val runnerUpMs = overlapTop.getOrNull(1)?.value ?: 0L
            if (topMs >= 1200L && topMs >= runnerUpMs * 2L) {
                candidates += Match(local, overlapTop.first().key, 2.0 + topMs.toDouble() / 100_000.0, true)
            }
            val embedding = embeddingsByLocal[local]
            if (embedding != null) {
                val similarities = prototypes.map { (id, prototype) -> id to cosine(embedding, prototype) }
                    .sortedByDescending { it.second }
                // Embedding similarity is not calibrated; reject poor or near-tied matches.
                similarities.forEachIndexed { index, (id, similarity) ->
                    val best = similarities.first().second
                    val runnerUp = similarities.getOrNull(1)?.second ?: -1.0
                    if (index == 0 && similarity >= 0.58 && best - runnerUp >= 0.035) {
                        candidates += Match(local, id, similarity, true)
                    }
                }
            }
        }
        val assigned = mutableMapOf<Int, Int>()
        val used = mutableSetOf<Int>()
        candidates.sortedWith(compareByDescending<Match> { it.score }.thenBy { it.local }).forEach { match ->
            if (match.reliable && match.local !in assigned && match.global !in used) {
                assigned[match.local] = match.global
                used += match.global
            }
        }
        for (local in labels) {
            if (local in assigned) continue
            // A genuinely new speaker may appear hours into a lecture. Fixed count never
            // creates more than the selected number, and weak matches are left unknown.
            if (nextIdentity < maxIdentities && (firstWindow || embeddingsByLocal[local] != null)) {
                assigned[local] = nextIdentity
                used += nextIdentity
                nextIdentity++
            } else {
                assigned[local] = -1
            }
        }
        for ((local, id) in assigned) {
            val vector = embeddingsByLocal[local] ?: continue
            if (id < 0) continue
            val old = prototypes[id]
            if (old == null) {
                prototypes[id] = vector
                updates[id] = 1
            } else {
                val count = (updates[id] ?: 1).coerceAtMost(8)
                val merged = FloatArray(vector.size) { i ->
                    (old[i] * count + vector[i]) / (count + 1)
                }
                prototypes[id] = normalized(merged) ?: old
                updates[id] = count + 1
            }
        }
        val mapped = intervals.map { it.copy(speaker = assigned[it.speaker] ?: -1) }
        previousWindow = mapped
        return mapped
    }

    companion object {
        private fun normalized(v: FloatArray): FloatArray? {
            if (v.isEmpty() || v.any { !it.isFinite() }) return null
            val length = sqrt(v.sumOf { it.toDouble() * it.toDouble() })
            if (length < 1.0e-9) return null
            return FloatArray(v.size) { i -> (v[i] / length).toFloat() }
        }
        private fun cosine(a: FloatArray, b: FloatArray): Double =
            if (a.size != b.size) -1.0 else a.indices.sumOf { a[it].toDouble() * b[it] }
    }
}
