# WhisperRecorder 6.7.5 — model card progress and auto-use

Version 6.7.5 (`versionCode=67005`). Incremented from 6.7.4.

- Models & downloads no longer shows live model-download status inside the generic Transcription models introduction. Each model (including Qwen 0.6B) displays its own progress, failed, cancelled, or completed status within the corresponding card. Non-download selection/cleanup feedback appears via a Snackbar instead.
- After verified download and installation, Qwen3-ASR 0.6B and Parakeet/Omi Qualcomm NPU packages are selected automatically in the foreground download service. Existing Whisper and local GPU download routines already perform this selection.
- Do not auto-select an incomplete/cancelled/failed package. If model verification or selection fails, the download state reports an error instead of announcing an active model.
- Qwen uses overlapping ~12-second decode windows and stitches text into one visible transcript; timestamps are estimated, not word-accurate. This release does not change speech alignment or model decoding.
- Fixed misleading "Segment 1 of 1" during Qwen inference. The app now reports the actual number of 12-second QNN windows as each encoder window completes, and shows an indeterminate progress bar during both encoding and llama.cpp decoding. This is truthful activity indication, not an invented decoding percentage. The recording-list UI, recording-detail UI, and foreground notification are updated. Other engines retain their normal outer-chunk percentage progress.

Validation: production source and Qwen-specific static audits; full Android/NDK build, lint and device playback tests remain CI/on-phone requirements.
