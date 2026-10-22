# WhisperRecorder Qwen GGUF Lab 6.6.23 — validation

## Why the hotfix was needed

GitHub rejected `.github/workflows/qwen-host-reference-verification.yml` during configuration validation (line 25): `Unrecognized named-value: runner` in job-level `env: HF_HOME: ${{ runner.temp }}/qwen-hf-cache`. This rejection prevents every host stage from running. It does **not** imply the model audits produced negative results.

## Source correction

The job-level `HF_HOME` declaration has been removed. The workflow now creates `$RUNNER_TEMP/qwen-hf-cache` in an early shell step and exports `HF_HOME` using `$GITHUB_ENV`, which persists to later steps. The SHA-256 pinned HF/GGUF checkpoint downloads and four diagnostic phases remain as in 6.6.22. Version bumped to 6.6.23 / code 66023. A stdlib regression test detects invalid job/workflow env contexts while permitting legitimate step context.

## Completed local checks

- `python3 scripts/verify-lab-source.py`: PASS.
- All 19 `scripts/test-qwen-*.py`: PASS, including the new context-scope mutation controls and prior multiphase/corruption tests.
- PyYAML BaseLoader parsed both workflow files and confirmed expected triggers/jobs.
- Runner initialization shell step executed in a temporary mock GitHub environment and wrote the expected `HF_HOME` assignment to `GITHUB_ENV`.
- Source/app and native inference tree compared byte-for-byte against 6.6.22; no changes.
- Complete source archive CRC checked and incremental update verified.
- Binary Git patch applied to the unmodified v6.6.22 source and verified to reproduce every v6.6.23 source file byte-for-byte.

## Still pending

GitHub must accept and run the corrected workflow. The public checkpoint downloads, BF16/F32 storage comparison, Q/K layout audit, Transformers positional-axis reference, consolidated report, and Android APK compilation have **not** run locally. Official Qwen multi-axis M-RoPE parity and real nursing-lecture accuracy remain unverified. No private recording, transcript, model weights, signing key, or on-device diagnostic data is packaged.

Expected host artifact: `qwen-6.6.23-hf-gguf-weight-identity-*.zip`.
