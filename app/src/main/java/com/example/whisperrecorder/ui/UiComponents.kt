package com.example.whisperrecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.R

@Composable
fun SettingCard(title: String, value: String, action: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(action, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AudioFormatCard(selected: String, onSelected: (String) -> Unit) {
    val options = listOf(
        Triple("opus", "OGG / Opus", "Recommended. Smallest files for speech and ideal for long lectures and meetings."),
        Triple("m4a", "M4A / AAC", "Compact, broadly compatible, and quick to decode."),
        Triple("wav", "WAV / PCM", "Largest files. Records 16 kHz mono PCM and minimizes transcription preprocessing.")
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Audio format", style = MaterialTheme.typography.titleMedium)
            options.forEach { (id, label, description) ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelected(id) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == id, onClick = { onSelected(id) })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "MP3 files can be imported, but Android does not provide a reliable built-in MP3 recording encoder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BackArrowButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
    }
}

@Composable
fun CompactProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(modifier.height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (fraction > 0f) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun DownloadProgressBar(progress: Int, modifier: Modifier = Modifier) {
    CompactProgressBar(progress.coerceIn(0, 100) / 100f, modifier)
}

/** Compact, accessible action for sharing a saved transcript or its media. */
@Composable
fun TranscriptShareIcon(iconRes: Int, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painter = painterResource(iconRes), contentDescription = label,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}
