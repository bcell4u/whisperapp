package com.example.whisperrecorder.audio

import android.media.MediaRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophoneProcessingSourcesTest {
    @Test fun allSupportedSourcesHaveTheExpectedAndroidRouting() {
        assertEquals(MediaRecorder.AudioSource.MIC, MicrophoneProcessing.sourceValue(MicrophoneProcessing.SOURCE_MIC))
        assertEquals(MediaRecorder.AudioSource.VOICE_RECOGNITION, MicrophoneProcessing.sourceValue(MicrophoneProcessing.SOURCE_VOICE_RECOGNITION))
        assertEquals(MediaRecorder.AudioSource.CAMCORDER, MicrophoneProcessing.sourceValue(MicrophoneProcessing.SOURCE_CAMCORDER))
    }

    @Test fun camcorderIsAnAvailableLabeledSource() {
        assertTrue(MicrophoneProcessing.SOURCE_CAMCORDER in MicrophoneProcessing.sourceIds)
        assertEquals("Camcorder", MicrophoneProcessing.sourceLabel(MicrophoneProcessing.SOURCE_CAMCORDER))
    }

    @Test fun existingPresetSourcesRemainUnchanged() {
        assertEquals(MicrophoneProcessing.SOURCE_MIC, MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_DEFAULT).source)
        assertEquals(MicrophoneProcessing.SOURCE_VOICE_RECOGNITION, MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_LECTURE).source)
        assertEquals(MicrophoneProcessing.SOURCE_VOICE_RECOGNITION, MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_MEETING).source)
        assertEquals(MicrophoneProcessing.SOURCE_MIC, MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_CLOSE).source)
        assertEquals(MicrophoneProcessing.SOURCE_VOICE_RECOGNITION, MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_SPEAKER).source)
    }
}
