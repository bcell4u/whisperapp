# WhisperRecorder Qwen GGUF Lab v6.6.4 — validation and build status

**Status: full-source update prepared; no Android APK was compiled in this session.**

## Scope
- Based on the complete 6.6.3 overlapping-segment Lab source ZIP.
- Version bumped to `6.6.4` / Android code `66004` in `version.properties`, verifier, and auto-release workflow.
- Kept the 6.6.3 12-second fixed QNN encoder windows, optional 1-second overlap, single GGUF model load, Q8_0 dot-product native decoder, four CPU threads, automatic Hugging Face model download, public model backups, and privacy-safe Downloads logs.
- Adopted exact 24-word tail/head boundary comparison and the 12-character minimum for a two-word match from the v6.5.22 production `TranscriptCleaner`. Uses the existing `TranscriptCleaner.cleanChunk` to remove only strong three-or-more-copy loops inside overlapping segments. Zero-overlap path remains unchanged.
- Added `stitched_repetition_copies_removed` as an integer to the existing privacy-safe diagnostic report (no text or audio logged).

## Tests actually performed in this session
- `python3 scripts/verify-lab-source.py`: PASS (static source and workflow gates, not a compile).
- `git diff --check`: PASS.
- Android XML parsing: PASS.
- YAML parsing of `.github/workflows/qwen-gguf-lab-6.6-autorelease.yml`: PASS.
- `bash -n` for workflow script blocks: PASS.
- `kotlinc` 1.9.0 JVM compilation of production `TranscriptCleaner`, updated Qwen stitcher, and the JUnit-style stitcher tests with a minimal JUnit shim: PASS.
- 13 stitcher unit tests invoked and passed in JVM harness (short and long seam matches, two-word safeguard, zero-overlap unchanged, repeats, medication terms, punctuation, max 24-word boundary, malformed overlap rejection): PASS.
- Source archive integrity and rebase-patch application checked before delivery (see generated SHA256SUMS).

## NOT verified
- Android SDK/NDK and a full Gradle toolchain are not installed in this execution environment, and no authenticated GitHub Actions connection was available. Therefore **no Android APK compilation, signing, installation, or GitHub Release occurred in this session**.
- No S26 device test of 6.6.4 was performed. No word error rate or accuracy gain is claimed.
- M-RoPE equivalence and model parity are still not qualified.

## Recommended next gate
Push the complete source ZIP contents to the separate GitHub Lab repository's `main` branch. The workflow should run `:app:assembleDebug :app:testDebugUnitTest`, verify app ID `com.example.whisperrecorder.qwengguflab.dev`, version `6.6.4-dev`/`66004`, ARM64 native decoder payload and APK certificate, then publish only if those checks succeed. Keep stable signing secrets across updates to avoid uninstalling.

After a successful APK build, transcribe the same lecture **once with one-second overlap**, compare its copied text against the existing 6.6.3 and 6.6.1 transcripts, and review the new numeric stitching diagnostic. Do not repeat thermal/thread sweeps.
