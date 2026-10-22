# 6.6.22 — Four-stage host correctness investigation

## Evidence / reason for this release

The complete uploaded 6.6.21 host report confirms **123 directly comparable BF16 tensors are byte-identical**, with 57 named-but-incompatible FP32 GGUF versus BF16 HF weights and 131 GGUF tensor names previously unmapped. The report's status remains inconclusive. Earlier 6.6.19 independent HF logits favor BF16 over Q8 but do not establish official multi-axis M-RoPE parity; 6.6.18's four-token continuation was identical despite batch-sensitive logits.

## Combined phases, on a single GitHub run

**Phase 0 — source/reproducibility controls.** Verify separate Lab application/version, synthetic input SHA, hostile/malformed array tests, tensor-corruption controls, Q/K permutation toy matrices and fail-closed report aggregator. Download exact HF revision and BF16 GGUF into runner temporary storage, with full pinned hashes verified; these weights are excluded from output artifacts.

**Phase 1 — weight values.** Preserve byte-identity checks for BF16 GGUF ↔ HF. Check eligible 1D GGUF F32 layer norms (`attn_norm`, `ffn_norm`, `attn_q_norm`, `attn_k_norm`, `output_norm`) against **every single** HF BF16 value exactly cast into F32 bits. Use explicit text-decoder HF namespace so audio-encoder tensors cannot be silently picked or treated as ambiguous. Unverified tensors remain counted.

**Phase 2 — Q/K projection layouts.** For all 28 decoder layers and each Q/K projection, check exact BF16 bytes with matching [out, in] HF dimensions against GGUF [in, out] tensor metadata. If raw bytes differ, try only two precisely described head-local interleave/split permutations, with `head_count` and `head_count_kv` taken from GGUF metadata. Report every layer's match class, head geometry, missing/incompatible data; do not blindly infer a permutation.

**Phase 3 — independent HF position-axis controls.** Execute the *published pinned HF Transformers model* (CPU, BF16, eager attention) using precisely the same 156×1024 synthetic embeddings and prompt as 6.6.19. Compare its original [batch,seq] sequential position IDs with explicit 3-axis equal coordinates, explicit 4-axis equal coordinates and a deliberately asymmetric 4-axis control. Save aggregate RMSE/max difference, token ID, vector hashes, package versions, and an independent first-token comparison with the SHA-pinned original S26 logit fixture only. The asymmetric control is **not official speech-grid positions**. Unsupported axis shapes are inconclusive, not interpreted as equal.

**Phase 4 — consolidated report.** `MULTIPHASE_REPORT.json` integrates Phase 1, 2 and 3 results (and failure diagnostics) without replacing missing reports with successful results. GitHub uploads report files even if one stage fails. The separate Android APK CI remains active.

## Boundaries

- No changes to the Android QNN encoder, Q8_0 ARM64 four-thread decoder, Flash Attention defaults, audio overlap, speaker functions, signing, or phone performance.
- HF ↔ GGUF Q/K *storage* parity is separate from correct RoPE application. Three- or four-axis synthetic positions are not an authoritative multiaxis audio spatial grid, and position aliases are still not official end-to-end ASR parity.
- No lecture recording, transcript, device private path, downloaded checkpoint, signing key or raw model tensor is ever exported.
- Refrain from changing the app until genuine correctness differences are corroborated by independent reference and real-audio accuracy tests.

## Expected artifact

`qwen-6.6.22-hf-gguf-weight-identity-*.zip` includes `WEIGHT_IDENTITY_REPORT.json`, `QK_LAYOUT_REPORT.json`, `HF_POSITION_AXES_REPORT.json`, `MULTIPHASE_REPORT.json` and `PROVENANCE.txt` when all phases execute. A failed phase may omit its individual JSON, while the consolidator reports `PARTIAL_PHASE_FAILURE`. Full Android app compilation and host tensor audits must be verified on GitHub.
