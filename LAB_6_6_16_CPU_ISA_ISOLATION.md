# Lab 6.6.16 — CPU instruction-set sensitivity (host-only)

## Why this is the next test

The uploaded `qwen-6.6.15-host-precision-batch-8.zip` completed successfully,
reported `Q8_MORE_BATCH_SENSITIVE`, and included 5 checked FP32 logit vectors
per host run. Mean within-model RMSE when changing `n_ubatch` 128 -> 256,
pooled across the original 156-frame embedding input, ordinary text control,
unified/three-stage prompt groupings and five teacher-forced positions:

| GGUF weight representation | Mean RMSE |
| --- | ---: |
| Published Q8_0, SHA256 `bca25981...` | 0.2138980447442739 |
| Published BF16, SHA256 `12b2894d...` | 0.05940874335871519 |

Q8's sensitivity was approximately 3.60x BF16's, **but BF16 was also
batch-dependent**. Across the earlier first-token experiments the winning
token remained unchanged; this does not prove clinical accuracy or official
Qwen equivalence. The numbers are **within a given model representation**;
comparing Q8 logits directly with BF16 logits does not establish a bug.

## 6.6.16 controlled build variants

Run the exact same public, SHA-verified Q8_0 and BF16 GGUFs on the same
`ubuntu-24.04` Actions runner, same pinned upstream llama.cpp, host C++ test,
four threads, fixed teacher-forced output positions, original privacy-safe
synthetic 156x1024 features, and two `n_ubatch` values (128, 256).

Compile two separate host executables from identical source:

- `simd`: `GGML_NATIVE=OFF`, plus `GGML_AVX2`, `GGML_AVX`, `GGML_SSE42`,
  `GGML_FMA`, `GGML_F16C` and `GGML_BMI2` enabled.
- `portable`: `GGML_NATIVE=OFF`, those six instruction-set switches disabled.
  x86-64's baseline architecture remains available. Other backend toggles
  are held constant. Both configurations and compiler version are recorded.

For each GGUF, compare `unified` vs `three-stage` and `n_ubatch=128` vs
`256`; repeat unified 256 in both ISA configurations. Export five full FP32
logit vectors per execution and compare within-ISA batching, cross-ISA
outputs under matched settings, exact replay, first-token S26 checkpoint,
RMSE and offset-centered RMSE.

### How to interpret

- If the portable Q8 build sharply reduces within-model microbatch
  sensitivity, investigate vectorized/reduction implementations and how
  GGML partitions graph evaluation. It is **not** evidence that portable
  CPU execution is mathematically correct or suitable for production.
- If both ISA builds remain batch-dependent, the discrepancy is broader
  than one host instruction set. Next investigate graph scheduling,
  attention/normalization reductions, and/or a mathematical reference.
- If the SIMD positive control does not reproduce 6.6.15, mark the result
  inconclusive rather than giving a false negative. The 6.6.16 runner is
  close to, but not guaranteed to be identical to, earlier CPU build flags.
- Any Q8/BF16, portable/SIMD or x86/S26 comparisons are **diagnostic**.
  Official model parity and ARM64 dot-product correctness are still
  unverified. Preserve Android's fast four-thread decoder until a
  validated correctness or medical-word accuracy improvement emerges.

## Running the next release

Push the full 6.6.16 source to the separate Lab repository's `main` branch.
The workflow `Qwen host CPU ISA isolation` runs automatically on its source
changes. Get `qwen-6.6.16-host-cpu-isa-*` from Actions > Artifacts and share it.
It includes `CPU_ISA_REPORT.json`, `PROVENANCE.txt`, two CMake flag manifests
and synthetic-only logit checkpoints. It contains **no private recording or
transcript**, no signing key, and neither large GGUF model. The models are
downloaded only into the ephemeral GitHub runner and SHA-checked.

The Android inference, QNN encoder, model downloads/backups, signed Lab
release process, overlap settings, and production Parakeet code remain
unchanged. No phone installation or real lecture run is needed. This package
has been statically checked locally; actual model inference and APK
compilation require the GitHub workflow.
