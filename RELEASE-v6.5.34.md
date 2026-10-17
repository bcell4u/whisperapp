# WhisperRecorder v6.5.34 — Diagnostics foundation

Development base: v6.5.22 stable source; experimental Qwen v6.5.32 used as reference only.

## Included

- Automatically creates a bounded private ZIP report when transcription ends, fails or is cancelled.
- Captures structured per-segment inference durations and completion totals; exception classes and sanitized stack frame symbols.
- Captures device, thermal and process PSS snapshot at report generation time.
- Settings > Diagnostics supports ZIP creation, Android sharing, individual deletion and delete-all confirmation.
- Max 8 diagnostic ZIPs and 32 MiB combined. Report files are never written to model or recording folders.
- On app restart, stale unfinished markers (older than five minutes) can yield an interruption report.
- Carry forward the previously tested v6.5.29 non-regressing progress policy for overlapping audio segments.
- Existing diagnostic text log retained, but is **not** included in auto-share ZIP because it can contain recording names, paths or other private data.

## Deliberately not included yet

- Qwen3-ASR 0.6B hybrid backend migration, FP16 embedding candidate, decoder optimizations or quantization.
- New Qualcomm SDK, cloud compile, model downloads, or device staging.
- Verified Android Gradle APK or physical-phone performance numbers.

## Validation required

1. CI source gate and complete Android Gradle compile with pinned native dependencies.
   **Do not install the v6.5.34 release build over v6.5.32 on a production phone:**
   this is the clean baseline and intentionally does not yet include the experimental
   Qwen3-ASR-0.6B hybrid backend. For isolated smoke tests, install the debug
   `.dev` application ID alongside the existing release; app-private storage will
   be separate. Preserve the production model archives and app data.
2. Upgrade over installed app (do not uninstall or clear storage), check all existing model cards.
3. Transcribe with one short synthetic clip and verify ZIP (including no sensitive text).
4. Cancel and force a controlled failure; verify ZIPs, sharing, deletion and non-destructive retention.
5. Verify Parakeet/Omi performance and progress behavior on the S26.

## Next release

After the diagnostic and clean-app baseline pass, integrate the already verified Qwen
FP16 QNN encoder and a decoder controlled by measured S26 profiling, without reusing
experimental file-migration code blindly.

## GitHub CI Kotlin-test migration fix

The v6.5.33 GitHub run `105873388122` failed at `:app:compileDebugUnitTestKotlin` because an older `app/src/test/java/com/example/whisperrecorder/transcribe/Qwen3HybridPolicyTest.kt` still referenced `AsrEngine.QWEN3_HYBRID`, which is absent in the clean v6.5.22 baseline. **This release includes a replacement at the same path.** Overwrite that exact file when applying changed files to a previously experimental repository. No experimental Qwen inference backend has been added to this diagnostic-only release.

`version.properties` now declares 6.5.34 / 65034. A full Gradle/GitHub Actions build is still required before using the APK.
