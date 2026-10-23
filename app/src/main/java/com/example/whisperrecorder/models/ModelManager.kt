package com.example.whisperrecorder.models

import android.content.Context
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class BuiltInModel(
    val id: String,
    val name: String,
    val fileName: String,
    val url: String,
    val approxSize: String,
    val sha1: String
)

object ModelManager {
    private val ENGLISH_ONLY_NAME = Regex("(^|[._-])en([._-]|$)")
    val whisperLanguages: LinkedHashMap<String, String> = linkedMapOf(
        "auto" to "Auto-detect", "en" to "English", "zh" to "Chinese", "de" to "German", "es" to "Spanish",
        "ru" to "Russian", "ko" to "Korean", "fr" to "French", "ja" to "Japanese", "pt" to "Portuguese",
        "tr" to "Turkish", "pl" to "Polish", "ca" to "Catalan", "nl" to "Dutch", "ar" to "Arabic",
        "sv" to "Swedish", "it" to "Italian", "id" to "Indonesian", "hi" to "Hindi", "fi" to "Finnish",
        "vi" to "Vietnamese", "he" to "Hebrew", "uk" to "Ukrainian", "el" to "Greek", "ms" to "Malay",
        "cs" to "Czech", "ro" to "Romanian", "da" to "Danish", "hu" to "Hungarian", "ta" to "Tamil",
        "no" to "Norwegian", "th" to "Thai", "ur" to "Urdu", "hr" to "Croatian", "bg" to "Bulgarian",
        "lt" to "Lithuanian", "la" to "Latin", "mi" to "Maori", "ml" to "Malayalam", "cy" to "Welsh",
        "sk" to "Slovak", "te" to "Telugu", "fa" to "Persian", "lv" to "Latvian", "bn" to "Bengali",
        "sr" to "Serbian", "az" to "Azerbaijani", "sl" to "Slovenian", "kn" to "Kannada", "et" to "Estonian",
        "mk" to "Macedonian", "br" to "Breton", "eu" to "Basque", "is" to "Icelandic", "hy" to "Armenian",
        "ne" to "Nepali", "mn" to "Mongolian", "bs" to "Bosnian", "kk" to "Kazakh", "sq" to "Albanian",
        "sw" to "Swahili", "gl" to "Galician", "mr" to "Marathi", "pa" to "Punjabi", "si" to "Sinhala",
        "km" to "Khmer", "sn" to "Shona", "yo" to "Yoruba", "so" to "Somali", "af" to "Afrikaans",
        "oc" to "Occitan", "ka" to "Georgian", "be" to "Belarusian", "tg" to "Tajik", "sd" to "Sindhi",
        "gu" to "Gujarati", "am" to "Amharic", "yi" to "Yiddish", "lo" to "Lao", "uz" to "Uzbek",
        "fo" to "Faroese", "ht" to "Haitian Creole", "ps" to "Pashto", "tk" to "Turkmen", "nn" to "Nynorsk",
        "mt" to "Maltese", "sa" to "Sanskrit", "lb" to "Luxembourgish", "my" to "Myanmar", "bo" to "Tibetan",
        "tl" to "Tagalog", "mg" to "Malagasy", "as" to "Assamese", "tt" to "Tatar", "haw" to "Hawaiian",
        "ln" to "Lingala", "ha" to "Hausa", "ba" to "Bashkir", "jw" to "Javanese", "su" to "Sundanese"
    )

    val builtIns = listOf(
        BuiltInModel("large-v3-turbo-q5_0", "Large V3 Turbo Q5", "ggml-large-v3-turbo-q5_0.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin", "~547 MB", "e050f7970618a659205450ad97eb95a18d69c9ee"),
        BuiltInModel("small-q5_1", "Small Q5", "ggml-small-q5_1.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin", "~181 MB", "6fe57ddcfdd1c6b07cdcc73aaf620810ce5fc771"),
        BuiltInModel("base-q5_1", "Base Q5", "ggml-base-q5_1.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin", "~57 MB", "a3733eda680ef76256db5fc5dd9de8629e62c5e7"),
        BuiltInModel("tiny-q5_1", "Tiny Q5", "ggml-tiny-q5_1.bin", "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin", "~31 MB", "2827a03e495b1ed3048ef28a6a4620537db4ee51")
    )

    private val builtInByNormalizedName: Map<String, BuiltInModel> = builtIns.associateBy { normalizedModelId(it.fileName) }


    /**
     * Normalize a model filename to the catalog identity used by the UI. This keeps an installed
     * file such as ggml-small-q5_1.bin from appearing as a second, separate "small-q5_1" model.
     */
    private fun normalizedModelId(name: String): String = name
        .substringAfterLast('/')
        .removeSuffix(".bin")
        .removePrefix("ggml-")
        .lowercase()
        .replace('_', '-')

    fun builtInFor(file: File): BuiltInModel? {
        val normalized = normalizedModelId(file.name)
        return builtInByNormalizedName[normalized] ?: builtIns.firstOrNull { it.id == normalized }
    }

    fun hasPartialDownload(context: Context, model: BuiltInModel): Boolean =
        File(modelsDir(context), model.fileName + ".part").let { it.isFile && it.length() > 0L }

    fun displayName(file: File): String = builtInFor(file)?.name
        ?: file.name.removePrefix("ggml-").removeSuffix(".bin")

