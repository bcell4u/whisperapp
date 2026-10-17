package com.example.whisperrecorder.advanced

import android.content.Context
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.models.ModelFileOps
import com.example.whisperrecorder.transcribe.AsrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LocalAsrModel(
    val id: String,
    val engine: AsrEngine,
    val name: String,
    val fileName: String,
    val url: String,
    val expectedBytes: Long?,
    val sha256: String,
    val sizeLabel: String,
    val languageCount: Int,
    val modelInfoUrl: String,
    val licenseName: String,
    /** Optional compatibility path for models installed by older releases. */
    val privateDirectory: String? = null,
    /** Optional compatibility archive name for models saved by older releases. */
    val persistentFileName: String? = null,
    /** Older app-private directories checked once so an existing verified download can be reused. */
    val legacyPrivateDirectories: List<String> = emptyList()
)

/**
 * Single download/install lifecycle for every supported single-file GGUF ASR engine.
 *
 * WhisperRecorder exposes one production Parakeet choice: NVIDIA Parakeet TDT 0.6B v3 Q8_0.
 * The runtime-compatible GGUF is generated from NVIDIA's checkpoint for parakeet.cpp; the
 * official NVIDIA model page remains the authoritative source link shown in the app.
 */
object LocalAsrModelManager {
    val catalog: List<LocalAsrModel> = listOf(
        LocalAsrModel(
            id = AsrEngine.PARAKEET.id,
            engine = AsrEngine.PARAKEET,
            name = "NVIDIA Parakeet TDT 0.6B v3 Q8_0 — Recommended",
            fileName = "tdt-0.6b-v3-q8_0.gguf",
            // parakeet.cpp-compatible Q8_0 converted from the official NVIDIA checkpoint.
            // NVIDIA's own 714 MB GGUF targets NeMo-Speech.cpp and is not assumed schema-compatible.
            url = "https://huggingface.co/mudler/parakeet-cpp-gguf/resolve/main/tdt-0.6b-v3-q8_0.gguf?download=true",
            expectedBytes = 940_663_680L,
            sha256 = "4d69a4a6683f4f2d952bad794c1357ca6eb628027695b4699c5a9ad4cd07d757",
            sizeLabel = "~941 MB",
            languageCount = 25,
            modelInfoUrl = "https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3",
            licenseName = "CC BY 4.0 • OpenCL GPU automatic",
            privateDirectory = "models",
            persistentFileName = "tdt-0.6b-v3-q8_0.gguf",
            legacyPrivateDirectories = listOf("models/benchmark/parakeet-cpp-v3-q8")
        ),
        LocalAsrModel(
            id = "omi-med-stt-v1-q8",
            engine = AsrEngine.OMI,
            name = "Omi Med STT v1 Q8_0 (0.6B) — Medical",
            fileName = "omi-med-stt-v1-q8_0.gguf",
            url = "https://huggingface.co/omi-health/omi-med-stt-v1-gguf/resolve/c78333664b14e8dd8e47ed063be9692ed9054390/omi-med-stt-v1-q8_0.gguf?download=true",
            expectedBytes = 929_205_888L,
            sha256 = "c4f364a730df7aa9bb0714cda1b1ad5e3104331db9919bb0e2a379d0fb64dbab",
            sizeLabel = "~929 MB",
            languageCount = 1,
            modelInfoUrl = "https://huggingface.co/omi-health/omi-med-stt-v1-gguf",
            licenseName = "CC BY 4.0 • OpenCL GPU automatic"
        ),
        LocalAsrModel(
            id = "qwen3-asr-1.7b-q4km",
            engine = AsrEngine.QWEN3,
            name = "Qwen3-ASR 1.7B",
            fileName = "Qwen3-ASR-1.7B-Q4_K_M.gguf",
            url = "https://huggingface.co/handy-computer/Qwen3-ASR-1.7B-gguf/resolve/main/Qwen3-ASR-1.7B-Q4_K_M.gguf?download=true",
            expectedBytes = 1_319_830_496L,
            sha256 = "b7afe3674f653fa84f712ed2440353c6e7cf7f93697fef76b05a26538b24844e",
            sizeLabel = "~1.32 GB",
            languageCount = 30,
            modelInfoUrl = "https://huggingface.co/handy-computer/Qwen3-ASR-1.7B-gguf",
            licenseName = "Apache 2.0"
        )
    )

    val nvidiaParakeetModels: List<LocalAsrModel> = catalog.filter { it.engine == AsrEngine.PARAKEET }
    val advancedModels: List<LocalAsrModel> = catalog.filter { it.engine != AsrEngine.PARAKEET }
    private val idIndex: Map<String, LocalAsrModel> = catalog.associateBy { it.id }
    private val engineIndex: Map<AsrEngine, LocalAsrModel> = catalog.associateBy { it.engine }
    val engineIds: Set<String> = engineIndex.keys.mapTo(linkedSetOf()) { it.id }

