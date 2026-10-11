# WhisperRecorder v6.5.25 — Qwen compact schema v2 from HF main

- Qwen 0.6B new model downloads resolve from Hugging Face `main`, not the temporary `compact-fp32-v2` branch.
- Requires first publishing **verified Stage H1 lossless compact package** to main using Builder v0.3.65 H3. If main still hosts Stage G v1, installation fails closed on schema check.
- Keeps SHA-256 verification, exact ONNX v2 shared-weight contract, old locally installed Stage G v1 support and optional old-model removal.
- Stage I experimental FP16-storage schema v3 is NOT supported; do not publish it to main until Android explicitly supports it and phone checks pass.
- Preserves NPU, GPU, CPU group ordering and uniform model cards in onboarding and Settings.
- No modification to Parakeet NPU, Omi, signing identity, or selected model preferences.
- Android Gradle compilation and S26 runtime testing remain pending.
