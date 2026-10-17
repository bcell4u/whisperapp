package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptPlaybackTest {
    private val cues = listOf(
        TranscriptCue(0, 1_000, "one"),
        TranscriptCue(1_000, 2_000, "two"),
        TranscriptCue(2_500, 3_000, "three")
    )

    @Test
    fun cueLookupHonorsBoundariesAndGaps() {
        assertEquals(0, cueIndexAt(cues, 0))
        assertEquals(0, cueIndexAt(cues, 999))
        assertEquals(1, cueIndexAt(cues, 1_000))
        assertEquals(-1, cueIndexAt(cues, 2_250))
        assertEquals(2, cueIndexAt(cues, 2_500))
        assertEquals(-1, cueIndexAt(cues, 3_000))
    }
}
