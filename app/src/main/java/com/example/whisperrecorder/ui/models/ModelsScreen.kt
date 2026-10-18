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
import com.example.whisperrecorder.npu.QwenDecoderPerformancePolicy
import com.example.whisperrecorder.qwen.Qwen3HybridClient
import com.example.whisperrecorder.diagnostics.DiagnosticReports
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
    var qwenVisibleSaved by remember { mutableStateOf(false) }
    var qwenStateChecking by remember { mutableStateOf(true) }
    var qwenDeleteConfirm by remember { mutableStateOf(false) }
    var qwenReclaimConfirm by remember { mutableStateOf(false) }
    var qwenAdvancedExpanded by remember { mutableStateOf(false) }
    var qwenFastDecoder by remember { mutableStateOf(QwenDecoderPerformancePolicy.fast(context)) }
    var qwenCpuArenaDisabled by remember { mutableStateOf(QwenDecoderPerformancePolicy.cpuArenaDisabled(context)) }
    var qwenPrepackingDisabled by remember { mutableStateOf(QwenDecoderPerformancePolicy.prepackingDisabled(context)) }
    var qwenFp16EmbeddingTrial by remember { mutableStateOf(QwenDecoderPerformancePolicy.useFp16EmbeddingTrial(context)) }
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

    // Independently recheck app-private Qwen files whenever this screen opens or a download
    // state changes. The previous guard skipped this check if the last download was for
    // Parakeet/Omi/Whisper, leaving a working Qwen install falsely shown as "Download".
    suspend fun refreshQwenState(): NpuArchiveState {
        qwenStateChecking = true
        return try {
            val (result, archived) = withContext(Dispatchers.IO) {
                Qwen3HybridPackageStore.state(context) to Qwen3HybridPackageStore.visibleArchiveReady(context)
            }
            qwenArchiveState = result
            qwenVisibleSaved = archived
            result
        } finally { qwenStateChecking = false }
    }
    LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {
        refreshQwenState()
    }

    if (qwenDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { qwenDeleteConfirm = false },
            title = { Text("Remove Qwen3 hybrid model?") },
            text = { Text("Remove the Qwen app-private runtime working copy? The user-visible archive in Whisperapp/Models, if saved, will NOT be deleted. Reusing the archive may require an offline restore.") },
            confirmButton = {
                Button(onClick = {
                    qwenDeleteConfirm = false
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val removed = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.delete(context) }
                            qwenArchiveState = withContext(Dispatchers.IO) { Qwen3HybridPackageStore.state(context) }
                            refreshModels(false)
                            status = if (removed) "Qwen runtime working copy removed; saved Whisperapp/Models archive retained" else "Qwen runtime working copy could not be fully removed"
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
                    persistent = qwenVisibleSaved,
                    selected = selectedEngine == AsrEngine.QWEN3_HYBRID.id,
                    restoring = npuActionRunning || localRestoringId != null || qwenStateChecking,
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
                    onDownload = {
                        scope.launch {
                            // A download button rendered from stale UI state must never trigger
                            // a needless multi-gigabyte Qwen download. Recheck local files first.
                            val actual = refreshQwenState()
                            if (actual == NpuArchiveState.READY) {
                                status = "Existing Qwen model detected; no download needed. Tap Use."
                            } else {
                                onStartModelDownload(Qwen3HybridPackageStore.DOWNLOAD_ID)
                            }
                        }
                    },
                    onCancel = onCancelModelDownload,
                    onOpenModel = { context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://huggingface.co/${Qwen3HybridPackageStore.REPO_ID}/tree/main"))) },
                    onRemove = { qwenDeleteConfirm = true }
                )
                OutlinedButton(onClick = {
                    scope.launch {
                        val actual = refreshQwenState()
                        status = when (actual) {
                            NpuArchiveState.READY -> if (qwenVisibleSaved)
                                "Qwen found in Whisperapp/Models. Use does not require a download."
                                else "Existing Qwen working copy found. Tap Save existing Qwen to Whisperapp/Models to archive it offline."
                            NpuArchiveState.INCOMPLETE -> "Qwen files found but package verification is incomplete. Use Resume to repair, without deleting existing files."
                            NpuArchiveState.NOT_INSTALLED -> "No installed Qwen package found in the app's recognized folders."
                        }
                    }
                }, enabled = !qwenStateChecking && !modelDownload.active && !npuActionRunning) {
                    Text("Recheck installed Qwen model")
                }
                if (qwenArchiveState == NpuArchiveState.READY && !qwenVisibleSaved) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            npuActionRunning = true
                            status = "Saving existing Qwen bytes to the selected Whisperapp/Models folder (offline). This may require several GB of available space."
                            try {
                                val location = withContext(Dispatchers.IO) {
                                    Qwen3HybridPackageStore.saveInstalledToWhisperappModels(context)
                                }
                                qwenVisibleSaved = withContext(Dispatchers.IO) {
                                    Qwen3HybridPackageStore.visibleArchiveReady(context)
                                }
                                status = "Qwen saved in $location. Existing runtime weights retained; no network download."
                            } catch (t: Throwable) {
                                status = "Qwen archive copy failed; existing working model preserved: ${t.message}"
                            } finally { npuActionRunning = false }
                        }
                    }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Save existing Qwen to Whisperapp/Models (no download)")
                    }
                }
                if (qwenArchiveState == NpuArchiveState.READY) {
                    OutlinedButton(onClick = {
                        qwenFastDecoder = !qwenFastDecoder
                        QwenDecoderPerformancePolicy.setFast(context, qwenFastDecoder)
                        status = if (qwenFastDecoder)
                            "Qwen speed experiment: 4 CPU decoder threads on the NEXT transcription. Compare time and phone heat to 2 threads."
                        else "Qwen balanced decoder: 2 CPU threads on the NEXT transcription."
                    }, enabled = !transcriptionState.active && !npuActionRunning) {
                        Text(if (qwenFastDecoder) "Qwen CPU decoder: 4 threads (speed test)" else "Qwen CPU decoder: 2 threads (balanced) — tap to test 4")
                    }
                    TextButton(onClick = { qwenAdvancedExpanded = !qwenAdvancedExpanded }) {
                        Text(if (qwenAdvancedExpanded) "Hide Qwen advanced diagnostics & maintenance" else "Qwen advanced diagnostics & maintenance")
                    }
                    if (qwenAdvancedExpanded) {
                        OutlinedButton(onClick = {
                            qwenPrepackingDisabled = !qwenPrepackingDisabled
                            QwenDecoderPerformancePolicy.setPrepackingDisabled(context, qwenPrepackingDisabled)
                            status = if (qwenPrepackingDisabled)
                                "Qwen weight prepacking disabled for NEXT transcription. Compare memory and token speed; may be slower."
                            else "Qwen normal decoder prepacking restored for NEXT transcription."
                        }, enabled = !transcriptionState.active && !npuActionRunning) {
                            Text(if (qwenPrepackingDisabled)
                                "Qwen weight prepacking: OFF (experimental)"
                                else "Qwen weight prepacking: ON (baseline)")
                        }
                        OutlinedButton(onClick = {
                            qwenCpuArenaDisabled = !qwenCpuArenaDisabled
                            QwenDecoderPerformancePolicy.setCpuArenaDisabled(context, qwenCpuArenaDisabled)
                            status = if (qwenCpuArenaDisabled)
                                "Qwen memory trial: CPU arena disabled on NEXT transcription. Compare PSS, timing and output; revert if slower."
                            else "Qwen standard ONNX Runtime CPU arena enabled for NEXT transcription."
                        }, enabled = !transcriptionState.active && !npuActionRunning) {
                            Text(if (qwenCpuArenaDisabled)
                                "Qwen CPU arena: OFF (experimental memory test)"
                                else "Qwen CPU arena: ON (baseline)")
                        }
                        OutlinedButton(onClick = {
                            qwenFp16EmbeddingTrial = !qwenFp16EmbeddingTrial
                            QwenDecoderPerformancePolicy.setFp16EmbeddingTrial(context, qwenFp16EmbeddingTrial)
                            status = if (qwenFp16EmbeddingTrial)
                                "K2A FP16 trial selected. Only exact SHA-256 verified companion files will load; baseline weights remain preserved."
                            else "K2A trial disabled; original verified FP32 embeddings used."
                        }, enabled = !transcriptionState.active && !npuActionRunning) {
                            Text(if (qwenFp16EmbeddingTrial)
                                "K2A FP16 embeddings: experimental ON (tap for baseline)"
                                else "K2A FP16 embeddings: OFF (optional parity trial)")
                        }
                        if (qwenVisibleSaved) {
                            OutlinedButton(onClick = { qwenReclaimConfirm = true },
                                enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                                Text("Reclaim duplicate private Qwen weights (after shared-runtime test)")
                            }
                        }
                    } // Advanced Qwen diagnostics and maintenance
                }
                if (qwenAdvancedExpanded) {
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
                }
                if (qwenArchiveState == NpuArchiveState.INCOMPLETE) {
                    OutlinedButton(onClick = { qwenDeleteConfirm = true },
                        enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                        Text("Clear incomplete Qwen downloads")
                    }
                }
                if (qwenAdvancedExpanded && qwenArchiveState == NpuArchiveState.READY) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            npuActionRunning = true
                            status = "Testing Qwen Android mel parity and S26 QNN context loading…"
                            try {
                                val report = withContext(Dispatchers.IO) {
                                    val probe = DiagnosticReports.begin(context, "qwen3_hybrid_encoder_probe", 0L)
                                    try {
                                        val result = Qwen3HybridClient.connect(context).use { it.prepare() }
                                        if (result.optBoolean("ok")) {
                                            probe.event("qwen_encoder_probe_success", result.optLong("golden_encoder_inference_ms", 0L))
                                            probe.workerMetric("encoder_graph_calls", result.optLong("native_android_qnn_encoder_graph_calls", 0L), 0)
                                            probe.finish("success")
                                        } else {
                                            probe.event("qwen_encoder_probe_failed")
                                            probe.finish("failure", "loading_model", IllegalStateException("Qwen encoder probe rejected"))
                                        }
                                        result
                                    } catch (t: Throwable) {
                                        probe.event("qwen_encoder_probe_exception")
                                        probe.finish("failure", "loading_model", t)
                                        throw t
                                    }
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

    if (qwenReclaimConfirm) {
        AlertDialog(onDismissRequest = { qwenReclaimConfirm = false },
            title = { Text("Reclaim duplicate Qwen storage?") },
            text = { Text("Only after a successful transcription using the Qwen shared-folder file-descriptor loader, this deletes its extra APP-PRIVATE copy. The saved Qwen package in Whisperapp/Models stays intact. If the document provider cannot load shared files on a future run, a temporary verified copy may be required. Never uninstall or clear app data to perform this operation.") },
            confirmButton = {
                Button(onClick = {
                    qwenReclaimConfirm = false
                    scope.launch {
                        npuActionRunning = true
                        try {
                            val bytes = withContext(Dispatchers.IO) {
                                Qwen3HybridPackageStore.reclaimPrivateDuplicate(context)
                            }
                            status = "Removed $bytes bytes of duplicate Qwen app-private weights; Whisperapp/Models retained"
                            refreshQwenState()
                        } catch (t: Throwable) {
                            status = "Private Qwen backup preserved: ${t.message}"
                        } finally { npuActionRunning = false }
                    }
                }, enabled = !transcriptionState.active && !modelDownload.active && !npuActionRunning) {
                    Text("Reclaim private duplicate")
                }
            }, dismissButton = { TextButton(onClick = { qwenReclaimConfirm = false }) { Text("Cancel") } })
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

