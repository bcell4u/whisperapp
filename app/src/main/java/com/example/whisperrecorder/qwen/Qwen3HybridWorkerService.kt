package com.example.whisperrecorder.qwen

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SharedMemory
import android.os.SystemClock
import android.system.Os
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.npu.NpuEpContextModel
import com.example.whisperrecorder.npu.NpuGraphContract
import com.example.whisperrecorder.npu.NpuTensorContract
import com.example.whisperrecorder.npu.Qwen3ByteTokenizer
import com.example.whisperrecorder.npu.Qwen3HybridPackageStore
import com.example.whisperrecorder.npu.QwenDecoderPerformancePolicy
import com.example.whisperrecorder.npu.Qwen3Mel
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.CancellationException

/** Dedicated :qwen worker. Never touches Parakeet/Omi sessions or their staging folders.
 * QNN encoder uses disable_cpu_ep_fallback=1. CPU is allowed ONLY for the two decoder ONNX graphs.
 * Execution remains EXPERIMENTAL until real Android S26 EPContext and transcript parity are proven.
 */
class Qwen3HybridWorkerService : Service() {
    private var runtime: Runtime? = null
    @Volatile private var cancelRequested = false
    private val binder = object : IQwen3HybridService.Stub() {
        override fun prepare(): String = response("prepare") {
            if (runtime == null) runtime = Runtime()
            try { runtime!!.prepare() } catch (t: Throwable) { closeSession(); throw t }
        }
        override fun transcribe(pcm: SharedMemory, sampleCount: Int): String = response("transcribe") {
            require(sampleCount in 1..Qwen3Mel.SAMPLES)
            val pcmBuffer = pcm.mapReadOnly().order(ByteOrder.nativeOrder())
            val samples = try {
                FloatArray(sampleCount).also { pcmBuffer.asFloatBuffer().get(it) }
            } finally { SharedMemory.unmap(pcmBuffer) }
            (runtime ?: error("Qwen hybrid session not prepared")).transcribe(samples)
        }
        override fun cancelCurrent() { cancelRequested = true }
        override fun closeRuntime() { closeSession() }
    }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { closeSession(); super.onDestroy() }
    private fun closeSession() {
        runCatching { runtime?.close() }
        runtime = null
    }
    private inline fun response(step: String, action: () -> JSONObject): String = try {
        action().put("ok", true).put("stage", step).toString()
    } catch (t: Throwable) {
        if (t !is CancellationException) {
            runCatching { DiagnosticLogger.appendError(applicationContext, "qwen3-hybrid/$step", t) }
        }
        JSONObject().put("ok", false).put("stage", step)
            .put("error", "${t.javaClass.simpleName}: ${t.message ?: "unknown"}")
            .put("experimental", true).toString()
    }

    // Nested data classes cannot be declared inside Kotlin `inner` classes.
    private data class DecodeStats(
        val tokens: List<Int>, val prefillMs: Long, val stepMs: Long,
        val steps: Int, val embeddingMs: Long, val argmaxMs: Long
    )

