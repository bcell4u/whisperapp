package com.example.whisperrecorder.advanced

import com.example.whisperrecorder.transcribe.TimedTextSegment
import com.example.whisperrecorder.transcribe.TranscriptTimelineBuilder

internal object TranscribeCppBridge {
    init { System.loadLibrary("transcribe_cpp_jni") }

    external fun nativeOpen(modelPath: String, threads: Int): Long
    external fun nativeTranscribe(handle: Long, samples: FloatArray, language: String): String
    external fun nativeTimingJson(handle: Long): String
    external fun nativeTokenCount(handle: Long): Int
    external fun nativeDetectedLanguage(handle: Long): String
    external fun nativeCancel(handle: Long)
    external fun nativeVersion(): String
    external fun nativeClose(handle: Long)
}

class TranscribeCppBackend private constructor(private var handle: Long) : AutoCloseable {
    data class Recognition(
        val text: String,
        val tokenCount: Int,
        val detectedLanguage: String,
        val timedSegments: List<TimedTextSegment>
    )

    fun transcribe(samples: FloatArray, language: String): Recognition {
        check(handle != 0L) { "transcribe.cpp backend is closed" }
        val text = TranscribeCppBridge.nativeTranscribe(handle, samples, language).trim()
        return Recognition(
            text = text,
            tokenCount = TranscribeCppBridge.nativeTokenCount(handle),
            detectedLanguage = TranscribeCppBridge.nativeDetectedLanguage(handle),
            timedSegments = TranscriptTimelineBuilder.parseNativeTimingJson(TranscribeCppBridge.nativeTimingJson(handle))
        )
    }

    fun cancel() {
        val current = handle
        if (current != 0L) TranscribeCppBridge.nativeCancel(current)
    }

    override fun close() {
        val current = handle
        handle = 0L
        if (current != 0L) TranscribeCppBridge.nativeClose(current)
    }

    companion object {
        fun open(modelPath: String, threads: Int): TranscribeCppBackend {
            val handle = TranscribeCppBridge.nativeOpen(modelPath, threads)
            check(handle != 0L) { "transcribe.cpp could not load the selected model" }
            return TranscribeCppBackend(handle)
        }

        fun version(): String = runCatching { TranscribeCppBridge.nativeVersion() }.getOrDefault("unknown")
    }
}
