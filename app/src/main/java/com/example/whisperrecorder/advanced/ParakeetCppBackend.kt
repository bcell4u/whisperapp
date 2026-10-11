package com.example.whisperrecorder.advanced

import android.os.SharedMemory
import com.example.whisperrecorder.transcribe.TimedTextSegment
import org.json.JSONArray

internal object ParakeetCppNativeBridge {
    init { System.loadLibrary("parakeet_jni") }

    external fun nativeResolveDevice(requestedBackend: String): String
    external fun nativeOpen(modelPath: String, deviceName: String): Long
    external fun nativeTranscribeShared(handle: Long, pcmSharedMemory: SharedMemory, sampleCount: Int): String
    external fun nativeLastInferenceWallNs(): Long
    external fun nativeLastInferenceCpuNs(): Long
    external fun nativeClose(handle: Long)
}

/** parakeet.cpp v0.5.0 result parser shared by the production OpenCL worker/client. */
object ParakeetCppBackend {
    data class Recognition(
        val text: String,
        val timedSegments: List<TimedTextSegment>,
        val nativeWallMs: Long,
        val nativeCpuMs: Long
    )

    internal fun parseRecognition(raw: String, nativeWallMs: Long, nativeCpuMs: Long): Recognition {
        val batch = JSONArray(raw)
        val result = batch.optJSONObject(0)
            ?: error("parakeet.cpp returned an empty timestamp result")
        val text = result.optString("text").trim()
        val words = result.optJSONArray("words") ?: JSONArray()
        val timedSegments = buildList {
            for (i in 0 until words.length()) {
                val word = words.optJSONObject(i) ?: continue
                val value = word.optString("w").trim()
                val startSeconds = word.optDouble("start", -1.0)
                val endSeconds = word.optDouble("end", -1.0)
                if (value.isBlank() || startSeconds < 0.0 || endSeconds <= startSeconds) continue
                val startMs = (startSeconds * 1000.0).toLong().coerceAtLeast(0L)
                val endMs = (endSeconds * 1000.0).toLong().coerceAtLeast(startMs + 1L)
                add(TimedTextSegment(startMs, endMs, value))
            }
        }
        return Recognition(text, timedSegments, nativeWallMs, nativeCpuMs)
    }

}
