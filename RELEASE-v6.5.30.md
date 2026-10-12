# WhisperRecorder v6.5.30 — Qwen installed-model detection

- Fix Models & downloads incorrectly showing Download for a usable, already-installed Qwen model when the last download activity belonged to another model.
- Always rescan installed Qwen model state asynchronously on opening Models & downloads or first-run setup; no internet or re-download required.
- Before starting any Qwen download, recheck the installed package to avoid accidental multi-GB duplicates.
- Add **Recheck installed Qwen model** recovery control in Models & downloads; show Ready/Incomplete/Not installed status without deleting files.
- Preserve app-private `whisperapp/models/qwen3-asr-0.6b` storage and existing legacy migration paths, model hashes, QNN context, Omi/Parakeet backends, model-group UI and signing IDs.
- `versionName=6.5.30`, `versionCode=65030`.

This is an Android source release. Run GitHub Actions and verify model detection on device without uninstalling or clearing app data.
