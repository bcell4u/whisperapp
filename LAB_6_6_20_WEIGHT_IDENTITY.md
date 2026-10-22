# Lab v6.6.20 — Independent decoder-weight identity audit

## 6.6.19 evidence

Pinned 6.6.19 GitHub `INDEPENDENT_REFERENCE_REPORT.json` confirmed independent CPU eager Transformers BF16 execution of `Qwen/Qwen3-ASR-0.6B-hf` revision `6aa69c382e2b426eee1f5870d4c95859a74b6445`. The model.safetensors SHA-256 is `d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35`. First-token ID 11528 agreed across HF, S26 and all llama GGUF controls. HF prefill logits differed from BF16 GGUF Flash ON (RMSE 0.209317), Flash OFF (0.218370), Q8 Flash ON (0.364979) and S26 Q8 (0.504284). These are *not* direct correctness errors without verified matching source weights.

## What v6.6.20 adds

Read-only `scripts/audit-qwen-gguf-hf-weights.py` parses safetensors and GGUF v3 headers directly (Python standard library) and audits **explicitly mapped, BF16-compatible decoder tensors**. The fixed public inputs must pass their exact SHA-256 checks. For layers, only value/output projections, FFN gate/down/up and two RMS norm families are considered. It also attempts text input/output embeddings and final norm. It intentionally omits Q/K projection weights: GGUF conversion may permute rotary head layouts, so naïve byte comparison would give false mismatches. It reports comparable count, exact-byte count, different count, layer coverage, mapping gaps, GGUF attention/RoPE metadata, and limited mismatch examples with optional sampled BF16 RMSE. Actual weights are **not** exported.

The audit is fail-closed if too few tensor mappings are established, if the checkpoint hashes do not match, or if binary metadata is invalid. Even if all audited tensors match, that proves **only the subset audited**, not that original Qwen GGUF conversion and `-hf` model are globally identical. HF linear text positions, original three-axis interleaving and Android provisional four-axis M-RoPE remain **UNQUALIFIED**. The synthetic 156×1024 audio embedding fixture bypasses the audio encoder; the previous four-token EOS continuation cannot evaluate long clinical speech.

## CI and privacy

Push 6.6.20 to separate Lab `main`: existing automatic APK release stays enabled. A host-only workflow temporarily downloads the pinned HF safetensors and BF16 GGUF, verifies their SHA-256 and emits `WEIGHT_IDENTITY_REPORT.json`, `PROVENANCE.txt` in artifact `qwen-6.6.20-hf-gguf-weight-identity-*`. If any stage fails it exports an explicit `INCONCLUSIVE` report. No phone verification needed. Neither checkpoint, logits, raw audio, transcript, private path nor keys are bundled or uploaded; no Android inference code is changed.

## Interpretation after CI

- If a sufficient subset of audited tensors is byte-identical, continue investigating RoPE coordinate transformations and the remaining unmapped tensors.
- If audited tensors differ, isolate original-Qwen-to-`-hf` checkpoint conversion before attributing the HF-vs-GGUF logit gap to llama.cpp.
- If mappings or checksums are insufficient, resolve that provenance/control issue rather than infer weight equality.
