# WhisperRecorder Qwen GGUF Lab 6.6.27 — M-RoPE axis attribution

Experimental Android versionName `6.6.27`, versionCode `66027`. The working QNN encoder, four-thread ARM64 Q8_0 decoder, Flash Attention behavior, transcription speed settings and separate production 6.5.22 app remain **unchanged**.

**6.6.26 evidence:** All 20 saved synthetic logit vectors were checked by SHA-256 and argmax. Using FP32 arithmetic with the same stored HF BF16 checkpoint weights greatly reduced the HF↔GGUF numerical discrepancy; BF16 rounding of the synthetic audio features alone made only a small difference. HF FP32 was almost invariant to uniform positional shifts, whereas the GGUF host remained more sensitive. This does not establish a correctness or transcription defect.

**6.6.27 experiment:** On one pinned llama.cpp GGUF BF16 host, compare +17 global shifts and suffix +11 gaps applied to all four position axes versus each individual axis. Retain a normal and +1 baseline and both Flash Attention graph paths. Generate 24 first-step logit vectors and report per-axis sensitivity, interaction residuals, and first-token agreement. No HF model download or Android execution is needed for this host experiment. See `LAB_6_6_27_AXIS_ATTRIBUTION.md`.

Push the complete source to the separate Lab repository `main` branch. Upload the GitHub artifact **`qwen-6.6.27-host-rope-axis-attribution-*.zip`** for analysis. No phone test is needed.

Source regression and packaging checks are not substitutes for the actual pinned GGUF run and Android APK compilation in GitHub Actions. Official Qwen ASR audio M-RoPE grid parity, on-device encoder parity, and medical-lecture transcription improvements remain **unverified**.
