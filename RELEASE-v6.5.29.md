# WhisperRecorder v6.5.29 — Qwen performance instrumentation & progress repair

## Observed on S26 (2026-10-11 log)

A 220.5-second recording finished in 113.5 seconds. The Qwen FP16 QNN encoder
reported about 86–107 ms per segment, whereas the CPU FP32 compute decoder
spent 2–6 seconds generating tokens on each of 20 segments. The first segment
spent an additional ~10 seconds beyond the reported decode time, consistent
with lazy loading of both ONNX decoder sessions. These are app measurements,
not independent DSP power or CPU thermal measurements. The phone heating is
consistent with prolonged autoregressive CPU compute, but temperatures were not
measured in the submitted log.

## Changes

* Every Qwen chunk now logs decoder model-load wall time separately from
  prefill/first-token inference and autoregressive step inference, step count,
  embedding I/O, argmax, token count, app worker CPU time, memory PSS, and
  Android thermal severity before and after inference. This metadata is
  aggregated into the existing exported diagnostic log, without tokens,
  transcript text, or model weights.
* The existing Qwen decoder remains CPU compute with two ORT intra-op threads.
  No unvalidated INT4/INT8 conversion, unsupported EP or compiler invocation.
  A confirmed NPU call remains mandatory for the encoder.
* The next PCM segment is prefetched concurrently with Qwen inference (as
  already done for working Parakeet/Omi code paths); this can reduce media
  decode stalls but is not expected to remove CPU autoregressive costs.
* Segment progress calculation is now monotonic across overlapping chunks;
  the app and notification bar never return to the earlier start timestamp
  of the next segment, and the heartbeat preserves determinate progress.
  100% is emitted only after the transcript has been saved.
* Qwen model package validation, private persistent storage, SHA-256 checks,
  EPContext, temporary-file cleanup and other model backends are not changed.

## Validation and what to send after device test

The new timing fields are diagnostic, not proof of a speedup. Test a short
(roughly 3 minute) audio sample and export Settings → Diagnostics logs. Look
for `decoderLoadMs`, `prefillMs`, `stepMs`, `decoderSteps`, `workerPssKb`,
`thermalStart`, and `thermalEnd` in each `inference complete` line.
Compare to the 2026-10-11 v6.5.28 baseline on the same recording. If the
step graph dominates, improving performance further requires a measured
CPU backend graph optimization or an independently validated QNN decoder,
not a mislabeled hybrid or a smaller on-disk model alone.

GitHub Actions APK build and on-device thermal/performance validation remain
outstanding in this source-only release.
