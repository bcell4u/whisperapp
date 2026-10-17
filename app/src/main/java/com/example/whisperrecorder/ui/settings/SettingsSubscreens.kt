@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.whisperrecorder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whisperrecorder.advanced.LocalAsrModelManager
import com.example.whisperrecorder.audio.MicrophoneProcessing
import com.example.whisperrecorder.audio.MicrophoneCapabilityProbe
import com.example.whisperrecorder.audio.RecordingRuntime
import com.example.whisperrecorder.speaker.SpeakerModels
import com.example.whisperrecorder.ui.SpeakerCountPicker
import com.example.whisperrecorder.data.AppPrefs
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.diagnostics.DiagnosticReports
import android.content.Intent
import com.example.whisperrecorder.models.ModelManager
import com.example.whisperrecorder.npu.NpuArchiveState
import com.example.whisperrecorder.npu.NpuPackageStore
import com.example.whisperrecorder.npu.OmiNpuPackageStore
import com.example.whisperrecorder.npu.Qwen3HybridPackageStore
import com.example.whisperrecorder.npu.NpuRuntimeClient
import com.example.whisperrecorder.ui.AudioFormatCard
import com.example.whisperrecorder.ui.BackArrowButton
import com.example.whisperrecorder.ui.SectionLabel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun AudioFormatSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var format by remember { mutableStateOf(AppPrefs.getAudioFormat(context)) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Audio format") }, navigationIcon = { BackArrowButton(onBack) }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Choose how new recordings are stored. This does not change existing recordings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AudioFormatCard(
                selected = format,
                onSelected = {
                    format = it
                    AppPrefs.setAudioFormat(context, it)
                }
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Transcription impact", style = MaterialTheme.typography.titleMedium)
                    Text("M4A and Opus require a short decode/resample step before transcription. WAV records 16 kHz mono PCM directly and skips most preprocessing, but uses much more storage.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun LanguageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = AppPrefs.getTranscriptionEngine(context)
    val selectedFile = if (engine == "whisper") AppPrefs.getModelPath(context)?.let(::File)?.takeIf { it.isFile } else null
    val localModel = LocalAsrModelManager.byEngine(engine)
    val localInstalled = localModel?.let { LocalAsrModelManager.isInstalled(context, it) } == true
    val englishOnly = selectedFile?.let(ModelManager::isEnglishOnly) == true
    val lockedEnglish = engine == "omi" || engine == "omi_npu"
    val automatic = engine == "qwen3" || engine == "qwen3_hybrid"
    val parakeetMultilingual = engine == "parakeet" || engine == "parakeet_npu"
    val npuReady = engine == "parakeet_npu" && NpuPackageStore.archiveState(context) == NpuArchiveState.READY
    val omiNpuReady = engine == "omi_npu" && OmiNpuPackageStore.archiveState(context) == NpuArchiveState.READY
    val qwenHybridReady = engine == "qwen3_hybrid" && Qwen3HybridPackageStore.state(context) == NpuArchiveState.READY
    var selected by remember(engine, selectedFile?.absolutePath) {
        mutableStateOf(if (englishOnly || lockedEnglish || (parakeetMultilingual && AppPrefs.getTranscriptionLanguage(context) == "auto")) "en" else AppPrefs.getTranscriptionLanguage(context))
    }
    LaunchedEffect(engine, englishOnly, lockedEnglish, automatic, parakeetMultilingual) {
        when {
            engine == "whisper" && englishOnly -> {
                selected = "en"
                AppPrefs.setTranscriptionLanguage(context, "en")
            }
            lockedEnglish -> {
                selected = "en"
                AppPrefs.setTranscriptionLanguage(context, "en")
            }
            automatic -> {
                selected = "auto"
                AppPrefs.setTranscriptionLanguage(context, "auto")
            }
            parakeetMultilingual && AppPrefs.getTranscriptionLanguage(context) == "auto" -> {
                selected = "en"
                AppPrefs.setTranscriptionLanguage(context, "en")
            }
        }
    }
    val modelName = when {
        engine == "parakeet_npu" -> NpuPackageStore.DISPLAY_NAME
        engine == "omi_npu" -> OmiNpuPackageStore.DISPLAY_NAME
        engine == "qwen3_hybrid" -> Qwen3HybridPackageStore.DISPLAY_NAME
        localModel != null -> localModel.name
        selectedFile != null -> ModelManager.displayName(selectedFile)
        else -> "No model selected"
    }
    val modelReady = when {
        engine == "parakeet_npu" -> npuReady
        engine == "omi_npu" -> omiNpuReady
        engine == "qwen3_hybrid" -> qwenHybridReady
        localModel != null -> localInstalled
        else -> selectedFile != null
    }
    val explanation = when {
        !modelReady && engine == "parakeet_npu" -> "The Qualcomm NPU package is incomplete. Repair it in Models & downloads."
        !modelReady && engine == "omi_npu" -> "The Omi Med NPU package is incomplete. Repair it in Models & downloads."
        !modelReady && engine == "qwen3_hybrid" -> "The Qwen hybrid package is incomplete. Repair it in Models & downloads."
        !modelReady && engine == "parakeet" -> "Parakeet model files are incomplete. Restore or reinstall Parakeet in Models & downloads."
        !modelReady && localModel != null -> "${localModel.name} is incomplete. Restore or reinstall it in Models & downloads."
        engine == "parakeet" || engine == "parakeet_npu" -> "Parakeet TDT 0.6B v3 supports 25 European languages. Select the spoken language for this model."
        engine == "qwen3" -> "Qwen3-ASR 1.7B automatically detects among its supported languages."
        engine == "omi_npu" -> "Omi Med NPU supports English. Review medical terms and doses; encoder numerical qualification is not complete."
        engine == "qwen3_hybrid" -> "Experimental Qwen 0.6B: 12-second windows, S26 QNN encoder plus CPU decoder. Validate fidelity before critical use."
        engine == "omi" -> "Omi Med STT v1 Q8_0 is an English medical-domain model and automatically uses the shared parakeet.cpp OpenCL/Adreno runtime."
        selectedFile == null -> "Select a Whisper model before choosing a language."
        englishOnly -> "This imported .en Whisper model supports English only."
        else -> "This Whisper model is multilingual and supports ${ModelManager.whisperLanguages.size - 1} languages plus automatic language detection."
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Transcription language") }, navigationIcon = { BackArrowButton(onBack) }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(modelName, style = MaterialTheme.typography.titleMedium)
                        Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (modelReady && automatic) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = true, onClick = null)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text("Automatic language detection")
                                Text(
                                    when (engine) {
                                        "parakeet" -> "25 European languages"
                                        else -> "30 supported languages"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (modelReady && (lockedEnglish || englishOnly)) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = true, onClick = null)
                            Text("English", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            } else if (modelReady && parakeetMultilingual) {
                val supported = setOf("bg", "hr", "cs", "da", "nl", "en", "et", "fi", "fr", "de", "el", "hu", "it", "lv", "lt", "mt", "pl", "pt", "ro", "ru", "sk", "sl", "es", "sv", "uk")
                val options = ModelManager.whisperLanguages.toList().filter { it.first in supported }
                items(options, key = { it.first }) { (code, label) ->
                    Card(Modifier.fillMaxWidth().clickable {
                        selected = code
                        AppPrefs.setTranscriptionLanguage(context, code)
                    }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selected == code,
                                onClick = { selected = code; AppPrefs.setTranscriptionLanguage(context, code) }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            } else if (selectedFile != null) {
                val options = ModelManager.whisperLanguages.toList()
                items(options, key = { it.first }) { (code, label) ->
                    Card(Modifier.fillMaxWidth().clickable {
                        selected = code
                        AppPrefs.setTranscriptionLanguage(context, code)
                    }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selected == code,
                                onClick = { selected = code; AppPrefs.setTranscriptionLanguage(context, code) }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PerformanceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    var preset by remember { mutableStateOf(AppPrefs.getPerformancePreset(context)) }
    var customThreads by remember { mutableIntStateOf(AppPrefs.getCustomThreads(context, cores)) }
    var chunkMinutes by remember { mutableIntStateOf(AppPrefs.getChunkMinutes(context)) }

    fun selectPreset(value: String) {
        preset = value
        AppPrefs.setPerformancePreset(context, value)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transcription performance") }, navigationIcon = { BackArrowButton(onBack) }) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Device", style = MaterialTheme.typography.titleMedium)
                        Text("CPU cores detected: $cores")
                        Text("Worker-thread presets apply to Whisper and Qwen3-ASR. NVIDIA Parakeet GPU and Omi Med use the runtime-managed OpenCL/Adreno worker. Qualcomm NPU uses ORT-QNN/HTP with Auto starting at the provider-default performance policy; CPU thread presets do not affect it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("The model is released from memory after each transcription. Language is configured separately in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Changes apply to the next transcription job.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { SectionLabel("PERFORMANCE PRESET") }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        listOf(
                            Triple("balanced", "Balanced", "${minOf(4, cores)} worker threads"),
                            Triple("high", "High Performance", "${minOf(6, cores)} worker threads • more heat and battery use"),
                            Triple("max", "Max", "$cores worker threads • maximum CPU use"),
                            Triple("custom", "Custom", "Choose worker threads for supported engines and Whisper segment limit")
                        ).forEach { (id, label, description) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { selectPreset(id) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = preset == id, onClick = { selectPreset(id) })
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(label, fontWeight = FontWeight.SemiBold)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            if (preset == "custom") {
                item { SectionLabel("CUSTOM") }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("Worker threads", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text("$customThreads", fontWeight = FontWeight.SemiBold)
                            }
                            if (cores > 1) {
                                Slider(
                                    value = customThreads.toFloat(),
                                    onValueChange = { raw ->
                                        customThreads = raw.roundToInt().coerceIn(1, cores)
                                        AppPrefs.setCustomThreads(context, customThreads, cores)
                                    },
                                    valueRange = 1f..cores.toFloat(),
                                    steps = (cores - 2).coerceAtLeast(0)
                                )
                            } else {
                                Text("This device exposes one CPU core, so supported engines use one worker thread.", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("This thread count applies to Whisper and Qwen3-ASR. NVIDIA Parakeet GPU and Omi Med use the runtime-managed OpenCL/Adreno worker; Qualcomm NPU uses ORT-QNN/HTP and ignores this CPU thread setting. More CPU threads can be faster for supported CPU engines, but mobile scaling is not linear and Max can increase heat or reduce sustained speed through thermal throttling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("Whisper maximum segment", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text("$chunkMinutes min", fontWeight = FontWeight.SemiBold)
                            }
                            Slider(
                                value = chunkMinutes.toFloat(),
                                onValueChange = { raw ->
                                    chunkMinutes = raw.roundToInt().coerceIn(1, 5)
                                    AppPrefs.setChunkMinutes(context, chunkMinutes)
                                },
                                valueRange = 1f..5f,
                                steps = 3
                            )
                            Text("This is a maximum for Whisper; shorter recordings stay in one segment and longer recordings are balanced to avoid a tiny final segment. Other local engines choose their own adaptive mobile-safe limit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Quality", style = MaterialTheme.typography.titleMedium)
                        Text("Balanced, High Performance, Max, and Custom use the same selected model and decoder. For engines that expose thread control, presets change CPU worker threads only and do not lower transcription quality. NVIDIA Parakeet and Omi Med use runtime-managed OpenCL execution.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MicrophoneProcessingSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val capabilities = remember { MicrophoneProcessing.capabilities() }
    var preset by remember { mutableStateOf(AppPrefs.getMicrophonePreset(context)) }
    var source by remember { mutableStateOf(AppPrefs.getCustomMicrophoneSource(context)) }
    var ns by remember { mutableStateOf(AppPrefs.getCustomNoiseSuppressorMode(context)) }
    var aec by remember { mutableStateOf(AppPrefs.getCustomAecMode(context)) }
    val scope = rememberCoroutineScope()
    var audioCheckRunning by remember { mutableStateOf(false) }
    var audioCheckMessage by remember { mutableStateOf("") }
    val recordingState by RecordingRuntime.state.collectAsState()

    fun checkCustomOption(nextSource: String, nextNs: String, nextAec: String, onApproved: () -> Unit) {
        if (audioCheckRunning) return
        audioCheckRunning = true
        audioCheckMessage = "Testing microphone configuration…"
        scope.launch {
            try {
                val format = AppPrefs.getAudioFormat(context)
                val result = withContext(Dispatchers.IO) {
                    MicrophoneCapabilityProbe.check(context, nextSource, format, nextNs, nextAec)
                }
                audioCheckMessage = result.message
                if (result.supported) onApproved()
            } finally {
                audioCheckRunning = false
            }
        }
    }

    fun choosePreset(value: String) {
        preset = value
        AppPrefs.setMicrophonePreset(context, value)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Microphone audio processing") }, navigationIcon = { BackArrowButton(onBack) }) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Recording profiles", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Choose a speech-oriented microphone path for different environments. Voice Recognition is an Android audio source intended for speech recognition; results vary by device and room.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Changes apply to new recordings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { SectionLabel("PRESETS") }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val presets = listOf(
                            Triple(MicrophoneProcessing.PRESET_DEFAULT, "Default", "General recording • standard microphone path"),
                            Triple(MicrophoneProcessing.PRESET_LECTURE, "Lecture / Distance", "Voice Recognition • noise suppression when controllable"),
                            Triple(MicrophoneProcessing.PRESET_MEETING, "Meeting / Room", "Voice Recognition • speech-focused room capture"),
                            Triple(MicrophoneProcessing.PRESET_CLOSE, "Close Speech", "Standard microphone • minimal explicit processing"),
                            Triple(MicrophoneProcessing.PRESET_SPEAKER, "Phone Speaker / Playback", "Voice Recognition • echo cancellation when controllable"),
                            Triple(MicrophoneProcessing.PRESET_CUSTOM, "Custom", "Choose the source and effect requests manually")
                        )
                        presets.forEach { (id, title, description) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { choosePreset(id) }.padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = preset == id, onClick = { choosePreset(id) })
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(title, fontWeight = FontWeight.SemiBold)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (preset == MicrophoneProcessing.PRESET_CUSTOM) {
                item { SectionLabel("CUSTOM") }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Audio source", style = MaterialTheme.typography.titleMedium)
                            Text("Each new source is tested before it is saved. A short microphone test is performed, without keeping audio.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (audioCheckMessage.isNotBlank()) Text(audioCheckMessage, style = MaterialTheme.typography.bodySmall)
                            if (recordingState.active) Text("Stop recording before changing custom audio options.", style = MaterialTheme.typography.bodySmall)
                            if (audioCheckRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
                            listOf(
                                MicrophoneProcessing.SOURCE_MIC to "Microphone",
                                MicrophoneProcessing.SOURCE_VOICE_RECOGNITION to "Voice Recognition",
                                MicrophoneProcessing.SOURCE_CAMCORDER to "Camcorder"
                            ).forEach { (id, label) ->
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !audioCheckRunning && !recordingState.active) {
                                        if (source != id) checkCustomOption(id, ns, aec) {
                                            source = id
                                            AppPrefs.setCustomMicrophoneSource(context, id)
                                        }
                                    }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = source == id,
                                        onClick = {
                                            if (source != id) checkCustomOption(id, ns, aec) {
                                                source = id
                                                AppPrefs.setCustomMicrophoneSource(context, id)
                                            }
                                        },
                                        enabled = !audioCheckRunning && !recordingState.active
                                    )
                                    Text(label, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                            Text("Camcorder selects Android's camera-oriented audio source; it does not record video. The actual microphone and processing depend on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    EffectModeCard("Noise suppression", ns, capabilities.noiseSuppressor, !audioCheckRunning && !recordingState.active) { choice ->
                        if (choice == MicrophoneProcessing.MODE_ON) {
                            checkCustomOption(source, choice, aec) { ns = choice; AppPrefs.setCustomNoiseSuppressorMode(context, choice) }
                        } else { ns = choice; AppPrefs.setCustomNoiseSuppressorMode(context, choice) }
                    }
                }
                item {
                    EffectModeCard("Acoustic echo cancellation", aec, capabilities.aec, !audioCheckRunning && !recordingState.active) { choice ->
                        if (choice == MicrophoneProcessing.MODE_ON) {
                            checkCustomOption(source, ns, choice) { aec = choice; AppPrefs.setCustomAecMode(context, choice) }
                        } else { aec = choice; AppPrefs.setCustomAecMode(context, choice) }
                    }
                }
            }

        }
    }
}

@Composable
internal fun EffectModeCard(title: String, selected: String, supported: Boolean, controlsEnabled: Boolean = true, onSelected: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(if (supported) "Supported" else "Not exposed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MicrophoneProcessing.MODE_OFF to "Off", MicrophoneProcessing.MODE_ON to "On", MicrophoneProcessing.MODE_AUTO to "Auto").forEach { (id, label) ->
                    FilterChip(
                        selected = selected == id,
                        onClick = { onSelected(id) },
                        label = { Text(label) },
                        enabled = controlsEnabled && (supported || id == MicrophoneProcessing.MODE_AUTO || id == MicrophoneProcessing.MODE_OFF)
                    )
                }
            }
        }
    }
}

@Composable
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var probeRunning by remember { mutableStateOf(false) }
    var testEncoder by remember { mutableStateOf(false) }
    var probeSummary by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf(DiagnosticLogger.readTail(context, 12_000)) }
    fun runOmiProbe() {
        if (!probeRunning) {
            probeRunning = true
            probeSummary = "Testing Omi QNN contexts. Keep the app open; encoder loading may take several minutes."
            scope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        NpuRuntimeClient.connect(context).use { client ->
                            client.probeOmiContexts(testEncoder)
                        }
                    }
                }
                outcome.onSuccess { result ->
                    val summary = buildString {
                        append("Omi NPU probe: ")
                        append(if (result.optBoolean("ok")) "PASS" else "FAILED")
                        append("; graphs=")
                        val graphs = result.optJSONArray("graphs")
                        if (graphs == null) append("no results") else {
                            for (i in 0 until graphs.length()) {
                                val graph = graphs.getJSONObject(i)
                                if (i > 0) append(", ")
                                append(graph.optString("graph"))
                                append(if (graph.optBoolean("ok")) " PASS" else " FAIL@${graph.optString("failure_stage")}")
                            }
                        }
                        result.optString("error").takeIf { it.isNotBlank() }?.let { append("; ").append(it) }
                    }
                    DiagnosticLogger.append(context, "omi-npu-probe RESULT $summary")
                    DiagnosticLogger.append(context, "omi-npu-probe JSON ${result}")
                    probeSummary = summary
                }.onFailure { error ->
                    DiagnosticLogger.appendError(context, "omi-npu-probe IPC", error)
                    probeSummary = "Probe could not finish: ${error::class.java.simpleName}: ${error.message}; export diagnostics for details"
                }
                probeRunning = false
                preview = DiagnosticLogger.readTail(context, 12_000)
            }
        }
    }
    var verbose by remember { mutableStateOf(AppPrefs.isVerboseDiagnostics(context)) }
    var status by remember { mutableStateOf("") }
    var reports by remember { mutableStateOf(emptyList<DiagnosticReports.Report>()) }
    LaunchedEffect(Unit) {
        reports = withContext(Dispatchers.IO) { DiagnosticReports.list(context) }
    }
    var generatingReport by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var deleteReportName by remember { mutableStateOf<String?>(null) }
    fun shareReport(report: DiagnosticReports.Report) {
        runCatching {
            val uri = DiagnosticReports.shareUri(context, report)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(context.contentResolver, report.name, uri)
            }
            context.startActivity(Intent.createChooser(send, "Share diagnostic ZIP"))
        }.onFailure { status = "Unable to share report: ${it::class.java.simpleName}" }
    }
    if (confirmDeleteAll) AlertDialog(
        onDismissRequest = { confirmDeleteAll = false },
        title = { Text("Delete all diagnostic ZIPs?") },
        text = { Text("Only saved diagnostic reports will be deleted. Models, recordings, transcripts, and the live log are unaffected.") },
        confirmButton = { TextButton(onClick = {
            confirmDeleteAll = false
            scope.launch {
                val count = withContext(Dispatchers.IO) { DiagnosticReports.deleteAll(context) }
                reports = withContext(Dispatchers.IO) { DiagnosticReports.list(context) }
                status = "$count diagnostic reports deleted"
            }
        }) { Text("Delete reports") } },
        dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } }
    )
    deleteReportName?.let { name -> AlertDialog(
        onDismissRequest = { deleteReportName = null },
        title = { Text("Delete this report?") },
        text = { Text(name) },
        confirmButton = { TextButton(onClick = {
            deleteReportName = null
            scope.launch {
                val deleted = withContext(Dispatchers.IO) { DiagnosticReports.delete(context, name) }
                reports = withContext(Dispatchers.IO) { DiagnosticReports.list(context) }
                status = if (deleted) "Diagnostic report deleted" else "Report could not be deleted"
            }
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteReportName = null }) { Text("Cancel") } }
    ) }
    val clipboard = LocalClipboardManager.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            try {
                val text = DiagnosticLogger.readAll(context)
                context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                    ?: error("Could not open destination")
                status = "Diagnostic log exported"
            } catch (t: Throwable) {
                status = "Export failed: ${t.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = { BackArrowButton(onBack) }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Verbose transcription status",
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(checked = verbose, onCheckedChange = {
                            verbose = it
                            AppPrefs.setVerboseDiagnostics(context, it)
                            DiagnosticLogger.append(context, "verbose diagnostics=${if (it) "enabled" else "disabled"}")
                        })
                    }
                    Text(
                        "Show detailed transcription stages and periodic inference timing. Logs are still written for errors even when this is off.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Omi Med NPU compatibility", style = MaterialTheme.typography.titleMedium)
                    Text("Download the Omi NPU package from Models & downloads first. This runs a synthetic QNN graph test on those installed assets, not a speech transcription test.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = testEncoder, onCheckedChange = { testEncoder = it }, enabled = !probeRunning)
                        Text("Include 1.2 GB encoder (longer test)", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { runOmiProbe() }, enabled = !probeRunning) {
                        Text(if (probeRunning) "Testing QNN contexts…" else "Test downloaded Omi NPU assets")
                    }
                    if (probeRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (probeSummary.isNotBlank()) Text(probeSummary, style = MaterialTheme.typography.bodySmall)
                    Text("The export below includes per-graph failure stage, session errors, stacks, execution timing and the NPU worker log. CPU fallback is disabled.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transcription log", style = MaterialTheme.typography.titleMedium)
                    Text("${DiagnosticLogger.sizeBytes(context) / 1024} KB stored locally. The log automatically trims itself after 2 MB.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exporter.launch("WhisperRecorder-transcription-log.txt") }) { Text("Export log") }
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(DiagnosticLogger.readAll(context)))
                            status = "Log copied to clipboard"
                        }) { Text("Copy") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            preview = DiagnosticLogger.readTail(context, 12_000)
                            status = "Log refreshed"
                        }) { Text("Refresh") }
                        TextButton(onClick = {
                            DiagnosticLogger.clear(context)
                            preview = ""
                            status = "Diagnostic log cleared"
                        }) { Text("Clear log", color = MaterialTheme.colorScheme.error) }
                    }
                    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automatic diagnostic ZIPs", style = MaterialTheme.typography.titleMedium)
                    Text("A sanitized ZIP is saved to your selected Whisperapp/Logs folder after each transcription (success, failure, or cancellation). If the folder is unavailable, the ZIP is retained privately. No recordings, transcripts, token IDs, model weights, or personal file paths are included.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !generatingReport, onClick = {
                            generatingReport = true
                            scope.launch {
                                val created = withContext(Dispatchers.IO) {
                                    DiagnosticReports.manualSnapshot(context)
                                }
                                status = if (created != null) "Diagnostic ZIP created" else "Could not generate diagnostic ZIP"
                                reports = withContext(Dispatchers.IO) { DiagnosticReports.list(context) }
                                generatingReport = false
                            }
                        }) { Text(if (generatingReport) "Creating..." else "Create ZIP now") }
                        OutlinedButton(onClick = {
                            scope.launch { reports = withContext(Dispatchers.IO) { DiagnosticReports.list(context) } }
                        }) { Text("Refresh ZIPs") }
                    }
                    Text(DiagnosticReports.storageDescription(context), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${reports.size} saved reports. Older reports are automatically pruned to at most 8 files / 32 MB.",
                        style = MaterialTheme.typography.bodySmall)
                    reports.take(8).forEach { report ->
                        HorizontalDivider()
                        Text(report.name, style = MaterialTheme.typography.bodySmall)
                        Text("${report.sizeBytes / 1024} KiB · ${if (report.location == DiagnosticReports.Location.WORKSPACE_LOGS) "Whisperapp/Logs" else "Private fallback"}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { shareReport(report) }) { Text("Share ZIP") }
                            TextButton(onClick = { deleteReportName = report.name }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    if (reports.isNotEmpty()) {
                        TextButton(onClick = { confirmDeleteAll = true }) { Text("Delete all diagnostic ZIPs", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            if (preview.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Recent log", style = MaterialTheme.typography.titleSmall)
                        Text(preview, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text("No diagnostic entries yet. Start a transcription to create a log.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun OfflineSpeakerSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var timestamps by remember { mutableStateOf(AppPrefs.speakerTimestamps(context)) }
    var speakerCount by remember { mutableIntStateOf(AppPrefs.speakerExpectedCount(context)) }
    var modelsReady by remember { mutableStateOf(SpeakerModels.installed(context)) }
    var downloading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Offline speaker identification") }, navigationIcon = { BackArrowButton(onBack) })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Identify different voices entirely on the phone after transcription. Your original transcript is preserved; a separate -speaker.txt file is created.")
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include timestamps", style = MaterialTheme.typography.titleMedium)
                        Text("Add [hh:mm:ss] to speaker transcripts. Off by default.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = timestamps, onCheckedChange = {
                        timestamps = it
                        AppPrefs.setSpeakerTimestamps(context, it)
                    })
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Expected speakers", style = MaterialTheme.typography.titleMedium)
                        Text("Select 2 for a two-person interview. Automatic may split one voice into several speakers.", style = MaterialTheme.typography.bodySmall)
                    }
                    SpeakerCountPicker(selected = speakerCount) { count ->
                        speakerCount = count
                        AppPrefs.setSpeakerExpectedCount(context, count)
                    }
                }
            }
            Text(if (modelsReady) "Speaker models installed (about 41 MB)" else "Two optional models (~41 MB): Pyannote INT8 and ERes2Net. Download once, then all processing is offline.")
            Button(onClick = {
                downloading = true
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching { SpeakerModels.install(context) { progress -> scope.launch { status = progress } } }
                    }
                    downloading = false
                    modelsReady = SpeakerModels.installed(context)
                    status = outcome.fold({ "Speaker models downloaded and verified." }, { "Download failed: ${it.message}" })
                }
            }, enabled = !downloading) {
                Text(if (downloading) "Downloading…" else if (modelsReady) "Verify / repair speaker models" else "Download speaker models")
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("You can also choose the expected speaker count when generating a transcript. A fixed count reduces over-splitting, but speaker attribution still needs review. Long recordings use bounded, overlapping windows with speaker identity matching. Labels represent acoustic similarity, not identity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
