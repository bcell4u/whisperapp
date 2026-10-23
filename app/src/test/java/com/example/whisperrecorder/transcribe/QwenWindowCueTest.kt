package com.example.whisperrecorder.transcribe

import org.junit.Assert.*
import org.junit.Test

class QwenWindowCueTest {
    @Test fun decodedWindowPhrasesStayNearTheirOwnAudioWindow() {
        val samples = FloatArray(25 * 16_000) { 0.4f }
        val windows = listOf(
            TimedTextSegment(0, 12_000, "This is the first spoken sentence in this audio window."),
            TimedTextSegment(11_000, 23_000, "The next spoken sentence belongs near eleven seconds."),
            TimedTextSegment(22_000, 25_000, "And the final words.")
        )
        val cues = requireNotNull(TranscriptTimelineBuilder.cuesForQwenWindows(
            cleanedText = windows.joinToString(" ") { it.text },
            chunkStartMs = 0, chunkEndMs = 25_000,
            nativeWindows = windows, minimumStartMs = 0,
            speechSamples = samples, speechSampleStart = 0, speechSampleEnd = samples.size
        ))
        assertTrue(cues.size >= 3)
        assertTrue(cues.filter { it.text.contains("first") }.all { it.endMs <= 11_000 })
        assertTrue(cues.filter { it.text.contains("next") }.all { it.startMs >= 11_000 })
        assertTrue(cues.filter { it.text.contains("final") }.all { it.startMs >= 22_000 })
        assertTrue(cues.zipWithNext().all { (a, b) -> a.endMs <= b.startMs })
    }

    @Test fun changedTranscriptRejectsUnsafeNativeWindowAlignment() {
        val windows = listOf(TimedTextSegment(0, 12_000, "One two three"))
        assertNull(TranscriptTimelineBuilder.cuesForQwenWindows(
            "One two", 0, 12_000, windows, 0,
            FloatArray(12 * 16_000), 0, 12 * 16_000
        ))
    }
}
