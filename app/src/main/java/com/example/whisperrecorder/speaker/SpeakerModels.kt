package com.example.whisperrecorder.speaker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Models downloaded ONLY on user request; weights are never shipped inside the APK. */
object SpeakerModels {
    data class Asset(val name: String, val url: String, val sha256: String, val bytes: Long)
    // SHA-256 values from the public Hugging Face LFS model cards, verified after download.
    private val assets = listOf(
        Asset("segmentation-int8.onnx",
            "https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0/resolve/main/model.int8.onnx",
            "d582f4b4c6b48205de7e0643c57df0df5615a3c176189be3fc461e9d18827b5d", 1_540_514),
        Asset("eres2net.onnx",
            "https://huggingface.co/csukuangfj/speaker-embedding-models/resolve/main/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
            "1a331345f04805badbb495c775a6ddffcdd1a732567d5ec8b3d5749e3c7a5e4b", 39_600_000)
    )

    private fun directory(context: Context) = File(context.filesDir, "speaker-models").apply { mkdirs() }
    fun paths(context: Context): Pair<File, File> {
        val dir = directory(context)
        return File(dir, assets[0].name) to File(dir, assets[1].name)
    }
    fun installed(context: Context): Boolean = assets.all { model ->
        File(directory(context), model.name).let { it.isFile && it.length() > model.bytes / 2 }
    }
    fun verify(context: Context): Boolean = assets.all { model ->
        val file = File(directory(context), model.name)
        file.isFile && digest(file).equals(model.sha256, ignoreCase = true)
    }

    suspend fun install(context: Context, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        assets.forEachIndexed { index, model ->
            val destination = File(directory(context), model.name)
            if (destination.isFile && digest(destination) == model.sha256) return@forEachIndexed
            val temp = File(destination.parentFile, destination.name + ".part")
            temp.delete()
            try {
                onProgress("Downloading speaker model ${index + 1} of ${assets.size}…")
                val connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 25_000
                    readTimeout = 45_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept-Encoding", "identity")
                }
                try {
                    check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Model HTTP ${connection.responseCode}" }
                    // Content-Length can be omitted by mirrors, but cap transfers regardless.
                    connection.inputStream.use { stream ->
                        temp.outputStream().buffered().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                val n = stream.read(buffer)
                                if (n < 0) break
                                total += n
                                check(total <= model.bytes * 2) { "Model download exceeded expected size" }
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                } finally { connection.disconnect() }
                check(digest(temp) == model.sha256) { "Speaker model checksum mismatch: ${model.name}" }
                // No partial file becomes active, and no validated file is replaced on download failure.
                if (!temp.renameTo(destination)) {
                    // Android rename generally replaces atomically on the same filesystem.
                    throw java.io.IOException("Cannot install speaker model")
                }
            } finally { temp.delete() }
        }
        check(verify(context)) { "Speaker model installation verification failed" }
        onProgress("Speaker models verified and ready")
    }

    private fun digest(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                sha.update(buffer, 0, size)
            }
        }
        return sha.digest().joinToString("") { "%02x".format(it) }
    }
}
