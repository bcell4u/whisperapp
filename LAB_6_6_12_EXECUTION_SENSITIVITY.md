# v6.6.12: host microbatch and CPU-thread sensitivity

## Evidence from v6.6.11 artifact

- `BOUNDARY_ISOLATION_REPORT.json`: status `GROUPING_INTERACTION_UNRESOLVED`.
- All four extended-API prefill groupings produce different numerical logits, with byte-identical within-mode replay. The standard/extended three-stage paths are byte-identical at all five checkpoints.
- S26 first-token RMSE: unified 0.313287; prefix+audio/suffix 0.418882; prefix/audio+suffix 0.253095; three-stage 0.471326. All agree on first highest-scoring token. No independent correctness determination.

## New controlled variable

Within the *same* GitHub runner and pinned GGUF/llama.cpp revision, compare identical `unified` and `three-stage` prompt approaches with (1) 256-token ubatch and four threads, (2) 64-token ubatch and four threads, (3) 256-token ubatch and one thread. `n_batch=256`, `n_ctx=512`, `n_gpu_layers=0`, prompt positions, feature data, model SHA, and teacher-forced tokens remain constant. Repeat the first configuration twice for each grouping, and repeat unified at ubatch=64. Compare all five output positions, top-1, top-5, centered and raw RMSE; S26 is comparable at prefill step 0 only.

### Outcome handling

- `NO_MICROBATCH_OR_THREAD_EFFECT`: the specific settings tested did not shift logits; consider prompt semantics and official PyTorch reference for follow-up.
- `MICROBATCH_SENSITIVE` and/or `THREAD_SENSITIVE`: the controlled host configuration altered logits; investigate numerical-kernel/batch-shape effects before ascribing a semantic bug.
- `FAIL_REPLAY`: same-configuration repeat disagrees, invalidating stable comparison.
- `INCONCLUSIVE_*`: missing/corrupt outputs or changed baseline. No success claim.

Even `NO_MICROBATCH_OR_THREAD_EFFECT` is **not** an official reference pass. Each test uses the same llama.cpp model internals, not an independent PyTorch implementation; M-RoPE numerical parity remains unqualified.

## Build and usage

Push full source to Lab `main`. Workflow `Qwen host execution sensitivity` automatically checks, downloads and verifies the SHA-pinned ~805 MB GGUF on the temporary runner, runs host reference, publishes `qwen-6.6.12-host-execution-sensitivity-*`. Upload this artifact to review the outcome. Android APK auto-release is preserved, but no phone run is required for this host experiment.
