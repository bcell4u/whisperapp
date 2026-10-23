# WhisperRecorder v6.7.0 — production 6.5.22 integration and post-integration audit

## Source lineage and scope

- BASE: genuine WhisperRecorder v6.5.22 source (`whisperapp-6.5.22.zip` from the New Chat Asset Kit). The independent v6.6 Lab fork is *not* the production application base.
- VERSION: v6.7.0, versionCode 67000; permanent production application ID `com.example.whisperrecorder`; existing debug `.dev` suffix retained.
- ADDED ENGINE: `qwen3_06` Qwen3-ASR 0.6B: ORT QNN 12 s audio encoder, 156×1024 float features, Q8_0 GGUF llama.cpp v0.6.0, ARM64 dotprod, 4 threads, one decoder model load per 240 s recording slice; 1 s optional overlap, conservative stitching, greedy decoder.
- REPLACED: legacy Qwen3-ASR 1.7B and its entire `transcribenative` JNI/Gradle module are removed. PRESERVED: independent Whisper, Parakeet GPU/NPU, Omi GPU/NPU, recorder, storage, playback, diarization and existing model-card UI.
- NOT PORTED: any Qwen Lab benchmark UI, logits exports, synthetic fixture/golden files, reference/model numerical verification workflows, experimental performance switches, logit probes or Lab package ID.

## Models and UX

