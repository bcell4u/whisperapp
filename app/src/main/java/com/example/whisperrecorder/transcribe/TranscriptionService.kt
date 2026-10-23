package com.example.whisperrecorder.transcribe

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import androidx.core.app.NotificationCompat
import com.example.whisperrecorder.MainActivity
import com.example.whisperrecorder.R
import com.example.whisperrecorder.data.RecordingDeletionManager
import com.example.whisperrecorder.data.RecordingStore
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.models.ModelManager
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import kotlinx.coroutines.*
import java.io.File
import java.util.ArrayDeque

class TranscriptionService : Service() {
    companion object {
        const val ACTION_TRANSCRIBE = "transcribe.start"
        const val ACTION_CANCEL = "transcribe.cancel"
        const val EXTRA_RECORDING_ID = "recording_id"
        const val EXTRA_MEDIA_URI = "media_uri"
        const val EXTRA_AUDIO_URI = "audio_uri" // Legacy queue/request compatibility.
        const val EXTRA_TREE_URI = "tree_uri"
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_ENGINE = "transcription_engine"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DURATION_MS = "duration_ms"
        const val EXTRA_KEEP_QUEUE = "keep_queue"
        private const val CHANNEL = "transcription"
        private const val NOTIFICATION_ID = 2001
    }

