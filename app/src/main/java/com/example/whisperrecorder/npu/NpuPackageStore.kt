package com.example.whisperrecorder.npu

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.DocumentsContract
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.models.ModelFileOps
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant

internal data class NpuPackageProgress(
    val phase: String,
    val path: String? = null,
    val fileIndex: Int = 0,
    val fileCount: Int = 0,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val message: String = ""
)

internal data class NpuFileVerification(
    val path: String,
    val exists: Boolean,
    val expectedBytes: Long?,
    val actualBytes: Long?,
    val expectedSha256: String?,
    val actualSha256: String?,
    val ok: Boolean,
    val message: String = ""
)

internal data class NpuPackageVerification(
    val ok: Boolean,
    val requiredFiles: List<String>,
    val files: List<NpuFileVerification>,
    val message: String
)

internal enum class NpuArchiveState { NOT_INSTALLED, INCOMPLETE, READY }

internal data class NpuArchiveArtifact(
    val relativePath: String,
    val uri: Uri,
    val size: Long
)

internal data class NpuRuntimeArchiveInfo(
    val verifiedAt: String,
    val manifestSha256: String,
    val artifacts: Map<String, NpuArchiveArtifact>
)

internal data class NpuOptionalArtifactInfo(
    val artifact: NpuArchiveArtifact,
    val downloaded: Boolean,
    val acquisitionMs: Long
)

/**
 * Manifest-driven installer for the precompiled Parakeet SM8850/V81 package.
 *
 * Durable package bytes live under the existing user-selected Whisperapp/Models workspace so they
 * survive APK uninstall/reinstall. App-private storage contains only one resumable transport file at
 * a time and may be deleted at any time without losing the archived package.
 */
internal object NpuPackageStore {
    const val DOWNLOAD_ID = "npu-parakeet-tdt-0.6b-v3-sm8850-v81-fp16"
    const val PACKAGE_ID = "parakeet-tdt-0.6b-v3-sm8850-v81-fp16"
    const val DISPLAY_NAME = "Parakeet TDT 0.6B v3 · Qualcomm NPU FP16"
    const val REPO_ID = "drinnird/parakeet-tdt-0.6b-v3-sm8850-v81-fp16"
    const val REVISION = "2291826cfb600fba90af386c98d8b2d9a325256b"
    const val TRANSCRIPTION_MODEL_KEY = "npu://$PACKAGE_ID@$REVISION"
    val ARCHIVE_DISPLAY_PATH: String get() = "Whisperapp/Models/$PACKAGE_ID/$REVISION"

    // Phase 6D accepted linked decoder. Keep it optional until a dedicated package-cleanup
    // release promotes it into the canonical required-file set and retires fallback assets.
    const val LINKED_DECODER_PATH = "experimental/linked_decoder.bin"
    const val LINKED_DECODER_BYTES = 26_198_016L
    const val LINKED_DECODER_SHA256 = "055430b77197d5bbe4de573385ff19a3dc16ecafe74919079ea5af4f87c8f9cc"

    private const val SCHEMA = "whisperrecorder.android-npu-package"
    private const val SCHEMA_VERSION = "1-draft"
    private const val PACKAGE_STATUS = "controller_validated_runtime_pending"
    private const val LAYOUT = "whisperrecorder-huggingface-deployment-v1"
    private const val BUFFER_BYTES = 1024 * 1024
    private const val READY_FILE = "READY.json"
    private const val SAFETY_RESERVE_BYTES = 64L * 1024L * 1024L

    private val allowedDeviceModels = setOf("SM-S942U", "SM-S942U1")

    data class ArchiveLocation(val treeUri: Uri, val revisionDir: Uri)

    fun remoteUrl(relativePath: String): String {
        val safe = NpuPackageFormat.safeRelativePath(relativePath)
        return "https://huggingface.co/$REPO_ID/resolve/$REVISION/$safe?download=true"
    }

