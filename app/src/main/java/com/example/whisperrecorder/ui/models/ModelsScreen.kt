@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.whisperrecorder.ui.models

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.advanced.LocalAsrModel
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.transcribe.AccelerationTier
import com.example.whisperrecorder.models.ModelDownloadRuntime
import com.example.whisperrecorder.models.ModelInventoryLoader
import com.example.whisperrecorder.models.ModelManager
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import com.example.whisperrecorder.npu.Qwen3HybridPackageStore
import com.example.whisperrecorder.qwen.Qwen3HybridClient
import com.example.whisperrecorder.transcribe.AsrEngine
import com.example.whisperrecorder.transcribe.TranscriptionRuntime
import com.example.whisperrecorder.ui.BackArrowButton
import com.example.whisperrecorder.ui.DownloadProgressBar
import com.example.whisperrecorder.ui.SectionLabel
import com.example.whisperrecorder.ui.modelDownloadProgressText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun ModelsScreen(
    onBack: () -> Unit,
    onStartModelDownload: (String) -> Unit,
    onCancelModelDownload: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transcriptionState by TranscriptionRuntime.state.collectAsState()
    val modelDownload by ModelDownloadRuntime.state.collectAsState()
    var installed by remember { mutableStateOf(emptyList<File>()) }
    var selectedEngine by remember { mutableStateOf(AppPrefs.getTranscriptionEngine(context)) }
    var selectedPath by remember { mutableStateOf(AppPrefs.getModelPath(context)) }
    var accelerationNotice by remember { mutableStateOf<AccelerationTier?>(null) }
    LaunchedEffect(selectedEngine, selectedPath) {
        if (selectedPath != null) accelerationNotice = AppPrefs.claimAccelerationNotice(context, selectedEngine)
    }
    var persistentModelIds by remember { mutableStateOf(emptySet<String>()) }
    var localInstalledIds by remember { mutableStateOf(emptySet<String>()) }
    var localPersistentIds by remember { mutableStateOf(emptySet<String>()) }
    var localRestoringId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var removeLocalTarget by remember { mutableStateOf<LocalAsrModel?>(null) }
    var npuArchiveState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var omiArchiveState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var qwenArchiveState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var qwenDeleteConfirm by remember { mutableStateOf(false) }
    var qwenLegacyDeleteConfirm by remember { mutableStateOf(false) }
    var qwenCompactDeleteConfirm by remember { mutableStateOf(false) }
    var npuDeleteConfirm by remember { mutableStateOf(false) }
    var omiDeleteConfirm by remember { mutableStateOf(false) }
    var npuActionRunning by remember { mutableStateOf(false) }

    suspend fun refreshModels(restoreWhisper: Boolean = true) {
        val inventory = runCatching { ModelInventoryLoader.load(context, restoreWhisper) }.getOrNull() ?: return
        installed = inventory.whisperInstalled
        persistentModelIds = inventory.whisperPersistentIds
        localInstalledIds = inventory.localInstalledIds
        localPersistentIds = inventory.localPersistentIds
        selectedEngine = inventory.selectedEngine
        selectedPath = inventory.selectedPath
    }

    // Keep model migration, verification and restoration off the Compose UI thread.
    // Both local model sections share the same mutually exclusive selection path.
    fun useLocalModel(model: LocalAsrModel, isInstalled: Boolean, isPersistent: Boolean) {
        if (localRestoringId != null || npuActionRunning || (!isInstalled && !isPersistent)) return
        scope.launch {
            localRestoringId = model.id
            if (!isInstalled) status = "Restoring ${model.name} from Whisperapp/Models…"
            try {
                val path = withContext(Dispatchers.IO) {
                    if (isInstalled) LocalAsrModelManager.select(context, model)
                    else check(LocalAsrModelManager.restoreAndSelect(context, model)) { "Restore did not complete" }
                    LocalAsrModelManager.modelFile(context, model).absolutePath
                }
                if (!isInstalled) localInstalledIds = localInstalledIds + model.id
                selectedEngine = model.engine.id
                selectedPath = path
                status = "${model.name} ready"
            } catch (t: Exception) {
                status = "Could not use ${model.name}: ${t.message ?: t.javaClass.simpleName}"
            } finally {
                localRestoringId = null
            }
        }
    }

    LaunchedEffect(Unit) { refreshModels() }
    LaunchedEffect(modelDownload.completedPath, modelDownload.error) {
        if (modelDownload.completedPath != null || modelDownload.error != null) {
            refreshModels(restoreWhisper = false)
        }
    }
    LaunchedEffect(modelDownload.status, modelDownload.error) {
        if (modelDownload.status.isNotBlank()) status = modelDownload.status
        modelDownload.error?.let { status = "Download failed: $it" }
    }
    LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        if (modelDownload.modelId == NpuPackageStore.DOWNLOAD_ID && !modelDownload.active) {
            npuArchiveState = withContext(Dispatchers.IO) { NpuPackageStore.archiveState(context) }
        } else if (modelDownload.modelId == null) {
            npuArchiveState = withContext(Dispatchers.IO) { NpuPackageStore.archiveState(context) }
        }
    }

    LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        if (modelDownload.modelId == null || (modelDownload.modelId == OmiNpuPackageStore.DOWNLOAD_ID && !modelDownload.active)) {
            omiArchiveState = withContext(Dispatchers.IO) {
                runCatching { OmiNpuPackageStore.archiveState(context) }.getOrDefault(NpuArchiveState.INCOMPLETE)
            }
        }
    }

    LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        if (modelDownload.modelId == null || (modelDownload.modelId == Qwen3HybridPackageStore.DOWNLOAD_ID && !modelDownload.active)) {
            qwenArchiveState = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.state(context) }
        }
    }

    if (qwenDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { qwenDeleteConfirm = false },
            title = { Text("Remove Qwen3 hybrid model?") },
            text = { Text("Remove only the experimental Qwen model files from private app storage? This does not touch your recordings, Parakeet, or Omi models.") },
            confirmButton = {
                Button(onClick = {
                    qwenDeleteConfirm = false
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val removed = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.delete(context) }
                            qwenArchiveState = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.state(context) }
                            refreshModels(false)
                            status = if (removed) "Qwen experimental model removed" else "Qwen files could not be fully removed"
                        } finally { npuActionRunning = false }
                    }
                }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) { Text("Remove") }
            }, dismissButton = { TextButton(onClick = { qwenDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    removeLocalTarget?.let { model ->
        AlertDialog(
            onDismissRequest = { removeLocalTarget = null },
            title = { Text("Remove ${model.name}?") },
            text = { Text("Remove the local and persistent ${model.name} model files from this device? If it is active, Whisper will become the transcription engine until another model is selected.") },
            confirmButton = {
                Button(
                    onClick = {
                        removeLocalTarget = null
                        scope.launch {
                            status = "Removing ${model.name}…"
                            val ok = withContext(Dispatchers.IO) {
                                runCatching { LocalAsrModelManager.remove(context, model) }.getOrDefault(false)
                            }
                            refreshModels(restoreWhisper = false)
                            status = if (ok) "Removed ${model.name}" else "Could not remove ${model.name} completely"
                        }
                    },
                    enabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removeLocalTarget = null }) { Text("Cancel") } }
        )
    }

    accelerationNotice?.let { tier -> AccelerationNoticeDialog(tier) { accelerationNotice = null } }

    Scaffold(topBar = { TopAppBar(title = { Text("Models & downloads") }, navigationIcon = { BackArrowButton(onBack) }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transcription models", style = MaterialTheme.typography.titleLarge)
                        Text("Select a transcription model. NPU models offer the best speed and battery efficiency. GPU models provide good speed but use more battery and generate more heat. CPU models are the slowest. Downloaded models are saved in Whisperapp/Models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (transcriptionState.active) Text("A transcription is currently running. Model removal is temporarily disabled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { SectionLabel("NPU") }
            item {
                ModelCatalogCard(
                    details = ModelCardDetails(
                        id = NpuPackageStore.DOWNLOAD_ID,
                        name = NpuPackageStore.DISPLAY_NAME,
                        licenseName = "CC BY 4.0 • Qualcomm NPU",
                        sizeLabel = "~1.30 GB",
                        languageCount = 25
                    ),
                    installed = npuArchiveState == NpuArchiveState.READY,
                    persistent = false,
                    selected = selectedEngine == AsrEngine.PARAKEET_NPU.id,
                    restoring = npuActionRunning || localRestoringId != null,
                    partialDownload = npuArchiveState == NpuArchiveState.INCOMPLETE,
                    modelDownload = modelDownload,
                    removalEnabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning && localRestoringId == null,
                    onUse = {
                        scope.launch {
                            npuActionRunning = true
                            try {
                                withContext(Dispatchers.IO) { NpuPackageStore.selectForTranscription(context) }
                                selectedEngine = AsrEngine.PARAKEET_NPU.id
                                selectedPath = NpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                status = "Parakeet NPU selected"
                            } catch (t: Throwable) {
                                status = "Could not select Parakeet NPU: ${t.message ?: t.javaClass.simpleName}"
                            } finally {
                                npuActionRunning = false
                            }
                        }
                    },
                    onDownload = { onStartModelDownload(NpuPackageStore.DOWNLOAD_ID) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${NpuPackageStore.REPO_ID}")))
                    },
                    onRemove = { npuDeleteConfirm = true }
                )
            }
            item {
                ModelCatalogCard(
                    details = ModelCardDetails(
                        id = OmiNpuPackageStore.DOWNLOAD_ID,
                        name = OmiNpuPackageStore.DISPLAY_NAME,
                        licenseName = "CC BY 4.0 • Qualcomm NPU",
                        sizeLabel = "~1.24 GB",
                        languageCount = 1
                    ),
                    installed = omiArchiveState == NpuArchiveState.READY,
                    persistent = false,
                    selected = selectedEngine == AsrEngine.OMI_NPU.id,
                    restoring = npuActionRunning || localRestoringId != null,
                    partialDownload = omiArchiveState == NpuArchiveState.INCOMPLETE,
                    modelDownload = modelDownload,
                    removalEnabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning && localRestoringId == null,
                    onUse = {
                        scope.launch {
                            npuActionRunning = true
                            try {
                                withContext(Dispatchers.IO) { OmiNpuPackageStore.selectForTranscription(context) }
                                selectedEngine = AsrEngine.OMI_NPU.id
                                selectedPath = OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                status = "Omi Med NPU selected; review medical terminology"
                            } catch (t: Throwable) {
                                status = "Could not select Omi NPU: ${t.message ?: t.javaClass.simpleName}"
                            } finally {
                                npuActionRunning = false
                            }
                        }
                    },
                    onDownload = { onStartModelDownload(OmiNpuPackageStore.DOWNLOAD_ID) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${OmiNpuPackageStore.REPO_ID}")))
                    },
                    onRemove = { omiDeleteConfirm = true }
                )
            }

            item {
                ModelCatalogCard(
                    details = ModelCardDetails(
                        id = Qwen3HybridPackageStore.DOWNLOAD_ID,
                        name = Qwen3HybridPackageStore.DISPLAY_NAME,
                        licenseName = "Apache 2.0 · Experimental hybrid, not clinically validated",
                        sizeLabel = Qwen3HybridPackageStore.packageDescription(context),
                        languageCount = 30
                    ),
                    installed = qwenArchiveState == NpuArchiveState.READY,
                    persistent = false,
                    selected = selectedEngine == AsrEngine.QWEN3_HYBRID.id,
                    restoring = npuActionRunning || localRestoringId != null,
                    partialDownload = qwenArchiveState == NpuArchiveState.INCOMPLETE,
                    modelDownload = modelDownload,
                    removalEnabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning,
                    onUse = {
                        scope.launch {
                            npuActionRunning = true
                            try {
                                withContext(Dispatchers.IO) { Qwen3HybridPackageStore.select(context) }
                                selectedEngine = AsrEngine.QWEN3_HYBRID.id
                                selectedPath = Qwen3HybridPackageStore.MODEL_KEY
                                status = "Experimental Qwen hybrid selected. First verify QNN restoration on the S26."
                            } catch (t: Throwable) { status = "Cannot select Qwen hybrid: ${t.message}" }
                            finally { npuActionRunning = false }
                        }
                    },
                    onDownload = { onStartModelDownload(Qwen3HybridPackageStore.DOWNLOAD_ID) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://huggingface.co/${Qwen3HybridPackageStore.REPO_ID}/tree/main"))) },
                    onRemove = { qwenDeleteConfirm = true }
                )
                OutlinedButton(onClick = {
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val cleared = withContext(Dispatchers.IO) {
                                Qwen3HybridPackageStore.clearTemporaryTestFiles(context)
                            }
                            status = if (cleared) "Qwen temporary test files deleted and verified"
                                else "Qwen temporary test files could not be fully deleted"
                        } catch (t: Throwable) { status = "Qwen temporary cleanup failed: ${t.message}" }
                        finally { npuActionRunning = false }
                    }
                }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                    Text("Clear Qwen temporary test files (manual)")
                }
                if (qwenArchiveState == NpuArchiveState.READY && !Qwen3HybridPackageStore.fp16Installed(context)) {
                    OutlinedButton(onClick = { onStartModelDownload(Qwen3HybridPackageStore.DOWNLOAD_ID) },
                        enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Upgrade Qwen to smaller Stage I FP16-storage model")
                    }
                }
                if ((Qwen3HybridPackageStore.fp16Installed(context) || Qwen3HybridPackageStore.compactInstalled(context)) && Qwen3HybridPackageStore.legacyInstalled(context)) {
                    OutlinedButton(onClick = { qwenLegacyDeleteConfirm = true                    }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Remove old Qwen v1 weights (newer model verified)")
                    }
                }
                if (Qwen3HybridPackageStore.fp16Installed(context) && Qwen3HybridPackageStore.compactInstalled(context)) {
                    OutlinedButton(onClick = { qwenCompactDeleteConfirm = true },
                        enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Remove previous Qwen v2 decoder package (v3 verified)")
                    }
                }
                if (qwenArchiveState == NpuArchiveState.INCOMPLETE) {
                    OutlinedButton(onClick = { qwenDeleteConfirm = true },
                        enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Clear incomplete Qwen downloads")
                    }
                }
                if (qwenArchiveState == NpuArchiveState.READY) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            npuActionRunning = true
                            status = "Testing Qwen Android mel parity and S26 QNN context loading…"
                            try {
                                val report = withContext(Dispatchers.IO) {
                                    Qwen3HybridClient.connect(context).use { it.prepare() }
                                }
                                status = if (report.optBoolean("ok"))
                                    "Qwen encoder loaded with CPU fallback disabled; experimental transcription may now be tested"
                                else "Qwen compatibility test failed: ${report.optString("error")}" 
                            } catch (t: Throwable) { status = "Qwen probe error: ${t.message}" }
                            finally { npuActionRunning = false }
                        }
                    }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Test Qwen preprocessing + S26 NPU encoder")
                    }
                }
            }

            item { SectionLabel("GPU") }
            items(LocalAsrModelManager.catalog.filter { it.engine == AsrEngine.PARAKEET || it.engine == AsrEngine.OMI }, key = { "gpu-${it.id}" }) { model ->
                val localInstalled = model.id in localInstalledIds
                val localPersistent = model.id in localPersistentIds
                val localSelected = selectedEngine == model.engine.id && localInstalled
                LocalAsrModelCard(
                    model = model,
                    installed = localInstalled,
                    persistent = localPersistent,
                    selected = localSelected,
                    restoring = localRestoringId != null || npuActionRunning,
                    modelDownload = modelDownload,
                    partialDownload = LocalAsrModelManager.hasPartialDownload(context, model),
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.modelInfoUrl))) },
                    removalEnabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                    onUse = { useLocalModel(model, localInstalled, localPersistent) },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = { onCancelModelDownload() },
                    onRemove = { removeLocalTarget = model }
                )
            }

            item { SectionLabel("CPU") }
            items(LocalAsrModelManager.catalog.filter { it.engine == AsrEngine.QWEN3 }, key = { "cpu-${it.id}" }) { model ->
                val localInstalled = model.id in localInstalledIds
                val localPersistent = model.id in localPersistentIds
                val localSelected = selectedEngine == model.engine.id && localInstalled
                LocalAsrModelCard(
                    model = model,
                    installed = localInstalled,
                    persistent = localPersistent,
                    selected = localSelected,
                    restoring = localRestoringId != null || npuActionRunning,
                    modelDownload = modelDownload,
                    partialDownload = LocalAsrModelManager.hasPartialDownload(context, model),
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.modelInfoUrl))) },
                    removalEnabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                    onUse = { useLocalModel(model, localInstalled, localPersistent) },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = { onCancelModelDownload() },
                    onRemove = { removeLocalTarget = model }
                )
            }

            val customModels = installed.filter { ModelManager.builtInFor(it) == null }
            items(customModels, key = { it.absolutePath }) { file ->
                val isSelected = selectedEngine == "whisper" && selectedPath == file.absolutePath
                ModelCatalogCard(
                    details = ModelCardDetails(file.absolutePath, ModelManager.displayName(file), "Imported Whisper · CPU",
                        "${file.length() / (1024 * 1024)} MiB", ModelManager.whisperLanguages.size - 1),
                    installed = true, persistent = false, selected = isSelected,
                    restoring = localRestoringId != null || npuActionRunning,
                    partialDownload = false, modelDownload = modelDownload,
                    onUse = { ModelManager.select(context, file); selectedEngine = "whisper"; selectedPath = file.absolutePath },
                    onDownload = {}, onCancel = onCancelModelDownload, onOpenModel = null,
                    removalEnabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning,
                    onRemove = { deleteTarget = file }
                )
            }
            items(ModelManager.multilingualBuiltIns, key = { it.id }) { model ->
                val already = installed.firstOrNull { ModelManager.builtInFor(it)?.id == model.id }
                val persistentDetected = model.id in persistentModelIds
                val isSelected = selectedEngine == "whisper" && already != null && selectedPath == already.absolutePath
                ModelCatalogCard(
                    details = ModelCardDetails(model.id, "Whisper ${model.name}", "whisper.cpp · CPU",
                        model.approxSize, ModelManager.whisperLanguages.size - 1),
                    installed = already != null, persistent = persistentDetected, selected = isSelected,
                    restoring = localRestoringId != null || npuActionRunning,
                    partialDownload = ModelManager.hasPartialDownload(context, model), modelDownload = modelDownload,
                    onUse = {
                        if (already != null) {
                            ModelManager.select(context, already)
                            selectedEngine = "whisper"; selectedPath = already.absolutePath
                        } else {
                            status = "Restore the saved Whisper model from Models & downloads"
                        }
                    },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://huggingface.co/ggerganov/whisper.cpp"))) },
                    removalEnabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning,
                    onRemove = { if (already != null) deleteTarget = already }
                )
            }
        }
    }

    if (qwenCompactDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { qwenCompactDeleteConfirm = false },
            title = { Text("Remove older Qwen v2 files?") },
            text = { Text("Only the Stage H compact FP32 package will be removed, after SHA-256 verification of the newer Stage I FP16-storage package. The NPU encoder, recordings and other models remain untouched.") },
            confirmButton = {
                Button(onClick = {
                    qwenCompactDeleteConfirm = false
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val ok = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.removeCompactAfterFp16(context) }
                            status = if (ok) "Previous Qwen v2 weights removed; verified FP16 v3 retained" else "Old compact Qwen files could not be fully removed"
                        } catch (t: Throwable) { status = "Qwen v2 cleanup failed: ${t.message}" }
                        finally { npuActionRunning = false }
                    }
                }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) { Text("Remove older v2 package") }
            },
            dismissButton = { TextButton(onClick = { qwenCompactDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (qwenLegacyDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { qwenLegacyDeleteConfirm = false },
            title = { Text("Remove old Qwen weights?") },
            text = { Text("Your newer verified Qwen model will be retained. Only the obsolete app-private Qwen Stage G decoder package will be deleted. Your recordings and other ASR models will not be touched.") },
            confirmButton = {
                Button(onClick = {
                    qwenLegacyDeleteConfirm = false
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val ok = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.removeLegacyAfterUpgrade(context) }
                            status = if (ok) "Old Qwen Stage G weights removed; compact model retained"
                                else "Could not remove old Qwen weights"
                        } catch (t: Throwable) { status = "Qwen old-package cleanup failed: ${t.message}" }
                        finally { npuActionRunning = false }
                    }
                }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                    Text("Remove old weights")
                }
            },
            dismissButton = { TextButton(onClick = { qwenLegacyDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (npuDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { npuDeleteConfirm = false },
            title = { Text("Remove Parakeet NPU model?") },
            text = { Text("Remove the downloaded Parakeet NPU model from Whisperapp/Models? If selected, transcription will switch to Whisper until another model is chosen.") },
            confirmButton = {
                Button(
                    enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning && localRestoringId == null,
                    onClick = {
                        npuDeleteConfirm = false
                        scope.launch {
                            npuActionRunning = true
                            status = "Removing Parakeet NPU model…"
                            try {
                                val ok = withContext(Dispatchers.IO) { NpuPackageStore.deleteArchive(context) }
                                npuArchiveState = withContext(Dispatchers.IO) { NpuPackageStore.archiveState(context) }
                                refreshModels(restoreWhisper = false)
                                status = if (ok) "Removed Parakeet NPU model" else "Could not remove Parakeet NPU model"
                            } catch (t: Exception) {
                                status = "Could not remove Parakeet NPU model: ${t.message ?: t.javaClass.simpleName}"
                            } finally {
                                npuActionRunning = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { npuDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (omiDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { omiDeleteConfirm = false },
            title = { Text("Remove Omi Med NPU model?") },
            text = { Text("Remove the downloaded Omi Med NPU model from Whisperapp/Models? If selected, transcription will switch to Whisper until another model is chosen.") },
            confirmButton = {
                Button(
                    onClick = {
                        omiDeleteConfirm = false
                        scope.launch {
                            npuActionRunning = true
                            status = "Removing Omi Med NPU model…"
                            try {
                                val ok = withContext(Dispatchers.IO) { OmiNpuPackageStore.deleteArchive(context) }
                                omiArchiveState = withContext(Dispatchers.IO) { OmiNpuPackageStore.archiveState(context) }
                                refreshModels(restoreWhisper = false)
                                status = if (ok) "Removed Omi Med NPU model" else "Could not remove Omi Med NPU model"
                            } catch (t: Exception) {
                                status = "Could not remove Omi Med NPU model: ${t.message ?: t.javaClass.simpleName}"
                            } finally {
                                npuActionRunning = false
                            }
                        }
                    },
                    enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning && localRestoringId == null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { omiDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { file ->
        val isSelected = selectedEngine == "whisper" && selectedPath == file.absolutePath
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove model?") },
            text = { Text(if (transcriptionState.active) "A transcription is currently running. Wait for it to finish or cancel it before removing a model." else "Remove ${file.name} (${file.length() / (1024 * 1024)} MiB) from this device?${if (isSelected) " It is currently selected, so the active model will be cleared." else ""}") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget = null
                        scope.launch {
                            status = "Removing ${file.name}…"
                            val ok = withContext(Dispatchers.IO) {
                                runCatching { ModelManager.delete(context, file) }.getOrDefault(false)
                            }
                            refreshModels(restoreWhisper = false)
                            status = if (ok) "Removed ${file.name}" else "Could not remove ${file.name}"
                        }
                    },
                    enabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

