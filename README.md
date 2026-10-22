# WhisperRecorder Qwen GGUF Lab 6.6.24 — HF position-contract verification

Android `versionName=6.6.24`, `versionCode=66024`. The QNN encoder, Q8_0 ARM64 four-thread decoder, Flash Attention behavior, native inference settings and original 6.5.22 production app are unchanged.

v6.6.23 established zero differences in 254 compatible BF16/FP32-cast decoder tensors plus all 56 Q/K projection tensors across 28 layers. The earlier HF 3D/4D position variants failed with `ValueError`, not a decoder accuracy finding.

The 6.6.24 host experiment retains the pinned weight and Q/K checks and adds an HF position-interface investigation: explicit versus implicit 2D positions; uniform positional offset; a synthetic suffix-gap control; attempted 3D/4D inputs with error reason and origin; and a separate direct RoPE module shape probe. The synthetic input hashes and original S26 first-token checkpoint remain pinned. **No official audio M-RoPE parity or clinical transcription improvement is claimed.**

Push the complete source to the separate Lab repository's `main` branch. Upload `qwen-6.6.24-hf-gguf-weight-identity-*.zip` from GitHub Actions. No phone test is needed.

See `LAB_6_6_24_POSITION_CONTRACT.md`.
