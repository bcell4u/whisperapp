# WhisperRecorder Qwen GGUF Lab v6.6.12 — source validation receipt

## What the uploaded 6.6.11 result established

`qwen-6.6.11-host-boundary-isolation-4.zip` contains `BOUNDARY_ISOLATION_REPORT.json` (status `GROUPING_INTERACTION_UNRESOLVED`) and five complete numerical checkpoints for each mode. All four grouping modes replayed byte-identically and the standard/extended three-stage modes were identical. The S26 first-token RMSE was 0.253095 for `[prefix][audio+suffix]`, 0.313287 for unified, 0.418882 for `[prefix+audio][suffix]`, and 0.471326 for three-stage. The experiments are not an official Qwen correctness reference.

## Build scope

- Release version **6.6.12**, Android version code **66012**.
- Host-only new controllable settings `n_ubatch=256/64`, `n_threads=n_threads_batch=4/1`. `n_batch` remains 256; the original prompt, features, model and five teacher-forced checkpoint IDs are unchanged.
- Host workflow runs unified and three-stage batching in three same-CPU context configurations and three deterministic replay controls, verifying the exact GGUF SHA-256 before use.
- `EXECUTION_SENSITIVITY_REPORT.json` explicitly distinguishes `MICROBATCH_SENSITIVE`, `THREAD_SENSITIVE`, `BOTH_MICROBATCH_AND_THREAD_SENSITIVE`, `NO_MICROBATCH_OR_THREAD_EFFECT`, `FAIL_REPLAY`, and `INCONCLUSIVE_*`.
- All new data are based on the previously exported **synthetic** S26 fixture and logit checkpoint, not patient recordings or lecture transcripts.

## Completed offline checks

- Python evaluator branch tests, negative/corrupt fixture cases, replay controls: **PASS**.
- Repeated prior reference tests from 6.6.10 and 6.6.11: **PASS**.
- New evaluator applied to **real v6.6.11 GitHub checkpoints**, reproducing step-0 grouping RMSE 0.2855397426 and phone-vs-unified RMSE 0.3132871340: **PASS**. The positive sensitivity scenarios were simulated by changing which real reference vectors are fed into the evaluator; these are not claims of fresh model inference.
- Native host C++ source syntax-check against a mock pinned API header: **PASS**.
- New and existing GitHub workflows parse as YAML, and source-level guarded verifier passes: **PASS**.
- See source ZIP / patch proof for archive, pristine baseline, and unchanged Android inference files: **PASS**.

## Not yet established

- No Android SDK/NDK compilation or signed APK publication was performed in this environment. GitHub Actions must compile the APK and host runner.
- The full ~805 MB GGUF **was not run** in the new experimental configurations locally; the 6.6.12 CI run has not yet occurred.
- Host kernel/batching sensitivity cannot establish which batching arrangement is mathematically correct. M-RoPE and official-model parity remain `UNQUALIFIED` / `NOT_VERIFIED`.

## Run instructions

Commit the full source ZIP to the isolated Lab repository and push to `main`. GitHub Action **Qwen host execution sensitivity** will automatically run, and publish artifact `qwen-6.6.12-host-execution-sensitivity-<run>.zip`. Supply that ZIP for diagnosis. The Android APK auto-release workflow is unchanged apart from its version metadata; no new on-phone test is required for the host-only experiment.
