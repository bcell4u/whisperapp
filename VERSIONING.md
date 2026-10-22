# WhisperRecorder Qwen GGUF Lab v6.6.21 - GGUF metadata header repair

Android `versionCode=66021`, `versionName=6.6.21` (Lab debug APK uses `6.6.21-dev`). Separate Lab app ID, optimized QNN encoder/ARM64 Q8_0 dotprod decoder and unchanged privacy controls remain in place.

Host-only CI fixes the 6.6.20 `GGUF_EXCESSIVE_ARRAY_LENGTH` abort and repeats the same pinned HF BF16 versus GGUF BF16 direct decoder tensor audit, using a bounded streaming GGUF metadata parser. Model identity and official M-RoPE parity remain unqualified until the actual GitHub evidence is evaluated. Expected artifact: `qwen-6.6.21-hf-gguf-weight-identity-*`.
