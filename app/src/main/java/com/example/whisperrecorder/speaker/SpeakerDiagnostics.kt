package com.example.whisperrecorder.speaker

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/** Diagnostics are stored in private app storage; nothing is uploaded automatically. */
object SpeakerDiagnostics {
    private const val LOG_TAG = "WhisperSpeaker"
    private val requiredClasses = listOf(
        "com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization",
        "com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig",
        "com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig",
        "com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig"
    )
    private val requiredLibraries = listOf("libsherpa-onnx-jni.so", "libsherpa_ort.so")

    private fun reportFile(context: Context, recordingId: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(recordingId.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        return File(dir, "speaker-$hash.txt")
    }

    fun read(context: Context, recordingId: String): String? =
        runCatching { reportFile(context, recordingId).takeIf { it.isFile }?.readText(Charsets.UTF_8) }.getOrNull()

    fun clear(context: Context, recordingId: String) {
        runCatching { reportFile(context, recordingId).delete() }
    }

    fun capture(context: Context, recordingId: String, error: Throwable, stage: String = "unknown"): String {
        // Capture must never replace the original failure with a diagnostic collection failure.
        val report = runCatching { build(context, error, stage) }
            .getOrElse { failure ->
                "Speaker diagnostic collection failed: ${failure.javaClass.name}: ${failure.message}\n" +
                    SpeakerFailureText.exceptionChain(error) + "\n" + SpeakerFailureText.stackTrace(error)
            }
        runCatching { reportFile(context, recordingId).writeText(report, Charsets.UTF_8) }
            .onFailure { Log.e(LOG_TAG, "Could not save private speaker diagnostic report", it) }
        Log.e(LOG_TAG, "Offline speaker identification failure", error)
        return report
    }

    private fun build(context: Context, error: Throwable, stage: String): String = buildString {
        append("WhisperRecorder offline speaker diagnostics\n")
        append("Generated: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())).append('\n')
        val pkg = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        append("App: ").append(context.packageName).append(' ')
            .append(pkg?.versionName ?: "unknown").append(" (code ")
            .append(pkg?.longVersionCode ?: "unknown").append(")\n")
        append("Android: ").append(Build.VERSION.SDK_INT).append(" (release ").append(Build.VERSION.RELEASE).append(")\n")
        append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
        append("Supported ABIs: ").append(Build.SUPPORTED_ABIS.joinToString()).append("\n\n")
        append("Last processing stage: ").append(stage.take(160)).append("\n\n")
        append("Failure chain:\n").append(SpeakerFailureText.exceptionChain(error)).append('\n')
        append("Class resolution (without running static initializers):\n")
        for (name in requiredClasses) {
            val status = runCatching { Class.forName(name, false, context.classLoader) }
                .fold(onSuccess = { "FOUND" }, onFailure = {
                    "FAILED (${it.javaClass.name}: ${it.message ?: "no message"})"
                })
            append("  ").append(name).append(": ").append(status).append('\n')
        }
        append("\nNative libraries in installed APK(s):\n")
        val appInfo = context.applicationInfo
        val apkFiles = listOfNotNull(appInfo.sourceDir) + appInfo.splitSourceDirs.orEmpty().toList()
        for (name in requiredLibraries) {
            var found = false
            val archiveErrors = mutableListOf<String>()
            for (path in apkFiles) {
                runCatching {
                    ZipFile(path).use { zip ->
                        if (zip.getEntry("lib/arm64-v8a/$name") != null) found = true
                    }
                }.onFailure { archiveErrors.add(it.javaClass.simpleName) }
            }
            append("  ").append(name).append(": ")
                .append(if (found) "PRESENT" else "NOT FOUND")
            if (archiveErrors.isNotEmpty()) append(" (inspection errors: ${archiveErrors.joinToString()})")
            append('\n')
        }
        append("Native library directory present: ")
            .append(File(appInfo.nativeLibraryDir).isDirectory).append('\n')
        val (segmentation, embeddings) = SpeakerModels.paths(context)
        append("\nSpeaker model files (size only, no file paths):\n")
        append("  Segmentation: ").append(if (segmentation.isFile) "${segmentation.length()} bytes" else "MISSING").append('\n')
        append("  Embeddings: ").append(if (embeddings.isFile) "${embeddings.length()} bytes" else "MISSING").append('\n')
        append("\nStack trace:\n").append(SpeakerFailureText.stackTrace(error)).append('\n')
        append("\nPrivacy: This report contains technical errors/device information but no transcript or audio data. ")
        append("An exception message could contain a local file path; review before sharing.\n")
    }
}
