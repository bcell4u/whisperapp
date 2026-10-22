# WhisperRecorder Lab 6.6.14 — validation receipt

## Result: SOURCE VALIDATED; ACTUAL INFERENCE AND APK BUILD NOT EXECUTED HERE

Input evidence: uploaded `qwen-6.6.13-host-microbatch-threshold-6.zip` GitHub
artifact. Read `MICROBATCH_THRESHOLD_REPORT.json` and SHA-pinned FP32 vectors.
The preceding run reported `UNIFIED_AND_AUDIO_THRESHOLD_SENSITIVE` and preserved
five synthetic-only teacher-forced checkpoints. At prefill step 0:

- Unified u64 = u128, differs at u160 (u128→u160 RMSE 0.221939) and at
  u192 (u160→u192 RMSE 0.299573); u192 = u256.
- Three-stage u64 = u128, differs at u160 (u128→u160 RMSE 0.234128);
  u160 = u192 = u256.
- Same-host replays were byte-identical; top-one token agreement across paths
  is not a correctness certificate.

## Implementation

- Host mixed-batch C++ runner now accepts an **optional synthetic input kind**:
  `original` (same code path as 6.6.13), `zero` (all-zero FP32 embeddings), or
  `text-filler` (156 ordinary token IDs 198). All preserve 171 positions and
  use `{p,p,p,p}` coordinates, the original prefix/suffix, pinned GGUF,
  and the same teacher-forced tokens.
- CI tests 3 input kinds × 2 prefill groupings × 2 microbatch sizes (128/256),
  plus 3 same-configuration replay controls. Five checkpoints per case.
- `INPUT_KIND_REPORT.json` checks same-kind microbatch and grouping differences,
  per-kind replay, input integrity and model/CPU provenance. Only the
  original-input step-0 logits are compared with the S26 checkpoint.
- Fail-closed status labels: `GENERAL_INPUT_BATCH_SENSITIVE`,
  `EMBEDDING_PATH_SENSITIVE`, `FEATURE_VALUE_SENSITIVE`,
  `MIXED_INPUT_SENSITIVITY`, `FAIL_REPLAY`, and `INCONCLUSIVE_*`.
- Android native decoding source, Kotlin audio pipeline, model backup,
  signing and dot-product tuning unchanged; Android versionName becomes
  `6.6.14-dev`, versionCode `66014`.

## Completed offline checks

1. `scripts/verify-lab-source.py` passed; all source/CI protocol gates.
2. Prior numerical comparators and tests 6.6.8–6.6.13 passed.
3. 6.6.14 input-kind positive, negative, corrupt-checkpoint, replay and
   incomplete-output tests passed.
4. 6.6.14 evaluator exercised end-to-end against **real prior-run FP32
   vectors** with deliberately reused vectors standing in for the new controls;
   this validates the evaluator, **not** a model execution for zero/text.
5. Host C++ `-fsyntax-only` passed against a test API stub; this is **not**
   upstream llama.cpp linking or a successful Android build.
6. Both GitHub Actions YAML files parse, and the model hash remains pinned.
7. Full source ZIP and patch reconstruction tests verified separately during
   packaging. No model weights, lecture audio, private transcripts, or signing
   keystores are included.

## Not completed

- No full Android SDK/NDK APK compile or GitHub release publication in this
  execution environment.
- No actual 6.6.14 805 MB-model host inference. Requires GitHub Actions.
- No PyTorch/official Qwen M-RoPE or logits parity check; it remains unqualified.
- No new on-device accuracy or speed claim. Existing Android inference retained.

## What to upload next

After pushing the complete source to `main`, download the artifact
`qwen-6.6.14-host-input-kind-*` from the **Qwen host input modality isolation**
GitHub Actions run. Upload the ZIP (including `INPUT_KIND_REPORT.json` and
`PROVENANCE.txt`). No phone test is required.
