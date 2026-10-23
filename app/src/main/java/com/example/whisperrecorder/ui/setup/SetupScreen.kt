@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.whisperrecorder.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.advanced.LocalAsrModel
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.transcribe.AccelerationTier
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.models.ModelDownloadRuntime
import com.example.whisperrecorder.models.ModelInventoryLoader
import com.example.whisperrecorder.models.ModelManager
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import com.example.whisperrecorder.ui.AudioFormatCard
import com.example.whisperrecorder.ui.DownloadProgressBar
import com.example.whisperrecorder.ui.SectionLabel
import com.example.whisperrecorder.ui.SettingCard
import com.example.whisperrecorder.ui.modelDownloadProgressText
import com.example.whisperrecorder.ui.models.LocalAsrModelCard
import com.example.whisperrecorder.ui.models.AccelerationNoticeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun SetupScreen(
    onComplete: () -> Unit,
    onStartModelDownload: (String) -> Unit,
    onCancelModelDownload: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var treeUri by rememberSaveable { mutableStateOf(AppPrefs.getSaveTree(context)) }
    var format by rememberSaveable { mutableStateOf(AppPrefs.getAudioFormat(context)) }
    var selectedEngine by rememberSaveable { mutableStateOf(AppPrefs.getTranscriptionEngine(context)) }
    var selectedModelPath by rememberSaveable { mutableStateOf(AppPrefs.getModelPath(context)) }
    var accelerationNotice by remember { mutableStateOf<AccelerationTier?>(null) }
    LaunchedEffect(selectedEngine, selectedModelPath) {
        if (selectedModelPath != null) accelerationNotice = AppPrefs.claimAccelerationNotice(context, selectedEngine)
    }
    var installed by remember { mutableStateOf(emptyList<File>()) }
    var persistentModelIds by remember { mutableStateOf(emptySet<String>()) }
    var localInstalledIds by remember { mutableStateOf(emptySet<String>()) }
    var localPersistentIds by remember { mutableStateOf(emptySet<String>()) }
    var localRestoringId by remember { mutableStateOf<String?>(null) }
    val modelDownload by ModelDownloadRuntime.state.collectAsState()
    var status by rememberSaveable { mutableStateOf("") }
    var npuState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var npuChecking by remember { mutableStateOf(false) }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            scope.launch {
                status = "Preparing Whisperapp folders…"
                val result = withContext(Dispatchers.IO) { runCatching { WorkspaceStorage.ensure(context, uri) } }
                result.onSuccess {
                    AppPrefs.setSaveTree(context, uri)
                    treeUri = uri
                    status = "Working directory ready"
                }.onFailure {
                    status = "Could not create Whisperapp folders: ${it.message}"
                }
            }
        }
    }
    suspend fun refreshInventory(restoreWhisper: Boolean) {
        val inventory = runCatching { ModelInventoryLoader.load(context, restoreWhisper) }.getOrNull() ?: return
        installed = inventory.whisperInstalled
        persistentModelIds = inventory.whisperPersistentIds
        localInstalledIds = inventory.localInstalledIds
        localPersistentIds = inventory.localPersistentIds
        selectedEngine = inventory.selectedEngine
        selectedModelPath = inventory.selectedPath
    }

    fun useLocalModel(model: LocalAsrModel, isInstalled: Boolean, isPersistent: Boolean) {
        if (localRestoringId != null || npuChecking || (!isInstalled && !isPersistent)) return
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
                selectedModelPath = path
                status = "${model.name} ready"
            } catch (t: Exception) {
                status = "Could not use ${model.name}: ${t.message ?: t.javaClass.simpleName}"
            } finally {
                localRestoringId = null
            }
        }
    }

    LaunchedEffect(treeUri) {
        if (treeUri != null) status = "Checking saved Whisperapp/Models folder…"
        refreshInventory(restoreWhisper = treeUri != null)
        if (treeUri != null) {
            status = if (installed.isNotEmpty() || localInstalledIds.isNotEmpty() || localPersistentIds.isNotEmpty()) "Saved models are available" else ""
        }
    }

    LaunchedEffect(modelDownload.completedPath, modelDownload.error) {
        if (modelDownload.completedPath != null || modelDownload.error != null) {
            refreshInventory(restoreWhisper = false)
        }
    }
    LaunchedEffect(treeUri, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        if (treeUri != null && !modelDownload.active) {
            npuState = withContext(Dispatchers.IO) { NpuPackageStore.archiveState(context) }
        } else if (treeUri == null) {
            npuState = NpuArchiveState.NOT_INSTALLED
        }
    }
    LaunchedEffect(modelDownload.status, modelDownload.error) {
        if (modelDownload.status.isNotBlank()) status = modelDownload.status
        modelDownload.error?.let { status = "Download failed: $it" }
    }

    accelerationNotice?.let { tier -> AccelerationNoticeDialog(tier) { accelerationNotice = null } }

    Scaffold(topBar = { TopAppBar(title = { Text("Set up Whisper Recorder") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 18.dp)
        ) {
            item {
                Text("Three quick choices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("You only need to do this once. These choices can be changed later in Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionLabel("1  WORKING DIRECTORY") }
            item {
                SettingCard(
                    "Working directory",
                    treeUri?.let { "${it.lastPathSegment ?: "Selected folder"}/Whisperapp" } ?: "Choose where Whisperapp stores its files",
                    if (treeUri == null) "Choose" else "Change"
                ) { treePicker.launch(treeUri) }
            }
            item { SectionLabel("2  AUDIO FORMAT") }
            item { AudioFormatCard(selected = format, onSelected = { format = it; AppPrefs.setAudioFormat(context, it) }) }
            item { SectionLabel("3  TRANSCRIPTION MODEL") }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose a transcription model", style = MaterialTheme.typography.titleMedium)
                        Text("Select a transcription model. NPU models offer the best speed and battery efficiency. GPU models provide good speed but use more battery and generate more heat. CPU models are the slowest. Downloaded models are saved in Whisperapp/Models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            val customInstalled = installed.filter { ModelManager.builtInFor(it) == null }
            if (customInstalled.isNotEmpty()) {
                item { Text("Imported Whisper models", style = MaterialTheme.typography.titleSmall) }
                items(customInstalled, key = { "setup-installed-${it.absolutePath}" }) { file ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ModelManager.displayName(file), fontWeight = FontWeight.SemiBold)
                                Text("${file.length() / (1024 * 1024)} MiB", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { ModelManager.select(context, file); selectedEngine = "whisper"; selectedModelPath = file.absolutePath }, enabled = !modelDownload.active && localRestoringId == null && !(selectedEngine == "whisper" && selectedModelPath == file.absolutePath)) {
                                Text(if (selectedEngine == "whisper" && selectedModelPath == file.absolutePath) "Selected" else "Use")
                            }
                        }
                    }
                }
            }

            item { SectionLabel("NPU ACCELERATED") }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(NpuPackageStore.DISPLAY_NAME, fontWeight = FontWeight.SemiBold)
                        Text("Optional ORT-QNN / HTP backend for validated SM8850/V81 devices. Requires a separate model download to the working directory. Unsupported devices cannot select this backend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${NpuPackageStore.REPO_ID} · " + when (npuState) {
                            NpuArchiveState.NOT_INSTALLED -> "Not installed"
                            NpuArchiveState.INCOMPLETE -> "Incomplete / repairable"
                            NpuArchiveState.READY -> "Package ready"
                        }, style = MaterialTheme.typography.bodySmall)
                        val downloadingNpu = modelDownload.active && modelDownload.modelId == NpuPackageStore.DOWNLOAD_ID
                        if (downloadingNpu) {
                            DownloadProgressBar(modelDownload.progress, Modifier.fillMaxWidth())
                            Text(modelDownloadProgressText(modelDownload.bytesDownloaded, modelDownload.totalBytes, modelDownload.progress, modelDownload.status), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = onCancelModelDownload) { Text("Cancel") }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onStartModelDownload(NpuPackageStore.DOWNLOAD_ID) },
                                    enabled = treeUri != null && !modelDownload.active && !npuChecking && localRestoringId == null
                                ) { Text(if (npuState == NpuArchiveState.NOT_INSTALLED) "Download package" else "Repair from HF") }
                            }
                            if (npuState == NpuArchiveState.READY) {
                                val npuSelected = selectedEngine == "parakeet_npu" && selectedModelPath == NpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                Button(
                                    onClick = {
                                        scope.launch {
                                            npuChecking = true
                                            try {
                                                withContext(Dispatchers.IO) { NpuPackageStore.selectForTranscription(context) }
                                                selectedEngine = "parakeet_npu"
                                                selectedModelPath = NpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                                status = "Qualcomm NPU model selected"
                                            } catch (t: Throwable) {
                                                status = "Cannot select NPU on this device: ${t.message ?: t.javaClass.simpleName}"
                                            } finally { npuChecking = false }
                                        }
                                    },
                                    enabled = !npuSelected && !modelDownload.active && !npuChecking && localRestoringId == null
                                ) { Text(if (npuSelected) "Selected" else "Use NPU") }
                            }
                        }
                    }
                }
            }

            item { SectionLabel("GPU ACCELERATED") }

            items(LocalAsrModelManager.gpuModels, key = { "setup-parakeet-local-${it.id}" }) { model ->
                val localInstalled = model.id in localInstalledIds
                val localPersistent = model.id in localPersistentIds
                val localSelected = selectedEngine == model.engine.id && localInstalled
                LocalAsrModelCard(
                    model = model,
                    installed = localInstalled,
                    persistent = localPersistent,
                    selected = localSelected,
                    restoring = localRestoringId != null || npuChecking,
                    modelDownload = modelDownload,
                    partialDownload = LocalAsrModelManager.hasPartialDownload(context, model),
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.modelInfoUrl))) },
                    onUse = { useLocalModel(model, localInstalled, localPersistent) },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = { onCancelModelDownload() }
                )
            }

            item { SectionLabel("HYBRID (CPU/NPU)") }
            items(LocalAsrModelManager.hybridModels, key = { "setup-local-${it.id}" }) { model ->
                val localInstalled = model.id in localInstalledIds
                val localPersistent = model.id in localPersistentIds
                val localSelected = selectedEngine == model.engine.id && localInstalled
                LocalAsrModelCard(
                    model = model,
                    installed = localInstalled,
                    persistent = localPersistent,
                    selected = localSelected,
                    restoring = localRestoringId != null || npuChecking,
                    modelDownload = modelDownload,
                    partialDownload = LocalAsrModelManager.hasPartialDownload(context, model),
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.modelInfoUrl))) },
                    onUse = { useLocalModel(model, localInstalled, localPersistent) },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = { onCancelModelDownload() }
                )
            }

            item { SectionLabel("MULTILINGUAL") }
            items(ModelManager.multilingualBuiltIns, key = { "setup-download-${it.fileName}" }) { model ->
                val already = installed.firstOrNull { ModelManager.builtInFor(it)?.id == model.id }
                val persistentDetected = model.id in persistentModelIds
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(model.name, fontWeight = FontWeight.SemiBold)
                                Text(model.approxSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ModelManager.languageSupportLabel(model), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            when {
                                already != null -> Column(horizontalAlignment = Alignment.End) {
                                    val selected = selectedEngine == "whisper" && selectedModelPath == already.absolutePath
                                    Button(onClick = { ModelManager.select(context, already); selectedEngine = "whisper"; selectedModelPath = already.absolutePath }, enabled = !modelDownload.active && localRestoringId == null && !selected) { Text(if (selected) "Selected" else "Use") }
                                    if (persistentDetected && !selected) Text("Model available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                modelDownload.active && modelDownload.modelId == model.id -> OutlinedButton(onClick = { onCancelModelDownload() }) { Text("Cancel") }
                                else -> Button(onClick = { onStartModelDownload(model.id) }, enabled = !modelDownload.active && localRestoringId == null) { Text(if (ModelManager.hasPartialDownload(context, model)) "Resume" else "Download") }
                            }
                        }
                        if (modelDownload.active && modelDownload.modelId == model.id) {
                            DownloadProgressBar(modelDownload.progress, Modifier.fillMaxWidth())
                            Text(modelDownloadProgressText(modelDownload.bytesDownloaded, modelDownload.totalBytes, modelDownload.progress, modelDownload.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onComplete,
                    enabled = treeUri != null &&
                        when {
                            selectedEngine == "parakeet_npu" -> selectedModelPath == NpuPackageStore.TRANSCRIPTION_MODEL_KEY && NpuPackageStore.archiveState(context) == NpuArchiveState.READY
                            selectedEngine == "omi_npu" -> selectedModelPath == OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY && OmiNpuPackageStore.archiveState(context) == NpuArchiveState.READY
                            selectedEngine in LocalAsrModelManager.engineIds -> selectedModelPath?.let { File(it).isFile } == true && LocalAsrModelManager.byEngine(selectedEngine)?.let { LocalAsrModelManager.isInstalled(context, it) } == true
                            else -> selectedModelPath?.let { File(it).isFile } == true
                        } && !modelDownload.active && localRestoringId == null && !npuChecking,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Finish setup") }
            }
        }
    }
}

