package com.example.whisperrecorder
import android.Manifest
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.whisperrecorder.audio.RecordingRuntime
import com.example.whisperrecorder.audio.MicrophoneProcessing
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.audio.RecordingService
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.data.RecordingDeletionManager
import com.example.whisperrecorder.data.RecordingItem
import com.example.whisperrecorder.data.RecordingStore
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.media.MediaSourceInspector
import com.example.whisperrecorder.models.ModelManager
import com.example.whisperrecorder.models.ModelSelectionResolver
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import com.example.whisperrecorder.models.ModelDownloadRuntime
import com.example.whisperrecorder.models.ModelDownloadService
import com.example.whisperrecorder.transcribe.TranscriptionRuntime
import com.example.whisperrecorder.transcribe.QwenWorkProgress
import com.example.whisperrecorder.transcribe.TranscriptionService
import com.example.whisperrecorder.ui.WhisperRecorderTheme
import com.example.whisperrecorder.ui.SynchronizedAudioPlayer
import com.example.whisperrecorder.ui.BackArrowButton
import com.example.whisperrecorder.ui.CompactProgressBar
import com.example.whisperrecorder.ui.SectionLabel
import com.example.whisperrecorder.ui.RecordingWaveform
import com.example.whisperrecorder.ui.SettingCard
import com.example.whisperrecorder.ui.ShareManager
import com.example.whisperrecorder.ui.TranscriptShareIcon
import com.example.whisperrecorder.ui.audioFormatLabel
import com.example.whisperrecorder.ui.formatElapsed
import com.example.whisperrecorder.ui.formatDateTime
import com.example.whisperrecorder.ui.formatTranscriptionDuration
import com.example.whisperrecorder.ui.sanitizeRecordingTitle
import com.example.whisperrecorder.ui.prompts.PromptsScreen
import com.example.whisperrecorder.ui.TranscriptViewerScreen
import com.example.whisperrecorder.ui.models.ModelsScreen
import com.example.whisperrecorder.ui.settings.AudioFormatSettingsScreen
import com.example.whisperrecorder.ui.settings.LanguageSettingsScreen
import com.example.whisperrecorder.ui.settings.PerformanceSettingsScreen
import com.example.whisperrecorder.ui.settings.MicrophoneProcessingSettingsScreen
import com.example.whisperrecorder.ui.settings.DiagnosticsScreen
import com.example.whisperrecorder.ui.settings.OfflineSpeakerSettingsScreen
import com.example.whisperrecorder.speaker.SpeakerModels
import com.example.whisperrecorder.ui.SpeakerTranscriptMenu
import com.example.whisperrecorder.speaker.SpeakerDiarizationService
import com.example.whisperrecorder.speaker.SpeakerDiarizationRuntime
import com.example.whisperrecorder.speaker.SpeakerTranscript
import com.example.whisperrecorder.speaker.SpeakerDiagnostics
import com.example.whisperrecorder.speaker.SpeakerFailureText
import com.example.whisperrecorder.ui.setup.SetupScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_RECORDING_ID = "open_recording_id"
    }
    private val requestedRecordingId = mutableStateOf<String?>(null)
    private val maintenanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var maintenanceJob: kotlinx.coroutines.Job? = null
    private val libraryIntegrityRevision = mutableIntStateOf(0)
    private var recordScreenVisible = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRecordingId.value = intent?.getStringExtra(EXTRA_OPEN_RECORDING_ID)
        setContent { WhisperRecorderTheme { App() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRecordingId.value = intent.getStringExtra(EXTRA_OPEN_RECORDING_ID)
    }
    override fun onStart() {
        super.onStart()
        if (recordScreenVisible && RecordingRuntime.state.value.active) setRecordingVisualizerEnabled(true)
    }
    override fun onStop() {
        if (RecordingRuntime.state.value.active) setRecordingVisualizerEnabled(false)
        super.onStop()
    }
    private fun setRecordingVisualizerEnabled(enabled: Boolean) {
        RecordingRuntime.setVisualizerVisible(enabled && RecordingRuntime.state.value.active)
    }
    override fun onResume() {
        super.onResume()
        maintenanceJob?.cancel()
        maintenanceJob = maintenanceScope.launch {
            AppPrefs.getSaveTree(this@MainActivity)?.let { tree ->
                runCatching { WorkspaceStorage.refresh(this@MainActivity, tree) }
                    .onFailure { DiagnosticLogger.append(this@MainActivity, "workspace refresh failed: ${it.message}") }
            }
            RecordingStore.reconcileAvailability(this@MainActivity)
            withContext(Dispatchers.Main) { libraryIntegrityRevision.intValue++ }
        }
    }
    override fun onDestroy() {
        maintenanceScope.cancel()
        super.onDestroy()
    }
    @Composable
    private fun App() {
        var setupComplete by rememberSaveable { mutableStateOf(AppPrefs.isSetupComplete(this)) }
        if (!setupComplete) {
            SetupScreen(
                onComplete = {
                    AppPrefs.setSetupComplete(this@MainActivity, true)
                    setupComplete = true
                },
                onStartModelDownload = ::startModelDownload,
                onCancelModelDownload = ::cancelModelDownload
            )
            return
        }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var selectedRecordingId by rememberSaveable { mutableStateOf<String?>(null) }
        var showExitDialog by rememberSaveable { mutableStateOf(false) }
        // StateFlow is observable Compose state here; never read StateFlow.value as
        // the initial value of a collection while composing (Android lint).
        val recordingStatus by RecordingRuntime.state.collectAsState()
        val transcriptionStatus by TranscriptionRuntime.state.collectAsState()
        val modelDownloadStatus by ModelDownloadRuntime.state.collectAsState()
        val backgroundWorkActive = recordingStatus.active || transcriptionStatus.active || modelDownloadStatus.active
        val requestedId = requestedRecordingId.value
        LaunchedEffect(requestedId) {
            if (requestedId != null) {
                selectedRecordingId = requestedId
                showSettings = false
                requestedRecordingId.value = null
            }
        }
        BackHandler(enabled = showSettings) { showSettings = false }
        BackHandler(enabled = selectedRecordingId != null && !showSettings) { selectedRecordingId = null }
        BackHandler(enabled = !showSettings && selectedRecordingId == null) {
            if (tab != 0) {
                tab = 0
            } else if (AppPrefs.isConfirmExitEnabled(this@MainActivity)) {
                showExitDialog = true
            } else {
                finishAndRemoveTask()
            }
        }
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Exit Whisper Recorder?") },
                text = {
                    Text(
                        if (backgroundWorkActive) {
                            "Whisper Recorder will close and be removed from Recents. Active recording, transcription, or model download work will continue in the background and remain available from its notification."
                        } else {
                            "Are you sure you want to exit the app?"
                        }
                    )
                },
                confirmButton = { Button(onClick = { showExitDialog = false; finishAndRemoveTask() }) { Text("Exit") } },
                dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } }
            )
        }

        when {
            showSettings -> SettingsScreen(onBack = { showSettings = false })
            selectedRecordingId != null -> RecordingDetailScreen(
                recordingId = selectedRecordingId!!,
                onBack = { selectedRecordingId = null }
            )
            else -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (tab == 0) "Whisper Recorder" else if (tab == 1) "Library" else "Prompts") },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = "Settings"
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("●") }, label = { Text("Record") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("▤") }, label = { Text("Library") })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("✦") }, label = { Text("Prompts") })
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (tab) {
                        0 -> RecordScreen(onOpenSettings = { showSettings = true })
                        1 -> LibraryScreen(
                            onOpen = { selectedRecordingId = it.id },
                            onOpenSettings = { showSettings = true }
                        )
                        else -> PromptsScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun RecordScreen(onOpenSettings: () -> Unit) {
        val state by RecordingRuntime.state.collectAsState()
        val liveLevel by RecordingRuntime.liveLevel.collectAsState()
        val scope = rememberCoroutineScope()
        val treeUri = AppPrefs.getSaveTree(this)
        val format = AppPrefs.getAudioFormat(this)
        var renameTarget by remember { mutableStateOf<RecordingItem?>(null) }
        var renameText by remember { mutableStateOf("") }

        DisposableEffect(Unit) {
            recordScreenVisible = true
            if (RecordingRuntime.state.value.active) setRecordingVisualizerEnabled(true)
            onDispose {
                recordScreenVisible = false
                if (RecordingRuntime.state.value.active) setRecordingVisualizerEnabled(false)
            }
        }
        LaunchedEffect(state.active) {
            if (state.active && recordScreenVisible) setRecordingVisualizerEnabled(true)
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
        LaunchedEffect(Unit) {
            val needs = buildList {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
                if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (needs.isNotEmpty()) permissionLauncher.launch(needs.toTypedArray())
        }
        LaunchedEffect(state.lastSavedRecordingId) {
            val id = state.lastSavedRecordingId ?: AppPrefs.getPendingRenameRecordingId(this@MainActivity) ?: return@LaunchedEffect
            val saved = RecordingStore.list(this@MainActivity).firstOrNull { it.id == id }
            if (saved != null) {
                renameTarget = saved
                renameText = saved.title
            } else {
                AppPrefs.setPendingRenameRecordingId(this@MainActivity, null)
            }
        }

        Box(Modifier.fillMaxSize().padding(20.dp)) {
            if (!state.active) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("Ready to record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            val tree = treeUri
                            if (tree == null) {
                                onOpenSettings()
                                return@Button
                            }
                            ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, RecordingService::class.java).apply {
                                action = RecordingService.ACTION_START
                                putExtra(RecordingService.EXTRA_TREE, tree.toString())
                                putExtra(RecordingService.EXTRA_FORMAT, format)
                            })
                        },
                        modifier = Modifier.size(176.dp),
                        shape = CircleShape
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("●", style = MaterialTheme.typography.displaySmall)
                            Text("RECORD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Tap to start", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(if (state.paused) "Paused" else "● Recording", style = MaterialTheme.typography.titleLarge, color = if (state.paused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                    Text(formatElapsed(state.elapsedMs), style = MaterialTheme.typography.displayMedium)
                    RecordingWaveform(
                        level = liveLevel,
                        paused = state.paused,
                        modifier = Modifier.fillMaxWidth(0.84f)
                    )
                    Text("Recording continues when the screen is locked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.micWarning) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Microphone input may be unavailable", fontWeight = FontWeight.Bold)
                                Text("Input has been silent for at least 15 seconds. Another app may be using the microphone, or the room may simply be silent. Recording will continue.")
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                startService(Intent(this@MainActivity, RecordingService::class.java).setAction(if (state.paused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE))
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) { Text(if (state.paused) "Resume" else "Pause") }
                        Button(
                            onClick = { startService(Intent(this@MainActivity, RecordingService::class.java).setAction(RecordingService.ACTION_STOP)) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Stop") }
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomCenter)) }
        }

        renameTarget?.let { target ->
            AlertDialog(
                onDismissRequest = {
                    renameTarget = null
                    RecordingRuntime.consumeLastSaved()
                    AppPrefs.setPendingRenameRecordingId(this@MainActivity, null)
                },
                title = { Text("Recording saved") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Give this recording a name, or keep the automatic name.")
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("Recording name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clean = sanitizeRecordingTitle(renameText).ifBlank { target.title }
                            renameTarget = null
                            RecordingRuntime.consumeLastSaved()
                            AppPrefs.setPendingRenameRecordingId(this@MainActivity, null)
                            scope.launch(Dispatchers.IO) {
                                val renamedUri = target.audioUri?.let { raw ->
                                    val uri = Uri.parse(raw)
                                    val ext = SafStorage.extensionForDocument(this@MainActivity, uri, target.mime)
                                    SafStorage.renameDocument(this@MainActivity, uri, "$clean.$ext")
                                }
                                RecordingStore.rename(this@MainActivity, target.id, clean, renamedUri?.toString())
                            }
                        }
                    ) { Text("Save name") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        renameTarget = null
                        RecordingRuntime.consumeLastSaved()
                        AppPrefs.setPendingRenameRecordingId(this@MainActivity, null)
                    }) { Text("Skip") }
                }
            )
        }
    }

    @Composable
    private fun LibraryScreen(onOpen: (RecordingItem) -> Unit, onOpenSettings: () -> Unit) {
        val scope = rememberCoroutineScope()
        var recordings by remember { mutableStateOf(RecordingStore.list(this)) }
        var revision by remember { mutableIntStateOf(0) }
        var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        var selectionMode by remember { mutableStateOf(false) }
        var showBatchDelete by remember { mutableStateOf(false) }
        var removeFromLibraryOnly by remember { mutableStateOf(false) }
        var deleteTranscriptsToo by remember { mutableStateOf(true) }
        var batchDeleting by remember { mutableStateOf(false) }
        var batchMessage by remember { mutableStateOf<String?>(null) }
        val tx by TranscriptionRuntime.state.collectAsState()
        val verboseDiagnostics = remember { AppPrefs.isVerboseDiagnostics(this@MainActivity) }
        val libraryRecordingStatus by RecordingRuntime.state.collectAsState()
        val lastSavedRecordingId = libraryRecordingStatus.lastSavedRecordingId
        val integrityRevision = libraryIntegrityRevision.intValue
        val modelPath by produceState<String?>(null, integrityRevision) {
            value = withContext(Dispatchers.IO) { ModelSelectionResolver.activeModelPath(this@MainActivity) }
        }
        var importMessage by remember { mutableStateOf<String?>(null) }
        val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) scope.launch {
                importMessage = "Adding media…"
                try {
                    val item = addMediaReference(uri)
                    importMessage = "Added ${if (item.isVideo) "video" else "audio"}: ${item.title}"
                    recordings = RecordingStore.list(this@MainActivity)
                } catch (t: Throwable) {
                    importMessage = "Import failed: ${t.message}"
                }
            }
        }

        fun refresh() {
            recordings = RecordingStore.list(this@MainActivity)
            revision++
        }

        LaunchedEffect(lastSavedRecordingId, tx.completedRecordingId, revision, integrityRevision) {
            recordings = RecordingStore.list(this@MainActivity)
        }

        BackHandler(enabled = selectionMode) {
            selectedIds = emptySet()
            selectionMode = false
        }

        val selectedItems = recordings.filter { it.id in selectedIds }
        val transcribableSelected = selectedItems.filter { it.audioUri != null && it.transcriptUri == null }
        val untranscribedAll = recordings
            .filter { it.audioUri != null && it.transcriptUri == null && !it.hasEverBeenTranscribed }
            .sortedBy { it.createdAt }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (!selectionMode) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onClick = { mediaPicker.launch(arrayOf("audio/*", "video/*")) }) {
                            Text("+ Add audio/video")
                        }
                    }
                }
                importMessage?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            if (selectionMode) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedIds.size} selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    selectedIds = if (selectedIds.size == recordings.size) emptySet() else recordings.map { it.id }.toSet()
                                }) { Text(if (selectedIds.size == recordings.size) "Clear all" else "Select all") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (modelPath == null) {
                                            onOpenSettings()
                                        } else {
                                            transcribableSelected.sortedBy { it.createdAt }.forEach { startTranscription(it, modelPath) }
                                            selectedIds = emptySet()
                                            selectionMode = false
                                        }
                                    },
                                    enabled = transcribableSelected.isNotEmpty(),
                                    modifier = Modifier.weight(1.35f)
                                ) { Text("Transcribe", maxLines = 1, softWrap = false) }
                                OutlinedButton(
                                    onClick = { ShareManager.shareMany(this@MainActivity, selectedItems) },
                                    enabled = selectedItems.any { it.audioUri != null || it.transcriptUri != null },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Share") }
                                OutlinedButton(
                                    onClick = {
                                        removeFromLibraryOnly = false
                                        deleteTranscriptsToo = true
                                        showBatchDelete = true
                                    },
                                    enabled = selectedItems.isNotEmpty(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Delete") }
                            }
                            if (modelPath == null && transcribableSelected.isNotEmpty()) {
                                Text("Select a transcription model in Settings before transcribing.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            batchMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            if (untranscribedAll.size > 1 && !selectionMode) {
                item {
                    FilledTonalButton(
                        onClick = {
                            if (modelPath == null) {
                                onOpenSettings()
                            } else {
                                untranscribedAll.forEach { startTranscription(it, modelPath) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (modelPath == null) "Choose model to transcribe all" else "Transcribe all (${untranscribedAll.size}) • oldest first")
                    }
                }
            }

            if (recordings.isNotEmpty() && !selectionMode) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Tap a recording for details, or select several for batch actions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectionMode = true }) { Text("Select") }
                    }
                }
            }

            if (recordings.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No recordings yet", style = MaterialTheme.typography.titleLarge)
                            Text("Your recordings and transcripts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(recordings, key = { it.id }) { item ->
                val selected = item.id in selectedIds
                Card(
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (selected) selectedIds - item.id else selectedIds + item.id
                            } else {
                                onOpen(item)
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            selectedIds = selectedIds + item.id
                        }
                    ),
                    colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val date = formatDateTime(item.createdAt)
                                Text(
                                    if (item.audioUri != null) "${if (item.isVideo) "Video • " else ""}${formatElapsed(item.durationMs)} • $date" else "Media removed • $date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.transcriptUri != null && item.transcriptionElapsedMs != null) {
                                    Text(
                                        "Transcribed in ${formatTranscriptionDuration(item.transcriptionElapsedMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (selectionMode) Checkbox(checked = selected, onCheckedChange = {
                                selectedIds = if (it) selectedIds + item.id else selectedIds - item.id
                            }) else Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        when {
                            tx.active && tx.recordingId == item.id -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tx.stage.ifBlank { "Working" }, modifier = Modifier.weight(1f))
                                    if (tx.qwenWindowsTotal > 0) {
                                        Text(if (tx.qwenPhase == "Decoding")
                                            "${tx.qwenDecoderDone}/${tx.qwenWindowsTotal} decoded"
                                        else "${tx.qwenWindowsDone}/${tx.qwenWindowsTotal} encoded",
                                            style = MaterialTheme.typography.bodySmall)
                                    } else if (tx.totalChunks > 1 && tx.chunk > 0) {
                                        Text("${tx.chunk}/${tx.totalChunks}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (tx.stage == "Transcribing" && tx.qwenWindowsTotal > 0) {
                                    CompactProgressBar(
                                        QwenWorkProgress.fraction(tx.qwenWindowsDone, tx.qwenDecoderDone, tx.qwenWindowsTotal, tx.chunk, tx.totalChunks),
                                        Modifier.fillMaxWidth())
                                } else if (tx.totalChunks > 1 && tx.progress in 1..99) {
                                    CompactProgressBar(tx.progress / 100f, Modifier.fillMaxWidth())
                                }
                                val verboseStatus = verboseDiagnostics && tx.detail.isNotBlank()
                                if (verboseStatus) {
                                    Text(
                                        tx.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val elapsedLabel = if (tx.elapsedMs > 0L) "Elapsed ${formatElapsed(tx.elapsedMs)}" else "Starting…"
                                    val segmentLabel = when {
                                        tx.qwenWindowsTotal > 0 && tx.qwenPhase == "Decoding" -> "Decoded ${tx.qwenDecoderDone} of ${tx.qwenWindowsTotal} Qwen windows • $elapsedLabel"
                                        tx.qwenWindowsTotal > 0 -> "Encoded ${tx.qwenWindowsDone} of ${tx.qwenWindowsTotal} Qwen windows • $elapsedLabel"
                                        tx.totalChunks > 1 && tx.chunk > 0 -> "Segment ${tx.chunk} of ${tx.totalChunks} • $elapsedLabel"
                                        else -> elapsedLabel
                                    }
                                    Text(
                                        segmentLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (tx.stage == "Stopping") {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        "Stopping safely…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            startService(
                                                Intent(this@MainActivity, TranscriptionService::class.java)
                                                    .setAction(TranscriptionService.ACTION_CANCEL)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Stop transcription") }
                                }
                            }
                            (item.transcriptUri != null || item.speakerTranscriptUri != null) && item.audioUri == null -> Text("✓ Transcript retained • Media removed", color = MaterialTheme.colorScheme.primary)
                            item.transcriptUri != null -> Text("✓ Transcript ready", color = MaterialTheme.colorScheme.primary)
                            item.transcriptMissing -> Text("⚠ Transcript file missing • Retranscribe available", color = MaterialTheme.colorScheme.error)
                            else -> Text("Not transcribed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (!selectionMode && item.audioUri != null && item.transcriptUri == null && !(tx.active && tx.recordingId == item.id)) {
                            Button(
                                onClick = {
                                    if (modelPath == null) onOpenSettings() else startTranscription(item, modelPath)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when {
                                        modelPath == null && item.transcriptMissing -> "Choose model to retranscribe"
                                        modelPath == null -> "Choose model to transcribe"
                                        item.transcriptMissing -> "Retranscribe"
                                        else -> "Transcribe"
                                    }
                                )
                            }
                        }

                        if (!selectionMode) {
                            Text(
                                "Tap this recording for playback, sharing and more options",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showBatchDelete) {
            val anyTranscript = selectedItems.any { it.transcriptUri != null || it.speakerTranscriptUri != null }
            val transcriptOnlyCount = selectedItems.count { it.audioUri == null && (it.transcriptUri != null || it.speakerTranscriptUri != null) }
            val appOwnedAudioCount = selectedItems.count { it.audioUri != null && it.audioOwnedByApp }
            val referencedAudioCount = selectedItems.count { it.audioUri != null && !it.audioOwnedByApp }
            AlertDialog(
                onDismissRequest = { if (!batchDeleting) showBatchDelete = false },
                title = { Text("Remove ${selectedItems.size} selected item${if (selectedItems.size == 1) "" else "s"}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { removeFromLibraryOnly = true }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = removeFromLibraryOnly, onClick = { removeFromLibraryOnly = true })
                            Column {
                                Text("Remove from Library only")
                                Text("Keep all recording and transcript files on the device.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(Modifier.fillMaxWidth().clickable { removeFromLibraryOnly = false }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !removeFromLibraryOnly, onClick = { removeFromLibraryOnly = false })
                            Column {
                                Text("Delete files where allowed")
                                Text("Delete app-owned recordings. Media imported from outside Whisper Recorder is never deleted.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!removeFromLibraryOnly) {
                            when {
                                appOwnedAudioCount > 0 && referencedAudioCount > 0 ->
                                    Text("App-created recordings will be deleted. Media added from outside Whisper Recorder is not deleted from its source location; only its Library entry is removed.")
                                appOwnedAudioCount > 0 ->
                                    Text("Selected app recordings will be permanently deleted.")
                                referencedAudioCount > 0 ->
                                    Text("These media files are outside Whisper Recorder's save location, so the source files will not be deleted. Remove the selected entries from the Library?")
                            }
                            if (anyTranscript) {
                                Row(Modifier.fillMaxWidth().clickable { deleteTranscriptsToo = false }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = !deleteTranscriptsToo, onClick = { deleteTranscriptsToo = false })
                                    Column {
                                        Text("Keep transcripts")
                                        Text("Keep transcripts when media is removed from Library.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(Modifier.fillMaxWidth().clickable { deleteTranscriptsToo = true }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = deleteTranscriptsToo, onClick = { deleteTranscriptsToo = true })
                                    Column {
                                        Text("Delete transcripts too")
                                        Text("Delete transcripts together with the selected media entries.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (transcriptOnlyCount > 0 && !deleteTranscriptsToo) {
                                    Text("Transcript-only entries have no media to remove, so those ${transcriptOnlyCount} selected transcript${if (transcriptOnlyCount == 1) "" else "s"} will remain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (batchDeleting) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val snapshot = selectedItems
                            val libraryOnly = removeFromLibraryOnly
                            val removeTranscripts = deleteTranscriptsToo
                            batchDeleting = true
                            val currentTxSelected = tx.active && tx.recordingId in selectedIds
                            if (currentTxSelected) {
                                startService(Intent(this@MainActivity, TranscriptionService::class.java).apply {
                                    action = TranscriptionService.ACTION_CANCEL
                                    putExtra(TranscriptionService.EXTRA_KEEP_QUEUE, true)
                                })
                            }
                            scope.launch {
                                val results = withContext(Dispatchers.IO) {
                                    snapshot.map { entry -> runCatching {
                                        if (libraryOnly) {
                                            RecordingDeletionManager.removeFromLibrary(this@MainActivity, entry)
                                        } else {
                                            when {
                                                entry.audioUri != null -> RecordingDeletionManager.deleteRecording(this@MainActivity, entry, removeTranscripts)
                                                (entry.transcriptUri != null || entry.speakerTranscriptUri != null) && removeTranscripts -> RecordingDeletionManager.deleteTranscriptOnly(this@MainActivity, entry)
                                                else -> RecordingDeletionManager.Result(true)
                                            }
                                        }
                                    }.getOrElse { error ->
                                        runCatching { DiagnosticLogger.appendError(this@MainActivity, "batch-delete", error) }
                                        RecordingDeletionManager.Result(false, message = "Unexpected error; check diagnostics and retry.")
                                    } }
                                }
                                val failed = results.filter { !it.success || it.partial }
                                batchMessage = if (failed.isEmpty()) {
                                    if (libraryOnly) "Selected entries removed from Library. Files were kept." else "Selected files deleted (or retained as requested)."
                                } else {
                                    "${failed.size} item(s) not fully deleted. ${failed.first().message.orEmpty()}"
                                }
                                batchDeleting = false
                                showBatchDelete = false
                                selectedIds = emptySet()
                                selectionMode = false
                                refresh()
                            }
                        },
                        enabled = !batchDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (removeFromLibraryOnly) "Remove entries" else "Delete") }
                },
                dismissButton = { TextButton(onClick = { showBatchDelete = false }, enabled = !batchDeleting) { Text("Cancel") } }
            )
        }
    }

    @Composable
    private fun RecordingDetailScreen(recordingId: String, onBack: () -> Unit) {
        var item by remember(recordingId) { mutableStateOf(RecordingStore.find(this, recordingId)) }
        val tx by TranscriptionRuntime.state.collectAsState()
        val verboseDiagnostics = remember { AppPrefs.isVerboseDiagnostics(this@MainActivity) }
        val modelPath by produceState<String?>(null, recordingId) {
            value = withContext(Dispatchers.IO) { ModelSelectionResolver.activeModelPath(this@MainActivity) }
        }
        val scope = rememberCoroutineScope()
        var transcript by remember { mutableStateOf<String?>(null) }
        var transcriptLoading by remember { mutableStateOf(false) }
        val speakerRuntime by SpeakerDiarizationRuntime.state.collectAsState()
        val speakerRunning = speakerRuntime.active && speakerRuntime.recordingId == recordingId
        var showSpeakerMenu by rememberSaveable(recordingId) { mutableStateOf(false) }
        var expectedSpeakers by remember(recordingId) { mutableIntStateOf(AppPrefs.speakerExpectedCount(this@MainActivity)) }
        var showSpeakerNamesDialog by remember(recordingId) { mutableStateOf(false) }
        var speakerNamesText by remember(recordingId) { mutableStateOf("") }
        var speakerPreviewRevision by remember(recordingId) { mutableIntStateOf(0) }
        var speakerMessage by remember(recordingId) { mutableStateOf<String?>(null) }
        var speakerDiagnostics by remember(recordingId) {
            mutableStateOf(SpeakerDiagnostics.read(this@MainActivity, recordingId))
        }
        var showSpeakerDiagnostics by remember(recordingId) { mutableStateOf(false) }
        var speakerUri by remember(recordingId) {
            mutableStateOf(RecordingStore.find(this@MainActivity, recordingId)?.speakerTranscriptUri?.let(Uri::parse))
        }
        var showFullTranscript by rememberSaveable(recordingId) { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var deleteTranscriptToo by remember(recordingId) { mutableStateOf(true) }
        var deleting by remember { mutableStateOf(false) }
        var deleteMessage by remember { mutableStateOf<String?>(null) }
        var showRenameDialog by remember { mutableStateOf(false) }
        var renameText by remember(recordingId) { mutableStateOf("") }
        var detailClockMs by remember(recordingId) { mutableLongStateOf(System.currentTimeMillis()) }
        var stableTranscriptionStartedAtMs by rememberSaveable(recordingId) { mutableLongStateOf(0L) }
        val integrityRevision = libraryIntegrityRevision.intValue

        // Keep the detail-screen clock independent from native Whisper heartbeat updates. Native
        // inference can run for many seconds without emitting a state object, but the UI timer must
        // still advance continuously.
        LaunchedEffect(recordingId, tx.active, tx.recordingId) {
            if (tx.active && tx.recordingId == recordingId) {
                while (true) {
                    detailClockMs = System.currentTimeMillis()
                    delay(1_000)
                }
            }
        }
        LaunchedEffect(tx.active, tx.recordingId, tx.startedAtMs) {
            if (tx.active && tx.recordingId == recordingId && tx.startedAtMs > 0L) {
                stableTranscriptionStartedAtMs = tx.startedAtMs
            }
            if (!tx.active && tx.recordingId == recordingId) {
                stableTranscriptionStartedAtMs = 0L
            }
        }

        BackHandler(enabled = showDeleteDialog && !deleting) { showDeleteDialog = false }
        BackHandler(enabled = showFullTranscript) { showFullTranscript = false }
        BackHandler(enabled = showSpeakerMenu) { showSpeakerMenu = false }

        LaunchedEffect(speakerRuntime) {
            if (speakerRuntime.recordingId == recordingId) {
                speakerMessage = speakerRuntime.message
                speakerRuntime.savedUri?.let { raw ->
                    speakerUri = Uri.parse(raw)
                    item = RecordingStore.find(this@MainActivity, recordingId)
                    speakerDiagnostics = null
                }
                speakerRuntime.diagnosticReport?.let { report -> speakerDiagnostics = report }
            }
        }
        LaunchedEffect(tx.completedRecordingId, item?.transcriptUri, integrityRevision) {
            item = RecordingStore.find(this@MainActivity, recordingId)
        }
        LaunchedEffect(item?.transcriptUri) {
            val uri = item?.transcriptUri
            if (uri == null) {
                transcript = null
                transcriptLoading = false
                return@LaunchedEffect
            }
            transcriptLoading = true
            transcript = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            transcriptLoading = false
        }

        val current = item
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recording not found") }
            return
        }

        if (showFullTranscript && transcript != null && current.transcriptUri != null) {
            TranscriptViewerScreen(
                title = current.title,
                transcript = transcript!!,
                audioUri = current.audioUri?.let(Uri::parse),
                audioDurationMs = current.durationMs,
                transcriptTimingPath = current.transcriptTimingPath,
                transcriptSyncProfileId = current.transcriptSyncProfileId,
                transcriptSyncProfileLabel = current.transcriptSyncProfileLabel,
                sourceLabel = if (current.isVideo) "Video audio" else "Audio",
                onBack = { showFullTranscript = false }
            )
            return
        }

        val generateSpeakerTranscript: () -> Unit = {
            speakerMessage = "Starting offline speaker identification…"
            val intent = Intent(this@MainActivity, SpeakerDiarizationService::class.java).apply {
                action = SpeakerDiarizationService.ACTION_CREATE
                putExtra(SpeakerDiarizationService.EXTRA_RECORDING_ID, current.id)
                putExtra(SpeakerDiarizationService.EXTRA_EXPECTED, expectedSpeakers)
            }
            ContextCompat.startForegroundService(this@MainActivity, intent)
        }
        if (showSpeakerMenu) {
            SpeakerTranscriptMenu(
                context = this@MainActivity, recording = current,
                speakerUri = speakerUri ?: current.speakerTranscriptUri?.let(Uri::parse),
                expectedSpeakers = expectedSpeakers, running = speakerRunning,
                busy = tx.active || speakerRuntime.active, modelsInstalled = SpeakerModels.installed(this@MainActivity),
                message = speakerMessage, hasDiagnostics = speakerDiagnostics != null,
                previewRevision = speakerPreviewRevision,
                onBack = { showSpeakerMenu = false },
                onExpectedSpeakers = { count ->
                    expectedSpeakers = count
                    AppPrefs.setSpeakerExpectedCount(this@MainActivity, count)
                }, onCreate = generateSpeakerTranscript,
                onShareTranscript = { uri ->
                    ShareManager.shareTranscriptFile(this@MainActivity, "${current.title}-speaker", uri)
                }, onRenameLabels = { speakerNamesText = ""; showSpeakerNamesDialog = true },
                onViewDiagnostics = { showSpeakerDiagnostics = true }
            )
        } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recording") },
                    navigationIcon = { BackArrowButton(onBack) },
                    actions = {
                        TextButton(onClick = { showDeleteDialog = true }, enabled = !deleting) {
                            Text(
                                when {
                                    current.audioUri == null -> "Delete transcript"
                                    !current.audioOwnedByApp -> "Remove"
                                    else -> "Delete"
                                },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        ) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { renameText = current.title; showRenameDialog = true },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = { renameText = current.title; showRenameDialog = true },
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Rename", maxLines = 1)
                    }
                }
                Text(
                    formatDateTime(current.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (current.audioUri != null && !deleting) {
                    SynchronizedAudioPlayer(
                        audioUri = Uri.parse(current.audioUri),
                        audioDurationMs = current.durationMs,
                        transcriptComplete = current.transcriptUri != null,
                        transcriptTimingPath = current.transcriptTimingPath,
                        transcriptSyncProfileId = current.transcriptSyncProfileId,
                        sourceLabel = if (current.isVideo) "Video audio" else "Audio"
                    )
                } else if (current.audioUri != null && deleting) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Removing media…", style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Media unavailable", style = MaterialTheme.typography.titleMedium)
                            Text("The transcript is still available. Media playback and re-transcription are no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Transcript", style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f))
                            if (current.transcriptUri != null) {
                                TranscriptShareIcon(R.drawable.ic_share_transcript, "Share transcript",
                                    enabled = transcript != null, onClick = {
                                        ShareManager.shareTranscriptFile(this@MainActivity, current.title,
                                            Uri.parse(current.transcriptUri))
                                    })
                            }
                            current.audioUri?.let { media ->
                                TranscriptShareIcon(if (current.isVideo) R.drawable.ic_share_video else R.drawable.ic_share_media,
                                    if (current.isVideo) "Share video" else "Share audio",
                                    onClick = { ShareManager.share(this@MainActivity, Uri.parse(media), current.mime) })
                            }
                        }
                        when {
                            tx.active && tx.recordingId == current.id -> {
                                Text(tx.stage.ifBlank { "Transcribing" }, fontWeight = FontWeight.SemiBold)
                                val determinate = tx.qwenWindowsTotal == 0 && tx.totalChunks > 1 && tx.progress in 1..99
                                if (tx.stage == "Transcribing" && tx.qwenWindowsTotal > 0) {
                                    CompactProgressBar(
                                        QwenWorkProgress.fraction(tx.qwenWindowsDone, tx.qwenDecoderDone, tx.qwenWindowsTotal, tx.chunk, tx.totalChunks),
                                        Modifier.fillMaxWidth())
                                } else if (determinate) {
                                    CompactProgressBar(tx.progress / 100f, Modifier.fillMaxWidth())
                                }
                                if (tx.stage == "Transcribing" && tx.totalChunks > 0) {
                                    Text(
                                        when {
                                            tx.qwenWindowsTotal > 0 && tx.qwenPhase == "Decoding" -> "Decoded ${tx.qwenDecoderDone} of ${tx.qwenWindowsTotal} Qwen audio windows"
                                            tx.qwenWindowsTotal > 0 -> "Encoded ${tx.qwenWindowsDone} of ${tx.qwenWindowsTotal} Qwen audio windows"
                                            tx.totalChunks == 1 -> "Segment 1 of 1"
                                            else -> "Processing ${formatElapsed(tx.processedMs)} of ${formatElapsed(tx.totalMs)} • Segment ${tx.chunk} of ${tx.totalChunks}"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (tx.detail.isNotBlank()) {
                                    Text(tx.detail, style = MaterialTheme.typography.bodySmall)
                                }
                                val timerStart = stableTranscriptionStartedAtMs.takeIf { it > 0L } ?: tx.startedAtMs
                                val overallElapsed = if (timerStart > 0L) (detailClockMs - timerStart).coerceAtLeast(0L) else tx.elapsedMs
                                Text(
                                    if (overallElapsed > 0L) "Elapsed ${formatElapsed(overallElapsed)}" else "Elapsed time unavailable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.modelName.isNotBlank()) {
                                    Text("Model: ${tx.modelName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (verboseDiagnostics) {
                                    if (tx.detail.isNotBlank()) Text("Detail: ${tx.detail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (tx.elapsedMs > 0) Text("Inference elapsed: ${formatElapsed(tx.elapsedMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Diagnostics are being written to Settings → Diagnostics.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (tx.stage == "Stopping") {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        "Stopping safely…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = { startService(Intent(this@MainActivity, TranscriptionService::class.java).setAction(TranscriptionService.ACTION_CANCEL)) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Stop transcription") }
                                }
                            }
                            current.transcriptUri != null -> {
                                Text("✓ Transcription complete", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                if (transcriptLoading) CircularProgressIndicator()
                                transcript?.let { body ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(body, modifier = Modifier.padding(14.dp), maxLines = 12, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Button(
                                    onClick = { showFullTranscript = true },
                                    enabled = transcript != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("View full transcript") }
                            }
                            current.audioUri != null -> {
                                if (current.transcriptMissing) {
                                    Text("Transcript file missing", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "The media file is still available. You can transcribe it again to recreate the transcript.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text("No transcript yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { startTranscription(current, modelPath) },
                                    enabled = modelPath != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (current.transcriptMissing) {
                                            if (current.isVideo) "Retranscribe video" else "Retranscribe audio"
                                        } else {
                                            if (current.isVideo) "Transcribe video" else "Transcribe audio"
                                        }
                                    )
                                }
                                if (modelPath == null) {
                                    Text("Select a transcription model in Settings first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (tx.recordingId == current.id && tx.error != null && !tx.active) {
                            Text(tx.error!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (current.transcriptUri != null && current.audioUri != null) {
                    Button(onClick = { showSpeakerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Create speaker transcript")
                    }
                }
                if (current.transcriptUri != null && current.audioUri != null) {
                    OutlinedButton(
                        onClick = { startTranscription(current, modelPath) },
                        enabled = modelPath != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Transcribe again") }
                }
                deleteMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        }

        if (showSpeakerDiagnostics && speakerDiagnostics != null) {
            val report = speakerDiagnostics.orEmpty()
            AlertDialog(
                onDismissRequest = { showSpeakerDiagnostics = false },
                title = { Text("Speaker identification diagnostics") },
                text = {
                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            "Stored on your device. Nothing is uploaded automatically. " +
                                "Review technical error text and possible local paths before sharing.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        SelectionContainer {
                            Text(report, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Speaker diagnostics", report))
                        }) { Text("Copy") }
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "WhisperRecorder speaker diagnostics")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            startActivity(Intent.createChooser(send, "Share speaker diagnostics"))
                        }) { Text("Share") }
                        TextButton(onClick = { showSpeakerDiagnostics = false }) { Text("Close") }
                    }
                }
            )
        }

        if (showSpeakerNamesDialog) {
            AlertDialog(
                onDismissRequest = { showSpeakerNamesDialog = false },
                title = { Text("Rename speakers") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter one name per line in the form 1=Doctor or 2=Patient. Only the separate speaker transcript will change.")
                        OutlinedTextField(value = speakerNamesText, onValueChange = { speakerNamesText = it },
                            label = { Text("Speaker names") }, minLines = 3)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val requested = speakerNamesText.lines().mapNotNull { line ->
                            val number = line.substringBefore('=', "").trim().toIntOrNull()
                            val name = line.substringAfter('=', "").trim()
                            if (number != null && number in 1..30 && name.isNotBlank()) (number - 1) to name else null
                        }.toMap()
                        if (requested.isEmpty()) {
                            speakerMessage = "Enter names as 1=Doctor, one per line."
                        } else {
                            showSpeakerNamesDialog = false
                            val target = speakerUri ?: current.speakerTranscriptUri?.let(Uri::parse)
                            if (target != null) scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val previous = requireNotNull(contentResolver.openInputStream(target)) {
                                            "Speaker transcript not found"
                                        }.bufferedReader().use { it.readText() }
                                        val updated = SpeakerTranscript.renameLabels(previous, requested)
                                        require(updated != previous) { "No matching default speaker labels remain to rename" }
                                        requireNotNull(contentResolver.openOutputStream(target, "wt")) {
                                            "Cannot update speaker transcript"
                                        }.bufferedWriter().use { it.write(updated) }
                                    }
                                }
                                speakerMessage = result.fold({
                                    speakerPreviewRevision++
                                    "Speaker labels updated."
                                }, { "Could not rename speakers: ${it.message}" })
                            }
                        }
                    }) { Text("Save names") }
                },
                dismissButton = { TextButton(onClick = { showSpeakerNamesDialog = false }) { Text("Cancel") } }
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename recording") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Recording name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val clean = sanitizeRecordingTitle(renameText).ifBlank { current.title }
                        showRenameDialog = false
                        scope.launch(Dispatchers.IO) {
                            val renamedAudio = if (current.audioOwnedByApp) current.audioUri?.let { raw ->
                                val uri = Uri.parse(raw)
                                val ext = SafStorage.extensionForDocument(this@MainActivity, uri, current.mime)
                                SafStorage.renameDocument(this@MainActivity, uri, "$clean.$ext")
                            } else null
                            val renamedTranscript = current.transcriptUri?.let { raw ->
                                SafStorage.renameDocument(this@MainActivity, Uri.parse(raw), "$clean.txt")
                            }
                            RecordingStore.rename(
                                this@MainActivity, current.id, clean,
                                renamedAudio?.toString(), renamedTranscript?.toString()
                            )
                            withContext(Dispatchers.Main) {
                                item = RecordingStore.find(this@MainActivity, recordingId)
                            }
                        }
                    }) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
            )
        }


        if (showDeleteDialog) {
            val transcriptOnly = current.audioUri == null && (current.transcriptUri != null || current.speakerTranscriptUri != null)
            AlertDialog(
                onDismissRequest = { if (!deleting) showDeleteDialog = false },
                title = {
                    Text(
                        when {
                            transcriptOnly -> "Delete transcript?"
                            !current.audioOwnedByApp -> "Remove library entry?"
                            else -> "Delete recording?"
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (transcriptOnly) {
                            Text("This permanently deletes the remaining transcript. This action cannot be undone.")
                        } else if (current.transcriptUri != null || current.speakerTranscriptUri != null) {
                            Text(
                                if (current.audioOwnedByApp)
                                    "The app-created media file will be permanently deleted. What should happen to the transcript?"
                                else
                                    "This media file is outside Whisper Recorder's save location, so its source file will not be deleted. What should happen to the transcript when this Library entry is removed?"
                            )
                            Row(
                                Modifier.fillMaxWidth().clickable { deleteTranscriptToo = false },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !deleteTranscriptToo, onClick = { deleteTranscriptToo = false })
                                Column { Text("Keep transcript"); Text("Keep a transcript-only entry in Library.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Row(
                                Modifier.fillMaxWidth().clickable { deleteTranscriptToo = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = deleteTranscriptToo, onClick = { deleteTranscriptToo = true })
                                Column {
                                    Text("Delete transcript too")
                                    Text(
                                        if (current.audioOwnedByApp) "Delete both media and transcript." else "Delete the transcript and remove the Library entry. The external source media stays in place.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                if (current.audioOwnedByApp)
                                    "This permanently deletes the app-created media file. This action cannot be undone."
                                else
                                    "This media file is outside Whisper Recorder's save location, so the source file will not be deleted. Remove this entry from the Library?"
                            )
                        }
                        if (deleting) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            RecordingDeletionManager.markDeleting(current.id)
                            if (tx.active && tx.recordingId == current.id) {
                                startService(Intent(this@MainActivity, TranscriptionService::class.java).setAction(TranscriptionService.ACTION_CANCEL))
                            }
                            deleting = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    if (transcriptOnly) {
                                        RecordingDeletionManager.deleteTranscriptOnly(this@MainActivity, current)
                                    } else {
                                        RecordingDeletionManager.deleteRecording(this@MainActivity, current, deleteTranscriptToo)
                                    }
                                }
                                deleting = false
                                showDeleteDialog = false
                                deleteMessage = result.message
                                val refreshed = RecordingStore.list(this@MainActivity).firstOrNull { it.id == recordingId }
                                item = refreshed
                                if (refreshed == null) onBack()
                            }
                        },
                        enabled = !deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (!transcriptOnly && !current.audioOwnedByApp) "Remove from Library" else "Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }, enabled = !deleting) { Text("Cancel") } }
            )
        }
    }

    @Composable
    private fun SettingsScreen(onBack: () -> Unit) {
        val scope = rememberCoroutineScope()
        var subScreen by rememberSaveable { mutableStateOf<String?>(null) }
        BackHandler(enabled = subScreen != null) { subScreen = null }
        when (subScreen) {
            "models" -> { ModelsScreen(onBack = { subScreen = null }, onStartModelDownload = ::startModelDownload, onCancelModelDownload = ::cancelModelDownload); return }
            "performance" -> { PerformanceSettingsScreen(onBack = { subScreen = null }); return }
            "audio" -> { AudioFormatSettingsScreen(onBack = { subScreen = null }); return }
            "language" -> { LanguageSettingsScreen(onBack = { subScreen = null }); return }
            "diagnostics" -> { DiagnosticsScreen(onBack = { subScreen = null }); return }
            "microphone" -> { MicrophoneProcessingSettingsScreen(onBack = { subScreen = null }); return }
            "speakers" -> { OfflineSpeakerSettingsScreen(onBack = { subScreen = null }); return }
        }

        var treeUri by remember { mutableStateOf(AppPrefs.getSaveTree(this)) }
        var format by remember { mutableStateOf(AppPrefs.getAudioFormat(this)) }
        var performancePreset by remember { mutableStateOf(AppPrefs.getPerformancePreset(this)) }
        var storageStatus by remember { mutableStateOf<String?>(null) }
        val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                scope.launch {
                    storageStatus = "Preparing Whisperapp folders…"
                    val result = withContext(Dispatchers.IO) { runCatching { WorkspaceStorage.ensure(this@MainActivity, uri) } }
                    result.onSuccess {
                        AppPrefs.setSaveTree(this@MainActivity, uri)
                        treeUri = uri
                        storageStatus = "Working directory ready"
                    }.onFailure {
                        storageStatus = "Could not create Whisperapp folders: ${it.message}"
                    }
                }
            }
        }
        LaunchedEffect(subScreen) { if (subScreen == null) format = AppPrefs.getAudioFormat(this@MainActivity) }
        LaunchedEffect(subScreen) { if (subScreen == null) performancePreset = AppPrefs.getPerformancePreset(this@MainActivity) }

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val performanceSummary = if (AppPrefs.getTranscriptionEngine(this@MainActivity) in listOf("parakeet_npu", "omi_npu")) {
            "NPU Auto • provider-default baseline"
        } else when (performancePreset) {
            "high" -> "High Performance • ${minOf(6, cores)} threads"
            "max" -> "Max • $cores threads"
            "custom" -> "Custom • ${AppPrefs.effectiveTranscriptionThreads(this, cores)} threads"
            else -> "Balanced • ${minOf(4, cores)} threads"
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { BackArrowButton(onBack) }) }
        ) { pad ->
            LazyColumn(
                Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
            ) {
                item { SectionLabel("TRANSCRIPTION") }
                item { SettingCard("Models & downloads", "Download, import, select, or remove models", "Open") { subScreen = "models" } }
                item {
                    val engine = AppPrefs.getTranscriptionEngine(this@MainActivity)
                    val selectedFile = AppPrefs.getModelPath(this@MainActivity)?.let(::File)?.takeIf { it.isFile }
                    val pref = AppPrefs.getTranscriptionLanguage(this@MainActivity)
                    val localModel = LocalAsrModelManager.byEngine(engine)
                    val languageSummary = when {
                        engine == "parakeet_npu" && NpuPackageStore.archiveState(this@MainActivity) == NpuArchiveState.READY -> "${ModelManager.whisperLanguages[pref.takeUnless { it == "auto" } ?: "en"] ?: "English"} • 25 European languages • Qualcomm NPU"
                        engine == "parakeet_npu" -> "NPU package is incomplete • repair or reinstall"
                        engine == "omi_npu" && OmiNpuPackageStore.archiveState(this@MainActivity) == NpuArchiveState.READY -> "English • medical-domain • Qualcomm NPU"
                        engine == "omi_npu" -> "Omi NPU package is incomplete • repair or reinstall"
                        engine == "parakeet" && LocalAsrModelManager.byEngine(engine)?.let { LocalAsrModelManager.isInstalled(this@MainActivity, it) } == true -> "${ModelManager.whisperLanguages[pref.takeUnless { it == "auto" } ?: "en"] ?: "English"} • 25 European languages"
                        engine == "parakeet" -> "Parakeet files are incomplete • restore or reinstall"
                        localModel != null && !LocalAsrModelManager.isInstalled(this@MainActivity, localModel) -> "${localModel.name} is incomplete • restore or reinstall"
                        engine == "qwen3_06" -> "Auto-detect • 30 languages"
                        engine == "omi" -> "English • medical-domain • OpenCL GPU automatic"
                        selectedFile == null -> "Choose a model first"
                        ModelManager.isEnglishOnly(selectedFile) -> "English • imported model is English-only"
                        pref == "auto" -> "Auto-detect • multilingual Whisper model"
                        else -> "${ModelManager.whisperLanguages[pref] ?: pref} • multilingual Whisper model"
                    }
                    SettingCard("Transcription language", languageSummary, "Change") { subScreen = "language" }
                }
                item { SettingCard("Transcription performance", performanceSummary, "Open") { subScreen = "performance" } }
                item {
                    SettingCard("Offline speaker identification", "Optional local diarization • timestamps optional", "Open") {
                        subScreen = "speakers"
                    }
                }
                item { SectionLabel("STORAGE & RECORDING") }
                item {
                    SettingCard(
                        "Working directory",
                        treeUri?.let { "${it.lastPathSegment ?: "Selected folder"}/Whisperapp" } ?: "Not selected",
                        "Change"
                    ) { treePicker.launch(treeUri) }
                }
                storageStatus?.let { message ->
                    item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                item {
                    Text(
                        "Whisperapp keeps Recordings, Transcripts, Models, and Logs in separate folders. Prompt backups are saved wherever you choose in the system file picker. Existing files from older versions remain referenced in place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { SettingCard("Audio format", audioFormatLabel(format), "Change") { subScreen = "audio" } }
                item {
                    val micPreset = AppPrefs.getMicrophonePreset(this@MainActivity)
                    val micConfig = MicrophoneProcessing.resolve(this@MainActivity)
                    val summary = "${MicrophoneProcessing.presetLabel(micPreset)} • ${MicrophoneProcessing.sourceLabel(micConfig.source)}"
                    SettingCard("Microphone audio processing", summary, "Open") { subScreen = "microphone" }
                }

                item { SectionLabel("BEHAVIOR") }
                item {
                    var confirmExit by remember { mutableStateOf(AppPrefs.isConfirmExitEnabled(this@MainActivity)) }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Confirm before exiting", style = MaterialTheme.typography.titleMedium)
                                Text("When enabled, Back from Record asks before closing the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = confirmExit, onCheckedChange = { confirmExit = it; AppPrefs.setConfirmExitEnabled(this@MainActivity, it) })
                        }
                    }
                }

                item { SectionLabel("DIAGNOSTICS") }
                item { SettingCard("Diagnostics", if (AppPrefs.isVerboseDiagnostics(this@MainActivity)) "Verbose status enabled • export transcription logs" else "Export transcription logs and enable verbose status", "Open") { subScreen = "diagnostics" } }

                item { SectionLabel("ABOUT") }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Whisper Recorder", style = MaterialTheme.typography.titleMedium)
                            Text("Version ${getString(R.string.app_version_name)} (${getString(R.string.app_release_version)}) • parakeet.cpp OpenCL production runtime", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    private fun startModelDownload(modelId: String) {
        ContextCompat.startForegroundService(
            this,
            Intent(this, ModelDownloadService::class.java).apply {
                action = ModelDownloadService.ACTION_START
                putExtra(ModelDownloadService.EXTRA_MODEL_ID, modelId)
            }
        )
    }

    private fun cancelModelDownload() {
        startService(Intent(this, ModelDownloadService::class.java).setAction(ModelDownloadService.ACTION_CANCEL))
    }

    private suspend fun addMediaReference(sourceUri: Uri): RecordingItem = withContext(Dispatchers.IO) {
        val saveTreeUri = requireNotNull(AppPrefs.getSaveTree(this@MainActivity)) { "Choose a working directory in Settings first" }
        val source = MediaSourceInspector.inspect(this@MainActivity, sourceUri)

        try {
            contentResolver.takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (t: SecurityException) {
            throw IllegalArgumentException(
                "Whisper Recorder could not keep read access to this media file. Select it through a document provider that allows persistent access.",
                t
            )
        }

        val title = source.displayName.substringBeforeLast('.', source.displayName).take(120)
        val item = RecordingItem(
            id = UUID.randomUUID().toString(),
            title = title,
            audioUri = sourceUri.toString(),
            treeUri = saveTreeUri.toString(),
            mime = source.mime,
            durationMs = source.durationMs,
            createdAt = System.currentTimeMillis(),
            audioOwnedByApp = false,
            sourceKind = source.kind.id
        )
        RecordingStore.add(this@MainActivity, item)
        item
    }

    private fun startTranscription(item: RecordingItem, modelPath: String?) {
        val model = modelPath ?: return
        val mediaUri = item.mediaUri ?: return
        ContextCompat.startForegroundService(this, Intent(this, TranscriptionService::class.java).apply {
            action = TranscriptionService.ACTION_TRANSCRIBE
            putExtra(TranscriptionService.EXTRA_RECORDING_ID, item.id)
            putExtra(TranscriptionService.EXTRA_ENGINE, AppPrefs.getTranscriptionEngine(this@MainActivity))
            putExtra(TranscriptionService.EXTRA_MEDIA_URI, mediaUri)
            putExtra(TranscriptionService.EXTRA_TREE_URI, item.treeUri)
            putExtra(TranscriptionService.EXTRA_MODEL_PATH, model)
            putExtra(TranscriptionService.EXTRA_TITLE, item.title)
            putExtra(TranscriptionService.EXTRA_DURATION_MS, item.durationMs)
        })
    }

}
