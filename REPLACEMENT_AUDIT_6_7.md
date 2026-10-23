# WhisperRecorder v6.7.0 — Qwen 0.6B llama.cpp-only replacement audit

## Requested correction and release status

This **source-only** revision supersedes the prior 6.7 draft that mistakenly preserved Qwen3-ASR 1.7B and its transcribe.cpp engine. The only Qwen ASR choice in Advanced Local ASR is **Qwen3-ASR 0.6B**, using the Snapdragon S26 ORT-QNN/HTP encoder plus a resident, 4-thread ARM64-dotprod llama.cpp Q8_0 decoder. The 1.7B model card, download metadata, transcribe.cpp JNI Kotlin wrapper, entire `transcribenative` Gradle module/source, Gradle dependencies, ProGuard rule and engine enum/dispatch are removed. Independent Whisper/Parakeet/Omi/diarization/recording features remain.

This revision retains **versionName 6.7.0**, **versionCode 67000**, production application ID `com.example.whisperrecorder` and safe side-by-side debug suffix `.dev`. No automatic publication or release build is enabled.

## Corrected code paths

- The Qwen download card continues to use `LocalAsrModelCard` and download/Use/Remove actions; metadata shows a **0.6-billion-parameter** model and **~1.20 GB** combined on-device download (~805 MB GGUF + ~391 MB device-specific QNN encoder/tokenizer), 30 languages and SM-S942U device qualification.
- Existing HTTPS/Hugging Face download flow verifies expected lengths and SHA-256 before enabling Use. Weights, QNN contexts, tokenizer and GGUF are NOT distributed inside the APK.
- `AsrSessionFactory` routes only Qwen `qwen3_06` to `Qwen06Session` (QNN encoder and llama.cpp decoder). `AsrEngine.QWEN3` and `TranscribeCppSession` are gone. No transcribe.cpp native artifact is built/packaged.
- `TranscriptionService` no longer carries an obsolete recursive generation-truncation retry designed for the removed 1.7B engine; normal inference and timestamps remain. `SegmentationPolicy` retains the 0.6B 240-second cap with 1-second overlap and eliminates the retired 60-second cap.
- Existing settings with saved engine ID `qwen3` clear the obsolete engine preference **and incompatible model path**; the model inventory then reselects an installed working engine. The on-disk former 1.7B GGUF, SAF files and recordings are **not silently deleted**. Selecting Qwen 0.6B remains an explicit action once its files are installed.
- Runtime thread-setting text now correctly states that Qwen 0.6B uses **fixed four decoder threads**; the settings slider affects Whisper, not Qwen's fixed NPU+CPU runtime.
- Workflow APK checks **forbid** `libtranscribe_cpp_jni.so` and the obsolete 1.7B DEX model label, while requiring `libwrqwen06.so` and the 0.6B DEX label. Unit tests now require absence of the retired model and engine ID.
- Old experimental Qwen Lab fixtures/logit probes, benchmark APK variants and synthetic diagnostics are not included; unrelated existing production diagnostics remain.

## Checks actually executed

- `scripts/verify-source.sh`: **PASS**, including existing speaker, Parakeet GPU/NPU, Omi GPU/NPU, recording, packaging and UI/source guards.
- `scripts/audit-qwen-production.py`: **27/27 PASS**, including absence of the native module, old catalog and engine dispatch, APK audit prohibitions, safe preference migration and Qwen06 route.
- Host Kotlin CLI smoke: **PASS** (retired Qwen engine ID fallback, acceleration policy, Qwen06 segmentation, sample mel dimensions/finiteness, transcript cleanup and overlap stitching).
- GitHub workflow: YAML parse **PASS**. Android manifest XML parse **PASS**. All scripts: Bash syntax checks **PASS**, Python compile **PASS**.
- Source verification, ZIP archive CRC check and a patch-vs-pristine-v6.5.22 reconstruction are performed before packaging the final artifacts.

## Remaining non-negotiable validation

**No Android SDK/NDK, linked Qwen Android native binary, GitHub Actions result, signed APK, or physical S26 device was available in this environment. This is not a verified Android build or proven end-to-end transcription.** GitHub must compile Gradle/AIDL/ORT + pinned llama.cpp, run unit tests/lint, verify the packaged ELF/DEX/APK signature and produce a matching build proof. The PIE executable packaged as `libwrqwen06.so` may be subject to Android execution/SELinux restrictions; only actual S26 SM-S942U testing can verify this arrangement. If blocked, implement supported JNI/library loading rather than chmod/security bypass.

Use the **debug `.dev` variant first**, beside the installed 6.5.22 production package, and verify model download, Use/Remove, QNN fallback disabled, short/boundary/220-second speech, cancellation, transcript save, playback and all independent production engines. Back up recordings before any future production upgrade. A *post-build* orphan/performance/lint audit cannot be completed until actual CI and device evidence is available.
