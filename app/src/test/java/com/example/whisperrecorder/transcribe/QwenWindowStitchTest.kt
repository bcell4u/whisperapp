package com.example.whisperrecorder.transcribe

import com.example.whisperrecorder.qwen.QwenTranscriptStitcher
import org.junit.Assert.*
import org.junit.Test

class QwenWindowStitchTest {
    @Test fun eachWindowRetainsOnlyTheWordsContributedToTheFinalTranscript() {
        val result = QwenTranscriptStitcher.stitch(listOf(
            "The patient reports chronic neck pain and limited range of motion",
            "limited range of motion without new injury or a recent fall"
        ), overlapSeconds = 1)
        assertEquals(4, result.removedDuplicateWords)
        assertTrue(result.keptWindows[1].startsWith("without"))
        assertEquals(result.transcript, result.keptWindows.joinToString(" ").trim())
    }

    @Test fun oneWindowAndNoOverlapStillExposeExactDecodedWindows() {
        val segments = listOf("First sentence", "Second sentence")
        val result = QwenTranscriptStitcher.stitch(segments, overlapSeconds = 0)
        assertEquals(segments, result.keptWindows)
        assertEquals("First sentence Second sentence", result.transcript)
    }
}
