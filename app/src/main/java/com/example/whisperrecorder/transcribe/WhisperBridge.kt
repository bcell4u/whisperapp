package com.example.whisperrecorder.transcribe

object WhisperBridge {
    init { System.loadLibrary("whisper_jni") }

    data class Recognition(
        val text: String,
        val timedSegments: List<TimedTextSegment>
    )

    external fun createContext(modelPath: String): Long
    external fun freeContext(handle: Long)
    private external fun transcribe(handle: Long, samples: FloatArray, language: String, threads: Int): String
    private external fun timingJson(handle: Long): String
    external fun cancel(handle: Long)
    external fun systemInfo(): String

    fun recognize(handle: Long, samples: FloatArray, language: String, threads: Int): Recognition {
        val text = transcribe(handle, samples, language, threads).trim()
        return Recognition(
            text = text,
            timedSegments = TranscriptTimelineBuilder.parseNativeTimingJson(timingJson(handle))
        )
    }
}
