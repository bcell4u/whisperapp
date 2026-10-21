# Whisper Recorder v6.3.22 - CI repair (GitHub Actions run 102188263384)

## Failure evidence

The uploaded `logs_102188263384.zip` shows GitHub Actions reached the `Run JVM unit tests` step and failed in `:app:compileDebugKotlin` at 2026-10-08 02:53:45 UTC. The Kotlin compiler reported **two** errors, both in `app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt`:

- Line 284: `JSONObject(device.epMetadata)` rejects ONNX Runtime's Java `Map<String, String>` where Android's `JSONObject(Map)` constructor requests raw `Map<Any?, Any?>`.
- Line 285: `JSONObject(device.epOptions)` has the identical generic type mismatch.

These two call sites were added during the prior repair of missing `deviceJson` and were not caught by structural source verification. The other previously reported unresolved Kotlin symbols and stale method call **do not recur in this log**. Kotlin compilation stops before the app unit tests can run; no native build, APK generation, signing, or device inference was reached.

## Source changes

1. **Fix both Kotlin compile errors.** Added a small `jsonStringMap(entries: Map<String, String>)` helper inside the worker, using `JSONObject().apply { entries.forEach { (key, value) -> put(key, value) } }`. Replaced the two generic-incompatible `JSONObject(Map)` call sites with this helper. Both property maps are preserved without unchecked casts or changes to QNN execution logic.
2. **Regression guards.** Extended `scripts/verify-npu-interfaces.sh` to require the typed helper and its two call sites, and reject reintroduction of both incompatible constructors. This script is already called by `scripts/verify-source.sh` in GitHub Actions. These guards are structural and do not substitute for Gradle compilation.
3. **Remove a broken, wasteful CI cache.** The `Restore Android build toolchain cache` action downloaded a 717 MiB archive and failed to restore it due to `tar: Cannot utime` / `Cannot change mode` permissions on pre-existing, runner-owned `/usr/local/lib/android/sdk` files. Removed only this invalid cache step. The existing `android-actions/setup-android@v4` and pinned `sdkmanager` dependency installation remain unchanged. Other native source/compiler caches are retained. Effect on total build time requires a subsequent GitHub run to measure.

## Local validation

- `bash scripts/verify-source.sh`: PASS.
- Reproduced the original compile problem using local Kotlin 1.9 + Java stub classes with the same problematic method signatures: compiler rejected `JSONObject(device.epMetadata)` with a map type mismatch.
- Compiled the **unmodified production `jsonStringMap` and `deviceJson` methods** against matching Java `JSONObject(Map<Object,Object>)` and `OrtEpDevice` stubs: PASS. Runtime harness verified six output fields, including the metadata and options values: **6/6 PASS**.
- YAML syntax, Git patch, source ZIP integrity: validated during packaging.

## What remains unverified

This environment lacks the Android SDK/Gradle toolchain and the proprietary native QNN dependency artifacts, so a complete Android build **was not executed locally**. The next GitHub Actions run is required to establish whether Kotlin compilation, JVM unit tests, native linking, APK packaging, APK signing, and device operation pass. The uploaded run contains no information about later-stage failures.

## How to apply

Replace the repo with the attached complete fixed source ZIP, or apply the accompanying patch to the exact preceding `WhisperRecorder-v6.3.22-CI-build-fixed-source.zip` tree. Re-run `.github/workflows/build.yml` and check `Run JVM unit tests` and `Build APK from source` through completion.
