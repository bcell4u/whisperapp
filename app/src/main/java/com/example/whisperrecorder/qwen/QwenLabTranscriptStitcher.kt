package com.example.whisperrecorder.qwen

import java.util.Locale

/**
 * Conservative text-only stitcher for the optional one-second overlapping audio windows.
 * The 12-second NPU encoder inputs and native dot-product decoder are unchanged.
 *
 * We remove a seam only when at least TWO entire words match exactly (ignoring only
 * case and punctuation), confined to the final/initial eight words. A single repeated
 * word, a partial word, or a merely similar medical term is NOT removed. No fuzzy
 * medical corrections are ever made. This does not prove word-level time alignment.
 */
internal object QwenLabTranscriptStitcher {
    internal data class Result(
        val transcript: String,
        val removedDuplicateWords: Int,
        val stitchedBoundaries: Int,
        val unmatchedBoundaries: Int
    )

    private const val MAX_SEAM_WORDS = 8
    private val word = Regex("\\S+")

    private data class Token(val normalized: String, val start: Int)

    private fun words(text: String): List<Token> = word.findAll(text).map { match ->
        Token(match.value.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT), match.range.first)
    }.filter { it.normalized.isNotEmpty() }.toList()

    /** Inputs have ALREADY had their ASR protocol headers removed. */
    fun stitch(segments: List<String>, overlapSeconds: Int): Result {
        require(overlapSeconds == 0 || overlapSeconds == 1) { "Unsupported overlap" }
        if (segments.isEmpty()) return Result("", 0, 0, 0)
        if (overlapSeconds == 0) return Result(segments.joinToString(" ") { it.trim() }.trim(), 0, 0, 0)

        val merged = StringBuilder(segments.first().trim())
        var removed = 0
        var matched = 0
        var unmatched = 0
        for (nextRaw in segments.drop(1)) {
            val next = nextRaw.trim()
            if (next.isBlank()) {
                unmatched++
                continue
            }
            val left = words(merged.toString()).takeLast(MAX_SEAM_WORDS)
            val right = words(next).take(MAX_SEAM_WORDS)
            val max = minOf(left.size, right.size, MAX_SEAM_WORDS)
            var matchedCount = 0
            for (count in max downTo 2) {
                if ((0 until count).all { i -> left[left.size - count + i].normalized == right[i].normalized }) {
                    matchedCount = count
                    break
                }
            }
            val remainder = if (matchedCount == 0) {
                unmatched++
                next
            } else {
                removed += matchedCount
                matched++
                if (matchedCount == right.size) "" else next.substring(right[matchedCount].start).trimStart()
            }
            if (remainder.isNotBlank()) {
                if (merged.isNotEmpty()) merged.append(' ')
                merged.append(remainder)
            }
        }
        return Result(merged.toString().trim(), removed, matched, unmatched)
    }
}
