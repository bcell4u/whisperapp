# 6.6.24 — Independent HF positional-interface contract

## Motivation

The 6.6.23 artifact reports 254 BF16/FP32-conversion-equal decoder tensors and 56 additional Q/K projection matrices byte-identical over all 28 layers. No compared tensor differed. The HF position stage accepted a 2D linear baseline with first-token ID 11528, but equal 3D, equal 4D and asymmetric 4D alternatives each failed with `ValueError`. Rejection of an API input shape says nothing by itself about a decoder bug.

## Phases

1. Reproduce pinned BF16/FP32 weight audit and Q/K storage-layout identity with SHA-verified HF and GGUF BF16 weights.
2. Run explicit and implicit 2D HF positional baseline on the same SHA-pinned 156×1024 synthetic features. Compare position +17 and a +11 suffix-only offset, retaining logit metrics and token IDs but not text or model weights.
3. Attempt 3D/4D positions at the HF ASR model interface and record sanitized exception types, error messages, failure origins and actual rotary-input shapes.
4. Probe the HF language-model rotary module directly for 2D/3D/4D shape handling, with optional cos/sin differences when shapes permit comparison.
5. Consolidate reports with fail-closed unavailable-phase labels; never treat an unsupported API shape as an official M-RoPE correctness failure.

## Boundaries

- Model/source: `Qwen/Qwen3-ASR-0.6B-hf` at `6aa69c382e2b426eee1f5870d4c95859a74b6445`, pinned `model.safetensors` SHA256 `d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35`.
- BF16 GGUF pinned SHA256 `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`.
- The GGUF RoPE metadata reports dimension sections `[24,20,20,0]`; this is metadata, **not proof** that HF receives or expects four independent position axes.
- Android model, Flash Attention setting, QNN, privacy, 4-thread dotprod performance and source code unchanged.
- No claims about official audio-grid parity, correct phone RoPE, medical term accuracy or comparative run time without direct testing.
