# WhisperRecorder Android v6.5.33 — Diagnostic Bundles, Minimal Runtime Changes

**Source release; not a compiled or physical-device validated APK.** Bumps `releaseVersion`, `versionName`, `versionCode` to `6.5.33`, `6.5.33`, `65033`. Application ID, signing and NPU/GPU/CPU backend selection unchanged.

- Reuses central `DiagnosticLogger` for automatic bounded `.zip` bundles after caught app exceptions, including Qwen `:qwen` worker and existing QNN `:npu` worker error responses. Expected user cancellations do not create error bundles. Uncaught native crashes/OOM may still need separate Android tombstones/logcat; this is not a crash-reporting guarantee.
- Each snapshot contains `failure-and-device.json`, up to 384 KiB of app log, 128 KiB of NPU worker log, a bounded exception stack, and a README. Device API/model, process PSS, available RAM and thermal status are recorded. ZIP snapshots are local until a user explicitly exports them.
- `Settings > Diagnostics`: Create & export snapshot ZIP, export latest ZIP, explicitly confirm Delete diagnostic ZIP files. Retain newest five bundles. Does not clear regular logs or user-selected `Whisperapp/Models`, model cache, recordings, original ONNX or QNN files.
- Text filtering best-effort redacts common token formats, storage and content URIs, and transcript-labelled lines. **Inspect the ZIP before sending**; no redactor guarantees removal of all sensitive text. Do not attach clinical/private recordings.
- Qwen SAF bridge and 2/4-thread experiment remain **unverified on physical S26**. This is diagnostic instrumentation, NOT a Qwen NPU decoder or proof of faster decoding.

Validation: run existing source verification, then GitHub Actions Gradle build, upgrade APK without uninstall, trigger a controlled error and inspect/export/delete a diagnostic ZIP; rerun Parakeet/Omi/Qwen smoke tests. Do not reclaim any private model duplicate until successful shared-folder phone transcription and full hash audit.
