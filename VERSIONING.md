# 6.6.11 — host-only prefill boundary isolation

versionName 6.6.11 / versionCode 66011. The Android decoder binary/source remain unchanged; the new host-only `[prefix+audio][suffix]` and `[prefix][audio+suffix]` comparisons isolate grouping effects.

# Lab v6.6.10 three-path decoder batching isolation

- versionName `6.6.10-dev`, versionCode `66010`, prerelease tags `v6.6.10-lab.*`.
- One additional **GitHub-only** llama.cpp host test: extended API with a 3-stage prefix/audio/suffix prefill. Compare to the existing standard split API and extended unified API on exactly the same CPU/model/revision; all retain the four fixed teacher-forced tokens.
- Android decoder binary source and normal transcription remain unchanged. Independent official Qwen logits/M-RoPE are still unqualified.
- New artifact `CHUNK_ISOLATION_REPORT.json`, `PROVENANCE.txt`, and the synthetic-only 5-step vectors.

# Lab v6.6.9 host batch path diagnostic build

- Experimental app versionName `6.6.9-dev`; Android versionCode `66009`; GitHub prerelease tags `v6.6.9-lab.*`.
- Separate Lab applicationId and existing Android signing variable names retained.
- On-device performance/inference is identical to 6.6.7; only version metadata and hosted verification code changed.
- A separate manually dispatched Actions workflow runs the S26 synthetic Q8 decoder reference on a hosted x86 CPU. Its output is diagnostic, **NOT** official PyTorch/ONNX M-RoPE parity.
