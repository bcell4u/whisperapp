# Whisper Recorder v6.3.22 — GitHub Actions build-log repair

## Source of diagnosis

Uploaded `logs_102185496559.zip`, GitHub Actions run 102185496559. The job reached Gradle's `:app:compileDebugKotlin` task and failed at 2026-10-08 02:40:44 UTC. Setup, source verification, Omi adapter validation, SDK installation and several resource-processing tasks had passed. Native compilation, APK packaging and signing **were not reached** in this run.

### Kotlin compiler errors in the uploaded log

| File:line in failing source | Error | Fix |
|---|---|---|
| `NpuRuntimeWorkerService.kt:266` | `configureDspSearchPath` unresolved | Restored a DSP library path initializer before loading ORT-QNN. Includes packaged native libraries and standard vendor DSP directories, preserving previously configured paths. |
| `NpuRuntimeWorkerService.kt:294` | `deviceJson` unresolved and subsequent map type-inference error | Restored JSON serialization for `OrtEpDevice` using public ORT Java API getters. |
| `NpuRuntimeWorkerService.kt:593` | `validateSessionNames` unresolved | Restored explicit input/output names validation against the NPU graph contract. |
| `NpuRuntimeWorkerService.kt:782,799,810` | `elapsedMs` unresolved | Restored elapsedRealtimeNanos-based conversion to milliseconds. |
| `NpuRuntimeWorkerService.kt:784` | `encoderPaddedMaxAbs` unresolved | Added a testable, channel-major, output-shape-checked padding magnitude helper. |
| `AsrSession.kt:155` | Too many arguments to `NpuRuntimeClient.transcribePreparedChunk(preparedId: Long)` | Removed obsolete `"auto"` argument; caller now agrees with the client and AIDL worker method. |

The above unresolved references and mismatched call were also in the original v6.3.22 package; they were not caused by the first audited ZIP. Static source-verification checks had not detected them.

## Regression protection

- Added `scripts/verify-npu-interfaces.sh`, invoked by the existing `scripts/verify-source.sh` so GitHub catches these missing names and the incompatible prepared-chunk call earlier in the workflow. These checks are **structural safeguards**, not a replacement for Kotlin compilation.
- Added `NpuEncoderPaddingTest.kt` with five tests: verifies valid/padded boundaries, correct channel-major indexing, a zero-padding case, out-of-range frame count rejection, and invalid output size rejection.
- Kept the existing `scripts/verify-source.sh` line-count budget intact (259 lines); no native decoding, tokenizer, model or other application functionality was removed.

## Validation performed locally

- `bash scripts/verify-source.sh`: **PASS** (including NPU interface safeguards).
- Kotlin 1.9.0 independent compilation of `NpuGraphContracts.kt` plus the new test class with a minimal local JUnit-compatible test stub: **PASS**.
- Execution of all five new padding tests through a local harness: **5/5 PASS**.
- Git diff whitespace check, patch application check, and ZIP integrity test: see packaged artifact verification.

### Remaining verification required

**The complete Gradle/Android build could not run here** (Android SDK/NDK and Gradle build toolchain are not available). Do not assume native module linking, APK build or device-side Qualcomm NPU loading is fixed until GitHub runs again. The uploaded log revealed no subsequent errors because the workflow stopped at Kotlin compilation. CI deprecation warnings concerning Node.js 20 on third-party GitHub Actions are nonfatal and unrelated to this failure.

## Re-run instructions

Upload/commit the complete updated source ZIP, including the two added regression files, and re-run `.github/workflows/build.yml`. Verify `Run JVM unit tests`, APK creation, native build, and app startup/inference on a supported QNN/HTP device. Do not overlay only select files onto an unknown repository revision; use the ZIP or apply the accompanying patch to the matching prior audited source.

## Follow-up: GitHub Actions run 102188263384

This follow-up run reached `:app:compileDebugKotlin` and found two generic-map mismatches in `NpuRuntimeWorkerService.kt:284-285`. These are resolved with an explicit `Map<String, String>` to `JSONObject` iteration helper; SDK cache restoration is also disabled because the runner-owned installation repeatedly rejects tar metadata writes. Full details and test scope are in `CI_BUILD_FIX_ROUND3_REPORT.md`.
