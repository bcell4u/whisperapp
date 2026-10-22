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
import com.example.whisperrecorder.npu.Qwen3ByteTokenizer
import com.example.whisperrecorder.npu.Qwen3Mel
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/** v6.6.0 Lab worker rebased on clean v6.5.22: genuine QNN EPContext encoder, never a CPU/ONNX decoder. */
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
        override fun prepareGgufLab(configJson: String): String = report("prepare") {
            synchronized(this@Qwen3HybridWorkerService) {
                closeRuntimeInternal()
                cancelRequested = false
                // Only pinned four-thread, 512-context Q8 path. No performance-policy switches.
                val config = JSONObject(configJson)
                require(config.getInt("threads") == 4) { "Lab decoder requires 4 threads" }
                val requestedOverlap = config.optInt("overlap_seconds", 0)
                require(requestedOverlap in 0..1) { "Lab overlap must be 0 or 1 second" }
                val prepared = prepareEncoder()
                overlapSeconds = requestedOverlap
                prepared
            }
        }
        override fun encodeGgufLab(pcm: SharedMemory, sampleCount: Int): String = report("encode") {
            synchronized(this@Qwen3HybridWorkerService) {
                require(sampleCount in 1..Qwen3Mel.SAMPLES) { "PCM segment longer than 12 s" }
                val buffer = pcm.mapReadOnly().order(ByteOrder.nativeOrder())
                val samples = try { FloatArray(sampleCount).also { buffer.asFloatBuffer().get(it) } }
                    finally { SharedMemory.unmap(buffer) }
                encode(samples)
            }
        }
        override fun verifyDecoderFixture(): String = report("verification") {
            synchronized(this@Qwen3HybridWorkerService) { verifyFixture() }
        }
        override fun finishGgufLab(): String = report("finish") {
            synchronized(this@Qwen3HybridWorkerService) { finish() }
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
        val model = QwenLabEncoderAssets.directory(this)
        require(QwenLabEncoderAssets.valid(this)) { "Import the QNN encoder package first" }
        val parity = Qwen3Mel.verifyGolden(model) { cancelRequested }
        require(parity.pass) { "Golden mel parity failed" }
        val binary = File(applicationInfo.nativeLibraryDir, "libwrqwengguflab.so")
        require(binary.isFile && binary.canExecute()) { "Native dot-product executable is unavailable" }
        val gguf = File(filesDir, "qwen-gguf-lab-models/decoder-q8.gguf")
        require(gguf.isFile && gguf.length() > 128L * 1024 * 1024 &&
            gguf.inputStream().use { String(it.readNBytes(4), Charsets.US_ASCII) } == "GGUF") {
            "Import the Q8 GGUF decoder first"
        }
        val nativeProbe = ProcessBuilder(binary.absolutePath, "--probe-info")
            .apply { environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir }.start()
        val probeText = try {
            check(nativeProbe.waitFor(20, TimeUnit.SECONDS)) { "Native probe timed out" }
            nativeProbe.inputStream.bufferedReader().use { it.readText().take(4096) }
        } finally { if (nativeProbe.isAlive) nativeProbe.destroyForcibly() }
        require(nativeProbe.exitValue() == 0 && probeText.contains("WR_BRIDGE_APP_PRIVATE_PROTOCOL_0001") &&
            probeText.contains("arm64_dotprod") &&
            probeText.contains("WR_QWEN_PROMPT_ROLE_8948_872_V1") &&
            probeText.contains("WR_QWEN_SYNTHETIC_LOGITS_V1")) { "Native bridge protocol/CPU mismatch" }

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
            // Synthetic fixture proves a non-zero NPU output before any private audio is decoded.
            val fixture = File(model, "golden-synthetic-mel-12s-f32.raw")
            val synthetic = FloatArray(128 * 1200)
            ByteBuffer.wrap(fixture.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(synthetic)
            OnnxTensor.createTensor(ort, FloatBuffer.wrap(synthetic), longArrayOf(1, 128, 1200)).use { input ->
                session.run(mapOf("mel" to input)).use { result ->
                    val output = result.get("audio_features").orElseThrow() as OnnxTensor
                    require(output.info.shape.contentEquals(longArrayOf(1, 156, 1024)))
                    val floats = output.floatBuffer
                    var nonzero = false
                    while (floats.hasRemaining()) {
                        val value = floats.get()
                        require(value.isFinite())
                        if (value != 0f) nonzero = true
                    }
                    require(nonzero) { "NPU generated all-zero features" }
                }
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
        val dir = File(cacheDir, "qwen-gguf-lab-worker")
        require(!dir.exists() || dir.deleteRecursively()) { "Lab cache cleanup failed" }
        require(dir.mkdirs())
        scratch = dir
        segments = 0
        preprocessMs = 0L
        qnnMs = 0L
        return JSONObject().put("encoder_execution_provider", "QNNExecutionProvider")
            .put("cpu_fallback", "disabled")
            .put("golden_max_abs", parity.maxAbs).put("golden_rmse", parity.rmse)
            .put("model_loads", 0).put("decoder_threads", 4)
    }

    private fun encode(samples: FloatArray): JSONObject {
        check(!cancelRequested) { "Lab cancelled" }
        val session = requireNotNull(encoder) { "Encoder not prepared" }
        val dir = requireNotNull(scratch)
        require(segments < 24) { "Maximum 24 segments" }
        val startMel = SystemClock.elapsedRealtime()
        val mel = Qwen3Mel.compute(samples) { cancelRequested }
        preprocessMs += SystemClock.elapsedRealtime() - startMel
        check(!cancelRequested) { "Lab cancelled" }
        val startNpu = SystemClock.elapsedRealtime()
        val values = OnnxTensor.createTensor(requireNotNull(env), FloatBuffer.wrap(mel),
            longArrayOf(1, 128, 1200)).use { input ->
            session.run(mapOf("mel" to input)).use { result ->
                val output = result.get("audio_features").orElseThrow() as OnnxTensor
                require(output.info.shape.contentEquals(longArrayOf(1, 156, 1024)))
                FloatArray(156 * 1024).also { output.floatBuffer.get(it) }
            }
        }
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

    /**
     * Explicit, isolated fixture verification: no recording and no generated transcript.
     * Runs the SAME synthetic mel twice through QNN, then decodes the SAME embedding
     * twice in one persistent llama model process (KV cleared between segments).
     * A second, independent reference is never fabricated from this local output.
     */
    private fun verifyFixture(): JSONObject {
        check(!cancelRequested) { "Verification cancelled" }
        check(QwenLabDecoderVerifier.selfTestGate()) { "Verification decision policy self-test failed" }
        val session = requireNotNull(encoder) { "Encoder not prepared" }
        val ort = requireNotNull(env)
        val dir = requireNotNull(scratch)
        require(segments == 0) { "Fixture verification requires an unused Lab session" }
        val fixture = File(QwenLabEncoderAssets.directory(this), "golden-synthetic-mel-12s-f32.raw")
        require(fixture.length() == 128L * 1200L * 4L)
        val mel = FloatArray(128 * 1200)
        ByteBuffer.wrap(fixture.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(mel)
        val started = SystemClock.elapsedRealtime()
        fun runQnn(): ByteArray {
            check(!cancelRequested) { "Verification cancelled" }
            return OnnxTensor.createTensor(ort, FloatBuffer.wrap(mel), longArrayOf(1, 128, 1200)).use { input ->
                session.run(mapOf("mel" to input)).use { result ->
                    val tensor = result.get("audio_features").orElseThrow() as OnnxTensor
                    require(tensor.info.shape.contentEquals(longArrayOf(1, 156, 1024)))
                    val floats = FloatArray(156 * 1024)
                    tensor.floatBuffer.get(floats)
                    require(floats.all { it.isFinite() })
                    ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                        .apply { asFloatBuffer().put(floats) }.array()
                }
            }
        }
        val firstFeatures = runQnn()
        val secondFeatures = runQnn()
        val featureHash = QwenLabDecoderVerifier.sha256(firstFeatures)
        val qnnSame = firstFeatures.contentEquals(secondFeatures)
        // Do not launch a decoder against two incompatible encoder outputs.
        val exported = runCatching { QwenLabSyntheticFeatureExport.save(this, firstFeatures) }
        if (!qnnSame) return JSONObject()
            .put("verification_status", "FAIL_QNN_FEATURE_REPLAY")
            .put("reference_status", "NOT_EVALUATED")
            .put("qnn_feature_repeat_identical", false)
            .put("feature_sha256", featureHash)
            .put("synthetic_export_status", if (exported.isSuccess) "SAVED" else "FAILED")
            .put("synthetic_export_filename", exported.getOrNull() ?: "")
            .put("mrope_parity_verified", false)
        val featureFile = File(dir, "fixture.f32le")
        featureFile.writeBytes(firstFeatures)
        val manifest = File(dir, "fixture.lst")
        manifest.writeText("fixture.f32le\nfixture.f32le\n", Charsets.UTF_8)
        val gguf = File(filesDir, "qwen-gguf-lab-models/decoder-q8.gguf")
        val modelHash = sha256File(gguf)
        val native = File(applicationInfo.nativeLibraryDir, "libwrqwengguflab.so")
        val nativeHash = sha256File(native)
        val reference = loadFixtureReference()
        val process = ProcessBuilder(native.absolutePath, gguf.absolutePath,
            "--multi", manifest.absolutePath, "128", "--app-private-trace",
            "--synthetic-logits", "fixture")
            .directory(dir).apply {
                environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir
            }.start()
        nativeProcess = process
        val errors = Thread {
            runCatching { process.errorStream.use { it.copyTo(java.io.OutputStream.nullOutputStream()) } }
        }.apply { isDaemon = true; start() }
        val watchdog = Thread {
            try { Thread.sleep(120_000) } catch (_: InterruptedException) { return@Thread }
            if (process.isAlive) process.destroyForcibly()
        }.apply { isDaemon = true; start() }
        try {
            val traces = mutableMapOf<Int, List<Int>>()
            val metrics = mutableMapOf<Int, JSONObject>()
            var summary: JSONObject? = null
            var received = 0
            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    check(!cancelRequested) { "Verification cancelled" }
                    received += line.length
                    require(received <= 45_000) { "Fixture verification IPC exceeded limit" }
                    when {
                        line.startsWith("WR_APP_PRIVATE_TOKENS=") -> {
                            val obj = JSONObject(line.substringAfter('='))
                            val index = obj.getInt("segment")
                            require(index in 0..1 && index !in traces)
                            val ids = obj.getJSONArray("ids")
                            require(ids.length() in 1..128)
                            traces[index] = (0 until ids.length()).map { i -> ids.getInt(i) }
                        }
                        line.startsWith("WR_BRIDGE_MULTI_METRIC=") -> {
                            val obj = JSONObject(line.substringAfter('='))
                            val index = obj.getInt("index")
                            require(index in 0..1 && index !in metrics)
                            metrics[index] = obj
                        }
                        line.startsWith("WR_BRIDGE_MULTI_SUMMARY=") -> {
                            require(summary == null)
                            summary = JSONObject(line.substringAfter('='))
                        }
                    }
                }
            }
            check(process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0)
            val stats = requireNotNull(summary) { "Missing native decoder summary" }
            require(stats.getInt("segments") == 2 && stats.getInt("model_loads") == 1 &&
                stats.getInt("decode_threads") == 4 && stats.getInt("batch_threads") == 4)
            require(traces.size == 2 && metrics.size == 2) { "Incomplete native token receipts" }
            val first = traces.getValue(0)
            val second = traces.getValue(1)
            require(metrics.getValue(0).getInt("generation_steps") == first.size &&
                metrics.getValue(1).getInt("generation_steps") == second.size)
            val outcome = QwenLabDecoderVerifier.compare(first, second, modelHash, featureHash, reference)
            // Native-reported EOS flags must also match private token streams.
            val nativeEos = metrics.values.count { it.getBoolean("eos_reached") }
            val eosConsistent = nativeEos == listOf(first, second).count {
                it.last() in listOf(QwenLabDecoderVerifier.EOS_A, QwenLabDecoderVerifier.EOS_B)
            }
            // Stronger local verification: compare every FP32 output logit from
            // identical synthetic prefill passes. Never run this on lecture inputs.
            val logitFirst = File(dir, "qwen-prefill-logits-0.f32le")
            val logitSecond = File(dir, "qwen-prefill-logits-1.f32le")
            val checkpoint = runCatching {
                QwenLabLogitReplay.compareFiles(logitFirst, logitSecond)
            }
            val exportCheckpoint = if (checkpoint.isSuccess) {
                runCatching { QwenLabLogitExport.save(this, logitFirst) }
            } else null
            val logitStatus = when {
                checkpoint.isFailure -> "INCONCLUSIVE_LOGIT_CHECKPOINT"
                checkpoint.getOrThrow().identical -> "PASS_LOGIT_REPLAY"
                else -> "FAIL_LOGIT_REPLAY"
            }
            val finalStatus = when {
                !eosConsistent -> "FAIL_NATIVE_RECEIPT"
                checkpoint.isFailure -> "INCONCLUSIVE_LOGIT_CHECKPOINT"
                !checkpoint.getOrThrow().identical -> "FAIL_LOGIT_REPLAY"
                else -> outcome.status
            }
            return JSONObject()
                .put("verification_status", finalStatus)
                .put("reference_status", outcome.referenceStatus)
                .put("internal_repeat_identical", outcome.internallyIdentical)
                .put("qnn_feature_repeat_identical", qnnSame)
                .put("eos_both", outcome.eosBoth)
                .put("first_divergent_token_index", outcome.firstDifferentToken)
                .put("generated_tokens", outcome.tokenCount)
                .put("model_sha256", modelHash).put("feature_sha256", featureHash)
                .put("native_sha256", nativeHash)
                .put("logit_replay_status", logitStatus)
                .put("logit_sha256", checkpoint.getOrNull()?.sha256 ?: "")
                .put("logit_first_different_index", checkpoint.getOrNull()?.firstDifferentFloat ?: -1)
                .put("logit_max_abs_delta", checkpoint.getOrNull()?.maxAbsoluteDelta ?: -1.0)
                .put("logit_checkpoint_export_status", when {
                    exportCheckpoint == null -> "NOT_AVAILABLE"
                    exportCheckpoint.isSuccess -> "SAVED"
                    else -> "FAILED"
                })
                .put("logit_checkpoint_export_filename", exportCheckpoint?.getOrNull() ?: "")
                .put("synthetic_export_status", if (exported.isSuccess) "SAVED" else "FAILED")
                .put("synthetic_export_filename", exported.getOrNull() ?: "")
                .put("native_loads", stats.getInt("model_loads"))
                .put("native_threads", stats.getInt("decode_threads"))
                .put("elapsed_ms", SystemClock.elapsedRealtime() - started)
                .put("decision_policy_selftest", true)
                .put("mrope_parity_verified", false)
                .put("numerical_logits_parity_verified", false)
        } finally {
            watchdog.interrupt()
            if (process.isAlive) process.destroyForcibly()
            errors.join(1000)
            nativeProcess = null
            closeRuntimeInternal()
        }
    }

    private fun sha256File(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                check(!cancelRequested) { "Verification cancelled" }
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    /** Imported local-only reference. Never logs token IDs or user-supplied provenance text. */
    private fun loadFixtureReference(): QwenLabDecoderVerifier.ExternalReference? {
        val file = File(filesDir, "qwen-gguf-lab-reference.json")
        if (!file.isFile) return null
        require(file.length() in 1..32_768) { "Reference JSON too large" }
        val json = JSONObject(file.readText(Charsets.UTF_8))
        require(json.getInt("schema") == QwenLabDecoderVerifier.SCHEMA &&
            json.getString("kind") == "qwen3-asr-0.6b-fixture-tokens") { "Reference schema mismatch" }
        val ids = json.getJSONArray("token_ids")
        require(ids.length() in 1..128) { "Reference token count invalid" }
        return QwenLabDecoderVerifier.ExternalReference(
            json.getString("gguf_sha256"), json.getString("feature_sha256"),
            (0 until ids.length()).map { ids.getInt(it) }, json.getString("origin")
        )
    }

    private fun finish(): JSONObject {
        val dir = requireNotNull(scratch) { "Lab session missing" }
        require(segments in 2..24) { "Expected 2..24 QNN segments" }
        val native = File(applicationInfo.nativeLibraryDir, "libwrqwengguflab.so")
        val gguf = File(filesDir, "qwen-gguf-lab-models/decoder-q8.gguf")
        val manifest = File(dir, "features.lst")
        manifest.writeText((0 until segments).joinToString(separator = "") {
            "feat%03d.f32le\n".format(Locale.ROOT, it)
        }, Charsets.UTF_8)
        val started = SystemClock.elapsedRealtime()
        val process = ProcessBuilder(native.absolutePath, gguf.absolutePath,
            "--multi", manifest.absolutePath, "128", "--app-private-trace")
            .directory(dir).apply { environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir }.start()
        nativeProcess = process
        val errors = Thread { // discard stderr; never store model paths or token-bearing diagnostics
            runCatching { process.errorStream.use { it.copyTo(java.io.OutputStream.nullOutputStream()) } }
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
                    check(!cancelRequested) { "Lab cancelled" }
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
            val tokenizer = Qwen3ByteTokenizer.load(File(QwenLabEncoderAssets.directory(this), "tokenizer.json"))
            val cleanSegments = (0 until segments).map { index ->
                QwenLabTranscriptCleaner.cleanSegment(tokenizer.decode(tokens.getValue(index)))
            }
            val stitched = QwenLabTranscriptStitcher.stitch(cleanSegments, overlapSeconds)
            return JSONObject().put("transcript", stitched.transcript)
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
                .put("mrope_parity_verified", false).put("experimental", true)
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
        scratch?.let { require(it.deleteRecursively() && !it.exists()) { "Lab cache cleanup failed" } }
        scratch = null
        overlapSeconds = 0
        // In case binder process crashed during an earlier run, clear only this worker's cache.
    }
}
