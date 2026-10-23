package com.example.whisperrecorder.npu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtEpDevice
import ai.onnxruntime.OrtSession
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.StatFs
import android.os.SharedMemory
import android.system.Os
import android.system.OsConstants
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

/** Persistent ORT-QNN worker isolated in :npu. Normal transcription will reuse this exact boundary. */
class NpuRuntimeWorkerService : Service() {
    private lateinit var engine: Engine

    override fun onCreate() {
        super.onCreate()
        engine = Engine(this)
        engine.log("worker onCreate pid=${Process.myPid()}")
    }

    private val binder = object : INpuRuntimeService.Stub() {
        override fun bootstrap(): String = guarded("bootstrap") { engine.bootstrap() }
        override fun prepareRuntime(): String = guarded("prepareRuntime") { engine.prepareRuntime() }
        override fun prepareOmiRuntime(): String = guarded("prepareOmiRuntime") { engine.prepareOmiRuntime() }
        override fun transcribeOmiChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): String =
            guarded("transcribeOmiChunk") { engine.transcribeOmiChunk(pcm, sampleCount, audioDurationMs) }
        override fun transcribeChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): String =
            guarded("transcribeChunk") { engine.transcribeChunk(pcm, sampleCount, audioDurationMs) }
        override fun prepareChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): String =
            guarded("prepareChunk") { engine.prepareChunk(pcm, sampleCount, audioDurationMs) }
        override fun transcribePreparedChunk(preparedId: Long): String =
            guarded("transcribePreparedChunk") { engine.transcribePreparedChunk(preparedId) }
        override fun releasePreparedChunk(preparedId: Long) { engine.releasePreparedChunk(preparedId) }
        override fun cancelCurrent() { engine.cancelCurrent() }
        override fun closeRuntime() { engine.closeSessions() }
    }

    private inline fun guarded(operation: String, block: () -> JSONObject): String = try {
        block().put("operation", operation).put("worker_pid", Process.myPid()).toString()
    } catch (t: Throwable) {
        engine.log("$operation FAIL ${t::class.java.name}: ${t.message}")
        JSONObject()
            .put("ok", false)
            .put("operation", operation)
            .put("worker_pid", Process.myPid())
            .put("error_type", t::class.java.name)
            .put("error", t.message ?: "(no message)")
            .put("stack", android.util.Log.getStackTraceString(t))
            .toString()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        runCatching { engine.close() }
        super.onDestroy()
    }

    private class Engine(private val service: Service) : AutoCloseable {
        @Volatile private var cancelRequested = false

        fun cancelCurrent() {
            cancelRequested = true
            clearPreparedChunk("cancel")
            log("cancel requested; current ORT call will stop at the next graph/controller boundary")
        }

        private fun checkNotCancelled() {
            if (cancelRequested) throw java.util.concurrent.CancellationException("NPU transcription cancelled")
        }

        private data class PredictorState(
            val predictorOut: FloatArray = FloatArray(640),
            val h0: FloatArray = FloatArray(640),
            val c0: FloatArray = FloatArray(640),
            val h1: FloatArray = FloatArray(640),
            val c1: FloatArray = FloatArray(640)
        )

        private data class PipelineExecution(
            val emittedTokenIds: List<Int>,
            val emittedFrames: List<Int>,
            val preprocessMs: Long,
            val encoderMs: Long,
            val projectorMs: Long,
            val decoderMs: Long,
            val totalMs: Long,
            val criticalPathMs: Long,
            val preprocessingOverlapped: Boolean,
            val validEncodedFrames: Int,
            val jointSteps: Int
        )

        private data class PreparedFeatures(
            val id: Long,
            val features: ParakeetPreprocessor.Result,
            val preprocessingMs: Long,
            val sampleCount: Int,
            val audioDurationMs: Long,
            val preparedAtMs: Long
        )

        /** Direct reusable ORT input/output buffers. No tensor objects are allocated in the hot loop. */
        private class GraphBuffers(
            environment: OrtEnvironment,
            val contract: NpuGraphContract
        ) : AutoCloseable {
            private data class Slot(
                val name: String,
                val buffer: FloatBuffer,
                val readView: FloatBuffer,
                val tensor: OnnxTensor,
                val count: Int
            )

            private val inputSlots = linkedMapOf<String, Slot>()
            private val outputSlots = linkedMapOf<String, Slot>()
            private val inputTensors = linkedMapOf<String, OnnxTensor>()
            private val outputTensors = linkedMapOf<String, OnnxTensor>()
            val inputs: Map<String, OnnxTensor> get() = inputTensors
            val pinnedOutputs: Map<String, OnnxTensor> get() = outputTensors

            init {
                contract.inputs.forEach { input ->
                    val created = slot(environment, input)
                    inputSlots[input.name] = created
                    inputTensors[input.name] = created.tensor
                }
                contract.outputs.forEach { output ->
                    val created = slot(environment, output)
                    outputSlots[output.name] = created
                    outputTensors[output.name] = created.tensor
                }
            }

            fun putInput(name: String, values: FloatArray) {
                val slot = requireNotNull(inputSlots[name]) { "Unknown ${contract.id} input $name" }
                require(values.size == slot.count) {
                    "${contract.id}/$name element count mismatch: expected=${slot.count} actual=${values.size}"
                }
                slot.buffer.duplicate().apply {
                    clear()
                    put(values)
                }
            }

            fun copyOutput(
                name: String,
                destination: FloatArray? = null,
                validateFinite: Boolean = true
            ): FloatArray {
                val slot = requireNotNull(outputSlots[name]) { "Unknown ${contract.id} output $name" }
                val out = destination ?: FloatArray(slot.count)
                require(out.size == slot.count) {
                    "${contract.id}/$name output element count mismatch: expected=${slot.count} actual=${out.size}"
                }
                slot.readView.apply {
                    rewind()
                    get(out)
                }
                if (validateFinite) {
                    require(out.all { it.isFinite() }) { "${contract.id} output $name contains non-finite values" }
                }
                return out
            }

            override fun close() {
                inputSlots.values.forEach { runCatching { it.tensor.close() } }
                outputSlots.values.forEach { runCatching { it.tensor.close() } }
                inputSlots.clear()
                outputSlots.clear()
                inputTensors.clear()
                outputTensors.clear()
            }

            private fun slot(environment: OrtEnvironment, contract: NpuTensorContract): Slot {
                val countLong = contract.shape.fold(1L) { a, b -> Math.multiplyExact(a, b) }
                require(countLong in 1..Int.MAX_VALUE.toLong()) { "Invalid tensor size for ${contract.name}" }
                val count = countLong.toInt()
                val buffer = ByteBuffer.allocateDirect(count * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                return Slot(
                    contract.name,
                    buffer,
                    buffer.duplicate(),
                    OnnxTensor.createTensor(environment, buffer, contract.shape),
                    count
                )
            }
        }

        private data class LoadedSession(
            val contract: NpuGraphContract,
            val session: OrtSession,
            val options: OrtSession.SessionOptions,
            val buffers: GraphBuffers,
            val loadMs: Long
        ) : AutoCloseable {
            override fun close() {
                runCatching { buffers.close() }
                runCatching { session.close() }
                runCatching { options.close() }
            }
        }

        private data class OpenGraphArtifact(
            val pfd: ParcelFileDescriptor?,
            val accessMode: String
        ) : AutoCloseable {
            override fun close() { runCatching { pfd?.close() } }
        }

        private data class SessionAttempt(
            val loaded: LoadedSession?,
            val error: String?,
            val trace: String? = null
        )

        private val logFile = File(service.filesDir, "npu-diagnostics/runtime-worker.log")
        private var env: OrtEnvironment? = null
        private var qnnDevices: List<OrtEpDevice> = emptyList()
        private var registered = false
        private var htpPath: File? = null
        private var bridgeRoot: File? = null
        private val sessions = linkedMapOf<String, LoadedSession>()
        private val performanceRunOptions = linkedMapOf<NpuPerformancePolicy.QnnMode, OrtSession.RunOptions>()
        private val graphArtifacts = linkedMapOf<String, OpenGraphArtifact>()
        private var embeddingStream: ParcelFileDescriptor.AutoCloseInputStream? = null
        private var embeddingChannel: FileChannel? = null
        private var embedding: FloatBuffer? = null
        private var tokenizer: ParakeetTokenizer? = null
        private var peakTransientContextBytes: Long = 0L
        private var stagedContextBytesDeleted: Long = 0L
        private var decoderTopology: String = "unprepared"
        private var linkedDecoderActive: Boolean = false
        private var linkedDecoderFallbackReason: String? = null
        private var linkedDecoderAssetDownloaded: Boolean = false
        private var linkedDecoderAssetAcquisitionMs: Long = 0L
        private var linkedDecoderSharedEpContexts: Boolean = false
        private val preparedLock = Any()
        private var preparedSequence = 0L
        private val preparedChunks = linkedMapOf<Long, PreparedFeatures>()

        fun log(message: String) {
            logFile.parentFile?.mkdirs()
            if (logFile.length() > 2L * 1024L * 1024L) {
                // Prevent repeated probes from growing the isolated worker log indefinitely.
                logFile.writeText("--- NPU worker log rotated ---\n", Charsets.UTF_8)
            }
            logFile.appendText("${System.currentTimeMillis()} $message\n", Charsets.UTF_8)
        }

        /** QNN's DSP skeleton loader uses ADSP_LIBRARY_PATH rather than the Java linker path. */
        private fun configureDspSearchPath() {
            val nativeDir = service.applicationInfo.nativeLibraryDir
            val existing = System.getenv("ADSP_LIBRARY_PATH").orEmpty()
            val candidates = listOf(
                nativeDir,
                "/vendor/lib/rfsa/adsp",
                "/system/lib/rfsa/adsp",
                "/dsp"
            )
            val paths = (candidates + existing.split(';'))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(";")
            Os.setenv("ADSP_LIBRARY_PATH", paths, true)
        }

        // JSONObject(Map) is declared with raw Object/Object generics in Android's Java API;
        // ORT's Map<String, String> getters cannot be passed to it safely from Kotlin.
        private fun jsonStringMap(entries: Map<String, String>): JSONObject = JSONObject().apply {
            entries.forEach { (key, value) -> put(key, value) }
        }

        private fun deviceJson(device: OrtEpDevice): JSONObject = JSONObject()
            .put("ep_name", device.epName)
            .put("ep_vendor", device.epVendor)
            .put("ep_metadata", jsonStringMap(device.epMetadata))
            .put("ep_options", jsonStringMap(device.epOptions))
            .put("device", device.device.toString())

        /** Fail early if the loaded QNN context's graph contract is stale. */
        private fun validateSessionNames(session: OrtSession, contract: NpuGraphContract) {
            val expectedInputs = contract.inputs.map { it.name }.toSet()
            val expectedOutputs = contract.outputs.map { it.name }.toSet()
            require(session.inputNames == expectedInputs) {
                "${contract.id} input names mismatch: expected=$expectedInputs actual=${session.inputNames}"
            }
            require(session.outputNames == expectedOutputs) {
                "${contract.id} output names mismatch: expected=$expectedOutputs actual=${session.outputNames}"
            }
        }

        private fun elapsedMs(startedNs: Long): Long =
            (SystemClock.elapsedRealtimeNanos() - startedNs).coerceAtLeast(0L) / 1_000_000L

        fun bootstrap(): JSONObject {
            val started = SystemClock.elapsedRealtime()
            configureDspSearchPath()
            val nativeDir = File(service.applicationInfo.nativeLibraryDir)
            val libs = nativeDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
            val plugin = libs.firstOrNull { it.name == "libonnxruntime_providers_qnn.so" }
                ?: libs.firstOrNull { it.name.contains("onnxruntime", true) && it.name.contains("qnn", true) }
                ?: error("QNN plugin library not found under ${nativeDir.absolutePath}")
            val htp = libs.firstOrNull { it.name == "libQnnHtp.so" }
                ?: error("libQnnHtp.so not found under ${nativeDir.absolutePath}")

            val environment = OrtEnvironment.getEnvironment()
            if (!registered) {
                environment.registerExecutionProviderLibrary(REGISTRATION_NAME, plugin.absolutePath)
                registered = true
            }
            val devices = environment.epDevices.filter { it.epName == REGISTRATION_NAME }
            require(devices.isNotEmpty()) { "QNNExecutionProvider registered but exposed no EP devices" }

            env = environment
            qnnDevices = devices
            htpPath = htp
            log("bootstrap PASS ort=${environment.version} plugin=${plugin.name} devices=${devices.size}")
            return JSONObject()
                .put("ok", true)
                .put("elapsed_ms", SystemClock.elapsedRealtime() - started)
                .put("ort_version", environment.version)
                .put("registration_name", REGISTRATION_NAME)
                .put("plugin_path", plugin.absolutePath)
                .put("htp_backend_path", htp.absolutePath)
                .put("ep_devices", JSONArray(devices.map(::deviceJson)))
                .put("cpu_fallback_policy", "disabled")
        }

        /**
         * Opens QNN contexts from the durable SAF archive. Each graph first probes direct descriptor
         * and resolved-path access. If ORT cannot reopen either path, one context is staged at a time,
         * loaded into a warm session, and deleted immediately. No persistent duplicate model cache exists.
         */
        @Synchronized
        fun prepareRuntime(): JSONObject {
            if (env == null || qnnDevices.isEmpty()) bootstrap()
            if (sessions.size == NpuGraphContracts.ALL.size && embedding != null && tokenizer != null) {
                return runtimePreparedJson()
            }
            closeSessions()
            val prepareStarted = SystemClock.elapsedRealtime()
            val archive = NpuPackageStore.runtimeArchiveInfo(service)

            val root = File(service.filesDir, "npu-runtime/${NpuPackageStore.REVISION}")
            root.deleteRecursively()
            require(root.mkdirs() || root.isDirectory) { "Unable to create NPU runtime working directory" }
            bridgeRoot = root
            peakTransientContextBytes = 0L
            stagedContextBytesDeleted = 0L

            val environment = requireNotNull(env)
            val htp = requireNotNull(htpPath)
            try {
                val encoderContract = NpuGraphContracts.ENCODER
                val encoderArchived = requireNotNull(archive.artifacts[encoderContract.artifactPath]) {
                    "Runtime archive metadata missing ${encoderContract.artifactPath}"
                }
                graphArtifacts[encoderContract.id] = openGraphSession(environment, htp, encoderContract, encoderArchived, root)

                try {
                    val linked = NpuPackageStore.ensureLinkedDecoderArtifact(service) { cancelRequested }
                    linkedDecoderAssetDownloaded = linked.downloaded
                    linkedDecoderAssetAcquisitionMs = linked.acquisitionMs
                    openLinkedDecoderSessions(environment, htp, linked.artifact, root)
                    decoderTopology = "linked_decoder_shared_context"
                    linkedDecoderActive = true
                    linkedDecoderSharedEpContexts = true
                    linkedDecoderFallbackReason = null
                } catch (linkedError: Throwable) {
                    if (linkedError is java.util.concurrent.CancellationException) throw linkedError
                    sessions.keys.filter { it != encoderContract.id }.toList().forEach { id ->
                        sessions.remove(id)?.let { loaded -> runCatching { loaded.close() } }
                        graphArtifacts.remove(id)?.let { artifact -> runCatching { artifact.close() } }
                    }
                    linkedDecoderActive = false
                    linkedDecoderSharedEpContexts = false
                    linkedDecoderFallbackReason = "${linkedError::class.java.simpleName}: ${linkedError.message}"
                    decoderTopology = "separate_contexts_fallback"
                    log("linked decoder fallback: $linkedDecoderFallbackReason")
                    listOf(NpuGraphContracts.PROJECTOR, NpuGraphContracts.PREDICTOR, NpuGraphContracts.JOINT).forEach { contract ->
                        val archived = requireNotNull(archive.artifacts[contract.artifactPath]) {
                            "Runtime archive metadata missing ${contract.artifactPath}"
                        }
                        graphArtifacts[contract.id] = openGraphSession(environment, htp, contract, archived, root)
                    }
                }

                val embeddingArtifact = requireNotNull(archive.artifacts[EMBEDDING_PATH]) {
                    "Runtime archive metadata missing $EMBEDDING_PATH"
                }
                val embeddingPfd = requireNotNull(service.contentResolver.openFileDescriptor(embeddingArtifact.uri, "r")) {
                    "Unable to open predictor embedding archive file"
                }
                val embeddingStatSize = embeddingPfd.statSize.takeIf { it >= 0L } ?: embeddingArtifact.size
                require(embeddingStatSize == EMBEDDING_BYTES) {
                    "Predictor embedding descriptor has wrong size: $embeddingStatSize"
                }
                val embeddingSeekable = runCatching {
                    Os.lseek(embeddingPfd.fileDescriptor, 0L, OsConstants.SEEK_SET)
                    true
                }.getOrDefault(false)
                require(embeddingSeekable) { "Predictor embedding archive descriptor is not seekable" }
                val stream = ParcelFileDescriptor.AutoCloseInputStream(embeddingPfd)
                val channel = stream.channel
                require(channel.size() == EMBEDDING_BYTES) {
                    "Predictor embedding mapped channel has wrong size: ${channel.size()}"
                }
                embedding = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size())
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asFloatBuffer()
                embeddingStream = stream
                embeddingChannel = channel
                tokenizer = loadTokenizer()
                val prepareMs = SystemClock.elapsedRealtime() - prepareStarted
                require(temporaryContextBytes(root) == 0L) {
                    "Transient QNN context staging bytes remain after session creation"
                }
                log("prepareRuntime PASS mode=${runtimeStorageMode()} topology=$decoderTopology linked=$linkedDecoderActive sharedEp=$linkedDecoderSharedEpContexts assetDownloaded=$linkedDecoderAssetDownloaded assetAcquireMs=$linkedDecoderAssetAcquisitionMs prepareMs=$prepareMs peakTransient=$peakTransientContextBytes deleted=$stagedContextBytesDeleted sessions=${sessions.size}")
                return runtimePreparedJson()
            } catch (t: Throwable) {
                closeSessions()
                throw t
            }
        }

        private fun openLinkedDecoderSessions(
            environment: OrtEnvironment,
            htp: File,
            archived: NpuArchiveArtifact,
            root: File
        ) {
            require(archived.relativePath == NpuPackageStore.LINKED_DECODER_PATH) {
                "Unexpected linked decoder artifact: ${archived.relativePath}"
            }
            require(archived.size == NpuPackageStore.LINKED_DECODER_BYTES) {
                "Linked decoder archive size mismatch: expected=${NpuPackageStore.LINKED_DECODER_BYTES} actual=${archived.size}"
            }

            val dir = File(root, "linked-decoder").apply { mkdirs() }
            val staged = File(dir, "linked-decoder-staged.bin")
            val available = StatFs(service.filesDir.absolutePath).availableBytes
            require(available >= archived.size + STAGING_HEADROOM_BYTES) {
                "Insufficient app-private storage for transient linked decoder staging: need=${archived.size + STAGING_HEADROOM_BYTES} available=$available"
            }
            val copyStarted = SystemClock.elapsedRealtime()
            var copied = 0L
            service.contentResolver.openInputStream(archived.uri).use { raw ->
                val input = requireNotNull(raw) { "Unable to read ${archived.relativePath} for transient staging" }
                FileOutputStream(staged).buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                    }
                }
            }
            require(copied == archived.size && staged.length() == archived.size) {
                "Linked decoder transient staging size mismatch: expected=${archived.size} copied=$copied actual=${staged.length()}"
            }
            val copyMs = SystemClock.elapsedRealtime() - copyStarted
            peakTransientContextBytes = max(peakTransientContextBytes, staged.length())

            val decoderContracts = listOf(NpuGraphContracts.PROJECTOR, NpuGraphContracts.PREDICTOR, NpuGraphContracts.JOINT)
            val createdIds = mutableListOf<String>()
            try {
                decoderContracts.forEach { contract ->
                    val linkedGraphName = when (contract.id) {
                        "projector" -> "wr_projector"
                        "predictor" -> "wr_predictor"
                        "joint" -> "wr_joint"
                        else -> error("Unexpected linked decoder graph id: ${contract.id}")
                    }
                    val linkedContract = contract.copy(graphName = linkedGraphName)
                    val wrapper = File(dir, "${contract.id}-context.onnx")
                    val attempt = trySession(
                        environment = environment,
                        htp = htp,
                        contract = linkedContract,
                        wrapper = wrapper,
                        cachePath = staged.name,
                        shareEpContexts = true
                    )
                    val loaded = requireNotNull(attempt.loaded) {
                        "${contract.id} linked-decoder session failed: ${attempt.error}"
                    }
                    sessions[contract.id] = loaded
                    createdIds += contract.id
                    graphArtifacts[contract.id] = OpenGraphArtifact(null, "linked-transient-staged-shared")
                    log("session ${contract.id} PASS mode=linked-transient-staged-shared linkedGraph=$linkedGraphName loadMs=${loaded.loadMs} sharedEp=true")
                }
            } catch (t: Throwable) {
                createdIds.asReversed().forEach { id ->
                    sessions.remove(id)?.let { loaded -> runCatching { loaded.close() } }
                    graphArtifacts.remove(id)?.let { artifact -> runCatching { artifact.close() } }
                }
                staged.delete()
                throw t
            }

            val stagedBytes = staged.length()
            require(staged.delete() && !staged.exists()) {
                "Unable to delete transient linked decoder context after shared session creation"
            }
            stagedContextBytesDeleted += stagedBytes
            log("linked decoder PASS graphs=${decoderContracts.joinToString(",") { it.id }} copyMs=$copyMs stagedBytes=$stagedBytes deleted=true sharedEp=true")
        }

        private fun openGraphSession(
            environment: OrtEnvironment,
            htp: File,
            contract: NpuGraphContract,
            archived: NpuArchiveArtifact,
            root: File,
            stagedOnly: Boolean = false
        ): OpenGraphArtifact {
            val pfd = requireNotNull(service.contentResolver.openFileDescriptor(archived.uri, "r")) {
                "Unable to open archived ${contract.artifactPath}"
            }
            try {
                val statSize = pfd.statSize.takeIf { it >= 0L } ?: archived.size
                require(statSize == archived.size) {
                    "Descriptor size mismatch for ${contract.artifactPath}: expected=${archived.size} actual=$statSize"
                }
                val seekable = runCatching {
                    Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_SET)
                    true
                }.getOrDefault(false)
                require(seekable) { "Archived context is not seekable: ${contract.artifactPath}" }

                val dir = File(root, contract.id).apply { mkdirs() }
                val wrapper = File(dir, "context.onnx")
                if (!stagedOnly) {
                    val procPath = "/proc/self/fd/${pfd.fd}"
                    val procAttempt = trySession(environment, htp, contract, wrapper, procPath)
                    if (procAttempt.loaded != null) {
                        sessions[contract.id] = procAttempt.loaded
                        log("session ${contract.id} PASS mode=proc-fd-direct fd=${pfd.fd} loadMs=${procAttempt.loaded.loadMs}")
                        return OpenGraphArtifact(pfd, "proc-fd-direct")
                    }

                    val resolvedPath = runCatching { Os.readlink(procPath) }.getOrNull()
                    if (!resolvedPath.isNullOrBlank()) {
                        val nativeReadable = runCatching {
                            FileInputStream(resolvedPath).use { input -> input.read() }
                            true
                        }.getOrDefault(false)
                        if (nativeReadable) {
                            val realAttempt = trySession(environment, htp, contract, wrapper, resolvedPath)
                            if (realAttempt.loaded != null) {
                                sessions[contract.id] = realAttempt.loaded
                                pfd.close()
                                log("session ${contract.id} PASS mode=resolved-real-path loadMs=${realAttempt.loaded.loadMs}")
                                return OpenGraphArtifact(null, "resolved-real-path")
                            }
                        }
                    }

                } else {
                    log("session ${contract.id} stage=load-transient-staged skip_known_absolute_epcontext_error=true")
                }

                val staged = File(dir, "context-staged.bin")
                val available = StatFs(service.filesDir.absolutePath).availableBytes
                require(available >= archived.size + STAGING_HEADROOM_BYTES) {
                    "Insufficient app-private storage for transient ${contract.id} staging: need=${archived.size + STAGING_HEADROOM_BYTES} available=$available"
                }
                val copyStarted = SystemClock.elapsedRealtime()
                var copied = 0L
                service.contentResolver.openInputStream(archived.uri).use { raw ->
                    val input = requireNotNull(raw) { "Unable to reopen ${contract.artifactPath} for transient staging" }
                    FileOutputStream(staged).buffered().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                        }
                    }
                }
                require(copied == archived.size && staged.length() == archived.size) {
                    "Transient staging size mismatch for ${contract.artifactPath}: expected=${archived.size} copied=$copied actual=${staged.length()}"
                }
                val copyMs = SystemClock.elapsedRealtime() - copyStarted
                peakTransientContextBytes = max(peakTransientContextBytes, staged.length())
                val stagedAttempt = trySession(environment, htp, contract, wrapper, staged.name)
                if (stagedAttempt.loaded == null) {
                    staged.delete()
                    throw IllegalStateException(
                        "${contract.id} ORT-QNN session load failed after direct descriptor, resolved path, and transient staging probes: ${stagedAttempt.error}"
                    )
                }
                sessions[contract.id] = stagedAttempt.loaded
                val stagedBytes = staged.length()
                require(staged.delete() && !staged.exists()) {
                    "Unable to delete transient staged context after ${contract.id} session creation"
                }
                stagedContextBytesDeleted += stagedBytes
                pfd.close()
                log("session ${contract.id} PASS mode=transient-staged copyMs=$copyMs loadMs=${stagedAttempt.loaded.loadMs} stagedBytes=$stagedBytes deleted=true")
                return OpenGraphArtifact(null, "transient-staged")
            } catch (t: Throwable) {
                runCatching { pfd.close() }
                throw t
            }
        }

        private fun trySession(
            environment: OrtEnvironment,
            htp: File,
            contract: NpuGraphContract,
            wrapper: File,
            cachePath: String,
            shareEpContexts: Boolean = false
        ): SessionAttempt {
            NpuEpContextModel.writeExternalPath(contract, cachePath, wrapper)
            val options = OrtSession.SessionOptions().apply {
                addConfigEntry("session.disable_cpu_ep_fallback", "1")
                if (shareEpContexts) addConfigEntry("ep.share_ep_contexts", "1")
                addExecutionProvider(qnnDevices, mapOf("backend_path" to htp.absolutePath))
            }
            var session: OrtSession? = null
            var buffers: GraphBuffers? = null
            return try {
                val loadStarted = SystemClock.elapsedRealtime()
                session = environment.createSession(wrapper.absolutePath, options)
                val loadMs = SystemClock.elapsedRealtime() - loadStarted
                validateSessionNames(session, contract)
                buffers = GraphBuffers(environment, contract)
                SessionAttempt(LoadedSession(contract, session, options, buffers, loadMs), null)
            } catch (t: Throwable) {
                runCatching { buffers?.close() }
                runCatching { session?.close() }
                runCatching { options.close() }
                SessionAttempt(null, "${t::class.java.simpleName}: ${t.message}", android.util.Log.getStackTraceString(t))
            }
        }

        private var omiEmbedding: FloatArray? = null
        private var omiTokenizer: OmiSentencePiece? = null

        /** Persistent, three-context Omi runtime. Uses the already-proven transient staging route. */
        @Synchronized
        fun prepareOmiRuntime(): JSONObject {
            var stage = "bootstrap"
            if (env == null || qnnDevices.isEmpty()) bootstrap()
            if (sessions.size == 3 && OmiNpuGraphContracts.ALL.all { sessions.containsKey(it.graph.id) } &&
                omiEmbedding != null && omiTokenizer != null) return omiReadyJson()
            closeSessions()
            cancelRequested = false
            stage = "find-archive"
            val docs = OmiNpuPackageStore.probeArtifacts(service)
            val root = File(service.filesDir, "npu-runtime/omi-${OmiNpuPackageStore.REVISION}")
            root.deleteRecursively()
            require(root.mkdirs() || root.isDirectory) { "Cannot create Omi NPU staging root" }
            bridgeRoot = root
            val environment = requireNotNull(env)
            val htp = requireNotNull(htpPath)
            val started = SystemClock.elapsedRealtime()
            try {
                // Load small contexts before allocating the large encoder HTP context.
                OmiNpuGraphContracts.ALL.forEach { spec ->
                    stage = "load-${spec.graph.id}"
                    checkNotCancelled()
                    val doc = requireNotNull(docs[spec.graph.artifactPath]) { "Missing ${spec.graph.artifactPath}" }
                    require(doc.size == spec.graph.artifactBytes) {
                        "Omi ${spec.graph.id} size mismatch expected=${spec.graph.artifactBytes} actual=${doc.size}"
                    }
                    log("omi transcription stage=$stage bytes=${doc.size} graph=${spec.graph.graphName}")
                    val archived = NpuArchiveArtifact(spec.graph.artifactPath, doc.uri, doc.size)
                    graphArtifacts[spec.graph.id] = openGraphSession(environment, htp, spec.graph, archived, root, stagedOnly = true)
                }
                stage = "load-npy-embedding"
                val embeddingDoc = requireNotNull(docs["token_embedding.npy"]) { "Omi embedding missing" }
                val embeddingBytes = readOmiSmallArtifact(embeddingDoc.uri, 2_624_128)
                omiEmbedding = parseOmiNpy(embeddingBytes)
                stage = "load-sentencepiece"
                val tokenizerDoc = requireNotNull(docs["tokenizer.model"]) { "Omi tokenizer missing" }
                omiTokenizer = OmiSentencePiece.fromModel(readOmiSmallArtifact(tokenizerDoc.uri, 251_396))
                stage = "assert-no-persistent-context-copy"
                require(temporaryContextBytes(root) == 0L) {
                    "Omi transient context copies were not deleted after session initialization"
                }
                log("omi transcription prepare PASS sessions=${sessions.size} elapsedMs=${SystemClock.elapsedRealtime() - started} storage=${runtimeStorageMode()} stagingDeleted=$stagedContextBytesDeleted")
                return omiReadyJson().put("prepare_ms", SystemClock.elapsedRealtime() - started)
            } catch (t: Throwable) {
                log("omi transcription prepare FAIL stage=$stage ${t.javaClass.name}: ${t.message} stack=${android.util.Log.getStackTraceString(t)}")
                closeSessions()
                throw IllegalStateException("Omi prepare failed at [$stage]: ${t.message}", t)
            }
        }

        private fun omiReadyJson(): JSONObject = JSONObject().put("ok", true)
            .put("session_count", 3)
            .put("decoder_topology", "omi-v2-three-separate-qnn-contexts")
            .put("storage_mode", runtimeStorageMode())
            .put("temporary_context_bytes", bridgeRoot?.let(::temporaryContextBytes) ?: 0L)
            .put("cpu_fallback", "disabled")
            .put("model_qualification", "experimental_encoder_numeric_gate_not_passed")

        private fun readOmiSmallArtifact(uri: android.net.Uri, expected: Int): ByteArray {
            val data = requireNotNull(service.contentResolver.openInputStream(uri)) { "Cannot read Omi model asset $uri" }
                .use { it.readBytes() }
            require(data.size == expected) { "Omi model asset byte count ${data.size} != $expected" }
            return data
        }

        private fun parseOmiNpy(bytes: ByteArray): FloatArray {
            require(bytes.size == 2_624_128 && bytes.copyOfRange(0, 6).contentEquals(
                byteArrayOf(0x93.toByte(), 0x4e, 0x55, 0x4d, 0x50, 0x59))) { "Invalid Omi embedding NPY magic/length" }
            val version = bytes[6].toInt() and 255
            require(version == 1) { "Unsupported Omi embedding NPY version $version" }
            val headerLen = (bytes[8].toInt() and 255) or ((bytes[9].toInt() and 255) shl 8)
            val offset = 10 + headerLen
            require(offset == 128) { "Omi embedding NPY header offset $offset != 128" }
            val header = bytes.copyOfRange(10, offset).toString(Charsets.US_ASCII)
            require(header.contains("<f4") && header.contains("1025") && header.contains("640") && header.contains("False")) {
                "Unexpected Omi embedding dtype/order/shape: $header"
            }
            val floats = FloatArray(1025 * 640)
            java.nio.ByteBuffer.wrap(bytes, offset, bytes.size - offset).order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer().get(floats)
            require(floats.all { it.isFinite() }) { "Non-finite Omi token embedding" }
            return floats
        }

        @Synchronized
        fun transcribeOmiChunk(pcm: SharedMemory, sampleCount: Int, durationMs: Long): JSONObject {
            var stage = "read-audio"
            val started = SystemClock.elapsedRealtime()
            cancelRequested = false
            try {
                require(sampleCount in 1..192_000 && durationMs in 1..12_500) {
                    "Omi NPU fixed 12s window exceeded samples=$sampleCount durationMs=$durationMs"
                }
                val samples = readPcm(pcm, sampleCount, durationMs)
                stage = "prepare-runtime"
                prepareOmiRuntime()
                stage = "nemo-feature-preprocessing"
                val preprocessStarted = SystemClock.elapsedRealtime()
                val features = ParakeetPreprocessor(ParakeetPreprocessor.Config(
                    sampleRate = 16_000, featureSize = 128, hopLength = 160,
                    nFft = 512, winLength = 400, preemphasis = 0.97f,
                    maxSamples = 192_000, physicalFrames = 1201
                )).extract(samples)
                val preprocessingMs = SystemClock.elapsedRealtime() - preprocessStarted
                val featureDiagnostics = OmiParityDiagnostics.summarize(samples, features)
                require(featureDiagnostics.featureNonFinite == 0 && featureDiagnostics.paddedNonZero == 0) {
                    "Omi preprocessing produced nonfinite or nonzero padded features"
                }
                require(features.validEncodedFrames in 1..150) { "Omi encoder valid frames ${features.validEncodedFrames} invalid" }
                checkNotCancelled()
                stage = "encoder-qnn-run"
                val encodeStart = SystemClock.elapsedRealtime()
                val encoded = runGraph(OmiNpuGraphContracts.ENCODER.graph, mapOf(
                    "audio_signal" to features.audioSignal,
                    "subsample_mask_1" to features.subsampleMask1,
                    "subsample_mask_2" to features.subsampleMask2,
                    "subsample_mask_3" to features.subsampleMask3
                )).getValue("output_0")
                val encoderMs = SystemClock.elapsedRealtime() - encodeStart
                checkNotCancelled()
                stage = "tdt-predictor-joint-loop"
                val decodeStarted = SystemClock.elapsedRealtime()
                val runtime = OmiDecoderRuntime(encoded)
                val decoded = OmiTdtController.decode(features.validEncodedFrames, runtime) { cancelRequested }
                val decoderMs = SystemClock.elapsedRealtime() - decodeStarted
                stage = "sentencepiece-decode"
                val tokenizer = requireNotNull(omiTokenizer) { "Omi tokenizer not prepared" }
                val text = tokenizer.decode(decoded.tokens)
                val words = runCatching {
                    val timed = tokenizer.timedWords(decoded.tokens, decoded.frames, durationMs)
                    val normalizedText = text.replace(Regex("\\s+"), " ").trim()
                    val fromWords = timed.joinToString(" ") { it.text }.replace(Regex("\\s+"), " ").trim()
                    require(fromWords == normalizedText) {
                        "Omi timed word text disagrees with SentencePiece decode " +
                            "wordCount=${timed.size} decodedChars=${text.length} wordChars=${fromWords.length}"
                    }
                    require(timed.all { it.startMs >= 0L && it.endMs > it.startMs && it.endMs <= durationMs }) {
                        "Omi timed words contain invalid frame-derived timestamps"
                    }
                    timed
                }
                val totalMs = SystemClock.elapsedRealtime() - started
                log("omi transcription chunk PASS durationMs=$durationMs samples=$sampleCount frames=${features.validEncodedFrames} preprocessMs=$preprocessingMs encoderMs=$encoderMs decoderMs=$decoderMs steps=${decoded.steps} blankDecisions=${decoded.blanks} zeroDurations=${decoded.zeroDurations} tokens=${decoded.tokens.size} chars=${text.length} wordTiming=${words.isSuccess} stop=${decoded.stopReason} symbolGuards=${decoded.symbolLimitAdvances} validMelFrames=${featureDiagnostics.validFrames} maskCounts=${featureDiagnostics.maskCounts} pcmRms=${featureDiagnostics.pcmRms} pcmPeak=${featureDiagnostics.pcmPeak} pcmClipPct=${featureDiagnostics.pcmClipPct} featureRms=${featureDiagnostics.featureRms} featurePeak=${featureDiagnostics.featurePeak} featureMean=${featureDiagnostics.featureMean} featureNonFinite=${featureDiagnostics.featureNonFinite} padNonZero=${featureDiagnostics.paddedNonZero}")
                return JSONObject().put("ok", true)
                    .put("transcript", text).put("transcript_chars", text.length)
                    .put("timed_words", JSONArray(words.getOrDefault(emptyList()).map {
                        JSONObject().put("text", it.text).put("startMs", it.startMs).put("endMs", it.endMs)
                    }))
                    .put("timing_status", if (words.isSuccess) "experimental-tdt-frame-estimate" else "fallback")
                    .put("timing_error", words.exceptionOrNull()?.message ?: "")
                    .put("total_ms", totalMs).put("critical_path_ms", totalMs)
                    .put("preprocessing_ms", preprocessingMs).put("encoder_ms", encoderMs)
                    .put("decoder_ms", decoderMs).put("joint_steps", decoded.steps)
                    .put("emitted_token_count", decoded.tokens.size)
                    .put("blank_decisions", decoded.blanks)
                    .put("zero_duration_decisions", decoded.zeroDurations)
                    .put("tdt_stop_reason", decoded.stopReason)
                    .put("tdt_symbol_limit_advances", decoded.symbolLimitAdvances)
                    .put("front_end_contract", OmiParityDiagnostics.FRONTEND_CONTRACT)
                    .put("pcm_rms", featureDiagnostics.pcmRms)
                    .put("pcm_peak", featureDiagnostics.pcmPeak)
                    .put("pcm_clipped_pct", featureDiagnostics.pcmClipPct)
                    .put("feature_rms", featureDiagnostics.featureRms)
                    .put("feature_peak", featureDiagnostics.featurePeak)
                    .put("feature_mean", featureDiagnostics.featureMean)
                    .put("feature_nonfinite_count", featureDiagnostics.featureNonFinite)
                    .put("feature_padding_nonzero", featureDiagnostics.paddedNonZero)
                    .put("valid_feature_frames", featureDiagnostics.validFrames)
                    .put("feature_mask_counts", featureDiagnostics.maskCounts)
                    .put("valid_encoded_frames", features.validEncodedFrames)
                    .put("qnn_context_binary_count", 3)
                    .put("decoder_topology", "omi-v2-three-separate-qnn-contexts")
                    .put("feature_preprocessing", "experimental_16k_128mel_25ms_10ms_slaney_preemphasis097_perfeature_not_nemo_parity_validated")
                    .put("model_qualification", "experimental_encoder_numeric_gate_not_passed")
            } catch (t: Throwable) {
                log("omi transcription chunk FAIL stage=$stage ${t.javaClass.name}: ${t.message} stack=${android.util.Log.getStackTraceString(t)}")
                throw IllegalStateException("Omi NPU transcription failed at [$stage]: ${t.message}", t)
            }
        }

        private inner class OmiDecoderRuntime(private val encoded: FloatArray) : OmiTdtController.Runtime {
            private val state = PredictorState()
            private val predictorEmbedding = FloatArray(640)
            private val encoderFrame = FloatArray(1024)
            private val logits = FloatArray(1030)
            private val predictorInputs = linkedMapOf(
                "token_embedding" to predictorEmbedding,
                "h0" to state.h0, "c0" to state.c0,
                "h1" to state.h1, "c1" to state.c1
            )
            private val predictorOutputs = linkedMapOf(
                "output_0" to state.predictorOut,
                "output_1" to state.h0, "output_2" to state.c0,
                "output_3" to state.h1, "output_4" to state.c1
            )
            override fun predict(tokenId: Int) {
                checkNotCancelled()
                require(tokenId in 0..1024) { "Omi predictor token id out of bounds $tokenId" }
                val table = requireNotNull(omiEmbedding) { "Omi token embeddings not loaded" }
                System.arraycopy(table, tokenId * 640, predictorEmbedding, 0, 640)
                runGraph(OmiNpuGraphContracts.PREDICTOR.graph, predictorInputs, predictorOutputs)
            }
            override fun joint(frameIndex: Int): FloatArray {
                checkNotCancelled()
                require(frameIndex in 0..150) { "Omi joint frame out of bounds $frameIndex" }
                for (channel in 0 until 1024) encoderFrame[channel] = encoded[channel * 151 + frameIndex]
                runGraph(OmiNpuGraphContracts.JOINT.graph,
                    mapOf("encoder_frame" to encoderFrame, "predictor_out" to state.predictorOut),
                    mapOf("output_0" to logits), validateOutputs = false)
                return logits
            }
        }

        /** Real 16 kHz PCM arrives through SharedMemory; no user audio is written to disk. */
        @Synchronized
        fun transcribeChunk(
            pcm: SharedMemory,
            sampleCount: Int,
            audioDurationMs: Long
        ): JSONObject {
            cancelRequested = false
            val samples = readPcm(pcm, sampleCount, audioDurationMs)
            val profile = productionProfile()
            val execution = executePcm(samples, profile, collectTokens = true)
            val result = transcriptionJson(execution, profile)
            log("transcribeChunk PASS samples=$sampleCount durationMs=$audioDurationMs profile=${profile.id} totalMs=${execution.totalMs} criticalMs=${execution.criticalPathMs} encoderMs=${execution.encoderMs} decoderMs=${execution.decoderMs} tokens=${execution.emittedTokenIds.size} chars=${result.optInt("transcript_chars")}")
            return result
        }

        /**
         * Phase 6B: preprocess exactly one future chunk while the current chunk is executing on HTP.
         * This method intentionally does no QNN work and is not synchronized on the engine so Binder
         * can run it concurrently with transcribeChunk/transcribePreparedChunk.
         */
        fun prepareChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): JSONObject {
            checkNotCancelled()
            val started = SystemClock.elapsedRealtime()
            val samples = readPcm(pcm, sampleCount, audioDurationMs)
            val extracted = extractFeatures(samples)
            checkNotCancelled()
            val prepared = synchronized(preparedLock) {
                preparedSequence++
                PreparedFeatures(
                    id = preparedSequence,
                    features = extracted.first,
                    preprocessingMs = extracted.second,
                    sampleCount = sampleCount,
                    audioDurationMs = audioDurationMs,
                    preparedAtMs = SystemClock.elapsedRealtime()
                ).also { created ->
                    preparedChunks[created.id] = created
                    require(preparedChunks.size <= 2) { "Prepared NPU double buffer exceeded two chunks" }
                }
            }
            val wallMs = SystemClock.elapsedRealtime() - started
            log("prepareChunk PASS id=${prepared.id} samples=$sampleCount durationMs=$audioDurationMs preprocessingMs=${prepared.preprocessingMs} wallMs=$wallMs validFrames=${prepared.features.validEncodedFrames}")
            return JSONObject()
                .put("ok", true)
                .put("prepared_id", prepared.id)
                .put("preparation_wall_ms", wallMs)
        }

        @Synchronized
        fun transcribePreparedChunk(preparedId: Long): JSONObject {
            cancelRequested = false
            val prepared = synchronized(preparedLock) {
                requireNotNull(preparedChunks.remove(preparedId)) {
                    "Prepared NPU chunk $preparedId is unavailable"
                }
            }
            checkNotCancelled()
            val profile = productionProfile()
            val ageMs = (SystemClock.elapsedRealtime() - prepared.preparedAtMs).coerceAtLeast(0L)
            val execution = executeFeatures(
                features = prepared.features,
                preprocessMs = prepared.preprocessingMs,
                profile = profile,
                collectTokens = true,
                preprocessingOverlapped = true
            )
            val result = transcriptionJson(execution, profile)
                .put("prepared_id", prepared.id)
                .put("prepared_age_ms", ageMs)
            log("transcribePreparedChunk PASS id=${prepared.id} samples=${prepared.sampleCount} durationMs=${prepared.audioDurationMs} profile=${profile.id} totalMs=${execution.totalMs} criticalMs=${execution.criticalPathMs} preprocessingMs=${execution.preprocessMs} encoderMs=${execution.encoderMs} decoderMs=${execution.decoderMs} tokens=${execution.emittedTokenIds.size} chars=${result.optInt("transcript_chars")}")
            return result
        }

        fun releasePreparedChunk(preparedId: Long) {
            val released = synchronized(preparedLock) { preparedChunks.remove(preparedId) != null }
            if (released) log("prepared chunk released id=$preparedId")
        }

        private fun clearPreparedChunk(reason: String) {
            val cleared = synchronized(preparedLock) {
                val ids = preparedChunks.keys.toList()
                preparedChunks.clear()
                ids
            }
            if (cleared.isNotEmpty()) log("prepared chunks cleared ids=${cleared.joinToString()} reason=$reason")
        }

        private fun readPcm(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): FloatArray {
            require(sampleCount in 1..640_000) { "NPU chunk sample count out of range: $sampleCount" }
            require(audioDurationMs in 1L..45_000L) { "NPU chunk duration out of range: $audioDurationMs" }
            require(pcm.size >= sampleCount * Float.SIZE_BYTES) {
                "SharedMemory is too small for PCM: bytes=${pcm.size} samples=$sampleCount"
            }
            val mapped = pcm.mapReadOnly().order(ByteOrder.nativeOrder())
            return try {
                val floats = mapped.asFloatBuffer()
                require(floats.remaining() >= sampleCount) { "SharedMemory float view is shorter than requested sample count" }
                FloatArray(sampleCount).also { floats.get(it) }
            } finally {
                SharedMemory.unmap(mapped)
            }
        }

        private fun productionProfile(): NpuPerformancePolicy.Profile =
            NpuPerformancePolicy.productionProfile(
                (service.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
            )

        private fun transcriptionJson(
            execution: PipelineExecution,
            profile: NpuPerformancePolicy.Profile
        ): JSONObject {
            val decoded = requireNotNull(tokenizer) { "Parakeet tokenizer is not loaded" }
                .decode(execution.emittedTokenIds, skipSpecialTokens = true)
            val wordTiming = runCatching {
                require(decoded.isNotBlank()) { "No speech tokens" }
                require(execution.emittedTokenIds.size == execution.emittedFrames.size) { "Token/frame count mismatch" }
                require(execution.emittedFrames.zipWithNext().all { (a, b) -> b >= a }) { "Token frames out of order" }
                require(execution.emittedFrames.all { it in 0 until execution.validEncodedFrames }) { "Token frame outside audio" }
                requireNotNull(tokenizer).decodeTimedWords(execution.emittedTokenIds, execution.emittedFrames,
                    execution.validEncodedFrames * 80L).also { words ->
                    require(words.isNotEmpty()) { "No decoded words" }
                    require(words.joinToString(" ") { it.text }.replace(Regex("\\s+"), " ").trim() ==
                        decoded.replace(Regex("\\s+"), " ").trim()) { "Word timing text mismatch" }
                }
            }
            // Timing is advisory. Never fail an otherwise successful NPU transcription for it.
            return pipelineJson(execution, profile)
                .put("timed_words", JSONArray(wordTiming.getOrDefault(emptyList()).map { word ->
                    JSONObject().put("startMs", word.startMs).put("endMs", word.endMs).put("text", word.text)
                }))
                .put("timing_error", wordTiming.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "")
                .put("timing_status", if (wordTiming.isSuccess) "decoder-frames" else "fallback")
                .put("emitted_token_count", execution.emittedTokenIds.size)
                .put("transcript", decoded)
                .put("transcript_chars", decoded.length)
                .put("decoder_topology", decoderTopology)
                .put("linked_decoder_active", linkedDecoderActive)
                .put("linked_decoder_shared_ep_contexts", linkedDecoderSharedEpContexts)
                .put("qnn_context_binary_count", if (linkedDecoderActive) 2 else sessions.size)
        }

        private fun extractFeatures(pcm: FloatArray): Pair<ParakeetPreprocessor.Result, Long> {
            val config = loadPreprocessorConfig()
            val preprocessor = ParakeetPreprocessor(config)
            val started = SystemClock.elapsedRealtime()
            val features = preprocessor.extract(pcm)
            val elapsed = SystemClock.elapsedRealtime() - started
            require(features.validEncodedFrames > 0) { "PCM produced no valid encoder frames" }
            return features to elapsed
        }

        private fun executePcm(
            pcm: FloatArray,
            profile: NpuPerformancePolicy.Profile,
            collectTokens: Boolean
        ): PipelineExecution {
            prepareRuntime()
            checkNotCancelled()
            val totalStarted = SystemClock.elapsedRealtime()
            val extracted = extractFeatures(pcm)
            checkNotCancelled()
            return executeFeatures(
                features = extracted.first,
                preprocessMs = extracted.second,
                profile = profile,
                collectTokens = collectTokens,
                preprocessingOverlapped = false,
                inlineStartedMs = totalStarted
            )
        }

        private fun executeFeatures(
            features: ParakeetPreprocessor.Result,
            preprocessMs: Long,
            profile: NpuPerformancePolicy.Profile,
            collectTokens: Boolean,
            preprocessingOverlapped: Boolean,
            inlineStartedMs: Long? = null
        ): PipelineExecution {
            prepareRuntime()
            checkNotCancelled()
            val neuralStarted = SystemClock.elapsedRealtime()

            val encoderStarted = SystemClock.elapsedRealtimeNanos()
            val encoded = runGraph(
                NpuGraphContracts.ENCODER,
                linkedMapOf(
                    "audio_signal" to features.audioSignal,
                    "subsample_mask_1" to features.subsampleMask1,
                    "subsample_mask_2" to features.subsampleMask2,
                    "subsample_mask_3" to features.subsampleMask3
                ),
                performanceMode = profile.encoderMode
            ).getValue("outputs")
            val encoderMs = elapsedMs(encoderStarted)
            checkNotCancelled()
            val paddedMax = encoderPaddedMaxAbs(encoded, features.validEncodedFrames)
            require(paddedMax <= 1.0e-4f) {
                "Encoder padded region is not zero: max_abs=$paddedMax validFrames=${features.validEncodedFrames}"
            }

            val projectorInput = FloatArray(1024 * 500)
            for (channel in 0 until 1024) {
                System.arraycopy(encoded, channel * 501, projectorInput, channel * 500, 500)
            }
            val projectorStarted = SystemClock.elapsedRealtimeNanos()
            val projected = runGraph(
                NpuGraphContracts.PROJECTOR,
                mapOf("encoder_features" to projectorInput),
                performanceMode = profile.decoderMode
            ).getValue("projected")
            val projectorMs = elapsedMs(projectorStarted)
            checkNotCancelled()

            val emitted = if (collectTokens) ArrayList<Int>() else null
            val emittedFrames = if (collectTokens) ArrayList<Int>() else null
            val decoder = OrtDecoderRuntime(projected, profile.decoderMode)
            val decoderStarted = SystemClock.elapsedRealtimeNanos()
            val summary = ParakeetTdtController.run(
                features.validEncodedFrames,
                loadTdtConfig(),
                decoder
            ) { token, frame -> emitted?.add(token); emittedFrames?.add(frame) }
            val decoderMs = elapsedMs(decoderStarted)
            require(summary.encoderExhausted) {
                "TDT controller hit its generated-decision cap before exhausting encoder frames"
            }
            val neuralMs = SystemClock.elapsedRealtime() - neuralStarted
            val totalMs = if (inlineStartedMs != null) {
                SystemClock.elapsedRealtime() - inlineStartedMs
            } else {
                preprocessMs + neuralMs
            }
            return PipelineExecution(
                emittedTokenIds = emitted ?: emptyList(),
                emittedFrames = emittedFrames ?: emptyList(),
                preprocessMs = preprocessMs,
                encoderMs = encoderMs,
                projectorMs = projectorMs,
                decoderMs = decoderMs,
                totalMs = totalMs,
                criticalPathMs = if (preprocessingOverlapped) neuralMs else totalMs,
                preprocessingOverlapped = preprocessingOverlapped,
                validEncodedFrames = features.validEncodedFrames,
                jointSteps = summary.steps
            )
        }

        private fun pipelineJson(execution: PipelineExecution, profile: NpuPerformancePolicy.Profile): JSONObject =
            JSONObject()
                .put("ok", true)
                .put("performance_profile", profile.id)
                .put("total_ms", execution.totalMs)
                .put("critical_path_ms", execution.criticalPathMs)
                .put("preprocessing_ms", execution.preprocessMs)
                .put("preprocessing_overlapped", execution.preprocessingOverlapped)
                .put("valid_encoded_frames", execution.validEncodedFrames)
                .put("encoder_ms", execution.encoderMs)
                .put("projector_ms", execution.projectorMs)
                .put("decoder_ms", execution.decoderMs)
                .put("decoder_hot_loop", "reused_view_bulk_copy_single_validation")
                .put("joint_steps", execution.jointSteps)

        @Synchronized
        fun closeSessions() {
            cancelRequested = false
            clearPreparedChunk("runtime-close")
            performanceRunOptions.values.forEach { runCatching { it.close() } }
            performanceRunOptions.clear()
            sessions.values.forEach { runCatching { it.close() } }
            sessions.clear()
            embedding = null
            tokenizer = null
            omiEmbedding = null
            omiTokenizer = null
            runCatching { embeddingChannel?.close() }
            runCatching { embeddingStream?.close() }
            embeddingChannel = null
            embeddingStream = null
            graphArtifacts.values.forEach { runCatching { it.close() } }
            graphArtifacts.clear()
            bridgeRoot?.deleteRecursively()
            bridgeRoot = null
            decoderTopology = "unprepared"
            linkedDecoderActive = false
            linkedDecoderSharedEpContexts = false
            linkedDecoderFallbackReason = null
            linkedDecoderAssetDownloaded = false
            linkedDecoderAssetAcquisitionMs = 0L
            log("sessions closed; archive descriptors released; runtime wrappers removed")
        }

        override fun close() {
            closeSessions()
            val environment = env
            if (registered && environment != null) {
                runCatching { environment.unregisterExecutionProviderLibrary(REGISTRATION_NAME) }
            }
            registered = false
            qnnDevices = emptyList()
            env = null
        }

        private fun runGraph(
            contract: NpuGraphContract,
            inputValues: Map<String, FloatArray>,
            outputDestinations: Map<String, FloatArray> = emptyMap(),
            performanceMode: NpuPerformancePolicy.QnnMode = NpuPerformancePolicy.QnnMode.PROVIDER_DEFAULT,
            validateOutputs: Boolean = true
        ): Map<String, FloatArray> {
            val loaded = requireNotNull(sessions[contract.id]) { "${contract.id} session is not prepared" }
            contract.inputs.forEach { input ->
                loaded.buffers.putInput(input.name, requireNotNull(inputValues[input.name]) { "Missing ${contract.id} input ${input.name}" })
            }
            val runOptions = runOptionsFor(performanceMode)
            val result = if (runOptions == null) {
                loaded.session.run(loaded.buffers.inputs, loaded.buffers.pinnedOutputs)
            } else {
                loaded.session.run(loaded.buffers.inputs, emptySet<String>(), loaded.buffers.pinnedOutputs, runOptions)
            }
            result.use {
                val outputs = linkedMapOf<String, FloatArray>()
                contract.outputs.forEach { output ->
                    outputs[output.name] = loaded.buffers.copyOutput(
                        output.name,
                        outputDestinations[output.name],
                        validateFinite = validateOutputs
                    )
                }
                return outputs
            }
        }

        private inner class OrtDecoderRuntime(
            private val projected: FloatArray,
            private val performanceMode: NpuPerformancePolicy.QnnMode
        ) : ParakeetTdtController.Runtime {
            private val state = PredictorState()
            private val frame = FloatArray(640)
            private val embeddingRow = FloatArray(640)
            private val jointLogits = FloatArray(8198)
            private val predictorInputs = linkedMapOf(
                "token_embedding" to embeddingRow,
                "h0" to state.h0,
                "c0" to state.c0,
                "h1" to state.h1,
                "c1" to state.c1
            )
            private val predictorOutputs = linkedMapOf(
                "predictor_out" to state.predictorOut,
                "h0_out" to state.h0,
                "c0_out" to state.c0,
                "h1_out" to state.h1,
                "c1_out" to state.c1
            )
            private val jointInputs = linkedMapOf(
                "encoder_projected_frame" to frame,
                "predictor_out" to state.predictorOut
            )
            private val jointOutputs = mapOf("tdt_logits" to jointLogits)

            override fun predict(tokenId: Int) {
                checkNotCancelled()
                readEmbedding(tokenId, embeddingRow)
                runGraph(NpuGraphContracts.PREDICTOR, predictorInputs, predictorOutputs, performanceMode)
            }

            override fun joint(frameIndex: Int): FloatArray {
                checkNotCancelled()
                require(frameIndex in 0 until 500) { "Projected frame index out of range: $frameIndex" }
                for (channel in 0 until 640) frame[channel] = projected[channel * 500 + frameIndex]
                runGraph(
                    NpuGraphContracts.JOINT,
                    jointInputs,
                    jointOutputs,
                    performanceMode,
                    validateOutputs = false
                )
                return jointLogits
            }
        }

        private fun readEmbedding(tokenId: Int, destination: FloatArray) {
            require(tokenId in 0 until 8193) { "Predictor token id out of range: $tokenId" }
            val source = requireNotNull(embedding) { "Predictor embedding table is not mapped" }.duplicate()
            source.position(tokenId * 640)
            source.get(destination, 0, 640)
        }

        private fun loadPreprocessorConfig(): ParakeetPreprocessor.Config {
            val preprocessing = JSONObject(requireNotNull(NpuPackageStore.readSmallTextArtifact(service, "config/preprocessing.json")))
            val processorRoot = JSONObject(requireNotNull(NpuPackageStore.readSmallTextArtifact(service, "config/processor_config.json")))
            val processor = processorRoot.optJSONObject("feature_extractor") ?: processorRoot
            val sampleRate = preprocessing.optInt("sample_rate_hz", processor.optInt("sampling_rate", 0))
            val featureSize = preprocessing.optInt("feature_bins", processor.optInt("feature_size", 0))
            val hop = preprocessing.optInt("hop_length", processor.optInt("hop_length", 0))
            val nFft = preprocessing.optInt("n_fft", processor.optInt("n_fft", 0))
            val win = processor.optInt("win_length", 0)
            val preemphasis = preprocessing.optDouble("preemphasis", processor.optDouble("preemphasis", Double.NaN)).toFloat()
            val physicalFrames = preprocessing.optInt("physical_feature_frames", 0)
            val chunkSeconds = preprocessing.optInt("chunk_seconds", 40)
            return ParakeetPreprocessor.Config(
                sampleRate = sampleRate,
                featureSize = featureSize,
                hopLength = hop,
                nFft = nFft,
                winLength = win,
                preemphasis = preemphasis,
                maxSamples = sampleRate * chunkSeconds,
                physicalFrames = physicalFrames
            ).also { it.requireValidatedContract() }
        }

        private fun loadTdtConfig(): ParakeetTdtConfig {
            val root = JSONObject(requireNotNull(NpuPackageStore.readSmallTextArtifact(service, "config/tdt_decoder.json")))
            val controller = root.getJSONObject("controller")
            val durationsJson = controller.getJSONArray("durations")
            val durations = IntArray(durationsJson.length()) { durationsJson.getInt(it) }
            val special = controller.optJSONObject("special_token_ids")
            val blank = controller.getInt("blank_token_id")
            return ParakeetTdtConfig(
                blankTokenId = blank,
                decoderStartTokenId = special?.optInt("decoder_start_token_id", blank) ?: blank,
                durations = durations,
                maxSymbolsPerStep = controller.getInt("max_symbols_per_step")
            ).also { it.requireValidatedContract() }
        }

        private fun loadTokenizer(): ParakeetTokenizer =
            ParakeetTokenizer.fromJson(
                requireNotNull(NpuPackageStore.readSmallTextArtifact(service, "config/tokenizer/tokenizer.json")) {
                    "Tokenizer JSON is missing from the verified package"
                }
            )

        private fun runtimePreparedJson(): JSONObject = JSONObject()
            .put("ok", true)
            .put("storage_mode", runtimeStorageMode())
            .put("persistent_duplicate_model_bytes", 0L)
            .put("temporary_context_bytes", bridgeRoot?.let(::temporaryContextBytes) ?: 0L)
            .put("session_count", sessions.size)
            .put("decoder_topology", decoderTopology)

        private fun runtimeStorageMode(): String {
            val modes = graphArtifacts.values.map { it.accessMode }.toSet()
            return when {
                modes.isEmpty() -> "unprepared"
                "linked-transient-staged-shared" in modes -> "archive-with-linked-decoder-transient-staging"
                "transient-staged" in modes -> "archive-with-transient-staging"
                modes == setOf("proc-fd-direct") -> "saf-proc-fd-direct"
                modes == setOf("resolved-real-path") -> "resolved-real-path-direct"
                else -> "mixed-direct"
            }
        }

        private fun temporaryContextBytes(root: File): Long =
            root.walkTopDown()
                .filter { it.isFile && it.name.endsWith("-staged.bin") }
                .sumOf { it.length() }

        private fun runOptionsFor(mode: NpuPerformancePolicy.QnnMode): OrtSession.RunOptions? {
            val qnnValue = mode.qnnValue ?: return null
            return performanceRunOptions.getOrPut(mode) {
                OrtSession.RunOptions().apply { addRunConfigEntry("qnn.perf_mode", qnnValue) }
            }
        }

        companion object {
            private const val REGISTRATION_NAME = "QNNExecutionProvider"
            private const val EMBEDDING_PATH = "model/predictor_embedding.f32"
            private const val EMBEDDING_BYTES = 20_974_080L
            private const val STAGING_HEADROOM_BYTES = 64L * 1024L * 1024L
        }
    }
}
