package com.example.whisperrecorder.transcribe

import android.content.Context
import android.os.SharedMemory
import android.system.OsConstants
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.NpuRuntimeClient
import com.example.whisperrecorder.npu.NpuRuntimeException
import org.json.JSONObject
import java.nio.ByteOrder
import com.example.whisperrecorder.advanced.ParakeetCppBackend
import com.example.whisperrecorder.advanced.TranscribeCppBackend
import com.example.whisperrecorder.gpu.ParakeetOpenClProcessClient

internal data class AsrRecognition(
    val text: String,
    val timedSegments: List<TimedTextSegment> = emptyList(),
    val diagnostics: String = "",
    val nativeWallMs: Long? = null,
    val nativeCpuMs: Long? = null
)

internal interface AsrPreparedChunk : AutoCloseable {
    val preparationWallMs: Long
}

/** Runtime-neutral transcription session so the service has one lifecycle/cancellation path. */
internal interface AsrSession : AutoCloseable {
    val nativeInfo: String
    fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition
    fun prepare(samples: FloatArray, language: String, threads: Int): AsrPreparedChunk? = null
    fun transcribePrepared(prepared: AsrPreparedChunk, language: String, threads: Int): AsrRecognition =
        error("Prepared transcription is not supported by this backend")
    fun cancel() = Unit
}

internal object AsrSessionFactory {
    fun open(context: Context, engine: AsrEngine, modelPath: String, threads: Int): AsrSession = when (engine) {
        AsrEngine.WHISPER -> WhisperSession(modelPath)
        // Existing GPU path remains untouched; Qualcomm NPU is a separate selectable backend.
        AsrEngine.PARAKEET, AsrEngine.OMI -> ParakeetOpenClSession(context, modelPath)
        AsrEngine.PARAKEET_NPU -> NpuParakeetSession(context)
        AsrEngine.OMI_NPU -> OmiNpuSession(context)
        AsrEngine.QWEN3 -> TranscribeCppSession(modelPath, threads)
        AsrEngine.QWEN3_HYBRID -> Qwen3HybridSession(context)
    }
}


private class ParakeetOpenClSession(context: Context, modelPath: String) : AsrSession {
    private val client = ParakeetOpenClProcessClient.connect(context)
    private var handle = client.open(modelPath)
    override val nativeInfo: String = client.runtimeDiagnostics()

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(handle != 0L) { "Parakeet-family OpenCL session is closed" }
        val remote = client.transcribe(handle, samples)
        val result = ParakeetCppBackend.parseRecognition(remote.resultJson, remote.nativeWallMs, remote.nativeCpuMs)
        return AsrRecognition(
            text = result.text,
            timedSegments = result.timedSegments,
            diagnostics = " timedWords=${result.timedSegments.size} backend=${remote.resolvedDevice} sharedMemoryPrepMs=${remote.sharedMemoryPrepMs} ipcRoundTripMs=${remote.ipcRoundTripMs} pcmBytes=${remote.pcmBytes} readOnly=${remote.readOnlyProtectionApplied}",
            nativeWallMs = result.nativeWallMs,
            nativeCpuMs = result.nativeCpuMs
        )
    }

    override fun cancel() {
        val current = handle
        if (current != 0L) client.cancel(current)
    }

    override fun close() {
        val current = handle
        handle = 0L
        if (current != 0L) client.closeSession(current)
        client.close()
    }
}


private class NpuParakeetSession(context: Context) : AsrSession {
    private val app = context.applicationContext
    private var info = "ORT-QNN Qualcomm NPU"
    @Volatile private var cancelled = false
    @Volatile private var recoveredWorker = false
    @Volatile private var client: NpuRuntimeClient = openClient()

    private inner class PreparedChunk(
        val id: Long,
        val samples: FloatArray,
        override val preparationWallMs: Long
    ) : AsrPreparedChunk {
        @Volatile private var consumed = false

        fun consume() {
            check(!consumed) { "Prepared NPU chunk has already been consumed" }
            consumed = true
        }

        override fun close() {
            if (!consumed) {
                consumed = true
                client.releasePreparedChunk(id)
            }
        }
    }

    override val nativeInfo: String get() = info