- Under Models & Downloads → Advanced Local ASR, the sole Qwen 0.6B option uses the established `LocalAsrModelCard`, with combined size label `~1.20 GB total · 805 MB Q8_0 + ~391 MB QNN encoder`, Qwen model link, Download/Use/Remove controls.
- GGUF decoder: `ggml-org/Qwen3-ASR-0.6B-GGUF`, pin `928ab95`, file `Qwen3-ASR-0.6B-Q8_0.gguf`; expected 804,749,248 bytes; SHA-256 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`.
- QNN encoder: `drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81` main manifest and referenced `encoder-qnn-fp16.bin`, `tokenizer.json`. Validate manifest identity, device, file lengths and SHA-256. The encoder is device-specific and not an official general Android Qwen distribution. No QNN CPU fallback.
- Both encoder and decoder must verify before Use is enabled. Downloads are on-demand, stream to app-private `.part` files with bounds, digest verification and transactional commit. A successful content fingerprint is cached while size/mtime remain unchanged to prevent repeated >1 GB hashing during model card refresh.
- Removal deletes its own decoder and QNN encoder files but not other model packages; normal existing SAF persistence rules remain in place.

## Runtime and source code efficiency

- One resident llama.cpp model per bounded multi-window slice; a 220 s lecture requires ~20 windows. No per-window model reload.
- Dedicated isolated `:qwen06` Android worker service for ORT-QNN + native subprocess. Shared-memory PCM avoids sending megabytes through Binder; bounded private scratch files are wiped after processing; no native token IPC or private audio in app diagnostic logs.
- Existing model segmentation applies a 240 s hard cap and one-second chunk overlap for `qwen3_06` rather than per-12-sec independent model loads.
- Native decoder is built `-march=armv8.2-a+dotprod`, forces exactly four decoder/batch threads and an ARM HWCAP dotprod check; no speculative Flash Attention or RoPE changes were introduced.
- The 0.6B engine is not included in the automatic preferred model list; selection is explicit, preserving existing auto-selection behavior.
- Fixed a Kotlin `LongRange` type mismatch, replaced Java API calls that were unnecessarily compatibility-sensitive, and preserved source-verifier expectations for existing Omi segmentation.
- All new file IO, model hashing and downloader operations happen in worker/background execution paths. Background executor and large model checksum caches avoid Compose-thread/refresh churn.

## Source quality / orphan checks

- Source audit checks the production Qwen invariants: model card, version, automatic push debug workflow with manual release gate, no Lab fixtures, no bundled model weights, removed legacy transcribe.cpp backend, release signing and private IPC. `scripts/verify-source.sh` additionally preserves the original 6.5.22 feature, speaker, Omi and recording guards.
- C++ CLI syntax checked using an API-shaped header for pinned llama v0.6; standalone Kotlin Qwen mel + transcript cleanup/merge compiled by `kotlinc` together with production `TranscriptCleaner`.
- Inspected all new Qwen classes for unwanted benchmark/fixture/probe paths, `Log` calls, orphan legacy Qwen Lab functions, implicit model downloads, and accidental model path diagnostics.

## GitHub build protection / execution caveat

- After the APK build, CI runs Android lint and records its exit code, preserving its log and HTML/XML reports in the build artifact. Lint failures require review before production deployment, even when the APK build itself succeeds.
- `.github/workflows/build.yml` now **builds debug `.dev` APKs automatically on every branch push**, with **debug `.dev` as the explicit default regardless of configured signing secrets** and a manual-only opt-in release variant; Android lint follows the selected variant, with no automatic GitHub Release/tag. It stages the pinned llama source at `d81235049384534c167caea52b85a694f6103d14`, cross-compiles one static ARM64 dotprod PIE, stages it as `libwrqwen06.so`, runs JVM tests, builds an APK, audits native/DEX entries, confirms the package/version/signing identity, and uploads the APK plus proof. Other production native dependencies and signing keys are preserved.
- **No Android SDK/NDK or runner was available in this environment.** The actual Android Gradle/AIDL/native link/build, APK size and install/update behavior are **NOT YET VERIFIED**. The host C++ check used a stub, not real pinned headers. The app-private executable launch/SELinux policy is also **NOT device-qualified**. These are gating risks, not claimed successes.
- **Do not install a signed 6.7 candidate over 6.5.22 until the GitHub build, checksum, UI, and S26 transcription checks pass and recordings are backed up.** Without production signing secrets, a debug `.dev` APK can be installed alongside the current production app.
- QNN encoder assets are not bundled: an on-device network download and first-load QNN/HTP EP check is required. The 6.7 S26-specific runtime is not qualified for other Snapdragon or Android devices.

## Required post-build verification on GitHub/S26

1. Run Actions → Build Android APK → Run workflow. Verify source gates, native CMake build/ELF, Gradle unit tests, `assembleDebug`/`assembleRelease`, `apksigner`, native ABI entry list, DEX model-card markers, package ID, versionCode 67000, model assets not in APK, and `build-proof.txt` SHA.
2. Install the debug `.dev` artifact **alongside** 6.5.22 for the first evaluation. Confirm Advanced Local ASR lists only the 0.6B Qwen card, model links open, Download/Cancel/Retry/Remove/Use work, and downloaded size matches expectation. Validate storage availability > ~2 GB for model plus temporary download space.
3. Confirm QNN EP loads (no silent CPU fallback), decoder executable starts without `Permission denied`/SELinux denial, isolated worker does not crash, cancellation cleans scratch, and the app handles unqualified devices safely.
4. Exercise a short clip (<12 s), a boundary clip (~13–24 s), and a long lecture (~220 s) with technical medical vocabulary. Verify saved transcript, no privacy leakage, sensible duplicate stitching and speed near the Lab baseline (~32 s / 220 s on the previously tested device).
5. Verify that Parakeet, Omi, Whisper, recording/player, speaker diarization, model deletion and user data have not regressed.

## Disposition

**Status: SOURCE-INTEGRATED / ANDROID-CI-COMPILE-FAILED-AT-HWCAP (FIX STAGED, RERUN PENDING) / DEVICE-QUALIFICATION-PENDING.** The requested post-build audit is prepared and enforced via GitHub APK checks but cannot honestly be called completed until an APK exists. No model performance or medical WER improvement is asserted solely from synthetic fixtures.

## 2026-10-22 source re-audit refinements (Android CI still pending)

- Removed the old 1.7B card, transcribe.cpp JNI/module, routing, obsolete adaptive retry and APK checks; added retired-model regression and upgrade-selection protection.
- Qwen 0.6B card now names its qualified phone build exactly: `SM-S942U only`. Runtime device gate remains unchanged.
- `QwenEncoderAssets` now recovers a verified `.previous` context after interruption between staging and commit instead of deleting the only good backup, and fingerprints the absolute location as well as size/mtime.
- Added cancellation gates after QNN execution and before starting the decoder subprocess.
- Model-download progress is monotonic across encoder context/tokenizer/decoder stage messages rather than jumping backward.
- GitHub push workflow automatically builds a safe `.dev` debug APK even when production signing secrets are configured; an explicit manual `release` choice is required for a production-package candidate.
- Expanded the Qwen static source audit to 27 checks, including recovery, cancellation, UI qualifier, auto-selection and safe default variant. Downloader SHA-256 is required, and generated decoder/llama checkouts are ignored by Git.
- Host `kotlinc` smoke tested mel output shape/finiteness, protocol cleanup, conservative seam stitching, and 240-second planning, without testing Android ORT/QNN.

**Validation boundary:** These are source improvements, not a compiled APK or qualified phone test. Re-run the GitHub job and the device checklist before upgrading the installed 6.5.22 release.
