package com.example.whisperrecorder.models

import android.content.Context
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import java.io.File

/** Shared resolution of the currently usable model path for UI and service launch code. */
object ModelSelectionResolver {
    fun activeModelPath(context: Context): String? {
        val engine = AppPrefs.getTranscriptionEngine(context)
        return if (engine == "parakeet_npu") {
            NpuPackageStore.TRANSCRIPTION_MODEL_KEY.takeIf { NpuPackageStore.archiveState(context) == NpuArchiveState.READY }
        } else if (engine == "omi_npu") {
            OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY.takeIf { OmiNpuPackageStore.archiveState(context) == NpuArchiveState.READY }
        } else if (engine in LocalAsrModelManager.engineIds) {
            LocalAsrModelManager.byEngine(engine)
                ?.takeIf { LocalAsrModelManager.isInstalled(context, it) }
                ?.let { LocalAsrModelManager.modelFile(context, it).absolutePath }
        } else {
            AppPrefs.getModelPath(context)?.takeIf { File(it).isFile }
        }
    }
}
