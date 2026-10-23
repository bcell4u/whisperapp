> Historical audit report, written before GitHub run 108120053894. See `AUTO_CI_BUILD_FIX_6_7.md` for the newer ARM64 compiler failure, fix and automatic push-build change.

> ARCHIVED PRE-v6.7 AUDIT: references to `transcribenative` / transcribe.cpp describe the now-removed 1.7B runtime and are not active v6.7 tasks.

# Whisper Recorder v6.3.22 — source audit and targeted remediation

Audit date: 2026-10-07 (user-local). Input: `WhisperRecorder-v6.3.22-final-cleanup(1).zip`. Output: accompanying audited source ZIP and a `CHANGES.patch`. The app version is intentionally **not** advanced or renamed.

## Audit scope and limits

- Reviewed the GitHub Actions build/release/audit steps; root/module Gradle/Kotlin DSL configurations; native CMake build definitions; Java/Kotlin JNI declarations versus C++ symbols; Android manifest/resource references; NPU/OpenCL/model download paths; transcription UI; WAV capture; references to retired code; and APK-related packaging switches.
- Ran the repository's `scripts/verify-source.sh`, checked shell syntax and workflow YAML, checked XML/resource availability, checked 20 native JNI entry-point names, compiled key Kotlin parts with limited platform stubs, and executed 28 targeted host-side tests (13 existing tests, seven resumable-download integration cases, four restoration cases, and four transcript-render cases). The new tests exercise the edited source itself.
- **Could not run** `gradle testDebugUnitTest`, CMake/NDK, `assembleDebug`, `assembleRelease`, GitHub Actions, APK inspection, or device performance tests: this isolated runtime has no Android SDK/Gradle distribution and cannot download them. The GitHub build and release status remains **not verified**. Passing host-side tests does not prove an Android build succeeds.
- The project has large transitive native and Qualcomm/ONNX dependencies fetched at build time; source-level inspection cannot establish their linked size, binary ABI compatibility, on-device NPU/GPU behavior, or real final APK size.

## Resolved findings (implemented in the package)

| Priority | File(s) | Finding and remediation |
| --- | --- | --- |
| High | `gpu/ParakeetOpenClProcessClient.kt` | If `SharedMemory.mapReadWrite()` failed, the file descriptor could be leaked because `close()` was only in a later `finally`. Bound the entire operation to `SharedMemory.use { ... }`. |
| High | `models/ModelFileOps.kt` | Restoring an archived model deleted the previously valid destination **before** renaming the verified replacement. A crash or failed rename could lose the working model. Replaced with `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` and a same-directory replacement fallback if atomic move unsupported. |
| High | `audio/WavPcmRecorder.kt` | WAV header could be finalized while the capture thread was still alive. Added a second unblock/join path and prevent header patching on a still-active writer, with reliable cleanup. |
| Medium | `transcribenative/.../transcribe_jni.cpp` | A Java `FloatArray` obtained via `GetFloatArrayElements()` could stay pinned for an entire native inference. Copy promptly with `GetFloatArrayRegion()` into owned native memory before running inference. This trades temporary memory/copy overhead for safer JVM GC behavior. |
| Medium | `models/ModelFileOps.kt` | Resume logic lacked proper Content-Range offset validation and could discard or re-request already complete partial files. Validated HTTP 206 offsets; handle a complete partial without refetching; handle HTTP 416 with matching `bytes */size` when total is initially unknown; safely restart oversized downloads; enforce cancellation at additional boundaries. **Caller still must verify digest before installation.** |
| Medium | `models/ModelDownloadService.kt` | Notifications were rebuilt continuously at high transfer rates. Throttle progress updates by percent/status or 750 ms and check a final cancel signal before reporting completion. |
| Medium | `ui/TranscriptLazyContent.kt` | A very long transcript with only sparse timing cues could render a giant unbounded Compose Text block, even though ordinary untimed transcripts were chunked. Split long transcripts into ~2.8K-character blocks while preserving cue boundaries; added sparse/long-cue tests. |
| Low | Four Kotlin helpers | Removed in-project unreferenced `ShareManager.shareLocalFile`, `LocalAsrModelManager.removeLocal`, `NpuGraphContracts.requireById`, and `NpuPackageStore.validatedManifestText`. Do not infer all other public methods are dead: Android, Compose, JNI, AIDL and reflection create indirect call paths. |
| CI/performance | `.github/workflows/build.yml` | `testDebugUnitTest`, `assembleDebug` and `assembleRelease` now limit Gradle parallel workers to two to reduce out-of-memory risk during large native builds; native cache key accounts for all module CMake files and the adapter patch. |
| CI/performance | `app/.../CMakeLists.txt`, `transcribenative/.../CMakeLists.txt` | Share the same pinned `.native-deps` source cache as the Parakeet native module and skip unnecessary updates of cached sources. This reduces duplicate fetches, not native library size. |
| APK size | `app/build.gradle.kts` | Explicitly disable obsolete v1 APK signing for signed builds (minSdk 29 supports v2); R8 code/resource shrinking, arm64-only ABI, and compressed native packaging already existed and were retained. |
| APK/source size | `app/src/main/res/mipmap-*dpi/*.png` | Removed ten raster launcher fallbacks (9,110 uncompressed source bytes); adaptive launcher icons exist in `mipmap-anydpi-v26` and `-v33`, so they cover all supported devices (minSdk 29). Exact **APK** benefit is unknown: AAPT/resource shrinking may already have removed them. |

