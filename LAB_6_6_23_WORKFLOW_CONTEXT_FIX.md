# WhisperRecorder Lab 6.6.23 — GitHub Actions context hotfix

**Failure before any job ran:** GitHub rejected `.github/workflows/qwen-host-reference-verification.yml` at line 25 with `Unrecognized named-value: runner` from `HF_HOME: ${{ runner.temp }}/qwen-hf-cache` inside `jobs.hf-gguf-multiphase.env`. GitHub's workflow validator cannot evaluate `runner` in the job-level `env` scope. This was a workflow configuration problem, **not** an HF/GGUF weight-comparison failure.

**Repair:** Remove the job-level `runner.temp` expression. After checkout, a dedicated shell step creates `$RUNNER_TEMP/qwen-hf-cache` and writes `HF_HOME=$RUNNER_TEMP/qwen-hf-cache` to `$GITHUB_ENV`. All four host phases (BF16/F32 storage; Q/K layout; independent HF position axes; consolidated report) are retained, and the original model hash/revision pins remain unchanged.

**Prevention:** `scripts/test-qwen-workflow-context.py` checks forbidden scopes, confirms temporary-cache setup, and includes a negative control that reproduces the original invalid expression. The test runs at the beginning of the host job and `verify-lab-source.py` enforces the source guard before packaging. Only synthetic vectors appear in diagnostic reports.

**No runtime change:** Android QNN encoder, ARM64 dot-product GGUF decoder, threading, model-loading behavior, signing, private data handling, and baseline transcription speed are untouched.

**Pending:** Real hosted multi-phase run and APK compilation on GitHub Actions. Expected diagnostic artifact: `qwen-6.6.23-hf-gguf-weight-identity-*.zip`.