    fun archiveState(context: Context): NpuArchiveState {
        val archive = existingArchive(context) ?: return NpuArchiveState.NOT_INSTALLED
        val children = runCatching { SafStorage.listDirectory(context, archive.treeUri, archive.revisionDir) }
            .getOrElse { return NpuArchiveState.INCOMPLETE }
        return if (children.any { it.name == READY_FILE && it.mime != DocumentsContract.Document.MIME_TYPE_DIR }) {
            NpuArchiveState.READY
        } else {
            NpuArchiveState.INCOMPLETE
        }
    }

    fun existingArchive(context: Context): ArchiveLocation? {
        val tree = AppPrefs.getSaveTree(context) ?: return null
        val models = runCatching { WorkspaceStorage.modelsDirectory(context, tree) }.getOrNull() ?: return null
        val packageDir = SafStorage.findChildDirectory(context, tree, models, PACKAGE_ID) ?: return null
        val revisionDir = SafStorage.findChildDirectory(context, tree, packageDir, REVISION) ?: return null
        return ArchiveLocation(tree, revisionDir)
    }

    fun ensureArchiveLocation(context: Context): ArchiveLocation {
        val tree = requireNotNull(AppPrefs.getSaveTree(context)) {
            "Choose the WhisperRecorder save folder before downloading the NPU package"
        }
        val models = WorkspaceStorage.modelsDirectory(context, tree)
        val packageDir = SafStorage.getOrCreateChildDirectory(context, tree, models, PACKAGE_ID)
        val revisionDir = SafStorage.getOrCreateChildDirectory(context, tree, packageDir, REVISION)
        return ArchiveLocation(tree, revisionDir)
    }

    /** Small read of a verified archived text/config artifact. Large model payloads are never exposed through this helper. */
    fun readSmallTextArtifact(context: Context, relativePath: String, maxBytes: Long = 2L * 1024L * 1024L): String? {
        val archive = existingArchive(context) ?: return null
        val safe = NpuPackageFormat.safeRelativePath(relativePath)
        val document = findDocument(context, archive, safe) ?: return null
        require(document.size <= maxBytes) { "Diagnostic text artifact is unexpectedly large: $safe (${document.size} bytes)" }
        return requireNotNull(context.contentResolver.openInputStream(document.uri)) { "Unable to read $safe" }
            .buffered()
            .reader(Charsets.UTF_8)
            .use { it.readText() }
    }

    /**
     * Lightweight runtime readiness check. Full payload hashing belongs to install/repair and explicit
     * diagnostics; normal runtime startup validates the READY trust record, manifest identity and exact
     * sizes of the five runtime artifacts without rereading ~1.3 GB.
     */
    fun runtimeArchiveInfo(context: Context): NpuRuntimeArchiveInfo {
        val archive = requireNotNull(existingArchive(context)) { "NPU package archive is not installed" }
        val ready = JSONObject(readRequiredText(context, archive, READY_FILE))
        require(ready.optString("state") == "READY") { "NPU package READY marker is invalid" }
        require(ready.optString("package_id") == PACKAGE_ID) { "READY package id mismatch" }
        require(ready.optString("repo_id") == REPO_ID) { "READY repository id mismatch" }
        require(ready.optString("revision") == REVISION) { "READY revision mismatch" }

        val manifestText = readRequiredText(context, archive, "manifest.json")
        val manifest = JSONObject(manifestText).also(::validateManifest)
        val actualManifestSha = sha256(manifestText.toByteArray(Charsets.UTF_8))
        val readyManifestSha = ready.optString("manifest_sha256")
        require(readyManifestSha.isNotBlank() && readyManifestSha.equals(actualManifestSha, ignoreCase = true)) {
            "READY manifest fingerprint no longer matches manifest.json"
        }

        val required = requiredFiles(manifest).toSet()
        val expected = linkedMapOf<String, Long>().apply {
            NpuGraphContracts.ALL.forEach { put(it.artifactPath, it.artifactBytes) }
            put("model/predictor_embedding.f32", 20_974_080L)
        }
        expected.keys.forEach { path -> require(path in required) { "Runtime artifact is not declared required: $path" } }

        val artifacts = linkedMapOf<String, NpuArchiveArtifact>()
        expected.forEach { (path, bytes) ->
            val doc = requireNotNull(findDocument(context, archive, path)) { "Archived runtime artifact is missing: $path" }
            require(doc.size == bytes) { "Archived runtime artifact size mismatch for $path: expected=$bytes actual=${doc.size}" }
            artifacts[path] = NpuArchiveArtifact(path, doc.uri, doc.size)
        }
        return NpuRuntimeArchiveInfo(
            verifiedAt = ready.optString("verified_at"),
            manifestSha256 = actualManifestSha,
            artifacts = artifacts
        )
    }

