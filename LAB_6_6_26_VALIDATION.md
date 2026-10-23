# 6.6.26 validation — source release

## Experiment inherited evidence

Original 6.6.25 hosted artifact `qwen-6.6.25-hf-gguf-position-parity-18.zip`: report status `MATCHED_POSITION_CROSS_BACKEND_COMPARISON_COMPLETE`, zero missing branches, 4 HF BF16 vectors and 8 GGUF BF16 vectors, all SHA-256 verified locally and all first tokens 11528. Follow-up precision test is warranted; no Android decoder change justified.

## Local validation

- Version source and GitHub prerelease metadata: 6.6.26 / 66026.
- FP32 input-quantization isolation uses a BF16 embedding snapshot and the same already-loaded HF source weights.
- Host-free evaluator test covers complete data, cross-branch position-response alignment, missing vectors, truncated vectors, nonfinite vectors, and output checksum forgery.
- Workflow config separates steps with best-effort artifact retention and explicit final failure on missing branches.
- Existing Qwen regression suite: **22 of 22 scripts passed locally**.
- `scripts/verify-lab-source.py`: **passed** on complete staged source.
- GitHub Actions workflow YAML: **both workflows parsed**; all embedded `run:` shell programs passed `bash -n`.
- Version and path checks: **passed**, including no job-level `runner.temp` expression and new host artifact naming.
- Android source and native inference: **no file changes** relative to the complete v6.6.25 archive.
- Source ZIP, incremental ZIP, and binary Git patch reconstruction: **verified before delivery**.

## Not locally executed

The real pinned HF model and GGUF runs, the Android Gradle/NDK build and phone transcription performance must be determined by GitHub/device, not inferred from local stubs or synthetic unit tests. No private audio or transcript is required.
