package com.example.whisperrecorder.qwen

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import java.util.UUID

/** Public TEST FIXTURE ONLY, not microphone audio or user-recording embeddings. */
internal object QwenLabSyntheticFeatureExport {
    private const val BYTES = 156 * 1024 * 4
    fun save(context: Context, fp32le: ByteArray): String {
        require(fp32le.size == BYTES)
        val filename = "qwen-synthetic-156x1024-${UUID.randomUUID().toString().take(8)}.f32le"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SFSU/Whisperapp/Verification/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Cannot create synthetic feature export")
        var committed = false
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(fp32le) }
                ?: error("Cannot write synthetic feature export")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, done, null, null) == 1)
            committed = true
            return filename
        } finally {
            if (!committed) runCatching { resolver.delete(uri, null, null) }
        }
    }
}
