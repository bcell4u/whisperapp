# Versioning, builds and repository updates

`version.properties` is the single authoritative version source. Its three entries must advance together for **each new released APK**:

```properties
releaseVersion=6.5.41
versionName=6.5.41
versionCode=65041
```

This source targets v6.5.41. Omi NPU remains separately selectable; its underlying model qualification is unchanged. Other speech engines and diarization are unchanged. Continue to increment all three fields for each subsequent release while preserving the existing application ID and **permanent signing certificate**. Follow [SIGNING.md](SIGNING.md) for CI signing secrets and update rules.

## Repository workflow

- `main` pushes trigger `.github/workflows/build.yml`; pull requests are suitable for compilation checks without publishing a production release.
- The workflow validates source, compiles native/Android components, runs JVM tests, and audits the APK. A local source verifier is not an Android build.
- Use a **Git patch** in GitHub Codespaces or a local clone to update an existing repository. A full source ZIP is appropriate for initial import, disaster recovery, or examining all files.
- In github.dev, copying the entire extracted source onto an existing project prompts to overwrite every matching filename, whether changed or identical. Use the small changed-files ZIP instead, and manually delete paths listed in its instructions. For deletions, a Git patch via a terminal is simpler.
- Check the final Git diff before committing. When changes seem unexpectedly numerous, verify folder roots and compare actual file contents; also check line-ending normalization before using `git add --renormalize`.
- Never commit signing keys, generated APKs, temporary downloaded model weights, or native build caches. `.gitignore` excludes key and build files.

## Compatibility requirements

Keep Parakeet QNN NPU, Parakeet/Omi OpenCL GPU, Qwen3/Whisper CPU, and optional sherpa-onnx CPU speaker diarization operational. Preserve the QNN and sherpa-onnx native runtime isolation and diagnostic export. Do not silently replace a manually selected backend or migrate user-owned SAF files without a tested plan.

In production, prefer NPU > GPU > CPU **only when choosing among installed models without a manual selection**. One-time GPU and CPU heat/battery notices are separate. Word-level playback and speaker attribution reuse existing NPU decoder word timings where available, and fall back safely otherwise.

See [README.md](README.md), [NPU.md](NPU.md), and [SPEAKER_DIARIZATION.md](SPEAKER_DIARIZATION.md) for maintained user-facing and technical behavior.

### v6.5.15

Historical (retired in v6.5.40): Omi QNN/ORT compatibility probe under Settings → Diagnostics;
three compiled QNN 2.50 graph contracts, SHA-256 checks, per-attempt diagnostic
capture and synthetic inference, no transcription or model selection changes.
`versionCode=65015`.

### v6.5.16

Omi NPU experimental model downloader using the existing foreground service; manifest
validation, pinned SHA-256 and per-file SAF archive verification, resume support and
per-stage diagnostics. The historical Omi compatibility probe read the archived package without
a document-tree selection. No Omi NPU speech recognition is enabled.
`versionCode=65016`.

### v6.5.17

Added opt-in Omi Med NPU transcription (three separate ORT-QNN contexts, 12s windows, host TDT/SentencePiece), experimental model selection, chunk diagnostics, and an explicit accuracy caveat. `versionCode=65017`. No signing key or application ID changes.

## v6.5.18

Fixed overlap-order native word alignment; native timing classification now reflects actual
matching rather than a cue-count heuristic. Added exact-token subsequence handling for
conservative repetition cleanup and text-free detailed failure diagnostics. Added Omi NPU
frontend feature/PCM statistical summaries and TDT exit-condition metrics. No changes to
model downloads, signing configuration, package ID or other ASR runtimes. `versionCode=65018`.

## v6.5.19 (source candidate; Android CI build not yet verified)

Corrects Omi NPU's per-encoder-frame TDT symbol-budget handling: positive duration advances reset the counter and no extra frame is skipped. Adds deterministic decoder regression tests and offline Mel feature parity fixture/comparison scripts. This is an experimental accuracy change, not a qualified model or a confirmed accuracy improvement. Keep permanent signing identity and installed models unchanged.

## v6.5.20 (Build 2 source candidate; Android CI build pending)

Adds independent PyTorch/Librosa reference-feature generation and provenance-checked
per-bin comparison for offline Omi NPU preprocessor evaluation, four targeted
Kotlin numerical-golden/mask/padding regression tests, and a log analyzer for
the last completed `omi_npu` session in mixed app logs. Baseline v6.5.19
Omi decoding, frontend behavior, QNN graph contexts, downloaded assets, GPU
engines, diarization, storage and release signing logic are unchanged. The
original Omi NeMo checkpoint and numerical encoder artifacts remain necessary
for canonical-model parity qualification, and clinical transcription accuracy
is not established. `versionCode=65020`.

