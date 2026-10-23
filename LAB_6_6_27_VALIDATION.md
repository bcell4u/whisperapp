# WhisperRecorder Lab 6.6.27 local validation

## Input evidence

- 6.6.26 uploaded host/HF precision artifact: 20/20 logits vectors individually checked for 151,936 finite FP32LE elements, reported SHA-256 match and first token 11528; report marked `SAME_WEIGHTS_EXECUTION_PRECISION_COMPARISON_COMPLETE`.
- Existing control: same stored BF16 HF model weights used for BF16/FP32 arithmetic; model accuracy and official M-RoPE grid **not** established.

## Source validation

- 6.6.27 / Android versionCode 66027; prerelease only.
- Targeted GGUF axis perturbation implemented in `verification/host/qwen_reference_mixed.cpp` and not Android code.
- New strict graph-checked response evaluator and synthetic fail-closed tests.
- Workflow uses already verified v0.6.0 pinned host source and GGUF BF16 model, avoids extra HF model download, captures artifacts on partial failure.
- The complete local source-level test suite (23/23 Qwen scripts), both workflow YAML parses and embedded Bash syntax, host C++ compilation syntax against the pinned-API-shaped stub, and source verifier **passed**. Actual GGUF inference and APK builds remain GitHub-only. Archive and patch integrity are verified during packaging.

## Still pending GitHub

The 24 real GGUF runs and Android APK compile cannot be established from the local synthetic tests. Position-axis sensitivity alone is not an official Qwen audio RoPE correctness oracle and should not justify changing the phone decoder without audio accuracy evidence.
