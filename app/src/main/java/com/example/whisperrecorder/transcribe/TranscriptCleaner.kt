package com.example.whisperrecorder.transcribe

/**
 * Conservative transcript cleanup for failure modes that are common near the end of Whisper output.
 * It intentionally only removes exact/near-exact consecutive repetitions; normal lecturer repetition
 * is preserved unless the same substantial phrase repeats at least three times in a row.
 */
object TranscriptCleaner {
    private val HORIZONTAL_WHITESPACE = Regex("[\\t ]+")
    private val WHITESPACE = Regex("\\s+")
    private val ASCII_WORD = Regex("[A-Za-z0-9']+")
    private val CLAUSE_BREAK = Regex("(?<=[.!?])\\s+|\\n{2,}")
    private val NON_CANONICAL = Regex("[^a-z0-9']+")
    private const val BOUNDARY_CONTEXT_CHARS = 4096
    data class CleanupResult(
        val text: String,
        val repetitionsRemoved: Int = 0,
        val boundaryDuplicateRemoved: Boolean = false
    )

    fun cleanChunk(raw: String): CleanupResult {
        var text = raw.trim().replace(HORIZONTAL_WHITESPACE, " ")
        if (text.isBlank()) return CleanupResult("")

        var removed = 0
        // Work with punctuation-delimited clauses. Whisper loops commonly repeat the same sentence/phrase
        // three or more times at the tail. Requiring >= 3 consecutive copies avoids deleting ordinary
        // rhetorical repetition.
        val parts = splitClauses(text).toMutableList()
        var changed = true
        while (changed && parts.size >= 3) {
            changed = false
            var i = 0
            while (i <= parts.size - 3) {
                val a = canonical(parts[i])
                if (a.length >= 18 && canonical(parts[i + 1]) == a && canonical(parts[i + 2]) == a) {
                    var end = i + 3
                    while (end < parts.size && canonical(parts[end]) == a) end++
                    // Preserve one occurrence, remove the pathological repeats.
                    val count = end - i
                    repeat(count - 1) { parts.removeAt(i + 1) }
                    removed += count - 1
                    changed = true
                    break
                }
                i++
            }
        }
        text = parts.joinToString(" ").replace(WHITESPACE, " ").trim()
        val wordLoop = collapseWordLoops(text)
        text = wordLoop.first
        removed += wordLoop.second
        return CleanupResult(text, repetitionsRemoved = removed)
    }

    /**
     * Chunks do not overlap in audio, so boundary de-duplication is deliberately exact and conservative.
     * It only drops the first clause of the new chunk when it is substantially identical to the final
     * clause already emitted. This protects against Whisper independently reconstructing the same phrase
     * around a cut without erasing legitimate similar material.
     */
    fun merge(previous: StringBuilder, rawPart: String, allowBoundaryWordOverlap: Boolean = false): CleanupResult {
        val cleaned = cleanChunk(rawPart)
        var part = cleaned.text
        var boundaryRemoved = false
        if (previous.isNotEmpty() && part.isNotBlank()) {
            // Boundary cleanup only needs the tail of the existing transcript. Avoid converting and
            // rescanning the entire accumulated StringBuilder on every segment of a long recording.
            val tailStart = (previous.length - BOUNDARY_CONTEXT_CHARS).coerceAtLeast(0)
            val previousTail = previous.substring(tailStart, previous.length)
            val prevClauses = splitClauses(previousTail)
            val nextClauses = splitClauses(part)
            val prevLast = prevClauses.lastOrNull()
            val nextFirst = nextClauses.firstOrNull()
            if (prevLast != null && nextFirst != null) {
                val a = canonical(prevLast)
                val b = canonical(nextFirst)
                if (a.length >= 24 && a == b) {
                    part = nextClauses.drop(1).joinToString(" ").trim()
                    boundaryRemoved = true
                }
            }
            if (allowBoundaryWordOverlap && part.isNotBlank()) {
                val deduped = removeExactBoundaryWordOverlap(previousTail, part)
                if (deduped != part) {
                    part = deduped
                    boundaryRemoved = true
                }
            }
        }
        if (part.isNotBlank()) {
            if (previous.isNotEmpty()) previous.append(' ')
            previous.append(part)
        }
        return cleaned.copy(text = part, boundaryDuplicateRemoved = boundaryRemoved)
    }


    /**
     * Parakeet is intentionally decoded with a small audio overlap. Remove only a high-confidence,
     * exact suffix/prefix word match so the overlap does not duplicate text in the saved transcript.
     */
    private fun removeExactBoundaryWordOverlap(previous: String, next: String): String {
        val wordRegex = ASCII_WORD
        val previousWords = wordRegex.findAll(previous).toList()
        val nextWords = wordRegex.findAll(next).toList()
        val maxWords = minOf(24, previousWords.size, nextWords.size)
        for (count in maxWords downTo 2) {
            var same = true
            for (i in 0 until count) {
                val a = previousWords[previousWords.size - count + i].value
                val b = nextWords[i].value
                if (!a.equals(b, ignoreCase = true)) {
                    same = false
                    break
                }
            }
            if (!same) continue
            val matchedChars = nextWords[count - 1].range.last - nextWords[0].range.first + 1
            // Two very short words can coincide naturally; require a little more evidence for 2-word matches.
            if (count == 2 && matchedChars < 12) continue
            return next.substring(nextWords[count - 1].range.last + 1)
                .trimStart { it.isWhitespace() || it in ",.;:!?-" }
        }
        return next
    }

    private fun collapseWordLoops(input: String): Pair<String, Int> {
        var text = input
        var removedCopies = 0
        var passes = 0
        val wordRegex = ASCII_WORD
        while (passes++ < 8) {
            val matches = wordRegex.findAll(text).toList()
            if (matches.size < 15) break
            val words = matches.map { it.value.lowercase() }
            var found: Triple<Int, Int, Int>? = null // start token, phrase length, repeat count

            // Prefer later loops and longer phrases. Requiring three exact word-sequence copies is
            // intentionally conservative and avoids deleting ordinary one-off lecturer repetition.
            val earliestTailToken = (words.size - 250).coerceAtLeast(0)
            for (start in (words.size - 15) downTo earliestTailToken) {
                val maxPhrase = minOf(18, (words.size - start) / 3)
                for (length in maxPhrase downTo 5) {
                    val phraseChars = matches[start + length - 1].range.last - matches[start].range.first + 1
                    if (phraseChars < 20) continue
                    var count = 1
                    while (start + (count + 1) * length <= words.size) {
                        var same = true
                        for (j in 0 until length) {
                            if (words[start + j] != words[start + count * length + j]) {
                                same = false
                                break
                            }
                        }
                        if (!same) break
                        count++
                    }
                    if (count >= 3) {
                        found = Triple(start, length, count)
                        break
                    }
                }
                if (found != null) break
            }

            val loop = found ?: break
            val start = loop.first
            val length = loop.second
            val count = loop.third
            val removeStart = matches[start + length].range.first
            val removeEndExclusive = matches[start + length * count - 1].range.last + 1
            text = (text.substring(0, removeStart) + text.substring(removeEndExclusive))
                .replace(WHITESPACE, " ")
                .trim()
            removedCopies += count - 1
        }
        return text to removedCopies
    }

    private fun splitClauses(text: String): List<String> {
        // Include punctuation in the clause where possible. Also split on long line breaks from model output.
        return text
            .replace("\r", "")
            .split(CLAUSE_BREAK)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun canonical(text: String): String = text
        .lowercase()
        .replace(NON_CANONICAL, " ")
        .replace(WHITESPACE, " ")
        .trim()
}
