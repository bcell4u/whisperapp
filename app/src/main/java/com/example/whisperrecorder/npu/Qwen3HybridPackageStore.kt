package com.example.whisperrecorder.npu

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import java.io.FileInputStream
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.models.ModelFileOps
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Stage G/H/I archives use the SAME SAF Whisperapp/Models workspace as Parakeet/Omi.
 * Runtime first tries seekable archive FDs with temporary /proc/self/fd links.
 * Private or transient copies are fallback only, and are never downloaded twice.
 */
internal object Qwen3HybridPackageStore {
    const val DOWNLOAD_ID = "qwen3-0.6b-sm8850-v81-hybrid"
    const val DISPLAY_NAME = "Qwen3-ASR 0.6B · S26 NPU encoder + CPU decoder (experimental)"
    const val MODEL_KEY = "qwen3-hybrid://sm8850-v81-0.6b-stage-g"
    // Generated locally at runtime, never downloaded or included in the signed model manifest.
    const val EP_CONTEXT_WRAPPER_NAME = "qwen3-encoder-epcontext.onnx"
    const val REPO_ID = "drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81"
    private const val SCHEMA = "whisperrecorder.qwen3-s26-hybrid-package"
    private const val EXPORTER = "4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50"
    private const val READY = "READY.json"
    private const val MANIFEST = "model-manifest.json"
    private val LEGACY_WEIGHTS = setOf("encoder-qnn-fp16.bin", "decoder_init.onnx", "decoder_init.onnx.data",
        "decoder_step.onnx", "decoder_step.onnx.data", "embed_tokens.bin", "tokenizer.json",
        "config.json", "golden-synthetic-pcm-12s-f32.raw", "golden-synthetic-mel-12s-f32.raw")
    private const val CONTRACT = "android-integration-contract.json"
    private const val COMPACT_REVISION = "main"
    private const val SHARED = "decoder_weights.data"
    private val COMPACT_WEIGHTS = (LEGACY_WEIGHTS - setOf("decoder_init.onnx.data", "decoder_step.onnx.data")) + SHARED
    private const val COMPACT_STORAGE = "lossless_shared_fp32_external_data"
    private const val FP16_PACKED = "decoder_weights_fp16.data"
    private const val FP16_STORAGE = "shared_fp16_storage_fp32_cast"
    private const val FP16_PARITY = "one_real_speech_and_synthetic_exact_token_parity"

    /** The unsigned experimental companion is never used without K2A's exact content hash.
     * It is optional and does not replace the v3 manifest's authoritative FP32 embeddings.
     */
    private fun validK2aCompanion(context: Context, uri: Uri): Boolean = runCatching {
        val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        (size < 0 || size == QwenEmbeddingReader.FP16_BYTES) &&
            archiveDigest(context, uri) { false }.equals(QwenEmbeddingReader.FP16_SHA256, true)
    }.getOrDefault(false)
    private val FP16_WEIGHTS = (COMPACT_WEIGHTS - SHARED) + FP16_PACKED
    private const val COMPACT_SMOKE = "exact_cpu_onnx_decoder_token_smoke_pass"
    private const val MAX_PACKAGE_BYTES = 9_000_000_000L
    data class Asset(val name: String, val bytes: Long, val sha256: String)

    /** Legacy app-private working directory, preserved until an explicit verified reclaim.
     * New shared-descriptor sessions avoid requiring another permanent model copy.
     */
    private const val PERSISTENT_ROOT = "whisperapp/models/qwen3-asr-0.6b"
    const val VISIBLE_PACKAGE = "qwen3-asr-0.6b"
    const val VISIBLE_PATH = "Whisperapp/Models/qwen3-asr-0.6b"
    private const val PREVIOUS_ROOT = "qwen3-hybrid"
    private val supportedStages = listOf("stage-i-fp16-0.6b", "stage-h-compact-0.6b", "stage-g-0.6b")
    private val migrationLock = Any()

    /** Rename, do not copy, gigabytes of existing verified/partial model data on the same volume.
     * A failed migration leaves the original folder intact and still usable. Never redownload.
     * The :qwen service can start independently, so both GUI and worker use the same logic.
     */
    private fun migrateExistingFolders(context: Context) {
        synchronized(migrationLock) {
            for (stage in supportedStages) {
                val previous = File(context.filesDir, "$PREVIOUS_ROOT/$stage")
                if (!previous.isDirectory) continue
                val destination = File(context.filesDir, "$PERSISTENT_ROOT/$stage")
                if (destination.exists()) continue // Never overwrite a newer/partial folder.
                val parent = destination.parentFile ?: continue
                if (!parent.isDirectory && !parent.mkdirs()) continue
                // renameTo is a filesystem move, so large decoder weights are not copied.
                // On failure the previous directory remains available for selection.
                previous.renameTo(destination)
            }
        }
    }

