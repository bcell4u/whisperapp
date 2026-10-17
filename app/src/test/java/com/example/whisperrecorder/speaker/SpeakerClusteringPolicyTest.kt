package com.example.whisperrecorder.speaker

import org.junit.Assert.*
import org.junit.Test

class SpeakerClusteringPolicyTest {
    @Test fun autoIsDefaultAndUnknownValuesFailClosed() {
        assertEquals(-1, SpeakerClusteringPolicy.AUTO)
        assertEquals(-1, SpeakerClusteringPolicy.normalize(-1))
        assertEquals(-1, SpeakerClusteringPolicy.normalize(0))
        assertEquals(-1, SpeakerClusteringPolicy.normalize(7))
        assertEquals(-1, SpeakerClusteringPolicy.normalize(Int.MAX_VALUE))
    }

    @Test fun knownTwoSpeakerInterviewIsPreserved() {
        assertEquals(2, SpeakerClusteringPolicy.normalize(2))
        assertEquals("2", SpeakerClusteringPolicy.label(2))
        assertTrue(SpeakerClusteringPolicy.description(2).contains("2 expected speakers"))
    }

    @Test fun fullSelectionRangeRoundTrips() {
        assertEquals(listOf(-1, 1, 2, 3, 4, 5, 6), SpeakerClusteringPolicy.choices)
        SpeakerClusteringPolicy.choices.forEach { assertEquals(it, SpeakerClusteringPolicy.normalize(it)) }
        assertEquals("Automatic", SpeakerClusteringPolicy.label(SpeakerClusteringPolicy.AUTO))
    }

    @Test fun autoThresholdIsMoreConservativeThanPrevious() {
        assertTrue(SpeakerClusteringPolicy.AUTO_THRESHOLD > 0.5f)
        assertTrue(SpeakerClusteringPolicy.AUTO_THRESHOLD <= 1f)
    }
}
