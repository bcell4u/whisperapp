# WhisperRecorder v6.5.41 — Diagnostics Kotlin Compile Fix

This release follows v6.5.40 and fixes the GitHub Actions failure at `:app:compileDebugKotlin` in `SettingsSubscreens.kt` (`Unresolved reference: preview` at four locations).

The v6.5.40 Omi synthetic compatibility-card removal also accidentally removed the `preview` Compose state belonging to the **general transcription log**. Restoring that single state declaration preserves the log refresh, clear, and recent-log rendering controls while keeping the obsolete Omi compatibility probe absent.

The `verify-retired-options.py` source check now guards against accidentally removing the normal Diagnostics preview state. No changes were made to the Qwen hybrid, QNN encoder, CPU decoder, production Omi NPU backend, model weights, AIDL contracts, storage or experimental memory settings. No reinstall, downloads or cloud compilations are required.

Version: **6.5.41**, Android version code: **65041**. Local source verification passes; a full Gradle build and S26 testing are pending GitHub Actions. Existing `.dev` installations can be updated without clearing app data.