    private inner class Runtime : AutoCloseable {
        private val env = OrtEnvironment.getEnvironment()
        private var sharedFallbackReason = "none"
        private var runtimeAssets: Qwen3HybridPackageStore.RuntimeAssets = openRuntimeAssets()
        private val modelDir: File get() = runtimeAssets.directory
        private val decoderThreads: Int = QwenDecoderPerformancePolicy.threads(applicationContext)

        private fun openRuntimeAssets(): Qwen3HybridPackageStore.RuntimeAssets {
            // Like Parakeet, hold seekable DocumentsProvider descriptors for the lifetime
            // of ONNX/QNN sessions. The bridge directory contains tiny symlinks, not weights.
            Qwen3HybridPackageStore.cleanupAbandonedRuntime(applicationContext)
            return try {
                Qwen3HybridPackageStore.openSharedRuntime(applicationContext)
                    ?: Qwen3HybridPackageStore.openFallbackRuntime(applicationContext)
            } catch (t: Throwable) {
                sharedFallbackReason = "${t.javaClass.simpleName}: ${t.message}".take(240)
                Qwen3HybridPackageStore.openFallbackRuntime(applicationContext)
            }
        }

        // External EPContext paths are resolved relative to the ONNX wrapper's directory.
        // Keep this tiny generated ONNX beside the SHA-256 verified QNN context, not in cacheDir.
        // The original context stays installed and is never copied or modified.
        private val encoderWrapper: File get() = File(modelDir, Qwen3HybridPackageStore.EP_CONTEXT_WRAPPER_NAME)
        private var qnnOptions: OrtSession.SessionOptions? = null
        private var encoder: OrtSession? = null
        private var decoderInit: OrtSession? = null
        private var decoderStep: OrtSession? = null
        private var embedding: RandomAccessFile? = null
        private var tokenizer: Qwen3ByteTokenizer? = null
        private var decoderLoadCount = 0
        private val contract get() = NpuGraphContract("qwen3_encoder", "qwen3_asr_encoder", "encoder-qnn-fp16.bin",
            File(modelDir, "encoder-qnn-fp16.bin").length(),
            listOf(NpuTensorContract("mel", longArrayOf(1,128,1200))),
            listOf(NpuTensorContract("audio_features", longArrayOf(1,156,1024))))

        fun prepare(): JSONObject {
            if (encoder != null) return JSONObject().put("encoder_loaded", true).put("experimental", true)
            return try {
                val result = prepareCore()
                // A successful descriptor bridge must also load both external-data ONNX
                // decoders. Catch unsupported SAF/proc-fd providers before transcription.
                if (runtimeAssets.mode == "saf-proc-fd") ensureDecoder()
                result.put("model_storage_mode", runtimeAssets.mode)
                    .put("model_storage_fallback", sharedFallbackReason)
                    .put("decoder_threads", decoderThreads)
            } catch (first: Throwable) {
                if (runtimeAssets.mode != "saf-proc-fd") throw first
                sharedFallbackReason = "${first.javaClass.simpleName}: ${first.message}".take(240)
                closeModels()
                runtimeAssets.close()
                runtimeAssets = Qwen3HybridPackageStore.openFallbackRuntime(applicationContext)
                prepareCore().put("model_storage_mode", runtimeAssets.mode)
                    .put("model_storage_fallback", sharedFallbackReason)
                    .put("decoder_threads", decoderThreads)
            }
        }

        private fun prepareCore(): JSONObject {
            cancelRequested = false
            require(Build.MODEL.equals("SM-S942U", ignoreCase = true)) {
                "Qwen QNN package targets SM-S942U, not ${Build.MODEL}"
            }
            val parity = Qwen3Mel.verifyGolden(modelDir) { cancelRequested }
            require(parity.pass) {
                "Qwen Android mel mismatches Stage G golden; max=${parity.maxAbs} rmse=${parity.rmse}; NPU blocked"
            }
            val nativeDir = File(applicationInfo.nativeLibraryDir)
            val plugin = requireNotNull(nativeDir.listFiles()?.firstOrNull {
                it.isFile && it.name == "libonnxruntime_providers_qnn.so" }) {
                "ORT QNN provider library missing"
            }
            val htp = File(nativeDir, "libQnnHtp.so")
            require(htp.isFile) { "QNN HTP library missing; CPU fallback prohibited" }
            Os.setenv("ADSP_LIBRARY_PATH", listOf(nativeDir.absolutePath, "/vendor/lib/rfsa/adsp", "/system/lib/rfsa/adsp", "/dsp")
                .joinToString(";"), true)
            // Isolated worker has an independent ORT environment from the :npu worker.
            if (env.epDevices.none { it.epName == "QNNExecutionProvider" }) {
                env.registerExecutionProviderLibrary("QNNExecutionProvider", plugin.absolutePath)
            }
            val devices = env.epDevices.filter { it.epName == "QNNExecutionProvider" }
            require(devices.isNotEmpty()) { "QNN EP is not available for the S26" }
            // ORT's QNN provider forbids absolute ep_cache_context in external mode.
            // NpuEpContextModel.write enforces same-directory placement and writes only
            // the relative basename "encoder-qnn-fp16.bin" into ep_cache_context.
            NpuEpContextModel.write(contract, File(modelDir, "encoder-qnn-fp16.bin"), encoderWrapper, "2.50")
            val opts = OrtSession.SessionOptions()
            try {
                opts.addConfigEntry("session.disable_cpu_ep_fallback", "1")
                opts.addExecutionProvider(devices, mapOf("backend_path" to htp.absolutePath))
                encoder = env.createSession(encoderWrapper.absolutePath, opts)
                require(encoder!!.inputNames == setOf("mel") && encoder!!.outputNames == setOf("audio_features")) {
                    "QNN encoder graph I/O contract does not match compiled Stage G artifact"
                }
                qnnOptions = opts
            } catch (t: Throwable) {
                runCatching { opts.close() }
                throw IllegalStateException("S26 ORT-QNN EPContext restore failed (native qnn-net-run proof is separate): ${t.message}", t)
            }
            // Actual Android graph invocation on deterministic synthetic mel. No patient audio.
            val fixture = File(modelDir, "golden-synthetic-mel-12s-f32.raw").readBytes()
            val golden = FloatArray(128 * 1200)
            ByteBuffer.wrap(fixture).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(golden)
            val inferenceStarted = SystemClock.elapsedRealtime()
            OnnxTensor.createTensor(env, FloatBuffer.wrap(golden), longArrayOf(1,128,1200)).use { input ->
                requireNotNull(encoder).run(mapOf("mel" to input)).use { result ->
                    val feature = result.get("audio_features").orElseThrow {
                        IllegalStateException("QNN probe did not return audio_features")
                    } as OnnxTensor
                    require(feature.info.shape.contentEquals(longArrayOf(1,156,1024)))
                    val output = feature.floatBuffer
                    var nonzero = false
                    while (output.hasRemaining()) {
                        val number = output.get()
                        require(number.isFinite()) { "QNN probe returned nonfinite encoder features" }
                        if (number != 0f) nonzero = true
                    }
                    require(nonzero) { "QNN probe returned all-zero features" }
                }
            }
            return JSONObject().put("encoder_loaded", true).put("cpu_fallback", "disabled")
                .put("golden_max_abs", parity.maxAbs).put("golden_rmse", parity.rmse)
                .put("native_android_qnn_encoder_graph_calls", 1)
                .put("golden_encoder_inference_ms", SystemClock.elapsedRealtime() - inferenceStarted)
                .put("decoder", "CPU FP32 ONNX (lazy load)").put("experimental", true)
        }

        private fun ensureDecoder(): Long {
            if (decoderInit != null && decoderStep != null) return 0L
            val start = SystemClock.elapsedRealtime()
            // Allow a controlled S26 A/B test: 2 cores (balanced) or 4 (speed trial).
            // Disabling idle thread-pool spinning reduces unnecessary CPU heating between
            // token steps, without changing FP32 numerical operations or stop conditions.
            fun cpuOptions() = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(decoderThreads)
                setInterOpNumThreads(1)
                addConfigEntry("session.intra_op.allow_spinning", "0")
                addConfigEntry("session.inter_op.allow_spinning", "0")
            }
            val initOpts = cpuOptions()
            val stepOpts = cpuOptions()
            try {
                decoderInit = env.createSession(File(modelDir,"decoder_init.onnx").absolutePath, initOpts)
                decoderStep = env.createSession(File(modelDir,"decoder_step.onnx").absolutePath, stepOpts)
                require(decoderInit!!.inputNames == setOf("input_ids", "position_ids", "audio_features", "audio_offset"))
                require(decoderStep!!.inputNames == setOf("input_embeds", "position_ids", "past_keys", "past_values"))
                embedding = RandomAccessFile(File(modelDir,"embed_tokens.bin"), "r")
                tokenizer = Qwen3ByteTokenizer.load(File(modelDir,"tokenizer.json"))
                decoderLoadCount++
            } catch (t: Throwable) {
                runCatching { decoderInit?.close() }; decoderInit = null
                runCatching { decoderStep?.close() }; decoderStep = null
                runCatching { embedding?.close() }; embedding = null
                throw IllegalStateException("Qwen CPU decoder cannot load FP32 ONNX weights (check available RAM/storage): ${t.message}", t)
            } finally {
                initOpts.close(); stepOpts.close()
            }
            return SystemClock.elapsedRealtime() - start
        }

