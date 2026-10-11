package com.example.whisperrecorder.ui

import com.example.whisperrecorder.transcribe.TranscriptCueRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptRenderBlocksTest {
    @Test
    fun shortTranscriptStaysSingleBlock() {
        val text = "hello world"
        val blocks = TranscriptRenderBlocks.build(text, listOf(TranscriptCueRange(0, 0, 5)))
        assertEquals(1, blocks.size)
        assertEquals(text, blocks.single().text)
        assertEquals(0, blocks.single().cueRanges.single().start)
    }

    @Test
    fun sparseCueRangesStillBoundLongUntimedTail() {
        val text = buildString { repeat(10_000) { append("word $it ") } }
        val ranges = listOf(TranscriptCueRange(0, 1_000, 1_025))
        val blocks = TranscriptRenderBlocks.build(text, ranges)
        assertTrue(blocks.size > 10)
        assertEquals(text, blocks.joinToString("") { it.text })
        assertTrue(blocks.all { it.text.length <= 3_300 })
        assertEquals(1, blocks.sumOf { it.cueRanges.size })
    }

    @Test
    fun longCueIsNeverSplitAcrossBlocks() {
        val text = "word ".repeat(4_000)
        val ranges = listOf(TranscriptCueRange(0, 2_300, 6_000))
        val blocks = TranscriptRenderBlocks.build(text, ranges)
        assertEquals(text, blocks.joinToString("") { it.text })
        assertEquals(1, blocks.sumOf { it.cueRanges.size })
    }

    @Test
    fun longTranscriptIsBoundedAndLossless() {
        val text = buildString { repeat(2_000) { append("sentence $it. ") } }
        val blocks = TranscriptRenderBlocks.build(text, emptyList())
        assertTrue(blocks.size > 1)
        assertEquals(text, blocks.joinToString(separator = "") { it.text })
        assertTrue(blocks.dropLast(1).all { it.text.length <= 3_300 })
    }

    @Test
    fun overlappingUnsortedCuesRemainInOneBlockAndKeepLocalOffsets() {
        val text = "word ".repeat(8_000)
        val ranges = listOf(
            TranscriptCueRange(2, 3_500, 5_500),
            TranscriptCueRange(0, 2_400, 3_700),
            TranscriptCueRange(3, 8_100, 8_130),
            TranscriptCueRange(1, 5_400, 6_400)
        )
        val blocks = TranscriptRenderBlocks.build(text, ranges)
        assertEquals(text, blocks.joinToString("") { it.text })
        assertEquals(ranges.size, blocks.sumOf { it.cueRanges.size })
        ranges.forEach { cue ->
            val matching = blocks.single { block -> block.cueRanges.any { it.cueIndex == cue.cueIndex } }
            val local = matching.cueRanges.single { it.cueIndex == cue.cueIndex }
            assertEquals(cue.start, matching.sourceStart + local.start)
            assertEquals(cue.endExclusive, matching.sourceStart + local.endExclusive)
        }
    }

    @Test
    fun manyCuesAndLongTranscriptStayLossless() {
        val text = "word ".repeat(30_000)
        val cues = (0 until 30_000 step 3).map { i ->
            TranscriptCueRange(i, i * 5, i * 5 + 4)
        }
        val blocks = TranscriptRenderBlocks.build(text, cues)
        assertEquals(text, blocks.joinToString("") { it.text })
        assertEquals(cues.size, blocks.sumOf { it.cueRanges.size })
        assertTrue(blocks.size > 20)
    }
}