    /**
     * Phase 6D optional linked-decoder asset. It remains outside manifest required_files so a losing
     * experiment does not permanently enlarge the production package contract. The immutable pinned
     * repository checksum is verified before the durable SAF archive is updated.
     */
    fun ensureLinkedDecoderArtifact(
        context: Context,
        isCancelled: () -> Boolean = { false }
    ): NpuOptionalArtifactInfo {
        val app = context.applicationContext
        val archive = requireNotNull(existingArchive(app)) { "NPU package archive is not installed" }
        val started = android.os.SystemClock.elapsedRealtime()

        val checksums = readSmallTextArtifact(app, "checksums.sha256", 4L * 1024L * 1024L)
            ?.let(NpuPackageFormat::parseChecksums)
            .orEmpty()
        checksums[LINKED_DECODER_PATH]?.let { declared ->
            require(declared.equals(LINKED_DECODER_SHA256, ignoreCase = true)) {
                "Pinned linked decoder checksum disagrees with checksums.sha256"
            }
        }

        val existing = findDocument(app, archive, LINKED_DECODER_PATH)
        if (existing != null && archiveDocumentMatches(
                app, existing, LINKED_DECODER_BYTES, LINKED_DECODER_SHA256, isCancelled
            )
        ) {
            return NpuOptionalArtifactInfo(
                NpuArchiveArtifact(LINKED_DECODER_PATH, existing.uri, existing.size),
                downloaded = false,
                acquisitionMs = android.os.SystemClock.elapsedRealtime() - started
            )
        }

        val partial = File(app.filesDir, "npu-download/$REVISION/$LINKED_DECODER_PATH.part")
        partial.parentFile?.mkdirs()
        if (partial.isFile && partial.length() == LINKED_DECODER_BYTES &&
            !ModelFileOps.verify(partial, LINKED_DECODER_BYTES, "SHA-256", LINKED_DECODER_SHA256)
        ) {
            require(partial.delete()) { "Unable to restart corrupt linked decoder partial" }
        }
        ensureAppPrivateFreeSpace(app, (LINKED_DECODER_BYTES - partial.length()).coerceAtLeast(0L))
        ModelFileOps.downloadResumable(
            url = remoteUrl(LINKED_DECODER_PATH),
            partial = partial,
            expectedBytes = LINKED_DECODER_BYTES,
            isCancelled = isCancelled
        ) { _, _ -> }
        require(ModelFileOps.verify(partial, LINKED_DECODER_BYTES, "SHA-256", LINKED_DECODER_SHA256)) {
            "SHA-256 or size verification failed for $LINKED_DECODER_PATH"
        }
        archiveLocalFile(app, archive, LINKED_DECODER_PATH, partial)
        require(partial.delete() || !partial.exists()) { "Unable to delete linked decoder download staging file" }

        val archived = requireNotNull(findDocument(app, archive, LINKED_DECODER_PATH)) {
            "$LINKED_DECODER_PATH disappeared after archive write"
        }
        require(archiveDocumentMatches(app, archived, LINKED_DECODER_BYTES, LINKED_DECODER_SHA256, isCancelled)) {
            "Archived linked decoder failed post-write verification"
        }
        return NpuOptionalArtifactInfo(
            NpuArchiveArtifact(LINKED_DECODER_PATH, archived.uri, archived.size),
            downloaded = true,
            acquisitionMs = android.os.SystemClock.elapsedRealtime() - started
        )
    }