        fun transcribe(samples: FloatArray): JSONObject {
            check(!cancelRequested) { "Qwen cancelled" }
            val prepared = requireNotNull(encoder) { "Qwen QNN encoder was not prepared" }
            val wallStart = SystemClock.elapsedRealtime()
            val cpuStart = Process.getElapsedCpuTime()
            val thermalStart = thermalStatus()
            val preprocessStart = SystemClock.elapsedRealtime()
            val mel = Qwen3Mel.compute(samples) { cancelRequested }
            val preprocessMs = SystemClock.elapsedRealtime() - preprocessStart
            val inferenceStart = SystemClock.elapsedRealtime()
            val features = OnnxTensor.createTensor(env, FloatBuffer.wrap(mel), longArrayOf(1,128,1200)).use { melTensor ->
                prepared.run(mapOf("mel" to melTensor)).use { results ->
                    val output = results.get("audio_features").orElseThrow {
                        IllegalStateException("QNN encoder missing audio_features")
                    } as OnnxTensor
                    val info = output.info.shape
                    require(info.contentEquals(longArrayOf(1,156,1024))) {
                        "Unexpected QNN encoder feature shape ${info.contentToString()}"
                    }
                    FloatArray(156 * 1024).also { output.floatBuffer.get(it) }
                }
            }
            require(features.all { it.isFinite() }) { "Nonfinite QNN encoder output" }
            val encoderMs = SystemClock.elapsedRealtime() - inferenceStart
            check(!cancelRequested) { "Qwen cancelled" }
            val decoderLoadMs = ensureDecoder() // First segment may spend seconds loading two ORT sessions.
            val decoderStart = SystemClock.elapsedRealtime()
            val decodeStats = decode(features)
            val decoderMs = SystemClock.elapsedRealtime() - decoderStart
            val transcript = requireNotNull(tokenizer).decode(decodeStats.tokens)
            if (runtimeAssets.mode == "saf-proc-fd") {
                // A full native QNN encoder + CPU decoder transcription has succeeded
                // with NO private model-weight copy. This permits an explicit reclaim.
                runCatching { Qwen3HybridPackageStore.noteSuccessfulSharedTranscription(applicationContext, runtimeAssets.stage) }
            }
            val memory = Debug.MemoryInfo()
            Debug.getMemoryInfo(memory)
            val thermalEnd = thermalStatus()
            val thermalGuardTriggered = decoderThreads == 4 && thermalEnd >= PowerManager.THERMAL_STATUS_SEVERE
            if (thermalGuardTriggered) QwenDecoderPerformancePolicy.setFast(applicationContext, false)
            return JSONObject().put("transcript", transcript).put("tokens", decodeStats.tokens.size)
                .put("backend", "QNN-HTP FP16 encoder + CPU FP32 ONNX decoder")
                .put("encoder_graph_calls", 1)
                .put("encoder_execution_provider", "QNNExecutionProvider")
                .put("decoder_execution_provider", "CPU")
                .put("decoder_npu_graph_calls", 0)
                .put("preprocess_ms", preprocessMs)
                .put("encoder_ms", encoderMs).put("decoder_ms", decoderMs)
                .put("decoder_load_ms", decoderLoadMs).put("decoder_load_count", decoderLoadCount)
                .put("decoder_prefill_ms", decodeStats.prefillMs)
                .put("decoder_step_ms", decodeStats.stepMs)
                .put("decoder_steps", decodeStats.steps)
                .put("decoder_tokens_per_second", if (decoderMs > 0L) decodeStats.tokens.size * 1000.0 / decoderMs else 0.0)
                .put("decoder_avg_step_ms", if (decodeStats.steps > 0) stepMsOrZero(decodeStats) else 0.0)
                .put("fast_mode_thermal_guard", thermalGuardTriggered)
                .put("decoder_embedding_io_ms", decodeStats.embeddingMs)
                .put("decoder_argmax_ms", decodeStats.argmaxMs)
                .put("decoder_intra_threads", decoderThreads)
                .put("model_storage_mode", runtimeAssets.mode)
                .put("model_storage_fallback", sharedFallbackReason)
                .put("worker_cpu_ms", Process.getElapsedCpuTime() - cpuStart)
                .put("worker_wall_ms", SystemClock.elapsedRealtime() - wallStart)
                .put("worker_pss_kb", memory.totalPss)
                .put("thermal_start", thermalStart).put("thermal_end", thermalEnd)
                .put("experimental", true)
        }

