package com.example.whisperrecorder.npu

import android.content.Context
import android.net.Uri
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.models.ModelFileOps
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File

/** Experimental Omi QNN package; reuses the app's resumable model-download service and SAF archive. */
internal object OmiNpuPackageStore {
    const val DOWNLOAD_ID = "omi-med-v1-sm8850-v81-npu-test"
    const val DISPLAY_NAME = "Omi Med STT v1 · Qualcomm NPU"
    const val TRANSCRIPTION_MODEL_KEY = "npu://omi-med-stt-v1-sm8850-v81-qnn-2.50"
    const val REPO_ID = "drinnird/omi-med-stt-v1-snapdragon-sm8850-v81"
    const val PACKAGE_ID = "omi-med-stt-v1-snapdragon-sm8850-v81"
    const val REVISION = "qnn-2.50-experimental"
    const val ARCHIVE_PATH = "Whisperapp/Models/$PACKAGE_ID/$REVISION"
    const val MANIFEST = "model-manifest.json"
    private const val READY = "READY.json"
    private const val BUFFER = 1024 * 1024
    val ASSETS = OmiNpuProbeContracts.ALL.associate { it.graph.artifactPath to
        Asset(it.graph.artifactBytes, it.sha256) } + mapOf(
        "token_embedding.npy" to Asset(2_624_128L, "7a3664cec196ff0345a94cfe763a45e268c56c93d2b71bbb2455112271e4b8f1"),
        "tokenizer.model" to Asset(251_396L, "5a3a82c48998709f9bc9f5db2924d99a899d20bf82550ca52272da7c7557b0a0")
    )
    val totalBytes = ASSETS.values.sumOf { it.bytes }
    data class Asset(val bytes: Long, val sha256: String)
    data class Archive(val tree: Uri, val folder: Uri)

    private fun url(name: String): String {
        require(name == MANIFEST || name in ASSETS) { "Invalid Omi artifact $name" }
        return "https://huggingface.co/$REPO_ID/resolve/main/$name?download=true"
    }

    fun archive(context: Context): Archive? {
        val tree = AppPrefs.getSaveTree(context) ?: return null
        val models = WorkspaceStorage.modelsDirectory(context, tree)
        val pkg = SafStorage.findChildDirectory(context, tree, models, PACKAGE_ID) ?: return null
        val rev = SafStorage.findChildDirectory(context, tree, pkg, REVISION) ?: return null
        return Archive(tree, rev)
    }

    private fun ensureArchive(context: Context): Archive {
        val tree = requireNotNull(AppPrefs.getSaveTree(context)) { "Select a WhisperRecorder storage folder first" }
        val models = WorkspaceStorage.modelsDirectory(context, tree)
        val pkg = SafStorage.getOrCreateChildDirectory(context, tree, models, PACKAGE_ID)
        val rev = SafStorage.getOrCreateChildDirectory(context, tree, pkg, REVISION)
        return Archive(tree, rev)
    }

    fun archiveState(context: Context): NpuArchiveState {
        val stored = archive(context) ?: return NpuArchiveState.NOT_INSTALLED
        val documents = SafStorage.listDirectory(context, stored.tree, stored.folder).associateBy { it.name }
        val ready = documents[READY] ?: return NpuArchiveState.INCOMPLETE
        val validSizes = ASSETS.all { (name, asset) -> documents[name]?.size == asset.bytes }
        return if (ready.size > 0L && validSizes && documents.containsKey(MANIFEST))
            NpuArchiveState.READY else NpuArchiveState.INCOMPLETE
    }

    fun selectForTranscription(context: Context) {
        require(archiveState(context) == NpuArchiveState.READY) { "Omi NPU package is not ready" }
        com.example.whisperrecorder.data.AppPrefs.setTranscriptionEngine(context, "omi_npu")
        com.example.whisperrecorder.data.AppPrefs.setModelPath(context, null)
    }

    /** Remove only this Omi package, leaving other NPU models and shared runtimes untouched. */
    fun deleteArchive(context: Context): Boolean {
        val tree = AppPrefs.getSaveTree(context) ?: return true
        val models = runCatching { WorkspaceStorage.modelsDirectory(context, tree) }.getOrNull() ?: return true
        val packageDir = SafStorage.findChildDirectory(context, tree, models, PACKAGE_ID) ?: return true
        val deleted = SafStorage.deleteDocument(context, packageDir)
        if (deleted) {
            File(context.filesDir, "npu-download/omi-$REVISION").deleteRecursively()
            File(context.filesDir, "npu-runtime/omi-$REVISION").deleteRecursively()
            if (AppPrefs.getTranscriptionEngine(context) == "omi_npu") {
                if (!AppPrefs.hasExplicitTranscriptionEngine(context)) {
                    AppPrefs.resetAutomaticTranscriptionSelection(context)
                } else {
                    AppPrefs.setTranscriptionEngine(context, "whisper")
                    AppPrefs.setModelPath(context, null)
                }
            }
        }
        return deleted
    }

    /** Called from the isolated NPU worker, with no implicit model download or selection. */
    fun probeArtifacts(context: Context): Map<String, SafStorage.TreeDocument> {
        require(archiveState(context) == NpuArchiveState.READY) {
            "Omi NPU package is not ready. Download or repair it in Models & downloads."
        }
        val stored = requireNotNull(archive(context))
        return SafStorage.listDirectory(context, stored.tree, stored.folder)
            .filter { it.name in ASSETS }.associateBy { it.name }
    }

