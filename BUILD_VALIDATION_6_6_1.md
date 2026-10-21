# WhisperRecorder Qwen GGUF Lab 6.6.1 — build and logging validation

## Delivery status

**Source prepared; NOT an Android APK compile.** The 6.6.0 S26 on-device 19-segment report (user-supplied) succeeded, but this 6.6.1 source update has not been compiled by Android Gradle or exercised on the phone. This runtime has neither the Android SDK/NDK/Gradle setup nor access to the user's GitHub repository, so a new APK and automatic GitHub release could not be produced locally.

## Changes

- Version 6.6.1 / code 66001, debug variant `6.6.1-dev`, package ID `com.example.whisperrecorder.qwengguflab.dev` and auto-release tags `v6.6.1-lab.<run>.<attempt>.g<sha>`.
- Per-segment cleanup of Qwen protocol header `language English<asr_text>` in the displayed transcript; token generation and native/QNN runtimes not modified.
- Scoped-storage `.log` written after each successful, failed or cancelled transcription attempt into `/Download/SFSU/Whisperapp/Logs` on the phone via Android `MediaStore.Downloads`, using `RELATIVE_PATH` and `IS_PENDING`. The user sees a success filename or save failure.
- Automatic log excludes transcript, recording, token IDs, user filenames, private paths, and exception messages. Numerical metrics, timestamps, status, execution-provider claim, CPU/RSS, model loads, EOS and M-RoPE-unqualified label only. Each log name is unique. Incomplete own MediaStore entry deleted only if save fails.
- Existing QNN encoder download/manual import, Q8 GGUF downloader, 4-thread ARM64 dotprod decoder and existing signing-secret names preserved.

## Local verification actually executed

- `scripts/verify-lab-source.py`: PASS for source inventory, package/version identity, manifest, model downloader, privacy and logging gates, native source contract, and auto-release workflow.
- Android manifest and resource XML parsing: PASS.
- Workflow YAML parsing: PASS; `scripts/activate-lab-workflow.sh` shell syntax: PASS.
- Kotlin 1.9 JVM compilation of the updated `QwenGgufLabActivity`, `QwenLabDiagnosticLog` and `QwenLabTranscriptCleaner` against representative Android stubs: PASS. **This is not full Android/Kotlin/Gradle compilation.**
- Kotlin 1.9 test harness for transcript header cleanup, exact public Downloads relative path, exclusion of sensitive transcript and private path keys, and delete-on-write-failure: PASS.
- ZIP reading/CRC and patch application check against prior complete auto-model-download source: verified when artifacts were produced.

## Outstanding gates

1. Commit **the source update** to the separate GitHub Lab repository `main`; the existing `.github/workflows/qwen-gguf-lab-6.6-autorelease.yml` should build `:app:assembleDebug :app:testDebugUnitTest`, verify the artifact and automatically publish a prerelease tagged `v6.6.1-lab.*` **if** every CI gate passes. The old production `.github/workflows/build.yml` must not remain in that repository.
2. Confirm Actions shows successful signing, APK package ID/version and release asset. Stable future in-place Lab upgrades require the same signer as the installed Lab; otherwise uninstall the old Lab before installing.
3. Perform **one** normal S26 transcription, verify spoken content no longer has Qwen protocol headers, and open **My Files → Internal storage → Download → SFSU → Whisperapp → Logs** to verify the `.log` exists and is metadata-only. Avoid uploading private transcript/audio.
4. Device verification of MediaStore behavior and M-RoPE/medical term correctness is still pending. Force-stop/process death before `finally` may prevent log creation.

Models, recordings, private app state, and signing credentials are **not** included in these ZIPs.
