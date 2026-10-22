# Lab 6.6.15 — isolate model-weight precision effects on host batching

## What the successful 6.6.14 GitHub result actually established

The GitHub artifact `qwen-6.6.14-host-input-kind-7.zip` reported
`GENERAL_INPUT_BATCH_SENSITIVE` using a pinned Q8_0 GGUF and llama.cpp revision.
At the first teacher-forced checkpoint, the RMSE when changing `n_ubatch`
from 128 to 256 was:

| Synthetic middle span | Unified | Three-stage |
| --- | ---: | ---: |
| 156 original S26 audio embeddings | 0.202782 | 0.234128 |
| 156 zero-valued embeddings | 0.458208 | 0.400946 |
| 156 ordinary text tokens (token 198) | 0.253845 | 0.248734 |

Replays were bit-for-bit identical, so those differences are reproducible on
that host. Input-kind replacement changes model input; **never compare text
logits directly to audio logits as a measure of accuracy**. Results do not
establish which batch configuration is correct, nor official Qwen3-ASR parity.

## Experiment

A new **host-only** GitHub test uses identical synthetic features, prompt tokens,
M-RoPE position map, teacher-forced tokens, llama.cpp revision, host CPU and
4 threads, but compares two published versions of decoder weights:

- `Qwen3-ASR-0.6B-Q8_0.gguf`: 805 MB; SHA-256
  `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`.
- `Qwen3-ASR-0.6B-bf16.gguf`: 1.51 GB; SHA-256
  `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`.

Within each weight representation run two input kinds (original synthetic
embeddings / text filler), two batch groupings (unified / three-stage),
and `n_ubatch` 128 / 256. Replay the original unified u256 test for both
precisions. The result includes five FP32 logit checkpoints per run, 16 primary
runs and 2 replays. For each model independently, compare batch size and grouping
logit RMSE/centered RMSE/argmax. Cross-precision comparisons are diagnostic and
will **not** be used to declare a numerical mismatch bug by themselves.

### Interpretation guards

- If BF16 also changes materially, this disfavors a strictly Q8-only bug;
  investigate generic decoder attention/prefill execution, including compiler
  kernels and numerical reductions.
- If BF16 is nearly invariant but Q8 varies, prioritize inspecting quantized
  matmul and batch scheduling, **not** an immediate model swap to BF16 on-device.
- If the Q8 positive control fails to reproduce or a replay differs, report
  inconclusive/failure explicitly.
- Neither model implements *independent* mathematics; this is still llama.cpp.
  Official PyTorch audio-to-token logits, M-RoPE parity and actual clinical
  accuracy are unqualified. A change to normal Android decoding requires
  stronger correctness evidence and controlled phone timing/accuracy tests.

## Build and privacy

Push complete 6.6.15 source to the separate Lab repository's `main` branch.
The workflow `Qwen host precision and batch isolation` builds the same host
runner and downloads both published SHA-pinned decoder GGUFs to **temporary
GitHub runner storage**; neither weights nor lecture data are uploaded to the
repository or APK. Upload the `qwen-6.6.15-host-precision-batch-*` workflow
artifact after it runs. No phone installation, transcription or private audio
upload is required. Current Lab Android normal NPU, decoder, overlap, signing,
and model backup code are unchanged.
