package com.example.whisperrecorder.transcribe

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A display-sized transcript cue aligned to the original recording timeline. */
data class TranscriptCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/** Relative timing returned by a native backend for one inference segment. */
data class TimedTextSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class TranscriptTimeline(
    val cues: List<TranscriptCue>
) {
    fun toJson(): String = JSONObject().apply {
        put("version", 1)
        put("cues", JSONArray().apply {
            cues.forEach { cue ->
                put(JSONObject().apply {
                    put("startMs", cue.startMs)
                    put("endMs", cue.endMs)
                    put("text", cue.text)
                })
            }
        })
    }.toString()

    companion object {
        fun fromJson(raw: String): TranscriptTimeline? = runCatching {
            val root = JSONObject(raw)
            val array = root.optJSONArray("cues") ?: JSONArray()
            val cues = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val text = item.optString("text").trim()
                    val start = item.optLong("startMs", -1L)
                    val end = item.optLong("endMs", -1L)
                    if (text.isNotBlank() && start >= 0L && end > start) {
                        add(TranscriptCue(start, end, text))
                    }
                }
            }.sortedBy { it.startMs }
            TranscriptTimeline(cues)
        }.getOrNull()
    }
}

/**
 * Stores timing metadata privately so the user's recording folder remains limited to audio and
 * normal .txt transcripts. The library metadata itself is app-private, so this sidecar follows the
 * same lifetime and is safe to regenerate by transcribing again.
 */
object TranscriptTimelineStore {
    private fun directory(context: Context): File =
        File(context.filesDir, "transcript-timelines").apply { mkdirs() }

    fun save(context: Context, recordingId: String, timeline: TranscriptTimeline): String {
        val target = File(directory(context), "$recordingId.json")
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(timeline.toJson())
        if (target.exists()) require(target.delete()) { "Unable to replace transcript timing metadata" }
        require(temp.renameTo(target)) { "Unable to finalize transcript timing metadata" }
        return target.absolutePath
    }

    fun speakerPath(context: Context, recordingId: String): String =
        File(directory(context), "$recordingId-speaker.json").absolutePath

    fun saveSpeaker(context: Context, recordingId: String, timeline: TranscriptTimeline): String {
        val path = speakerPath(context, recordingId)
        val target = File(path)
        val temp = File(path + ".tmp")
        temp.writeText(timeline.toJson())
        require(temp.renameTo(target) || (target.delete() && temp.renameTo(target))) {
            "Unable to finalize speaker timing metadata"
        }
        return path
    }

    fun load(path: String?): TranscriptTimeline? {
        val file = path?.let(::File)?.takeIf { it.isFile } ?: return null
        return runCatching { TranscriptTimeline.fromJson(file.readText()) }.getOrNull()
    }

    fun delete(path: String?): Boolean {
        val file = path?.let(::File) ?: return true
        return !file.exists() || file.delete()
    }
}

/** Converts model output into short display lines while preserving the best timestamps available. */
object TranscriptTimelineBuilder {
    /** Detailed, text-free alignment outcome for support diagnostics. No decoded words are logged. */
    data class CueBuildResult(
        val cues: List<TranscriptCue>,
        val mode: String,
        val reason: String,
        val nativeProvided: Int,
        val nativeAligned: Int,
        val overlapDiscarded: Int,
        val overlapClipped: Int,
        val droppedForCleanup: Int
    )

    private data class NativeAlignment(
        val segments: List<TimedTextSegment>,
        val strategy: String,
        val removedSegments: Int
    )
    private const val MAX_WORDS_PER_CUE = 14
    private const val MAX_NATIVE_WORDS_PER_CUE = 10
    private const val MAX_ESTIMATED_WORDS_PER_CUE = 7
    private const val MIN_ESTIMATED_WORDS_BEFORE_PUNCT_BREAK = 3
    private const val SPEECH_FRAME_SAMPLES = 320 // 20 ms at 16 kHz
    private const val NATIVE_PAUSE_BREAK_MS = 900L
    private val NON_WHITESPACE = Regex("\\S+")
    private val DISPLAY_LINE_BREAK = Regex("(?<=[.!?])\\s+|\\n+")
    private val CANONICAL_NON_TOKEN = Regex("[^\\p{L}\\p{N}']+")
    private val CANONICAL_WHITESPACE = Regex("\\s+")

