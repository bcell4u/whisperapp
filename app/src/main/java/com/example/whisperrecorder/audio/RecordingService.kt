package com.example.whisperrecorder.audio

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.whisperrecorder.MainActivity
import com.example.whisperrecorder.R
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.RecordingItem
import com.example.whisperrecorder.data.RecordingStore
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object RecordingRuntime {
    data class State(
        val active: Boolean = false,
        val paused: Boolean = false,
        val elapsedMs: Long = 0,
        val lastSavedUri: String? = null,
        val lastSavedRecordingId: String? = null,
        val lastSavedTitle: String? = null,
        val error: String? = null,
        val micWarning: Boolean = false
    )
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    private val _liveLevel = MutableStateFlow(0f)
    val liveLevel: StateFlow<Float> = _liveLevel
    @Volatile var visualizerVisible: Boolean = false
        private set
    @Synchronized fun update(value: State) { _state.value = value }
    @Synchronized fun mutate(transform: (State) -> State) { _state.value = transform(_state.value) }
    @Synchronized fun consumeLastSaved() {
        _state.value = _state.value.copy(lastSavedUri = null, lastSavedRecordingId = null, lastSavedTitle = null)
    }
    fun updateLiveLevel(level: Float) {
        _liveLevel.value = level.coerceIn(0f, 1f)
    }
    @Synchronized fun setVisualizerVisible(visible: Boolean) {
        visualizerVisible = visible
        if (!visible) _liveLevel.value = 0f
    }
}