## Results verified locally

- `bash scripts/verify-source.sh`: **PASS**. App version still `6.3.22`, versionCode `83`.
- `bash -n scripts/verify-source.sh scripts/create-release-keystore.sh`: **PASS**.
- GitHub workflow YAML parsed; Android XML/resource reference consistency checked; 20 declared JNI export names matched native symbols. These are **static checks**, not full compilation/link verification.
- Seven HTTP download tests using a local HTTP server and the modified real `ModelFileOps.kt`: **PASS** (`200`, complete-partial reuse, good/bad `206`, complete `416`, forced restart, cancellation).
- Four `restoreDocument` tests: **PASS** (new, replace, validation failure retaining previous data, unavailable source retaining previous data).
- Four transcript render tests: **PASS** (two original and two new sparse/oversized cue cases).
- Thirteen pre-existing JVM-friendly pure-Kotlin tests across NPU formatting/performance, Parakeet preprocessing/TDT, segmentation, and transcript playback: **PASS**. `LocalAsrCatalogTest` and Android-dependent unit tests were **not** included in these host-only runs.
- `ParakeetOpenClProcessClient.kt` compiled against hand-written Android/AIDL API stubs: **PASS**. This is only a Kotlin type-check and does **not** validate Android behavior or the actual AIDL-generated interface.
- Native dependency tags (transcribe.cpp v0.1.3, parakeet.cpp v0.5.0, Khronos OpenCL v2026.05.29) were cross-checked against upstream public tag listings; the actual adapter patch and all transitive native builds were **not** run here.

## Source / size accounting

- Original unpacked file payload: **129 files, 791,763 bytes** (excluding ZIP directory entries).
- Patched source **before** including this audit report: **119 files, 785,047 bytes** (**10 fewer files and 6,716 fewer bytes, net**, despite added safety checks). The ten deleted PNGs account for 9,110 raw bytes; remaining source edits add roughly 2,394 bytes net.
- This is a **source package** reduction, not a measured APK shrinkage. APK bytes may be dominated by statically linked whisper/ggml/transcribe/Parakeet libraries and Qualcomm/ORT dependencies. The audited ZIP includes this report, so its total ZIP size will differ.
- No models are bundled in the app assets; downloading model weights remains the intended behavior.

## Remaining risks / follow-up required

