package com.example.whisperrecorder.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.advanced.LocalAsrModel
import com.example.whisperrecorder.models.ModelDownloadState
import com.example.whisperrecorder.ui.DownloadProgressBar
import com.example.whisperrecorder.ui.modelDownloadProgressText

/** Display-only model metadata. NPU packages and local GGUF models use the same card layout. */
internal data class ModelCardDetails(
    val id: String,
    val name: String,
    val licenseName: String,
    val sizeLabel: String,
    val languageCount: Int
)

/** Shared layout for GPU, NPU and other local transcription model cards. */
@Composable
internal fun ModelCatalogCard(
    details: ModelCardDetails,
    installed: Boolean,
    persistent: Boolean,
    selected: Boolean,
    restoring: Boolean,
    partialDownload: Boolean,
    modelDownload: ModelDownloadState,
    removalEnabled: Boolean = false,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpenModel: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val downloading = modelDownload.active && modelDownload.modelId == details.id
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(details.name, fontWeight = FontWeight.SemiBold)
                    Text(details.licenseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(details.sizeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (details.languageCount == 1) "1 language" else "${details.languageCount} languages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        installed || persistent -> {
                            Button(
                                onClick = onUse,
                                enabled = !modelDownload.active && !restoring && !selected
                            ) { Text(if (restoring) "Restoring…" else if (selected) "Selected" else "Use") }
                            if (persistent && !installed && !restoring) {
                                Text("Model available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        downloading -> OutlinedButton(onClick = onCancel) { Text("Cancel") }
                        else -> Button(onClick = onDownload, enabled = !modelDownload.active && !restoring) {
                            Text(if (partialDownload) "Resume" else "Download")
                        }
                    }
                    TextButton(onClick = onOpenModel) { Text("Model") }
                }
            }
            if (onRemove != null) {
                ModelRemovalAction(
                    visible = installed || persistent,
                    enabled = removalEnabled,
                    onClick = onRemove
                )
            }
            // Each download message belongs to its model, never the screen-level
            // "Transcription models" introduction (notably the 805 MB Qwen GGUF).
            if (modelDownload.modelId == details.id) {
                if (downloading) {
                    DownloadProgressBar(modelDownload.progress, Modifier.fillMaxWidth())
                    Text(
                        modelDownloadProgressText(modelDownload.bytesDownloaded, modelDownload.totalBytes, modelDownload.progress, modelDownload.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (modelDownload.status.isNotBlank()) {
                    Text(
                        if (modelDownload.error != null) "Download failed: ${modelDownload.error}" else modelDownload.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (modelDownload.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Local GGUF model adapter; keeps the existing call sites and behavior unchanged. */
@Composable
fun LocalAsrModelCard(
    model: LocalAsrModel,
    installed: Boolean,
    persistent: Boolean,
    selected: Boolean,
    restoring: Boolean,
    partialDownload: Boolean,
    modelDownload: ModelDownloadState,
    removalEnabled: Boolean = false,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpenModel: () -> Unit,
    onRemove: (() -> Unit)? = null
) = ModelCatalogCard(
    details = ModelCardDetails(model.id, model.name, model.licenseName, model.sizeLabel, model.languageCount),
    installed = installed,
    persistent = persistent,
    selected = selected,
    restoring = restoring,
    partialDownload = partialDownload,
    modelDownload = modelDownload,
    removalEnabled = removalEnabled,
    onUse = onUse,
    onDownload = onDownload,
    onCancel = onCancel,
    onOpenModel = onOpenModel,
    onRemove = onRemove
)
