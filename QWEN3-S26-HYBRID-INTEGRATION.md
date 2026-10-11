# Experimental Qwen3-ASR-0.6B hybrid for Galaxy S26 / SM8850 / Hexagon V81

Source integration against WhisperRecorder **v6.5.23**. This is a **source-code prototype**, not a verified Android APK. No existing Omi, Parakeet or Qwen GGUF backend has been replaced. No model or Qualcomm cloud compilation is included.

## Artifact & qualification boundary

Uses the independently published Stage G repository:
`drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81`.

The prior Windows real-speech test demonstrated an **S26 native QNN FP16 encoder + Windows FP32 ONNX decoder** transcript/token parity on one test clip. It did **not** test this Android integration. In particular, restoring an AI Hub linked `.bin` through Android ORT-QNN EPContext is unproven; the native Android QAIRT runtime is 2.50 while the native QNN runner used in Stage F reported 2.51. The two CPU FP32 decoder sidecars total multiple gigabytes and may exceed the phone's practical RAM budget. The app fails explicitly instead of silently falling back to a CPU encoder or returning an unverified result.

## Implemented in this source

- Separate `qwen3_hybrid` engine, **never auto-selected**, without changing existing paths.
- Models & downloads card: experimental label, install/resume, select, remove, repository link and **Test Qwen preprocessing + S26 NPU encoder**.
- Manifest-driven large-file download with `.part` resume, SHA-256 of **every required artifact**, pinned exporter/revision schema/identity, minimum expected embedding and golden file sizes; atomic activation via READY marker. The manifest itself is frozen once accepted. A remote repo that changes files during download fails integrity checks rather than silently overwriting.
- One app-private storage copy under `filesDir/qwen3-hybrid/stage-g-0.6b`, rather than duplicating huge external decoder `.onnx.data` files into two locations. Models remain through app upgrades; uninstalling the app removes app-private files. The in-app Remove action affects this Qwen folder only. It does not delete user recordings or any Omi/Parakeet files.
- Qwen preprocessing: 16 kHz mono, 192,000 input samples, pad short tail, `[1,128,1200]` mel, centered reflect STFT 400/160, periodic Hann, librosa-Slaney filters, power/log normalization and final STFT frame removal. Byte-level synthetic golden comparison **before** QNN execution; independent local JVM reference check max absolute difference `3.314e-05`, RMSE `3.083e-06` for Stage G's synthesized waveform, under gates `3e-4`/`3e-5`.
- Separate `:qwen` Android process and AIDL channel. It loads `encoder-qnn-fp16.bin` using a generated EPContext wrapper, requires an actual QNN provider/`libQnnHtp.so`, explicitly disables CPU EP fallback, runs a synthetic-mel encoder graph on Android, and checks tensor shape and finiteness.
- CPU ONNX decoder init/step using INT64 prompt/position IDs, input embeddings from the **FP32** `[151936,1024]` table, 28-layer KV cache outputs recycled at each step, and 256-token EOS-bound greedy decoding. `tokenizer.json` output uses a ByteLevel decoder.
- Fixed **12-second** audio chunks, 1-second overlap, existing transcription cancellation, error propagation and independent model selection.
- Scratch EPContext `.onnx` wrapper is confined to `cacheDir/qwen3-hybrid-epcontext` and removed on Qwen session shutdown. The Models screen includes an independent **Clear Qwen temporary test files (manual)** recovery button, which deletes only that directory and verifies its absence; this does not touch recordings or models. No ADB staging or `/data/local/tmp` phone folders are used by Android Qwen.

## How to test