class RecordingService : Service() {
    companion object {
        const val ACTION_START = "record.start"
        const val ACTION_PAUSE = "record.pause"
        const val ACTION_RESUME = "record.resume"
        const val ACTION_STOP = "record.stop"
        const val EXTRA_TREE = "tree"
        const val EXTRA_FORMAT = "format"
        private const val CHANNEL = "recording"
        private const val NOTIFICATION_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaRecorder: MediaRecorder? = null
    private var wavRecorder: WavPcmRecorder? = null
    private var tempFile: File? = null
    private var treeUri: Uri? = null
    private var format = "opus"
    private var startedAt = 0L
    private var pauseStartedAt = 0L
    private var totalPausedMs = 0L
    private var paused = false
    private var silentSince = 0L
    @Volatile private var latestAmplitude = 0
    private var processingConfig = MicrophoneProcessing.presetConfig(MicrophoneProcessing.PRESET_DEFAULT)

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun recorderActive(): Boolean = mediaRecorder != null || wavRecorder != null

    private fun formatInfo(value: String): Triple<String, String, String> = when (value) {
        "opus" -> Triple("ogg", "audio/ogg", "OGG / Opus")
        "wav" -> Triple("wav", "audio/wav", "WAV / PCM")
        else -> Triple("m4a", "audio/mp4", "M4A / AAC")
    }

    private fun startRecording(intent: Intent) {
        if (recorderActive()) return
        val tree = intent.getStringExtra(EXTRA_TREE) ?: run {
            RecordingRuntime.update(RecordingRuntime.State(error = "Choose a working directory first")); stopSelf(); return
        }
        treeUri = Uri.parse(tree)
        val requestedFormat = intent.getStringExtra(EXTRA_FORMAT)
        format = if (requestedFormat in setOf("m4a", "opus", "wav")) requireNotNull(requestedFormat) else "opus"
        val (ext, _, _) = formatInfo(format)
        tempFile = File(cacheDir, "active_${System.currentTimeMillis()}.$ext")
        processingConfig = MicrophoneProcessing.resolve(this)

        try {
            if (format == "wav") {
                WavPcmRecorder(requireNotNull(tempFile), processingConfig).also { recorder ->
                    recorder.start()
                    wavRecorder = recorder
                    DiagnosticLogger.append(
                        this,
                        "recording start format=wav preset=${processingConfig.preset} requestedSource=${processingConfig.source} actualSource=${recorder.actualSource} ${recorder.effectSummary}"
                    )
                }
            } else {
                val requestedSource = MicrophoneProcessing.sourceValue(processingConfig.source)
                var actualSource = processingConfig.source
                val mr = runCatching { createCompressedRecorder(requestedSource) }.getOrElse { firstError ->
                    if (requestedSource == MediaRecorder.AudioSource.MIC) throw firstError
                    DiagnosticLogger.append(
                        this,
                        "recording source fallback requested=${processingConfig.source} reason=${firstError.message ?: firstError.javaClass.simpleName}"
                    )
                    tempFile?.delete()
                    actualSource = MicrophoneProcessing.SOURCE_MIC
                    createCompressedRecorder(MediaRecorder.AudioSource.MIC)
                }
                mediaRecorder = mr
                DiagnosticLogger.append(
                    this,
                    "recording start format=$format preset=${processingConfig.preset} requestedSource=${processingConfig.source} actualSource=$actualSource NS=${processingConfig.noiseSuppressor} AEC=${processingConfig.aec} effects=system-managed-for-compressed"
                )
            }

            startedAt = SystemClock.elapsedRealtime()
            totalPausedMs = 0L
            paused = false
            silentSince = 0L
            RecordingRuntime.update(RecordingRuntime.State(active = true))
            startForeground(NOTIFICATION_ID, notification("Recording", false, false))
            // Status/notification work remains at 1 Hz. A separate lightweight peak sampler is
            // enabled only while the Record screen is visible, so background recording keeps the
            // same low-overhead behavior as before.
            scope.launch {
                while (isActive && recorderActive()) {
                    val now = SystemClock.elapsedRealtime()
                    val activePause = if (paused) now - pauseStartedAt else 0L
                    val elapsed = (now - startedAt - totalPausedMs - activePause).coerceAtLeast(0)
                    var warning = RecordingRuntime.state.value.micWarning
                    if (!paused) {
                        val amp = if (RecordingRuntime.visualizerVisible) latestAmplitude else readAmplitude()
                        if (amp <= 1) {
                            if (silentSince == 0L) silentSince = now
                            if (now - silentSince >= 15_000L) warning = true
                        } else {
                            silentSince = 0L
                            warning = false
                        }
                    } else {
                        silentSince = 0L
                    }
                    val wavFailure = wavRecorder?.error
                    if (wavFailure != null) {
                        RecordingRuntime.update(RecordingRuntime.State(error = "Recording failed: ${wavFailure.message ?: "PCM capture error"}"))
                        tempFile?.delete()
                        cleanupRecorder()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                    RecordingRuntime.mutate { it.copy(active = true, paused = paused, elapsedMs = elapsed, micWarning = warning) }
                    updateNotification()
                    delay(1000)
                }
            }
            scope.launch {
                while (isActive && recorderActive()) {
                    if (!RecordingRuntime.visualizerVisible || paused) {
                        if (RecordingRuntime.liveLevel.value != 0f) {
                            RecordingRuntime.updateLiveLevel(0f)
                        }
                        delay(if (paused) 200 else 500)
                        continue
                    }
                    val amp = readAmplitude()
                    latestAmplitude = amp
                    val normalized = (amp.toFloat() / 32767f).coerceIn(0f, 1f)
                    RecordingRuntime.updateLiveLevel(normalized)
                    delay(80)
                }
            }
        } catch (t: Throwable) {
            cleanupRecorder()
            tempFile?.delete()
            RecordingRuntime.update(RecordingRuntime.State(error = t.message ?: "Unable to start recording"))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }


    private fun readAmplitude(): Int = when {
        wavRecorder != null -> wavRecorder?.maxAmplitude ?: 0
        else -> runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
    }

    private fun createCompressedRecorder(audioSource: Int): MediaRecorder {
        val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            mr.setAudioSource(audioSource)
            if (format == "opus") {
                mr.setOutputFormat(MediaRecorder.OutputFormat.OGG)
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                mr.setAudioEncodingBitRate(64_000)
                mr.setAudioSamplingRate(48_000)
            } else {
                mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mr.setAudioEncodingBitRate(96_000)
                mr.setAudioSamplingRate(44_100)
            }
            mr.setAudioChannels(1)
            mr.setOutputFile(requireNotNull(tempFile).absolutePath)
            mr.prepare()
            mr.start()
            return mr
        } catch (t: Throwable) {
            runCatching { mr.release() }
            throw t
        }
    }

    private fun pauseRecording() {
        if (!recorderActive() || paused) return
        val result = runCatching {
            if (wavRecorder != null) wavRecorder?.pause() else mediaRecorder?.pause()
        }
        result.onSuccess {
            paused = true
            pauseStartedAt = SystemClock.elapsedRealtime()
            silentSince = 0L
            latestAmplitude = 0
            RecordingRuntime.mutate { it.copy(paused = true) }
            RecordingRuntime.updateLiveLevel(0f)
            updateNotification()
        }.onFailure { RecordingRuntime.update(RecordingRuntime.state.value.copy(error = it.message)) }
    }

    private fun resumeRecording() {
        if (!recorderActive() || !paused) return
        val result = runCatching {
            if (wavRecorder != null) wavRecorder?.resume() else mediaRecorder?.resume()
        }
        result.onSuccess {
            totalPausedMs += SystemClock.elapsedRealtime() - pauseStartedAt
            paused = false
            silentSince = 0L
            RecordingRuntime.mutate { it.copy(paused = false) }
            updateNotification()
        }.onFailure { RecordingRuntime.update(RecordingRuntime.state.value.copy(error = it.message)) }
    }

    private fun stopRecording() {
        if (!recorderActive()) return
        val now = SystemClock.elapsedRealtime()
        if (paused) totalPausedMs += now - pauseStartedAt
        val duration = (now - startedAt - totalPausedMs).coerceAtLeast(0)

        val stopResult = runCatching {
            if (wavRecorder != null) wavRecorder?.stop() else mediaRecorder?.stop()
        }
        if (stopResult.isFailure) {
            tempFile?.delete()
            RecordingRuntime.update(RecordingRuntime.State(error = "Recording could not be finalized: ${stopResult.exceptionOrNull()?.message ?: "unknown error"}"))
            cleanupRecorder()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        cleanupRecorder()

        scope.launch(Dispatchers.IO) {
            try {
                val source = requireNotNull(tempFile)
                val tree = requireNotNull(treeUri)
                require(source.isFile && source.length() > 44L) { "Recording was too short to save" }
                val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
                val (ext, mime, _) = formatInfo(format)
                val title = "Recording_$stamp"
                val recordingsDir = WorkspaceStorage.recordingsDirectory(this@RecordingService, tree)
                val dest = SafStorage.copyFileToDirectory(
                    this@RecordingService,
                    source,
                    tree,
                    recordingsDir,
                    mime,
                    "$title.$ext"
                )
                val recordingId = UUID.randomUUID().toString()
                RecordingStore.add(this@RecordingService, RecordingItem(
                    id = recordingId,
                    title = title,
                    audioUri = dest.toString(),
                    treeUri = tree.toString(),
                    mime = mime,
                    durationMs = duration,
                    createdAt = System.currentTimeMillis(),
                    audioOwnedByApp = true
                ))
                AppPrefs.setPendingRenameRecordingId(this@RecordingService, recordingId)
                source.delete()
                RecordingRuntime.update(RecordingRuntime.State(
                    lastSavedUri = dest.toString(),
                    lastSavedRecordingId = recordingId,
                    lastSavedTitle = title
                ))
            } catch (t: Throwable) {
                RecordingRuntime.update(RecordingRuntime.State(error = "Save failed: ${t.message}"))
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun cleanupRecorder() {
        runCatching { mediaRecorder?.release() }
        runCatching { wavRecorder?.release() }
        mediaRecorder = null
        wavRecorder = null
        latestAmplitude = 0
        RecordingRuntime.setVisualizerVisible(false)
        RecordingRuntime.updateLiveLevel(0f)
    }

    private fun notification(label: String, isPaused: Boolean, warning: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pauseAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val pauseLabel = if (isPaused) "Resume" else "Pause"
        val pausePi = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(pauseAction), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopPi = PendingIntent.getService(this, 2, Intent(this, RecordingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(if (isPaused) R.drawable.ic_notification_pause else R.drawable.ic_notification_mic)
            .setContentTitle(if (warning) "Microphone input may be unavailable" else "Whisper Recorder")
            .setContentText(if (warning) "Input has been silent for 15 seconds. Another app may be using the microphone." else label)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(!warning)
            .addAction(0, pauseLabel, pausePi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    private fun updateNotification() {
        val s = RecordingRuntime.state.value
        val mm = s.elapsedMs / 60_000
        val ss = (s.elapsedMs / 1000) % 60
        val label = if (paused) "Paused • %02d:%02d".format(mm, ss) else "Recording • %02d:%02d".format(mm, ss)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(label, paused, s.micWarning))
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Recording", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onDestroy() {
        cleanupRecorder()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
