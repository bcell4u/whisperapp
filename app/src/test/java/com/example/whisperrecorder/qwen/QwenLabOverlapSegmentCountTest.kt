package com.example.whisperrecorder.qwen

import com.example.whisperrecorder.transcribe.PcmDecoder
import org.junit.Assert.assertEquals
import org.junit.Test

class QwenLabOverlapSegmentCountTest {
    @Test fun originalLectureAddsOnlyOne12SecondWindow() {
        assertEquals(19, PcmDecoder.countSegmentsByDuration(220_540L, 12, 0))
        assertEquals(20, PcmDecoder.countSegmentsByDuration(220_540L, 12, 1))
    }

    @Test fun longestSupportedLectureStaysUnder24SegmentLimit() {
        assertEquals(20, PcmDecoder.countSegmentsByDuration(240_000L, 12, 0))
        assertEquals(22, PcmDecoder.countSegmentsByDuration(240_000L, 12, 1))
    }

    @Test fun shortRecordingNeedsOnlyOneWindow() {
        assertEquals(1, PcmDecoder.countSegmentsByDuration(7_000L, 12, 1))
    }
}
