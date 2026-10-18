# WhisperRecorder v6.5.42 — Qwen JVM JSON Test Fix

GitHub Actions progressed beyond source verification and Kotlin compilation in v6.5.41, but `:app:testDebugUnitTest` stopped with 2 failures out of 126 unit tests. Both failures occurred in `QwenDecoderRunConfigTest` when the host JVM tests executed Android `org.json.JSONObject` methods (`configRoundTripAndVersionedHandshake` and `rejectsUnsupportedParametersBeforeWorkerAllocation`). The Android SDK's mockable `org.json` classes are nonfunctional for local JVM tests.

This patch adds `testImplementation("org.json:json:20240303")` to the Android app module, providing a real JSON library for JVM unit tests only. The application continues using the Android platform `org.json` at runtime; no new Android runtime dependency or package installation is required. The cross-process Qwen configuration and safety-validation tests remain unchanged. A source-verification guard checks that the JSON test dependency is present and not added as a runtime implementation dependency.

No inference implementation, QNN encoder, ONNX CPU decoder, Omi/Parakeet/Whisper backends, model files, diagnostic storage, signing identity or package ID was changed. The existing `.dev` flavor remains side by side with production. Neither Qwen model data nor phone settings need to be cleared.

Version: **6.5.42**, Android version code: **65042**. Static source verification passes. The complete GitHub Actions Gradle/JVM build and S26 execution remain to be confirmed.
