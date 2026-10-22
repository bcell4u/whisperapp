# WhisperRecorder Qwen GGUF Lab 6.6.20 — weight identity

Android versionCode 66020; separate Lab app ID and optimized QNN/ARM64 Q8_0 decoder retained exactly. The host-only GitHub workflow compares directly mapped BF16 decoder tensors from `Qwen/Qwen3-ASR-0.6B-hf` and `ggml-org/Qwen3-ASR-0.6B-GGUF`. A direct tensor match is only partial identity; Q/K permutation, multi-axis M-RoPE and clinical accuracy remain unqualified.

Push to Lab `main`; auto-release builds APK, host output `qwen-6.6.20-hf-gguf-weight-identity-*`.
