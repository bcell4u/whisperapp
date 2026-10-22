# WhisperRecorder Qwen GGUF Lab 6.6.25 — matched-position independent numerical comparison

Android `versionName=6.6.25`, `versionCode=66025`. The QNN encoder, existing Q8_0 ARM64 four-thread decoder, Flash Attention behavior, native inference settings, and original 6.5.22 production app remain unchanged.

The v6.6.24 GitHub results confirmed HF implicit and explicit linear 2D positions were exactly equal and showed 254 BF16/FP32-cast decoder tensors plus 56 Q/K tensors matching the published GGUF (no confirmed mismatches). Unsupported 3D/4D HF decoder inputs do not establish an ASR M-RoPE defect.

**New experiment:** in the pinned host llama.cpp and independent published HF BF16 model, compare four *matched* prefill position maps (linear, uniform +1, uniform +17, suffix gap +11) with exactly the same synthetic audio embeddings and prompt. The GGUF host receives the same scalar position on all four axes; HF receives standard 2D text positions. GGUF Flash Attention ON/OFF are separate controls. A self-describing JSON report records paired RMSE, cosine, max difference, centered RMSE and top-1 token agreement. Incomplete branches are marked inconclusive.

Push the complete ZIP source to the separate Lab repository `main` branch. Upload the resulting GitHub Actions artifact **`qwen-6.6.25-hf-gguf-position-parity-*.zip`**. No phone test is needed.

This synthetic diagnostic **does not establish** official Qwen audio positional grids, end-to-end audio encoder correctness or medical transcription accuracy. See `LAB_6_6_25_MATCHED_POSITION_PARITY.md` and `LAB_6_6_25_VALIDATION.md`.