    fun cuesForChunk(
        cleanedText: String,
        chunkStartMs: Long,
        chunkEndMs: Long,
        nativeSegments: List<TimedTextSegment> = emptyList(),
        wordByWord: Boolean = false,
        minimumStartMs: Long = chunkStartMs,
        speechSamples: FloatArray? = null,
        speechSampleStart: Int = 0,
        speechSampleEnd: Int = speechSamples?.size ?: 0
    ): List<TranscriptCue> = cuesForChunkDetailed(
        cleanedText, chunkStartMs, chunkEndMs, nativeSegments, wordByWord,
        minimumStartMs, speechSamples, speechSampleStart, speechSampleEnd
    ).cues

    /**
     * Align the complete native text BEFORE removing any words outside the non-overlap time floor.
     * Previously, clipping word timestamps first could remove enough words to defeat exact alignment,
     * even when the TDT tokenizer supplied correct tokens and timestamps.
     */
    fun cuesForChunkDetailed(
        cleanedText: String,
        chunkStartMs: Long,
        chunkEndMs: Long,
        nativeSegments: List<TimedTextSegment> = emptyList(),
        wordByWord: Boolean = false,
        minimumStartMs: Long = chunkStartMs,
        speechSamples: FloatArray? = null,
        speechSampleStart: Int = 0,
        speechSampleEnd: Int = speechSamples?.size ?: 0
    ): CueBuildResult {
        val text = cleanedText.trim()
        fun result(cues: List<TranscriptCue>, mode: String, reason: String, aligned: Int = 0,
                   expired: Int = 0, clipped: Int = 0, removed: Int = 0) =
            CueBuildResult(cues, mode, reason, nativeSegments.size, aligned, expired, clipped, removed)
        if (text.isBlank() || chunkEndMs <= minimumStartMs) {
            return result(emptyList(), "empty", "empty-text-or-window")
        }
        val fullNative = nativeSegments
            .filter { it.text.isNotBlank() && it.endMs > it.startMs && it.startMs >= 0L }
            .map { segment ->
                TimedTextSegment(chunkStartMs + segment.startMs, chunkStartMs + segment.endMs,
                    segment.text.trim())
            }
        val alignment = alignNativeToCleanedText(fullNative, text)
        var nativeFailure = if (fullNative.isEmpty()) "no-valid-native-words" else
            "text-token-mismatch nativeTokens=${alignmentTokens(fullNative.joinToString(" ") { it.text }).size}" +
            " cleanedTokens=${alignmentTokens(text).size}"
        if (alignment != null) {
            val valid = alignment.segments.mapNotNull { segment ->
                val start = segment.startMs.coerceAtLeast(minimumStartMs).coerceAtLeast(chunkStartMs)
                val end = segment.endMs.coerceAtMost(chunkEndMs)
                if (end > start) TimedTextSegment(start, end, segment.text) else null
            }
            val expired = alignment.segments.size - valid.size
            val clipped = alignment.segments.count { it.startMs < minimumStartMs && it.endMs > minimumStartMs }
            val nativeCues = cuesFromNativeSegments(valid, minimumStartMs, wordByWord)
            if (nativeCues.isNotEmpty()) {
                return result(nativeCues, if (expired > 0) "native-partial-overlap" else "native",
                    alignment.strategy, alignment.segments.size, expired, clipped,
                    alignment.removedSegments)
            }
            nativeFailure = "aligned-native-outside-nonoverlap-window"
        }
        if (speechSamples != null) {
            val skippedSamples = (((minimumStartMs - chunkStartMs).coerceAtLeast(0L) * 16_000L) / 1000L)
                .toInt()
            val speechCues = allocateBySpeechActivity(splitEstimatedPhrases(text), minimumStartMs,
                chunkEndMs, speechSamples,
                (speechSampleStart + skippedSamples).coerceAtMost(speechSampleEnd), speechSampleEnd)
            if (speechCues.isNotEmpty()) return result(speechCues, "estimated-speech", nativeFailure)
        }
        return result(allocate(splitDisplayLines(text), minimumStartMs, chunkEndMs),
            "estimated-uniform", nativeFailure)
    }