    private fun openClient(): NpuRuntimeClient {
        val connected = NpuRuntimeClient.connect(app)
        try {
            val bootstrap = connected.bootstrap()
            require(bootstrap.optBoolean("ok", false)) { bootstrap.optString("error", "ORT-QNN bootstrap failed") }
            val prepare = connected.prepareRuntime()
            require(prepare.optBoolean("ok", false)) { prepare.optString("error", "NPU runtime preparation failed") }
            require(prepare.optLong("persistent_duplicate_model_bytes", -1L) == 0L) { "Persistent NPU model duplication returned" }
            require(prepare.optLong("temporary_context_bytes", -1L) == 0L) { "Transient QNN context remains after session load" }
            info = "ORT-QNN HTP sessions=${prepare.optInt("session_count", 0)} topology=${prepare.optString("decoder_topology", "unknown")} storage=${prepare.optString("storage_mode", "unknown")} revision=${NpuPackageStore.REVISION.take(12)}"
            return connected
        } catch (t: Throwable) {
            connected.close()
            throw t
        }
    }

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(!cancelled) { "NPU transcription is cancelled" }
        require(samples.isNotEmpty()) { "NPU transcription received empty PCM" }
        val durationMs = (samples.size * 1000L) / 16_000L
        return runChunk(samples, durationMs, allowRecovery = true)
    }

    override fun prepare(samples: FloatArray, language: String, threads: Int): AsrPreparedChunk {
        check(!cancelled) { "NPU transcription is cancelled" }
        require(samples.isNotEmpty()) { "NPU preprocessing received empty PCM" }
        val durationMs = (samples.size * 1000L) / 16_000L
        return withSharedPcm(samples, "whisperrecorder-npu-prepare") { shared ->
            val result = client.prepareChunk(shared, samples.size, durationMs)
            require(result.optBoolean("ok", false)) { result.optString("error", "NPU chunk preprocessing failed") }
            val preparedId = result.optLong("prepared_id", -1L)
            require(preparedId > 0L) { "NPU worker returned an invalid prepared chunk id" }
            PreparedChunk(
                id = preparedId,
                samples = samples,
                preparationWallMs = result.optLong("preparation_wall_ms", -1L).coerceAtLeast(0L)
            )
        }
    }

    override fun transcribePrepared(prepared: AsrPreparedChunk, language: String, threads: Int): AsrRecognition {
        check(!cancelled) { "NPU transcription is cancelled" }
        val chunk = prepared as? PreparedChunk ?: error("Prepared chunk belongs to a different ASR backend")
        chunk.consume()
        return try {
            val result = client.transcribePreparedChunk(chunk.id)
            if (!result.optBoolean("ok", false)) {
                val durationMs = (chunk.samples.size * 1000L) / 16_000L
                return runChunk(chunk.samples, durationMs, allowRecovery = true)
            }
            recognitionFromResult(result)
        } catch (t: Throwable) {
            if (cancelled) throw java.util.concurrent.CancellationException("NPU transcription cancelled")
            val recoverable = t is NpuRuntimeException || t.message?.contains("worker", ignoreCase = true) == true
            if (!recoverable) throw t
            runCatching { client.close() }
            client = openClient()
            recoveredWorker = true
            val durationMs = (chunk.samples.size * 1000L) / 16_000L
            runChunk(chunk.samples, durationMs, allowRecovery = false)
        }
    }

    private fun runChunk(samples: FloatArray, durationMs: Long, allowRecovery: Boolean): AsrRecognition {
        try {
            return withSharedPcm(samples, "whisperrecorder-npu-transcribe") { shared ->
                val result = client.transcribeChunk(shared, samples.size, durationMs)
                require(result.optBoolean("ok", false)) { result.optString("error", "NPU chunk inference failed") }
                recognitionFromResult(result)
            }
        } catch (t: Throwable) {
            if (cancelled) throw java.util.concurrent.CancellationException("NPU transcription cancelled")
            val recoverable = allowRecovery && (t is NpuRuntimeException || t.message?.contains("worker", ignoreCase = true) == true)
            if (!recoverable) throw t
            runCatching { client.close() }
            client = openClient()
            recoveredWorker = true
            return runChunk(samples, durationMs, allowRecovery = false)
        }
    }

    private fun <T> withSharedPcm(samples: FloatArray, name: String, block: (SharedMemory) -> T): T =
        SharedMemory.create(name, samples.size * Float.SIZE_BYTES).use { shared ->
            val mapped = shared.mapReadWrite().order(ByteOrder.nativeOrder())
            try { mapped.asFloatBuffer().put(samples) } finally { SharedMemory.unmap(mapped) }
            require(shared.setProtect(OsConstants.PROT_READ)) { "Unable to make NPU PCM SharedMemory read-only" }
            block(shared)
        }

    private fun recognitionFromResult(result: JSONObject): AsrRecognition {
        require(result.opt("transcript") is String) { "NPU response is missing its transcript field" }
        val text = result.optString("transcript")
        // A successful inference can legitimately decode no speech (silence, special tokens,
        // or a transient TDT blank output). Let the service retry and preserve earlier chunks.
        // The worker's explicit ok=false and transport errors remain fatal.
        return AsrRecognition(
            text = text,
            timedSegments = TranscriptTimelineBuilder.parseNativeTimingJson(result.optJSONArray("timed_words")?.toString()),
            diagnostics = telemetryText(result) + " blankTranscript=${text.isBlank()}" +
                " timedWords=${result.optJSONArray("timed_words")?.length() ?: 0}" +
                " timingStatus=${result.optString("timing_status", "missing")}" +
                " timingError=${result.optString("timing_error", "none").take(180)}",
            nativeWallMs = result.optLong("critical_path_ms", result.optLong("total_ms", -1L)).takeIf { it >= 0L }
        ).also { recoveredWorker = false }
    }

    private fun telemetryText(result: JSONObject): String =
        " npuProfile=${result.optString("performance_profile", "provider_default")}" +
            " decoderTopology=${result.optString("decoder_topology", "unknown")}" +
            " linkedDecoder=${result.optBoolean("linked_decoder_active", false)}" +
            " sharedEp=${result.optBoolean("linked_decoder_shared_ep_contexts", false)}" +
            " qnnContexts=${result.optInt("qnn_context_binary_count", 0)}" +
            " criticalMs=${result.optLong("critical_path_ms", result.optLong("total_ms", -1L))}" +
            " preprocessMs=${result.optLong("preprocessing_ms", -1L)}" +
            " preprocessOverlap=${result.optBoolean("preprocessing_overlapped", false)}" +
            " preparedAgeMs=${result.optLong("prepared_age_ms", -1L)}" +
            " encoderMs=${result.optLong("encoder_ms", -1L)}" +
            " projectorMs=${result.optLong("projector_ms", -1L)}" +
            " decoderMs=${result.optLong("decoder_ms", -1L)}" +
            " decoderHotLoop=${result.optString("decoder_hot_loop", "reused_view_bulk_copy_single_validation")}" +
            " jointSteps=${result.optInt("joint_steps", 0)}" +
            " tokens=${result.optInt("emitted_token_count", 0)}" +
            " validFrames=${result.optInt("valid_encoded_frames", 0)}" +
            " recoveredWorker=$recoveredWorker"


    override fun cancel() {
        cancelled = true
        client.cancelCurrent()
    }

    override fun close() {
        runCatching { client.closeRuntime() }
        runCatching { client.close() }
    }
}