    /** Fetches manifest/checksums first, then downloads or repairs only required package files. */
    fun installOrRepair(
        context: Context,
        isCancelled: () -> Boolean,
        onProgress: (NpuPackageProgress) -> Unit
    ): NpuPackageVerification {
        val app = context.applicationContext
        val archive = ensureArchiveLocation(app)
        val stagingRoot = File(app.filesDir, "npu-download/$REVISION").apply { mkdirs() }

        val manifestFile = fetchBootstrap(
            relativePath = "manifest.json",
            destination = File(stagingRoot, "manifest.json"),
            isCancelled = isCancelled
        ) { done, total ->
            onProgress(NpuPackageProgress("manifest", "manifest.json", bytesDone = done, bytesTotal = total, message = "Downloading manifest.json…"))
        }
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        validateManifest(manifest)

        val checksumsFile = fetchBootstrap(
            relativePath = "checksums.sha256",
            destination = File(stagingRoot, "checksums.sha256"),
            isCancelled = isCancelled
        ) { done, total ->
            onProgress(NpuPackageProgress("manifest", "checksums.sha256", bytesDone = done, bytesTotal = total, message = "Downloading checksums.sha256…"))
        }
        val checksums = NpuPackageFormat.parseChecksums(checksumsFile.readText(Charsets.US_ASCII))
        require(checksums.isNotEmpty()) { "checksums.sha256 contained no usable entries" }
        checksums["manifest.json"]?.let { expected ->
            require(ModelFileOps.digest(manifestFile, "SHA-256", isCancelled).equals(expected, ignoreCase = true)) {
                "Downloaded manifest.json failed SHA-256 verification"
            }
        }

        val required = requiredFiles(manifest)
        require("manifest.json" in required) { "Package required_files does not include manifest.json" }
        require("checksums.sha256" in required) { "Package required_files does not include checksums.sha256" }
        val ignoredPrefixes = ignoredPrefixes(manifest)
        val sizeHints = collectSizeHints(manifest)

        // Network/bootstrap errors above leave an existing READY package untouched. From this point
        // forward a repair is mutating package contents, so invalidate readiness first.
        removeReadyMarker(app, archive)
        archiveLocalFile(app, archive, "manifest.json", manifestFile)
        archiveLocalFile(app, archive, "checksums.sha256", checksumsFile)

        val totalKnownBytes = required.sumOf { sizeHints[it] ?: 0L }
        var completedKnownBytes = required
            .filter { it == "manifest.json" || it == "checksums.sha256" }
            .sumOf { sizeHints[it] ?: 0L }

        required.forEachIndexed { index, relativePath ->
            if (isCancelled()) throw CancellationException("NPU package download cancelled")
            val safe = NpuPackageFormat.safeRelativePath(relativePath)
            if (safe == "manifest.json" || safe == "checksums.sha256") return@forEachIndexed
            require(ignoredPrefixes.none { safe.startsWith(it) }) {
                "Normal Android install unexpectedly requested ignored artifact: $safe"
            }

            val expectedSha = requireNotNull(checksums[safe]) { "Missing SHA-256 entry for required file $safe" }
            val expectedBytes = sizeHints[safe]
            val existing = findDocument(app, archive, safe)
            if (existing != null && archiveDocumentMatches(app, existing, expectedBytes, expectedSha, isCancelled)) {
                completedKnownBytes += expectedBytes ?: existing.size
                onProgress(NpuPackageProgress(
                    phase = "download",
                    path = safe,
                    fileIndex = index + 1,
                    fileCount = required.size,
                    bytesDone = completedKnownBytes,
                    bytesTotal = totalKnownBytes,
                    message = "Already verified ${safe.substringAfterLast('/')}"
                ))
                return@forEachIndexed
            }

            val partial = File(stagingRoot, "$safe.part")
            partial.parentFile?.mkdirs()
            if (partial.isFile && runCatching {
                    ModelFileOps.verify(partial, expectedBytes, "SHA-256", expectedSha)
                }.getOrDefault(false)
            ) {
                onProgress(NpuPackageProgress("archive", safe, index + 1, required.size, completedKnownBytes, totalKnownBytes, "Saving $safe to Whisperapp/Models…"))
                archiveLocalFile(app, archive, safe, partial)
                val archivedBytes = expectedBytes ?: partial.length()
                partial.delete()
                completedKnownBytes += archivedBytes
                return@forEachIndexed
            }
            // A full-length partial with a bad hash cannot be resumed from EOF; restart only that file.
            if (expectedBytes != null && partial.isFile && partial.length() == expectedBytes) {
                require(partial.delete()) { "Unable to restart corrupt partial file for $safe" }
            }

            ensureAppPrivateFreeSpace(app, ((expectedBytes ?: 0L) - partial.length()).coerceAtLeast(0L))
            ModelFileOps.downloadResumable(
                url = remoteUrl(safe),
                partial = partial,
                expectedBytes = expectedBytes,
                isCancelled = isCancelled
            ) { done, _ ->
                onProgress(NpuPackageProgress(
                    phase = "download",
                    path = safe,
                    fileIndex = index + 1,
                    fileCount = required.size,
                    bytesDone = completedKnownBytes + done,
                    bytesTotal = totalKnownBytes,
                    message = "Downloading ${safe.substringAfterLast('/')}…"
                ))
            }

            onProgress(NpuPackageProgress("verify", safe, index + 1, required.size, completedKnownBytes, totalKnownBytes, "Verifying ${safe.substringAfterLast('/')}…"))
            require(ModelFileOps.verify(partial, expectedBytes, "SHA-256", expectedSha)) {
                "SHA-256 or size verification failed for $safe"
            }
            onProgress(NpuPackageProgress("archive", safe, index + 1, required.size, completedKnownBytes, totalKnownBytes, "Saving $safe to Whisperapp/Models…"))
            archiveLocalFile(app, archive, safe, partial)
            val archivedBytes = expectedBytes ?: partial.length()
            partial.delete()
            completedKnownBytes += archivedBytes
        }

        onProgress(NpuPackageProgress("verify", message = "Re-verifying archived NPU package…"))
        val verification = verifyArchive(app, isCancelled, onProgress)
        require(verification.ok) { verification.message }
        writeReadyMarker(app, archive, ModelFileOps.digest(manifestFile, "SHA-256"))
        stagingRoot.deleteRecursively()
        onProgress(NpuPackageProgress("ready", bytesDone = totalKnownBytes, bytesTotal = totalKnownBytes, message = "NPU package ready in Whisperapp/Models"))
        return verification
    }

