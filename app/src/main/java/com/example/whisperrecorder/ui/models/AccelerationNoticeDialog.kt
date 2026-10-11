package com.example.whisperrecorder.ui.models

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.whisperrecorder.transcribe.AccelerationTier

/** Shown once per slower acceleration tier, never once per recording. */
@Composable
fun AccelerationNoticeDialog(tier: AccelerationTier, onDismiss: () -> Unit) {
    val gpu = tier == AccelerationTier.GPU
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (gpu) "GPU acceleration" else "CPU transcription") },
        text = {
            Text(if (gpu)
                "This model is not compatible with the Qualcomm NPU. WhisperRecorder uses the OpenCL GPU backend when available instead. More heat and battery consumption than NPU operation can be normal, especially on long recordings."
            else
                "This model runs on the CPU, not the Qualcomm NPU or OpenCL GPU. Longer transcription times, higher device temperatures, and increased battery consumption can be normal. For the most efficient supported option, select Parakeet NPU.")
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}
