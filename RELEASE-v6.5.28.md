# WhisperRecorder v6.5.28 — Qwen ORT-QNN EPContext path fix

## Actual S26 failure

2026-10-11 `WhisperRecorder-transcription-log.txt` shows successful OGG/Opus media inspection followed by Qwen initialization failure:

`ORT_INVALID_GRAPH: External mode should set ep_cache_context field with a relative path, but it is an absolute path`.

The previous Qwen backend wrote a small ONNX EPContext wrapper under `cacheDir` and encoded an absolute app-private path to the downloaded encoder QNN `.bin`. ONNX Runtime QNN requires a **relative** path in `ep_cache_context`, resolved against the ONNX wrapper model directory.

## Fix

- Generate `qwen3-encoder-epcontext.onnx` **next to** the already verified `encoder-qnn-fp16.bin` in the selected v1/v2/v3 package directory.
- Use `NpuEpContextModel.write`, which enforces same-directory placement and writes only the binary **basename** to the ONNX EPContext attribute.
- Create the QNN encoder ORT session from that on-disk wrapper; retain disabled CPU fallback.
- On session close/error, remove just the generated wrapper and any legacy cache wrapper. Independent manual Qwen cleanup handles interrupted sessions across all three package versions. Never delete the model `.bin`, decoder weights, app recordings, or tokens.
- Add focused JUnit checks and source-verification guards against returning to an absolute EPContext path.
- Increment releaseVersion/versionName to 6.5.28 and versionCode to 65028 without changing signing/application IDs or other ASR backends.

## Validation scope

Source verification and standalone Kotlin tests can run locally. Full APK compilation and real S26 QNN EPContext load/inference are **not** demonstrated by this patch; rerun GitHub Actions and the on-device Qwen compatibility test. A further QNN SDK/graph compatibility error is possible after this path issue is resolved.

## Persistent Qwen package migration (same release)

- Qwen models now live in `filesDir/whisperapp/models/qwen3-asr-0.6b/<stage>/`; app-private storage persists across restarts and in-place updates, not uninstall or Clear storage.
- Installed Stage G/H/I model directories are *renamed* from the former `filesDir/qwen3-hybrid/<stage>/` without copying gigabytes or performing a new network download. An unsuccessful filesystem rename leaves the previous directory usable.
- Existing partial downloads can resume. Each stage's `READY.json`, manifest, hashes, and adjacent ONNX weight files move together.
- Explicit deletion includes only the Qwen stage directories in either storage layout; temporary-wrapper cleanup deletes only named wrappers, never model weights.
- The Qwen model card now reports its actual installed Stage G/H/I variant rather than always labeling Stage H.
