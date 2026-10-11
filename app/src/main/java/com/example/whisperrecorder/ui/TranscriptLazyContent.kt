package com.example.whisperrecorder.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import com.example.whisperrecorder.transcribe.TranscriptCueRange

/** A bounded slice of a long transcript plus cue ranges translated to local character offsets. */
internal data class TranscriptRenderBlock(
    val sourceStart: Int,
    val sourceEndExclusive: Int,
    val text: String,
    val cueRanges: List<TranscriptCueRange>
)

/**
 * Long transcripts are split only at cue boundaries (or whitespace when timing is unavailable),
 * keeping each Compose Text layout bounded. Short transcripts remain a single block and therefore
 * retain one simple text layout.
 */
internal object TranscriptRenderBlocks {
    private const val LAZY_THRESHOLD_CHARS = 12_000
    private const val TARGET_BLOCK_CHARS = 2_800

    fun build(transcript: String, cueRanges: List<TranscriptCueRange>): List<TranscriptRenderBlock> {
        if (transcript.isEmpty()) return listOf(TranscriptRenderBlock(0, 0, "", emptyList()))
        if (transcript.length <= LAZY_THRESHOLD_CHARS) return listOf(block(transcript, 0, transcript.length, cueRanges))

        val sorted = cueRanges.sortedBy { it.start }
        // Merge overlapping cue spans once. A monotonic cursor avoids scanning
        // thousands of word cues again for every long-transcript block.
        val mergedSpans = ArrayList<Pair<Int, Int>>()
        sorted.forEach { cue ->
            val first = cue.start.coerceIn(0, transcript.length)
            val last = cue.endExclusive.coerceIn(0, transcript.length)
            if (last > first) {
                val previous = mergedSpans.lastOrNull()
                if (previous != null && first < previous.second) {
                    mergedSpans[mergedSpans.lastIndex] = previous.first to maxOf(previous.second, last)
                } else {
                    mergedSpans += first to last
                }
            }
        }
        val boundaries = mutableListOf<Int>()
        var start = 0
        var spanIndex = 0
        while (transcript.length - start > TARGET_BLOCK_CHARS) {
            val target = (start + TARGET_BLOCK_CHARS).coerceAtMost(transcript.length)
            var end = nearestWhitespaceBoundary(transcript, start, target)
            while (spanIndex < mergedSpans.size && mergedSpans[spanIndex].second <= end) spanIndex++
            if (spanIndex < mergedSpans.size && mergedSpans[spanIndex].first < end) {
                end = mergedSpans[spanIndex].second
            }
            if (end <= start) break
            boundaries += end
            start = end
        }
        return fromBoundaries(transcript, sorted, boundaries)
    }

    private fun nearestWhitespaceBoundary(text: String, start: Int, target: Int): Int {
        if (target >= text.length) return text.length
        val searchFloor = (target - 500).coerceAtLeast(start + 1)
        for (i in target downTo searchFloor) {
            if (text[i - 1].isWhitespace()) return i
        }
        val searchCeiling = (target + 500).coerceAtMost(text.length)
        for (i in target until searchCeiling) {
            if (text[i].isWhitespace()) return i + 1
        }
        return target
    }

    private fun fromBoundaries(
        transcript: String,
        sortedCueRanges: List<TranscriptCueRange>,
        rawBoundaries: List<Int>
    ): List<TranscriptRenderBlock> {
        val boundaries = rawBoundaries.asSequence()
            .map { it.coerceIn(1, transcript.length) }
            .distinct()
            .sorted()
            .toMutableList()
        if (boundaries.lastOrNull() != transcript.length) boundaries += transcript.length

        val out = ArrayList<TranscriptRenderBlock>(boundaries.size)
        var start = 0
        var cueCursor = 0
        boundaries.forEach { end ->
            if (end > start) {
                while (cueCursor < sortedCueRanges.size && sortedCueRanges[cueCursor].endExclusive <= start) cueCursor++
                val localRanges = ArrayList<TranscriptCueRange>()
                var index = cueCursor
                while (index < sortedCueRanges.size && sortedCueRanges[index].start < end) {
                    val cue = sortedCueRanges[index]
                    if (cue.start >= start && cue.endExclusive <= end) {
                        localRanges += cue.copy(start = cue.start - start, endExclusive = cue.endExclusive - start)
                    }
                    index++
                }
                out += TranscriptRenderBlock(start, end, transcript.substring(start, end), localRanges)
            }
            start = end
        }
        return out.ifEmpty { listOf(block(transcript, 0, transcript.length, sortedCueRanges)) }
    }

