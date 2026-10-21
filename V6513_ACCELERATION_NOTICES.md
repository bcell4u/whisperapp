# v6.5.13 — Acceleration priority and first-use notices

- Default when **no engine was explicitly selected**: verified Qualcomm Parakeet NPU > installed OpenCL Parakeet > installed OpenCL Omi > installed CPU Qwen3 > installed CPU Whisper. If no model is installed, setup still requires a download/selection. No automatic downloads or model substitutions.
- Explicitly selected engines remain selected across upgrades and restarts. The automatic first-run choice retains an `engine_auto` preference marker, so it can upgrade from CPU to GPU or NPU if a preferred backend is subsequently installed. WhisperRecorder does **not** override manual choices or silently switch models during a transcription.
- GPU and CPU selections each show a **one-time, persisted in-app dialog**. Selecting NPU does not show a warning. Both setup and Models & downloads screens share the same seen state.
- GPU dialog explains NPU incompatibility, OpenCL acceleration **when available**, and higher heat and battery use; CPU dialog explains that neither NPU nor GPU acceleration is used by the chosen model and that CPU may be hotter/slower.
- Only source and preference changes; audio inference, NPU runtime, signing and downloads are unchanged. Diagnostics emit ACCELERATION auto-select and ACCELERATION notice without audio/text data.
- Includes AccelerationPolicyTest.kt (tier classifications, fallback order, no installed model).
- No local Android SDK/Gradle; use .github/workflows/build.yml for real Android compile, JNI/native build and APK validation. If GitHub Actions fails, send the CI log before installing.
