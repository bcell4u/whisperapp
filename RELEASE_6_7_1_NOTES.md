# WhisperRecorder v6.7.1 - GitHub Actions CI cleanup correction

This patch addresses GitHub Actions run 108136917571. That run stopped during
`Audit Qwen 0.6B production integration before compilation` because the Git
checkout contained at least one retired Omi compatibility diagnostic artifact.
The clean v6.7.0 package passed the same audit; a ZIP overlay does not delete
older Git-tracked files.

## Changes

- Increment releaseVersion/versionName to **6.7.1**, versionCode to **67001**.
- Add a narrowly scoped source cleanup utility for exactly two obsolete files:
  `scripts/verify-omi-probe.sh` and
  `app/src/main/java/com/example/whisperrecorder/npu/OmiNpuProbeContracts.kt`.
- Execute cleanup in the GitHub build checkout before source gates; it prints a
  warning if obsolete files remain tracked in the repository. This makes the
  build resilient to copied-over source ZIPs, but **does not remove files from
  Git history or the remote branch**. Delete/commit these two files in Git.
- Keep source audits strict and add a descriptive repair command when obsolete
  diagnostics are detected. Do not loosen checks on the actual UI or IPC code.
- Preserve all prior 6.7.0 user-interface grouping, Qwen approximate timing,
  QNN download behavior and auto-published debug `.dev` prereleases.

## How to apply to an existing Git checkout

After overlaying the changed-files ZIP into the repository root, run:

```bash
bash scripts/clean-retired-omi-diagnostics.sh
git status --short
git add -A
git commit -m "Prepare WhisperRecorder 6.7.1 CI cleanup"
git push
```

The current source cleanup removes old diagnostic code only. **Do not delete**
`scripts/verify-omi-transcription.sh` or the real Omi NPU implementation.

## Verification status

The uploaded GitHub run did not reach NDK, Kotlin Android compilation, APK
packaging or Releases. Local source/static verification alone cannot establish
Android build or handset success. Re-run CI on 6.7.1 and attach the new logs if
there are additional compiler or packaging errors.
