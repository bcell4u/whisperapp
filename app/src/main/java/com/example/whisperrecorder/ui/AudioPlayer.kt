package com.example.whisperrecorder.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

data class PlaybackSeekRequest(val token: Long, val positionMs: Long)

@Stable
class AudioPlaybackState internal constructor(durationHint: Long) {
    var prepared by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(durationHint)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    internal fun prepare(context: android.content.Context, uri: Uri) {
        release()
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.setOnPreparedListener {
                prepared = true
                duration = it.duration.toLong()
                playing = false
                position = 0L
            }
            mp.setOnCompletionListener {
                playing = false
                position = duration
            }
            mp.setOnErrorListener { _, what, extra ->
                playing = false
                error = "MediaPlayer error $what/$extra"
                true
            }
            mp.prepareAsync()
        }.onFailure {
            error = it.message ?: "Unable to prepare media"
            runCatching { mp.release() }
            if (player === mp) player = null
        }
    }

    internal fun release() {
        val current = player
        player = null
        prepared = false
        playing = false
        if (current != null) {
            runCatching { current.pause() }
            runCatching { current.release() }
        }
    }

    fun previewPosition(value: Long) {
        position = value.coerceIn(0L, duration.coerceAtLeast(0L))
    }

    fun commitPreviewPosition() {
        if (!prepared) return
        runCatching { player?.seekTo(position.toInt()) }
    }

    fun seekTo(positionMs: Long) {
        if (!prepared) return
        val target = positionMs.coerceIn(0L, duration.coerceAtLeast(0L))
        runCatching { player?.seekTo(target.toInt()) }
        position = target
    }

    fun seekBy(deltaMs: Long) {
        seekTo(position + deltaMs)
    }

    fun togglePlayback() {
        if (!prepared) return
        val current = player ?: return
        if (runCatching { current.isPlaying }.getOrDefault(false)) {
            runCatching { current.pause() }
            playing = false
            return
        }
        if (position >= (duration - 250).coerceAtLeast(0L)) {
            runCatching { current.seekTo(0) }
            position = 0L
        }
        runCatching { current.setPlaybackParams(current.playbackParams.setSpeed(speed)) }
        runCatching { current.start() }
            .onSuccess { playing = true }
            .onFailure {
                playing = false
                error = it.message ?: "Unable to start playback"
            }
    }

    fun updatePlaybackSpeed(value: Float) {
        speed = value
        val current = player ?: return
        if (runCatching { current.isPlaying }.getOrDefault(false)) {
            runCatching { current.setPlaybackParams(current.playbackParams.setSpeed(value)) }
        }
    }

    internal fun pollPosition() {
        val current = player ?: return
        if (!runCatching { current.isPlaying }.getOrDefault(false)) {
            playing = false
            return
        }
        position = runCatching { current.currentPosition.toLong() }.getOrDefault(position)
    }
}

@Composable
fun rememberAudioPlaybackState(
    uri: Uri,
    durationHint: Long,
    onPositionChanged: (Long) -> Unit = {},
    onPlayingChanged: (Boolean) -> Unit = {},
    seekRequest: PlaybackSeekRequest? = null
): AudioPlaybackState {
    val context = LocalContext.current
    val state = remember(uri) { AudioPlaybackState(durationHint) }
    val currentPositionCallback by rememberUpdatedState(onPositionChanged)
    val currentPlayingCallback by rememberUpdatedState(onPlayingChanged)

    DisposableEffect(uri) {
        state.prepare(context, uri)
        onDispose { state.release() }
    }

    LaunchedEffect(state.position) { currentPositionCallback(state.position) }
    LaunchedEffect(state.playing) { currentPlayingCallback(state.playing) }
    LaunchedEffect(seekRequest?.token, state.prepared) {
        val request = seekRequest ?: return@LaunchedEffect
        if (state.prepared) state.seekTo(request.positionMs)
    }
    LaunchedEffect(state.playing, state) {
        while (state.playing) {
            state.pollPosition()
            delay(100)
        }
    }

    return state
}

