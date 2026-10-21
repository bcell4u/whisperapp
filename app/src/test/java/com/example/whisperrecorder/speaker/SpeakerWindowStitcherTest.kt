package com.example.whisperrecorder.speaker

import org.junit.Assert.*
import org.junit.Test

class SpeakerWindowStitcherTest {
    @Test fun stableIdentityWhenLocalLabelsSwapBetweenWindows() {
        val stitcher = SpeakerWindowStitcher(2)
        stitcher.map(listOf(SpeakerInterval(0, 20_000, 0), SpeakerInterval(20_000, 40_000, 1)),
            mapOf(0 to floatArrayOf(1f, 0f), 1 to floatArrayOf(0f, 1f)))
        val next = stitcher.map(listOf(SpeakerInterval(40_000, 60_000, 0), SpeakerInterval(60_000, 80_000, 1)),
            mapOf(0 to floatArrayOf(0.02f, 0.98f), 1 to floatArrayOf(0.99f, 0.01f)))
        assertEquals(listOf(1, 0), next.map { it.speaker })
    }

    @Test fun sharedBoundaryCanMatchSpeakerWithoutAnEmbedding() {
        val stitcher = SpeakerWindowStitcher(2)
        stitcher.map(listOf(SpeakerInterval(0, 160_000, 0), SpeakerInterval(160_000, 180_000, 1)),
            mapOf(0 to floatArrayOf(1f, 0f), 1 to floatArrayOf(0f, 1f)))
        val next = stitcher.map(listOf(SpeakerInterval(160_000, 175_000, 0), SpeakerInterval(175_000, 210_000, 1)), emptyMap())
        assertEquals(1, next[0].speaker)
        assertEquals(-1, next[1].speaker)
    }

    @Test fun neverExceedsFixedCountAndDoesNotInventIdentityOnAmbiguity() {
        val stitcher = SpeakerWindowStitcher(2)
        val result = stitcher.map((0..3).map { SpeakerInterval(it * 1_000L, (it + 1) * 1_000L, it) },
            (0..3).associateWith { speaker -> FloatArray(4) { if (it == speaker) 1f else 0f } })
        assertEquals(listOf(0, 1, -1, -1), result.map { it.speaker })
    }

    @Test fun initialFastConversationCanBootstrapWithoutLongVoiceSamples() {
        val stitcher = SpeakerWindowStitcher(2)
        val first = stitcher.map(listOf(SpeakerInterval(0, 700, 0), SpeakerInterval(700, 1_500, 1)), emptyMap())
        assertEquals(listOf(0, 1), first.map { it.speaker })
    }

    @Test fun overlappingBoundaryIsEmittedExactlyOnce() {
        val merger = SpeakerWindowMerge()
        assertTrue(merger.push(0, 180_000, listOf(SpeakerInterval(150_000, 180_000, 0))).isEmpty())
        val complete = merger.push(150_000, 330_000,
            listOf(SpeakerInterval(150_000, 180_000, 0), SpeakerInterval(180_000, 200_000, 1)))
        assertEquals(listOf(SpeakerInterval(150_000, 165_000, 0)), complete)
        assertEquals(listOf(SpeakerInterval(165_000, 180_000, 0), SpeakerInterval(180_000, 200_000, 1)), merger.finish())
    }

    @Test fun onlyCleanContiguousSpeechWithinWindowIsUsedForEmbeddings() {
        val samples = FloatArray(180 * 16_000)
        val voice = SpeakerWindowAudio.representative(samples, 150_000,
            listOf(SpeakerInterval(160_000, 180_000, 0)), 0)
        assertNotNull(voice)
        assertEquals(9 * 16_000, voice!!.size)
        assertNull(SpeakerWindowAudio.representative(samples, 150_000,
            listOf(SpeakerInterval(160_000, 160_700, 0)), 0))
    }
}
