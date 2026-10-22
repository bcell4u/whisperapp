# WhisperRecorder Lab 6.6.17 — validation record

**Current state:** source/package/stub/logic validation completed locally. The actual Q8/BF16 Flash Attention ON-vs-OFF inference and Android APK compilation **have not run here**; they require the GitHub Actions workflow. Official Qwen numerical and M-RoPE parity remain **NOT VERIFIED / UNQUALIFIED**.

## Evidence from actual 6.6.16 GitHub artifact

- `CPU_ISA_REPORT.json`: `CPU_ISA_VARIANTS_DIFFER_BATCH_EFFECT_PERSISTS`.
- Mean Q8 microbatch RMSE: SIMD **0.1986951**, portable **0.1876288**. BF16 SIMD **0.0521775**, portable **0.0460576**.
- Mean cross-ISA RMSE: Q8 **0.2238125**, BF16 **0.0542479**.
- `PROVENANCE.txt`: pinned llama.cpp commit `d81235049384534c167caea52b85a694f6103d14`, published SHA-verified Q8/BF16, original synthetic S26 features, four threads, one CPU.
- Real Q8 SIMD unified/128 vs unified/256, step-00 vector RMSE validated as **0.20278193037**.

## Checks completed locally

- `scripts/verify-lab-source.py` — PASS for Lab isolation and versioned workflow contracts.
- All `scripts/test-qwen-*.py` regression scripts — PASS (including the new attention decision branches).
- Python syntax compilation of the new evaluator and tests — PASS.
- C++17 **syntax-only** compilation of the changed host runner and unchanged sliced runner using a deliberately minimal llama.cpp API stub — PASS; **not an actual llama.cpp/Android compilation**.
- Both GitHub Actions YAML documents parse — PASS; source workflow syntax inspected; GitHub runtime not tested locally.
- The SHA-pinned synthetic S26 fixture/logit files satisfy their expected integrity contracts.
- Source patch reconstruction and ZIP integrity: see packaging verification performed with this release.

## Intentionally not tested locally

- Full llama.cpp C++ link/build against official `v0.6.0` header (requires external library checkout and compiler environment).
- Actual Flash Attention CPU backend support or selection; the report identifies a requested setting, not a traced kernel.
- Android SDK/NDK Gradle compilation or APK signing.
- Any real lecture audio, transcription accuracy or QNN NPU execution.

## Release safety

Changes are confined to synthetic-only host reference program, host test/report scripts, GitHub verification workflow, Lab version/CI metadata, static source verifier, and documentation. No lecture audio, private transcripts, model weights, signing keys or personal paths are in the release. Existing Android decoding and production Parakeet code are unchanged.
