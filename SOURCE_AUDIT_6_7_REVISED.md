# WhisperRecorder v6.7.0 — revised source audit and Android build gate

## Deliverable and provenance

The genuine v6.5.22 `whisperapp` source is the baseline. This package revises the already staged v6.7.0 production integration; it is **source, not a built or installable APK**. The archived v6.6 experimental Qwen Lab diagnostics, golden fixtures, numerical sweeps and experiment APK variants are not included. Existing production speaker/Omi diagnostics and production test gates remain intact.

The Qwen3-ASR 0.6B option uses a device-qualified QNN/HTP encoder (about 391 MB) and an independent pinned Q8_0 llama.cpp decoder (804,749,248 bytes), an approximate combined install size of 1.20 GB. The model is displayed in the established Advanced Local ASR model-card component. The old Qwen3-ASR 1.7B card and transcribe.cpp implementation have been removed. The 0.6B hybrid requires S26 SM-S942U; it does not silently fall back to CPU for encoder execution.

## Concrete source refinements

1. QNN download atomic-replacement recovery protects the last verified encoder package following an interrupted `.pending` → live-directory swap. A completed new install wins over a stale backup; the backup is verified before any restore.
2. Worker cancellation now checks immediately after QNN execution and before decoder process launch, avoiding unnecessary processing after cancellation.
3. Foreground download progress never decreases when switching among the encoder manifest, context, tokenizer and decoder messages.
4. Advanced Local ASR metadata explicitly says `S26 SM-S942U only`, alongside the full combined size and 30 supported languages.
5. Manual CI workflow runs Android lint against the selected debug or release variant, and defaults to `build_variant=debug` even if release signing secrets are present. Release APKs require explicit `build_variant=release` and the original release credentials; workflow has no push/automatic publish triggers.
6. Existing catalog regression tests now cover Qwen06 ID, bytes, tokenizer/encoder qualifier, and absence of the retired 1.7B option and backend.
7. Qwen production static invariants increased to 27 checks. Model payload downloads now require a non-null SHA-256 in every call; the workflow-generated decoder executable and downloaded llama.cpp source are Git-ignored, not bundled in the repository.

## What can be verified without Android

- `scripts/verify-source.sh`: production model, recording, speaker, NPU and Omi source guard checks.
- `scripts/audit-qwen-production.py`: 27 static invariant checks.
- Kotlin CLI smoke: mel dimension/finiteness, Qwen transcript header cleanup, overlap stitching, segmentation limit, and distinct Qwen engine IDs.
- YAML load, Android manifest XML syntax, shell `bash -n` and Python compile checks.
- A 6.5.22→6.7.0 diff, and source archive CRC and file-inventory scans.

## Android/CI validation still blocked in this environment

No Android SDK/NDK or access to the user's GitHub Actions runner was available. A full `assembleDebug`, Android AIDL/Kotlin/ORT compile, ARM64 llama v0.6.0 native link, APK signing, DEX/resource audit, and actual S26 tests **were not run and are not claimed to pass**. In particular, treating an ELF PIE executable packaged as `libwrqwen06.so` as an executable via `ProcessBuilder` may be blocked by device Android/SELinux policy. If that is observed, replace the executable arrangement with a supported JNI/library-loaded runtime rather than weakening application security. Source tests cannot prove the QNN provider loads or transcribes speech.

## CI and device proof required before installation

1. Push the complete source into a separate test branch, removing any old files omitted from this archive in Git rather than simply overlaying a ZIP.
2. In **Actions → Build Android APK → Run workflow** choose the default **debug**. Inspect the native build, unit tests, Gradle/AIDL compilation, Android lint and packaged APK proof. Do not select release initially.
3. Verify the produced `.dev` ID (`com.example.whisperrecorder.dev`), code 67000, name `6.7.0-dev`, signer fingerprint, source hash, and packaged native ELF/DEX assets; no model files should be embedded.
4. Install `.dev` beside the original production app on S26 SM-S942U. Test the 0.6B card and both downloads with checksum verification, retired 1.7B model absence, cancel/failure retry, and Remove without deleting other models. Confirm real QNN/HTP with CPU fallback disabled.
5. Transcribe short, boundary-crossing and ~220-second lecture samples, check actual latency, RSS, speech quality, transcript saving, service lifecycle, and cleanup. Confirm existing production recording, playback, storage, diarization, Parakeet, Omi and Whisper regressions do not appear.
6. Only after a passing device report and backing up recordings consider an **explicit** signed-release run and a production upgrade. Re-run a post-build dead-code/efficiency audit using the actual lint/compiler diagnostics.

## Source audit boundaries

A static inventory did not find a Qwen Lab source tree, lab fixture, bundled GGUF/safetensors/QNN-weight payload, or native token export to app diagnostic logs. It cannot prove there are no unreachable methods in Android-resolved code or unexpected device linker errors. Retain unresolved gates rather than deleting production functionality speculatively.
