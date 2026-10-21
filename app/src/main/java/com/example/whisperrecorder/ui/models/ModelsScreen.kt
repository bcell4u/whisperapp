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

            item { SectionLabel("QUALCOMM NPU") }
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

            item { SectionLabel("NVIDIA PARAKEET") }
            items(LocalAsrModelManager.nvidiaParakeetModels, key = { "parakeet-local-${it.id}" }) { model ->
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

            item { SectionLabel("ADVANCED LOCAL ASR") }
            items(LocalAsrModelManager.advancedModels, key = { "local-${it.id}" }) { model ->
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
            if (customModels.isNotEmpty()) {
                item { SectionLabel("IMPORTED WHISPER") }
                items(customModels, key = { it.absolutePath }) { file ->
                    val isSelected = selectedEngine == "whisper" && selectedPath == file.absolutePath
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ModelManager.displayName(file), fontWeight = FontWeight.SemiBold)
                            Text("${file.length() / (1024 * 1024)} MiB${if (isSelected) " • Active" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ModelManager.languageSupportLabel(file), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { ModelManager.select(context, file); selectedEngine = "whisper"; selectedPath = file.absolutePath },
                                enabled = !modelDownload.active && localRestoringId == null && !isSelected
                            ) { Text(if (isSelected) "Selected" else "Use") }
                            ModelRemovalAction(
                                visible = true,
                                enabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                                onClick = { deleteTarget = file }
                            )
                        }
                    }
                }
            }

            item { SectionLabel("MULTILINGUAL") }
            items(ModelManager.multilingualBuiltIns, key = { it.id }) { model ->
                val already = installed.firstOrNull { ModelManager.builtInFor(it)?.id == model.id }
                val persistentDetected = model.id in persistentModelIds
                val isSelected = selectedEngine == "whisper" && already != null && selectedPath == already.absolutePath
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(model.name, fontWeight = FontWeight.SemiBold)
                                Text(if (already != null) "${already.length() / (1024 * 1024)} MiB${if (isSelected) " • Active" else " • Installed"}" else model.approxSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ModelManager.languageSupportLabel(model), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (already == null) {
                                if (modelDownload.active && modelDownload.modelId == model.id) OutlinedButton(onClick = { onCancelModelDownload() }) { Text("Cancel") }
                                else Button(onClick = { onStartModelDownload(model.id) }, enabled = !modelDownload.active && localRestoringId == null) { Text(if (ModelManager.hasPartialDownload(context, model)) "Resume" else "Download") }
                            } else {
                                Column(horizontalAlignment = Alignment.End) {
                                    Button(
                                        onClick = { ModelManager.select(context, already); selectedEngine = "whisper"; selectedPath = already.absolutePath },
                                        enabled = !modelDownload.active && localRestoringId == null && !isSelected
                                    ) { Text(if (isSelected) "Selected" else "Use") }
                                    if (persistentDetected && !isSelected) Text("Model available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (already != null) {
                            ModelRemovalAction(
                                visible = true,
                                enabled = !transcriptionState.active && !modelDownload.active && localRestoringId == null && !npuActionRunning,
                                onClick = { deleteTarget = already }
                            )
                        }
                        if (modelDownload.active && modelDownload.modelId == model.id) {
                            DownloadProgressBar(modelDownload.progress, Modifier.fillMaxWidth())
                            Text(modelDownloadProgressText(modelDownload.bytesDownloaded, modelDownload.totalBytes, modelDownload.progress, modelDownload.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
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

