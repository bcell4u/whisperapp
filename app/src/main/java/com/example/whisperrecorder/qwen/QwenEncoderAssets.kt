package com.example.whisperrecorder.qwen

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Two required Hugging Face QNN inputs, verified against their manifest before use. */
object QwenEncoderAssets {
    private const val ROOT = "qwen3-asr-0.6b-qnn"
    private const val REPOSITORY = "https://huggingface.co/drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81/resolve/main/"
    private const val MANIFEST = "model-manifest.json"
    private val files = linkedMapOf("encoder-qnn-fp16.bin" to 1L..1_500_000_000L,
                                    "tokenizer.json" to 100_000L..30_000_000L)
    @Volatile private var verifiedStamp: String? = null

    fun directory(context: Context): File = File(context.filesDir, ROOT)
    private fun stamp(directory: File): String = (files.keys + MANIFEST).joinToString("|") { name ->
        val f = File(directory, name)
        "${f.absolutePath}:${f.length()}:${f.lastModified()}"
    }
    fun valid(context: Context): Boolean {
        val folder = directory(context)
        val current = stamp(folder)
        if (current == verifiedStamp) return true
        val ok = runCatching { verify(folder) }.isSuccess
        if (ok) verifiedStamp = current
        return ok
    }
    private fun verify(folder: File) {
        val manifestFile = File(folder, MANIFEST)
        require(manifestFile.isFile && manifestFile.length() in 100L..3_000_000L)
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        require(manifest.getString("model_id") == "Qwen/Qwen3-ASR-0.6B")
        require(manifest.getString("validated_s26_model") == "SM-S942U")
        require(manifest.getString("variant") == "0.6B")
        val entries = manifest.getJSONArray("files")
        var contextHash: String? = null
        for ((name, range) in files) {
            val entry = (0 until entries.length()).asSequence().map { entries.getJSONObject(it) }
                .singleOrNull { it.optString("path") == name } ?: error("Missing encoder entry")
            val model = File(folder, name)
            val expected = entry.getString("sha256")
            require(model.isFile && model.length() in range &&
                    model.length() == entry.getLong("size_bytes") &&
                    expected.matches(Regex("[0-9a-fA-F]{64}")))
            val actual = sha256(model)
            require(actual.equals(expected, true)) { "Model checksum mismatch" }
            if (name == "encoder-qnn-fp16.bin") contextHash = actual
        }
        require(manifest.getString("compiled_context_sha256").equals(
            requireNotNull(contextHash), true))
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                val size = input.read(buf)
                if (size < 0) break
                digest.update(buf, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    /**
     * After process death between renaming the live directory to .previous and committing
     * .pending, restore a previously verified package. Never discard the only good encoder.
     * A verified new live package wins over stale .previous from a completed transaction.
     */
    private fun recoverInterruptedInstall(folder: File, backup: File) {
        if (!backup.exists()) return
        if (runCatching { verify(folder) }.isSuccess) {
            require(backup.deleteRecursively()) { "Cannot remove obsolete encoder backup" }
            verifiedStamp = stamp(folder)
            return
        }
        verify(backup)
        if (folder.exists()) require(folder.deleteRecursively()) { "Cannot remove broken encoder install" }
        require(backup.renameTo(folder)) { "Cannot restore previously verified encoder" }
        verifiedStamp = stamp(folder)
    }

    /** User-triggered download; a failed/cancelled staging folder never replaces valid assets. */
    fun downloadOfficial(context: Context, cancelled: () -> Boolean, status: (String) -> Unit) {
        val folder = directory(context)
        val staging = File(context.filesDir, "$ROOT.pending")
        val backup = File(context.filesDir, "$ROOT.previous")
        // Never delete .previous until interrupted-install recovery has examined it.
        recoverInterruptedInstall(folder, backup)
        require(!staging.exists() || staging.deleteRecursively())
        require(staging.mkdirs())
        try {
            QwenModelDownloader.downloadVariableSize(REPOSITORY + MANIFEST,
                File(staging, MANIFEST), 3_000_000, cancelled, status)
            val manifest = JSONObject(File(staging, MANIFEST).readText(Charsets.UTF_8))
            require(manifest.getString("model_id") == "Qwen/Qwen3-ASR-0.6B" &&
                    manifest.getString("validated_s26_model") == "SM-S942U" &&
                    manifest.getString("variant") == "0.6B")
            val entries = manifest.getJSONArray("files")
            for ((name, range) in files) {
                QwenModelDownloader.ensureActive(cancelled)
                val entry = (0 until entries.length()).asSequence().map { entries.getJSONObject(it) }
                    .singleOrNull { it.optString("path") == name } ?: error("Missing QNN file")
                val size = entry.getLong("size_bytes")
                val hash = entry.getString("sha256")
                require(size in range && hash.matches(Regex("[0-9a-fA-F]{64}")))
                QwenModelDownloader.downloadFile(REPOSITORY + name, File(staging, name),
                    size, hash, name, cancelled, status)
            }
            verify(staging)
            if (folder.exists()) require(folder.renameTo(backup))
            try {
                require(staging.renameTo(folder))
                if (backup.exists()) backup.deleteRecursively()
            } catch (t: Throwable) {
                if (!folder.exists() && backup.exists()) backup.renameTo(folder)
                throw t
            }
            verifiedStamp = stamp(folder)
        } finally {
            if (staging.exists()) staging.deleteRecursively()
        }
    }
}
