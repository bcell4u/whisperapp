package com.example.whisperrecorder.speaker

/** Independent speaker interval; these labels identify voices within this recording only. */
data class SpeakerInterval(val startMs: Long, val endMs: Long, val speaker: Int, val confidence: Float = -2.0f)
data class SpeakerTextCue(val startMs: Long, val endMs: Long, val text: String)

data class SpeakerTextLine(val speaker: Int?, val startMs: Long, val text: String)

/** Deliberately pure Kotlin: alignment must be tested without native models or Android. */
object SpeakerTranscript {
    fun align(cues: List<SpeakerTextCue>, intervals: List<SpeakerInterval>): List<SpeakerTextLine> {
        val encountered = LinkedHashMap<Int, Int>()
        val byStart = intervals.sortedBy { it.startMs }
        val active = ArrayList<SpeakerInterval>()
        var next = 0
        var lastStart = -1L
        return cues.filter { it.text.isNotBlank() && it.endMs > it.startMs }.map { cue ->
            // Monotonic word cues only inspect nearby speaker windows, not the whole lecture.
            val nearby = if (cue.startMs >= lastStart) {
                active.removeAll { it.endMs <= cue.startMs }
                while (next < byStart.size && byStart[next].startMs < cue.endMs) active += byStart[next++]
                active
            } else intervals // unusual out-of-order cue: correctness over speed
            lastStart = cue.startMs
            val speaker = SpeakerAttribution.bestSpeaker(cue, nearby)
            SpeakerTextLine(speaker?.let { encountered.getOrPut(it) { encountered.size } }, cue.startMs, cue.text.trim())
        }
    }

    /** Merge adjacent word labels for readable output; speaker changes still split mid-sentence. */
    fun groupWords(words: List<SpeakerTextLine>): List<SpeakerTextLine> {
        val out = ArrayList<SpeakerTextLine>()
        for (word in words) {
            val prev = out.lastOrNull()
            if (prev != null && prev.speaker == word.speaker &&
                word.startMs - prev.startMs < 8_000L && prev.text.split(' ').size < 18 &&
                !prev.text.endsWith('.') && !prev.text.endsWith('?') && !prev.text.endsWith('!')) {
                out[out.lastIndex] = prev.copy(text = prev.text + " " + word.text)
            } else out += word
        }
        return out
    }

    fun render(lines: List<SpeakerTextLine>, timestamps: Boolean, names: Map<Int, String> = emptyMap()): String {
        if (lines.isEmpty()) return ""
        return buildString {
            var lastSpeaker: Int? = null
            var first = true
            lines.forEach { line ->
                if (!first) append('\n')
                if (first || lastSpeaker != line.speaker) {
                    if (!first) append('\n')
                    val fallback = line.speaker?.let { "Speaker ${it + 1}" } ?: "Unknown speaker"
                    val name = line.speaker?.let { names[it]?.trim()?.takeIf(String::isNotEmpty) } ?: fallback
                    append(name).append(':').append('\n')
                }
                if (timestamps) append('[').append(timestamp(line.startMs)).append("] ")
                append(line.text)
                first = false
                lastSpeaker = line.speaker
            }
            append('\n')
        }
    }

    /** Renames only speaker headings in the derived output; the original ASR text is untouched. */
    fun renameLabels(transcript: String, names: Map<Int, String>): String {
        val speakerHeading = Regex("(?m)^Speaker ([1-9][0-9]*):$")
        return speakerHeading.replace(transcript) { match ->
            val index = match.groupValues[1].toIntOrNull()?.minus(1)
            val newName = index?.let(names::get)?.trim()
                ?.replace(Regex("[\\r\\n\\t:]"), " ")
                ?.take(60)?.trim()?.takeIf { it.isNotBlank() }
            if (newName != null) "$newName:" else match.value
        }
    }

    fun filename(originalName: String, collision: Int = 1): String {
        require(collision >= 1)
        val base = originalName.substringBeforeLast('.', originalName).ifBlank { "transcript" }
        return if (collision == 1) "$base-speaker.txt" else "$base ($collision)-speaker.txt"
    }

    private fun timestamp(ms: Long): String {
        val total = ms.coerceAtLeast(0) / 1000
        return "%02d:%02d:%02d".format(java.util.Locale.ROOT, total / 3600, total / 60 % 60, total % 60)
    }
}