    /**
     * Qwen text has no native word timestamps. Its native segment start/end are 12-second
     * decoding WINDOW boundaries, not speech onset/offset. Align a few-word cue inside
     * each window using local PCM activity; never spread the entire transcript over a
     * 240-second slice. If post-processing altered the text, fall back to safe general
     * timing rather than mismatching highlights with visible transcript words.
     */
    fun cuesForQwenWindows(
        cleanedText: String,
        chunkStartMs: Long,
        chunkEndMs: Long,
        nativeWindows: List<TimedTextSegment>,
        minimumStartMs: Long,
        speechSamples: FloatArray,
        speechSampleStart: Int,
        speechSampleEnd: Int
    ): List<TranscriptCue>? {
        if (nativeWindows.isEmpty()) return null
        val combined = nativeWindows.joinToString(" ") { it.text.trim() }.trim()
        if (combined.replace(Regex("\\s+"), " ") != cleanedText.trim().replace(Regex("\\s+"), " ")) return null
        val sorted = nativeWindows.sortedBy { it.startMs }
        val out = mutableListOf<TranscriptCue>()
        for ((index, window) in sorted.withIndex()) {
            val startMs = (chunkStartMs + window.startMs).coerceAtLeast(minimumStartMs)
            // The next decoding window begins at +11s; assign its overlap to
            // that next window to prevent double timing of the same 1s audio.
            val nextStart = sorted.getOrNull(index + 1)?.let { chunkStartMs + it.startMs }
            val endMs = minOf(chunkStartMs + window.endMs, nextStart ?: chunkEndMs, chunkEndMs)
            if (endMs <= startMs) continue
            val startSample = (speechSampleStart + ((startMs - chunkStartMs) * 16L).toInt())
                .coerceIn(speechSampleStart, speechSampleEnd)
            val endSample = (speechSampleStart + ((endMs - chunkStartMs) * 16L).toInt())
                .coerceIn(startSample, speechSampleEnd)
            val estimated = allocateBySpeechActivity(
                splitEstimatedPhrases(window.text.trim(), maxWords = 3), startMs, endMs,
                speechSamples, startSample, endSample
            )
            val phrases = if (estimated.isNotEmpty()) estimated else
                allocate(splitEstimatedPhrases(window.text.trim(), maxWords = 3), startMs, endMs)
            // Speech-frame rounding can overlap adjacent 20 ms bins by one frame.
            // Clip the next phrase to the previous phrase's end: cueIndexAt must
            // never be asked to resolve overlapping intervals.
            var floorMs = startMs
            for (phrase in phrases) {
                val cueStart = maxOf(phrase.startMs, floorMs)
                val cueEnd = minOf(phrase.endMs, endMs)
                if (cueEnd > cueStart) {
                    out += TranscriptCue(cueStart, cueEnd, phrase.text)
                    floorMs = cueEnd
                }
            }
        }
        return out.takeIf { it.isNotEmpty() }
    }

