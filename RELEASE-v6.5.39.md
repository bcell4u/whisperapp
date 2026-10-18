# WhisperRecorder v6.5.39 — Qwen worker configuration and decoder memory investigation

Base: v6.5.38. Android versionCode=65039; development flavor shows 6.5.39-dev.

## Included

1. The Qwen session snapshots decoder settings in the main process and delivers them to the dedicated Qwen worker through the existing AIDL `prepare(configJson)` call. The worker validates, acknowledges and applies the exact thread, embedding and CPU-arena settings. Disagreement fails explicitly; no silent stale preferences.
2. Retires experimental reusable step-buffer mode following the October 17 S26 A/B tests (no demonstrated performance or PSS benefit). This cannot be re-enabled from the model card, even if the old SharedPreferences key was left ON.
3. Keeps the working four-thread CPU hybrid available. Adds a single **optional** ORT CPU-arena OFF test (default ON) using the supported Java SessionOptions API. This may reduce allocator memory retention but can hurt latency; it is NOT a weight-sharing implementation and is not guaranteed to reduce 5.9 GB PSS.
4. Adds before/after per-session native/Dalvik/other PSS, total PSS deltas, private dirty and individual session-load timing to the existing automatic `Whisperapp/Logs` diagnostic ZIPs. This is to classify why init + step ONNX Runtime sessions use so much memory before attempting a much riskier decoder rearchitecture.
5. Applies severe-thermal next-run fallback in the main process, not by mutating worker-local SharedPreferences. Existing QNN encoder, model files, migration and backend isolation unchanged.

## One-build S26 test plan

Install 6.5.39-dev as an update, no uninstall / data clear. Use the same installed Stage I model and leave K2A embeddings OFF. Record automatic ZIP for:

A. Four threads, CPU arena ON (baseline): same 220.534 s recording; this validates new cross-process handoff and memory breakdown.
B. Four threads, CPU arena OFF (experimental): same recording after comparable cool/idle state. Compare completed transcript text, time, session PSS, provider placement and cleanup.

Stop the arena experiment if memory does not materially decrease, throughput deteriorates, output differs or stability degrades. Do not assume reduced arena overhead shares model weights. No need to repeat the retired step-buffer experiment.

## Verification scope

Source checks, isolated JVM verification and archive integrity are local checks. GitHub Actions full Gradle build and actual S26 results are required before any performance or memory claim.
