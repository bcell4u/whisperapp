# WhisperRecorder Qwen GGUF Lab 6.6.1 — transcript presentation + automatic logs

## Changes from last 6.6.0 auto-model-download source

- `version.properties`: 6.6.1 (Android versionCode 66001) and matching auto-release workflow tags and aapt gates. Package ID stays `com.example.whisperrecorder.qwengguflab.dev`, signing secret names unchanged.
- `Qwen3HybridWorkerService`: cleans only a recognised `language English<asr_text>`-style header **per decoded segment**. No change to tokenizer mapping, native token output, QNN encoder, decoder, model hashes, thread count, or M-RoPE calculations.
- `QwenLabDiagnosticLog`: scoped `MediaStore.Downloads` export after success, cancellation or failure. The public path is **Internal storage/Download/SFSU/Whisperapp/Logs/**. Each attempted run creates a unique `WhisperRecorder-QwenGGUF-YYYYMMDD-HHMMSS-xxxxxxxx.log` filename (UTC timestamp). Android creates the folder on first successful save.
- The Lab Activity shows `Diagnostic log saved: Download/SFSU/Whisperapp/Logs/<filename>` after each run; if MediaStore save fails, the app shows a non-sensitive error and does not alter the transcript.
- Model setup and manual imports remain the same; users do not need to recreate private models when updating a same-package/same-certificate Lab APK.

## What is written

A plain-text log records status (`SUCCESS`, `FAILED`, `CANCELLED`), last operation, timestamp, attempt wall time, encoded segment count, model-load count, stage timings, generated tokens/EOS, CPU/RSS, expected execution providers and an explicitly **unqualified** M-RoPE status. In case of failure it reports only the error *class*, never arbitrary exception text.

**Excluded by design:** transcription text, audio, token IDs/hashes, selected media name, private absolute paths, GGUF content, user information. This is a public Downloads folder; other apps and anyone with access to the phone may read these logs. Do not upload recordings or transcripts to GitHub. Each run adds a new log; the app does not prune unrelated files or previously created logs.

The implementation uses Android 10+ `MediaStore.Downloads`, `RELATIVE_PATH`, and `IS_PENDING`, without storage-wide permissions. If the app is force-stopped or the process is killed before the final callback, no log is guaranteed.

## Build and verification

```bash
python3 scripts/verify-lab-source.py
# Then commit the changed files (or full source) to main.
# .github/workflows/qwen-gguf-lab-6.6-autorelease.yml compiles/debug-signs,
# audits app id / 6.6.1-dev / code 66001 / native libs, and publishes a GitHub prerelease.
```

This environment has no Android SDK/Gradle; source preflight and Kotlin/JVM test-stub compilation are NOT equivalent to APK compilation. Verify the next Actions run and, only after it passes, do one ordinary lecture transcription and inspect the new `.log` in My Files. Check the text prefix is absent and the log contains metrics but no transcript. There is no need to repeat CPU/thermal sweeps.
