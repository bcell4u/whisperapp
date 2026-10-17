package com.example.whisperrecorder.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.transcribe.TranscriptCue
import com.example.whisperrecorder.transcribe.cueIndexAt
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun SynchronizedAudioPlayer(
    audioUri: Uri,
    audioDurationMs: Long,
    transcriptComplete: Boolean,
    transcriptTimingPath: String?,
    transcriptSyncProfileId: String?,
    sourceLabel: String = "Audio"
) {
    val context = LocalContext.current
    var cues by remember(transcriptTimingPath) { mutableStateOf<List<TranscriptCue>>(emptyList()) }
    var timelineLoaded by remember(transcriptTimingPath, transcriptComplete) {
        mutableStateOf(!transcriptComplete || transcriptTimingPath == null)
    }

    LaunchedEffect(transcriptComplete, transcriptTimingPath) {
        if (!transcriptComplete || transcriptTimingPath == null) {
            cues = emptyList()
            timelineLoaded = true
            return@LaunchedEffect
        }
        timelineLoaded = false
        cues = withContext(Dispatchers.IO) {
            val loaded = TranscriptTimelineStore.load(transcriptTimingPath)
            if (loaded == null) DiagnosticLogger.append(context, "word-sync player metadata unreadable: phrase sync unavailable")
            loaded?.cues.orEmpty()
        }
        timelineLoaded = true
    }

    val playback = rememberAudioPlaybackState(audioUri, audioDurationMs)
    var highlightOffsetMs by remember(transcriptSyncProfileId) {
        mutableIntStateOf(AppPrefs.getTranscriptHighlightOffsetMs(context, transcriptSyncProfileId))
    }
    // Keep the 100 ms playback clock local to AudioPlayerCard. The surrounding recording detail
    // only needs to recompose when the highlighted cue changes, not on every position tick.
    val activeCueIndex by remember(playback, cues, highlightOffsetMs) {
        derivedStateOf { cueIndexAt(cues, (playback.position + highlightOffsetMs).coerceAtLeast(0L)) }
    }
    val activeCue = cues.getOrNull(activeCueIndex)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AudioPlayerCard(playback, title = sourceLabel, footer = if (cues.isNotEmpty()) {
                {
                    CompactHighlightTimingControl(
                        offsetMs = highlightOffsetMs,
                        onOffsetChanged = { offset ->
                            highlightOffsetMs = offset
                            AppPrefs.setTranscriptHighlightOffsetMs(context, transcriptSyncProfileId, offset)
                        }
                    )
                }
            } else null)

        when {
            !transcriptComplete -> Text(
                "Playback/text sync available after transcription completes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            !timelineLoaded -> Text(
                "Preparing playback/text sync…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            cues.isNotEmpty() -> CurrentTranscriptCue(cue = activeCue, playing = playback.playing)
            else -> Text(
                "Playback/text sync unavailable for this transcription.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CurrentTranscriptCue(
    cue: TranscriptCue?,
    playing: Boolean
) {
    val active = playing && cue != null
    // Keep the card's surface stable while words change; a per-word crossfade
    // looks like flicker during fast speech. The full viewer animates text color.
    val ink = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                if (playing) "Now playing" else "Current position",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
            Text(cue?.text ?: "—", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = ink)
        }
    }
}