        private fun stepMsOrZero(stats: DecodeStats): Double =
            if (stats.steps > 0) stats.stepMs.toDouble() / stats.steps else 0.0

        private fun thermalStatus(): Int = runCatching {
            getSystemService(PowerManager::class.java).currentThermalStatus
        }.getOrDefault(-1)

        private fun decode(features: FloatArray): DecodeStats {
            val prefix = longArrayOf(151644,9125,198,151645,198,151644,882,198,151669)
            val suffix = longArrayOf(151670,151645,198,151644,77091,198)
            val prompt = prefix + LongArray(156) { 151676 } + suffix
            val positions = LongArray(prompt.size) { it.toLong() }
            val outputs = ArrayList<Int>(256)
            var current: OrtSession.Result? = null
            val prefillStart = SystemClock.elapsedRealtime()
            var prefillMs = 0L
            var stepMs = 0L
            var embeddingMs = 0L
            var argmaxMs = 0L
            var steps = 0
            try {
                OnnxTensor.createTensor(env, LongBuffer.wrap(prompt), longArrayOf(1,prompt.size.toLong())).use { ids ->
                    OnnxTensor.createTensor(env, LongBuffer.wrap(positions), longArrayOf(1,prompt.size.toLong())).use { pos ->
                        OnnxTensor.createTensor(env, FloatBuffer.wrap(features), longArrayOf(1,156,1024)).use { audio ->
                            OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(prefix.size.toLong())), longArrayOf(1)).use { offset ->
                                current = requireNotNull(decoderInit).run(mapOf("input_ids" to ids,
                                    "position_ids" to pos, "audio_features" to audio, "audio_offset" to offset))
                            }
                        }
                    }
                }
                prefillMs = SystemClock.elapsedRealtime() - prefillStart
                val firstArgmaxStart = SystemClock.elapsedRealtime()
                var token = nextToken(requireNotNull(current))
                argmaxMs += SystemClock.elapsedRealtime() - firstArgmaxStart
                outputs.add(token)
                for (step in 0 until 255) {
                    if (token == 151643 || token == 151645) return DecodeStats(
                        outputs, prefillMs, stepMs, steps, embeddingMs, argmaxMs
                    )
                    if (cancelRequested) throw CancellationException("Qwen token generation cancelled")
                    val embeddingStart = SystemClock.elapsedRealtime()
                    val inputEmbedding = readEmbedding(token)
                    embeddingMs += SystemClock.elapsedRealtime() - embeddingStart
                    val prior = requireNotNull(current)
                    val keys = prior.get("present_keys").orElseThrow { IllegalStateException("Qwen keys output missing") } as OnnxTensor
                    val values = prior.get("present_values").orElseThrow { IllegalStateException("Qwen values output missing") } as OnnxTensor
                    val stepStart = SystemClock.elapsedRealtime()
                    val next = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputEmbedding), longArrayOf(1,1,1024)).use { embeds ->
                        OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(prompt.size + step.toLong())), longArrayOf(1,1)).use { pos ->
                            requireNotNull(decoderStep).run(mapOf("input_embeds" to embeds,
                                "position_ids" to pos, "past_keys" to keys, "past_values" to values))
                        }
                    }
                    stepMs += SystemClock.elapsedRealtime() - stepStart
                    steps++
                    current = next
                    prior.close()
                    val argmaxStart = SystemClock.elapsedRealtime()
                    token = nextToken(next)
                    argmaxMs += SystemClock.elapsedRealtime() - argmaxStart
                    outputs.add(token)
                }
                throw IllegalStateException("Qwen output truncated at tested 256-token generation cap")
            } finally { runCatching { current?.close() } }
        }

        private fun nextToken(result: OrtSession.Result): Int {
            val logits = result.get("logits").orElseThrow { IllegalStateException("Missing Qwen logits") } as OnnxTensor
            val buffer = logits.floatBuffer
            require(buffer.capacity() >= 151936 && buffer.capacity() % 151936 == 0) {
                "Qwen logits do not match 151936 vocabulary"
            }
            val base = buffer.capacity() - 151936
            var best = 0
            var value = Float.NEGATIVE_INFINITY
            for (i in 0 until 151936) {
                val candidate = buffer.get(base + i)
                if (candidate > value) { value = candidate; best = i }
            }
            require(value.isFinite()) { "Decoder produced nonfinite logits" }
            return best
        }

        private fun readEmbedding(token: Int): FloatArray {
            require(token in 0 until 151936)
            val file = requireNotNull(embedding)
            val bytes = ByteArray(1024 * 4)
            file.seek(token.toLong() * bytes.size)
            file.readFully(bytes)
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            return FloatArray(1024).also { buf.get(it) }
        }

        private fun closeModels() {
            runCatching { decoderStep?.close() }; decoderStep = null
            runCatching { decoderInit?.close() }; decoderInit = null
            runCatching { encoder?.close() }; encoder = null
            runCatching { embedding?.close() }; embedding = null
            tokenizer = null
            runCatching { qnnOptions?.close() }; qnnOptions = null
        }

        override fun close() {
            closeModels()
            // Contexts + external weights must stay reachable while ORT sessions exist.
            // Only after closing them may we unlink proc-fd bridges or staged files.
            runCatching { runtimeAssets.close() }
            check(Qwen3HybridPackageStore.clearTemporaryTestFiles(applicationContext)) {
                "Qwen temporary wrapper cleanup failed"
            }
        }
    }
}
