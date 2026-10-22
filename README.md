# WhisperRecorder Qwen GGUF Lab 6.6.19 — independent Transformers prefill check

The 6.6.18 synthetic greedy fixture stops at the EOS after **four tokens**.  All 12 host configurations chose the same four token IDs despite their logit differences. That is limited evidence; it is not a long-utterance or medical-word accuracy comparison.

6.6.19 runs the **published Qwen3-ASR 0.6B HF Transformers** checkpoint independently from ggml/llama.cpp, directly injecting the pinned FP32 synthetic 156×1024 embeddings between the exact nine-token prefix and six-token suffix. It compares the independent first-token logits with pinned S26, host Q8 Flash ON and GGUF BF16 Flash OFF/ON. The HF execution uses plain eager attention. Exact PyTorch/Transformers package versions and resolved HF model revision are logged. All model weights stay in GitHub runner ephemeral storage.

This tests same-embedding decoder numerical agreement; it **does not** validate the QNN encoder, text/chat prompt authenticity, official Qwen ASR audio preprocessing, real transcription accuracy, or the Android's provisional four-axis M-RoPE. The original Qwen legacy text positions are 3-axis; this independent BF16 HF converter uses linear text positions, appropriate only to the existing identical-axis synthetic fixture, NOT full M-RoPE parity. A weight conversion or checkpoint mismatch is reported inconclusive rather than called an Android error.

Push source to the separate Lab `main` branch; automatic GitHub output is `qwen-6.6.19-hf-independent-prefill-*`. Keep the existing optimized Android QNN and ARM64 dotprod decoder unchanged. No phone experiment is requested.
