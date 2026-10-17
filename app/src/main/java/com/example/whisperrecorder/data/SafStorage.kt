package com.example.whisperrecorder.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File

object SafStorage {

    data class TreeDocument(val uri: Uri, val name: String, val size: Long, val mime: String)

    fun getOrCreateDirectory(context: Context, treeUri: Uri, name: String): Uri {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        return getOrCreateChildDirectory(context, treeUri, parent, name)
    }

    fun findChildDirectory(context: Context, treeUri: Uri, parentUri: Uri, name: String): Uri? {
        val resolver = context.contentResolver
        val parentDocId = DocumentsContract.getDocumentId(parentUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                if (nameCol >= 0 && mimeCol >= 0 && cursor.getString(nameCol) == name &&
                    cursor.getString(mimeCol) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idCol))
                }
            }
        }
        return null
    }

    fun findDirectory(context: Context, treeUri: Uri, name: String): Uri? {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        return findChildDirectory(context, treeUri, parent, name)
    }

    fun getOrCreateChildDirectory(context: Context, treeUri: Uri, parentUri: Uri, name: String): Uri {
        findChildDirectory(context, treeUri, parentUri, name)?.let { return it }
        return requireNotNull(
            DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
            )
        ) { "Unable to create $name folder" }
    }

    fun listDirectory(context: Context, treeUri: Uri, directoryUri: Uri): List<TreeDocument> {
        val resolver = context.contentResolver
        val docId = DocumentsContract.getDocumentId(directoryUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val out = mutableListOf<TreeDocument>()
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val child = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idCol))
                out += TreeDocument(
                    child,
                    if (nameCol >= 0) cursor.getString(nameCol) ?: "" else "",
                    if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L,
                    if (mimeCol >= 0) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                )
            }
        }
        return out
    }

    fun copyFileToDirectory(context: Context, source: File, treeUri: Uri, directoryUri: Uri, mime: String, displayName: String): Uri {
        val resolver = context.contentResolver
        val tempName = "$displayName.saving"
        val existingDocuments = listDirectory(context, treeUri, directoryUri)
        existingDocuments.firstOrNull { it.name == tempName }?.let { deleteDocument(context, it.uri) }
        val existing = existingDocuments.firstOrNull { it.name == displayName }
        val temp = requireNotNull(
            DocumentsContract.createDocument(resolver, directoryUri, mime, tempName)
        ) { "Unable to create destination document" }
        try {
            requireNotNull(resolver.openOutputStream(temp, "w")) { "Unable to open destination" }.use { output ->
                source.inputStream().use { input -> input.copyTo(output, 1024 * 1024) }
            }
            existing?.let { require(deleteDocument(context, it.uri)) { "Unable to replace existing saved model" } }
            return requireNotNull(DocumentsContract.renameDocument(resolver, temp, displayName)) {
                "Unable to finalize saved model"
            }
        } catch (t: Throwable) {
            deleteDocument(context, temp)
            throw t
        }
    }
    fun createDocument(context: Context, treeUri: Uri, mime: String, displayName: String): Uri {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return createDocumentInDirectory(context, parent, mime, displayName)
    }

    fun createDocumentInDirectory(context: Context, directoryUri: Uri, mime: String, displayName: String): Uri =
        requireNotNull(DocumentsContract.createDocument(context.contentResolver, directoryUri, mime, displayName)) {
            "Unable to create destination document"
        }

    fun createDirectoryInDirectory(context: Context, directoryUri: Uri, displayName: String): Uri =
        requireNotNull(
            DocumentsContract.createDocument(
                context.contentResolver, directoryUri, DocumentsContract.Document.MIME_TYPE_DIR, displayName
            )
        ) { "Unable to create $displayName folder" }

    fun writeTextToDirectory(context: Context, directoryUri: Uri, displayName: String, text: String): Uri {
        val dest = createDocumentInDirectory(context, directoryUri, "text/plain", displayName)
        writeText(context, dest, text)
        return dest
    }

    private fun writeText(context: Context, destination: Uri, text: String) {
        val out = requireNotNull(context.contentResolver.openOutputStream(destination, "w")) {
            "Unable to open destination"
        }
        out.bufferedWriter().use { it.write(text) }
    }

    /** Returns whether a referenced document is definitely missing without reading its payload.
     * Unknown/provider-temporary failures are preserved as available so reconciliation never
     * destroys Library metadata merely because a provider is temporarily inaccessible. */
    fun documentExists(context: Context, uri: Uri): Boolean {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.path?.let { File(it) }?.isFile == true
        }
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            ) ?: return true
            cursor.use { it.moveToFirst() }
        } catch (t: Throwable) {
            var cause: Throwable? = t
            var definitelyMissing = false
            while (cause != null && !definitelyMissing) {
                val message = cause.message
                definitelyMissing = cause is java.io.FileNotFoundException ||
                    message?.contains("missing file", ignoreCase = true) == true
                cause = cause.cause
            }
            !definitelyMissing
        }
    }

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    } catch (_: Exception) {
        null
    }

    fun extensionForDocument(context: Context, uri: Uri, fallbackMime: String? = null): String {
        val name = displayName(context, uri).orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
        val ext = name.substringAfterLast('.', "").lowercase().substringBefore('?').takeIf { it.isNotBlank() && it.length <= 8 }
        if (ext != null) return ext
        return when (fallbackMime?.lowercase()) {
            "audio/ogg", "audio/opus" -> "ogg"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/aac" -> "aac"
            else -> "m4a"
        }
    }

    fun renameDocument(context: Context, uri: Uri, displayName: String): Uri? {
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        return try {
            DocumentsContract.renameDocument(context.contentResolver, uri, displayName)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    data class RecordingDeleteOutcome(val removed: Boolean, val status: String)

    /** Idempotent deletion for Library items. A missing document is already deleted, not an error. */
    fun deleteRecordingDocument(context: Context, uri: Uri): RecordingDeleteOutcome {
        return try {
            val deleted = if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
            when {
                deleted -> RecordingDeleteOutcome(true, "deleted")
                !documentExists(context, uri) -> RecordingDeleteOutcome(true, "already-missing")
                else -> RecordingDeleteOutcome(false, "provider-refused")
            }
        } catch (e: java.io.FileNotFoundException) {
            RecordingDeleteOutcome(true, "already-missing")
        } catch (e: Exception) {
            RecordingDeleteOutcome(false, "error-${e.javaClass.simpleName}")
        }
    }

    fun deleteDocument(context: Context, uri: Uri): Boolean = try {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } else {
            context.contentResolver.delete(uri, null, null) > 0
        }
    } catch (_: SecurityException) {
        false
    } catch (_: Exception) {
        false
    }

}