/** Explicit experimental Omi NPU route; Parakeet NPU and Omi OpenCL remain independent. */
private class OmiNpuSession(context: Context) : AsrSession {
    private val app = context.applicationContext
    @Volatile private var cancelled = false
    private var worker = connectAndPrepare()
    override val nativeInfo: String = "ORT-QNN Omi Med v2; 3 QNN contexts; 12s windows; approximate frame timestamps; accuracy unqualified"

    private fun connectAndPrepare(): NpuRuntimeClient {
        val client = NpuRuntimeClient.connect(app)
        try {
            val boot = client.bootstrap()
            require(boot.optBoolean("ok")) { "Omi NPU bootstrap: ${boot.optString("error")} " }
            val info = client.prepareOmiRuntime()
            require(info.optBoolean("ok")) { "Omi NPU prepare: ${info.optString("error")} stage=${info.optString("operation")} " }
            require(info.optInt("session_count") == 3) { "Omi NPU needs 3 sessions: $info" }
            require(info.optLong("temporary_context_bytes", -1) == 0L) { "Omi staged contexts were not deleted" }
            return client
        } catch (t: Throwable) {
            client.close()
            throw t
        }
    }

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(!cancelled) { "Omi NPU transcription cancelled" }
        require(samples.isNotEmpty() && samples.size <= 192_000) {
            "Omi NPU requires 1..192000 samples per 12-second chunk; got ${samples.size}"
        }
        return SharedMemory.create("whisperrecorder-omi-npu-pcm", samples.size * Float.SIZE_BYTES).use { shared ->
            val mapped = shared.mapReadWrite().order(ByteOrder.nativeOrder())
            try { mapped.asFloatBuffer().put(samples) } finally { SharedMemory.unmap(mapped) }
            require(shared.setProtect(OsConstants.PROT_READ)) { "Failed protecting Omi NPU PCM shared memory" }
            val json = worker.transcribeOmiChunk(shared, samples.size, samples.size * 1000L / 16_000L)
            require(json.optBoolean("ok", false)) {
                "Omi NPU inference error operation=${json.optString("operation")} type=${json.optString("error_type")}: ${json.optString("error")}"
            }
            AsrRecognition(
                text = json.getString("transcript"),
                timedSegments = TranscriptTimelineBuilder.parseNativeTimingJson(json.optJSONArray("timed_words")?.toString()),
                diagnostics = " backend=omi_npu qualification=experimental " +
                    "preprocessMs=${json.optLong("preprocessing_ms")} encoderMs=${json.optLong("encoder_ms")} " +
                    "decoderMs=${json.optLong("decoder_ms")} jointSteps=${json.optInt("joint_steps")} " +
                    "tokens=${json.optInt("emitted_token_count")} blanks=${json.optInt("blank_decisions")} zeroDurations=${json.optInt("zero_duration_decisions")} frames=${json.optInt("valid_encoded_frames")} " +
                    "stop=${json.optString("tdt_stop_reason")} symbolGuards=${json.optInt("tdt_symbol_limit_advances")} " +
                    "featureContract=${json.optString("front_end_contract")} validMelFrames=${json.optInt("valid_feature_frames")} maskCounts=${json.optString("feature_mask_counts")} " +
                    "pcmRms=${json.optDouble("pcm_rms")} pcmPeak=${json.optDouble("pcm_peak")} pcmClipPct=${json.optDouble("pcm_clipped_pct")} " +
                    "featureRms=${json.optDouble("feature_rms")} featurePeak=${json.optDouble("feature_peak")} featureMean=${json.optDouble("feature_mean")} " +
                    "featureNonFinite=${json.optInt("feature_nonfinite_count")} paddingNonzero=${json.optInt("feature_padding_nonzero")} " +
                    "timingStatus=${json.optString("timing_status")} timingError=${json.optString("timing_error").take(150)} " +
                    "blankTranscript=${json.getString("transcript").isBlank()}",
                nativeWallMs = json.optLong("total_ms", -1L).takeIf { it >= 0L }
            )
        }
    }

    override fun cancel() {
        cancelled = true
        worker.cancelCurrent()
    }

    override fun close() {
        runCatching { worker.closeRuntime() }
        worker.close()
    }
}

