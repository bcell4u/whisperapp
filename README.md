# WhisperRecorder Qwen GGUF Lab 6.6.26 — same-weight execution-precision controls

Experimental Android versionName `6.6.26`, versionCode `66026`. The working QNN encoder, four-thread ARM64 Q8_0 decoder, Flash Attention behavior, transcription speed settings and separate production 6.5.22 app remain **unchanged**.

**6.6.25 evidence:** twelve hashed synthetic logit vectors verified. All conditions selected token ID 11528; HF BF16 responded more strongly than GGUF BF16 to a uniform `+17` position offset. This observation does not establish a positional implementation defect or clinical accuracy problem.

**6.6.26 experiment:** use the same pinned public HF BF16 checkpoint in BF16 arithmetic, in FP32 arithmetic with the *same BF16-rounded inputs*, and in FP32 arithmetic using original FP32 synthetic audio embeddings. Compare four matched positional maps to GGUF BF16 Flash-ON/OFF controls. Report position-response residuals instead of treating different raw logits as a bug. See `LAB_6_6_26_EXECUTION_PRECISION.md`.

Push the complete source to the separate Lab repository `main` branch. Upload the GitHub artifact **`qwen-6.6.26-hf-execution-precision-*.zip`** for analysis. No phone test is needed.

The source regression suite and packaging verification are **not** a substitute for the scheduled GitHub model experiment and APK compilation. Official Qwen audio M-RoPE parity, full end-to-end encoder accuracy and medical transcription improvements remain unverified.
