package com.example.whisperrecorder.speaker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.whisperrecorder.MainActivity
import com.example.whisperrecorder.R
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.RecordingStore
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Activity-independent speaker job: long lectures continue with screen locked. */
data class SpeakerDiarizationState(
    val recordingId: String? = null,
    val active: Boolean = false,
    val message: String? = null,
    val completedRecordingId: String? = null,
    val savedUri: String? = null,
    val diagnosticReport: String? = null
)

object SpeakerDiarizationRuntime {
    private val mutable = MutableStateFlow(SpeakerDiarizationState())
    val state: StateFlow<SpeakerDiarizationState> = mutable
    fun update(value: SpeakerDiarizationState) { mutable.value = value }
}

class SpeakerDiarizationService : Service() {
    companion object {
        const val ACTION_CREATE = "speaker.create"
        const val EXTRA_RECORDING_ID = "speaker.recording_id"
        const val EXTRA_EXPECTED = "speaker.expected_count"
        private const val CHANNEL_ID = "speaker_diarization"
        private const val NOTIFICATION_ID = 2005
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CREATE || job?.isActive == true) return START_NOT_STICKY
        val recordingId = intent.getStringExtra(EXTRA_RECORDING_ID)
            ?: return START_NOT_STICKY.also { stopSelf(startId) }
        val expected = intent.getIntExtra(EXTRA_EXPECTED, SpeakerClusteringPolicy.AUTO)
        val item = RecordingStore.find(this, recordingId)
            ?: return START_NOT_STICKY.also { stopSelf(startId) }
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Speaker identification", NotificationManager.IMPORTANCE_LOW))
        SpeakerDiarizationRuntime.update(SpeakerDiarizationState(recordingId = recordingId,
            active = true, message = "Preparing speaker identification…"))
        // Promptly enter foreground before the lengthy model loading / inference begins.
        startForeground(NOTIFICATION_ID, notification("Preparing speaker identification…"))
        val lock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:speaker")
        lock.setReferenceCounted(false)
        lock.acquire(2 * 60 * 60 * 1000L) // Safety timeout; released in finally.
        wakeLock = lock
        job = scope.launch {
            var stage = "Starting"
            try {
                val result = OfflineSpeakerService.generate(this@SpeakerDiarizationService, item,
                    AppPrefs.speakerTimestamps(this@SpeakerDiarizationService), expected) { message ->
                    stage = message
                    SpeakerDiarizationRuntime.update(SpeakerDiarizationState(recordingId = recordingId,
                        active = true, message = message))
                    notifications.notify(NOTIFICATION_ID, notification(message))
                }
                SpeakerDiagnostics.clear(this@SpeakerDiarizationService, recordingId)
                SpeakerDiarizationRuntime.update(SpeakerDiarizationState(recordingId = recordingId,
                    active = false, message = "Saved separate speaker transcript. Original unchanged.",
                    savedUri = result.toString(), completedRecordingId = recordingId))
                notifications.notify(NOTIFICATION_ID, notification("Speaker transcript ready"))
            } catch (t: Throwable) {
                val report = SpeakerDiagnostics.capture(this@SpeakerDiarizationService,
                    recordingId, t, stage)
                val summary = SpeakerFailureText.summary(t)
                DiagnosticLogger.append(this@SpeakerDiarizationService,
                    "speaker FAILED type=${t.javaClass.simpleName} stage=$stage")
                SpeakerDiarizationRuntime.update(SpeakerDiarizationState(recordingId = recordingId,
                    active = false, message = summary, diagnosticReport = report))
                notifications.notify(NOTIFICATION_ID, notification("Speaker transcript failed; view diagnostics in app"))
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                wakeLock = null
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(message: String): android.app.Notification {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_RECORDING_ID, SpeakerDiarizationRuntime.state.value.recordingId)
        }
        val pending = android.app.PendingIntent.getActivity(this, 2005, open,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_whisper)
            .setContentTitle("WhisperRecorder speakers")
            .setContentText(message)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(SpeakerDiarizationRuntime.state.value.active)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }
}
