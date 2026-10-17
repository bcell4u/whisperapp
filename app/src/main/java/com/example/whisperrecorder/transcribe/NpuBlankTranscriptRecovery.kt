package com.example.whisperrecorder.transcribe

/** Visible, time-localized notice used only when two successful NPU decodes both return blank. */
internal object NpuBlankTranscriptRecovery {
    fun marker(startMs: Long, endMs: Long): String {
        require(startMs >= 0L && endMs > startMs) { "Invalid untranscribed audio interval" }
        return "[No transcription returned for ${clock(startMs)}–${clock(endMs)}]"
    }

    private fun clock(ms: Long): String {
        val seconds = ms / 1_000L
        val hours = seconds / 3_600L
        val minutes = (seconds / 60L) % 60L
        val remainder = seconds % 60L
        return "%02d:%02d:%02d".format(java.util.Locale.US, hours, minutes, remainder)
    }
}
