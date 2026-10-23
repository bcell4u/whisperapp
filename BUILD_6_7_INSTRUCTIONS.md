# Build 6.7 and verify before replacing production

1. Copy the full source ZIP contents (or apply the v6.5.22→v6.7 patch) into your `whisperapp` Git checkout. Preserve signing secrets, the original installed app, and recordings.
2. Commit and push the v6.7 source to any branch. **GitHub Actions automatically starts the debug `.dev` APK build on push.** The **Actions → Build Android APK → Run workflow** button also supports optional manual runs; leave `build_variant=debug` (safe default). Only a manual `release` selection with signing secrets builds a production-package candidate. No GitHub Release is published automatically.
3. The workflow fetches pinned llama.cpp v0.6.0 and builds the ARM64 dotprod decoder, runs existing production source/JVM tests and the new Qwen production source gate, then builds Android. It produces a debug `.dev` APK by default even when release signing secrets exist. Choosing `build_variant=release` explicitly requires the original signing secrets and produces a signed production-package release candidate (not auto-published).
4. Download `WhisperRecorder-build-<run>` from Actions artifacts. Verify the APK, `build-proof.txt`, `android_lint_exit`, and the attached lint reports, then perform the UI, model download and S26 smoke tests in `INTEGRATION_AUDIT_6_7.md`.
5. **First use debug `.dev`** so v6.5.22 stays installed and untouched. Do not distribute or upgrade production until real CI and device checks pass.

The Qwen3-ASR 0.6B card downloads the ~805 MB GGUF decoder plus device-specific ~391 MB QNN context/tokenizer on demand. Model weights are not bundled with the APK. The QNN encoder is currently targeted at Snapdragon S26 model SM-S942U.
