package com.example.whisperrecorder.models

import android.content.Context
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import com.example.whisperrecorder.transcribe.AsrEngine
import com.example.whisperrecorder.transcribe.AccelerationPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One-pass model state snapshot for setup/settings UI refreshes. */
data class ModelInventory(
    val whisperInstalled: List<File>,
    val whisperPersistentIds: Set<String>,
    val localInstalledIds: Set<String>,
    val localPersistentIds: Set<String>,
    val selectedEngine: String,
    val selectedPath: String?
)

object ModelInventoryLoader {
    suspend fun load(context: Context, restoreWhisper: Boolean): ModelInventory = withContext(Dispatchers.IO) {
        val archive = ModelFileOps.snapshot(context)
        val whisperInstalled = if (restoreWhisper) {
            ModelManager.restorePersistentModels(context, archive)
        } else {
            ModelManager.installed(context)
        }
        val documents = archive?.documents.orEmpty()
        val localInstalledIds = LocalAsrModelManager.installedIds(context)
        // Migrate a removed Qwen 1.7B preference before checking whether a model
        // was explicitly selected; otherwise the first refresh can remain empty.
        AppPrefs.getTranscriptionEngine(context)
        // Only choose an acceleration default if the user has never selected a model.
        // Resolve availability on Dispatchers.IO; NPU archive checks may access SAF storage.
        if (!AppPrefs.hasExplicitTranscriptionEngine(context)) {
            val available = buildSet {
                if (runCatching { NpuPackageStore.archiveState(context) }
                    .onFailure { error -> runCatching { DiagnosticLogger.appendError(context, "acceleration NPU availability", error) } }
                    .getOrNull() == NpuArchiveState.READY) add(AsrEngine.PARAKEET_NPU)
                LocalAsrModelManager.catalog.filter { it.id in localInstalledIds }.forEach { add(it.engine) }
                if (whisperInstalled.isNotEmpty()) add(AsrEngine.WHISPER)
            }
            AccelerationPolicy.preferredAvailable(available)?.let { chosen ->
                val path = when (chosen) {
                    AsrEngine.PARAKEET_NPU, AsrEngine.OMI_NPU -> null
                    AsrEngine.WHISPER -> whisperInstalled.first().absolutePath
                    else -> LocalAsrModelManager.byEngine(chosen)?.let {
                        LocalAsrModelManager.modelFile(context, it).absolutePath
                    }
                }
                if (AppPrefs.getTranscriptionEngine(context) != chosen.id || AppPrefs.getModelPath(context) != path) {
                    AppPrefs.setAutomaticTranscriptionSelection(context, chosen.id, path)
                    runCatching { DiagnosticLogger.append(context, "ACCELERATION auto-select engine=${chosen.id} tier=${AccelerationPolicy.tier(chosen)}") }
                }
            }
        }
        val selectedEngine = AppPrefs.getTranscriptionEngine(context)
        val storedPath = AppPrefs.getModelPath(context)
        val selectedPath = when {
            selectedEngine == "parakeet_npu" && NpuPackageStore.archiveState(context) == NpuArchiveState.READY -> NpuPackageStore.TRANSCRIPTION_MODEL_KEY
            selectedEngine == "omi_npu" && OmiNpuPackageStore.archiveState(context) == NpuArchiveState.READY -> OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY
            selectedEngine in LocalAsrModelManager.engineIds -> LocalAsrModelManager.byEngine(selectedEngine)
                ?.takeIf { it.id in localInstalledIds }
                ?.let { LocalAsrModelManager.modelFile(context, it).absolutePath }
            selectedEngine == "whisper" -> storedPath?.takeIf { path -> whisperInstalled.any { it.absolutePath == path } }
            else -> null
        }
        ModelInventory(
            whisperInstalled = whisperInstalled,
            whisperPersistentIds = ModelManager.persistentBuiltInIds(documents),
            localInstalledIds = localInstalledIds,
            localPersistentIds = LocalAsrModelManager.persistentDetectedIds(documents),
            selectedEngine = selectedEngine,
            selectedPath = selectedPath
        )
    }
}