    private fun alignNativeToCleanedText(
        nativeSegments: List<TimedTextSegment>, cleanedText: String
    ): NativeAlignment? {
        if (nativeSegments.isEmpty()) return null
        val targetTokens = alignmentTokens(cleanedText)
        if (targetTokens.isEmpty()) return null
        val nativeTokenRows = nativeSegments.map { alignmentTokens(it.text) }
        val full = nativeTokenRows.flatten()
        if (full == targetTokens) return NativeAlignment(nativeSegments, "exact", 0)
        var suffixCount = 0
        for (i in nativeSegments.lastIndex downTo 1) {
            suffixCount += nativeTokenRows[i].size
            if (suffixCount > targetTokens.size) break
            if (suffixCount == targetTokens.size &&
                nativeTokenRows.subList(i, nativeTokenRows.size).flatten() == targetTokens) {
                return NativeAlignment(nativeSegments.drop(i), "prefix-removed", i)
            }
        }

        // A decoded phrase segment (e.g. Qwen) may have its duplicate prefix cut within a segment.
        data class TokenRef(val value: String, val segment: Int, val charStart: Int)
        val refs = buildList {
            nativeSegments.forEachIndexed { index, segment ->
                alignmentTokenRegex.findAll(segment.text).forEach { match ->
                    add(TokenRef(normalizeToken(match.value), index, match.range.first))
                }
            }
        }
        if (refs.size >= targetTokens.size) {
            val start = refs.size - targetTokens.size
            if (refs.subList(start, refs.size).map { it.value } == targetTokens) {
                val first = refs[start]
                val segment = nativeSegments[first.segment]
                val removedWithin = refs.count { it.segment == first.segment && it.charStart < first.charStart }
                val inSegment = refs.count { it.segment == first.segment }.coerceAtLeast(1)
                val adjustedStart = segment.startMs +
                    (segment.endMs - segment.startMs) * removedWithin / inSegment
                val prefix = segment.text.substring(first.charStart).trim()
                val aligned = listOfNotNull(if (prefix.isNotBlank() && segment.endMs > adjustedStart)
                    TimedTextSegment(adjustedStart, segment.endMs, prefix) else null) +
                    nativeSegments.drop(first.segment + 1)
                if (alignmentTokens(aligned.joinToString(" ") { it.text }) == targetTokens) {
                    return NativeAlignment(aligned, "intra-segment-prefix-removed", first.segment)
                }
            }
        }

        // Conservative exact-token subsequence for cleanChunk's removal of repeated clauses/word
        // loops inside the chunk. Only supports actual per-word native cues. Do not invent an
        // alignment for substitutions or reorderings; those still use estimated timing.
        if (nativeTokenRows.all { it.size == 1 } && targetTokens.size >= 2 &&
            targetTokens.size * 2 >= nativeSegments.size && targetTokens.size < nativeSegments.size) {
            val selected = IntArray(targetTokens.size)
            var search = nativeSegments.lastIndex
            var matched = true
            for (i in targetTokens.indices.reversed()) {
                while (search >= 0 && nativeTokenRows[search][0] != targetTokens[i]) search--
                if (search < 0) { matched = false; break }
                selected[i] = search--
            }
            if (matched && selected.toList().zipWithNext().all { (a,b) -> b > a }) {
                return NativeAlignment(selected.map(nativeSegments::get), "exact-token-subsequence",
                    nativeSegments.size - selected.size)
            }
        }
        return null
    }

