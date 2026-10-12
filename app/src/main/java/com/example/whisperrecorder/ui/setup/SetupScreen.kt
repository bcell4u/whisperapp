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
import com.example.whisperrecorder.npu.Qwen3HybridPackageStore
import com.example.whisperrecorder.ui.AudioFormatCard
import com.example.whisperrecorder.ui.DownloadProgressBar
import com.example.whisperrecorder.ui.SectionLabel
import com.example.whisperrecorder.ui.SettingCard
import com.example.whisperrecorder.ui.modelDownloadProgressText
import com.example.whisperrecorder.ui.models.LocalAsrModelCard
import com.example.whisperrecorder.ui.models.ModelCatalogCard
import com.example.whisperrecorder.ui.models.ModelCardDetails
import com.example.whisperrecorder.transcribe.AsrEngine
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
    var omiState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var qwenState by remember { mutableStateOf(NpuArchiveState.NOT_INSTALLED) }
    var qwenVisibleSaved by remember { mutableStateOf(false) }
    var qwenStateChecking by remember { mutableStateOf(true) }
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
            omiState = withContext(Dispatchers.IO) { OmiNpuPackageStore.archiveState(context) }
        } else if (treeUri == null) {
            npuState = NpuArchiveState.NOT_INSTALLED
            omiState = withContext(Dispatchers.IO) { OmiNpuPackageStore.archiveState(context) }
        }
    }
    // Qwen has a durable user-visible archive in the same SAF Whisperapp/Models folder
    // as other models, plus an ONNX/QNN-compatible private working copy. Always check both.
    suspend fun refreshQwenState(): NpuArchiveState {
        qwenStateChecking = true
        return try {
            val (actual, saved) = withContext(Dispatchers.IO) {
                Qwen3HybridPackageStore.state(context) to Qwen3HybridPackageStore.visibleArchiveReady(context)
            }
            qwenState = actual
            qwenVisibleSaved = saved
            actual
        } finally { qwenStateChecking = false }
    }
    LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        refreshQwenState()
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

            item { SectionLabel("NPU") }
            item {
                ModelCatalogCard(
                    details = ModelCardDetails(NpuPackageStore.DOWNLOAD_ID, NpuPackageStore.DISPLAY_NAME,
                        "CC BY 4.0 · Qualcomm Hexagon NPU", "~1.30 GB", 25),
                    installed = npuState == NpuArchiveState.READY, persistent = false,
                    selected = selectedEngine == "parakeet_npu", restoring = npuChecking || treeUri == null || localRestoringId != null,
                    partialDownload = npuState == NpuArchiveState.INCOMPLETE, modelDownload = modelDownload,
                    onDownload = { onStartModelDownload(NpuPackageStore.DOWNLOAD_ID) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${NpuPackageStore.REPO_ID}"))) },
                    onUse = {
                        scope.launch {
                            npuChecking = true
                            try {
                                withContext(Dispatchers.IO) { NpuPackageStore.selectForTranscription(context) }
                                selectedEngine = "parakeet_npu"
                                selectedModelPath = NpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                status = "Parakeet NPU selected"
                            } catch (t: Throwable) { status = "Could not select Parakeet NPU: ${t.message}" }
                            finally { npuChecking = false }
                        }
                    }
                )
            }
            item {
                ModelCatalogCard(
                    details = ModelCardDetails(OmiNpuPackageStore.DOWNLOAD_ID, OmiNpuPackageStore.DISPLAY_NAME,
                        "CC BY 4.0 · Qualcomm Hexagon NPU", "~1.24 GB", 1),
                    installed = omiState == NpuArchiveState.READY, persistent = false,
                    selected = selectedEngine == "omi_npu", restoring = npuChecking || treeUri == null || localRestoringId != null,
                    partialDownload = omiState == NpuArchiveState.INCOMPLETE, modelDownload = modelDownload,
                    onDownload = { onStartModelDownload(OmiNpuPackageStore.DOWNLOAD_ID) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${OmiNpuPackageStore.REPO_ID}"))) },
                    onUse = {
                        scope.launch {
                            npuChecking = true
                            try {
                                withContext(Dispatchers.IO) { OmiNpuPackageStore.selectForTranscription(context) }
                                selectedEngine = "omi_npu"
                                selectedModelPath = OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY
                                status = "Omi Med NPU selected"
                            } catch (t: Throwable) { status = "Could not select Omi NPU: ${t.message}" }
                            finally { npuChecking = false }
                        }
                    }
                )
            }
            item {
                ModelCatalogCard(
                    details = ModelCardDetails(Qwen3HybridPackageStore.DOWNLOAD_ID, Qwen3HybridPackageStore.DISPLAY_NAME,
                        "Apache 2.0 · Experimental hybrid, not clinically validated", Qwen3HybridPackageStore.packageDescription(context), 30),
                    installed = qwenState == NpuArchiveState.READY, persistent = qwenVisibleSaved,
                    selected = selectedEngine == "qwen3_hybrid", restoring = npuChecking || localRestoringId != null || qwenStateChecking,
                    partialDownload = qwenState == NpuArchiveState.INCOMPLETE, modelDownload = modelDownload,
                    onDownload = {
                        scope.launch {
                            val actual = refreshQwenState()
                            if (actual == NpuArchiveState.READY) {
                                status = "Existing Qwen model detected. No download needed; tap Use."
                            } else {
                                onStartModelDownload(Qwen3HybridPackageStore.DOWNLOAD_ID)
                            }
                        }
                    },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${Qwen3HybridPackageStore.REPO_ID}/tree/main"))) },
                    onUse = {
                        scope.launch {
                            npuChecking = true
                            try {
                                withContext(Dispatchers.IO) { Qwen3HybridPackageStore.select(context) }
                                selectedEngine = "qwen3_hybrid"
                                selectedModelPath = Qwen3HybridPackageStore.MODEL_KEY
                                status = "Experimental Qwen hybrid selected"
                            } catch (t: Throwable) { status = "Could not select Qwen hybrid: ${t.message}" }
                            finally { npuChecking = false }
                        }
                    }
                )
            }
            item { SectionLabel("GPU") }
            items(LocalAsrModelManager.catalog.filter { it.engine == AsrEngine.PARAKEET || it.engine == AsrEngine.OMI }, key = { "setup-gpu-${it.id}" }) { model ->
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

            item { SectionLabel("CPU") }
            items(LocalAsrModelManager.catalog.filter { it.engine == AsrEngine.QWEN3 }, key = { "setup-cpu-${it.id}" }) { model ->
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

            val customInstalled = installed.filter { ModelManager.builtInFor(it) == null }
            items(customInstalled, key = { "setup-installed-${it.absolutePath}" }) { file ->
                ModelCatalogCard(
                    details = ModelCardDetails(file.absolutePath, ModelManager.displayName(file), "Imported Whisper · CPU",
                        "${file.length() / (1024 * 1024)} MiB", ModelManager.whisperLanguages.size - 1),
                    installed = true, persistent = false,
                    selected = selectedEngine == "whisper" && selectedModelPath == file.absolutePath,
                    restoring = localRestoringId != null,
                    partialDownload = false, modelDownload = modelDownload,
                    onUse = { ModelManager.select(context, file); selectedEngine = "whisper"; selectedModelPath = file.absolutePath },
                    onDownload = {}, onCancel = onCancelModelDownload, onOpenModel = null
                )
            }
            items(ModelManager.multilingualBuiltIns, key = { "setup-download-${it.fileName}" }) { model ->
                val already = installed.firstOrNull { ModelManager.builtInFor(it)?.id == model.id }
                ModelCatalogCard(
                    details = ModelCardDetails(model.id, "Whisper ${model.name}", "whisper.cpp · CPU",
                        model.approxSize, ModelManager.whisperLanguages.size - 1),
                    installed = already != null,
                    persistent = model.id in persistentModelIds,
                    selected = selectedEngine == "whisper" && already != null && selectedModelPath == already.absolutePath,
                    restoring = localRestoringId != null || treeUri == null,
                    partialDownload = ModelManager.hasPartialDownload(context, model), modelDownload = modelDownload,
                    onUse = {
                        if (already != null) {
                            ModelManager.select(context, already)
                            selectedEngine = "whisper"; selectedModelPath = already.absolutePath
                        } else {
                            status = "Restore saved Whisper model in Models & downloads"
                        }
                    },
                    onDownload = { onStartModelDownload(model.id) },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/ggerganov/whisper.cpp"))) }
                )
            }
            item {
                Button(
                    onClick = onComplete,
                    enabled = treeUri != null &&
                        when {
                            selectedEngine == "parakeet_npu" -> selectedModelPath == NpuPackageStore.TRANSCRIPTION_MODEL_KEY && NpuPackageStore.archiveState(context) == NpuArchiveState.READY
                            selectedEngine == "omi_npu" -> selectedModelPath == OmiNpuPackageStore.TRANSCRIPTION_MODEL_KEY && OmiNpuPackageStore.archiveState(context) == NpuArchiveState.READY
                            selectedEngine == "qwen3_hybrid" -> selectedModelPath == Qwen3HybridPackageStore.MODEL_KEY && Qwen3HybridPackageStore.state(context) == NpuArchiveState.READY
                            selectedEngine in LocalAsrModelManager.engineIds -> selectedModelPath?.let { File(it).isFile } == true && LocalAsrModelManager.byEngine(selectedEngine)?.let { LocalAsrModelManager.isInstalled(context, it) } == true
                            else -> selectedModelPath?.let { File(it).isFile } == true
                        } && !modelDownload.active && localRestoringId == null && !npuChecking,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Finish setup") }
            }
        }
    }
}

