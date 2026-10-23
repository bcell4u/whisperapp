# WhisperRecorder 6.6.27 Lab versioning

- Experimental Lab versionName `6.6.27`, versionCode `66027`; production v6.5.22 remains untouched.
- Automated prerelease: `v6.6.27-lab.*` with isolated application ID and `.dev` debug suffix.
- Host experiment: BF16 GGUF M-RoPE axis-attribution matrix, 12 synthetic position maps × Flash Attention ON/OFF, no HF weight re-download. Artifact: `qwen-6.6.27-host-rope-axis-attribution-*`.
- A single-axis perturbation is a host numerical control, **not** the official audio M-RoPE grid.
- No Android inference source modifications. Host model execution and full Android Gradle/NDK APK compilation are GitHub gates.
