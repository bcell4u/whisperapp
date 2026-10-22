# WhisperRecorder 6.6.22 — Multi-phase diagnostic release validation

## Scope

This source release adds a **single multi-stage GitHub host audit** to the prior 6.6.21 Lab source. It does not modify the Android QNN encoder, ARM64 Q8_0 decoder, numerical inference parameters, or Flash Attention defaults. It does not compile an APK locally.

## Source evidence driving the new work

The **uploaded v6.6.21 GitHub artifact** reported 123/123 compatible decoder BF16 tensors byte-identical, 57 incompatible FP32/BF16 named tensors, 131 GGUF tensor names not mapped, and zero verified value differences. This is incomplete *coverage*, not a proof of total model parity.

## Newly combined phases

1. **Source + provenance**: pinned 6.6.22 version, 66022 Android code, separate Lab app, source and standalone test suite. Public BF16 HF and GGUF checkpoints are SHA-256-pinned; no model, private speech data, signing key or transcript is included in output.
2. **BF16 / FP32 norm weight values**: bitwise BF16 comparison; exact BF16-to-FP32 bit extension for eligible norms (including attention Q/K norms), with explicit HF text decoder namespace.
3. **Q/K projection storage**: raw BF16 equality plus two candidate head-local row permutations using GGUF metadata head-counts, with per-layer coverage and fail-closed flags; this does not verify dynamic RoPE use.
4. **Independent Transformers positions**: published Transformers decoder on the pinned synthetic 156x1024 fixture, comparing 2D linear IDs to equal 3D and 4D ID tensors and an intentionally asymmetric 4D control. Synthetic positions are not a real speech M-RoPE spatial grid. The independent 2D HF baseline is also compared against the SHA-pinned original S26 first-token logits.
5. **Consolidated decision**: aggregates available stage reports into `MULTIPHASE_REPORT.json`, never treating missing or failed stages as successes.

## Local tests completed

- `scripts/verify-lab-source.py`: PASS.
- All `scripts/test-qwen-*.py`: PASS, including the 6.6.22 exact-FP32 norm conversion/corruption tests and candidate Q/K permutation and incomplete-report controls.
- Both GitHub workflow YAML files parsed: PASS.
- `scripts/activate-lab-workflow.sh` syntax: PASS.
- Source ZIP / update ZIP integrity: PASS after final packaging.
- Binary-safe Git patch reconstructs the complete source byte-for-byte: PASS after final packaging.
- Android Kotlin, native decoder and transcriber C++ sources unchanged versus v6.6.21: PASS.

## Still pending in GitHub Actions

- Real 1+ GB pinned BF16 checkpoint tensor audit and Q/K layouts.
- Real HF Transformers BF16 positional-axes runs and consolidated artifact.
- Compilation/signing of the Android Lab APK.
- Independent proof of official Qwen multi-axis M-RoPE, full ASR accuracy, medical vocabulary quality and on-device runtime behavior. **None is claimed in this release.**

## Next artifact

`qwen-6.6.22-hf-gguf-weight-identity-*.zip` containing `WEIGHT_IDENTITY_REPORT.json`, `QK_LAYOUT_REPORT.json`, `HF_POSITION_AXES_REPORT.json`, `MULTIPHASE_REPORT.json` and `PROVENANCE.txt` as available. Upload this single artifact for the next decision. Phone tests are not needed for the host audit.
