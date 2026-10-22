# WhisperRecorder 6.6.22 Lab versioning

- Lab version `6.6.22`, Android version code `66022`; original production 6.5.22 is untouched.
- Lab app ID and existing signing contracts preserved.
- GitHub main builds a distinct lab APK prerelease and runs the host `qwen-6.6.22-hf-gguf-weight-identity-*` multi-stage audit.
- Four diagnostics: checkpoint storage including FP32 norms, Q/K head layout, HF independent positional representation, and combined fail-closed classification.
- No on-device runtime or speed changes and no private user data in reports; HF/GGUF downloads are ephemeral.
