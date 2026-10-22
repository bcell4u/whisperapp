# WhisperRecorder Qwen GGUF Lab 6.6.20 — BF16 checkpoint weight audit

The 6.6.19 independent published HF Transformers prefill agreed with the phone and GGUF decoders on first token 11528 but differed in numerical logits (HF vs BF16 GGUF Flash ON RMSE 0.2093, OFF 0.2184, Q8 Flash ON 0.3650, S26 Q8 0.5043). The HF `-hf` model may not have identical weights to the GGUF conversion of the original Qwen checkpoint. No decoder bug can be inferred until weight identity and positional mapping are investigated.

6.6.20 audits selected directly comparable text decoder BF16 tensors against SHA-256 pinned safetensors and GGUF files: all covered layers plus embedding/norm/output when mapping is unambiguous. It checks raw BF16 bytes, counts unmatched/mismatched tensors, records GGUF attention/RoPE metadata, and does **not** claim full checkpoint equivalence (Q/K RoPE permutations excluded). Original three-axis versus HF linear text positions and Android provisional four-axis M-RoPE remain unqualified. No model checkpoints, private content, transcripts, credentials, or speech are packaged. No Android inference changes.

GitHub Actions auto-run both Lab APK release and the host-only weight audit on push. Upload `qwen-6.6.20-hf-gguf-weight-identity-*` to continue root-cause investigation. See `LAB_6_6_20_WEIGHT_IDENTITY.md`.