    private fun stageFolder(context: Context, stage: String, version: Int): File {
        require(stage in supportedStages)
        migrateExistingFolders(context)
        val persistent = File(context.filesDir, "$PERSISTENT_ROOT/$stage")
        val previous = File(context.filesDir, "$PREVIOUS_ROOT/$stage")
        return when {
            localState(persistent, version) == NpuArchiveState.READY -> persistent
            localState(previous, version) == NpuArchiveState.READY -> previous
            persistent.exists() -> persistent // Resume partial new-layout downloads.
            previous.exists() -> previous // Resume the original download without re-fetching.
            else -> persistent
        }
    }

    /** SAF archive is the durable, user-visible master. It is never treated as a direct ONNX path. */
    private data class VisibleLocation(val tree: Uri, val directory: Uri)
    private fun visibleLocation(context: Context, stage: String, create: Boolean): VisibleLocation? {
        require(stage in supportedStages)
        val tree = AppPrefs.getSaveTree(context) ?: return null
        val models = if (create) WorkspaceStorage.modelsDirectory(context, tree)
            else runCatching { WorkspaceStorage.modelsDirectory(context, tree) }.getOrNull() ?: return null
        val root = if (create) SafStorage.getOrCreateChildDirectory(context, tree, models, VISIBLE_PACKAGE)
            else SafStorage.findChildDirectory(context, tree, models, VISIBLE_PACKAGE) ?: return null
        val directory = if (create) SafStorage.getOrCreateChildDirectory(context, tree, root, stage)
            else SafStorage.findChildDirectory(context, tree, root, stage) ?: return null
        return VisibleLocation(tree, directory)
    }

    private fun visibleItems(context: Context, loc: VisibleLocation): Map<String, SafStorage.TreeDocument> =
        SafStorage.listDirectory(context, loc.tree, loc.directory).associateBy { it.name }

