# WhisperRecorder v6.5.32 — Shared Qwen Storage + Controlled Speed Trial

- **The only authoritative permanent model archive** remains the user-selected `Whisperapp/Models/qwen3-asr-0.6b/` (Stage I v3, or existing v2/v1).
- Qwen now attempts to hold **seekable SAF file descriptors** for ONNX graphs, the FP16 shared external weight file, tokenizer, embedding file and compiled QNN encoder. Tiny symlinks in a Qwen-owned cache directory reference `/proc/self/fd` while the worker is running. No full permanent weight duplication for successful bridge mode.
- If direct loading fails (Android DocumentsProvider or ONNX/QNN restrictions), the app **fails over safely** to an already verified private working copy, then to a **temporary hash-verified, offline materialization** if needed. Temporary staging is deleted on worker close; interrupted runs are cleaned at next startup. Never deletes recordings, other model families or SAF archives.
- New **Reclaim duplicate private Qwen weights** action requires both a successful real transcription in direct shared-descriptor mode and SHA-256 verification of the visible archive. It never runs automatically. Existing private copies are preserved until proven safe.
- New CPU decoder A/B profile toggle: balanced (2 ORT intra-op threads, default) vs experimental speed (4 threads). Both disable idle ORT thread-pool spinning and keep CPU decoder precision/graphs unchanged. Actual speedup and thermal impact MUST be measured on S26; 4 threads could be slower or hotter.
- Logs now expose `qwenStorage`, storage fallback reason, decoder thread count, token count, prefill/step time and thermal state. Keep QNN encoder CPU fallback disabled. No new QNN compilation or model redownload.
- Parakeet NPU, Omi NPU, other CPU/GPU backends and the global progress fix are unchanged.

## Validation boundary
Source checks and local regressions are not evidence that SAF symlink/proc-FD loading works on S26. Install over the existing app. First test a short 3-minute recording with 2 threads and verify `qwenStorage=saf-proc-fd`; then compare a repeat with 4 threads. **Do not reclaim private copies until the direct shared-folder path has proven itself on your phone.** If it reports `previous-private-runtime` or `saf-transient-verified`, send the log first.
