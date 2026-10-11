package com.example.whisperrecorder.npu

import android.content.Context
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.models.ModelFileOps
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Stage G v1 and Stage H compact v2 ONNX models need real adjacent external-data files.
 * Therefore Qwen uses one versioned app-private copy, not an additional SAF + staging duplicate.
 * It is NEVER an automatic first-run selection and never overwrites other ASR models.
 */
internal object Qwen3HybridPackageStore {
    const val DOWNLOAD_ID = "qwen3-0.6b-sm8850-v81-hybrid"
    const val DISPLAY_NAME = "Qwen3-ASR 0.6B · S26 NPU encoder + CPU decoder (experimental)"
    const val MODEL_KEY = "qwen3-hybrid://sm8850-v81-0.6b-stage-g"
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
    private const val COMPACT_SMOKE = "exact_cpu_onnx_decoder_token_smoke_pass"
    private const val MAX_PACKAGE_BYTES = 9_000_000_000L
    data class Asset(val name: String, val bytes: Long, val sha256: String)

    /** New installs target verified compact schema v2 at HF main. An already installed v1 is kept for rollback. */
    fun compactFolder(context: Context): File = File(context.filesDir, "qwen3-hybrid/stage-h-compact-0.6b")
    fun legacyFolder(context: Context): File = File(context.filesDir, "qwen3-hybrid/stage-g-0.6b")
    fun folder(context: Context): File = when {
        localState(compactFolder(context), 2) == NpuArchiveState.READY -> compactFolder(context)
        localState(legacyFolder(context), 1) == NpuArchiveState.READY -> legacyFolder(context)
        else -> compactFolder(context)
    }
    fun compactInstalled(context: Context): Boolean = localState(compactFolder(context), 2) == NpuArchiveState.READY
    fun legacyInstalled(context: Context): Boolean = localState(legacyFolder(context), 1) == NpuArchiveState.READY
    private fun url(name: String): String {
        require(name in COMPACT_WEIGHTS || name == MANIFEST || name == CONTRACT)
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
        if (schemaVersion == 2) {
            require(manifest.optString("decoder_weight_storage") == COMPACT_STORAGE)
            require(manifest.getJSONObject("decoder_smoke").optString("status") == COMPACT_SMOKE)
            require(manifest.optLong("saved_bytes") > 0L)
            require(manifest.optString("original_stage_g_manifest_sha256").matches(Regex("[0-9a-fA-F]{64}")))
        }
        val weights = if (schemaVersion == 2) COMPACT_WEIGHTS else LEGACY_WEIGHTS
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
        val expectedWeight = if (schemaVersion == 2) SHARED else "decoder_init.onnx.data"
        require(contract.getJSONObject("decoder_init").getString("external_weights") == expectedWeight)
        require(contract.getJSONObject("decoder_step").getString("external_weights") ==
            (if (schemaVersion == 2) SHARED else "decoder_step.onnx.data"))
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
        val compact = localState(compactFolder(context), 2)
        val legacy = localState(legacyFolder(context), 1)
        return when {
            compact == NpuArchiveState.READY || legacy == NpuArchiveState.READY -> NpuArchiveState.READY
            compact == NpuArchiveState.INCOMPLETE || legacy == NpuArchiveState.INCOMPLETE -> NpuArchiveState.INCOMPLETE
            else -> NpuArchiveState.NOT_INSTALLED
        }
    }

    /** Verify every installed model hash and all decoder sidecar names before loading ORT. */
    fun verifiedFolder(context: Context): File {
        require(state(context) == NpuArchiveState.READY) { "Qwen model package is not installed" }
        val dir = folder(context)
        val version = if (dir == compactFolder(context)) 2 else 1
        val manifest = JSONObject(File(dir, MANIFEST).readText())
        expected(manifest, version).forEach { asset ->
            require(ModelFileOps.verify(File(dir, asset.name), asset.bytes, "SHA-256", asset.sha256)) {
                "Qwen artifact failed SHA-256: ${asset.name}"
            }
        }
        checkContract(JSONObject(File(dir, CONTRACT).readText()), manifest, version)
        return dir
    }

    /** New installations require schema-v2 compact HF main; no implicit fallback to a 6 GB download. */
    fun installOrRepair(context: Context, cancelled: () -> Boolean, progress: (Int, Long, Long, String) -> Unit) {
        val dir = compactFolder(context)
        if (localState(dir, 2) == NpuArchiveState.READY) {
            progress(100, 1, 1, "Compact Qwen model already installed")
            return
        }
        require(dir.mkdirs() || dir.isDirectory)
        val manifestFile = File(dir, MANIFEST)
        val incomingManifest = File(dir, "$MANIFEST.part")
        // Keep the verified manifest on resumption: a moving branch cannot change expected hashes midway.
        if (!manifestFile.isFile) {
            ModelFileOps.downloadResumable(url(MANIFEST), incomingManifest, isCancelled = cancelled) { _, _ -> }
            val inspected = JSONObject(incomingManifest.readText())
            expected(inspected, 2)
            require(incomingManifest.renameTo(manifestFile)) { "Cannot activate compact Qwen manifest" }
        }
        val modelManifest = JSONObject(manifestFile.readText())
        val assets = expected(modelManifest, 2)
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
                    "Qwen compact SHA-256 mismatch: ${asset.name}"
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
            checkContract(JSONObject(contractPart.readText()), modelManifest, 2)
            require(contractPart.renameTo(contractFile))
        }
        checkContract(JSONObject(contractFile.readText()), modelManifest, 2)
        require(assets.all { ModelFileOps.verify(File(dir, it.name), it.bytes, "SHA-256", it.sha256) })
        val markerPart = File(dir, "$READY.part")
        markerPart.writeText(JSONObject().put("manifest_sha256", sha(manifestFile))
            .put("qualification", "experimental_android_unverified").toString())
        require(markerPart.renameTo(File(dir, READY)))
        progress(100, total, total, "Compact Qwen hybrid downloaded and verified")
    }

    /** Explicit user action after compact is ready; never remove the original during upgrade. */
    fun removeLegacyAfterUpgrade(context: Context): Boolean {
        require(verifiedFolder(context) == compactFolder(context)) {
            "Fully verify compact Qwen SHA-256 before deleting legacy weights"
        }
        val old = legacyFolder(context)
        return (!old.exists() || old.deleteRecursively()) && !old.exists()
    }

    fun select(context: Context) {
        require(state(context) == NpuArchiveState.READY) { "Install Qwen hybrid package first" }
        AppPrefs.setTranscriptionEngine(context, "qwen3_hybrid")
        AppPrefs.setModelPath(context, null)
    }

    /** Recovery for a killed :qwen process: only this backend's generated EPContext wrapper. */
    fun clearTemporaryTestFiles(context: Context): Boolean {
        val scratch = File(context.cacheDir, "qwen3-hybrid-epcontext")
        val deleted = !scratch.exists() || scratch.deleteRecursively()
        return deleted && !scratch.exists()
    }

    fun delete(context: Context): Boolean {
        val dirs = listOf(compactFolder(context), legacyFolder(context))
        val deleted = dirs.all { (!it.exists() || it.deleteRecursively()) && !it.exists() }
        if (deleted && AppPrefs.getTranscriptionEngine(context) == "qwen3_hybrid") {
            AppPrefs.setTranscriptionEngine(context, "whisper")
            AppPrefs.setModelPath(context, null)
        }
        return deleted
    }
}