    /** Offline verification of the durable archive. No network access is required. */
    fun verifyArchive(
        context: Context,
        isCancelled: () -> Boolean = { false },
        onProgress: (NpuPackageProgress) -> Unit = {}
    ): NpuPackageVerification {
        val archive = existingArchive(context)
            ?: return NpuPackageVerification(false, emptyList(), emptyList(), "NPU package archive is not installed")

        val manifest = runCatching {
            JSONObject(readRequiredText(context, archive, "manifest.json")).also(::validateManifest)
        }.getOrElse {
            return NpuPackageVerification(false, emptyList(), emptyList(), "Manifest validation failed: ${it.message}")
        }
        val checksums = runCatching {
            NpuPackageFormat.parseChecksums(readRequiredText(context, archive, "checksums.sha256"))
        }.getOrElse {
            return NpuPackageVerification(false, emptyList(), emptyList(), "checksums.sha256 missing or invalid: ${it.message}")
        }
        val required = runCatching { requiredFiles(manifest) }.getOrElse {
            return NpuPackageVerification(false, emptyList(), emptyList(), "required_files invalid: ${it.message}")
        }
        val sizeHints = collectSizeHints(manifest)
        val results = ArrayList<NpuFileVerification>(required.size)

        required.forEachIndexed { index, path ->
            if (isCancelled()) throw CancellationException("NPU package verification cancelled")
            val safe = NpuPackageFormat.safeRelativePath(path)
            onProgress(NpuPackageProgress("verify", safe, index + 1, required.size, message = "Verifying $safe…"))
            val doc = findDocument(context, archive, safe)
            val expectedSha = if (safe == "checksums.sha256") null else checksums[safe]
            val expectedBytes = sizeHints[safe]
            if (safe != "checksums.sha256" && expectedSha == null) {
                results += NpuFileVerification(safe, doc != null, expectedBytes, doc?.size, null, null, false, "Missing checksum entry")
                return@forEachIndexed
            }
            if (doc == null) {
                results += NpuFileVerification(safe, false, expectedBytes, null, expectedSha, null, false, "Missing")
                return@forEachIndexed
            }
            val actualBytes = doc.size
            val sizeOk = expectedBytes == null || actualBytes == expectedBytes
            val actualSha = if (expectedSha != null) digestUri(context, doc.uri, isCancelled) else null
            val shaOk = expectedSha == null || expectedSha.equals(actualSha, ignoreCase = true)
            results += NpuFileVerification(
                path = safe,
                exists = true,
                expectedBytes = expectedBytes,
                actualBytes = actualBytes,
                expectedSha256 = expectedSha,
                actualSha256 = actualSha,
                ok = sizeOk && shaOk,
                message = when {
                    !sizeOk -> "Size mismatch"
                    !shaOk -> "SHA-256 mismatch"
                    else -> "PASS"
                }
            )
        }

        val ok = results.all { it.ok }
        return NpuPackageVerification(
            ok = ok,
            requiredFiles = required,
            files = results,
            message = if (ok) "All required NPU package files verified" else "One or more NPU package files failed verification"
        )
    }

