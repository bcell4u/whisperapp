# WhisperRecorder Lab 6.6.15 — validation report

**Status: SOURCE AND NUMERICAL EVALUATOR VALIDATED LOCALLY. Real BF16/Q8 GGUF inference and Android APK compilation pending GitHub Actions.**

## Grounded input: successful 6.6.14 GitHub numerical artifact

Uploaded `qwen-6.6.14-host-input-kind-7.zip` contains `INPUT_KIND_REPORT.json`, `PROVENANCE.txt`, and five full 151,936-logit FP32 checkpoints per host configuration. Its reported status is `GENERAL_INPUT_BATCH_SENSITIVE`, under SHA-pinned Q8_0 model and pinned llama.cpp v0.6.0. The first-token results for u128 vs u256 are:

| Middle input kind | Unified RMSE | Three-stage RMSE |
| --- | ---: | ---: |
| 156 original synthetic QNN audio features | 0.202782 | 0.234128 |
| 156 all-zero audio embeddings | 0.458208 | 0.400946 |
| 156 plain token IDs 198 | 0.253845 | 0.248734 |

Each of the 3 kinds replayed byte-for-byte identically across all five steps; no text or audio input was sourced from a real lecture. The experiment establishes a repeatable *generic* batch sensitivity but no authoritative Qwen/M-RoPE correctness result.

## Changes made for 6.6.15

- New host-only **precision isolation** GitHub Actions workflow. Pin public Q8_0 model SHA-256 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971` and BF16 model SHA-256 `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`. The models are downloaded to transient CI storage only; neither is in the release source ZIP or phone APK.
- Host C++ source and llama.cpp pin preserved exactly. Run two model precisions × two middle input kinds (original features or text tokens) × two prompt groupings (unified / three-stage) × two microbatch sizes (128 / 256) = **16 primary executions**, plus **two same-configuration replays**. Save five FP32 checkpoints from each.
- New `scripts/evaluate-qwen-precision-batch.py` numerical comparator reports within-model microbatch and grouping effects, paired model representation differences, and synthetic S26 first-token/Q8 comparison. **Cross-precision logits are expected to differ** and not used as independent truth.
- New `scripts/test-qwen-precision-batch.py` exercises classification branches, replay failure, incomplete/nonfinite checkpoint rejection, and an actual archived Q8 text-vector regression check.
- Source validation and Android autorelease workflow version updated to `6.6.15`, `66015`. Normal Android QNN encoder, ARM dotprod decoder, overlap, download/backup, privacy/logging, and signing code unchanged.

## Completed local checks

1. `python3 scripts/verify-lab-source.py` passes current Android/protocol/host source checks.
2. All `scripts/test-qwen-*.py` regression tests pass, including the new precision comparator.
3. Two GitHub YAML workflow files parse and the host experiment and status guards are present.
4. End-to-end evaluator exercised on real 6.6.14 FP32 vectors with an explicitly **simulated BF16 control**. It reproduced real Q8 first-token text-control u128/u256 RMSE `0.253845` and correctly classified the deliberately equal simulated branch. This **is not a BF16 model inference run**.
5. The host C++ source was not modified; this is the same runner that compiled and executed in the uploaded 6.6.14 GitHub artifact. No local SDK/NDK or full llama.cpp/GGUF build was executed.
6. Full source archive, incremental update, binary-safe Git patch, source-vs-patch exact byte integrity, and no bundled private models/keystores checked during packaging.

## Not yet performed

- A real high-precision BF16 GGUF decoding run (requires 1.51 GB downloaded public decoder weights and GitHub Actions).
- A v6.6.15 Android APK compile and automatic prerelease (requires GitHub Actions).
- Official PyTorch/ONNX audio decoder/M-RoPE parity, or evidence of improved real-audio medical transcription or runtime speed. Do not promote a decoder change without those gates.

## Next user action

Push the complete `WhisperRecorder-Qwen-GGUF-Lab-v6.6.15-Precision-Batch-Source.zip` contents to the **separate Lab** repo `main`. The workflows automatically build the Android APK and run **Qwen host precision and batch isolation**. Upload the GitHub Actions artifact `qwen-6.6.15-host-precision-batch-*` (contains `PRECISION_BATCH_REPORT.json`, `PROVENANCE.txt` and synthetic-only logit vectors). No phone tests are necessary yet.
