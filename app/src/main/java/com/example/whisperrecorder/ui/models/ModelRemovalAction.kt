package com.example.whisperrecorder.ui.models

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared removal affordance for downloaded model cards.
 *
 * Keeping the label and placement contract in one composable prevents the model catalog from
 * drifting between "Remove", "Remove model", and "Remove from device" as new backends are added.
 */
@Composable
fun ModelRemovalAction(
    visible: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text("Remove from device")
    }
}