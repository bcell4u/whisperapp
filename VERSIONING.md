# Versioning, builds and repository updates

`version.properties` is the single authoritative version source. Its three entries must advance together for **each new released APK**:

```properties
releaseVersion=6.7.9
versionName=6.7.9
versionCode=67009
```

This source targets v6.7.9. Omi NPU remains separately selectable; its underlying model qualification is unchanged. Other speech engines and diarization are unchanged. Continue to increment all three fields for each subsequent release while preserving the existing application ID and **permanent signing certificate**. Follow [SIGNING.md](SIGNING.md) for CI signing secrets and update rules.

## Repository workflow

- The v6.7 integration is built on every branch push via the `push` trigger (always the debug `.dev` APK). Manual `workflow_dispatch` also remains available, and **only explicit manual `build_variant=release`** with the original signing credentials can build the production-package candidate. Successful push-triggered builds publish uniquely tagged debug `.dev` prereleases to GitHub Releases after lint/APK audits. Production builds and stable tags remain explicitly manual, signed operations; no APK is installed automatically.
- The workflow validates source, compiles native/Android components, runs JVM tests, and audits the APK. A local source verifier is not an Android build.
- Use a **Git patch** in GitHub Codespaces or a local clone to update an existing repository. A full source ZIP is appropriate for initial import, disaster recovery, or examining all files.
- In github.dev, copying the entire extracted source onto an existing project prompts to overwrite every matching filename, whether changed or identical. Use the small changed-files ZIP instead, and manually delete paths listed in its instructions. For deletions, a Git patch via a terminal is simpler.
- Check the final Git diff before committing. When changes seem unexpectedly numerous, verify folder roots and compare actual file contents; also check line-ending normalization before using `git add --renormalize`.
- Never commit signing keys, generated APKs, temporary downloaded model weights, or native build caches. `.gitignore` excludes key and build files.

## Compatibility requirements

Keep Parakeet QNN NPU, Parakeet/Omi OpenCL GPU, Qwen3/Whisper CPU, and optional sherpa-onnx CPU speaker diarization operational. Preserve the QNN and sherpa-onnx native runtime isolation and diagnostic export. Do not silently replace a manually selected backend or migrate user-owned SAF files without a tested plan.

In production, prefer NPU > GPU > CPU **only when choosing among installed models without a manual selection**. Successfully downloading a specific model is an explicit choice and activates that model for the next transcription; do not activate cancelled or failed downloads. One-time GPU and CPU heat/battery notices are separate. Word-level playback and speaker attribution reuse existing NPU decoder word timings where available, and fall back safely otherwise.

See [README.md](README.md), [NPU.md](NPU.md), and [SPEAKER_DIARIZATION.md](SPEAKER_DIARIZATION.md) for maintained user-facing and technical behavior.

### v6.5.15

Experimental Omi QNN/ORT compatibility probe under Settings → Diagnostics;
three compiled QNN 2.50 graph contracts, SHA-256 checks, per-attempt diagnostic
capture and synthetic inference, no transcription or model selection changes.
`versionCode=65015`.

### v6.5.16

Omi NPU experimental model downloader using the existing foreground service; manifest
validation, pinned SHA-256 and per-file SAF archive verification, resume support and
per-stage diagnostics. Omi compatibility probe now reads the archived package without
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

## v6.7.0 (Qwen3-ASR 0.6B integration candidate)

Rebased on v6.5.22 production, not the Qwen Lab fork. Adds the Snapdragon
SM-S942U QNN 12-second audio encoder plus pinned llama.cpp v0.6.0 ARM64
dotprod Q8_0 decoder (4 threads, one load per recording slice), and a two-file
Hugging Face downloader with SHA-256 verification. The 0.6B model is shown
as the only Qwen entry under Advanced Local ASR with a combined download size. The
legacy 1.7B/transcribe.cpp backend is retired in v6.7; Parakeet, Omi and other independent production features remain.
No Lab synthetic diagnostics or benchmark UI is included. The workflow is
push-triggered for debug and manually opt-in for release, and uploads APKs as Actions artifacts; production deployment must
wait for CI APK build, signer audit, and S26 transcription verification.
`versionCode=67000`.


## v6.7.1 (Omi probe cleanup for GitHub source overlays)

