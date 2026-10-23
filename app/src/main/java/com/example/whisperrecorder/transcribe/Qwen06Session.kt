package com.example.whisperrecorder.transcribe

import android.content.Context
import android.os.SharedMemory
import android.system.OsConstants
import com.example.whisperrecorder.qwen.Qwen3HybridClient
import org.json.JSONObject
import java.nio.ByteOrder
import java.util.concurrent.CancellationException

/**
 * Production Qwen3-ASR 0.6B: 16k mono PCM -> 12s QNN windows -> one four-thread
 * Q8_0 llama.cpp decoder process per recording slice. No CPU encoder fallback.
 * Model and worker scratch belong to app-private storage.
 */
internal class Qwen06Session(context: Context) : AsrSession {
    private val client = Qwen3HybridClient.connect(context.applicationContext)
    @Volatile private var cancelled = false
    // UI-only progress callback: called after each real QNN window and at decoder handoff.
    // Decoder token progress is not exposed, so the UI must not invent a percentage.
    @Volatile var onWindowProgress: ((completed: Int, total: Int, phase: String) -> Unit)? = null

    companion object {
        internal fun estimatedWindowCount(sampleCount: Int): Int {
            require(sampleCount > 0 && sampleCount <= 240 * 16_000)
            return 1 + ((sampleCount - 192_000).coerceAtLeast(0) + 175_999) / 176_000
        }
    }
    override val nativeInfo = "Qwen3-ASR 0.6B · ORT-QNN/HTP encoder + llama.cpp Q8_0 ARM64 dotprod (4 threads)"

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(!cancelled) { "Qwen transcription cancelled" }
        require(samples.isNotEmpty()) { "Qwen PCM chunk is empty" }
        require(samples.size <= 240 * 16000) { "Qwen PCM exceeds 240 second safety limit" }
        // Each 12s window steps forward by 11s. A 240s slice has at most 22 windows.
        // Keeping 1s overlap reduces errors at 12s boundaries, with bounded stitching.
        val prepared = client.prepare(overlapSeconds = 1)
        require(prepared.optBoolean("ok")) { "Qwen QNN encoder preparation failed" }
        val totalWindows = estimatedWindowCount(samples.size)
        onWindowProgress?.invoke(0, totalWindows, "Encoding")
        var next = 0
        var count = 0
        while (next < samples.size) {
            if (cancelled) throw CancellationException("Qwen transcription cancelled")
            val end = minOf(samples.size, next + 192_000)
            val sampleCount = end - next
            SharedMemory.create("qwen06-audio", sampleCount * Float.SIZE_BYTES).use { shared ->
                val map = shared.mapReadWrite().order(ByteOrder.nativeOrder())
                try { map.asFloatBuffer().put(samples, next, sampleCount) }
                finally { SharedMemory.unmap(map) }
                require(shared.setProtect(OsConstants.PROT_READ)) { "Cannot protect Qwen shared PCM" }
                val result = client.encode(shared, sampleCount)
                require(result.optBoolean("ok")) { "Qwen QNN encoder failed" }
                count++
                onWindowProgress?.invoke(count, totalWindows, "Encoding")
            }
            if (end == samples.size) break
            next += 176_000
        }
        require(count in 1..24 && count == totalWindows) { "Qwen window count outside safety bound" }
        if (cancelled) throw CancellationException("Qwen transcription cancelled")
        onWindowProgress?.invoke(count, totalWindows, "Decoding")
        val result: JSONObject = client.finish()
        require(result.optBoolean("ok")) { "Qwen llama.cpp decoding failed" }
        require(result.getInt("segments") == count && result.getInt("model_loads") == 1) {
            "Qwen decoder returned incomplete results"
        }
        val windows = result.getJSONArray("kept_windows")
        require(windows.length() == count) { "Qwen stitched window count mismatch" }
        val alignedWindows = (0 until count).mapNotNull { index ->
            val startSample = index * 176_000
            val endSample = minOf(samples.size, startSample + 192_000)
            val phrase = windows.getString(index).trim()
            if (phrase.isEmpty()) null else TimedTextSegment(
                startMs = startSample * 1000L / 16_000L,
                endMs = endSample * 1000L / 16_000L,
                text = phrase
            )
        }
        return AsrRecognition(
            text = result.getString("transcript"),
            timedSegments = alignedWindows,
            diagnostics = " backend=qwen06-qnn-llama decoderThreads=4" +
                " qnnWindows=$count qnnMs=${result.optLong("encoder_ms", -1L)}" +
                " preprocessingMs=${result.optLong("preprocess_ms", -1L)}" +
                " decoderMs=${result.optLong("decoder_ms", -1L)}" +
                " modelLoads=${result.getInt("model_loads")}" +
                " eosWindows=${result.optInt("eos_segments", 0)}",
            nativeWallMs = result.optLong("decoder_ms", -1L).takeIf { it >= 0L },
            nativeCpuMs = result.optLong("native_cpu_ms", -1L).takeIf { it >= 0L }
        )
    }

    override fun cancel() {
        cancelled = true
        client.cancelCurrent()
    }

    override fun close() = client.close()
}
