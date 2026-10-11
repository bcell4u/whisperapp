package com.example.whisperrecorder.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.data.RecordingItem
import com.example.whisperrecorder.R
import androidx.compose.ui.res.painterResource
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import com.example.whisperrecorder.transcribe.SpeakerWordSync
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The speaker transcript has its own recording-style page; the parent has one entry only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerTranscriptMenu(
    context: Context,
    recording: RecordingItem,
    speakerUri: Uri?,
    expectedSpeakers: Int,
    running: Boolean,
    busy: Boolean,
    modelsInstalled: Boolean,
    message: String?,
    hasDiagnostics: Boolean,
    previewRevision: Int,
    onBack: () -> Unit,
    onExpectedSpeakers: (Int) -> Unit,
    onCreate: () -> Unit,
    onShareTranscript: (Uri) -> Unit,
    onRenameLabels: () -> Unit,
    onViewDiagnostics: () -> Unit
) {
    val speakerTimingPath = remember(recording.id, speakerUri, previewRevision) {
        if (speakerUri != null) TranscriptTimelineStore.speakerPath(context, recording.id)
            .takeIf { java.io.File(it).isFile } else null
    }
    var excerpt by remember(speakerUri, previewRevision) { mutableStateOf<String?>(null) }
    var fullText by remember(speakerUri, previewRevision) { mutableStateOf<String?>(null) }
    var previewTimingPath by remember(speakerUri, recording.transcriptTimingPath, previewRevision) {
        mutableStateOf<String?>(speakerTimingPath)
    }
    var viewingFull by remember { mutableStateOf(false) }
    BackHandler(enabled = viewingFull) { viewingFull = false }
    LaunchedEffect(viewingFull, speakerUri, previewRevision) {
        if (viewingFull && speakerUri != null) {
            fullText = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(speakerUri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
        }
    }
    LaunchedEffect(speakerUri, previewRevision) {
        excerpt = speakerUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val chars = CharArray(2200)
                        val count = reader.read(chars)
                        if (count <= 0) "" else String(chars, 0, count)
                    }
                }.getOrNull()
            }
        }
    }
    // The speaker sidecar can predate a newer word-timed regular transcription.
    // Use its words for the menu player only when they match the speaker text preview.
    LaunchedEffect(excerpt, speakerTimingPath, recording.transcriptTimingPath, previewRevision) {
        val text = excerpt
        if (text.isNullOrBlank()) return@LaunchedEffect
        val selection = withContext(Dispatchers.IO) {
            runCatching {
                val oldCues = TranscriptTimelineStore.load(speakerTimingPath)?.cues.orEmpty()
                val newCues = TranscriptTimelineStore.load(recording.transcriptTimingPath)?.cues.orEmpty()
                SpeakerWordSync.choose(text, oldCues, newCues)
            }.onFailure { DiagnosticLogger.appendError(context, "speaker word-sync menu matching failed", it) }
                .getOrNull()
        } ?: return@LaunchedEffect
        previewTimingPath = if (selection.useRegular) recording.transcriptTimingPath else speakerTimingPath
        DiagnosticLogger.append(context, "speaker word-sync menu source=${selection.source} " +
            "previewWords=${selection.textWords} speakerMapped=${selection.speakerMapped} " +
            "regularWordsMapped=${selection.regularWordMapped}")
    }
    if (viewingFull && fullText != null && speakerUri != null) {
        TranscriptViewerScreen(
            title = "${recording.title} — Speakers", transcript = fullText!!,
            audioUri = recording.audioUri?.let(Uri::parse),
            audioDurationMs = recording.durationMs, transcriptTimingPath = speakerTimingPath,
            speakerRegularTimingPath = recording.transcriptTimingPath,
            transcriptSyncProfileId = recording.id, transcriptSyncProfileLabel = "Speaker transcript",
            sourceLabel = if (recording.isVideo) "Video audio" else "Audio",
            onBack = { viewingFull = false }
        )
        return
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Speaker transcript") }, navigationIcon = { BackArrowButton(onBack) })
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(recording.title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatDateTime(recording.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            recording.audioUri?.let { raw ->
                SynchronizedAudioPlayer(
                    audioUri = Uri.parse(raw), audioDurationMs = recording.durationMs,
                    transcriptComplete = speakerUri != null, transcriptTimingPath = previewTimingPath,
                    transcriptSyncProfileId = recording.id, sourceLabel = if (recording.isVideo) "Video audio" else "Audio"
                )
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Speaker transcript", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f))
                        speakerUri?.let { uri ->
                            TranscriptShareIcon(R.drawable.ic_share_transcript, "Share speaker transcript",
                                onClick = { onShareTranscript(uri) })
                        }
                    }
                    Text(if (speakerUri != null) "✓ Speaker transcript available" else "No speaker transcript yet",
                        color = if (speakerUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (speakerUri != null) {
                        Button(onClick = { viewingFull = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("View full speaker transcript")
                        }
                    }
                    if (!excerpt.isNullOrEmpty()) {
                        Text(excerpt.orEmpty(), maxLines = 12, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Speaker settings", style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Expected speakers", modifier = Modifier.weight(1f))
                        SpeakerCountPicker(selected = expectedSpeakers, enabled = !running && !busy,
                            onSelected = onExpectedSpeakers)
                    }
                    Button(onClick = onCreate, enabled = modelsInstalled && !running && !busy,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (running) "Identifying speakers…" else if (speakerUri != null)
                            "Regenerate speaker transcript" else "Create speaker transcript")
                    }
                    if (speakerUri != null) {
                        OutlinedButton(onClick = onRenameLabels, enabled = !running,
                            modifier = Modifier.fillMaxWidth()) { Text("Rename speaker labels") }
                    }
                    if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!modelsInstalled) Text("Install the offline speaker models in Settings first.",
                        style = MaterialTheme.typography.bodySmall)
                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (hasDiagnostics && !running) {
                        TextButton(onClick = onViewDiagnostics) { Text("View speaker diagnostics") }
                    }
                }
            }
        }
    }
}
