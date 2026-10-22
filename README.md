# WhisperRecorder Qwen GGUF Lab 6.6.22 — multi-phase decoder verification

Android `versionName=6.6.22`, `versionCode=66022`. This is a separately installable experimental Lab app; the QNN encoder, ARM64 4-thread Q8 decoder, Flash Attention behavior, overlap, privacy, model persistence and signing remain unchanged.

The **same host workflow** now audits (1) pinned BF16 byte equality and exact GGUF FP32 norm casts, including Q/K normalization, (2) all decoder Q/K projection tensor layouts with explicit head-permutation candidates, (3) published HF Transformers equal-axis 2D/3D/4D positional representations plus a synthetic asymmetric control, and (4) an independent fail-closed consolidated report.

Push the entire source into the Lab repo `main`. Both Lab APK auto-prerelease and host audit start automatically. Upload `qwen-6.6.22-hf-gguf-weight-identity-*` with the four reports. No new phone transcription test needed. Official Qwen multi-axis RoPE correctness and real medical transcription improvements still need separate proof.

See [LAB_6_6_22_NORM_WEIGHT_EQUIVALENCE.md](LAB_6_6_22_NORM_WEIGHT_EQUIVALENCE.md) for full design and limitations.
