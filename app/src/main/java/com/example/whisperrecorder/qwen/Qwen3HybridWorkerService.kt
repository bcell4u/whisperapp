package com.example.whisperrecorder.qwen

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SharedMemory
import android.os.SystemClock
import android.system.Os
import com.example.whisperrecorder.npu.NpuEpContextModel
import com.example.whisperrecorder.npu.NpuGraphContract
import com.example.whisperrecorder.npu.NpuTensorContract
import com.example.whisperrecorder.npu.Qwen3Mel
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Qwen3-ASR 0.6B QNN EPContext encoder + ARM64 llama.cpp decoder. No CPU encoder fallback. */
class Qwen3HybridWorkerService : Service() {
    @Volatile private var cancelRequested = false
    @Volatile private var nativeProcess: Process? = null
    private var env: OrtEnvironment? = null
    private var options: OrtSession.SessionOptions? = null
    private var encoder: OrtSession? = null
    private var scratch: File? = null
    private var segments = 0
    private var preprocessMs = 0L
    private var qnnMs = 0L
    private var overlapSeconds = 0

    // Binder transactions may arrive on distinct threads. Never run multiple encoder commands at once.
    private val binder = object : IQwen3HybridService.Stub() {
        override fun prepare(configJson: String): String = report("prepare") {
            synchronized(this@Qwen3HybridWorkerService) {
                closeRuntimeInternal()
                cancelRequested = false
                // Only pinned four-thread, 512-context Q8 path. No performance-policy switches.
                val config = JSONObject(configJson)
                require(config.getInt("threads") == 4) { "Qwen 0.6B requires four decoder threads" }
                val requestedOverlap = config.optInt("overlap_seconds", 0)
                require(requestedOverlap in 0..1) { "Overlap must be 0 or 1 second" }
                val prepared = prepareEncoder()
                overlapSeconds = requestedOverlap
                prepared
            }
        }
        override fun encode(pcm: SharedMemory, sampleCount: Int): String = report("encode") {
            synchronized(this@Qwen3HybridWorkerService) {
                require(sampleCount in 1..Qwen3Mel.SAMPLES) { "PCM segment longer than 12 s" }
                val buffer = pcm.mapReadOnly().order(ByteOrder.nativeOrder())
                val samples = try { FloatArray(sampleCount).also { buffer.asFloatBuffer().get(it) } }
                    finally { SharedMemory.unmap(buffer) }
                this@Qwen3HybridWorkerService.encode(samples)
            }
        }
        override fun finish(): String = report("finish") {
            synchronized(this@Qwen3HybridWorkerService) { this@Qwen3HybridWorkerService.finish() }
        }
        override fun cancelCurrent() {
            cancelRequested = true
            nativeProcess?.destroyForcibly()
        }
        override fun closeRuntime() { synchronized(this@Qwen3HybridWorkerService) { closeRuntimeInternal() } }
    }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() {
        cancelRequested = true
        nativeProcess?.destroyForcibly()
        synchronized(this) { closeRuntimeInternal() }
        super.onDestroy()
    }
    private inline fun report(stage: String, block: () -> JSONObject): String = try {
        block().put("ok", true).put("stage", stage).toString()
    } catch (t: Throwable) {
        // Never propagate filesystem paths, audio, or native token content over error IPC.
        JSONObject().put("ok", false).put("stage", stage)
            .put("error", t.javaClass.simpleName).toString()
    }

