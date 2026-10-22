# 6.6.24 Lab validation and scope

## Uploaded 6.6.23 evidence

The GitHub archive `qwen-6.6.23-hf-gguf-weight-identity-16.zip` reported all 254 mapped BF16/FP32-cast tensors equivalent, all 56 Q/K projections BF16-byte-identical, and zero measured mismatches across 28 decoder layers. HF 2D synthetic baseline top1 is ID 11528. HF equal 3D/4D and asymmetric 4D position inputs failed with `ValueError`; error messages were not captured. The report explicitly states `COMPLETED_WITH_UNRESOLVED_CONTROLS` and official M-RoPE remains unverified.

## New experimental controls

The pinned independent HF checkpoint is SHA-verified again on GitHub, with 2D explicit vs implicit positions, 2D uniform +17 and suffix-only +11 positional offsets, 3D/4D input-shape tests with sanitized error details, and direct rotary module shape/function probes. The existing BF16/FP32 and Q/K audit phases and consolidated fail-closed report continue.

## Local checks

- All 20 `scripts/test-qwen-*.py` suites passed, including the new position-contract tests, real older synthetic logit controls, and workflow-context regression.
- `scripts/verify-lab-source.py` passed.
- Both GitHub Actions YAML workflows parsed with PyYAML.
- Actual HF/GGUF execution, APK compilation, and official audio M-RoPE parity remain **unverified locally**.
- No Android inference source, source recording, private path, user audio, transcript, model weight or signing credential is added to the artifacts.

## Interpretation boundaries

Success means the supported HF 2D position argument and independent RoPE module input behavior have been characterized on the synthetic fixture, **not** that the provisional four-coordinate GGUF mapping is authoritative or that clinical terminology recognition improves.