    /** Offline full verification that also restores READY when every required file is valid. */
    fun verifyAndMarkReady(
        context: Context,
        isCancelled: () -> Boolean = { false },
        onProgress: (NpuPackageProgress) -> Unit = {}
    ): NpuPackageVerification {
        val result = verifyArchive(context, isCancelled, onProgress)
        if (result.ok) {
            val archive = requireNotNull(existingArchive(context)) { "NPU package archive disappeared" }
            val manifest = requireNotNull(findDocument(context, archive, "manifest.json")) { "manifest.json disappeared" }
            val manifestSha = digestUri(context, manifest.uri, isCancelled)
            writeReadyMarker(context, archive, manifestSha)
        }
        return result
    }

    fun selectForTranscription(context: Context) {
        require(archiveState(context) == NpuArchiveState.READY) { "NPU package is not ready" }
        // Validate the lightweight READY/manifest/size contract before persisting selection.
        runtimeArchiveInfo(context)
        AppPrefs.setTranscriptionEngine(context, "parakeet_npu")
        AppPrefs.setModelPath(context, null)
    }

    fun deleteArchive(context: Context): Boolean {
        val tree = AppPrefs.getSaveTree(context) ?: return true
        val models = runCatching { WorkspaceStorage.modelsDirectory(context, tree) }.getOrNull() ?: return true
        val packageDir = SafStorage.findChildDirectory(context, tree, models, PACKAGE_ID) ?: return true
        val deleted = SafStorage.deleteDocument(context, packageDir)
        if (deleted) {
            // Do not remove the sibling Omi runtime staging directory.
            File(context.filesDir, "npu-download/$REVISION").deleteRecursively()
            File(context.filesDir, "npu-runtime/$REVISION").deleteRecursively()
        }
        if (deleted && AppPrefs.getTranscriptionEngine(context) == "parakeet_npu") {
            if (!AppPrefs.hasExplicitTranscriptionEngine(context)) {
                AppPrefs.resetAutomaticTranscriptionSelection(context)
            } else {
                AppPrefs.setTranscriptionEngine(context, "whisper")
                AppPrefs.setModelPath(context, null)
            }
        }
        return deleted
    }

