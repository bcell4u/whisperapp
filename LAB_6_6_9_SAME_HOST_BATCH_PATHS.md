# 6.6.9 — same-host standard vs extended batching (synthetic-only)

## Reason for this release

The 6.6.8 GitHub artifact reported S26-versus-host step-00 logits RMSE 0.471325597, cosine 0.996574711, max absolute delta 1.458520412, equal top-1 and 4-of-5 top-5 overlap. Host replay was exact but did not isolate input batching from ARM64 versus x86 floating-point differences. These measurements do not prove either decoder is wrong.

## What's included

- `verification/host/qwen_reference_sliced.cpp`: the unchanged split-batch `llama_decode` reference.
- `verification/host/qwen_reference_mixed.cpp`: a NEW independent test CLI using the standard upstream **extended** batch API, `llama_batch_ext_add_token`, `llama_batch_ext_add_embd`, four-axis `llama_batch_ext_set_pos`, and `llama_process`; it does **not** include or link the Android native decoder source.
- Both host binaries use exactly the same pinned upstream llama.cpp v0.6.0 source, same SHA-verified Q8 GGUF and synthetic FP32 features, 512-token context, 256-token batch/ubatch, 4 decode and 4 batch threads, CPU-only, same prompt IDs and positions. Both export five logit vectors: first-token prefill and four **synthetic** teacher-forced token positions.
- `scripts/evaluate-qwen-batch-paths.py` compares both host paths at all five positions, validates the same-host mixed replay, and compares **step 00 only** against S26. It reports raw RMSE, max deviation, cosine similarity, top1/top5, mean logit offset and mean-centered RMSE. A common logit shift cannot be confused with probability distortion.
- Positive and negative tests exercise identical paths, mixed-closer, split-closer, no-winner, non-repeatability, zero-variance offset, missing files, short files, and NaNs.
- The new GitHub **Qwen host batch-path comparison** workflow runs automatically when its host tests or workflow change on `main`; it may also be run manually. It saves `BATCH_PATH_REPORT.json`, raw synthetic comparison vectors and `PROVENANCE.txt` to the Actions artifact. The model is never uploaded or shipped in APKs.

## Interpretation

| Result status | Conclusion | Safe next engineering step |
|---|---|---|
| `BATCH_PATH_IDENTICAL` | Both host batching APIs produce byte-identical logits for five checkpoints | Examine ARM64-vs-x86 backend numerical behavior, compiler/ggml kernels, data contracts. Do not alter Android ASR. |
| `BATCH_PATH_DIFFERENCE_MIXED_CLOSER_TO_S26` | Batching implementations differ; same-host mixed is more similar to S26 | Check split batch positional planes, cross-batch KV-cache behavior and prompt boundary assembly; require an independent confirmation. |
| `BATCH_PATH_DIFFERENCE_SPLIT_CLOSER_TO_S26` | Batching implementations differ; split is more similar to S26 | Check extended-batch position semantics, token/embedding sequencing and logit indexing before any Android change. |
| `BATCH_PATH_DIFFERENCE_NO_CLEAR_S26_WINNER` | Two host APIs disagree without a clearly closer reference | Investigate both input assembly routes; no correctness conclusion. |
| `FAIL_MIXED_REPLAY` | Repeated host mixed output changed | Debug host determinism/state before any parity claim. |
| `INCONCLUSIVE` | Missing, invalid, truncated, nonfinite or incomplete data | Fix test infrastructure; do not interpret numeric results. |

The classifications use a **20% relative reduction in mean-centered RMSE** only to prioritize the follow-up investigation, not as a correctness/pass threshold. Even exact host agreement **does not** verify official Qwen M-RoPE or Python reference logits because both host branches share llama.cpp transformer mathematics.

## Instructions

Push the complete 6.6.9 source to the separate Lab GitHub repository `main`. Both the automatic Android prerelease workflow and the reference workflow can run. The expensive host reference workflow starts on `main` only when verification-host code or its workflow changes; use `workflow_dispatch` for reruns. Download the `qwen-6.6.9-host-batch-compare-*` artifact and share `BATCH_PATH_REPORT.json` and `PROVENANCE.txt`. No phone installation or transcription is needed: the Android decoding binary and audio processing are unchanged.

## Validation boundaries

Local Python numerical regression tests and project static assertions can run. Host C++ source can be syntax-checked against an API-only stub. Real Q8 inference, a full Android APK compile, and GitHub execution must not be described as completed until CI artifacts prove they were run. A mixed/split agreement establishes cross-API consistency only, **not** independent model-math correctness.
