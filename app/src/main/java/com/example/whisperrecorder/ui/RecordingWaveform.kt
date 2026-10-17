package com.example.whisperrecorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * Lightweight live recording visualization.
 *
 * This renders a short history of the recorder's existing peak-level meter instead of opening a
 * second audio capture path. The service only publishes fast level samples while the Record screen
 * is visible; the fixed circular buffer below is UI-only and nothing is persisted.
 */
@Composable
fun RecordingWaveform(
    level: Float,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val sampleCount = 46
    val history = remember { FloatArray(sampleCount) }
    var cursor by remember { mutableIntStateOf(0) }
    // Append each service-published peak once. The UI no longer runs a second timer.
    LaunchedEffect(level, paused) {
        history[cursor] = if (paused) 0f else level.coerceIn(0f, 1f)
        cursor = (cursor + 1) % sampleCount
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Canvas(Modifier.fillMaxWidth().height(34.dp)) {
                val gap = 2.dp.toPx()
                val minimumHeight = 2.dp.toPx()
                val usableWidth = (size.width - gap * (sampleCount - 1)).coerceAtLeast(1f)
                val barWidth = (usableWidth / sampleCount).coerceAtLeast(1f)
                val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
                val centerY = size.height / 2f
                val color = if (paused) idleColor.copy(alpha = 0.24f) else activeColor.copy(alpha = 0.72f)

                for (index in 0 until sampleCount) {
                    val sample = history[(cursor + index) % sampleCount]
                    // A square-root response makes normal speech visible without exaggerating silence.
                    val shaped = kotlin.math.sqrt(sample.coerceIn(0f, 1f))
                    val height = (minimumHeight + shaped * (size.height - minimumHeight)).coerceAtMost(size.height)
                    val x = index * (barWidth + gap)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, centerY - height / 2f),
                        size = Size(barWidth, height),
                        cornerRadius = corner
                    )
                }
            }
        }
    }
}