    private fun cuesFromNativeSegments(
        segments: List<TimedTextSegment>,
        minimumStartMs: Long,
        wordByWord: Boolean
    ): List<TranscriptCue> {
        if (segments.isEmpty()) return emptyList()
        if (wordByWord && segments.all { wordCount(it.text) == 1 }) {
            return segments.mapNotNull { segment ->
                val start = segment.startMs.coerceAtLeast(minimumStartMs)
                if (segment.endMs > start) TranscriptCue(start, segment.endMs, segment.text) else null
            }
        }
        val looksWordTimed = segments.size > 1 && segments.all { wordCount(it.text) <= 2 }
        if (!looksWordTimed) {
            // A native segment boundary is more trustworthy than an invented subdivision inside it.
            // Preserve each real segment as one cue; proportional allocation is reserved for engines
            // that provide no usable timing metadata at all.
            val out = mutableListOf<TranscriptCue>()
            var floor = minimumStartMs
            segments.forEach { segment ->
                val start = segment.startMs.coerceAtLeast(floor)
                if (segment.endMs > start) {
                    out += TranscriptCue(start, segment.endMs, segment.text.trim())
                    floor = segment.endMs
                }
            }
            return out
        }

        // Word timestamps are more precise than the UI needs. Group nearby words into short phrases
        // while preserving real silence gaps; this keeps the inline highlight readable without
        // reverting to proportional timing across the entire chunk.
        val out = mutableListOf<TranscriptCue>()
        val group = mutableListOf<TimedTextSegment>()
        var groupWords = 0

        fun flush() {
            if (group.isEmpty()) return
            val start = group.first().startMs.coerceAtLeast(minimumStartMs)
            val end = group.last().endMs
            if (end > start) {
                out += TranscriptCue(start, end, group.joinToString(" ") { it.text }.trim())
            }
            group.clear()
            groupWords = 0
        }

        segments.forEach { segment ->
            val gap = if (group.isEmpty()) 0L else segment.startMs - group.last().endMs
            if (group.isNotEmpty() && gap >= NATIVE_PAUSE_BREAK_MS) flush()
            group += segment
            groupWords += wordCount(segment.text)
            val terminal = segment.text.trimEnd().lastOrNull()
            val sentenceEnd = terminal == '.' || terminal == '!' || terminal == '?'
            if (sentenceEnd || groupWords >= MAX_NATIVE_WORDS_PER_CUE) flush()
        }
        flush()
        return out
    }

    private fun wordCount(text: String): Int = NON_WHITESPACE.findAll(text).count().coerceAtLeast(1)

    fun parseNativeTimingJson(raw: String?): List<TimedTextSegment> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val text = item.optString("text").trim()
                    val start = item.optLong("startMs", -1L)
                    val end = item.optLong("endMs", -1L)
                    if (text.isNotBlank() && start >= 0L && end > start) {
                        add(TimedTextSegment(start, end, text))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }


