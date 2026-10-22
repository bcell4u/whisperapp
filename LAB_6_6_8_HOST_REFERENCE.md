# 6.6.8 — hosted Qwen synthetic reference decoding

## Objective

The S26 6.6.7 numerical replay showed identical 151,936-float prefill logits twice (SHA-256 `9951cd0ed4143a97f4181c3fe0b87086aaf7f77b8571c63ea41d5597ef41a8e2`). The remaining useful task is comparison with a **separately constructed host decoding bridge**, rather than another self-replay or another lecture segmentation test.

## What is implemented

1. The two user-exported **synthetic-only** files are included under `verification/fixtures/`, checked by SHA-256 before execution. They contain no audio recording, transcript or private path. The feature tensor is FP32LE `[156,1024]`, and the S26 prefill logits are FP32LE `[151936]`.
2. `.github/workflows/qwen-host-reference-verification.yml` is manually launched. The public Q8 model is downloaded on the runner and checked against the S26 model SHA-256 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`.
3. `verification/host/qwen_reference_sliced.cpp` contains a **new separate bridge**, not a copy of the Android `qwen_npu_llama_probe.cpp`. It runs **three separate standard llama_decode batches** (prefix, embedded audio, suffix) rather than an extended mixed batch. The model's M-RoPE positions for audio are allocated explicitly in four **section-major** positional planes: `pos[axis * 156 + index]`. This avoids a documented buffer-size pitfall in some llama.cpp `llama_batch` paths.
4. It emits a first-token prefill vector and four further **fixed teacher-forced** checkpoint vectors, using synthetic public test IDs `[11528,4128,102064,13806]`. These are not actual generated token IDs or a transcript.
5. It repeats the reference run independently and runs an **audio-axis perturbation** control (axes 1–3 shifted by +1 for audio positions only). This tests whether modifying non-primary axes changes logits, NOT which positional map is correct.
6. `scripts/evaluate-qwen-reference.py` computes RMSE, cosine similarity, maximum absolute delta, top-1 agreement and top-5 overlap vs S26 on step 00; tests five-vector host-to-host repeatability and reports axis perturbation sensitivity. No arbitrary official parity threshold or fake PASS is generated. Errors and missing files are `INCONCLUSIVE`, and replay disagreement is `FAIL_REFERENCE_REPLAY`. Negative controls cover truncated, NaN, corrupted SHA and divergence.

## Exactly what agreement would prove

Even if step 00 agreed to machine precision, this proves **same-input cross-runtime execution agreement with the provisional linear4 map**. It does **not** independently validate llama.cpp M-RoPE mathematics because both the Lab and hosted runner use the llama.cpp core; it does not establish that four equal axes are the official Qwen-ASR positional mapping; it does not numerically verify the QNN encoder's features. Steps 01–04 are reference-only until Android exports matching teacher-forced logits.

If the S26 step 00 differs substantially, inspect the exact first-logit comparison, first-token top1, model/input hashes, context settings, mixed-vs-split batching, and M-RoPE axis semantics before changing the native inference code. If step 00 closely agrees, the next *genuinely independent* verification should use upstream Qwen PyTorch/ONNX with separately derived BF16/FP16 decoder weights or an independently validated multimodal Qwen inference implementation.

## How to run

- Push the complete 6.6.8 source to the **Lab** repository's `main` branch. Existing workflow builds/publishes the Android Lab APK if its CI checks pass.
- Open GitHub > Actions > **Qwen host decoder reference** > **Run workflow**; choose the pinned `v0.6.0` library revision first. If useful later, repeat using `master` to compare across llama.cpp revisions; its resolved full SHA is saved to the artifact.
- Download the `qwen-6.6.8-host-reference-*` Actions artifact, not the model. Share `REFERENCE_REPORT.json` and `PROVENANCE.txt` from it. The artifact also contains synthetic reference FP32LE logits.
- There is **no need to install 6.6.8 or rerun a lecture** for this desktop comparison. The 6.6.7 S26 checkpoint is already included, and the Android decoder binary is unchanged.

## Validation and limits

- Local negative/positive parser tests, SHA-256 of both attached synthetic files, source inventory, GitHub workflow structural checks and C++ syntax using an isolated mock API are performed before packaging.
- **No actual GGUF inference or full GitHub Actions reference execution can occur locally** because model weights and GitHub access are not available in this runtime. The result remains `NOT_RUN` until Actions produces a receipt.
- The reference workflow is intentionally manual: downloading ~805MB of weights and running three host decodes on every source push is wasteful. The model is held in `RUNNER_TEMP`, never shipped in APKs.