    fun byId(id: String): LocalAsrModel? = idIndex[id]
    fun byEngine(engine: AsrEngine): LocalAsrModel? = engineIndex[engine]
    fun byEngine(engine: String): LocalAsrModel? = byEngine(AsrEngine.fromId(engine))

    private fun modelDir(context: Context, model: LocalAsrModel): File =
        File(context.filesDir, model.privateDirectory ?: "models/local-asr/${model.id}")

    private fun ensureModelDir(context: Context, model: LocalAsrModel): File =
        modelDir(context, model).apply { mkdirs() }

    private fun rawModelFile(context: Context, model: LocalAsrModel): File = File(modelDir(context, model), model.fileName)

    /** Reuse the legacy Q8_0 install location instead of forcing another ~941 MB download. */
    private fun migrateLegacyLocalModelIfPresent(context: Context, model: LocalAsrModel) {
        val target = rawModelFile(context, model)
        if (target.isFile && (model.expectedBytes == null || target.length() == model.expectedBytes)) return
        model.legacyPrivateDirectories.forEach { legacyDir ->
            val legacy = File(File(context.filesDir, legacyDir), model.fileName)
            val valid = legacy.isFile && (model.expectedBytes == null || legacy.length() == model.expectedBytes)
            if (valid) {
                ensureModelDir(context, model)
                if (target.exists()) target.delete()
                if (!legacy.renameTo(target)) legacy.copyTo(target, overwrite = true)
                if (target.isFile && target.length() == legacy.length()) legacy.delete()
                return
            }
        }
    }

    fun modelFile(context: Context, model: LocalAsrModel): File {
        migrateLegacyLocalModelIfPresent(context, model)
        return rawModelFile(context, model)
    }

    private fun verificationMarker(file: File): File = File(file.parentFile, file.name + ".sha256-ok")

    private fun markVerified(file: File, model: LocalAsrModel) {
        verificationMarker(file).writeText(model.sha256, Charsets.UTF_8)
    }

    /**
     * v5.49 wrote the marker for size-unknown restored models beside the temporary
     * <model>.restoring file before the atomic rename. Recover that already-verified
     * state without forcing another multi-hundred-MB SAF restore.
     */
    private fun repairLegacyRestoreMarker(file: File, model: LocalAsrModel) {
        if (model.expectedBytes != null || !file.isFile || verificationMarker(file).isFile) return
        val legacyMarker = verificationMarker(File(file.parentFile, file.name + ".restoring"))
        val verified = legacyMarker.takeIf { it.isFile }
            ?.readText(Charsets.UTF_8)
            ?.trim()
            ?.equals(model.sha256, ignoreCase = true) == true
        if (verified) {
            markVerified(file, model)
            legacyMarker.delete()
        }
    }

    fun isInstalled(context: Context, model: LocalAsrModel): Boolean {
        val file = modelFile(context, model)
        if (!file.isFile) return false
        model.expectedBytes?.let { return file.length() == it }
        repairLegacyRestoreMarker(file, model)
        return verificationMarker(file).takeIf { it.isFile }?.readText(Charsets.UTF_8)?.trim() == model.sha256
    }

    fun installedIds(context: Context): Set<String> = catalog.asSequence()
        .filter { isInstalled(context, it) }
        .mapTo(linkedSetOf()) { it.id }

    fun hasPartialDownload(context: Context, model: LocalAsrModel): Boolean =
        File(modelDir(context, model), model.fileName + ".part").let { it.isFile && it.length() > 0L }

    private fun persistentName(model: LocalAsrModel): String =
        model.persistentFileName ?: "wr-${model.id}-${model.fileName}"

    internal fun persistentDetectedIds(documents: List<SafStorage.TreeDocument>): Set<String> {
        val docs = documents.associateBy { it.name }
        return catalog.asSequence().filter { model ->
            docs[persistentName(model)]?.let { doc ->
                doc.size <= 0L || model.expectedBytes == null || doc.size == model.expectedBytes
            } == true
        }.mapTo(linkedSetOf()) { it.id }
    }

    fun hasPersistentCopy(context: Context, model: LocalAsrModel): Boolean {
        val doc = ModelFileOps.findPersistentDocument(context, persistentName(model)) ?: return false
        return doc.size <= 0L || model.expectedBytes == null || doc.size == model.expectedBytes
    }

    fun select(context: Context, model: LocalAsrModel) {
        require(isInstalled(context, model)) { "${model.name} model file is incomplete" }
        AppPrefs.setTranscriptionEngine(context, model.engine.id)
        AppPrefs.setModelPath(context, modelFile(context, model).absolutePath)
    }

