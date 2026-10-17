package com.example.whisperrecorder.transcribe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TranscriptionState(
    val recordingId: String? = null,
    val active: Boolean = false,
    val progress: Int = 0,
    val processedMs: Long = 0,
    val totalMs: Long = 0,
    val chunk: Int = 0,
    val totalChunks: Int = 0,
    val modelName: String = "",
    val stage: String = "",
    val detail: String = "",
    val elapsedMs: Long = 0,
    val startedAtMs: Long = 0,
    val error: String? = null,
    val completedRecordingId: String? = null
)

object TranscriptionRuntime {
    private val _state = MutableStateFlow(TranscriptionState())
    val state: StateFlow<TranscriptionState> = _state
    fun update(state: TranscriptionState) { _state.value = state }
}
