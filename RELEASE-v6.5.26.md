# WhisperRecorder v6.5.26 — Experimental Qwen Stage I FP16-storage Decoder Support

- New Qwen downloads use the Hugging Face `main` **schema v3** package with `decoder_weights_fp16.data` and rewritten decoder ONNX graphs that cast FP16 stored weights to FP32 before CPU operations.
- Requires verified Stage I1 and a successful **Builder v0.3.66 I2** publication to main. Existing v6.5.25 Android clients expect v2 and cannot download v3. Publish only after deploying or preparing v6.5.26.
- Strict SHA-256 validation, metadata/contract checks, manifest provenance, and exact real-speech/synthetic CPU token-parity receipts are required. A versioned, app-private Stage I model folder does not overwrite existing v1 or v2 installs.
- Allows user-confirmed deletion of the older v2 package **only after fully verifying Stage I v3**. Qwen temporary-file cleanup and independent Omi/Parakeet model paths remain unchanged.
- **Unverified on device:** v3 ONNX Runtime Android graph load, decoder memory consumption and latency, and complete speech transcription. Retain experimental label. This source release is NOT proof of a compiled APK or functioning S26 FP16 decoder.
- Source version `6.5.26`, Android `versionCode=65026`, same existing app ID and signing requirements.
