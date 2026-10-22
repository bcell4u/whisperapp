package com.example.whisperrecorder.qwen

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import java.io.File
import java.util.UUID

/** Exports ONLY logits from the hardcoded synthetic verification fixture, never lecture logits. */
internal object QwenLabLogitExport {
    fun save(context: Context, syntheticCheckpoint: File): String {
        require(syntheticCheckpoint.isFile &&
            syntheticCheckpoint.length() == QwenLabLogitReplay.BYTES.toLong())
        val filename = "qwen-synthetic-prefill-logits-${UUID.randomUUID().toString().take(8)}.f32le"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SFSU/Whisperapp/Verification/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Cannot create synthetic logits export")
        var committed = false
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                syntheticCheckpoint.inputStream().use { input ->
                    check(input.copyTo(output) == QwenLabLogitReplay.BYTES.toLong())
                }
            } ?: error("Cannot write synthetic logits export")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, done, null, null) == 1)
            committed = true
            return filename
        } finally {
            if (!committed) runCatching { resolver.delete(uri, null, null) }
        }
    }
}
