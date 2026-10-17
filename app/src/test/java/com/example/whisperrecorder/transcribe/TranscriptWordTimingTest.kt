package com.example.whisperrecorder.transcribe

import org.junit.Assert.*
import org.junit.Test

class TranscriptWordTimingTest {
    @Test fun nativeWordsBecomeIndependentPlaybackCues() {
        val words = listOf(TimedTextSegment(200, 300, "Hello"), TimedTextSegment(350, 450, "there!"))
        val cues = TranscriptTimelineBuilder.cuesForChunk("Hello there!", 40_000, 42_000,
            nativeSegments = words, wordByWord = true)
        assertEquals(listOf("Hello", "there!"), cues.map { it.text })
        assertEquals(40_200L, cues[0].startMs)
        assertEquals(40_350L, cues[1].startMs)
    }

    @Test fun overlapDedupTrimsNativeWordPrefix() {
        val words = listOf(TimedTextSegment(200, 300, "Hello"), TimedTextSegment(350, 450, "there"))
        val cues = TranscriptTimelineBuilder.cuesForChunk("there", 0, 2000,
            nativeSegments = words, wordByWord = true)
        assertEquals(listOf("there"), cues.map { it.text })
    }

    @Test fun speakerHeadingsAndOptionalTimestampsAreNotWords() {
        val text = "Speaker 1:\n[00:00:01] Hello\n\nSpeaker 2:\n[00:00:02] there\n"
        val cues = listOf(TranscriptCue(1000, 1100, "Hello"), TranscriptCue(2000, 2100, "there"))
        val map = TranscriptCueMapper.map(text, cues, skipSpeakerHeadings = true)
        assertEquals(2, map.size)
        assertEquals("Hello", text.substring(map[0].start, map[0].endExclusive))
        assertEquals("there", text.substring(map[1].start, map[1].endExclusive))
    }
}