@Composable
fun AudioPlayerCard(
    state: AudioPlaybackState,
    modifier: Modifier = Modifier,
    title: String = "Audio",
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${formatElapsed(state.position)} / ${formatElapsed(state.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                onValueChange = { state.previewPosition(it.toLong()) },
                onValueChangeFinished = state::commitPreviewPosition,
                valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                enabled = state.prepared,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { state.seekBy(-15_000L) },
                    enabled = state.prepared,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("−15s", maxLines = 1) }
                Button(
                    onClick = state::togglePlayback,
                    enabled = state.prepared,
                    modifier = Modifier.weight(1.15f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text(if (state.playing) "Pause" else "Play", maxLines = 1) }
                OutlinedButton(
                    onClick = { state.seekBy(15_000L) },
                    enabled = state.prepared,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("+15s", maxLines = 1) }
                Box(modifier = Modifier.weight(0.9f)) {
                    OutlinedButton(
                        onClick = { speedMenuExpanded = true },
                        enabled = state.prepared,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(formatSpeed(state.speed), maxLines = 1, fontWeight = FontWeight.SemiBold)
                    }
                    PlaybackSpeedMenu(
                        expanded = speedMenuExpanded,
                        onDismiss = { speedMenuExpanded = false },
                        onSelect = {
                            state.updatePlaybackSpeed(it)
                            speedMenuExpanded = false
                        }
                    )
                }
            }
            footer?.invoke(this)
            state.error?.let {
                Text(
                    "Playback unavailable: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun FloatingAudioPlayerControls(
    state: AudioPlaybackState,
    modifier: Modifier = Modifier,
    highlightOffsetMs: Int? = null,
    onHighlightOffsetChanged: ((Int) -> Unit)? = null,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (!expanded) speedMenuExpanded = false
    }

    Surface(
        modifier = modifier.animateContentSize(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        shape = RoundedCornerShape(if (expanded) 28.dp else 22.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = if (expanded) 4.dp else 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { onExpandedChange(!expanded) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .sizeIn(minWidth = 48.dp, minHeight = 40.dp)
                        .semantics {
                            contentDescription = if (expanded) {
                                "Collapse playback controls"
                            } else {
                                "Expand playback controls"
                            }
                        }
                ) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expanded) {
                        TextButton(
                            onClick = { state.seekBy(-15_000L) },
                            enabled = state.prepared,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("−15") }
                    }
                    FilledTonalButton(
                        onClick = state::togglePlayback,
                        enabled = state.prepared,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) { Text(if (state.playing) "Pause" else "Play") }
                    if (expanded) {
                        TextButton(
                            onClick = { state.seekBy(15_000L) },
                            enabled = state.prepared,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("+15") }
                        Box {
                            TextButton(
                                onClick = { speedMenuExpanded = true },
                                enabled = state.prepared,
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                            ) { Text(formatSpeed(state.speed), fontWeight = FontWeight.SemiBold) }
                            PlaybackSpeedMenu(
                                expanded = speedMenuExpanded,
                                onDismiss = { speedMenuExpanded = false },
                                onSelect = {
                                    state.updatePlaybackSpeed(it)
                                    speedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    formatElapsed(state.position),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
            if (expanded && highlightOffsetMs != null && onHighlightOffsetChanged != null) {
                CompactHighlightTimingControl(
                    offsetMs = highlightOffsetMs,
                    onOffsetChanged = onHighlightOffsetChanged,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CompactHighlightTimingControl(
    offsetMs: Int,
    onOffsetChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            Modifier.fillMaxWidth().height(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Highlight sync",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (offsetMs > 0) "+$offsetMs ms" else "$offsetMs ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (offsetMs != 0) {
                TextButton(
                    onClick = { onOffsetChanged(0) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) { Text("Reset", style = MaterialTheme.typography.labelSmall) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Slower",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = offsetMs.toFloat(),
                onValueChange = { raw ->
                    val snapped = (raw / 50f).roundToInt() * 50
                    onOffsetChanged(snapped.coerceIn(-1_000, 1_000))
                },
                valueRange = -1_000f..1_000f,
                steps = 39,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .semantics {
                        contentDescription = "Highlight sync. Move left to slow the highlight, right to advance it."
                    }
            )
            Text(
                "Faster",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackSpeedMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PLAYBACK_SPEEDS.forEach { option ->
            DropdownMenuItem(
                text = { Text(formatSpeed(option)) },
                onClick = { onSelect(option) }
            )
        }
    }
}

private fun formatSpeed(speed: Float): String =
    "${"%.2f".format(speed).trimEnd('0').trimEnd('.')}×"
