# WhisperRecorder Lab 6.6.17 — decoder attention-path isolation (host-only)

## Grounded input: GitHub Actions 6.6.16

The user-supplied `qwen-6.6.16-host-cpu-isa-9.zip` contained `CPU_ISA_REPORT.json`, two CMake manifests, model/S26 fixture provenance, and five checkpoints per host run. It reported `CPU_ISA_VARIANTS_DIFFER_BATCH_EFFECT_PERSISTS` with **identical per-configuration replays**. The mean RMSE (128-vs-256 `n_ubatch`) across unified/three-stage modes and five teacher-forced checkpoints was:

| Model | AVX2/FMA host | Portable x86-64 host |
| --- | ---: | ---: |
| Q8_0 | 0.1986951 | 0.1876288 |
| BF16 | 0.0521775 | 0.0460576 |

Mean matched-configuration cross-ISA RMSE was 0.2238125 for Q8 and 0.0542479 for BF16. Neither instruction-set implementation removes the batch effect. This does **not** establish a quantization defect or that either runtime agrees with official Qwen3-ASR. The Android-S26 prefill logits, although similar in top-1 token, remain numerically different.

## 6.6.17 test

Use the same pinned llama.cpp `v0.6.0` revision, Q8_0 and BF16 model SHA256s, synthetic 156x1024 FP32 audio embeddings, 171-position Qwen prompt, four fixed teacher-forced tokens, four CPU threads, x86-64 AVX2/FMA/F16C/BMI2 build, and identical test machine. Change only:

- `llama_context_params.flash_attn_type` explicitly to `LLAMA_FLASH_ATTN_TYPE_DISABLED` versus `LLAMA_FLASH_ATTN_TYPE_ENABLED`.
- Prompt grouping: unified vs prefix/audio/suffix in three stages.
- `n_ubatch`: 128 versus 256.

For each model and requested attention setting, export five complete 151,936-value FP32 logits vectors for each grouping/microbatch combination and repeat unified/256 once to validate exact replay. The host run records logs for each configuration, model/fixture hashes and compiler/build provenance. All results are **synthetic-only**, with no lecture, transcript or private path in the package.

The report computes within-model `n_ubatch` RMSE and attention-setting RMSE. Decision cases include attention-request effects that reduce Q8 microbatch sensitivity, differences that do not remove it, failed replays, unsupported/incomplete runs, and no observable attention change.

**Important:** The flag records the *requested* attention mode. The test does not instrument the internal graph to independently certify that distinct attention kernels ran. If outputs are identical, label the result `INCONCLUSIVE_ATTENTION_BRANCH_NOT_OBSERVED`, not proof that attention is irrelevant. If the host backend rejects Flash Attention enabled, mark the GitHub result `INCONCLUSIVE` and keep all previous evidence.

The test still uses llama.cpp mathematics for all modes. It **cannot** prove independent official-model numerical parity, Qwen M-RoPE correctness, Android ARM64 Q8 dot-product kernel correctness, clinical transcription accuracy or performance benefit.

## No phone change

The on-device QNN encoder, four-thread ARM64 decoder, segment/overlap logic, model backups, verification UI, signing configuration and production Parakeet pathway remain untouched. Version metadata becomes `6.6.17` / `66017` for the separately installed Lab only.

## Run and interpret

Push the complete Lab source to `main` to trigger `Qwen host attention-path isolation` and the existing Android Lab build. Upload `qwen-6.6.17-host-attention-path-*` from Actions Artifacts. The host artifact contains `ATTENTION_PATH_REPORT.json`, `PROVENANCE.txt`, logs and synthetic FP32 logits vectors. No model weights or phone recording are included.

**After the run:** If changing attention paths materially changes the batch sensitivity, investigate attention graph/scheduling and kernel implementation under the pinned revision. If differences persist essentially unchanged, move to per-layer intermediate-activation tracing or authoritative PyTorch numerical comparison rather than another arbitrary configuration sweep. Do not change the phone decoder until a verified correctness or actual speech-accuracy improvement is demonstrated.
