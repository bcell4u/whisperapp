package com.example.whisperrecorder.models

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.whisperrecorder.MainActivity
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.R
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private typealias DownloadProgress = (percent: Int, downloaded: Long, total: Long, status: String) -> Unit
private typealias DownloadVerify = (downloaded: Long, total: Long) -> Unit
private typealias DownloadArchive = () -> Unit
private typealias DownloadCancelled = () -> Boolean
private typealias DownloadRunner = suspend (DownloadProgress, DownloadVerify, DownloadArchive, DownloadCancelled) -> String

class ModelDownloadService : Service() {
    companion object {
        const val ACTION_START = "com.example.whisperrecorder.model.DOWNLOAD"
        const val ACTION_CANCEL = "com.example.whisperrecorder.model.CANCEL"
        const val EXTRA_MODEL_ID = "model_id"
        private const val CHANNEL_ID = "model_downloads"
        private const val NOTIFICATION_ID = 4402
    }

    private data class DownloadTarget(
        val id: String,
        val name: String,
        val expectedBytes: Long = 0L,
        val run: DownloadRunner
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    @Volatile private var cancelRequested = false

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background transcription model downloads"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelRequested = true
                downloadJob?.cancel()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (downloadJob?.isActive == true) return START_NOT_STICKY
                val target = intent.getStringExtra(EXTRA_MODEL_ID)?.let(::resolveTarget) ?: return START_NOT_STICKY
                startDownload(target)
                // Existing model downloads keep their current behavior. The large NPU package opts
                // into redelivery so Android can restart the same manifest-driven repair/resume
                // operation after an unexpected service-process kill.
                return if (target.id == NpuPackageStore.DOWNLOAD_ID || target.id == OmiNpuPackageStore.DOWNLOAD_ID) START_REDELIVER_INTENT else START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun resolveTarget(modelId: String): DownloadTarget? {
        LocalAsrModelManager.byId(modelId)?.let { model ->
            return DownloadTarget(model.id, model.name, model.expectedBytes ?: 0L) { progress, verify, archive, cancelled ->
                LocalAsrModelManager.download(this, model, progress, verify, archive, cancelled).absolutePath
            }
        }

        ModelManager.builtIns.firstOrNull { it.id == modelId }?.let { model ->
            return DownloadTarget(model.id, model.name) { progress, verify, archive, cancelled ->
                ModelManager.download(
                    context = this,
                    model = model,
                    onProgress = { percent, done, total -> progress(percent, done, total, "Downloading ${model.name}…") },
                    onVerifying = verify,
                    onArchiving = archive,
                    isCancelled = cancelled
                ).absolutePath
            }
        }

        if (modelId == OmiNpuPackageStore.DOWNLOAD_ID) {
            return DownloadTarget(OmiNpuPackageStore.DOWNLOAD_ID, OmiNpuPackageStore.DISPLAY_NAME,
                OmiNpuPackageStore.totalBytes) { progress, _, _, cancelled ->
                OmiNpuPackageStore.installOrRepair(this, cancelled) { update ->
                    val percent = if (update.bytesTotal > 0L)
                        ((update.bytesDone * 100L) / update.bytesTotal).toInt().coerceIn(0, 100) else 0
                    progress(percent, update.bytesDone, update.bytesTotal, update.message)
                }
                DiagnosticLogger.append(this, "omi-npu-download completed repo=${OmiNpuPackageStore.REPO_ID} archive=${OmiNpuPackageStore.ARCHIVE_PATH}")
                OmiNpuPackageStore.ARCHIVE_PATH
            }
        }

        if (modelId == NpuPackageStore.DOWNLOAD_ID) {
            return DownloadTarget(NpuPackageStore.DOWNLOAD_ID, NpuPackageStore.DISPLAY_NAME) { progress, _, _, cancelled ->
                NpuPackageStore.installOrRepair(
                    context = this,
                    isCancelled = cancelled,
                    onProgress = { update ->
                        val percent = when {
                            update.bytesTotal > 0L -> ((update.bytesDone * 100L) / update.bytesTotal).toInt().coerceIn(0, 100)
                            update.fileCount > 0 -> ((update.fileIndex * 100L) / update.fileCount).toInt().coerceIn(0, 100)
                            else -> 0
                        }
                        progress(percent, update.bytesDone, update.bytesTotal, update.message.ifBlank { update.phase })
                    }
                )
                DiagnosticLogger.append(
                    this,
                    "npu package install/repair completed revision=${NpuPackageStore.REVISION}"
                )
                NpuPackageStore.ARCHIVE_DISPLAY_PATH
            }
        }
        return null
    }

    private fun startDownload(target: DownloadTarget) {
        cancelRequested = false
        ModelDownloadRuntime.starting(target.id, target.name)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(target.name, 0, 0L, target.expectedBytes, "Starting download…")
        )
        downloadJob = scope.launch {
            try {
                var lastPublishedPercent = -1
                var lastPublishedStatus = ""
                var lastPublishedAtMs = 0L
                val completedPath = target.run(
                    { percent, done, total, status ->
                        val now = SystemClock.elapsedRealtime()
                        if (percent != lastPublishedPercent || status != lastPublishedStatus || now - lastPublishedAtMs >= 750L) {
                            lastPublishedPercent = percent
                            lastPublishedStatus = status
                            lastPublishedAtMs = now
                            ModelDownloadRuntime.progress(target.id, target.name, percent, done, total, status)
                            notifyProgress(target.name, percent, done, total, status)
                        }
                    },
                    { done, total ->
                        ModelDownloadRuntime.verifying(target.id, target.name, done, total)
                        notifyProgress(target.name, 100, done, total, "Verifying model…")
                    },
                    {
                        ModelDownloadRuntime.archiving(target.id, target.name)
                        notifyProgress(target.name, 100, 0L, 0L, "Saving model to Whisperapp/Models…")
                    },
                    { cancelRequested }
                )
                if (cancelRequested) throw CancellationException("Model download cancelled")
                ModelDownloadRuntime.completed(target.id, target.name, completedPath)
            } catch (_: CancellationException) {
                ModelDownloadRuntime.cancelled(target.id, target.name)
                if (target.id == NpuPackageStore.DOWNLOAD_ID || target.id == OmiNpuPackageStore.DOWNLOAD_ID) {
                    DiagnosticLogger.append(this@ModelDownloadService, "model=${target.id} install/repair cancelled; verified files retained")
                }
            } catch (t: Throwable) {
                ModelDownloadRuntime.failed(target.id, target.name, t.message ?: t.javaClass.simpleName)
                if (target.id == NpuPackageStore.DOWNLOAD_ID || target.id == OmiNpuPackageStore.DOWNLOAD_ID) {
                    DiagnosticLogger.appendError(this@ModelDownloadService, "npu-package-download model=${target.id}", t)
                }
            } finally {
                finishDownloadService()
            }
        }
    }

    private fun notifyProgress(modelName: String, percent: Int, done: Long, total: Long, status: String) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildNotification(modelName, percent, done, total, status)
        )
    }

    private fun finishDownloadService() {
        downloadJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        modelName: String,
        percent: Int,
        done: Long,
        total: Long,
        status: String
    ): android.app.Notification {
        val openPending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelPending = PendingIntent.getService(
            this,
            1,
            Intent(this, ModelDownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val sizeText = when {
            total > 0L -> "${formatBytes(done)} / ${formatBytes(total)} · ${percent.coerceIn(0, 100)}%"
            done > 0L -> formatBytes(done)
            else -> status
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_whisper)
            .setContentTitle("Downloading $modelName")
            .setContentText(sizeText)
            .setSubText(status)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Cancel", cancelPending)
            .setProgress(if (total > 0L) 100 else 0, if (total > 0L) percent.coerceIn(0, 100) else 0, total <= 0L)
            .build()
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / 1_000_000.0
        return if (mb >= 100) "%.0f MB".format(mb) else "%.1f MB".format(mb)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