1. **GitHub build not executed.** Trigger `workflow_dispatch` on the audited commit. Confirm that upstream clone, native adapter patch, CMake/NDK, AGP/R8, DEX marker validation and APK verification all pass. Download/network outages and third-party upstream changes can still cause builds to fail.
2. **Stale tracked source files can break CI.** The supplied `HANDOFF.md` warns that `app/src/main/java/com/example/whisperrecorder/ui/BenchmarkScreen.kt` must be absent. If deploying the ZIP over an existing Git checkout, **delete obsolete tracked files**; merely copying the ZIP over the repository does not remove old files. Run `bash scripts/verify-source.sh` in the actual committed checkout.
3. **Existing release tag collision.** The workflow deliberately refuses to publish a different signed commit under an existing immutable `v6.3.22` tag. If that tag already exists, a subsequent signed main-branch release must bump `version.properties` and keep `scripts/verify-source.sh`'s version assertions in sync. Do not delete/move established signed release tags to bypass this protection.
4. **Real APK size unknown.** After GitHub builds, use `build-proof.txt` (already logs total APK bytes, native/Dex compressed and uncompressed breakdowns) and `apkanalyzer` to identify the largest `.so` files. Compare against a baseline APK built with the same signing/build mode and commit inputs. Do not assume the 9.1 KB asset deletion or v1-signing setting changes size one-for-one.
5. **WAV capture edge case.** `WavPcmRecorder.stop()` can wait up to ~4 seconds if a device driver keeps reads blocked; stop is invoked by a service. Device-stress-test rapid stop/pause/resume, process death, microphone revocation, low storage, and very long recordings. A background shutdown design would be safer for ANR avoidance but requires broader lifecycle refactoring.
6. **Native heap use tradeoff.** JNI now creates a native float copy of the PCM chunk to avoid prolonged JVM array pinning. Peak native memory may rise while inference is running. Benchmark long recordings and consider chunk-streaming if native APIs permit it.
7. **Large native dependency duplication.** Three separate module builds may each link parts of ggml and a C++ runtime. Deduplication or building reusable shared cores may substantially reduce APK size but carries major symbol/ABI/regression risk. Only pursue with reproducible NDK builds and speech accuracy/performance tests on target devices.
8. **NPU/HTP and OpenCL paths remain hardware-dependent.** Validate Qualcomm library resolution, model SHA/size checks, FastRPC permissions, service binder deaths/reconnect, memory pressure, cancellation, and UI responsiveness on devices. No device tests were possible.
9. **Test coverage is incomplete.** Existing tests cover some NPU inference control logic but not complete SAF restore/download services, native engine outputs, UI activity/Compose interactions, wake locks, media ingestion, lifecycle, or service notifications. Further tests using Android instrumented/Robolectric test runners would be beneficial.
10. **Package preservation.** This pass intentionally avoids altering downloaded model IDs, the accepted NPU decoding path, audio results, or release identities, to reduce regression risk. Source verifier includes version- and structure-specific guards that will need conscious maintenance on future version upgrades.

## Rebuild / verify checklist

```bash
bash scripts/verify-source.sh
gradle --no-daemon testDebugUnitTest --stacktrace --build-cache --max-workers=2
gradle --no-daemon assembleDebug --stacktrace --build-cache --max-workers=2
# For signed production builds configure approved signing secrets then:
gradle --no-daemon assembleRelease --stacktrace --build-cache --max-workers=2
```

Compare GitHub Actions' post-build `Audit built APK` step and `build-proof.txt` for the resulting artifact, including expected JNI `.so` libraries, APK signature, variant label, and model assets. Treat release validation as incomplete until a real runner and device test finish successfully.

---

## GitHub Actions compiler follow-up (2026-10-08)

The next GitHub build (`logs_102185496559.zip`) exposed pre-existing Kotlin compilation failures in the NPU worker (`configureDspSearchPath`, `deviceJson`, `validateSessionNames`, `elapsedMs`, `encoderPaddedMaxAbs`) and one API mismatch in `AsrSession.kt` (`transcribePreparedChunk` was called with two arguments instead of one). All reported references/call signatures are repaired in the updated source archive. A structural NPU interface verifier now runs as part of `scripts/verify-source.sh`; five NPU encoder padding regression tests have been added. Local source verification and isolated Kotlin compilation/tests passed. **A complete Android GitHub CI build, native link and device inference still must be verified.** See `CI_BUILD_FIX_REPORT.md` for the detailed log-to-fix mapping.