private class WhisperSession(modelPath: String) : AsrSession {
    private var handle = WhisperBridge.createContext(modelPath).also {
        require(it != 0L) { "Whisper could not load the selected model" }
    }

    override val nativeInfo: String = runCatching { WhisperBridge.systemInfo() }.getOrDefault("unavailable")

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        check(handle != 0L) { "Whisper session is closed" }
        val result = WhisperBridge.recognize(handle, samples, language, threads)
        return AsrRecognition(
            text = result.text,
            timedSegments = result.timedSegments,
            diagnostics = " timedUnits=${result.timedSegments.size}"
        )
    }

    override fun cancel() {
        val current = handle
        if (current != 0L) runCatching { WhisperBridge.cancel(current) }
    }

    override fun close() {
        val current = handle
        handle = 0L
        if (current != 0L) WhisperBridge.freeContext(current)
    }
}


private class TranscribeCppSession(modelPath: String, threads: Int) : AsrSession {
    private val backend = TranscribeCppBackend.open(modelPath, threads)
    override val nativeInfo: String = "transcribe.cpp ${TranscribeCppBackend.version()} cpu"

    override fun transcribe(samples: FloatArray, language: String, threads: Int): AsrRecognition {
        val result = backend.transcribe(samples, language)
        return AsrRecognition(
            text = result.text,
            timedSegments = result.timedSegments,
            diagnostics = " tokens=${result.tokenCount} timedSegments=${result.timedSegments.size} detectedLanguage=${result.detectedLanguage.ifBlank { "unknown" }}"
        )
    }

    override fun cancel() = backend.cancel()
    override fun close() = backend.close()
}
