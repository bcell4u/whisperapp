# Lab 6.6.14 — synthetic input-kind control

## Evidence from actual 6.6.13 GitHub artifact

The original features and SHA-pinned Q8 model were tested with 5 teacher-forced
logit positions and 4 CPU threads. For the first-token checkpoint:

| Host batching | u64 vs u128 | u128 vs u160 | u160 vs u192 | u192 vs u256 |
| --- | --- | --- | --- | --- |
| Unified 171-position prefill | identical | RMSE 0.221939 | RMSE 0.299573 | identical |
| Three-stage prefix/audio/suffix | identical | RMSE 0.234128 | identical | identical |

The prefill grouping RMSE at u256 is 0.285540, and at u128 0.221684.
All reported 6.6.13 same-config replays were byte-identical; this is a
reproducible batching sensitivity, not evidence that either output is correct.

## 6.6.14 controlled experiment

Keep fixed GGUF SHA-256, llama.cpp tag, native host CPU, 4 threads, n_ctx=512,
n_batch=256, M-RoPE coordinates `{p,p,p,p}`, identical Qwen prompt/prefix/suffix,
and 4 teacher-forced tokens. Rather than sweep further batch sizes, run only
n_ubatch={128,256} in unified and three-stage modes, with three input kinds:

1. `orig`: exact S26 synthetic features, 156x1024 FP32. This is the **positive
   control**; earlier microbatch effect must reproduce.
2. `zero`: exact same 156 FP32 embedding slots, but all values forced to zero.
   This controls whether the mixed-embedding API / position handling differs
   independently of the actual feature content.
3. `text`: replace 156 middle embedding slots with **156 ordinary text token IDs
   198**, retaining the 171-position length and all prefix/suffix tokens. This
   controls whether physical prefill chunking affects ordinary token-only
   execution too. It is a deliberately nonsensical ASR prompt, not a transcript.

For all three input kinds, compare same-kind u128/u256 and unified/three-stage
full FP32 logits at five positions. Replay one u256 unified execution for every
kind. For original features only, also compare step 0 with the saved S26
first-token checkpoint. No cross-kind numeric proximity claims: different
input kinds are supposed to produce different logits.

### Decision states

- `GENERAL_INPUT_BATCH_SENSITIVE`: original, zero and ordinary text are all
  sensitive to physical batch size, favoring a generic decoder scheduling or
  attention arithmetic issue rather than an audio-only special case.
- `EMBEDDING_PATH_SENSITIVE`: original and zero embeddings sensitive while
  text control invariant; investigate mixed embeddings and their positions.
- `FEATURE_VALUE_SENSITIVE`: only original nonzero features sensitive;
  investigate value-dependent kernels or their numerical stability.
- `MIXED_INPUT_SENSITIVITY`: original and text sensitive while zeros are not.
- `FAIL_REPLAY`: same input/configuration mismatch; fail the workflow.
- `INCONCLUSIVE_*`: original effect absent, missing/corrupt files, model failure,
  or partial experiment. Never claim a parity pass from missing evidence.

These are **diagnostic classification labels only**. The host runner still
uses llama.cpp math; this is not official Qwen/PyTorch M-RoPE or logit parity.

## User workflow

Push the complete Lab source to `main`. The standard Android prerelease and
`Qwen host input modality isolation` GitHub workflows run independently. The
host artifact `qwen-6.6.14-host-input-kind-*` contains
`INPUT_KIND_REPORT.json`, `PROVENANCE.txt`, and synthetic-only 5-step FP32
checkpoint directories. Do **not** install the APK or transcribe a lecture for
this host-only experiment. Normal Android decoding, model backups, signing,
4-thread dot-product optimization, and optional 1-second overlap are unchanged.
The model is downloaded into the ephemeral CI runner, never shipped in ZIP/APK.
