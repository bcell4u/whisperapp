package com.example.whisperrecorder.qwen

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.json.JSONObject

/** Lab-only private encoder import. Does not look in any production-model directories. */
internal object QwenLabEncoderAssets {
    private const val ROOT = "qwen-gguf-lab-encoder"
    private val required = linkedMapOf(
        "encoder-qnn-fp16.bin" to 1L..1_500_000_000L,
        "tokenizer.json" to 100_000L..30_000_000L,
        "golden-synthetic-pcm-12s-f32.raw" to 768_000L..768_000L,
        "golden-synthetic-mel-12s-f32.raw" to 614_400L..614_400L
    )
    private const val MANIFEST = "model-manifest.json"
    private const val HF_ROOT =
        "https://huggingface.co/drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81/resolve/main/"
    private val allowed = required.keys + MANIFEST

    fun directory(context: Context): File = File(context.filesDir, ROOT)

    /** Fast size-only UI check; cryptographic verification runs in the worker thread. */
    fun present(context: Context): Boolean {
        val folder = directory(context)
        return folder.isDirectory && required.all { (name, limit) ->
            val file = File(folder, name)
            file.isFile && file.length() in limit
        }
    }

    fun valid(context: Context): Boolean = runCatching { verify(directory(context)) }.isSuccess

    private fun verify(folder: File) {
        require(folder.isDirectory)
        for ((name, limit) in required) {
            val file = File(folder, name)
            require(file.isFile && file.length() in limit) { "Encoder package file missing or wrong size: $name" }
        }
        val manifestFile = File(folder, MANIFEST)
        if (manifestFile.isFile) {
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.optString("model_id") == "Qwen/Qwen3-ASR-0.6B" &&
                manifest.optString("validated_s26_model") == "SM-S942U" &&
                manifest.optString("variant") == "0.6B") { "Encoder manifest targets a different model/device" }
            val files = manifest.getJSONArray("files")
            for (name in required.keys) {
                val entry = (0 until files.length()).map { files.getJSONObject(it) }
                    .singleOrNull { it.getString("path") == name }
                    ?: error("Manifest lacks $name")
                val file = File(folder, name)
                require(entry.getLong("size_bytes") == file.length()) { "Size mismatch: $name" }
                val expected = entry.getString("sha256")
                require(expected.matches(Regex("[a-fA-F0-9]{64}")) && hash(file).equals(expected, true)) {
                    "SHA-256 mismatch: $name"
                }
            }
            require(manifest.getString("compiled_context_sha256").equals(hash(File(folder,
                "encoder-qnn-fp16.bin")), true)) { "QNN binary does not match package context" }
        }
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun receive(input: InputStream, target: File, max: Long) {
        var count = 0L
        FileOutputStream(target).use { out ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val got = input.read(buffer)
                if (got < 0) break
                count += got
                require(count <= max) { "Oversize selected encoder asset" }
                out.write(buffer, 0, got)
            }
            out.fd.sync()
        }
    }

    /** SAF tree picker for an unpacked stage folder. Caller runs this off the UI thread. */
    fun importFolder(context: Context, uri: Uri) {
        val root = requireNotNull(DocumentFile.fromTreeUri(context, uri)) { "Unusable encoder directory" }
        val assets = root.listFiles().filter { it.isFile }.associateBy { it.name }
        require(required.keys.all { assets[it] != null }) {
            "Choose the folder containing encoder-qnn-fp16.bin, tokenizer.json and the two golden fixtures"
        }
        importAtomically(context) { staging ->
            for (name in allowed) {
                val doc = assets[name] ?: continue
                val max = required[name]?.last ?: 3_000_000L
                requireNotNull(context.contentResolver.openInputStream(doc.uri)).use { input ->
                    receive(input, File(staging, name), max)
                }
            }
        }
    }

    /** ZIP picker accepts a full stage archive but extracts ONLY the four encoder inputs. */
    fun importZip(context: Context, uri: Uri) {
        importAtomically(context) { staging ->
            requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                ZipInputStream(stream.buffered()).use { zip ->
                    val seen = mutableSetOf<String>()
                    var entries = 0
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        require(++entries <= 64) { "Too many ZIP entries" }
                        // Only take safe single-file basenames; never evaluate paths from the archive.
                        val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                        if (!entry.isDirectory && name in allowed) {
                            require(seen.add(name)) { "Duplicate encoder file: $name" }
                            val max = required[name]?.last ?: 3_000_000L
                            require(entry.size <= max || entry.size < 0) { "Oversized encoder ZIP entry" }
                            receive(zip, File(staging, name), max)
                        }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /** Downloads only four necessary QNN assets + their manifest, not the ONNX decoder.
     * The remote manifest SHA-256 values protect every large asset; the manifest itself
     * arrives over HTTPS and should be pinned to a reviewed repository revision for
     * long-term releases. Old verified encoder files remain until staging is valid.
     */
    fun downloadOfficial(
        context: Context, cancelled: () -> Boolean, status: (String) -> Unit
    ) {
        importAtomically(context) { staging ->
            val manifestFile = File(staging, MANIFEST)
            // Small manifest gets bounded transfer. Its exact size can vary by revision.
            QwenLabModelDownloader.downloadVariableSize(
                HF_ROOT + MANIFEST, manifestFile, 3_000_000L, cancelled, status)
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.optString("model_id") == "Qwen/Qwen3-ASR-0.6B" &&
                manifest.optString("validated_s26_model") == "SM-S942U" &&
                manifest.optString("variant") == "0.6B") { "Encoder manifest is incompatible" }
            val files = manifest.getJSONArray("files")
            for ((name, range) in required) {
                QwenLabModelDownloader.ensureActive(cancelled)
                val item = (0 until files.length()).map { files.getJSONObject(it) }
                    .singleOrNull { it.optString("path") == name }
                    ?: error("Encoder manifest missing required model input")
                val expectedSize = item.getLong("size_bytes")
                val expectedSha = item.getString("sha256")
                require(expectedSize in range && expectedSha.matches(Regex("[a-fA-F0-9]{64}"))) {
                    "Encoder package size or digest invalid"
                }
                QwenLabModelDownloader.downloadFile(
                    HF_ROOT + name, File(staging, name), expectedSize, expectedSha,
                    name, cancelled, status)
            }
        }
    }

    private inline fun importAtomically(context: Context, copy: (File) -> Unit) {
        val dir = directory(context)
        val pending = File(context.filesDir, "$ROOT.pending")
        val previous = File(context.filesDir, "$ROOT.previous")
        // These three exact app-owned directories are the entire cleanup scope.
        require(!pending.exists() || pending.deleteRecursively())
        require(!previous.exists() || previous.deleteRecursively())
        require(pending.mkdirs())
        try {
            copy(pending)
            verify(pending)
            if (dir.exists()) require(dir.renameTo(previous))
            try {
                require(pending.renameTo(dir))
                if (previous.exists()) require(previous.deleteRecursively())
            } catch (error: Throwable) {
                if (!dir.exists() && previous.exists()) previous.renameTo(dir)
                throw error
            }
        } finally {
            if (pending.exists()) pending.deleteRecursively()
        }
    }
}
