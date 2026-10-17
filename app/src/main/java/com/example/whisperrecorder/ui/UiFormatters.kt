package com.example.whisperrecorder.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val INVALID_FILENAME_CHARS = Regex("[\\/:*?\"<>|]")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun audioFormatLabel(format: String): String = when (format) {
    "opus" -> "OGG / Opus"
    "wav" -> "WAV / PCM"
    else -> "M4A / AAC"
}

fun modelDownloadProgressText(done: Long, total: Long, percent: Int, status: String): String {
    fun mb(value: Long): String = if (value <= 0L) "0 MB" else "%.1f MB".format(Locale.US, value / 1_000_000.0)
    return when {
        total > 0L -> "${mb(done)} of ${mb(total)} • ${percent.coerceIn(0, 100)}%"
        done > 0L -> mb(done)
        status.isNotBlank() -> status
        else -> "Starting…"
    }
}

fun sanitizeRecordingTitle(value: String): String =
    value.trim().replace(INVALID_FILENAME_CHARS, "-").take(120)

fun formatDateTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)

fun formatTranscriptionDuration(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    if (safe < 60_000L) return if (safe < 10_000L) "%.1fs".format(Locale.US, safe / 1000.0) else "${safe / 1000L}s"
    val totalSeconds = safe / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"
}

private fun twoDigits(value: Long): String = if (value < 10L) "0$value" else value.toString()

fun formatElapsed(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val seconds = total % 60L
    val minutes = (total / 60L) % 60L
    val hours = total / 3600L
    return if (hours > 0L) {
        "${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}"
    } else {
        "${twoDigits(total / 60L)}:${twoDigits(seconds)}"
    }
}