    /**
     * Qwen3-ASR 0.6B currently returns no native word timestamps. Build short display
     * phrases and place them over speech-active PCM frames instead of stretching text uniformly
     * across the whole inference slice. This remains an estimate, but real silence no longer
     * consumes the same share of the timeline as spoken audio.
     */
    private fun allocateBySpeechActivity(
        lines: List<String>,
        startMs: Long,
        endMs: Long,
        samples: FloatArray,
        sampleStart: Int,
        sampleEnd: Int
    ): List<TranscriptCue> {
        if (lines.isEmpty() || endMs <= startMs) return emptyList()
        val from = sampleStart.coerceIn(0, samples.size)
        val until = sampleEnd.coerceIn(from, samples.size)
        if (until - from < SPEECH_FRAME_SAMPLES) return emptyList()

        val frameRms = mutableListOf<Double>()
        var offset = from
        while (offset < until) {
            val frameEnd = (offset + SPEECH_FRAME_SAMPLES).coerceAtMost(until)
            var sumSquares = 0.0
            for (i in offset until frameEnd) {
                val value = samples[i].toDouble()
                sumSquares += value * value
            }
            frameRms += kotlin.math.sqrt(sumSquares / (frameEnd - offset).coerceAtLeast(1))
            offset = frameEnd
        }
        if (frameRms.isEmpty()) return emptyList()

        val sorted = frameRms.sorted()
        val noiseFloor = sorted[(sorted.lastIndex * 20) / 100]
        val peak = sorted.last()
        if (peak <= 0.0) return emptyList()
        val threshold = maxOf(0.0015, noiseFloor + (peak - noiseFloor) * 0.10)
        val activeFrames = frameRms.indices.filter { frameRms[it] >= threshold }
        if (activeFrames.size < lines.size.coerceAtLeast(3)) return emptyList()

        val weights = lines.map { wordCount(it) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        var accumulated = 0
        val frameDurationMs = 20L
        val audioWindowMs = (endMs - startMs).coerceAtLeast(1L)
        val frameCount = frameRms.size.coerceAtLeast(1)
        val out = mutableListOf<TranscriptCue>()

        lines.forEachIndexed { index, line ->
            val startActivePos = ((activeFrames.size.toLong() * accumulated) / totalWeight)
                .toInt().coerceIn(0, activeFrames.lastIndex)
            accumulated += weights[index]
            val endActivePos = (((activeFrames.size.toLong() * accumulated) + totalWeight - 1) / totalWeight - 1)
                .toInt().coerceIn(startActivePos, activeFrames.lastIndex)
            val firstFrame = activeFrames[startActivePos]
            val lastFrame = activeFrames[endActivePos]
            val cueStart = startMs + (audioWindowMs * firstFrame) / frameCount
            var cueEnd = startMs + (audioWindowMs * (lastFrame + 1L)) / frameCount
            cueEnd = maxOf(cueEnd, cueStart + frameDurationMs).coerceAtMost(endMs)
            if (cueEnd > cueStart) out += TranscriptCue(cueStart, cueEnd, line)
        }
        return out
    }

    /** Short punctuation-aware phrases for engines that have text but no native timestamps. */
    private fun splitEstimatedPhrases(text: String, maxWords: Int = MAX_ESTIMATED_WORDS_PER_CUE): List<String> {
        val words = NON_WHITESPACE.findAll(text).toList()
        if (words.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var startWord = 0
        var count = 0
        words.forEachIndexed { index, match ->
            count++
            val terminal = match.value.trimEnd().lastOrNull()
            val punctuationBreak = count >= MIN_ESTIMATED_WORDS_BEFORE_PUNCT_BREAK &&
                (terminal == '.' || terminal == '!' || terminal == '?' || terminal == ',' || terminal == ';' || terminal == ':')
            if (punctuationBreak || count >= maxWords || index == words.lastIndex) {
                val startChar = words[startWord].range.first
                val endChar = match.range.last + 1
                out += text.substring(startChar, endChar).trim()
                startWord = index + 1
                count = 0
            }
        }
        return out.filter { it.isNotBlank() }
    }

    private fun allocate(lines: List<String>, startMs: Long, endMs: Long): List<TranscriptCue> {
        if (lines.isEmpty() || endMs <= startMs) return emptyList()
        val weights = lines.map { line -> NON_WHITESPACE.findAll(line).count().coerceAtLeast(1) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        val duration = endMs - startMs
        var accumulated = 0
        return lines.mapIndexedNotNull { index, line ->
            val cueStart = startMs + (duration * accumulated) / totalWeight
            accumulated += weights[index]
            var cueEnd = startMs + (duration * accumulated) / totalWeight
            if (index == lines.lastIndex) cueEnd = endMs
            if (cueEnd <= cueStart) cueEnd = (cueStart + 1L).coerceAtMost(endMs)
            if (cueEnd > cueStart) TranscriptCue(cueStart, cueEnd, line) else null
        }
    }

    private fun splitDisplayLines(text: String): List<String> {
        val clauses = text.replace("\r", "")
            .split(DISPLAY_LINE_BREAK)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val out = mutableListOf<String>()
        clauses.forEach { clause ->
            val words = NON_WHITESPACE.findAll(clause).toList()
            if (words.size <= MAX_WORDS_PER_CUE) {
                out += clause
            } else {
                var offset = 0
                while (offset < words.size) {
                    val endIndex = (offset + MAX_WORDS_PER_CUE).coerceAtMost(words.size) - 1
                    val startChar = words[offset].range.first
                    val endChar = words[endIndex].range.last + 1
                    out += clause.substring(startChar, endChar).trim()
                    offset = endIndex + 1
                }
            }
        }
        return out.ifEmpty { listOf(text.trim()) }
    }

    private val alignmentTokenRegex = Regex("[\\p{L}\\p{N}'’]+")

    private fun alignmentTokens(text: String): List<String> =
        alignmentTokenRegex.findAll(text).map { normalizeToken(it.value) }.toList()

    private fun normalizeToken(value: String): String = value.lowercase().replace('’', '\'')

    private fun canonical(text: String): String = text
        .lowercase()
        .replace('’', '\'')
        .replace(CANONICAL_NON_TOKEN, " ")
        .replace(CANONICAL_WHITESPACE, " ")
        .trim()
}
