# WhisperRecorder Lab 6.6.16 — validation record

**Status: source, GitHub workflow, evaluator logic and C++ API-stub syntax validated locally.**
The actual dual-ISA Q8_0/BF16 GGUF inference and Android APK compile remain
**pending GitHub Actions**. No numerical claim about portable-vs-SIMD decoding
has been made, because that experiment has not yet executed.

## Grounded 6.6.15 GitHub result

The uploaded `qwen-6.6.15-host-precision-batch-8.zip` contains a complete
`PRECISION_BATCH_REPORT.json` and both precision models' synthetic-only full
logits across five forced checkpoints. It reported `Q8_MORE_BATCH_SENSITIVE`,
model SHA256 Q8 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`,
BF16 `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`,
and mean microbatch RMSE 0.213898 Q8 vs 0.059409 BF16 (ratio ~3.6).
Both models were batch-sensitive; neither is an official oracle.

## Changes in 6.6.16

- GitHub host workflow compares an AVX2/FMA/F16C/BMI2-enabled x86-64 CPU build
  to one with those extra ISA features and SSE4.2 disabled (portable x86-64
  baseline), from the **same pinned llama.cpp** and same Q8/BF16 files.
- Host CMake now accepts the requested `GGML_NATIVE` configuration while
  defaulting to the previously selected non-native build.
- `scripts/evaluate-qwen-cpu-isa.py`: explicit 16-case, four-replay, five-step
  report; paired within-model microbatch and cross-ISA comparisons, first-token
  S26/Q8 comparisons, RMSE/centered RMSE/top-token, and failure guards.
- `scripts/test-qwen-cpu-isa.py`: branch decisions, truncated/replay handling,
  fixture SHA validation, and successful validation of the uploaded 6.6.15
  numerical positive control.
- Android versionName 6.6.16, versionCode 66016, CI autorelease name/badging.
  **No Android native decoder, QNN, transcription worker, model download or
  signing implementation changes.**

## Local validation completed

- `python3 scripts/verify-lab-source.py`: PASS.
- Every `scripts/test-qwen-*.py` regression: PASS.
- `python3 -m py_compile` on added Python evaluator/tests: PASS.
- Both GitHub Actions YAML documents parse: PASS.
- Host reference C++ syntax check with the preexisting API stub: PASS.
- Historical real Q8 vectors reproduce RMSE 0.20278193 for unified u128 vs
  u256; the uploaded Q8/BF16 report's relative sensitivity passes direct
  regression assertion.
- A local **full-dataset** mockup for new CPU ISA evaluator exceeded the
  environment's execution limit, and was not counted as a passing test.
- Full Android APK compilation: NOT RUN locally.
- Actual SIMD versus portable GGUF inference: NOT RUN locally.

## Outcome limits

This is an x86-64 CPU control, not proof of a quantized-kernel bug, not an
independent official Qwen PyTorch/M-RoPE comparison, not a test of Android
ARM64 dot-product kernels, and not a measure of medical speech recognition.
The current fast phone decoder and production baseline are untouched.

## User action

Push the full source ZIP contents to Lab GitHub `main` (not production).
Then upload `qwen-6.6.16-host-cpu-isa-*` from Actions > Artifacts; it contains
`CPU_ISA_REPORT.json`, `PROVENANCE.txt`, ISA CMake manifests and synthetic
logit vectors. No phone recording, transcript, or new phone test is required.
