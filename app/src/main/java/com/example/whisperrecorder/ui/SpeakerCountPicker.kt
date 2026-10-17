package com.example.whisperrecorder.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.whisperrecorder.speaker.SpeakerClusteringPolicy

/** Selects the expected speaker count for the next local diarization run. */
@Composable
fun SpeakerCountPicker(selected: Int, enabled: Boolean = true, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text(SpeakerClusteringPolicy.label(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SpeakerClusteringPolicy.choices.forEach { count ->
                DropdownMenuItem(
                    text = { Text(SpeakerClusteringPolicy.label(count)) },
                    onClick = {
                        expanded = false
                        onSelected(count)
                    }
                )
            }
        }
    }
}
