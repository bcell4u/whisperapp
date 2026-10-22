# WhisperRecorder Lab 6.6.10 — same-host prompt grouping isolation

## Why this release

The completed GitHub 6.6.9 reference artifact found standard split-batch versus extended unified-batch RMSE **0.2855397426**, cosine **0.9985463503**, and equal first-token argmax. S26-to-host prefill RMSE was **0.4713255973** for the standard path and **0.3132871340** for the extended path. The extended path was closer by raw RMSE and mean-centered RMSE, but this does not establish correctness. All five host checkpoint pairs differed numerically; the mixed replay matched exactly. The report's own 20%-improvement classifier returned NO_CLEAR_S26_WINNER because mean-centered RMSE improves by only ~12.3%.

## Exactly what changed

- Add a **three-stage** execution mode to `verification/host/qwen_reference_mixed.cpp`. It calls **the same public `llama_process` extended API** three times (9 prompt-prefix tokens, 156 synthetic FP32 feature embeddings, 6 prompt-suffix tokens) with `llama_batch_ext_clear` between calls. Crucially, the KV cache and incremental text/audio position counter are **not reset** between calls. The first-token logit flag is set only on the suffix's final token; the four subsequent teacher-forced positions are unchanged.
- Keep the **unified** extended mode: one batch of 171 items. Keep the prior **standard split** mode: `llama_decode` with 9/156/6 chunks.
- A single pinned host runner builds and executes all three paths using the same verified GGUF, 156x1024 fixture, llama.cpp revision, thread count, batch settings and CPU. All three export 5 checkpoint vectors. Both extended paths rerun for deterministic replay.
- The new `scripts/evaluate-qwen-chunk-isolation.py` writes `CHUNK_ISOLATION_REPORT.json` and distinguishes grouped-batch causes, API causes, mixed discrepancies, replay failures and incomplete results. Comparisons include raw RMSE, max absolute error, cosine, top-1/top-5, average logit offset and mean-centered RMSE. The first checkpoint is also compared to the saved S26 checkpoint. Negative regression cases test replay, file truncation, nonfinite values and missing files.
- The `Qwen host chunking isolation` GitHub workflow **automatically starts on main** when reference code or the workflow changes. Existing Android auto-release is preserved with versionName **6.6.10-dev** and versionCode **66010**.

## How to interpret the artifact

| Status | Evidence | Next release direction |
|---|---|---|
| `CHUNK_GROUPING_EXPLAINS_DIFFERENCE` | Three-stage extended equals standard split exactly across all 5 checkpoints; unified differs | Audit prefill batch shape and kernel behavior. No phone-side modification until independent correctness work. |
| `API_PATH_EXPLAINS_DIFFERENCE` | Three-stage extended equals unified extended exactly; standard split differs | Audit standard versus extended batch position/logit and token/embedding semantics. |
| `CHUNK_GROUPING_SIMILAR_TO_SPLIT` | Extended three-stage is substantially closer to standard split but not identical (centered error ratio) | Examine batch size and quantized matmul kernels; beware innocuous numerical rounding. |
| `CHUNK_GROUPING_SIMILAR_TO_UNIFIED` | Extended three-stage is closer to unified mode | Examine API path and positional map. |
| `THREE_WAY_DIFFERENCE_UNRESOLVED` | All paths disagree without dominant grouping | Test kernel shape and positional planes separately. |
| `ALL_THREE_IDENTICAL` | No discrepancy reproduced on that runner | Investigate ARM64 backend and compile options. |
| `FAIL_REPLAY` / `INCONCLUSIVE` | Nonrepeatability or missing/invalid data | Fix diagnostics, **not** ASR. |

The classification is only a **triage signal**; shared llama.cpp transformer math means even exact agreement between all host modes **does not prove official Qwen M-RoPE or logit correctness**. Only S26's **step 00** is known; steps 01–04 are host-to-host teacher-forced comparisons, not S26 parity.

## Test procedure

Push complete source to the separate Lab GitHub repository `main`. The native Android inference code and normal transcript/overlap settings are **byte-for-byte unchanged**. Download the GitHub Actions artifact `qwen-6.6.10-host-chunk-isolation-*`, and upload the ZIP or `CHUNK_ISOLATION_REPORT.json` plus `PROVENANCE.txt`. There is **no new phone test, recording upload, or model upload**. The ~805 MB Q8 GGUF is downloaded only to an ephemeral GitHub runner after SHA-256 verification.

## Validation limits

The offline tests cover source/static gates, comparator decisions using known 6.6.9 real vectors (two *simulated possible* new-path outcomes), negative cases, and fixture integrity. **They do not execute the newly added third path on a GGUF.** That requires GitHub Actions to compile the pinned C++ and run the full reference job. Android APK compile and future numerical results cannot be claimed until GitHub supplies artifacts.
