# WhisperRecorder 6.7.9 - first-run Qwen status placement

## Bug observed

On fresh installation, the setup step 3 introduction displayed model-download
messages (for example, "Downloaded Qwen3-ASR 0.6B QNN + Q8_0") even though
Qwen had its own model card under Hybrid (CPU/NPU). This happened because
SetupScreen copied every ModelDownloadRuntime status into the general setup
`status` text via a LaunchedEffect.

## Correction

- Removed the global download-status-to-introduction binding.
- The shared LocalAsrModelCard already renders byte progress and terminal
  download status only when its model ID matches the active/completed download.
  It now remains the one source of Qwen download status in onboarding.
- Retained workspace-folder messages below the working-directory picker.
- Routed transient model-selection success/failure through a snackbar rather
  than the transcription-model introduction.
- Added first-run setup regression checks to the existing source verifiers.
- No changes to model files, inference, speaker diarization, or downloading.

## Speaker diarization

Qwen3-ASR 0.6B does not natively output speaker identity. WhisperRecorder's
optional, separate CPU sherpa-onnx speaker diarization can be used to label
transcripts from Qwen. Because Qwen's playback word times are estimates,
speaker-to-word matching at short speaker turns may be approximate. The
speaker pipeline itself is unchanged in this revision.

## Verification limits

Host production verification and package integrity should be checked before
use. Android/NDK compilation, lint and Snapdragon phone behavior need a
separate GitHub Actions run and device test.
