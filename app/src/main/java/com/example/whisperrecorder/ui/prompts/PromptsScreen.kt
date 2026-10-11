package com.example.whisperrecorder.ui.prompts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.prompts.PromptItem
import com.example.whisperrecorder.prompts.PromptStore
import java.util.UUID

private const val NEW_PROMPT_ID = "__new_prompt__"

@Composable
fun PromptsScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var category by rememberSaveable { mutableStateOf("Lecture") }
    var customVersion by remember { mutableIntStateOf(0) }
    var customTabVisible by rememberSaveable { mutableStateOf(PromptStore.custom(context).isNotEmpty()) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingId by rememberSaveable { mutableStateOf<String?>(null) }
    var copied by rememberSaveable { mutableStateOf<String?>(null) }
    var backupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var pendingImportCount by remember { mutableIntStateOf(0) }

    val customPrompts = remember(customVersion) { PromptStore.custom(context) }
    val prompts = remember(category, customVersion) {
        if (category == PromptStore.CUSTOM_CATEGORY) PromptStore.custom(context)
        else PromptStore.builtIns.filter { it.category == category }
    }
    val viewing = prompts.firstOrNull { it.id == viewingId }
    val editing = when (editingId) {
        NEW_PROMPT_ID -> PromptItem("", "", "", PromptStore.CUSTOM_CATEGORY, "")
        null -> null
        else -> PromptStore.custom(context).firstOrNull { it.id == editingId }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(PromptStore.exportCustomJson(context))
                } ?: error("Could not open the selected destination.")
            }.onSuccess {
                backupMessage = "Custom prompts exported."
            }.onFailure {
                backupMessage = "Export failed: ${it.message ?: "unknown error"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected backup.")
                val count = PromptStore.importPreviewCount(raw)
                raw to count
            }.onSuccess { (raw, count) ->
                pendingImport = raw
                pendingImportCount = count
            }.onFailure {
                backupMessage = "Import failed: ${it.message ?: "invalid backup"}"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = category == "Lecture", onClick = { category = "Lecture" }, label = { Text("Lectures") })
            FilterChip(selected = category == "Meeting", onClick = { category = "Meeting" }, label = { Text("Meetings") })
            if (customTabVisible || customPrompts.isNotEmpty() || category == PromptStore.CUSTOM_CATEGORY) {
                FilterChip(
                    selected = category == PromptStore.CUSTOM_CATEGORY,
                    onClick = { category = PromptStore.CUSTOM_CATEGORY },
                    label = { Text("Custom") }
                )
            }
            TextButton(onClick = {
                customTabVisible = true
                category = PromptStore.CUSTOM_CATEGORY
                viewingId = null
                editingId = NEW_PROMPT_ID
            }) { Text("+ New") }
        }

        copied?.let {
            Text("Copied: $it", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        backupMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (category == PromptStore.CUSTOM_CATEGORY) {
                item("custom-backup-actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("WhisperRecorder-custom-prompts.json") },
                                enabled = customPrompts.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("Export backup") }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("Import backup") }
                        }
                        if (customPrompts.isEmpty()) {
                            Text(
                                "Custom prompts you create or import will appear here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(prompts, key = { it.id }) { prompt ->
                Card(Modifier.fillMaxWidth().clickable { viewingId = prompt.id }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(prompt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (prompt.quizlet) AssistChip(onClick = {}, label = { Text("Quizlet") })
                        }
                        Text(prompt.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { clipboard.setText(AnnotatedString(prompt.text)); copied = prompt.title }) { Text("Copy") }
                            if (!prompt.builtIn) OutlinedButton(onClick = { editingId = prompt.id }) { Text("Edit") }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewingId = null },
            title = { Text(prompt.title) },
            text = { Text(prompt.text, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(prompt.text)); copied = prompt.title; viewingId = null
                }) { Text("Copy") }
            },
            dismissButton = { TextButton(onClick = { viewingId = null }) { Text("Close") } }
        )
    }

    editing?.let { original ->
        PromptEditorDialog(
            prompt = original,
            onDismiss = { editingId = null },
            onSave = { saved ->
                PromptStore.saveCustom(context, saved.copy(category = PromptStore.CUSTOM_CATEGORY))
                customTabVisible = true
                category = PromptStore.CUSTOM_CATEGORY
                customVersion++
                editingId = null
                backupMessage = null
            },
            onDelete = if (original.id.isBlank()) null else ({
                PromptStore.deleteCustom(context, original.id)
                customVersion++
                editingId = null
            })
        )
    }

    pendingImport?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import custom prompts") },
            text = {
                Text(
                    "Found $pendingImportCount custom prompt${if (pendingImportCount == 1) "" else "s"}. " +
                        "Merge keeps your current prompts and adds/updates the backup. Replace restores the backup exactly."
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        runCatching { PromptStore.importCustomJson(context, raw, replace = false) }
                            .onSuccess { count ->
                                customTabVisible = true
                                category = PromptStore.CUSTOM_CATEGORY
                                customVersion++
                                backupMessage = "Imported $count custom prompt${if (count == 1) "" else "s"}."
                            }
                            .onFailure { backupMessage = "Import failed: ${it.message ?: "invalid backup"}" }
                        pendingImport = null
                    }) { Text("Merge") }
                    Button(onClick = {
                        runCatching { PromptStore.importCustomJson(context, raw, replace = true) }
                            .onSuccess { count ->
                                customTabVisible = true
                                category = PromptStore.CUSTOM_CATEGORY
                                customVersion++
                                backupMessage = "Restored $count custom prompt${if (count == 1) "" else "s"}."
                            }
                            .onFailure { backupMessage = "Import failed: ${it.message ?: "invalid backup"}" }
                        pendingImport = null
                    }) { Text("Replace") }
                }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PromptEditorDialog(prompt: PromptItem, onDismiss: () -> Unit, onSave: (PromptItem) -> Unit, onDelete: (() -> Unit)?) {
    val stateKey = prompt.id.ifBlank { NEW_PROMPT_ID }
    var title by rememberSaveable(stateKey) { mutableStateOf(prompt.title) }
    var description by rememberSaveable(stateKey) { mutableStateOf(prompt.description) }
    var text by rememberSaveable(stateKey) { mutableStateOf(prompt.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prompt.id.isBlank()) "New custom prompt" else "Edit custom prompt") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                OutlinedTextField(text, { text = it }, label = { Text("Prompt") }, minLines = 6)
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete custom prompt", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        prompt.copy(
                            id = prompt.id.ifBlank { UUID.randomUUID().toString() },
                            title = title.trim(),
                            description = description.trim(),
                            category = PromptStore.CUSTOM_CATEGORY,
                            text = text.trim(),
                            builtIn = false
                        )
                    )
                },
                enabled = title.isNotBlank() && text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
