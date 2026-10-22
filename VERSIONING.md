# WhisperRecorder 6.6.25 Lab versioning

- Separate experimental Lab app: versionName `6.6.25`, versionCode `66025`, production version 6.5.22 unchanged.
- Host workflow: pinned HF 2D and GGUF BF16 four-axis **matched position** controls, with Flash Attention ON/OFF requests, producing `qwen-6.6.25-hf-gguf-position-parity-*`.
- The evidence is synthetic and cannot establish official Qwen ASR M-RoPE parity or medical transcription accuracy. Android runtime is unchanged.
- Local CI scripts and packaging are validated; actual pinned checkpoint computation and Android APK compilation are GitHub stages.
