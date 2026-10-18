# WhisperRecorder v6.5.35 — Diagnostic ZIPs in Whisperapp/Logs

- New diagnostic ZIPs are published to the existing user-selected `Whisperapp/Logs`
  workspace directory (Android SAF). No model/recording directories are modified.
- Private atomic ZIP generation; SHA-256 verification of the workspace copy before
  removing the private staging copy. If no workspace is configured or the provider
  is unavailable, the complete report remains in the existing app-private directory.
- Settings > Diagnostics lists, shares and deletes reports from both locations.
  Deletes affect only generated diagnostic ZIPs, not text logs or unrelated files.
- Development and release variants use distinct diagnostic ZIP names in shared Logs,
  so one variant cannot prune the other variant's reports.
- Existing private reports from v6.5.34 remain accessible. Reports are limited to
  8 files / 32 MiB in the available report locations.
- This release retains the clean v6.5.22 base and diagnostic foundation. It does
  NOT add the experimental Qwen3-ASR-0.6B hybrid, quantization or new QNN kernels.
- versionName 6.5.35; versionCode 65035; debug variant 6.5.35-dev.

Validation: run CI Android build, install the .dev variant as an update without
clearing data, select your workspace, create a manual ZIP, confirm it appears in
Whisperapp/Logs, share and delete it; then confirm automatic reports after a
short transcription. Revoke the workspace permission only in a controlled test
to verify that reports are preserved in the private fallback.