    private fun readVisibleText(context: Context, uri: Uri, maxBytes: Int = 2_000_000): String {
        val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() <= maxBytes) {
                val count = input.read(buffer)
                if (count == -1) break
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        }
        require(bytes.size <= maxBytes) { "Qwen metadata is unexpectedly large" }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun visibleState(context: Context, stage: String, version: Int): NpuArchiveState {
        val loc = runCatching { visibleLocation(context, stage, false) }.getOrNull()
            ?: return NpuArchiveState.NOT_INSTALLED
        return runCatching {
            val documents = visibleItems(context, loc)
            val manifestDoc = documents[MANIFEST] ?: return@runCatching NpuArchiveState.INCOMPLETE
            val contractDoc = documents[CONTRACT] ?: return@runCatching NpuArchiveState.INCOMPLETE
            val readyDoc = documents[READY] ?: return@runCatching NpuArchiveState.INCOMPLETE
            val manifestText = readVisibleText(context, manifestDoc.uri)
            val parsed = JSONObject(manifestText)
            val required = expected(parsed, version)
            val ready = JSONObject(readVisibleText(context, readyDoc.uri))
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(manifestText.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            require(ready.getString("manifest_sha256").equals(digest, ignoreCase = true))
            checkContract(JSONObject(readVisibleText(context, contractDoc.uri)), parsed, version)
            require(required.all { documents[it.name]?.size == it.bytes })
            NpuArchiveState.READY
        }.getOrDefault(NpuArchiveState.INCOMPLETE)
    }

    fun visibleArchiveReady(context: Context): Boolean = supportedStages.withIndex().any { (index, stage) ->
        visibleState(context, stage, 3 - index) == NpuArchiveState.READY
    }

    private fun archiveDigest(context: Context, uri: Uri, cancelled: () -> Boolean): String {
        val hash = MessageDigest.getInstance("SHA-256")
        requireNotNull(context.contentResolver.openInputStream(uri)).buffered(1024 * 1024).use { stream ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                if (cancelled()) throw CancellationException("Qwen archive operation cancelled")
                val count = stream.read(buffer)
                if (count == -1) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    /** One-time offline migration / fresh-download archive. This COPIES existing local bytes,
     * never downloads or deletes the verified runtime package. READY.json is published LAST.
     * ONNX Runtime requires an app-private working copy for its external-data relative paths.
     */
    fun saveInstalledToWhisperappModels(
        context: Context,
        cancelled: () -> Boolean = { false },
        progress: (Int, Long, Long, String) -> Unit = { _, _, _, _ -> }
    ): String {
        val dir = verifiedFolder(context)
        val stage = dir.name
        val version = when(stage) { "stage-i-fp16-0.6b" -> 3; "stage-h-compact-0.6b" -> 2; else -> 1 }
        val manifest = File(dir, MANIFEST)
        val assets = expected(JSONObject(manifest.readText()), version)
        val loc = requireNotNull(visibleLocation(context, stage, true)) {
            "Choose the existing Whisperapp storage folder first"
        }
        val files = listOf(MANIFEST, CONTRACT) + assets.map { it.name } + READY
        val total = files.sumOf { File(dir, it).length() }
        var done = 0L
        for (name in files) {
            if (cancelled()) throw CancellationException("Qwen archiving cancelled")
            val source = File(dir, name)
            require(source.isFile) { "Missing verified Qwen artifact: $name" }
            val digest = ModelFileOps.digest(source, "SHA-256", cancelled)
            var existing = visibleItems(context, loc)[name]
            val alreadyValid = existing != null && existing.size == source.length() &&
                archiveDigest(context, existing.uri, cancelled).equals(digest, true)
            if (!alreadyValid) {
                val copied = SafStorage.copyFileToDirectory(context, source, loc.tree, loc.directory,
                    "application/octet-stream", name)
                require(archiveDigest(context, copied, cancelled).equals(digest, true)) {
                    "Qwen archive checksum mismatch: $name; runtime copy was preserved"
                }
                existing = visibleItems(context, loc)[name]
                require(existing?.size == source.length()) { "Qwen archive size mismatch: $name" }
            }
            done += source.length()
            progress(((done * 100L) / total).toInt().coerceIn(0, 100), done, total,
                "Saving Qwen/$stage/$name to Whisperapp/Models")
        }
        require(visibleState(context, stage, version) == NpuArchiveState.READY) {
            "Qwen public archive verification failed; original runtime copy preserved"
        }
        return "$VISIBLE_PATH/$stage"
    }

    /** Restore missing local ONNX/QNN runtime bytes from the SAF master, without network access. */
    private fun restoreFromVisibleArchive(context: Context, stage: String, version: Int): Boolean {
        if (visibleState(context, stage, version) != NpuArchiveState.READY) return false
        val loc = visibleLocation(context, stage, false) ?: return false
        val documents = visibleItems(context, loc)
        val parsed = JSONObject(readVisibleText(context, documents.getValue(MANIFEST).uri))
        val required = expected(parsed, version)
        val dir = stageFolder(context, stage, version)
        require(dir.mkdirs() || dir.isDirectory)
        val desired = listOf(MANIFEST, CONTRACT) + required.map { it.name } + READY
        for (name in desired) {
            val doc = documents.getValue(name)
            val file = File(dir, name)
            val expectedHash = required.firstOrNull { it.name == name }?.sha256
                ?: archiveDigest(context, doc.uri) { false }
            if (ModelFileOps.verify(file, doc.size, "SHA-256", expectedHash)) continue
            ModelFileOps.restoreDocument(context, doc.uri, file) { incoming ->
                require(ModelFileOps.verify(incoming, doc.size, "SHA-256", expectedHash)) {
                    "Qwen restored artifact failed SHA-256: $name"
                }
            }
        }
        require(localState(dir, version) == NpuArchiveState.READY)
        return true
    }


    /** File-descriptor bridge from the canonical SAF archive (Parakeet-style).
     * A private directory contains only symlinks to already-open archive descriptors,
     * never permanent weight copies. All descriptors MUST outlive ORT sessions.
     * If an Android DocumentsProvider supplies nonseekable descriptors, caller can
     * fall back to the older local model or transient verified materialization.
     */
    class RuntimeAssets internal constructor(
        val directory: File,
        val mode: String,
        val stage: String,
        private val owned: Boolean,
        private val handles: List<ParcelFileDescriptor>
    ) : AutoCloseable {
        override fun close() {
            // ORT sessions must already have closed before descriptors or links go away.
            if (owned) directory.deleteRecursively()
            handles.forEach { runCatching { it.close() } }
        }
    }

    private fun sharedCache(context: Context): File = File(context.cacheDir, "qwen3-shared-fd-runtime")
    private fun transientCache(context: Context): File = File(context.filesDir, "qwen3-verified-transient-runtime")

    /** Recover abandoned Qwen-only temporary files after a killed process. */
    fun cleanupAbandonedRuntime(context: Context): Boolean = listOf(sharedCache(context), transientCache(context))
        .all { !it.exists() || (it.deleteRecursively() && !it.exists()) }

    private fun bestVisibleStage(context: Context): Pair<String, Int>? =
        supportedStages.zip(listOf(3, 2, 1)).firstOrNull { (stage, version) ->
            visibleState(context, stage, version) == NpuArchiveState.READY
        }

    /** Hash-check every SAF asset on worker startup, not just its displayed file size.
     * /proc/self/fd symlinks need actual seekable file descriptors. Never resolve arbitrary
     * paths from the SAF provider and never write model contents into app-private storage.
     */
    fun openSharedRuntime(context: Context): RuntimeAssets? {
        val (stage, version) = bestVisibleStage(context) ?: return null
        val loc = requireNotNull(visibleLocation(context, stage, false))
        val documents = visibleItems(context, loc)
        val metadata = readVisibleText(context, documents.getValue(MANIFEST).uri)
        val assets = expected(JSONObject(metadata), version)
        val directory = sharedCache(context)
        require(!directory.exists() || directory.deleteRecursively()) { "Stale Qwen descriptor bridge could not be cleared" }
        require(directory.mkdirs())
        val descriptors = ArrayList<ParcelFileDescriptor>()
        try {
            for (asset in assets) {
                val source = requireNotNull(documents[asset.name]) { "Missing saved Qwen asset ${asset.name}" }
                require(source.size == asset.bytes) { "Shared Qwen file size changed: ${asset.name}" }
                val handle = requireNotNull(context.contentResolver.openFileDescriptor(source.uri, "r")) {
                    "Cannot open Qwen archive descriptor ${asset.name}"
                }
                descriptors.add(handle)
                require(handle.statSize < 0L || handle.statSize == asset.bytes)
                Os.lseek(handle.fileDescriptor, 0L, OsConstants.SEEK_SET)
                require(archiveDigest(context, source.uri) { false }.equals(asset.sha256, true)) {
                    "Saved Qwen file hash mismatch: ${asset.name}"
                }
                val link = File(directory, asset.name)
                Os.symlink("/proc/self/fd/${handle.fd}", link.absolutePath)
                // Some SAF providers expose proc FDs but do not permit reopening them.
                FileInputStream(link).use { require(it.read() >= 0) }
            }
            if (version == 3) {
                documents[QwenEmbeddingReader.FP16_FILE]?.let { companion ->
                    if (validK2aCompanion(context, companion.uri)) {
                        val handle = context.contentResolver.openFileDescriptor(companion.uri, "r")
                        if (handle != null) {
                            descriptors.add(handle)
                            Os.lseek(handle.fileDescriptor, 0L, OsConstants.SEEK_SET)
                            Os.symlink("/proc/self/fd/${handle.fd}",
                                File(directory, QwenEmbeddingReader.FP16_FILE).absolutePath)
                        }
                    }
                }
            }
            File(directory, MANIFEST).writeText(metadata)
            File(directory, CONTRACT).writeText(readVisibleText(context, documents.getValue(CONTRACT).uri))
            File(directory, READY).writeText(readVisibleText(context, documents.getValue(READY).uri))
            return RuntimeAssets(directory, "saf-proc-fd", stage, true, descriptors)
        } catch (t: Throwable) {
            directory.deleteRecursively()
            descriptors.forEach { runCatching { it.close() } }
            throw IllegalStateException("Qwen SAF descriptor bridge unavailable: ${t.message}", t)
        }
    }

    /** If direct SAF loading fails, prefer an already-installed working copy. The last
     * resort is a transient, hash-verified staging directory, deleted when the session
     * closes. Nothing here triggers a network download.
     */
    fun openFallbackRuntime(context: Context): RuntimeAssets {
        for ((stage, version) in supportedStages.zip(listOf(3, 2, 1))) {
            val dir = stageFolder(context, stage, version)
            if (localState(dir, version) == NpuArchiveState.READY) {
                // A READY marker alone is insufficient for safety.
                val manifest = JSONObject(File(dir, MANIFEST).readText())
                require(expected(manifest, version).all { asset ->
                    ModelFileOps.verify(File(dir, asset.name), asset.bytes, "SHA-256", asset.sha256)
                }) { "Previously installed Qwen working copy has changed" }
                return RuntimeAssets(dir, "previous-private-runtime", stage, false, emptyList())
            }
        }
        val (stage, version) = requireNotNull(bestVisibleStage(context)) {
            "No verified shared Qwen archive and no existing private model; network download is not required"
        }
        val loc = requireNotNull(visibleLocation(context, stage, false))
        val docs = visibleItems(context, loc)
        val parsed = JSONObject(readVisibleText(context, docs.getValue(MANIFEST).uri))
        val assets = expected(parsed, version)
        val directory = transientCache(context)
        val necessaryBytes = assets.sumOf { it.bytes } + 256L * 1024L * 1024L
        require(StatFs(context.filesDir.absolutePath).availableBytes >= necessaryBytes) {
            "Shared Qwen descriptor loading unavailable; transient restore needs $necessaryBytes free bytes. Original archive remains in Whisperapp/Models."
        }
        require(!directory.exists() || directory.deleteRecursively())
        require(directory.mkdirs())
        try {
            for (asset in assets) {
                val target = File(directory, asset.name)
                ModelFileOps.restoreDocument(context, docs.getValue(asset.name).uri, target) { incoming ->
                    require(ModelFileOps.verify(incoming, asset.bytes, "SHA-256", asset.sha256)) {
                        "Qwen transient copy failed SHA-256: ${asset.name}"
                    }
                }
            }
            if (version == 3) {
                docs[QwenEmbeddingReader.FP16_FILE]?.let { companion ->
                    if (validK2aCompanion(context, companion.uri)) {
                        ModelFileOps.restoreDocument(context, companion.uri,
                            File(directory, QwenEmbeddingReader.FP16_FILE)) { incoming ->
                            require(incoming.length() == QwenEmbeddingReader.FP16_BYTES)
                            require(ModelFileOps.digest(incoming, "SHA-256")
                                .equals(QwenEmbeddingReader.FP16_SHA256, true))
                        }
                    }
                }
            }
            File(directory, MANIFEST).writeText(readVisibleText(context, docs.getValue(MANIFEST).uri))
            File(directory, CONTRACT).writeText(readVisibleText(context, docs.getValue(CONTRACT).uri))
            File(directory, READY).writeText(readVisibleText(context, docs.getValue(READY).uri))
            return RuntimeAssets(directory, "saf-transient-verified", stage, true, emptyList())
        } catch (t: Throwable) {
            directory.deleteRecursively()
            throw t
        }
    }

    private const val PROOF_PREFS = "qwen-shared-fd-proof"
    fun noteSuccessfulSharedTranscription(context: Context, stage: String) {
        val folder = visibleLocation(context, stage, false) ?: return
        val manifest = visibleItems(context, folder)[MANIFEST] ?: return
        val digest = MessageDigest.getInstance("SHA-256").digest(
            readVisibleText(context, manifest.uri).toByteArray(Charsets.UTF_8)
        ).joinToString("") { "%02x".format(it) }
        context.getSharedPreferences(PROOF_PREFS, Context.MODE_PRIVATE).edit()
            .putString("proven_$stage", digest).apply()
    }

    /** Only remove duplicate PRIVATE weights on explicit request, after an actual shared-FD
     * transcript and full SHA-256 verification of the persistent archive. Never remove SAF.
     */
    fun reclaimPrivateDuplicate(context: Context): Long {
        val (stage, version) = requireNotNull(bestVisibleStage(context)) { "Qwen must be saved in Whisperapp/Models first" }
        val loc = requireNotNull(visibleLocation(context, stage, false))
        val docs = visibleItems(context, loc)
        val content = readVisibleText(context, docs.getValue(MANIFEST).uri)
        val manifestDigest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        require(context.getSharedPreferences(PROOF_PREFS, Context.MODE_PRIVATE)
            .getString("proven_$stage", null) == manifestDigest) {
            "Run a successful Qwen transcription from the shared archive before removing the backup"
        }
        require(expected(JSONObject(content), version).all { asset ->
            val doc = docs[asset.name] ?: return@all false
            doc.size == asset.bytes && archiveDigest(context, doc.uri) { false }.equals(asset.sha256, true)
        }) { "Qwen shared archive hash verification failed; private backup preserved" }
        val duplicate = File(context.filesDir, "$PERSISTENT_ROOT/$stage")
        val old = File(context.filesDir, "$PREVIOUS_ROOT/$stage")
        var reclaimed = 0L
        for (dir in listOf(duplicate, old)) {
            if (!dir.isDirectory) continue
            // Only the known stage inside exact app-owned directories is deletable.
            val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            require(dir.deleteRecursively() && !dir.exists()) { "Could not clear Qwen private duplicate" }
            reclaimed += size
        }
        return reclaimed
    }

    private fun ensureLocalRuntime(context: Context) {
        if (listOf(fp16Folder(context) to 3, compactFolder(context) to 2,
                legacyFolder(context) to 1).any { (dir, version) -> localState(dir, version) == NpuArchiveState.READY }) return
        for ((stage, version) in supportedStages.zip(listOf(3, 2, 1))) {
            if (restoreFromVisibleArchive(context, stage, version)) return
        }
    }

    fun fp16Folder(context: Context): File = stageFolder(context, "stage-i-fp16-0.6b", 3)
    fun compactFolder(context: Context): File = stageFolder(context, "stage-h-compact-0.6b", 2)
    fun legacyFolder(context: Context): File = stageFolder(context, "stage-g-0.6b", 1)

    fun packageDescription(context: Context): String = when {
        fp16Installed(context) -> "Stage I · shared FP16-stored CPU decoder · S26 only"
        compactInstalled(context) -> "Stage H · shared FP32 CPU decoder · S26 only"
        legacyInstalled(context) -> "Stage G · original FP32 CPU decoder · S26 only"
        else -> "Stage I · compact FP16-stored CPU decoder · S26 only"
    }
    fun folder(context: Context): File = when {
        localState(fp16Folder(context), 3) == NpuArchiveState.READY -> fp16Folder(context)
        localState(compactFolder(context), 2) == NpuArchiveState.READY -> compactFolder(context)
        localState(legacyFolder(context), 1) == NpuArchiveState.READY -> legacyFolder(context)
        else -> fp16Folder(context)
    }
    fun fp16Installed(context: Context): Boolean = localState(fp16Folder(context), 3) == NpuArchiveState.READY
    fun compactInstalled(context: Context): Boolean = localState(compactFolder(context), 2) == NpuArchiveState.READY
    fun legacyInstalled(context: Context): Boolean = localState(legacyFolder(context), 1) == NpuArchiveState.READY
    private fun url(name: String): String {
        require(name in FP16_WEIGHTS || name == MANIFEST || name == CONTRACT)
        return "https://huggingface.co/$REPO_ID/resolve/$COMPACT_REVISION/$name?download=true"
    }
    private fun sha(file: File): String = ModelFileOps.digest(file, "SHA-256")
    private fun expected(manifest: JSONObject, schemaVersion: Int): List<Asset> {
        require(manifest.optString("schema") == SCHEMA && manifest.optInt("schema_version") == schemaVersion)
            { "Unsupported Qwen package manifest" }
        require(manifest.optString("variant") == "0.6B" && manifest.optString("model_id") == "Qwen/Qwen3-ASR-0.6B")
        require(manifest.optString("exporter_commit") == EXPORTER)
        require(manifest.optString("status") == "experimental_hybrid_real_speech_pass_android_pending")
        require(manifest.optString("backend") == "SM8850_V81_QNN_FP16_encoder_and_ONNX_FP32_CPU_decoder")
        require(manifest.optString("validated_s26_model") == "SM-S942U")
        require(manifest.optBoolean("real_speech_exact_token_parity") && manifest.optBoolean("device_temp_files_removed"))
        require(!manifest.optBoolean("production_qualified", true) && !manifest.optBoolean("npu_decoder_compiled", true))
        if (schemaVersion == 3) {
            require(manifest.optString("decoder_weight_storage") == FP16_STORAGE)
            require(manifest.optBoolean("experimental_fp16_storage") && !manifest.optBoolean("production_qualified", true))
            require(manifest.getJSONObject("fp16_cpu_parity").getString("status") == FP16_PARITY)
            require(manifest.optLong("additional_saved_bytes") > 0L)
            require(manifest.optString("original_stage_h_manifest_sha256").matches(Regex("[0-9a-fA-F]{64}")))
        }
        if (schemaVersion == 2) {
            require(manifest.optString("decoder_weight_storage") == COMPACT_STORAGE)
            require(manifest.getJSONObject("decoder_smoke").optString("status") == COMPACT_SMOKE)
            require(manifest.optLong("saved_bytes") > 0L)
            require(manifest.optString("original_stage_g_manifest_sha256").matches(Regex("[0-9a-fA-F]{64}")))
        }
        val weights = when(schemaVersion) {
            3 -> FP16_WEIGHTS
            2 -> COMPACT_WEIGHTS
            1 -> LEGACY_WEIGHTS
            else -> error("Unsupported Qwen manifest version")
        }
        val files = manifest.getJSONArray("files")
        val found = LinkedHashMap<String, Asset>()
        var total = 0L
        for (i in 0 until files.length()) {
            val entry = files.getJSONObject(i)
            val name = entry.getString("path")
            require(name in weights && name !in found) { "Unexpected or duplicated Qwen package file: $name" }
            val bytes = entry.getLong("size_bytes")
            val hash = entry.getString("sha256")
            require(bytes in 1L..3_000_000_000L && hash.matches(Regex("[0-9a-fA-F]{64}")))
            total = Math.addExact(total, bytes)
            found[name] = Asset(name, bytes, hash)
        }
        require(found.keys == weights && total < MAX_PACKAGE_BYTES) { "Qwen package assets missing or too large" }
        require(found.getValue("encoder-qnn-fp16.bin").sha256.equals(manifest.getString("compiled_context_sha256"), true))
        require(found.getValue("embed_tokens.bin").bytes == 151936L * 1024L * 4L)
        require(found.getValue("golden-synthetic-pcm-12s-f32.raw").bytes == 192000L * 4L)
        require(found.getValue("golden-synthetic-mel-12s-f32.raw").bytes == 128L * 1200L * 4L)
        return found.values.toList()
    }

    private fun checkContract(contract: JSONObject, manifest: JSONObject, schemaVersion: Int) {
        require(contract.optString("schema") == "whisperrecorder.qwen3-sm8850-hybrid-android-contract")
        require(contract.optInt("version") == schemaVersion) { "Incorrect Qwen Android contract version" }
        require(contract.optString("backend") == "experimental_qnn_htp_encoder_onnx_cpu_decoder")
        require(contract.getJSONObject("target").getString("compiled_context_sha256") == manifest.getString("compiled_context_sha256"))
        require(contract.getJSONObject("encoder").getJSONObject("input").getJSONArray("shape").toString() == "[1,128,1200]")
        require(contract.getJSONObject("encoder").getJSONObject("output").getJSONArray("shape").toString() == "[1,156,1024]")
        val expectedWeight = when(schemaVersion) {
            3 -> FP16_PACKED
            2 -> SHARED
            else -> "decoder_init.onnx.data"
        }
        require(contract.getJSONObject("decoder_init").getString("external_weights") == expectedWeight)
        require(contract.getJSONObject("decoder_step").getString("external_weights") ==
            (if (schemaVersion == 3) FP16_PACKED else if (schemaVersion == 2) SHARED else "decoder_step.onnx.data"))
        if (schemaVersion == 3) {
            val storage = contract.getJSONObject("weight_storage")
            require(storage.getString("mode") == "shared_fp16_storage_with_fp32_cast")
            require(storage.getString("shared_file") == FP16_PACKED)
            require(storage.getBoolean("both_graphs_use_shared_file"))
            require(storage.getString("weights_precision") == "FP16_STORAGE_FP32_COMPUTE")
        }
        if (schemaVersion == 2) {
            val storage = contract.getJSONObject("weight_storage")
            require(storage.getString("mode") == "shared_external_data" && storage.getString("shared_file") == SHARED)
            require(storage.getBoolean("both_graphs_use_shared_file"))
            require(storage.getString("weights_precision") == "FP32")
        }
    }

    private fun localState(dir: File, schemaVersion: Int): NpuArchiveState {
        if (!dir.exists()) return NpuArchiveState.NOT_INSTALLED
        val manifest = File(dir, MANIFEST)
        val ready = File(dir, READY)
        if (!manifest.isFile || !ready.isFile) return NpuArchiveState.INCOMPLETE
        return runCatching {
            val source = JSONObject(manifest.readText())
            require(JSONObject(ready.readText()).getString("manifest_sha256") == sha(manifest))
            require(expected(source, schemaVersion).all { File(dir, it.name).length() == it.bytes })
            checkContract(JSONObject(File(dir, CONTRACT).readText()), source, schemaVersion)
            NpuArchiveState.READY
        }.getOrDefault(NpuArchiveState.INCOMPLETE)
    }

    fun state(context: Context): NpuArchiveState {
        val fp16 = localState(fp16Folder(context), 3)
        val compact = localState(compactFolder(context), 2)
        val legacy = localState(legacyFolder(context), 1)
        val saved = visibleArchiveReady(context)
        return when {
            fp16 == NpuArchiveState.READY || compact == NpuArchiveState.READY || legacy == NpuArchiveState.READY || saved -> NpuArchiveState.READY
            fp16 == NpuArchiveState.INCOMPLETE || compact == NpuArchiveState.INCOMPLETE || legacy == NpuArchiveState.INCOMPLETE -> NpuArchiveState.INCOMPLETE
            else -> NpuArchiveState.NOT_INSTALLED
        }
    }

    /** Verify every installed model hash and all decoder sidecar names before loading ORT. */
    fun verifiedFolder(context: Context): File {
        require(state(context) == NpuArchiveState.READY) { "Qwen model package is not installed" }
        ensureLocalRuntime(context)
        val dir = folder(context)
        val version = when(dir) {
            fp16Folder(context) -> 3
            compactFolder(context) -> 2
            else -> 1
        }
        val manifest = JSONObject(File(dir, MANIFEST).readText())
        expected(manifest, version).forEach { asset ->
            require(ModelFileOps.verify(File(dir, asset.name), asset.bytes, "SHA-256", asset.sha256)) {
                "Qwen artifact failed SHA-256: ${asset.name}"
            }
        }
        checkContract(JSONObject(File(dir, CONTRACT).readText()), manifest, version)
        return dir
    }

    /** New installs require Stage I schema v3 from HF main; no unverified v1/v2 fallback downloads. */
    fun installOrRepair(context: Context, cancelled: () -> Boolean, progress: (Int, Long, Long, String) -> Unit) {
        val dir = fp16Folder(context)
        // A completed visible archive IS an installed model. Never copy gigabytes back
        // to private storage merely because the user pressed Download or Upgrade.
        if (visibleState(context, "stage-i-fp16-0.6b", 3) == NpuArchiveState.READY) {
            progress(100, 1, 1, "Qwen already saved in Whisperapp/Models; no download or private copy needed")
            return
        }
        if (localState(dir, 3) == NpuArchiveState.READY) {
            val location = saveInstalledToWhisperappModels(context, cancelled, progress)
            progress(100, 1, 1, "Qwen already downloaded; saved in $location")
            return
        }
        require(dir.mkdirs() || dir.isDirectory)
        val manifestFile = File(dir, MANIFEST)
        val incomingManifest = File(dir, "$MANIFEST.part")
        // Keep the verified manifest on resumption: a moving branch cannot change expected hashes midway.
        if (!manifestFile.isFile) {
            ModelFileOps.downloadResumable(url(MANIFEST), incomingManifest, isCancelled = cancelled) { _, _ -> }
            val inspected = JSONObject(incomingManifest.readText())
            expected(inspected, 3)
            require(incomingManifest.renameTo(manifestFile)) { "Cannot activate compact Qwen manifest" }
        }
        val modelManifest = JSONObject(manifestFile.readText())
        val assets = expected(modelManifest, 3)
        val total = assets.sumOf { it.bytes }
        var done = 0L
        for (asset in assets) {
            if (cancelled()) throw CancellationException("Qwen download paused")
            val final = File(dir, asset.name)
            if (!ModelFileOps.verify(final, asset.bytes, "SHA-256", asset.sha256)) {
                val partial = File(dir, "${asset.name}.part")
                if (partial.length() > asset.bytes) require(partial.delete())
                val base = done
                ModelFileOps.downloadResumable(url(asset.name), partial, asset.bytes, cancelled) { current, _ ->
                    val downloaded = base + current
                    progress(((downloaded * 100L) / total).toInt().coerceIn(0, 100), downloaded, total, "Downloading ${asset.name}")
                }
                require(ModelFileOps.verify(partial, asset.bytes, "SHA-256", asset.sha256)) {
                    "Qwen FP16 package SHA-256 mismatch: ${asset.name}"
                }
                if (final.exists()) require(final.delete()) { "Cannot replace corrupt ${asset.name}" }
                require(partial.renameTo(final)) { "Cannot commit ${asset.name}" }
            }
            done += asset.bytes
            progress(((done * 100L) / total).toInt().coerceIn(0, 100), done, total, "Verified ${asset.name}")
        }
        val contractPart = File(dir, "$CONTRACT.part")
        val contractFile = File(dir, CONTRACT)
        if (!contractFile.isFile) {
            ModelFileOps.downloadResumable(url(CONTRACT), contractPart, isCancelled = cancelled) { _, _ -> }
            checkContract(JSONObject(contractPart.readText()), modelManifest, 3)
            require(contractPart.renameTo(contractFile))
        }
        checkContract(JSONObject(contractFile.readText()), modelManifest, 3)
        require(assets.all { ModelFileOps.verify(File(dir, it.name), it.bytes, "SHA-256", it.sha256) })
        val markerPart = File(dir, "$READY.part")
        markerPart.writeText(JSONObject().put("manifest_sha256", sha(manifestFile))
            .put("qualification", "experimental_android_unverified").toString())
        require(markerPart.renameTo(File(dir, READY)))
        progress(99, total, total, "Archiving Qwen into the selected Whisperapp/Models workspace")
        val location = saveInstalledToWhisperappModels(context, cancelled, progress)
        progress(100, total, total, "Qwen verified and saved in $location")
    }

    /** Explicit user action after compact is ready; never remove the original during upgrade. */
    fun removeLegacyAfterUpgrade(context: Context): Boolean {
        require(verifiedFolder(context) == fp16Folder(context) || verifiedFolder(context) == compactFolder(context)) {
            "Verify newer Qwen SHA-256 before deleting legacy weights"
        }
        val old = legacyFolder(context)
        return (!old.exists() || old.deleteRecursively()) && !old.exists()
    }

    /** Explicit storage recovery: remove ONLY the verified older v2 package after v3 is ready. */
    fun removeCompactAfterFp16(context: Context): Boolean {
        require(verifiedFolder(context) == fp16Folder(context)) {
            "Verify every Stage I FP16 asset SHA-256 before deleting v2"
        }
        val old = compactFolder(context)
        return (!old.exists() || old.deleteRecursively()) && !old.exists()
    }

    fun select(context: Context) {
        require(state(context) == NpuArchiveState.READY) { "Install Qwen hybrid package first" }
        // Worker uses the shared SAF archive directly whenever its descriptor bridge works.
        // Selection must NOT materialize a second permanent multi-GB working copy.
        AppPrefs.setTranscriptionEngine(context, "qwen3_hybrid")
        AppPrefs.setModelPath(context, null)
    }

    /** Recovery for a killed :qwen process. Only exact Builder-owned temporary names are removed.
     * Never removes QNN contexts, model weights, recordings, or package receipts.
     */
    fun clearTemporaryTestFiles(context: Context): Boolean {
        // Clean legacy cacheDir wrappers left by v6.5.27 and earlier.
        val scratch = File(context.cacheDir, "qwen3-hybrid-epcontext")
        var clean = (!scratch.exists() || scratch.deleteRecursively()) && !scratch.exists()
        // v6.5.32: these are exact Qwen-owned scratch directories. Called only
        // after the worker has closed, or by the explicit inactive-session button.
        for (temporary in listOf(sharedCache(context), transientCache(context))) {
            if (temporary.exists() && (!temporary.deleteRecursively() || temporary.exists())) clean = false
        }
        // v6.5.28's wrapper is adjacent to the context binary so ORT can resolve a
        // basename-only ep_cache_context. Check every supported package version.
        for (stage in supportedStages) {
            // Exact file only; support both old and migrated installation paths.
            for (base in listOf(PERSISTENT_ROOT, PREVIOUS_ROOT)) {
                val wrapper = File(context.filesDir, "$base/$stage/$EP_CONTEXT_WRAPPER_NAME")
                val deleted = (!wrapper.exists() || wrapper.delete()) && !wrapper.exists()
                if (!deleted) clean = false
            }
        }
        return clean
    }

    fun delete(context: Context): Boolean {
        val dirs = supportedStages.flatMap { stage ->
            listOf(File(context.filesDir, "$PERSISTENT_ROOT/$stage"), File(context.filesDir, "$PREVIOUS_ROOT/$stage"))
        }
        // Only the explicit Delete Model command may delete model weights.
        val deleted = dirs.map { (!it.exists() || it.deleteRecursively()) && !it.exists() }.all { it }
        if (deleted && AppPrefs.getTranscriptionEngine(context) == "qwen3_hybrid") {
            AppPrefs.setTranscriptionEngine(context, "whisper")
            AppPrefs.setModelPath(context, null)
        }
        return deleted
    }
}