    suspend fun restoreAndSelect(context: Context, model: LocalAsrModel): Boolean = withContext(Dispatchers.IO) {
        requireNotNull(AppPrefs.getSaveTree(context)) { "Choose a working directory before restoring ${model.name}" }
        val doc = ModelFileOps.findPersistentDocument(context, persistentName(model))
            ?: error("Saved ${model.name} model was not found")
        model.expectedBytes?.let { expected ->
            if (doc.size > 0L) require(doc.size == expected) { "Saved ${model.name} model has the wrong size" }
        }

        ensureModelDir(context, model)
        val target = modelFile(context, model)
        ModelFileOps.restoreDocument(context, doc.uri, target) { restored ->
            model.expectedBytes?.let { expected ->
                require(restored.length() == expected) { "Saved ${model.name} model has the wrong size" }
            }
            require(ModelFileOps.verify(restored, model.expectedBytes, "SHA-256", model.sha256)) {
                "Saved ${model.name} failed checksum verification"
            }
        }
        // The marker must follow the final model filename, not the temporary .restoring file.
        markVerified(target, model)
        verificationMarker(File(target.parentFile, target.name + ".restoring")).delete()
        select(context, model)
        true
    }

    suspend fun download(
        context: Context,
        model: LocalAsrModel,
        onProgress: (percent: Int, downloaded: Long, total: Long, status: String) -> Unit,
        onVerifying: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
        onArchiving: () -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): File = withContext(Dispatchers.IO) {
        ensureModelDir(context, model)
        val target = modelFile(context, model)
        val partial = File(target.parentFile, target.name + ".part")
        val targetValid = isInstalled(context, model)

        if (!targetValid) {
            if (target.exists()) target.delete()
            ModelFileOps.downloadResumable(
                url = model.url,
                partial = partial,
                expectedBytes = model.expectedBytes,
                isCancelled = isCancelled
            ) { done, total ->
                val resolvedTotal = model.expectedBytes ?: total
                val bounded = if (resolvedTotal > 0L) done.coerceAtMost(resolvedTotal) else done
                val percent = if (resolvedTotal > 0L) ((bounded * 100L) / resolvedTotal).toInt().coerceIn(0, 100) else 0
                onProgress(percent, bounded, resolvedTotal, "Downloading ${model.name}…")
            }
            model.expectedBytes?.let { expected ->
                require(partial.length() == expected) {
                    partial.delete()
                    "Downloaded ${model.name} has unexpected size ${partial.length()} (expected $expected)"
                }
            }
            onVerifying(partial.length(), model.expectedBytes ?: partial.length())
            require(ModelFileOps.verify(partial, model.expectedBytes, "SHA-256", model.sha256)) {
                partial.delete()
                "${model.name} checksum verification failed"
            }
            if (target.exists()) require(target.delete()) { "Unable to replace ${model.name}" }
            require(partial.renameTo(target)) { "Unable to finalize ${model.name}" }
            markVerified(target, model)
        } else {
            partial.delete()
            val installedBytes = target.length()
            onProgress(100, installedBytes, model.expectedBytes ?: installedBytes, "${model.name} already downloaded")
            onVerifying(installedBytes, model.expectedBytes ?: installedBytes)
        }

        val shouldArchive = AppPrefs.getSaveTree(context) != null && !hasPersistentCopy(context, model)
        if (shouldArchive) {
            onArchiving()
            archivePersistent(context, model, target)
        }
        select(context, model)
        target
    }

    private fun archivePersistent(context: Context, model: LocalAsrModel, source: File) {
        val (tree, dir) = ModelFileOps.persistentModelsDirectory(context) ?: return
        SafStorage.copyFileToDirectory(
            context = context,
            source = source,
            treeUri = tree,
            directoryUri = dir,
            mime = "application/octet-stream",
            displayName = persistentName(model)
        )
    }

    fun remove(context: Context, model: LocalAsrModel): Boolean {
        val target = modelFile(context, model)
        val localOk = (!target.exists() || target.delete()).also {
            File(target.parentFile, target.name + ".part").delete()
            File(target.parentFile, target.name + ".restoring").delete()
            verificationMarker(target).delete()
        }
        val persistentOk = runCatching {
            ModelFileOps.deletePersistentDocuments(context, persistentName(model))
        }.getOrDefault(false)

        if (AppPrefs.getTranscriptionEngine(context) == model.engine.id && !isInstalled(context, model)) {
            if (!AppPrefs.hasExplicitTranscriptionEngine(context)) {
                AppPrefs.resetAutomaticTranscriptionSelection(context)
            } else {
                AppPrefs.setTranscriptionEngine(context, "whisper")
                AppPrefs.setModelPath(context, null)
            }
        }
        return localOk && persistentOk
    }
}