    private data class Request(
        val id: String,
        val mediaUri: Uri,
        val treeUri: Uri,
        val modelPath: String,
        val engine: AsrEngine,
        val title: String,
        val durationMs: Long
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ArrayDeque<Request>()
    private var job: Job? = null
    private var currentRecordingId: String? = null
    @Volatile private var activeSession: AsrSession? = null
    private data class SessionKey(val engine: AsrEngine, val modelPath: String, val threads: Int)
    private var warmSessionKey: SessionKey? = null
    private var warmSession: AsrSession? = null
    private var transcriptionWakeLock: PowerManager.WakeLock? = null

    private fun acquireTranscriptionWakeLock() {
        val lock = transcriptionWakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:transcription")
            .apply { setReferenceCounted(false) }
            .also { transcriptionWakeLock = it }
        if (!lock.isHeld) {
            lock.acquire()
            DiagnosticLogger.append(this, "performance wakeLock=acquired type=PARTIAL_WAKE_LOCK")
        }
    }

    private fun releaseTranscriptionWakeLock() {
        transcriptionWakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                DiagnosticLogger.append(this, "performance wakeLock=released")
            }
        }
    }

    /**
     * Keep the Java/JNI caller at foreground thread priority while model setup or inference is
     * running. Native worker pools created by a backend can inherit this priority, while the
     * priority is restored immediately after the blocking native call returns.
     */
    private inline fun <T> withForegroundInferencePriority(block: () -> T): T {
        val tid = Process.myTid()
        val originalPriority = runCatching { Process.getThreadPriority(tid) }
            .getOrDefault(Process.THREAD_PRIORITY_DEFAULT)
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND) }
        return try {
            block()
        } finally {
            runCatching { Process.setThreadPriority(originalPriority) }
        }
    }

    private fun currentProcessImportance(): Int {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance
    }

    private data class InferenceSlice(
        val recognition: AsrRecognition,
        val startSample: Int,
        val endSample: Int
    )

    private data class ChunkPrefetch(
        val chunk: PcmDecoder.TimedChunk?,
        val decodeWorkMs: Long,
        val prepared: AsrPreparedChunk?,
        val preparationWallMs: Long?,
        val preparationError: String? = null
    )

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Transcription", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                DiagnosticLogger.append(this, "cancel requested recording=${currentRecordingId ?: "none"}")
                if (!intent.getBooleanExtra(EXTRA_KEEP_QUEUE, false)) {
                    synchronized(pending) { pending.clear() }
                }
                // Cancellation is cooperative. Signal native code first, then cancel the coroutine,
                // but keep the foreground service and native session alive until the in-flight JNI
                // call has actually returned. A cancelled Job reports isActive=false immediately,
                // even while its thread is still blocked in native code, so stopping/destroying the
                // service here can free the native session underneath transcribe_run().
                activeSession?.cancel()
                job?.cancel(CancellationException("User stopped transcription"))
                val current = TranscriptionRuntime.state.value
                if (currentRecordingId != null && job?.isCompleted == false) {
                    TranscriptionRuntime.update(
                        current.copy(
                            active = true,
                            stage = "Stopping",
                            detail = "Stopping safely after the current native inference step",
                            error = null
                        )
                    )
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID,
                        workingNotification("Stopping transcription safely…", progress = null, showStopAction = false)
                    )
                    DiagnosticLogger.append(this, "cancel waiting for in-flight native inference to return")
                }
            }
            ACTION_TRANSCRIBE -> {
                val request = parseRequest(intent) ?: return START_NOT_STICKY
                if (job?.isCompleted == false) {
                    synchronized(pending) {
                        if (request.id != currentRecordingId && pending.none { it.id == request.id }) {
                            pending.addLast(request)
                        }
                    }
                } else {
                    startRequest(request)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun parseRequest(intent: Intent): Request? {
        val id = intent.getStringExtra(EXTRA_RECORDING_ID) ?: return null
        val mediaUri = (intent.getStringExtra(EXTRA_MEDIA_URI) ?: intent.getStringExtra(EXTRA_AUDIO_URI))
            ?.let(Uri::parse) ?: return null
        val treeUri = intent.getStringExtra(EXTRA_TREE_URI)?.let(Uri::parse) ?: return null
        val modelPath = intent.getStringExtra(EXTRA_MODEL_PATH) ?: return null
        val engine = AsrEngine.fromId(intent.getStringExtra(EXTRA_ENGINE))
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Recording"
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
        return Request(id, mediaUri, treeUri, modelPath, engine, title, durationMs)
    }

    private fun startRequest(request: Request) {
        val available = RecordingStore.hasMedia(this, request.id)
        if (!available || RecordingDeletionManager.isDeleting(request.id)) {
            val next = synchronized(pending) { if (pending.isEmpty()) null else pending.removeFirst() }
            if (next != null) startRequest(next) else {
                closeWarmSession(); releaseTranscriptionWakeLock(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
            return
        }
        currentRecordingId = request.id
        val sessionStarted = System.currentTimeMillis()
        val modelName = when (request.engine) {
            AsrEngine.WHISPER -> File(request.modelPath).name.removePrefix("ggml-").removeSuffix(".bin")
            AsrEngine.PARAKEET_NPU -> "NVIDIA Parakeet TDT 0.6B v3 · Qualcomm NPU"
            AsrEngine.OMI_NPU -> "Omi Med STT v1 · Qualcomm NPU"
            else -> LocalAsrModelManager.byEngine(request.engine)?.name ?: File(request.modelPath).name.removeSuffix(".gguf")
        }
        TranscriptionRuntime.update(
            TranscriptionState(
                recordingId = request.id, active = true, progress = 0, processedMs = 0,
                totalMs = request.durationMs, chunk = 0, totalChunks = 0, modelName = modelName,
                stage = "Preparing media", detail = "Starting transcription", startedAtMs = sessionStarted
            )
        )
        DiagnosticLogger.append(this, "SESSION start engine=${request.engine.id} durationMs=${request.durationMs}")
        startForeground(NOTIFICATION_ID, workingNotification("Preparing media…", progress = null))
        acquireTranscriptionWakeLock()

        var decodeElapsedMs = 0L
        var inferenceTotalMs = 0L
        var modelLoadElapsedMs = 0L

        job = scope.launch {
            var preparedSource: PcmDecoder.PreparedSource? = null
            // Keep the successfully decoded words available to the error handler so a
            // late fatal error cannot discard an entire long lecture.
            val partialTranscript = StringBuilder()
            try {
                val stored = RecordingStore.find(this@TranscriptionService, request.id)
                require(stored?.mediaUri != null) { "Recording media is no longer available" }
                require(!RecordingDeletionManager.isDeleting(request.id)) { "Recording is being deleted" }
                if (request.engine == AsrEngine.PARAKEET_NPU) {
                    require(request.modelPath == NpuPackageStore.TRANSCRIPTION_MODEL_KEY) { "Selected NPU package identity is invalid" }
                    require(NpuPackageStore.archiveState(this@TranscriptionService) == NpuArchiveState.READY) { "Parakeet NPU package is not ready" }
                    NpuPackageStore.runtimeArchiveInfo(this@TranscriptionService)
                } else if (request.engine == AsrEngine.OMI_NPU) {
                    require(request.modelPath == OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY) { "Selected Omi NPU package identity is invalid" }
                    require(OmiNpuPackageStore.archiveState(this@TranscriptionService) == NpuArchiveState.READY) { "Omi NPU assets are incomplete" }
                } else {
                    require(File(request.modelPath).isFile) { "Selected transcription model is missing" }
                    if (request.engine != AsrEngine.WHISPER) {
                        val spec = requireNotNull(LocalAsrModelManager.byEngine(request.engine)) { "Unknown transcription engine ${request.engine.id}" }
                        require(LocalAsrModelManager.isInstalled(this@TranscriptionService, spec)) { "${spec.name} model file is incomplete" }
                        require(LocalAsrModelManager.modelFile(this@TranscriptionService, spec).canonicalPath == File(request.modelPath).canonicalPath) {
                            "Selected ${spec.name} model path is invalid"
                        }
                    }
                }

                updateStage(request.id, "Preparing media", "Inspecting source audio track", modelName, request.durationMs)
                DiagnosticLogger.append(this@TranscriptionService, "media inspect start")
                preparedSource = try {
                    PcmDecoder.prepare(this@TranscriptionService, request.mediaUri)
                } catch (t: Throwable) {
                    DiagnosticLogger.appendError(this@TranscriptionService, "media inspect failed", t)
                    throw t
                }
                val streamInfo = requireNotNull(preparedSource).info
                ensureActive()
                val audioDurationMs = streamInfo.durationMs.takeIf { it > 0L } ?: request.durationMs
                require(audioDurationMs >= 1_000L) { "Media audio is shorter than one second" }
                val total = streamInfo.audioStartMs + audioDurationMs
                val performancePreset = AppPrefs.getPerformancePreset(this@TranscriptionService)
                val chunkMinutes = if (performancePreset == "custom") AppPrefs.getChunkMinutes(this@TranscriptionService) else 2
                val segmentPlan = SegmentationPolicy.plan(
                    engine = request.engine,
                    totalDurationMs = audioDurationMs,
                    sourceSampleRate = streamInfo.sampleRate,
                    whisperChunkMinutes = chunkMinutes
                )
                val segmentSeconds = segmentPlan.segmentSeconds
                val overlapSeconds = segmentPlan.overlapSeconds
                val totalChunks = PcmDecoder.countSegmentsByDuration(
                    durationMs = audioDurationMs,
                    segmentSeconds = segmentSeconds,
                    overlapSeconds = overlapSeconds
                ).coerceAtLeast(1)
                DiagnosticLogger.append(
                    this@TranscriptionService,
                    "media stream ready sampleRate=${streamInfo.sampleRate} channels=${streamInfo.channelCount} trackMime=${streamInfo.trackMime} audioStartMs=${streamInfo.audioStartMs} audioDurationMs=$audioDurationMs noFullPcmStaging=true"
                )
                updateStage(request.id, "Loading model", "Loading $modelName", modelName, total, totalChunks = totalChunks)
                DiagnosticLogger.append(this@TranscriptionService, "model acquire start engine=${request.engine.id}")
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                val configuredThreads = AppPrefs.effectiveTranscriptionThreads(this@TranscriptionService, cores)
                val threads = if (request.engine.supportsConfigurableThreads) configuredThreads else 0
                val requestedLanguage = AppPrefs.getTranscriptionLanguage(this@TranscriptionService)
                val language = request.engine.resolveLanguage(
                    requestedLanguage = requestedLanguage,
                    whisperEnglishOnly = request.engine == AsrEngine.WHISPER && ModelManager.isEnglishOnly(File(request.modelPath))
                )
                val modelLoadStarted = System.currentTimeMillis()
                val sessionKey = SessionKey(request.engine, if (request.engine == AsrEngine.PARAKEET_NPU) NpuPackageStore.TRANSCRIPTION_MODEL_KEY else if (request.engine == AsrEngine.OMI_NPU) OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY else File(request.modelPath).canonicalPath, threads)
                val reusedSession = warmSession != null && warmSessionKey == sessionKey
                val session = if (reusedSession) {
                    requireNotNull(warmSession)
                } else {
                    closeWarmSession()
                    try {
                        withForegroundInferencePriority {
                            AsrSessionFactory.open(
                                context = this@TranscriptionService,
                                engine = request.engine,
                                modelPath = request.modelPath,
                                threads = threads
                            )
                        }
                    } catch (t: Throwable) {
                        throw t
                    }.also {
                        warmSession = it
                        warmSessionKey = sessionKey
                    }
                }
                ensureActive()
                activeSession = session
                modelLoadElapsedMs = System.currentTimeMillis() - modelLoadStarted
                DiagnosticLogger.append(
                    this@TranscriptionService,
                    "model acquire complete engine=${request.engine.id} elapsedMs=$modelLoadElapsedMs reused=$reusedSession"
                )
                val nativeInfo = session.nativeInfo
                val threadLabel = if (request.engine.supportsConfigurableThreads) threads.toString() else "runtime-managed"
                DiagnosticLogger.append(this@TranscriptionService, "performance engine=${request.engine.id} nativeBuild=Release cpuCores=$cores threads=$threadLabel preset=$performancePreset language=$language chunkMinutes=$chunkMinutes segmentation=${segmentPlan.strategy} runtimeMaxSegmentSeconds=${segmentPlan.runtimeMaxSegmentSeconds} pcmSafetyMaxSeconds=${segmentPlan.pcmSafetyMaxSeconds} maxSegmentSeconds=${segmentPlan.maxSegmentSeconds} segmentSeconds=$segmentSeconds overlapSeconds=$overlapSeconds")
                DiagnosticLogger.append(this@TranscriptionService, "native systemInfo=$nativeInfo")

                val verboseDiagnostics = AppPrefs.isVerboseDiagnostics(this@TranscriptionService)

                val text = partialTranscript
                val timelineCues = mutableListOf<TranscriptCue>()
                var lastProcessedAudioEndMs = 0L
                var decodedEndMs = 0L
                var chunk = 0
                var unrecoveredNpuChunks = 0
                try {
                    requireNotNull(preparedSource).open16kSegmentStream(
                        segmentSeconds = segmentSeconds, overlapSeconds = overlapSeconds
                    ).use { stream ->
                        val firstDecodeStarted = System.currentTimeMillis()
                        var currentChunk: PcmDecoder.TimedChunk? = if (stream.hasNext()) stream.next() else null
                        var currentPrepared: AsrPreparedChunk? = null
                        decodeElapsedMs += System.currentTimeMillis() - firstDecodeStarted
                        while (currentChunk != null) {
                            val timedChunk = requireNotNull(currentChunk)
                            decodedEndMs = maxOf(decodedEndMs, timedChunk.endMs)
                            val samples = timedChunk.samples
                            val prefetchedNext = if (request.engine == AsrEngine.PARAKEET || request.engine == AsrEngine.PARAKEET_NPU || request.engine == AsrEngine.OMI) async(Dispatchers.IO) {
                                val decodeStarted = System.currentTimeMillis()
                                val next = if (stream.hasNext()) stream.next() else null
                                val decodeWorkMs = System.currentTimeMillis() - decodeStarted
                                var prepared: AsrPreparedChunk? = null
                                var preparationWallMs: Long? = null
                                var preparationError: String? = null
                                if (request.engine == AsrEngine.PARAKEET_NPU && next != null) {
                                    val prepStarted = System.currentTimeMillis()
                                    try {
                                        prepared = session.prepare(next.samples, language, threads)
                                        preparationWallMs = prepared?.preparationWallMs
                                            ?: (System.currentTimeMillis() - prepStarted)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (t: Throwable) {
                                        preparationWallMs = System.currentTimeMillis() - prepStarted
                                        preparationError = "${t::class.java.name}: ${t.message ?: "(no message)"}"
                                        DiagnosticLogger.append(
                                            this@TranscriptionService,
                                            "npu prefetch preparation failed nextChunk=${chunk + 1}/$totalChunks type=${t::class.java.name}; falling back to inline preprocessing"
                                        )
                                    }
                                }
                                ChunkPrefetch(next, decodeWorkMs, prepared, preparationWallMs, preparationError)
                            } else null
                        ensureActive()
                        if (RecordingDeletionManager.isDeleting(request.id)) throw CancellationException("Recording deletion requested")
                        chunk++
                        val processedBefore = timedChunk.startMs.coerceAtMost(total)
                        val progressBefore = if (total > 0) ((processedBefore * 100) / total).toInt() else 0
                        val inferenceStarted = System.currentTimeMillis()
                        TranscriptionRuntime.update(
                            TranscriptionState(
                                recordingId = request.id, active = true, progress = progressBefore,
                                processedMs = processedBefore, totalMs = total, chunk = chunk, totalChunks = totalChunks,
                                modelName = modelName, stage = "Transcribing", detail = "Segment $chunk of $totalChunks",
                                elapsedMs = System.currentTimeMillis() - sessionStarted, startedAtMs = sessionStarted
                            )
                        )
                        val chunkEndMs = timedChunk.endMs.coerceAtMost(total)
                        DiagnosticLogger.append(this@TranscriptionService, "chunk boundary engine=${request.engine.id} chunk=$chunk/$totalChunks audioStartMs=$processedBefore audioEndMs=$chunkEndMs samples=${samples.size}")
                        if (verboseDiagnostics && request.engine != AsrEngine.WHISPER) {
                            val stats = audioStats(samples)
                            DiagnosticLogger.append(
                                this@TranscriptionService,
                                "audio stats engine=${request.engine.id} chunk=$chunk/$totalChunks peak=${formatAudioLevel(stats.peak)} rms=${formatAudioLevel(stats.rms)} nonZeroPct=${formatPercent(stats.nonZeroFraction)}"
                            )
                        }
                        val powerManager = getSystemService(PowerManager::class.java)
                        DiagnosticLogger.append(
                            this@TranscriptionService,
                            "inference start engine=${request.engine.id} chunk=$chunk/$totalChunks samples=${samples.size} threads=$threads importance=${currentProcessImportance()} interactive=${powerManager.isInteractive} wakeLockHeld=${transcriptionWakeLock?.isHeld == true}"
                        )
                        getSystemService(NotificationManager::class.java).notify(
                            NOTIFICATION_ID,
                            workingNotification("Transcribing segment $chunk of $totalChunks", if (totalChunks > 1) progressBefore else null)
                        )

                        val heartbeat = launch {
                            var lastLoggedSecond = 0L
                            while (isActive) {
                                delay(2000)
                                val chunkElapsed = System.currentTimeMillis() - inferenceStarted
                                val elapsed = System.currentTimeMillis() - sessionStarted
                                val seconds = elapsed / 1000L
                                val current = TranscriptionRuntime.state.value
                                if (current.recordingId == request.id && current.active) {
                                    val slowNote = if (seconds >= 60) " • still processing" else ""
                                    val detail = "Segment $chunk of $totalChunks • ${seconds}s total • ${chunkElapsed / 1000L}s this segment$slowNote"
                                    TranscriptionRuntime.update(current.copy(stage = "Transcribing", detail = detail, elapsedMs = elapsed, startedAtMs = sessionStarted))
                                    if (seconds % 5L == 0L) {
                                        getSystemService(NotificationManager::class.java).notify(
                                            NOTIFICATION_ID,
                                            workingNotification("Transcribing • ${seconds}s elapsed$slowNote", null)
                                        )
                                    }
                                }
                                if (verboseDiagnostics && seconds >= lastLoggedSecond + 10) {
                                    DiagnosticLogger.append(this@TranscriptionService, "inference heartbeat engine=${request.engine.id} chunk=$chunk/$totalChunks sessionElapsedMs=$elapsed chunkElapsedMs=$chunkElapsed")
                                    lastLoggedSecond = seconds
                                }
                            }
                        }
                        val preparedForChunk = currentPrepared.also { currentPrepared = null }
                        var slices = try {
                            if (request.engine == AsrEngine.PARAKEET_NPU && preparedForChunk != null) {
                                listOf(
                                    InferenceSlice(
                                        recognition = withForegroundInferencePriority {
                                            session.transcribePrepared(preparedForChunk, language, threads)
                                        },
                                        startSample = 0,
                                        endSample = samples.size
                                    )
                                )
                            } else {
                                currentCoroutineContext().ensureActive()
                                listOf(
                                    InferenceSlice(
                                        recognition = withForegroundInferencePriority {
                                            session.transcribe(samples, language, threads)
                                        },
                                        startSample = 0,
                                        endSample = samples.size
                                    )
                                )
                            }
                        } finally {
                            runCatching { preparedForChunk?.close() }
                            heartbeat.cancel()
                        }
                        // A blank NPU decode must not invalidate the preceding hour of a lecture.
                        // Retry once through inline PCM inference (rather than the prepared path).
                        // If it is still blank, mark that interval visibly instead of inventing
                        // words or silently losing the audio segment. Other inference errors fail
                        // normally; only successful, explicitly empty results use this policy.
                        if (request.engine == AsrEngine.PARAKEET_NPU &&
                            slices.size == 1 && slices[0].recognition.text.isBlank()) {
                            val stats = audioStats(samples)
                            DiagnosticLogger.append(
                                this@TranscriptionService,
                                "npu blank chunk=$chunk/$totalChunks action=retry-inline " +
                                    "startMs=$processedBefore endMs=$chunkEndMs " +
                                    "first=${slices[0].recognition.diagnostics.trim()} " +
                                    "peak=${formatAudioLevel(stats.peak)} rms=${formatAudioLevel(stats.rms)} " +
                                    "nonZeroPct=${formatPercent(stats.nonZeroFraction)}"
                            )
                            ensureActive()
                            val retry = withForegroundInferencePriority { session.transcribe(samples, language, threads) }
                            slices = listOf(InferenceSlice(retry, 0, samples.size))
                            if (retry.text.isBlank()) {
                                unrecoveredNpuChunks++
                                val marker = NpuBlankTranscriptRecovery.marker(processedBefore, chunkEndMs)
                                // The marker is intentionally visible in the saved transcript:
                                // neither silence nor lost speech may be silently misrepresented.
                                TranscriptCleaner.merge(text, marker)
                                val cueStart = maxOf(processedBefore, lastProcessedAudioEndMs)
                                if (chunkEndMs > cueStart) {
                                    timelineCues += TranscriptCue(cueStart, chunkEndMs, marker)
                                }
                                DiagnosticLogger.append(
                                    this@TranscriptionService,
                                    "npu blank chunk=$chunk/$totalChunks action=marked-untranscribed " +
                                        "retry=${retry.diagnostics.trim()} totalMarked=$unrecoveredNpuChunks"
                                )
                            } else {
                                DiagnosticLogger.append(
                                    this@TranscriptionService,
                                    "npu blank chunk=$chunk/$totalChunks action=recovered " +
                                        "chars=${retry.text.length} retry=${retry.diagnostics.trim()}"
                                )
                            }
                        }
                        val chunkInferenceMs = System.currentTimeMillis() - inferenceStarted
                        inferenceTotalMs += chunkInferenceMs
                        val totalChars = slices.sumOf { it.recognition.text.length }
                        val retryNote = if (slices.size > 1) " adaptiveSlices=${slices.size}" else ""
                        val diagnostics = slices.joinToString(separator = "") { it.recognition.diagnostics }.trim()
                        DiagnosticLogger.append(
                            this@TranscriptionService,
                            "inference complete engine=${request.engine.id} chunk=$chunk/$totalChunks elapsedMs=$chunkInferenceMs chars=$totalChars$retryNote${if (diagnostics.isNotBlank()) " $diagnostics" else ""}"
                        )

                        ensureActive()
                        if (RecordingDeletionManager.isDeleting(request.id)) throw CancellationException("Recording deletion requested")
                        for (slice in slices) {
                            val part = slice.recognition.text
                            if (part.isBlank()) continue
                            val sliceStartMs = (timedChunk.startMs + (slice.startSample * 1000L) / 16_000L)
                                .coerceAtMost(chunkEndMs)
                            val sliceEndMs = (timedChunk.startMs + (slice.endSample * 1000L) / 16_000L)
                                .coerceAtMost(chunkEndMs)
                            if (sliceEndMs <= sliceStartMs) continue

                            val cleanup = TranscriptCleaner.merge(
                                text,
                                part,
                                allowBoundaryWordOverlap = request.engine.usesBoundaryOverlap && (overlapSeconds > 0 || slices.size > 1)
                            )
                            if (cleanup.repetitionsRemoved > 0 || cleanup.boundaryDuplicateRemoved) {
                                DiagnosticLogger.append(
                                    this@TranscriptionService,
                                    "transcript cleanup engine=${request.engine.id} chunk=$chunk/$totalChunks repetitionsRemoved=${cleanup.repetitionsRemoved} boundaryDuplicateRemoved=${cleanup.boundaryDuplicateRemoved} rawChars=${part.length} keptChars=${cleanup.text.length}"
                                )
                            }
                            if (cleanup.text.isNotBlank()) {
                                val cueFloor = maxOf(sliceStartMs, lastProcessedAudioEndMs).coerceAtMost(sliceEndMs)
                                val mayNeedSpeechTiming = request.engine == AsrEngine.QWEN06 ||
                                    request.engine == AsrEngine.PARAKEET_NPU || request.engine == AsrEngine.OMI_NPU
                                val cueResult = TranscriptTimelineBuilder.cuesForChunkDetailed(
                                    cleanedText = cleanup.text,
                                    chunkStartMs = sliceStartMs,
                                    chunkEndMs = sliceEndMs,
                                    // Use native Parakeet NPU words when valid. If cleanup changed
                                    // them, fall back to speech-active PCM rather than uniform timing.
                                    nativeSegments = slice.recognition.timedSegments,
                                    wordByWord = request.engine == AsrEngine.PARAKEET_NPU || request.engine == AsrEngine.OMI_NPU,
                                    minimumStartMs = cueFloor,
                                    speechSamples = if (mayNeedSpeechTiming) samples else null,
                                    speechSampleStart = if (mayNeedSpeechTiming) slice.startSample else 0,
                                    speechSampleEnd = if (mayNeedSpeechTiming) slice.endSample else 0
                                )
                                timelineCues += cueResult.cues
                                if (request.engine == AsrEngine.PARAKEET_NPU || request.engine == AsrEngine.OMI_NPU) {
                                    DiagnosticLogger.append(this@TranscriptionService,
                                        "npu word-sync chunk=$chunk/$totalChunks nativeWords=${cueResult.nativeProvided} " +
                                        "alignedWords=${cueResult.nativeAligned} savedCues=${cueResult.cues.size} " +
                                        "mode=${cueResult.mode} reason=${cueResult.reason} " +
                                        "overlapDiscarded=${cueResult.overlapDiscarded} overlapClipped=${cueResult.overlapClipped} " +
                                        "cleanupDropped=${cueResult.droppedForCleanup} " +
                                        "startMs=$sliceStartMs endMs=$sliceEndMs")
                                }
                            }
                            lastProcessedAudioEndMs = maxOf(lastProcessedAudioEndMs, sliceEndMs)
                        }
                        lastProcessedAudioEndMs = maxOf(lastProcessedAudioEndMs, chunkEndMs)

                        val processed = timedChunk.endMs.coerceAtMost(total)
                        val progress = if (total > 0) ((processed * 100) / total).toInt().coerceIn(0, 99) else 0
                            TranscriptionRuntime.update(
                                TranscriptionState(
                                    recordingId = request.id, active = true, progress = progress, processedMs = processed,
                                    totalMs = total, chunk = chunk, totalChunks = totalChunks, modelName = modelName,
                                    stage = "Transcribing", detail = "Completed segment $chunk of $totalChunks",
                                    elapsedMs = System.currentTimeMillis() - sessionStarted, startedAtMs = sessionStarted
                                )
                            )
                            if (prefetchedNext != null) {
                                val waitStarted = System.currentTimeMillis()
                                val nextPrefetch = prefetchedNext.await()
                                val waitMs = System.currentTimeMillis() - waitStarted
                                decodeElapsedMs += nextPrefetch.decodeWorkMs
                                currentChunk = nextPrefetch.chunk
                                currentPrepared = nextPrefetch.prepared
                                if (request.engine == AsrEngine.PARAKEET_NPU && nextPrefetch.chunk != null) {
                                    DiagnosticLogger.append(
                                        this@TranscriptionService,
                                        "npu prefetch nextChunk=${chunk + 1}/$totalChunks decodeMs=${nextPrefetch.decodeWorkMs} preparationWallMs=${nextPrefetch.preparationWallMs ?: -1L} postInferenceWaitMs=$waitMs prepared=${nextPrefetch.prepared != null}"
                                    )
                                }
                            } else {
                                val decodeStarted = System.currentTimeMillis()
                                currentChunk = if (stream.hasNext()) stream.next() else null
                                decodeElapsedMs += System.currentTimeMillis() - decodeStarted
                                currentPrepared = null
                            }
                        }
                    }
                    val decodedAudioDurationMs = (decodedEndMs - streamInfo.audioStartMs).coerceAtLeast(0L)
                    require(chunk > 0 && decodedAudioDurationMs >= 1_000L) { "Decoded media audio is shorter than one second" }
                    if (audioDurationMs >= 5_000L) {
                        val minimumExpected = (audioDurationMs * 80L) / 100L
                        require(decodedAudioDurationMs >= minimumExpected) {
                            "Media audio decoding stopped early (${decodedAudioDurationMs / 1000.0}s decoded from ${audioDurationMs / 1000.0}s audio track)"
                        }
                    }
                } finally {
                    activeSession = null
                }

                ensureActive()
                if (RecordingDeletionManager.isDeleting(request.id)) throw CancellationException("Recording deletion requested")
                require(text.isNotBlank()) { "The transcription engine returned an empty transcript" }
                require(RecordingStore.hasMedia(this@TranscriptionService, request.id)) {
                    "Recording was deleted before transcription completed"
                }

                updateStage(request.id, "Saving transcript", "Writing transcript file", modelName, total, totalChunks, totalChunks)
                DiagnosticLogger.append(this@TranscriptionService, "save transcript start chars=${text.length}")
                val saveStarted = System.currentTimeMillis()
                val previousItem = RecordingStore.find(this@TranscriptionService, request.id)
                val previousTranscriptUri = previousItem?.transcriptUri
                val previousTimingPath = previousItem?.transcriptTimingPath
                val transcriptDir = WorkspaceStorage.transcriptsDirectory(this@TranscriptionService, request.treeUri)
                val transcriptUri = SafStorage.writeTextToDirectory(
                    this@TranscriptionService,
                    transcriptDir,
                    "${request.title}.txt",
                    text.toString().trim()
                )
                val timingPath = if (timelineCues.isNotEmpty()) {
                    runCatching {
                        TranscriptTimelineStore.save(
                            this@TranscriptionService,
                            request.id,
                            TranscriptTimeline(timelineCues)
                        )
                    }.onFailure {
                        DiagnosticLogger.appendError(this@TranscriptionService, "save transcript timing metadata failed", it)
                    }.getOrNull()
                } else null
                val saveElapsedMs = System.currentTimeMillis() - saveStarted
                val wallTimeMs = System.currentTimeMillis() - sessionStarted
                RecordingStore.setTranscript(
                    this@TranscriptionService,
                    request.id,
                    transcriptUri.toString(),
                    timingPath,
                    transcriptSyncProfileId = "${request.engine.id}:$modelName",
                    transcriptSyncProfileLabel = modelName,
                    transcriptionElapsedMs = wallTimeMs
                )
                if (previousTimingPath != null && previousTimingPath != timingPath) {
                    TranscriptTimelineStore.delete(previousTimingPath)
                }
                DiagnosticLogger.append(this@TranscriptionService, "save transcript complete elapsedMs=$saveElapsedMs cues=${timelineCues.size}")
                previousTranscriptUri
                    ?.takeIf { it != transcriptUri.toString() }
                    ?.let { SafStorage.deleteDocument(this@TranscriptionService, Uri.parse(it)) }

                TranscriptionRuntime.update(
                    TranscriptionState(
                        recordingId = request.id,
                        active = false,
                        progress = 100,
                        processedMs = total,
                        totalMs = total,
                        chunk = totalChunks,
                        totalChunks = totalChunks,
                        modelName = modelName,
                        stage = "Complete",
                        detail = if (unrecoveredNpuChunks > 0) "Transcript saved with $unrecoveredNpuChunks untranscribed segment(s)" else "Transcript saved",
                        elapsedMs = wallTimeMs,
                        startedAtMs = sessionStarted,
                        completedRecordingId = request.id
                    )
                )

                // Keep one foreground-service lifecycle across queued recordings.
                if (synchronized(pending) { pending.isEmpty() }) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, terminalNotification("${request.title} is ready", success = true)
                    )
                }
                val realtimeFactor = if (total > 0L) wallTimeMs.toDouble() / total.toDouble() else 0.0
                DiagnosticLogger.append(this@TranscriptionService, "SESSION complete recording=${request.id} audioDurationMs=$total wallTimeMs=$wallTimeMs decodeMs=$decodeElapsedMs modelLoadMs=$modelLoadElapsedMs inferenceMs=$inferenceTotalMs saveMs=$saveElapsedMs realtimeFactor=${"%.3f".format(java.util.Locale.US, realtimeFactor)} chunks=$totalChunks chars=${text.length} untranscribedNpuChunks=$unrecoveredNpuChunks")
            } catch (ce: CancellationException) {
                closeWarmSession()
                DiagnosticLogger.append(this@TranscriptionService, "SESSION cancelled recording=${request.id} message=${ce.message}")
                TranscriptionRuntime.update(
                    TranscriptionRuntime.state.value.copy(active = false, stage = "Cancelled", detail = "Transcription cancelled", error = null)
                )
                if (synchronized(pending) { pending.isEmpty() }) stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (t: Throwable) {
                closeWarmSession()
                DiagnosticLogger.appendError(this@TranscriptionService, "SESSION failed recording=${request.id}", t)
                // Write to a unique separate file; never replace an existing transcript or
                // attach an incomplete result as the recording's official transcription.
                val partialSaved = if (partialTranscript.isNotBlank()) runCatching {
                    val dir = WorkspaceStorage.transcriptsDirectory(this@TranscriptionService, request.treeUri)
                    val name = "${request.title}-partial-${System.currentTimeMillis()}.txt"
                    val content = "[INCOMPLETE TRANSCRIPT: transcription stopped before the recording ended]\n\n" +
                        partialTranscript.toString().trim()
                    val uri = SafStorage.writeTextToDirectory(this@TranscriptionService, dir, name, content)
                    DiagnosticLogger.append(this@TranscriptionService, "SESSION partial transcript saved chars=${partialTranscript.length}")
                    true
                }.onFailure {
                    DiagnosticLogger.appendError(this@TranscriptionService, "SESSION partial transcript save failed", it)
                }.getOrDefault(false) else false
                val failureDetail = (t.message ?: "Transcription failed") +
                    if (partialSaved) " • Partial transcript saved in Transcripts folder" else ""
                TranscriptionRuntime.update(
                    TranscriptionRuntime.state.value.copy(active = false, stage = "Failed", detail = failureDetail, error = failureDetail)
                )
                if (synchronized(pending) { pending.isEmpty() }) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID, terminalNotification("${request.title}: ${t.message ?: "Unknown error"}", success = false)
                    )
                }
            } finally {
                preparedSource?.close()
                job = null
                currentRecordingId = null
                val next = synchronized(pending) { if (pending.isEmpty()) null else pending.removeFirst() }
                if (next != null) {
                    startRequest(next)
                } else {
                    closeWarmSession()
                    releaseTranscriptionWakeLock()
                    stopSelf()
                }
            }
        }
    }

    private fun closeWarmSession() {
        val session = warmSession
        warmSession = null
        warmSessionKey = null
        if (activeSession === session) activeSession = null
        if (session != null) runCatching { session.close() }
    }

    private data class AudioStats(val peak: Float, val rms: Float, val nonZeroFraction: Double)

    private fun audioStats(samples: FloatArray): AudioStats {
        if (samples.isEmpty()) return AudioStats(0f, 0f, 0.0)
        var peak = 0f
        var sumSquares = 0.0
        var nonZero = 0L
        for (sample in samples) {
            val abs = kotlin.math.abs(sample)
            if (abs > peak) peak = abs
            sumSquares += sample.toDouble() * sample.toDouble()
            if (abs > 1e-5f) nonZero++
        }
        return AudioStats(
            peak = peak,
            rms = kotlin.math.sqrt(sumSquares / samples.size.toDouble()).toFloat(),
            nonZeroFraction = nonZero.toDouble() / samples.size.toDouble()
        )
    }

    private fun formatAudioLevel(value: Float): String =
        String.format(java.util.Locale.US, "%.6f", value)

    private fun formatPercent(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value * 100.0)

    private fun updateStage(
        recordingId: String,
        stage: String,
        detail: String,
        modelName: String,
        totalMs: Long,
        totalChunks: Int = 0,
        chunk: Int = 0
    ) {
        val current = TranscriptionRuntime.state.value
        TranscriptionRuntime.update(
            current.copy(
                recordingId = recordingId,
                active = true,
                modelName = modelName,
                totalMs = totalMs,
                totalChunks = totalChunks,
                chunk = chunk,
                stage = stage,
                detail = detail,
                elapsedMs = current.startedAtMs.takeIf { it > 0L }?.let { System.currentTimeMillis() - it } ?: current.elapsedMs,
                error = null
            )
        )
        DiagnosticLogger.append(this, "stage=$stage detail=$detail recording=$recordingId")
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            workingNotification(if (detail.isBlank()) stage else "$stage • $detail", progress = null)
        )
    }

    private fun workingNotification(text: String, progress: Int?, showStopAction: Boolean = true): Notification {
        val open = openAppPendingIntent()
        val cancel = PendingIntent.getService(
            this,
            11,
            Intent(this, TranscriptionService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_whisper)
            .setContentTitle("Whisper Recorder")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (showStopAction) builder.addAction(0, "Stop", cancel)
        // Only show a progress bar when we have a real, determinate percentage (multi-chunk audio).
        // A single whisper_full() call does not expose trustworthy percentage progress.
        if (progress != null) builder.setProgress(100, progress.coerceIn(0, 99), false)
        return builder.build()
    }

    private fun terminalNotification(text: String, success: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_whisper)
            .setContentTitle(if (success) "Transcription complete" else "Transcription failed")
            .setContentText(text)
            .setContentIntent(openAppPendingIntent())
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            // No setProgress call: completed/failed work must never look active.
            .build()

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        10,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            currentRecordingId?.let { putExtra(MainActivity.EXTRA_OPEN_RECORDING_ID, it) }
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onDestroy() {
        val inFlightJob = job
        activeSession?.cancel()
        inFlightJob?.cancel(CancellationException("Transcription service destroyed"))
        // isActive becomes false as soon as cancellation is requested, not when JNI has returned.
        // Only close synchronously when the job is genuinely complete; otherwise the coroutine's
        // normal cancellation/finally path owns native teardown after the current JNI call exits.
        if (inFlightJob == null || inFlightJob.isCompleted) closeWarmSession()
        synchronized(pending) { pending.clear() }
        scope.cancel()
        releaseTranscriptionWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
