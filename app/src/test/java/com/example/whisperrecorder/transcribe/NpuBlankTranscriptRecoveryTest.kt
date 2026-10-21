package com.example.whisperrecorder.transcribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NpuBlankTranscriptRecoveryTest {
    @Test fun markerIsTimeLocalizedAtFailurePoint() {
        assertEquals("[No transcription returned for 00:59:32–01:00:12]",
            NpuBlankTranscriptRecovery.marker(3_572_000L, 3_612_000L))
    }

    @Test fun markerUsesMinuteClockForShortRecordings() {
        assertEquals("[No transcription returned for 00:00:40–00:01:20]",
            NpuBlankTranscriptRecovery.marker(40_000L, 80_000L))
    }

    @Test fun markerRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException::class.java) {
            NpuBlankTranscriptRecovery.marker(10_000L, 10_000L)
        }
    }
}
