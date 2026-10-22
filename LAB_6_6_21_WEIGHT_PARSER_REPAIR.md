# WhisperRecorder Qwen GGUF Lab v6.6.21 - large GGUF metadata repair

## Evidence from 6.6.20

GitHub Actions job `hf-gguf-weight-identity`, run 13 (commit `f21f89c1f3ef6699d8535adf9b36c5643f37c261`) successfully downloaded and SHA-256 verified both pinned public BF16 models:

- `ggml-org/Qwen3-ASR-0.6B-GGUF`, BF16 GGUF SHA-256 `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`
- `Qwen/Qwen3-ASR-0.6B-hf`, revision `6aa69c382e2b426eee1f5870d4c95859a74b6445`, safetensors SHA-256 `d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35`

The audit failed **before** tensor comparison in the GGUF metadata header parser with `ValueError: GGUF_EXCESSIVE_ARRAY_LENGTH` (line 63, old parser). The 6.6.20 parser capped any array at 100,000 entries, but GGUF tokenizer metadata can legitimately exceed that count. The fallback status is `INCONCLUSIVE`; it does not imply model mismatch or any numerical correctness result.

## 6.6.21 fix

- Raise the bounded GGUF array-entry limit to 2,000,000, sufficient for the model's large tokenizer metadata while retaining a hard corrupt-input limit.
- Skip large metadata arrays without creating a Python list or decoding every tokenizer token. Scalar arrays are skipped in a single bounds-checked operation; strings and nested arrays are advanced with per-entry bounds and depth checks.
- Keep small metadata arrays (16 entries or fewer) readable and preserve metadata alignment for the following tensor table.
- Keep explicit checks for oversized strings, unsupported metadata types, invalid/truncated headers, nested depth, untrusted tensor offsets and pinned model checksums.
- No changes to tensor mappings, numerical criteria or the policy excluding Q/K RoPE-permuted tensors. This is a parser repair, not a declared weight identity result.

## Validation and next run

The synthetic test creates two GGUF tokenizer metadata arrays, each with **151,936** entries (string and u32), followed by alignment metadata and 30 BF16 tensors in ten synthetic layers. It requires byte-identical comparison and rejects corrupted counts and truncated arrays. Existing independent-reference/attention/precision regression tests are rerun where locally available. Source archive and patch reconstruction are verified. No production Android path or decoder performance setting is changed.

The host workflow downloads the same exact public checkpoint hashes, audits directly mapped BF16 tensors and emits `WEIGHT_IDENTITY_REPORT.json` with explicit mapping coverage. The result artifact is `qwen-6.6.21-hf-gguf-weight-identity-*`. A successful partial identity audit does NOT establish full GGUF/HF identity, official M-RoPE parity or lecture transcription accuracy.

No checkpoint weights, private recordings/transcripts, private paths, credentials or signing material are distributed by this release.
