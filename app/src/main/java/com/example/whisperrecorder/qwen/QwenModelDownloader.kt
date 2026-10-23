package com.example.whisperrecorder.qwen

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

/**
 * User-initiated Qwen 0.6B downloads. No production directories, secrets, adb or background
 * downloading. HTTPS only (including redirects), bound file sizes, SHA-256 and atomic commit.
 * Files already imported into this app are reused when they match the pinned official Q8 model.
 */
object QwenModelDownloader {
    private const val DECODER_URL =
        "https://huggingface.co/ggml-org/Qwen3-ASR-0.6B-GGUF/resolve/928ab95/Qwen3-ASR-0.6B-Q8_0.gguf"
    // ggml-org Q8_0 model from the phone-tested Builder family. This GGUF is the decoder;
    // do NOT download mmproj because QNN supplies the 156x1024 audio features instead.
    const val DECODER_BYTES = 804_749_248L
    const val DECODER_SHA256 =
        "bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971"

    fun decoder(context: Context) = File(context.filesDir, "models/local-asr/qwen3-asr-0.6b-q8/Qwen3-ASR-0.6B-Q8_0.gguf")

    // Large-model SHA-256 must not run on every Compose model-card recomposition.
    // Recheck the content after a process restart or any file length/mtime change.
    @Volatile private var lastVerifiedDecoder: String? = null
    fun officialDecoderPresent(context: Context): Boolean {
        val file = decoder(context)
        if (!file.isFile || file.length() != DECODER_BYTES) return false
        val stamp = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        if (stamp == lastVerifiedDecoder) return true
        val verified = sha256(file).equals(DECODER_SHA256, ignoreCase = true)
        if (verified) lastVerifiedDecoder = stamp
        return verified
    }

    fun downloadMissing(context: Context, cancelled: () -> Boolean, status: (String) -> Unit) {
        ensureActive(cancelled)
        if (QwenEncoderAssets.valid(context)) {
            status("QNN encoder already verified in app storage; skipping encoder download.")
        } else {
            status("Downloading QNN encoder assets from Hugging Face (about 391 MB)...")
            QwenEncoderAssets.downloadOfficial(context, cancelled, status)
        }
        ensureActive(cancelled)
        if (officialDecoderPresent(context)) {
            status("Official Q8 GGUF already verified in app storage; no decoder download needed.")
        } else {
            status("Downloading official 805 MB Q8 GGUF decoder from Hugging Face...")
            val target = decoder(context)
            require(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
            downloadFile(DECODER_URL, target, DECODER_BYTES, DECODER_SHA256,
                "Q8 GGUF decoder", cancelled, status)
        }
        check(QwenEncoderAssets.valid(context) && officialDecoderPresent(context)) {
            "Model verification incomplete"
        }
    }

    /** For the small JSON manifest, whose exact length may change between upstream revisions. */
    fun downloadVariableSize(
        url: String, destination: File, maxBytes: Long,
        cancelled: () -> Boolean, status: (String) -> Unit
    ) {
        require(maxBytes in 1..3_000_000L && destination.parentFile?.isDirectory == true)
        val part = File(destination.parentFile, destination.name + ".part")
        require(!part.exists() || part.delete())
        var conn: HttpsURLConnection? = null
        try {
            conn = openHttps(url, cancelled)
            require(conn.responseCode == HttpURLConnection.HTTP_OK) { "Manifest fetch failed" }
            require(conn.contentLengthLong in -1L..maxBytes) { "Manifest response is oversized" }
            var count = 0L
            conn.inputStream.buffered().use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        ensureActive(cancelled)
                        val got = input.read(buffer)
                        if (got == -1) break
                        count += got
                        require(count <= maxBytes) { "Manifest exceeds size limit" }
                        output.write(buffer, 0, got)
                    }
                    output.fd.sync()
                }
            }
            ensureActive(cancelled)
            require(count in 1..maxBytes) { "Empty model manifest" }
            require(part.renameTo(destination)) { "Cannot commit encoder manifest" }
            status("Encoder manifest downloaded over HTTPS.")
        } finally {
            conn?.disconnect()
            if (part.exists()) part.delete()
        }
    }

    /**
     * Streaming installer. Writes only to <destination>.part, verifies, then renames.
     * The verified existing destination is not touched on failure/cancel. Ranges are deliberately
     * not reused because a stale HF revision or CDN response must never mix bytes across builds.
     */
    fun downloadFile(
        url: String, destination: File, expectedBytes: Long, expectedSha256: String,
        label: String, cancelled: () -> Boolean, status: (String) -> Unit
    ) {
        require(expectedBytes in 1..1_500_000_000L &&
            expectedSha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(destination.parentFile?.isDirectory == true)
        val part = File(destination.parentFile, destination.name + ".part")
        require(!part.exists() || part.delete()) { "Cannot reset interrupted Qwen download" }
        var connection: HttpsURLConnection? = null
        try {
            connection = openHttps(url, cancelled)
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Hugging Face returned HTTP ${connection.responseCode}"
            }
            val suppliedLength = connection.contentLengthLong
            require(suppliedLength == -1L || suppliedLength == expectedBytes) {
                "Unexpected HTTP content length for $label"
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            var nextUpdate = 0L
            connection.inputStream.buffered().use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        ensureActive(cancelled)
                        val count = input.read(buffer)
                        if (count == -1) break
                        copied += count
                        require(copied <= expectedBytes) { "$label exceeds expected length" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        if (copied >= nextUpdate) {
                            status("$label: ${copied / 1_000_000} / ${expectedBytes / 1_000_000} MB")
                            nextUpdate = copied + 16L * 1024 * 1024
                        }
                    }
                    output.fd.sync()
                }
            }
            ensureActive(cancelled)
            require(copied == expectedBytes) { "$label download truncated" }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual.equals(expectedSha256, ignoreCase = true)) {
                "$label SHA-256 mismatch; downloaded data rejected"
            }
            // All paths are app-private. Android/Linux rename replaces only this exact file.
            require(part.renameTo(destination)) { "Cannot commit verified $label" }
            status("$label: verified and installed.")
        } finally {
            connection?.disconnect()
            if (part.exists()) part.delete()
        }
    }

    private fun openHttps(url: String, cancelled: () -> Boolean): HttpsURLConnection {
        var current = URL(url)
        repeat(8) {
            ensureActive(cancelled)
            require(current.protocol == "https" && current.userInfo == null) {
                "Only authenticated HTTPS downloads are allowed"
            }
            val conn = current.openConnection() as HttpsURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.setRequestProperty("User-Agent", "WhisperRecorder/6.7.0")
            val code = try { conn.responseCode } catch (e: Exception) {
                conn.disconnect()
                throw e
            }
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                require(!location.isNullOrBlank()) { "Download redirect without Location" }
                val redirected = URL(current, location)
                conn.disconnect()
                require(redirected.protocol == "https" && redirected.userInfo == null) {
                    "Non-HTTPS model redirect rejected"
                }
                current = redirected
            } else {
                return conn
            }
        }
        error("Too many model download redirects")
    }

    internal fun ensureActive(cancelled: () -> Boolean) {
        if (cancelled()) throw java.util.concurrent.CancellationException("Model download cancelled")
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