    private fun validateManifest(manifest: JSONObject) {
        require(manifest.optString("schema") == SCHEMA) { "Unsupported package schema ${manifest.optString("schema")}" }
        require(manifest.optString("schema_version") == SCHEMA_VERSION) { "Unsupported package schema version ${manifest.optString("schema_version")}" }
        require(manifest.optString("package_status") == PACKAGE_STATUS) { "Unexpected package status ${manifest.optString("package_status")}" }
        require(manifest.optString("precision").equals("float16", ignoreCase = true)) { "Package precision ${manifest.optString("precision")} is unsupported" }
        val pending = manifest.optJSONArray("pending_graphs")
        require(pending == null || pending.length() == 0) { "Package still has pending neural graphs" }

        val distribution = manifest.requireObject("distribution")
        require(distribution.optString("provider") == "huggingface") { "Package distribution provider is not huggingface" }
        require(distribution.optString("layout") == LAYOUT) { "Unsupported package layout ${distribution.optString("layout")}" }
        val install = distribution.requireObject("android_install")
        require(install.optBoolean("manifest_first", false)) { "Package does not declare manifest-first installation" }

        val target = manifest.requireObject("target")
        require(target.optString("soc").equals("SM8850", ignoreCase = true)) { "Package targets ${target.optString("soc")}, not SM8850" }
        require(target.optString("htp_arch").equals("V81", ignoreCase = true)) { "Package targets ${target.optString("htp_arch")}, not V81" }
        require(target.optString("abi") == "arm64-v8a") { "Package ABI ${target.optString("abi")} is unsupported" }
        require(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) { "Device does not expose arm64-v8a ABI" }

        val declaredModels = target.optJSONArray("android_models")
        if (declaredModels != null && declaredModels.length() > 0) {
            val accepted = (0 until declaredModels.length()).map { declaredModels.optString(it) }.filter { it.isNotBlank() }.toSet()
            require(Build.MODEL in accepted || Build.MODEL in allowedDeviceModels) {
                "Device ${Build.MODEL} is not declared compatible with this package"
            }
        } else {
            require(Build.MODEL in allowedDeviceModels) { "Device ${Build.MODEL} is not in the validated SM8850 package family" }
        }
    }

    private fun requiredFiles(manifest: JSONObject): List<String> {
        val array = manifest.requireObject("distribution").requireObject("android_install").getJSONArray("required_files")
        val result = ArrayList<String>(array.length())
        val normalized = HashSet<String>()
        for (i in 0 until array.length()) {
            val path = NpuPackageFormat.safeRelativePath(array.getString(i))
            require(normalized.add(path)) { "Duplicate required path after normalization: $path" }
            result += path
        }
        return result
    }

    private fun ignoredPrefixes(manifest: JSONObject): List<String> {
        val array = manifest.requireObject("distribution").requireObject("android_install").optJSONArray("ignore_prefixes") ?: return emptyList()
        return (0 until array.length()).map { index ->
            val raw = array.getString(index)
            require(raw.endsWith('/')) { "Ignored prefix must end with '/': $raw" }
            NpuPackageFormat.safeRelativePath(raw.dropLast(1)) + "/"
        }
    }

