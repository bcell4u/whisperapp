# WhisperRecorder Qwen GGUF Lab v6.6.11 — validation receipt

## What is established

- Source upgraded from complete v6.6.10 archive to v6.6.11, code 66011.
- Android native QNN/llama decoder, model downloader, four-thread configuration, optional overlap and transcript cleaner are **byte-for-byte unchanged**.
- Existing synthetic fixtures retain their S26 SHA-256 identity. No raw recording, transcript, GGUF or signing key is included.
- New host test isolates two possible cut points in the 171-position synthetic prompt; original unified and three-stage host modes retained.
- Both two-stage modes run twice, compare five checkpoints, guard standard-versus-extended three-stage reference, check finite numerical vectors and produce an explicit `INCONCLUSIVE` if inputs are unavailable.
- Python positive, negative, corruption and integrity tests passed locally, including all categorical outcomes.
- Real 6.6.10 host vectors exercised new evaluator for controlled cases where the new paths exactly match known previous outputs; calculated first-token S26 comparisons matched the real 6.6.10 report.
- Both host C++ sources passed **syntax-only** compilation with test API stubs; YAML workflow parsing and static project source checks passed.
- Complete source, incremental ZIP and Git binary patch are packaged from an exact v6.6.10 source baseline and reconstructed byte-for-byte in a separate directory.

## Not yet established

- This environment did **not** build the Android APK (no Android SDK/NDK), publish a GitHub release, compile against the real pinned `llama.cpp` library or execute the v6.6.11 two-stage model runs with the actual ~805 MB GGUF.
- v6.6.11 source does not yet prove which batching arrangement is mathematically correct, whether the S26 and host use exactly equivalent kernels, or official Qwen3-ASR M-RoPE / token / numerical-logit parity.
- No new on-device performance or ASR accuracy claim is made. A working release requires successful GitHub Actions compilation and correct artifacts.

## Next action

Push the full v6.6.11 source to the Lab repo `main`; the host workflow produces `qwen-6.6.11-host-boundary-isolation-*`. Provide that ZIP for analysis and the subsequent release selection. A phone test is not warranted for this host-only change.