    private fun prepareEncoder(): JSONObject {
        require(Build.MODEL.equals("SM-S942U", true)) { "Only Snapdragon S26 SM-S942U is qualified for this trial" }
        val model = QwenEncoderAssets.directory(this)
        require(QwenEncoderAssets.valid(this)) { "Import the QNN encoder package first" }
        val binary = File(applicationInfo.nativeLibraryDir, "libwrqwen06.so")
        require(binary.isFile && binary.canExecute()) { "Native dot-product executable is unavailable" }
        val gguf = File(filesDir, "models/local-asr/qwen3-asr-0.6b-q8/Qwen3-ASR-0.6B-Q8_0.gguf")
        require(gguf.isFile && gguf.length() > 128L * 1024 * 1024 &&
            gguf.inputStream().use { stream ->
                val magic = ByteArray(4)
                stream.read(magic) == 4 && magic.contentEquals("GGUF".toByteArray(Charsets.US_ASCII))
            }) {
            "Import the Q8 GGUF decoder first"
        }
        val nativeDir = File(applicationInfo.nativeLibraryDir)
        val plugin = File(nativeDir, "libonnxruntime_providers_qnn.so")
        val htp = File(nativeDir, "libQnnHtp.so")
        require(plugin.isFile && htp.isFile) { "QNN libraries unavailable; no CPU fallback" }
        Os.setenv("ADSP_LIBRARY_PATH", listOf(nativeDir.absolutePath, "/vendor/lib/rfsa/adsp",
            "/system/lib/rfsa/adsp", "/dsp").joinToString(";"), true)
        val ort = OrtEnvironment.getEnvironment()
        if (ort.epDevices.none { it.epName == "QNNExecutionProvider" }) {
            ort.registerExecutionProviderLibrary("QNNExecutionProvider", plugin.absolutePath)
        }
        val qnnDevices = ort.epDevices.filter { it.epName == "QNNExecutionProvider" }
        require(qnnDevices.isNotEmpty()) { "QNNExecutionProvider missing" }
        val context = File(model, "encoder-qnn-fp16.bin")
        val wrapper = File(model, "qwen3-encoder-epcontext.onnx")
        val contract = NpuGraphContract("qwen3_encoder", "qwen3_asr_encoder", context.name,
            context.length(), listOf(NpuTensorContract("mel", longArrayOf(1, 128, 1200))),
            listOf(NpuTensorContract("audio_features", longArrayOf(1, 156, 1024))))
        NpuEpContextModel.write(contract, context, wrapper, "2.50")
        val opts = OrtSession.SessionOptions()
        var pendingSession: OrtSession? = null
        try {
            opts.addConfigEntry("session.disable_cpu_ep_fallback", "1")
            opts.addExecutionProvider(qnnDevices, mapOf("backend_path" to htp.absolutePath))
            val session = ort.createSession(wrapper.absolutePath, opts)
            pendingSession = session
            require(session.inputNames == setOf("mel") && session.outputNames == setOf("audio_features")) {
                "QNN encoder input/output contract mismatch"
            }
            env = ort
            options = opts
            encoder = session
            pendingSession = null // Runtime now owns the live encoder reference.
        } catch (t: Throwable) {
            runCatching { pendingSession?.close() }
            opts.close()
            throw t
        }
        val dir = File(cacheDir, "qwen06-worker")
        require(!dir.exists() || dir.deleteRecursively()) { "Qwen cache cleanup failed" }
        require(dir.mkdirs())
        scratch = dir
        segments = 0
        preprocessMs = 0L
        qnnMs = 0L
        return JSONObject().put("encoder_execution_provider", "QNNExecutionProvider")
            .put("cpu_fallback", "disabled")
            .put("model_loads", 0).put("decoder_threads", 4)
    }

    private fun encode(samples: FloatArray): JSONObject {
        check(!cancelRequested) { "Qwen transcription cancelled" }
        val session = requireNotNull(encoder) { "Encoder not prepared" }
        val dir = requireNotNull(scratch)
        require(segments < 24) { "Maximum 24 segments" }
        val startMel = SystemClock.elapsedRealtime()
        val mel = Qwen3Mel.compute(samples) { cancelRequested }
        preprocessMs += SystemClock.elapsedRealtime() - startMel
        check(!cancelRequested) { "Qwen transcription cancelled" }
        val startNpu = SystemClock.elapsedRealtime()
        val values = OnnxTensor.createTensor(requireNotNull(env), FloatBuffer.wrap(mel),
            longArrayOf(1, 128, 1200)).use { input ->
            session.run(mapOf("mel" to input)).use { result ->
                val output = result.get("audio_features").orElseThrow() as OnnxTensor
                require(output.info.shape.contentEquals(longArrayOf(1, 156, 1024)))
                FloatArray(156 * 1024).also { output.floatBuffer.get(it) }
            }
        }
        check(!cancelRequested) { "Qwen transcription cancelled" }
        require(values.all { it.isFinite() }) { "QNN features invalid" }
        qnnMs += SystemClock.elapsedRealtime() - startNpu
        val file = File(dir, "feat%03d.f32le".format(Locale.ROOT, segments))
        BufferedOutputStream(FileOutputStream(file)).use { out ->
            val byteBuffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.asFloatBuffer().put(values)
            out.write(byteBuffer.array())
        }
        segments++
        return JSONObject().put("encoded_segments", segments).put("preprocess_ms", preprocessMs)
            .put("encoder_ms", qnnMs).put("encoder_execution_provider", "QNNExecutionProvider")
    }

