# WhisperRecorder 6.6.24 Lab versioning

- Separate Lab app version `6.6.24`, Android versionCode `66024`. Production 6.5.22 remains untouched.
- GitHub Actions publishes an experimental Lab APK on pushes and runs pinned BF16 weight, Q/K layout, HF positional-interface and consolidated host verification under `qwen-6.6.24-hf-gguf-weight-identity-*`.
- No local Android APK compilation or actual 6.6.24 GGUF/HF run is claimed.
- HF positions are not yet certified as equivalent to Android 4-axis provisional linear M-RoPE; all tests use public models and synthetic inputs only.
