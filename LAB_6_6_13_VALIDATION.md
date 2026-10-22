# WhisperRecorder Lab 6.6.13 — validation and scope

## GitHub artifact input (actual, not simulated)

The uploaded 6.6.12 artifact reported `MICROBATCH_SENSITIVE` on pinned host llama.cpp
revision `d81235049384534c167caea52b85a694f6103d14` using the same
SHA-pinned Q8 GGUF and synthetic features. All five comparisons were reproducible.
The first-token **unified vs three-stage** RMSE measured **0.285540** at microbatch
256, **0.221684** at microbatch 64. Within unified, the 256-to-64 RMSE was
**0.202782**; within three-stage **0.234128**. Single-thread and four-thread
checkpoints were byte-identical for both modes at all five positions. The
uploaded S26 checkpoint remains closer to unified/256 (RMSE **0.313287**) than
to three-stage/256 (**0.471326**). Smaller RMSE to S26 does not prove correctness.

## Changes in 6.6.13

- New host microbatch threshold experiment: 64, 128, 160, 192, 256, with
  n_batch 256, 4 CPU threads, n_ctx 512, zero GPU layers, unchanged model/input,
  positions and four teacher-forced tokens.
- Both unified and three-stage prompt routes, five logits checkpoints each.
- Replay controls for unified/256, three-stage/256 and unified/160.
- Comparisons across each adjacent size and both modes; S26 only at step 0.
- Distinct outcome classes and strict `FAIL_REPLAY` and `INCONCLUSIVE_*` results.
- Separate GitHub workflow automatically runs the pinned SHA-checked host test
  when diagnostic source is pushed to `main`; APK auto-release is unchanged.
- Version bumped to 6.6.13 (version code 66013). On-device Kotlin, native
  decoder, QNN encoding, GGUF handling and model backups are unchanged.

## Completed offline gates

- Source inventory/safety verifier (`scripts/verify-lab-source.py`) PASSED.
- Previous host comparator controls and new threshold classifier tests PASSED.
- Host mixed C++ runner syntax checked using API stubs (not linked to full llama.cpp).
- Both GitHub workflow YAML documents structurally parsed and checked.
- Actual 6.6.12 numerical report and synthetic SHA-256 fixtures verified.
- Release source and patch checked against unchanged Android inference code.
- Full source archive, incremental archive, and binary-safe patch reconstructed
  and compared (performed during packaging).

## Unperformed verification

No Android SDK/NDK compile, no GH Actions run of the 805MB GGUF, and no phone
transcription were performed in the local environment. All three require the
user's GitHub pipeline or device. This host experiment does not provide an
independent Qwen mathematical implementation or official M-RoPE equivalence.

**After pushing:** upload `qwen-6.6.13-host-microbatch-threshold-*`, including
`MICROBATCH_THRESHOLD_REPORT.json` and `PROVENANCE.txt`. No phone run needed.
