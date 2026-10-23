# WhisperRecorder 6.7.0 UI, audio sync, and GitHub Release update

## Changes (source only)
- Removed the Omi Med NPU **compatibility diagnostic only**, its UI, AIDL entry, worker
  test/inference paths, IPC client call, and verification script. Kept the real Omi NPU
  transcription model and its three QNN graph contracts. Renamed those contracts away
  from the misleading `ProbeContracts` name.
- Excluded `encoder-qnn-*.bin` from selectable/restored Whisper models, including the
  startup imported list. Existing files are left untouched. The Qwen QNN context and
  tokenizer continue to be fetched only when the Qwen 0.6B model download is requested.
- Renamed NPU headings **NPU Accelerated**, grouped Parakeet + Omi OpenCL as
  **GPU Accelerated**, and moved only Qwen 0.6B to **Hybrid (CPU/NPU)**.
- Added per-Qwen-decoding-window text retention through Qwen native worker IPC.
  The session exposes *approximate window boundaries*, not native word timestamps;
  timestamp builder estimates short (up to three-word) cues against the 16kHz PCM
  within each <=12s window. It clips 1s-overlap boundaries and 20ms speech-frame
  rounding overlaps, falling back when transcript cleanup altered segment text.
  Playback offset slider remains. Existing transcript timelines are **not rewritten**;
  retranscribe the recording to regenerate timing metadata.
- Every successful CI push/debug build produces a `.dev` APK and a uniquely tagged
  **GitHub prerelease** with APK and build-proof.txt. Manual `build_variant=release`
  with matching original signing secrets produces the production app ID; only then
  is the stable version release created. GitHub token needs `contents: write` in
  the publish job. Lint must return zero, and the SHA-256 is rechecked before publishing.
  Existing release tags are never overwritten.

## Verification scope
Static source verifier and Qwen invariants passed locally. Kotlin host smoke
compiled and exercised window-boundary cue placement and stitched text equality.
There is no verified Android SDK/NDK Gradle build or S26 on-device run in this
container. GH Actions and S26 smoke tests are required before treating this
as device-qualified. Staged native PIE execution from APK native-library path
remains an outstanding Android-policy check.

## Delivery
The changed-files-only ZIP mirrors repository-relative paths and includes
`DELETED_FILES.txt` for removals (a copy-overlay cannot apply deletions).
The companion incremental patch applies against the preceding
`LlamaOnly-AutoBuild-NDKFix` source ZIP. Do not use as an APK.