    private fun collectSizeHints(root: Any?): Map<String, Long> {
        val out = linkedMapOf<String, Long>()
        fun walk(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val path = value.optString("path").takeIf { it.isNotBlank() }
                        ?: value.optString("relative_path").takeIf { it.isNotBlank() }
                    if (path != null && value.has("size_bytes")) {
                        val size = value.optLong("size_bytes", -1L)
                        if (size >= 0L) runCatching { out[NpuPackageFormat.safeRelativePath(path)] = size }
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) walk(value.opt(keys.next()))
                }
                is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i))
            }
        }
        walk(root)
        return out
    }

    private fun fetchBootstrap(
        relativePath: String,
        destination: File,
        isCancelled: () -> Boolean,
        onProgress: (Long, Long) -> Unit
    ): File {
        destination.parentFile?.mkdirs()
        // Bootstrap files are tiny. Restarting avoids ambiguous range-at-EOF responses after a crash.
        destination.delete()
        ModelFileOps.downloadResumable(remoteUrl(relativePath), destination, null, isCancelled, onProgress)
        return destination
    }

    private fun ensureAppPrivateFreeSpace(context: Context, requestedBytes: Long) {
        if (requestedBytes <= 0L) return
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        require(available >= requestedBytes + SAFETY_RESERVE_BYTES) {
            "Insufficient temporary storage: need ${requestedBytes + SAFETY_RESERVE_BYTES} bytes including safety reserve, have $available"
        }
    }

    private fun archiveLocalFile(context: Context, archive: ArchiveLocation, relativePath: String, local: File) {
        val safe = NpuPackageFormat.safeRelativePath(relativePath)
        val parent = ensureDirectoryPath(context, archive, safe.substringBeforeLast('/', ""))
        SafStorage.copyFileToDirectory(
            context = context,
            source = local,
            treeUri = archive.treeUri,
            directoryUri = parent,
            mime = mimeFor(safe.substringAfterLast('/')),
            displayName = safe.substringAfterLast('/')
        )
    }

    private fun removeReadyMarker(context: Context, archive: ArchiveLocation) {
        findDocument(context, archive, READY_FILE)?.let { marker ->
            require(SafStorage.deleteDocument(context, marker.uri)) { "Unable to invalidate stale NPU READY marker" }
        }
    }

    private fun writeReadyMarker(context: Context, archive: ArchiveLocation, manifestSha256: String) {
        val marker = JSONObject().apply {
            put("package_id", PACKAGE_ID)
            put("repo_id", REPO_ID)
            put("revision", REVISION)
            put("manifest_sha256", manifestSha256)
            put("verified_at", Instant.now().toString())
            put("state", "READY")
        }
        val local = File(context.cacheDir, "npu-ready-${System.nanoTime()}.json")
        try {
            local.writeText(marker.toString(2), Charsets.UTF_8)
            archiveLocalFile(context, archive, READY_FILE, local)
        } finally {
            local.delete()
        }
    }

    private fun readRequiredText(context: Context, archive: ArchiveLocation, relativePath: String): String {
        val doc = requireNotNull(findDocument(context, archive, relativePath)) { "$relativePath is missing" }
        return requireNotNull(context.contentResolver.openInputStream(doc.uri)) { "Unable to open $relativePath" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun archiveDocumentMatches(
        context: Context,
        doc: SafStorage.TreeDocument,
        expectedBytes: Long?,
        expectedSha: String,
        isCancelled: () -> Boolean
    ): Boolean {
        if (expectedBytes != null && doc.size != expectedBytes) return false
        return runCatching { digestUri(context, doc.uri, isCancelled).equals(expectedSha, ignoreCase = true) }.getOrDefault(false)
    }

    private fun digestUri(context: Context, uri: Uri, isCancelled: () -> Boolean): String {
        val digest = MessageDigest.getInstance("SHA-256")
        requireNotNull(context.contentResolver.openInputStream(uri)) { "Unable to read $uri" }
            .buffered(BUFFER_BYTES).use { input ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (true) {
                    if (isCancelled()) throw CancellationException("NPU package verification cancelled")
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
        val bytes = digest.digest()
        val digits = "0123456789abcdef"
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, value ->
            val unsigned = value.toInt() and 0xff
            chars[index * 2] = digits[unsigned ushr 4]
            chars[index * 2 + 1] = digits[unsigned and 0x0f]
        }
        return String(chars)
    }

    private fun ensureDirectoryPath(context: Context, archive: ArchiveLocation, relativeDirectory: String): Uri {
        var current = archive.revisionDir
        if (relativeDirectory.isBlank()) return current
        relativeDirectory.split('/').forEach { part ->
            current = SafStorage.getOrCreateChildDirectory(context, archive.treeUri, current, part)
        }
        return current
    }

    private fun findDocument(context: Context, archive: ArchiveLocation, relativePath: String): SafStorage.TreeDocument? {
        val safe = NpuPackageFormat.safeRelativePath(relativePath)
        val parts = safe.split('/')
        var directory = archive.revisionDir
        for (part in parts.dropLast(1)) {
            directory = SafStorage.findChildDirectory(context, archive.treeUri, directory, part) ?: return null
        }
        return SafStorage.listDirectory(context, archive.treeUri, directory).firstOrNull {
            it.name == parts.last() && it.mime != DocumentsContract.Document.MIME_TYPE_DIR
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val digits = "0123456789abcdef"
        val chars = CharArray(digest.size * 2)
        digest.forEachIndexed { index, value ->
            val unsigned = value.toInt() and 0xff
            chars[index * 2] = digits[unsigned ushr 4]
            chars[index * 2 + 1] = digits[unsigned and 0x0f]
        }
        return String(chars)
    }

    private fun mimeFor(name: String): String = when {
        name.endsWith(".json", ignoreCase = true) -> "application/json"
        name.endsWith(".txt", ignoreCase = true) || name.endsWith(".sha256", ignoreCase = true) -> "text/plain"
        else -> "application/octet-stream"
    }

    private fun JSONObject.requireObject(name: String): JSONObject = optJSONObject(name)
        ?: error("Manifest is missing object '$name'")
}