## v6.5.21 (model catalog UI cleanup source candidate)

Unifies Parakeet NPU and Omi Med NPU cards with the existing GPU/local-model card
layout (model name, backend/license, size, language count, Use/Download/Resume,
Model link and removal). Removes the Parakeet NPU Verify files button from Models
& downloads and initial setup, shortens the introductory performance/storage guidance, and removes
the user-facing Omi Experimental label without changing model identity, pinned
revision, checksum validation, decoder, QNN graphs, or numerical qualification.
Adds a confirmed Omi package removal action with safe selected-engine fallback.
`versionCode=65021`.

## v6.5.22 (static audit and stability maintenance source candidate)

Removes synchronous model inventory/migration work from the initial Models and Setup
Compose render, and resolves library/detail model paths asynchronously. Makes NPU model
removal mutually exclusive with other model actions,
limits cleanup of temporary NPU runtime files to the removed package, and improves
long-transcript cue partitioning without changing saved text or cue timestamps.
Refreshes stale release documentation and adds regression guards. No model contexts,
transcription inference, speaker diarization, signing identity or storage format changed.
Android Gradle compilation and on-device regression testing remain pending.
`versionCode=65022`.


## v6.5.35 (diagnostic ZIPs in Workspace Logs)

New diagnostic ZIPs are published to the user-selected `Whisperapp/Logs` SAF directory,
with checksum verification and a bounded app-private fallback if the provider is unavailable.
Existing private reports remain accessible; deletion only targets generated ZIP names.
Version code 65035; debug application version 6.5.35-dev.

## v6.5.34 (diagnostics foundation source candidate)

Based on v6.5.22, adds app-private structured diagnostic ZIPs, bounded per-chunk timing,
engine/device/thermal/memory metadata, failure stack symbols and controlled retention.
A new Settings > Diagnostics section supports create, share, delete one, and delete all.
No model inference backend, recording, model file, signing identity or SAF storage change.
This is not a working experimental 0.6B Qwen hybrid migration or a validated Android APK.
`versionCode=65034`.

This v6.5.34 patch also replaces an obsolete Qwen hybrid unit-test file that can be left behind by incremental uploads to a v6.5.32 GitHub repository. GitHub unit tests for this clean baseline now cover the existing Qwen3 CPU policy instead of a hybrid engine that is not included yet.


## v6.5.37 (GitHub APK audit hotfix)

The v6.5.36 GitHub Actions run compiled Kotlin and native code, passed JVM unit tests, and assembled and signed the debug APK. It failed only in the post-build APK audit, which still required the obsolete UI text `Use NPU`. Restore the existing regression-proven check for `parakeet_npu`, `omi_npu`, and `qwen3_hybrid` engine identifiers and add a source guard against reintroducing that marker. Inference, model loading, diagnostics, signing and application IDs are unchanged. `versionCode=65037`. Hardware validation remains pending.

## v6.5.38 (experimental Qwen decoder optimization and diagnostics)

- Version name 6.5.38; code 65038; `.dev` debug build remains side-by-side.
- Preserve the NPU encoder, verified Stage I model, FP32 baseline decoder path,
  Parakeet/Omi and the shared user-selected model folder.
- Add OFF-by-default reusable direct input buffers for the 28-layer CPU decoder's
  per-token embedding/position inputs, plus a scratch-buffered embedding reader.
  KV outputs remain ORT-owned and are never overwritten while in use.
- Add worker PSS snapshots around separate ONNX decoder session creation, prefill
  PSS, first-token delay, and counts of >250 ms token steps to diagnostic ZIPs.
- Existing K2A FP16 embeddings remain optional and digest-gated; no model download
  or conversion occurs on the phone. Qualcomm compilation is not performed.
- Source validation is not a device-performance benchmark. Perform controlled
  baseline/reuse A/B with original FP32 embeddings and identical audio, then K2A
  only after supplying a validated companion file and separate accuracy review.


Version 6.5.40: Qwen cross-process configuration synchronization and optional CPU-arena memory experiment; see RELEASE-v6.5.40.md.

Version 6.5.41: restored the general Diagnostics transcription-log preview state inadvertently removed with the Omi probe. No model/backend or runtime behavior changed. See RELEASE-v6.5.41.md.
