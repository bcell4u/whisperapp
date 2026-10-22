# Lab 6.6.16 — host CPU ISA isolation

- `versionName=6.6.16`, `versionCode=66016`, separate Lab package and existing signing workflow preserved.
- New host-only SIMD versus portable x86 CPU CMake configurations, synthetic numerical evaluation and CI artifacts.
- Android JNI/native CPU decoder and QNN path are unchanged; GitHub still must compile the APK and run the GGUF test.

## v6.6.15 (versionCode 66015)
Synthetic-only host precision control: Q8_0 and BF16 weights, 128 vs 256 microbatch, unified vs three-stage, original synthetic embeddings and 156 text filler tokens. Android normal inference unchanged; GH workflow does the real model run.

## v6.6.14 (versionCode 66014)
Synthetic-only input modality control: original features, all-zero audio embeddings, and 156 ordinary text filler tokens. Compare unified/three-stage 128 vs 256 microbatches on pinned host Q8 model. Android transcription path unchanged.

## v6.6.13 (versionCode 66013)
Host-only numerical probe at n_ubatch=64,128,160,192,256, pinned four-thread
CPU/256 n_batch and original 156-frame/171-position synthetic fixture.
Android native transcription is unchanged. Full APK compilation and host GGUF
execution require the GitHub Actions workflows; local checks are not APK proof.

# 6.6.12 — host-only microbatch and CPU thread sensitivity

versionName 6.6.12 / versionCode 66012. Compared with 6.6.11, only host synthetic decoder arguments, host workflow, host comparison/test scripts, release metadata, and documentation are changed. **Android native decoder, Java/Kotlin ASR, QNN execution, stitcher, signing configuration, and model backup logic remain untouched.** Actual APK compilation and actual model inference require GitHub Actions.

---

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