    private fun finish(): JSONObject {
        check(!cancelRequested) { "Qwen transcription cancelled" }
        val dir = requireNotNull(scratch) { "Qwen session missing" }
        require(segments in 1..24) { "Expected 1..24 QNN segments" }
        val native = File(applicationInfo.nativeLibraryDir, "libwrqwen06.so")
        val gguf = File(filesDir, "models/local-asr/qwen3-asr-0.6b-q8/Qwen3-ASR-0.6B-Q8_0.gguf")
        val manifest = File(dir, "features.lst")
        manifest.writeText((0 until segments).joinToString(separator = "") {
            "feat%03d.f32le\n".format(Locale.ROOT, it)
        }, Charsets.UTF_8)
        check(!cancelRequested) { "Qwen transcription cancelled" }
        val started = SystemClock.elapsedRealtime()
        val process = ProcessBuilder(native.absolutePath, gguf.absolutePath,
            "--multi", manifest.absolutePath, "128", "--app-private-output")
            .directory(dir).apply { environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir }.start()
        nativeProcess = process
        val errors = Thread { // discard stderr; never store model paths or token-bearing diagnostics
            runCatching { process.errorStream.use { stream ->
                val discarded = ByteArray(4096)
                while (stream.read(discarded) >= 0) { /* never persist private native stderr */ }
            } }
        }.apply { isDaemon = true; start() }
        val watchdog = Thread {
            try { Thread.sleep(180_000) } catch (_: InterruptedException) { return@Thread }
            if (process.isAlive) process.destroyForcibly()
        }.apply { isDaemon = true; start() }
        try {
            val tokens = mutableMapOf<Int, List<Int>>()
            var summary: JSONObject? = null
            var eos = 0
            var generated = 0
            var received = 0
            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    check(!cancelRequested) { "Qwen transcription cancelled" }
                    received += line.length
                    require(received <= 250_000) { "Native IPC size exceeded" }
                    when {
                        line.startsWith("WR_APP_PRIVATE_TOKENS=") -> {
                            val obj = JSONObject(line.substringAfter('='))
                            val index = obj.getInt("segment")
                            require(index in 0 until segments && index !in tokens)
                            val ids = obj.getJSONArray("ids")
                            require(ids.length() in 1..128)
                            tokens[index] = (0 until ids.length()).map { i ->
                                ids.getInt(i).also { require(it in 0 until 151936) }
                            }
                        }
                        line.startsWith("WR_BRIDGE_MULTI_METRIC=") -> {
                            val obj = JSONObject(line.substringAfter('='))
                            if (obj.getBoolean("eos_reached")) eos++
                            generated += obj.getInt("generation_steps")
                        }
                        line.startsWith("WR_BRIDGE_MULTI_SUMMARY=") -> {
                            require(summary == null)
                            summary = JSONObject(line.substringAfter('='))
                        }
                    }
                }
            }
            check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { "Native decoder failed" }
            check(!cancelRequested)
            val stats = requireNotNull(summary)
            require(stats.getInt("segments") == segments && stats.getInt("model_loads") == 1)
            require(tokens.size == segments && eos == segments) { "Native EOS/token receipts incomplete" }
            val tokenizer = com.example.whisperrecorder.npu.Qwen3ByteTokenizer.load(File(QwenEncoderAssets.directory(this), "tokenizer.json"))
            val cleanSegments = (0 until segments).map { index ->
                QwenTranscriptCleaner.cleanSegment(tokenizer.decode(tokens.getValue(index)))
            }
            val stitched = QwenTranscriptStitcher.stitch(cleanSegments, overlapSeconds)
            // Window membership is used for approximate playback alignment only.
            // No native word-level timestamps are claimed or exported to logs.
            return JSONObject().put("transcript", stitched.transcript)
                .put("kept_windows", org.json.JSONArray(stitched.keptWindows))
                .put("overlap_seconds", overlapSeconds)
                .put("stitched_duplicate_words", stitched.removedDuplicateWords)
                .put("stitched_boundaries", stitched.stitchedBoundaries)
                .put("unmatched_boundaries", stitched.unmatchedBoundaries)
                .put("stitched_repetition_copies_removed", stitched.repetitionCopiesRemoved)
                .put("segments", segments).put("model_loads", stats.getInt("model_loads"))
                .put("model_load_ms", stats.optLong("model_load_ms"))
                .put("preprocess_ms", preprocessMs).put("encoder_ms", qnnMs)
                .put("decoder_ms", SystemClock.elapsedRealtime() - started)
                .put("decoder_segments_wall_ms", stats.optLong("decoder_segments_wall_ms"))
                .put("native_rss_peak_kb", stats.optLong("rss_peak_kb"))
                .put("native_cpu_ms", stats.optLong("process_cpu_ms", -1L))
                .put("eos_segments", eos).put("generated_tokens", generated)
                .put("encoder_execution_provider", "QNNExecutionProvider")
                .put("decoder_execution_provider", "llama.cpp Q8_0 ARM64 dotprod")
        } finally {
            watchdog.interrupt()
            if (process.isAlive) process.destroyForcibly()
            errors.join(1000)
            nativeProcess = null
            // Exact-scope cache deletion; no external files, model files, or recordings touched.
            closeRuntimeInternal()
        }
    }

    private fun closeRuntimeInternal() {
        nativeProcess?.destroyForcibly()
        nativeProcess = null
        runCatching { encoder?.close() }; encoder = null
        runCatching { options?.close() }; options = null
        env = null // ORT environment is process-global; do not close other users' instance.
        scratch?.let { require(it.deleteRecursively() && !it.exists()) { "Qwen cache cleanup failed" } }
        scratch = null
        overlapSeconds = 0
        // In case binder process crashed during an earlier run, clear only this worker's cache.
    }
}