    fun isEnglishOnly(file: File): Boolean {
        val n = file.name.lowercase()
        return ENGLISH_ONLY_NAME.containsMatchIn(n)
    }

    fun isEnglishOnly(model: BuiltInModel): Boolean = model.id.contains(".en-") || model.fileName.contains(".en-")

    fun languageSupportLabel(file: File): String =
        if (isEnglishOnly(file)) "English only • Optimized for English" else "Multilingual • ${whisperLanguages.size - 1} languages + auto-detect"

    fun languageSupportLabel(model: BuiltInModel): String =
        if (isEnglishOnly(model)) "English only • Optimized for English" else "Multilingual • ${whisperLanguages.size - 1} languages + auto-detect"

    val multilingualBuiltIns: List<BuiltInModel> = builtIns.filterNot(::isEnglishOnly)

    private fun archiveToPersistentStorage(context: Context, source: File) {
        val (tree, dir) = ModelFileOps.persistentModelsDirectory(context) ?: return
        SafStorage.copyFileToDirectory(context, source, tree, dir, "application/octet-stream", source.name)
    }

    internal fun persistentBuiltInIds(documents: List<SafStorage.TreeDocument>): Set<String> = documents
        .asSequence()
        .filter { it.name.endsWith(".bin", true) }
        .mapNotNull { doc ->
            val normalized = normalizedModelId(doc.name)
            builtInByNormalizedName[normalized]?.id
        }
        .toSet()

    private fun isWhisperModelCandidate(name: String): Boolean =
        name.endsWith(".bin", ignoreCase = true) &&
            !name.startsWith("encoder-qnn-", ignoreCase = true)

    internal fun restorePersistentModels(context: Context, archive: ModelArchiveSnapshot?): List<File> {
        val docs = archive?.documents?.filter { isWhisperModelCandidate(it.name) } ?: return installed(context)
        val localDir = modelsDir(context)
        docs.forEach { doc ->
            val local = File(localDir, doc.name)
            val builtIn = builtInFor(local)
            val localValid = local.isFile && (doc.size <= 0L || local.length() == doc.size)
            if (!localValid) {
                ModelFileOps.restoreDocument(context, doc.uri, local) { restored ->
                    if (doc.size > 0L) require(restored.length() == doc.size) { "Saved model ${doc.name} has the wrong size" }
                    if (builtIn != null) {
                        require(ModelFileOps.verify(restored, algorithm = "SHA-1", expectedDigest = builtIn.sha1)) {
                            "Saved model ${doc.name} failed checksum verification"
                        }
                    }
                }
            }
        }
        return installed(context)
    }

    fun modelsDir(context: Context): File = File(context.filesDir, "models").apply { mkdirs() }
    fun installed(context: Context): List<File> = modelsDir(context).listFiles()
        ?.filter { file ->
            file.isFile && isWhisperModelCandidate(file.name)
        }?.sortedBy { it.name } ?: emptyList()

    suspend fun download(
        context: Context,
        model: BuiltInModel,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit,
        onVerifying: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
        onArchiving: () -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): File = withContext(Dispatchers.IO) {
        val target = File(modelsDir(context), model.fileName)
        val part = File(modelsDir(context), model.fileName + ".part")
        val total = ModelFileOps.downloadResumable(
            url = model.url,
            partial = part,
            isCancelled = isCancelled
        ) { done, resolvedTotal ->
            if (resolvedTotal > 0L) {
                onProgress(((done * 100L) / resolvedTotal).toInt().coerceIn(0, 100), done, resolvedTotal)
            } else {
                onProgress(0, done, 0L)
            }
        }
        require(part.length() > 1_000_000) { "Downloaded model is unexpectedly small" }
        onVerifying(part.length(), total.takeIf { it > 0L } ?: part.length())
        val actual = ModelFileOps.digest(part, "SHA-1", isCancelled)
        if (!actual.equals(model.sha1, ignoreCase = true)) {
            part.delete()
            error("Model checksum verification failed")
        }
        if (target.exists()) require(target.delete()) { "Unable to replace existing model" }
        require(part.renameTo(target)) { "Unable to finalize downloaded model" }
        onArchiving()
        archiveToPersistentStorage(context, target)
        AppPrefs.setTranscriptionEngine(context, "whisper")
        AppPrefs.setModelPath(context, target.absolutePath)
        target
    }

    fun select(context: Context, file: File) {
        require(file.parentFile?.canonicalFile == modelsDir(context).canonicalFile && file.isFile) { "Invalid model file" }
        AppPrefs.setTranscriptionEngine(context, "whisper")
        AppPrefs.setModelPath(context, file.absolutePath)
    }

    fun delete(context: Context, file: File): Boolean {
        val dir = modelsDir(context).canonicalFile
        val candidate = file.canonicalFile
        require(candidate.parentFile == dir) { "Only app-managed models can be removed" }
        if (!candidate.exists()) return true
        val wasSelected = AppPrefs.getModelPath(context)?.let { File(it).canonicalFile == candidate } == true
        val deleted = candidate.delete()
        if (deleted) {
            runCatching { ModelFileOps.deletePersistentDocuments(context, candidate.name) }
            if (wasSelected) AppPrefs.setModelPath(context, null)
        }
        return deleted
    }


}
