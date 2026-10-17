package com.example.whisperrecorder.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ModelDownloadState(
    val active: Boolean = false,
    val modelId: String? = null,
    val modelName: String = "",
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: String = "",
    val completedPath: String? = null,
    val error: String? = null
)

object ModelDownloadRuntime {
    private val _state = MutableStateFlow(ModelDownloadState())
    val state: StateFlow<ModelDownloadState> = _state

    fun starting(modelId: String, modelName: String) {
        _state.value = ModelDownloadState(
            active = true,
            modelId = modelId,
            modelName = modelName,
            status = "Starting download…"
        )
    }

    fun progress(modelId: String, modelName: String, percent: Int, done: Long, total: Long, status: String = "Downloading $modelName…") {
        _state.value = ModelDownloadState(
            active = true,
            modelId = modelId,
            modelName = modelName,
            progress = percent.coerceIn(0, 100),
            bytesDownloaded = done.coerceAtLeast(0L),
            totalBytes = total.coerceAtLeast(0L),
            status = status
        )
    }

    fun verifying(modelId: String, modelName: String, done: Long, total: Long) {
        _state.value = ModelDownloadState(
            active = true,
            modelId = modelId,
            modelName = modelName,
            progress = 100,
            bytesDownloaded = done,
            totalBytes = total,
            status = "Verifying model…"
        )
    }

    fun archiving(modelId: String, modelName: String) {
        val current = _state.value
        _state.value = current.copy(
            active = true,
            modelId = modelId,
            modelName = modelName,
            progress = 100,
            status = "Saving model to Whisperapp/Models…",
            error = null
        )
    }

    fun completed(modelId: String, modelName: String, path: String) {
        _state.value = ModelDownloadState(
            active = false,
            modelId = modelId,
            modelName = modelName,
            progress = 100,
            status = "Downloaded $modelName",
            completedPath = path
        )
    }

    fun cancelled(modelId: String, modelName: String) {
        _state.value = ModelDownloadState(
            active = false,
            modelId = modelId,
            modelName = modelName,
            status = "Download paused. You can resume it later."
        )
    }

    fun failed(modelId: String, modelName: String, message: String) {
        _state.value = ModelDownloadState(
            active = false,
            modelId = modelId,
            modelName = modelName,
            status = "Download failed",
            error = message
        )
    }

}
