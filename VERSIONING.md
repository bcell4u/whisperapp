# WhisperRecorder 6.6.26 Lab versioning

- Experimental Lab versionName `6.6.26`, versionCode `66026`; production v6.5.22 not changed.
- Automated release: `v6.6.26-lab.*` prerelease; debug application ID retains `.dev` suffix.
- Host workflow: independently compute synthetic HF BF16/FP32 same-source-weight positional responses, with GGUF BF16 Flash ON/OFF controls. Artifact name `qwen-6.6.26-hf-execution-precision-*`.
- The test does not qualify the official Qwen ASR audio M-RoPE grid or prove medical transcript accuracy.
- No Android inference source code changes. CI compilation and actual GGUF/HF calculations are pending GitHub.
