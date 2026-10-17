# WhisperRecorder v6.5.37 — APK audit compatibility hotfix

Source base: v6.5.36 consolidated Qwen hybrid and v6.5.35 diagnostics in Workspace Logs. Android `versionCode=65037`; debug package `com.example.whisperrecorder.dev`.

## Reason for update

GitHub Actions built and signed the v6.5.36 debug APK successfully and the JVM tests passed. Its post-build APK audit then failed at `APK DEX audit is missing expected ASCII marker: Use NPU`. That string is obsolete UI text, not a runtime invariant. The same failure was documented and addressed earlier in the experimental Qwen branch.

## Changes

- Reuse the established engine-ID audit: require `parakeet_npu`, `omi_npu` and `qwen3_hybrid` in APK DEX strings instead of `Use NPU`.
- Add source regression coverage to prevent that particular audit regression from returning.
- Bump version source to 6.5.37 / 65037.

No QNN contexts, decoder graphs, weights, model storage, thermal settings, diagnostic behavior, application ID or signing configuration changed.

## Validation boundary

Source and regression checks must pass; a complete Gradle build and S26 inference remain for GitHub and the physical phone. This is not a measured Qwen speedup. Use the `.dev` APK and preserve all installed model data.
