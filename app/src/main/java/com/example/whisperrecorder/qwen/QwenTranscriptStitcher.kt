package com.example.whisperrecorder.qwen

import com.example.whisperrecorder.transcribe.TranscriptCleaner

/**
 * Text-only boundary cleanup for optional 1-second overlap between 12-second QNN windows.
 *
 * This intentionally follows the conservative rules from production Parakeet's
 * TranscriptCleaner: a maximum 24-word EXACT suffix/prefix match, and a 12-character
 * minimum for a match of only two words. Per-window cleanup removes only substantial
 * phrases repeated at least three times. No fuzzy matching, word substitution, or
 * timing-based deletion is performed; medical terminology is never auto-corrected.
 * Neither the QNN encoder nor the ARM64 Q8 native decoder is changed here.
 */
internal object QwenTranscriptStitcher {
    internal data class Result(
        val transcript: String,
        val removedDuplicateWords: Int,
        val stitchedBoundaries: Int,
        val unmatchedBoundaries: Int,
        val repetitionCopiesRemoved: Int,
        val keptWindows: List<String>
    )

    private const val MAX_SEAM_WORDS = 24
    private const val TWO_WORD_MIN_MATCH_CHARS = 12
    private val word = Regex("[A-Za-z0-9']+")

    /** Inputs have ALREADY had their Qwen ASR protocol headers removed. */
    fun stitch(segments: List<String>, overlapSeconds: Int): Result {
        require(overlapSeconds == 0 || overlapSeconds == 1) { "Unsupported overlap" }
        if (segments.isEmpty()) return Result("", 0, 0, 0, 0, emptyList())
        // Preserve the original 6.6.1 non-overlap baseline byte-for-byte.
        if (overlapSeconds == 0) {
            return Result(segments.joinToString(" ") { it.trim() }.trim(), 0, 0, 0, 0, segments.map { it.trim() })
        }

        // Production Parakeet's cleaner preserves ordinary speech repetitions, removing
        // only obvious >=3-copy loops of substantial phrases within a decoded window.
        val cleaned = segments.map { TranscriptCleaner.cleanChunk(it) }
        val keptWindows = ArrayList<String>(cleaned.size).apply { add(cleaned.first().text) }
        val merged = StringBuilder(cleaned.first().text)
        var removed = 0
        var matched = 0
        var unmatched = 0
        for (piece in cleaned.drop(1)) {
            val next = piece.text
            if (next.isBlank()) {
                keptWindows.add("")
                unmatched++
                continue
            }
            // Limit rescanning for long lectures, as in production TranscriptCleaner.merge.
            val tail = merged.substring((merged.length - 4096).coerceAtLeast(0))
            val left = word.findAll(tail).toList().takeLast(MAX_SEAM_WORDS)
            val right = word.findAll(next).toList().take(MAX_SEAM_WORDS)
            val max = minOf(left.size, right.size, MAX_SEAM_WORDS)
            var matchedCount = 0
            for (count in max downTo 2) {
                val identical = (0 until count).all { i ->
                    left[left.size - count + i].value.equals(right[i].value, ignoreCase = true)
                }
                if (!identical) continue
                val matchedChars = right[count - 1].range.last - right[0].range.first + 1
                if (count == 2 && matchedChars < TWO_WORD_MIN_MATCH_CHARS) continue
                matchedCount = count
                break
            }
            val remainder = if (matchedCount == 0) {
                unmatched++
                next
            } else {
                removed += matchedCount
                matched++
                // Match production Parakeet's trim behavior, including punctuation
                // immediately following the duplicated prefix.
                next.substring(right[matchedCount - 1].range.last + 1)
                    .trimStart { it.isWhitespace() || it in ",.;:!?-" }
            }
            keptWindows.add(remainder)
            if (remainder.isNotBlank()) {
                if (merged.isNotEmpty()) merged.append(' ')
                merged.append(remainder)
            }
        }
        return Result(merged.toString().trim(), removed, matched, unmatched,
            cleaned.sumOf { it.repetitionsRemoved }, keptWindows)
    }
}
