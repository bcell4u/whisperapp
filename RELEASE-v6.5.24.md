# WhisperRecorder v6.5.24 — Qwen compact Stage H + unified model menus

- Incremented Android `versionName`/`releaseVersion` to 6.5.24 and `versionCode` to 65024.
- Stage H decoder compact package: `main (compact schema v2)` branch, schema v2, `decoder_weights.data` shared FP32 sidecar.
- Preserved installed Stage G v1 package in its original directory; new/upgrade download targets compact v2 exclusively.
- Verified model SHA-256 and contract before session creation; QNN CPU fallback remains disabled.
- Optional user-confirmed deletion of old Stage G package after v2 is installed and verified.
- Onboarding and Settings model cards consistently grouped NPU, GPU, CPU, including Parakeet, Omi, Qwen, Whisper.
- Experimental model status, QNN encoder, preprocessing, decoder KV-cache and independent backends unchanged.
- This is an **experimental source package**. Android S26 hardware runtime, Gradle build, and H2 publishing require independent validation.