    private fun validateManifest(file: File) {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        require(json.optString("kind") == "whisperrecorder_omi_med_v2_sm8850_v81_experimental_qnn") {
            "Hugging Face Omi package kind mismatch"
        }
        require(json.optString("model_id") == "omi-health/omi-med-stt-v1") {
            "Hugging Face Omi model identity mismatch"
        }
        val artifacts = json.getJSONObject("artifacts")
        ASSETS.forEach { (name, expected) ->
            val item = artifacts.getJSONObject(name)
            require(item.getLong("bytes") == expected.bytes &&
                item.getString("sha256").equals(expected.sha256, ignoreCase = true)) {
                "Published Omi manifest does not match pinned $name checksum or size"
            }
        }
    }

    private fun verifyArchived(
        context: Context, document: SafStorage.TreeDocument, asset: Asset,
        cancelled: () -> Boolean
    ): Boolean {
        if (document.size != asset.bytes) return false
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val stream = context.contentResolver.openInputStream(document.uri) ?: return false
        var count = 0L
        stream.buffered(BUFFER).use { input ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                if (cancelled()) throw CancellationException("Omi archive verification cancelled")
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
                count += n
            }
        }
        return count == asset.bytes && digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }.equals(asset.sha256, ignoreCase = true)
    }

    fun installOrRepair(
        context: Context,
        isCancelled: () -> Boolean,
        onProgress: (NpuPackageProgress) -> Unit
    ) {
        val app = context.applicationContext
        // An intact READY package is not re-hashed on every UI refresh (encoder is 1.2 GB).
        if (archiveState(app) == NpuArchiveState.READY) {
            onProgress(NpuPackageProgress("ready", bytesDone = totalBytes, bytesTotal = totalBytes,
                message = "Omi NPU package already installed"))
            return
        }
        val stored = ensureArchive(app)
        val staging = File(app.filesDir, "npu-download/omi-$REVISION").apply { mkdirs() }
        val manifest = File(staging, MANIFEST + ".part")
        var stage = "fetch-manifest"
        var current = MANIFEST
        try {
            onProgress(NpuPackageProgress("manifest", MANIFEST, message = "Downloading Omi model manifest…"))
            ModelFileOps.downloadResumable(url(MANIFEST), manifest, isCancelled = isCancelled) { _, _ -> }
            stage = "validate-manifest"
            try { validateManifest(manifest) } catch (t: Throwable) {
                manifest.delete() // Force a fresh remote manifest on retry after a mismatch.
                throw t
            }
            val existing = SafStorage.listDirectory(app, stored.tree, stored.folder).associateBy { it.name }
            var completed = 0L
            ASSETS.forEach { (name, info) ->
                if (isCancelled()) throw CancellationException("Omi NPU model download cancelled")
                current = name
                stage = "check-existing"
                val present = existing[name]
                if (present != null && verifyArchived(app, present, info, isCancelled)) {
                    completed += info.bytes
                    onProgress(NpuPackageProgress("verified", name, bytesDone = completed, bytesTotal = totalBytes,
                        message = "Verified existing $name"))
                    return@forEach
                }
                val partial = File(staging, "$name.part")
                if (partial.isFile && partial.length() == info.bytes &&
                    !ModelFileOps.verify(partial, info.bytes, "SHA-256", info.sha256)) {
                    require(partial.delete()) { "Unable to discard corrupt Omi partial $name" }
                }
                val base = completed
                stage = "download"
                ModelFileOps.downloadResumable(url(name), partial, info.bytes, isCancelled) { done, _ ->
                    onProgress(NpuPackageProgress("download", name, bytesDone = base + done,
                        bytesTotal = totalBytes, message = "Downloading $name…"))
                }
                onProgress(NpuPackageProgress("verify", name, bytesDone = completed,
                    bytesTotal = totalBytes, message = "Verifying $name SHA-256…"))
                stage = "verify-download"
                require(ModelFileOps.verify(partial, info.bytes, "SHA-256", info.sha256)) {
                    "Downloaded $name failed SHA-256 verification"
                }
                stage = "archive-copy"
                val archived = SafStorage.copyFileToDirectory(app, partial, stored.tree, stored.folder,
                    "application/octet-stream", name)
                stage = "verify-archived-copy"
                require(verifyArchived(app, SafStorage.TreeDocument(archived, name, info.bytes,
                    "application/octet-stream"), info, isCancelled)) {
                    "Archived $name failed SHA-256 readback after copy; temporary download retained"
                }
                require(partial.delete()) { "Unable to clean up staging for $name" }
                completed += info.bytes
                onProgress(NpuPackageProgress("archive", name, bytesDone = completed,
                    bytesTotal = totalBytes, message = "Saved $name in Whisperapp/Models"))
            }
            current = READY
            stage = "finalize"
            val finalDocuments = SafStorage.listDirectory(app, stored.tree, stored.folder).associateBy { it.name }
            require(ASSETS.all { (name, asset) -> finalDocuments[name]?.size == asset.bytes }) {
                "Omi archive is missing an expected artifact or has an incorrect size"
            }
            SafStorage.copyFileToDirectory(app, manifest, stored.tree, stored.folder,
                "application/json", MANIFEST)
            val marker = File(staging, READY)
            marker.writeText(JSONObject().put("package", PACKAGE_ID).put("revision", REVISION)
                .put("status", "verified").toString())
            SafStorage.copyFileToDirectory(app, marker, stored.tree, stored.folder,
                "application/json", READY)
            onProgress(NpuPackageProgress("ready", bytesDone = totalBytes, bytesTotal = totalBytes,
                message = "Omi NPU assets verified and saved"))
        } catch (t: Throwable) {
            runCatching { DiagnosticLogger.appendError(app, "omi-npu-install stage=$stage file=$current", t) }
            throw t
        } finally {
            if (runCatching { archiveState(app) }.getOrNull() == NpuArchiveState.READY) {
                staging.deleteRecursively()
            }
        }
    }
}
