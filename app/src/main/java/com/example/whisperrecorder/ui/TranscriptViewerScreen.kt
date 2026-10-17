package com.example.whisperrecorder.ui

import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.transcribe.TranscriptCue
import com.example.whisperrecorder.transcribe.TranscriptCueMapper
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import com.example.whisperrecorder.transcribe.SpeakerWordSync
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.transcribe.cueIndexAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val FOLLOW_SUSPEND_AFTER_MANUAL_SCROLL_MS = 4_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptViewerScreen(
    title: String,
    transcript: String,
    audioUri: Uri?,
    audioDurationMs: Long,
    transcriptTimingPath: String?,
    speakerRegularTimingPath: String? = null,
    transcriptSyncProfileId: String?,
    transcriptSyncProfileLabel: String?,
    sourceLabel: String = "Audio",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var seekToken by remember(audioUri) { mutableLongStateOf(0L) }
    var seekRequest by remember(audioUri) { mutableStateOf<PlaybackSeekRequest?>(null) }
    var cues by remember(transcriptTimingPath, speakerRegularTimingPath) { mutableStateOf<List<TranscriptCue>>(emptyList()) }
    var highlightOffsetMs by remember(transcriptSyncProfileId) {
        mutableIntStateOf(AppPrefs.getTranscriptHighlightOffsetMs(context, transcriptSyncProfileId))
    }
    var audioCardHeightPx by remember(audioUri) { mutableIntStateOf(0) }
    var followSuspendedUntilMs by remember { mutableLongStateOf(0L) }
    val collapseMarginPx = with(density) { 8.dp.toPx() }
    var floatingPlayerExpanded by rememberSaveable(audioUri) { mutableStateOf(true) }

    LaunchedEffect(transcriptTimingPath, speakerRegularTimingPath, transcript) {
        cues = withContext(Dispatchers.IO) {
            val speakerCues = TranscriptTimelineStore.load(transcriptTimingPath)?.cues.orEmpty()
            if (speakerRegularTimingPath == null) {
                if (speakerCues.isEmpty()) DiagnosticLogger.append(context,
                    "word-sync viewer metadata missing-or-unreadable")
                speakerCues
            } else {
                val regularCues = TranscriptTimelineStore.load(speakerRegularTimingPath)?.cues.orEmpty()
                val choice = runCatching { SpeakerWordSync.choose(transcript, speakerCues, regularCues) }
                    .onFailure { DiagnosticLogger.appendError(context, "speaker word-sync viewer matching failed", it) }
                    .getOrNull()
                if (choice != null) DiagnosticLogger.append(context, "speaker word-sync viewer source=${choice.source} " +
                    "textWords=${choice.textWords} speakerMapped=${choice.speakerMapped} " +
                    "regularMapped=${choice.regularMapped} regularWordsMapped=${choice.regularWordMapped} " +
                    "speakerCues=${speakerCues.size} regularCues=${regularCues.size}")
                if (choice?.useRegular == true) regularCues else speakerCues
            }
        }
    }

    val audioPlayback = if (audioUri != null) {
        rememberAudioPlaybackState(uri = audioUri, durationHint = audioDurationMs, seekRequest = seekRequest)
    } else null
    val playing = audioPlayback?.playing == true
    val activeCueIndex by remember(audioPlayback, cues, highlightOffsetMs) {
        derivedStateOf {
            audioPlayback?.position?.let { cueIndexAt(cues, (it + highlightOffsetMs).coerceAtLeast(0L)) } ?: -1
        }
    }

    val speakerTranscript = transcriptSyncProfileLabel == "Speaker transcript"
    val cueRanges = remember(transcript, cues, speakerTranscript) {
        TranscriptCueMapper.map(transcript, cues, skipSpeakerHeadings = speakerTranscript)
    }
    val renderBlocks = remember(transcript, cueRanges) { TranscriptRenderBlocks.build(transcript, cueRanges) }
    val cueToBlock = remember(renderBlocks) {
        buildMap<Int, Int> {
            renderBlocks.forEachIndexed { blockIndex, block ->
                block.cueRanges.forEach { put(it.cueIndex, blockIndex) }
            }
        }
    }
    val activeTextColor = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    // Title and audio card are the first list items. The docked player appears once the
    // inline audio card has fully scrolled out, avoiding a duplicate player during the handoff.
    val audioItemIndex = if (audioPlayback != null) 1 else -1
    val transcriptStartIndex = if (audioPlayback != null) 4 else 3
    val showFloatingPlayer by remember(audioPlayback, audioItemIndex, audioCardHeightPx, collapseMarginPx, listState) {
        derivedStateOf {
            if (audioPlayback == null || audioItemIndex < 0 || audioCardHeightPx <= 0) false
            else when {
                listState.firstVisibleItemIndex > audioItemIndex -> true
                listState.firstVisibleItemIndex < audioItemIndex -> false
                else -> listState.firstVisibleItemScrollOffset >= (audioCardHeightPx - collapseMarginPx).roundToInt().coerceAtLeast(0)
            }
        }
    }

    val manualScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    followSuspendedUntilMs = SystemClock.elapsedRealtime() + FOLLOW_SUSPEND_AFTER_MANUAL_SCROLL_MS
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(followSuspendedUntilMs) {
        val deadline = followSuspendedUntilMs
        if (deadline <= 0L) return@LaunchedEffect
        val waitMs = deadline - SystemClock.elapsedRealtime()
        if (waitMs > 0L) delay(waitMs)
        if (followSuspendedUntilMs == deadline) followSuspendedUntilMs = 0L
    }

    LaunchedEffect(activeCueIndex, playing, followSuspendedUntilMs, renderBlocks) {
        if (!playing || activeCueIndex < 0) return@LaunchedEffect
        if (followSuspendedUntilMs > SystemClock.elapsedRealtime()) return@LaunchedEffect
        val blockIndex = cueToBlock[activeCueIndex] ?: return@LaunchedEffect
        val targetItem = transcriptStartIndex + blockIndex
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetItem }
        if (!visible) listState.animateScrollToItem(targetItem)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Full Transcript") }, navigationIcon = { BackArrowButton(onBack) })
                AnimatedVisibility(
                    visible = showFloatingPlayer && audioPlayback != null,
                    enter = expandVertically(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) +
                        fadeIn(tween(180, delayMillis = 35)) +
                        slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 4 },
                    exit = shrinkVertically(tween(190, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) +
                        fadeOut(tween(135)) +
                        slideOutVertically(tween(190, easing = FastOutSlowInEasing)) { it / 4 }
                ) {
                    audioPlayback?.let { playback ->
                        FloatingAudioPlayerControls(
                            state = playback,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                            highlightOffsetMs = highlightOffsetMs.takeIf { cues.isNotEmpty() },
                            onHighlightOffsetChanged = if (cues.isNotEmpty()) {
                                { offset ->
                                    highlightOffsetMs = offset
                                    AppPrefs.setTranscriptHighlightOffsetMs(context, transcriptSyncProfileId, offset)
                                }
                            } else null,
                            expanded = floatingPlayerExpanded,
                            onExpandedChange = { floatingPlayerExpanded = it }
                        )
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(pad).fillMaxSize().nestedScroll(manualScrollConnection),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item(key = "title") {
                Text(title, modifier = Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (audioPlayback != null) {
                item(key = "audio") {
                    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp).onSizeChanged { audioCardHeightPx = it.height }) {
                        AudioPlayerCard(
                            audioPlayback,
                            title = sourceLabel,
                            footer = if (cues.isNotEmpty()) {
                                {
                                    CompactHighlightTimingControl(
                                        offsetMs = highlightOffsetMs,
                                        onOffsetChanged = { offset ->
                                            highlightOffsetMs = offset
                                            AppPrefs.setTranscriptHighlightOffsetMs(context, transcriptSyncProfileId, offset)
                                        }
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            item(key = "divider") { HorizontalDivider(Modifier.padding(bottom = 10.dp)) }
            item(key = "transcript-label") {
                Text("Transcript", modifier = Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            itemsIndexed(renderBlocks, key = { index, block -> "transcript-${block.sourceStart}-$index" }) { _, block ->
                TranscriptBlockText(
                    block = block,
                    activeCueIndex = activeCueIndex,
                    activeTextColor = activeTextColor,
                    onCueTapped = { cueIndex ->
                        cues.getOrNull(cueIndex)?.let { cue ->
                            seekToken += 1L
                            seekRequest = PlaybackSeekRequest(seekToken, cue.startMs)
                        }
                    }
                )
            }
        }
    }
}
