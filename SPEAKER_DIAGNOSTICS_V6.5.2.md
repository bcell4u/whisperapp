# WhisperRecorder v6.5.2 — Speaker Diagnostics Release

## Trigger

A user reported `Speaker identification failed: com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization` on a device. The screenshot does **not** include the throwable class, stack trace, cause chain or linker errors, so it cannot distinguish `NoClassDefFoundError`, `ExceptionInInitializerError`, `UnsatisfiedLinkError` or another problem. The preceding v6.5.1 release was built after isolation of sherpa's `libonnxruntime.so` from the QNN runtime.

## Source changes

- The Recording screen shows the exception type and a **View speaker diagnostics** button after failures. The dialog supports selecting text, Copy, Share, and Close.
- The report records the last speaker-processing stage, full exception/cause chain and up to 24k characters of stack trace, Android/build version, device/ABI, sherpa Java class resolution, installed APK native libraries, model file existence/size and time of failure.
- Reports are stored in private app storage and scoped to the recording. They survive reopening the recording and are cleared after a successful speaker transcript. There is **no automatic upload**. Users should review exception strings for possible device-local file paths before sharing.
- Errors are logged to Android logcat as `WhisperSpeaker`, for cases where the on-screen report cannot be shown.
- The CI APK audit now uses Android SDK `dexdump` to verify the diarization classes remain in the produced APK after R8, in addition to its existing `.so` checks.
- Release identity advances to `versionName=6.5.2`, `versionCode=65002`.

## Why we did not patch the actual native failure yet

Only the error message is available. The exact root cause is **not established**, and changing ONNX Runtime versions, JNI loading order or Gradle `pickFirst` without a stack trace risks breaking the existing Parakeet NPU backend. This is a diagnostics release, not proof of a fixed speaker runtime.

## Next reproduction

1. Build v6.5.2 from GitHub using the same permanent signing key.
2. Confirm existing Parakeet NPU transcription still works.
3. On a short recording with at least two speakers, press **Create speaker transcript**.
4. If it fails, open **View speaker diagnostics** and use **Copy** or **Share**. Inspect for personal paths before sending the report.
5. Send that report and the latest GitHub Actions build log for a targeted correction. If the app terminates without a dialog, collect device logcat lines tagged `WhisperSpeaker` or AndroidRuntime (`adb logcat -d -v time`).

## Validation actually performed

- Source and feature-gate verification: PASS.
- Diagnostic collector Kotlin type-check against test Android stubs: PASS.
- Report generation, Throwable cause chain and per-recording persistence/clear behavior: PASS (stubbed Android environment).
- CI speaker DEX class audit: PASS with mock input and correctly fails when classes are omitted.
- Workflow YAML syntax and shell script syntax: PASS.
- New complete ZIP integrity and Git patch application: verified before delivery.

A full Android/NDK build, real native-library linking, and on-device reproduction are **not** available in this environment. Do not infer that speaker diarization is repaired or production-ready from these source checks alone.
