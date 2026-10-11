package com.example.whisperrecorder.transcribe

import android.content.Context
import android.os.SharedMemory
import android.system.OsConstants
import com.example.whisperrecorder.qwen.Qwen3HybridClient
import com.example.whisperrecorder.npu.Qwen3HybridPackageStore
import org.json.JSONObject
import java.nio.ByteOrder

/** Experimental Qwen QNN encoder + CPU ONNX decoder. Never silently falls back to GGUF. */
internal class Qwen3HybridSession(context: Context) : AsrSession {
    private val app = context.applicationContext
    private val client = Qwen3HybridClient.connect(context)
    @Volatile private var cancelled = false
    override val nativeInfo: String
    init {
        try {
            val outcome = client.prepare()
            require(outcome.optBoolean("ok")) {
                "Qwen hybrid initialization failed: ${outcome.optString("error")}. This does not affect existing backends."
            }
            nativeInfo = "EXPERIMENTAL Qwen3 0.6B · QNN FP16 encoder + CPU FP32 ONNX decoder; ${outcome.optString("cpu_fallback")}; golden error ${outcome.optDouble("golden_max_abs")}"
        } catch (t: Throwable) {
            client.close()
            throw t
        }
    }
    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(!cancelled) { "Qwen hybrid cancelled" }
        require(samples.size in 1..192000) { "Qwen3 hybrid fixed window is 12s / 192000 samples max" }
        return SharedMemory.create("whisperrecorder-qwen3-hybrid-pcm", samples.size * 4).use { shared ->
            val writable = shared.mapReadWrite().order(ByteOrder.nativeOrder())
            try { writable.asFloatBuffer().put(samples) } finally { SharedMemory.unmap(writable) }
            require(shared.setProtect(OsConstants.PROT_READ))
            val response: JSONObject = client.transcribe(shared, samples.size)
            require(response.optBoolean("ok")) {
                "Qwen hybrid inference failed: ${response.optString("error")}. CPU fallback is disabled for the encoder."
            }
            AsrRecognition(text = response.getString("transcript"),
                diagnostics = " qualification=experimental qwen3Hybrid=true encoderGraphCalls=${response.optInt("encoder_graph_calls")} encoderMs=${response.optLong("encoder_ms")} decoderMs=${response.optLong("decoder_ms")}")
        }
    }
    override fun cancel() { cancelled = true; client.cancelCurrent() }
    override fun close() {
        client.close()
        check(Qwen3HybridPackageStore.clearTemporaryTestFiles(app)) {
            "Qwen scratch wrapper cleanup did not verify; use manual cleanup in Models & downloads"
        }
    }
}
