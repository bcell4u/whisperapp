# WhisperRecorder v6.5.27 — APK DEX Audit Fix

## Failure diagnosed

The GitHub Actions v6.5.26 log confirms source verification, JVM tests, Kotlin compilation, native compilation, APK packaging, signature verification, and native ELF checks succeeded. The **Audit built APK** step failed at `APK DEX audit is missing expected ASCII marker: Use NPU`.

The common model card now uses **Use**, so the older NPU-specific button text is no longer a reliable compiled-code marker. This was an audit-only failure, not Qwen runtime evidence.

## Correction

- APK DEX audit checks the three runtime engine identifiers `parakeet_npu`, `omi_npu`, `qwen3_hybrid` rather than removed presentation text.
- Source verifier enforces these audit markers against the identifiers in `AsrEngine.kt` and blocks reintroduction of the obsolete label check.
- Version advanced to v6.5.27 (`versionCode=65027`); application ID and signing remain unchanged.
- No modifications to downloaded models, Qwen Stage I v3 manifest, NPU execution, or the existing Omi/Parakeet inference paths.

**Verification:** Local source checks are run on this release. The previous GitHub log verifies v6.5.26 APK compilation but does not verify a v6.5.27 APK or physical-device transcription; GitHub Actions must rerun.
