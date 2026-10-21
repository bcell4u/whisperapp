package com.example.whisperrecorder.speaker

import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerFailureTextTest {
    @Test fun classLoadFailureHasActionableSummary() {
        val error = NoClassDefFoundError("com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization")
        assertTrue(SpeakerFailureText.summary(error).contains("NoClassDefFoundError"))
        assertTrue(SpeakerFailureText.summary(error).contains("diagnostics"))
    }
    @Test fun exceptionChainPreservesOriginalCauses() {
        val error = IllegalStateException("wrapper", UnsatisfiedLinkError("libonnxruntime_sherpa.so"))
        val chain = SpeakerFailureText.exceptionChain(error)
        assertTrue(chain.contains("java.lang.IllegalStateException: wrapper"))
        assertTrue(chain.contains("java.lang.UnsatisfiedLinkError: libonnxruntime_sherpa.so"))
    }
    @Test fun exceptionWithoutMessageIncludesType() {
        assertTrue(SpeakerFailureText.summary(IllegalArgumentException()).contains("IllegalArgumentException"))
    }
    @Test fun traceContainsFailureAndFrames() {
        assertTrue(SpeakerFailureText.stackTrace(IllegalStateException("test")).contains("IllegalStateException: test"))
    }
}