In GitHub Actions run 108136917571, strict source auditing found a legacy
Omi compatibility diagnostic file left in a Git checkout by a ZIP overlay.
Adds exact-path cleanup before source verification on the runner and explicit
Git deletion instructions; preserves the actual Omi transcription engine.
Restores successful CI progression without weakening the old-diagnostic guard.
VersionName `6.7.1`, versionCode `67001`. Android compilation awaits next run.


## v6.7.2 (retired Omi probe unit-test cleanup)

GitHub Actions run 108138936323 passed production/Qwen audits and compiled the
ARM64 llama.cpp decoder successfully. `:app:compileDebugUnitTestKotlin` then
failed because an old Git-tracked diagnostic unit test
`OmiNpuProbeContractsTest.kt` referenced the previously retired
`OmiNpuProbeContracts` class. The exact-path overlay cleanup now removes that
test before JVM compilation. The Qwen source audit verifies the test is absent
and that no main/test Kotlin, Java or AIDL source references the removed class.
The Omi recognition backends and their production tests are unaffected.
VersionName `6.7.2`, versionCode `67002`; APK and S26 verification pending.


## v6.7.3 (lint visibility and fail-closed publishing)

GitHub Actions run 39899122284 built and APK-audited a 6.7.2 debug APK, but
Android lint returned 1. Its detailed output was saved to an artifact rather
than printed in the run log, so its root cause is not yet known. CI now prints
issue IDs/locations and a bounded Gradle failure excerpt, retains the full lint
log/XML in the build artifact, then marks the BUILD job failed before release
publishing. No lint checks are disabled or ignored. VersionName `6.7.3`,
versionCode `67003`. Rebuild and inspect lint findings before claiming success.


## v6.7.4 (microphone permission safety and Android lint remediation)

GitHub run 108142028325 produced and APK-audited the 6.7.2 debug APK but
Android lint returned 7 errors and 9 warnings. The user-provided Gradle log
reveals only the first error: `MissingPermission` for the AudioRecord
constructor in `MicrophoneCapabilityProbe.kt`. This revision adds explicit
RECORD_AUDIO permission validation directly at the probe capture entry point,
WAV recorder entry and constructor boundary, and compressed recording service.
No lint checks were suppressed or disabled. The six other lint errors cannot
be identified without the XML/full lint report. A new Android build/lint run
and actual S26 playback smoke test remain mandatory. Version code `67004`.


## v6.7.5 (model download and Qwen window progress)

Model downloads show progress inside their corresponding cards and auto-select
newly verified models. Qwen transcription shows real encoder-window counts and
an indeterminate indicator during llama.cpp decoding. No new timing algorithm.
Version code `67005`.

## v6.7.6 (cumulative update-repair packaging)

GitHub run `108363851626` passed production source gates but stopped at the
Qwen source audit: 3 failed invariants. The **complete v6.7.5 source** already
passes all those checks locally. The evidence is consistent with applying the
v6.7.5 changed-only ZIP without the prerequisite v6.7.3 lint-workflow and
v6.7.4 microphone-permission files. This version includes those prerequisites
in a *cumulative repair overlay*, makes the audit identify relevant paths,
and retains strict permission/lint enforcement. Version code `67006`.
CI compilation/lint and S26 phone tests for 6.7.6 are not yet verified.

## v6.7.7 (Android lint errors from run 108367174862)

Fixes the five verified Android lint errors without suppressing checks: four Compose
`StateFlowValueCalledInComposition` errors in `MainActivity` and one
`SuspiciousIndentation` in `TranscriptionService`. Continues to enforce lint for
automatic prerelease publication. Nine lower-severity lint warnings are documented
for later review; this revision does not alter model runtimes or target SDK.
`versionCode=67007`. CI and S26 testing remain pending.

## v6.7.8 (measured Qwen transcription progress)

The Qwen QNN encoder and llama.cpp decoder report completed audio-window work
units, replacing a repeating indeterminate progress animation with a
count-based two-stage progress bar. `versionCode=67008`.

## v6.7.9 (setup download status placement)

The first-run setup model-selection introduction no longer repeats active
model-download messages. Qwen3-ASR progress and completion/failure reports
stay in the matching Hybrid (CPU/NPU) model card. Folder setup messages appear
under the folder picker; transient model-selection messages use a snackbar.
The transcription backend, speaker pipeline, and downloaded model files remain
unchanged. `versionCode=67009`. Android CI and device QA remain pending.