    private fun block(
        transcript: String,
        start: Int,
        end: Int,
        cueRanges: List<TranscriptCueRange>
    ): TranscriptRenderBlock {
        val localRanges = cueRanges.asSequence()
            .filter { it.start >= start && it.endExclusive <= end }
            .map { it.copy(start = it.start - start, endExclusive = it.endExclusive - start) }
            .toList()
        return TranscriptRenderBlock(start, end, transcript.substring(start, end), localRanges)
    }
}

@Composable
internal fun TranscriptBlockText(
    block: TranscriptRenderBlock,
    activeCueIndex: Int,
    activeTextColor: Color,
    onCueTapped: (Int) -> Unit
) {
    var textLayout by remember(block.sourceStart, block.sourceEndExclusive) { mutableStateOf<TextLayoutResult?>(null) }
    val activeRange = remember(block.cueRanges, activeCueIndex) {
        block.cueRanges.firstOrNull { it.cueIndex == activeCueIndex }
    }
    // Accent the spoken word without a flashing background or changing text layout.
    // Preserve the outgoing opacity when speech advances faster than the color transition.
    var current by remember(block.sourceStart, block.text) { mutableStateOf<TranscriptCueRange?>(null) }
    var previous by remember(block.sourceStart, block.text) { mutableStateOf<TranscriptCueRange?>(null) }
    val fade = remember(block.sourceStart, block.text) { Animatable(1f) }
    var outgoingStrength by remember(block.sourceStart, block.text) { mutableFloatStateOf(1f) }
    LaunchedEffect(activeRange, block.text) {
        if (current != activeRange) {
            outgoingStrength = fade.value
            previous = current
            current = activeRange
            fade.snapTo(0f)
            fade.animateTo(1f, tween(135, easing = FastOutSlowInEasing))
            previous = null
        }
    }
    val baseText = MaterialTheme.colorScheme.onSurface
    val bodyStyle = MaterialTheme.typography.bodyLarge
    // A small inline type-size lift shares the existing 135 ms color animation.
    // No per-word view, background flash, or additional playback timer is needed.
    val normalSize = bodyStyle.fontSize
    val display = remember(block.text, current, previous, fade.value, outgoingStrength, activeTextColor, baseText, normalSize) {
        buildAnnotatedString {
            append(block.text)
            fun tint(range: TranscriptCueRange?, strength: Float) {
                if (range != null && range.start >= 0 && range.endExclusive <= block.text.length && range.endExclusive > range.start) {
                    addStyle(SpanStyle(
                        color = lerp(baseText, activeTextColor, strength),
                        fontSize = normalSize * (1f + 0.065f * strength)
                    ), range.start, range.endExclusive)
                }
            }
            tint(previous, outgoingStrength * (1f - fade.value))
            tint(current, fade.value)
        }
    }

    Text(
        text = display,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(block.cueRanges) {
                detectTapGestures { position ->
                    val layout = textLayout ?: return@detectTapGestures
                    if (block.cueRanges.isEmpty()) return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    block.cueRanges.firstOrNull { offset >= it.start && offset < it.endExclusive }
                        ?.let { onCueTapped(it.cueIndex) }
                }
            },
        style = bodyStyle,
        onTextLayout = { textLayout = it }
    )
}
