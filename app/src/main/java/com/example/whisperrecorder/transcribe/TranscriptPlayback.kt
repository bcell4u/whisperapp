package com.example.whisperrecorder.transcribe

/** Character range in the saved transcript that corresponds to one timed cue. */
data class TranscriptCueRange(
    val cueIndex: Int,
    val start: Int,
    val endExclusive: Int
)

/**
 * Maps cue text back onto the original saved transcript without reformatting it.
 * Matching is token-based so punctuation and whitespace remain exactly as stored for display.
 */
object TranscriptCueMapper {
    private data class Token(val value: String, val start: Int, val endExclusive: Int)

    private val tokenRegex = Regex("[\\p{L}\\p{N}'’]+")

    fun map(transcript: String, cues: List<TranscriptCue>, skipSpeakerHeadings: Boolean = false): List<TranscriptCueRange> {
        if (transcript.isBlank() || cues.isEmpty()) return emptyList()
        val headers = speakerHeaderRanges(transcript, skipSpeakerHeadings)
        val transcriptTokens = playableTokens(transcript, headers)
        if (transcriptTokens.isEmpty()) return emptyList()

        val ranges = mutableListOf<TranscriptCueRange>()
        var searchFrom = 0
        cues.forEachIndexed { cueIndex, cue ->
            val cueTokens = tokens(cue.text).map { it.value }
            if (cueTokens.isEmpty()) return@forEachIndexed
            val tokenIndex = findSequence(transcriptTokens, cueTokens, searchFrom)
            if (tokenIndex < 0) return@forEachIndexed

            val lastIndex = tokenIndex + cueTokens.lastIndex
            ranges += TranscriptCueRange(
                cueIndex = cueIndex,
                start = transcriptTokens[tokenIndex].start,
                endExclusive = transcriptTokens[lastIndex].endExclusive
            )
            searchFrom = lastIndex + 1
        }
        return ranges
    }

    /** Excludes labels and time marks from speaker sync coverage statistics. */
    fun playableWordCount(text: String): Int =
        playableTokens(text, speakerHeaderRanges(text, true)).size

    private fun speakerHeaderRanges(text: String, skip: Boolean): List<IntRange> =
        if (!skip) emptyList() else (
            Regex("(?m)^.{1,64}:\r?$").findAll(text) +
                Regex("(?m)^\\[\\d{2}:\\d{2}:\\d{2}\\] ").findAll(text)
            ).map { it.range }.sortedBy { it.first }.toList()

    private fun playableTokens(text: String, headers: List<IntRange>): List<Token> {
        if (headers.isEmpty()) return tokens(text)
        var headerIndex = 0
        return tokens(text).filter { token ->
            while (headerIndex < headers.size && headers[headerIndex].last < token.start) headerIndex++
            headerIndex == headers.size || token.start !in headers[headerIndex]
        }
    }

    private fun findSequence(
        transcriptTokens: List<Token>,
        cueTokens: List<String>,
        startIndex: Int
    ): Int {
        if (cueTokens.isEmpty() || startIndex >= transcriptTokens.size) return -1
        val lastStart = transcriptTokens.size - cueTokens.size
        if (lastStart < startIndex) return -1
        for (candidate in startIndex..lastStart) {
            var matches = true
            for (offset in cueTokens.indices) {
                if (transcriptTokens[candidate + offset].value != cueTokens[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return candidate
        }
        return -1
    }

    private fun tokens(text: String): List<Token> = tokenRegex.findAll(text).map { match ->
        Token(
            value = match.value.lowercase().replace('’', '\''),
            start = match.range.first,
            endExclusive = match.range.last + 1
        )
    }.toList()
}

fun cueIndexAt(cues: List<TranscriptCue>, positionMs: Long): Int {
    if (cues.isEmpty()) return -1
    var low = 0
    var high = cues.lastIndex
    var candidate = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (cues[mid].startMs <= positionMs) {
            candidate = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    if (candidate < 0) return -1
    return candidate.takeIf { positionMs < cues[it].endMs } ?: -1
}