1. Build this source in the **existing Android Gradle/CI environment**, using the project's pinned Android SDK/NDK and existing signing configuration. Install a debug version on **SM-S942U**. A local Android Gradle build was **not possible in the review environment** because the Android SDK, Gradle build wrapper and QNN libraries were not present.
2. Make the Stage G Hugging Face repo public with Builder v0.3.62 (G2 or G3). Confirm it contains `model-manifest.json`, `android-integration-contract.json` and the ten manifest-listed model / fixture files. The app does not collect Hugging Face credentials and a private repo will return HTTP 401/403.
3. Use **Models & downloads → Qwen3-ASR hybrid → Download**. Allow multiple GB of free private app storage; downloads resume if interrupted. Verify READY and the expected files.
4. Click **Test Qwen preprocessing + S26 NPU encoder**. The app first rechecks model checksums, verifies Android mel vs the synthetic Stage G fixture, attempts to restore the QNN context with CPU fallback disabled, and actually executes one synthetic NPU encoder call. If any stage fails, **do not proceed as if the encoder is validated**. Save the exact UI error and isolated process logs for a targeted correction. There is no automatic cloud recompilation.
5. After the probe passes, select the separate experimental Qwen model and transcribe a **non-sensitive ~8-12 second** `.ogg` clip. Compare its text/tokens with the Windows Stage G reference (matching **decoded PCM**, not just file extension). Decoder RAM/OOM, tokenizer fidelity and per-token latency remain unproven. Do not use patient recordings for the first validation.
6. Test 2-minute audio and error handling only after the 12-second gate passes. Long-form concatenation, overlap policies, transcript timestamping, cancellation mid-ORT run, thermals and clinical accuracy remain unqualified.
7. Capture a clean Android result: phone identity, mel golden max/RMSE, QNN graph call count, CPU fallback `disabled`, decoder generated IDs/transcripts parity and stable memory/latency. Do **not** switch production defaults until validated.

## Known integration risks (not successes)

- Requested graph name `qwen3_asr_encoder` may not match the linked QNN context's actual internal name. The Android probe should reveal this; do not silently rename or recompile without evidence.
- ORT QNN 2.50 and native QNN 2.51 may be incompatible. No assertion of successful EPContext restoration in Android has been made.
- Android ORT CPU may run out of RAM loading two FP32 decoder graphs and their separate 2.38 GB sidecars. Model package is about 6 GB; compressed INT4 and/or mobile memory-mapped shared-weights decoder **must be independently validated** before replacing the proven FP32 parity reference.
- `qwen3_hybrid` is intentionally experimental and **never an automatic first-run selection**; original Parakeet, Omi and Qwen GGUF paths remain available.
- Medical ASR use requires a separate accuracy/quality validation; one Stage G parity clip alone is insufficient.

## Source provenance

- Model and contract: Stage G in WhisperRecorder NPU Builder v0.3.62 (uploaded in the conversation).
- Encoder/decoder contract: `python/qwen3_asr_stage_g.py`, pinned exporter commit `4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50`.
- Preprocessing reference: `src/mel.py` of the same pinned upstream exporter.
- No downloaded model weights, patient audio, credentials, signing keys or Qualcomm runtime shared libraries are included in this source ZIP.

## v6.5.23 source-verification update

The GitHub Actions `Verify source` step previously expected only two NPU cards
and forbade all user-facing experimental warnings. Those assumptions did not
match the new separate Qwen hybrid card. Both guards now accept the third card
while still requiring Qwen's experimental warning and checking that Parakeet
and Omi display labels do not acquire it. This is not an Android runtime fix.


## v6.5.24 — Stage H lossless compact decoder (experimental)

The app now recognizes **both** Qwen package schemas. The original Stage G v1
release on Hugging Face `main` uses two independent `decoder_init.onnx.data` and
`decoder_step.onnx.data` files. The Stage H v2 release is published only to the
`main (compact schema v2)` branch and uses `decoder_weights.data` referenced by **both**
`decoder_init.onnx` and `decoder_step.onnx` in the same app-private directory.
A filename rename is not valid: the Stage H Builder rewrites external-data
references and validates generated tokens and logits losslessly.

New downloads and upgrades request only the Stage H branch, check its schema v2
manifest, Stage H CPU smoke status, SHA-256 for all model assets, QNN context
identity, and shared-weight Android contract before writing `READY.json`.
Existing verified Stage G installs are still usable for rollback. Stage H
installs to `qwen3-hybrid/stage-h-compact-0.6b` and never overwrites
`qwen3-hybrid/stage-g-0.6b`. Users with both installed can deliberately remove
the old v1 assets *after* v2 is verified, from Models & downloads. This may
need temporary free space for both installations during migration.

**Publishing prerequisite:** Complete Stage H1 with real weights and H2 with a
verified `main (compact schema v2)` upload. Without H2, a new compact download will
fail rather than silently downloading the 6 GB model from `main`. This source
update does not prove Android S26 decoder memory/performance or QNN EPContext
compatibility; keep the experimental warning and run the Android hardware
probe followed by audio transcript comparisons.

Both onboarding and Settings > Models & downloads now use uniform catalog cards
grouped NPU, then GPU, then CPU. The old NVIDIA Parakeet and Advanced Local ASR
section titles are removed, without removing those models.
