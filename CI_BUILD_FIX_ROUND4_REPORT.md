# Whisper Recorder v6.3.22 - CI post-build APK audit correction (round 4)

## Uploaded run and verified result

GitHub Actions log: `logs_102190112209.zip`, timestamps 2026-10-08 UTC.

This is **not** a Kotlin, native, or APK packaging failure. GitHub Actions successfully completed:

- `Run JVM unit tests`: `BUILD SUCCESSFUL in 1m 15s` (51 actionable tasks).
- `Build APK from source`: signed release `assembleRelease`, `BUILD SUCCESSFUL in 7m 16s` (135 actionable tasks).
- Release APK created at `app/build/outputs/apk/release/app-release.apk`.
- APK certificate verification: Android APK Signature Scheme **v2: true**, and expected permanent release certificate matched.
- JNI libraries found: `libwhisper_jni.so`, `libtranscribe_cpp_jni.so`, `libparakeet_jni.so` for arm64-v8a.

The workflow then failed in **Audit built APK** with:

`APK DEX audit is missing expected ASCII marker: parakeet.cpp v0.5.0 OpenCL / Adreno`

The obsolete expected text does not exist as a runtime diagnostic in production source. The real worker in `app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClWorkerService.kt` produces a diagnostic with prefix:

`parakeet.cpp 0.5.0 OpenCL device=`

The stale APK string check is the only error reported. Because the audit step failed, subsequent **Fingerprint APK**, **Upload installable APK and build proof**, and **Publish GitHub Release** steps were not executed. The latest log proves APK generation and signing, **not** that a distributable artifact was uploaded.

## Changes in this source ZIP

1. `.github/workflows/build.yml`: replaced the non-existent marker check with `require_dex_string 'parakeet.cpp 0.5.0 OpenCL device='`, corresponding to the actual Kotlin worker diagnostic. Kept all other APK inspections and security checks (signing fingerprint, required JNI libraries, vendor OpenCL declaration, model-weight exclusions) unchanged.
2. `scripts/verify-source.sh`: coupled the APK-audit marker to its defining runtime source, and rejects reintroduction of the obsolete assertion.
3. Added this report. There are **no** changes to app Kotlin code, NPU / OpenCL / Whisper native code, resources, model catalog, dependency versions, or Android build settings. APK bytes produced from identical toolchains/source should therefore be unchanged by this correction.

## Local checks

- `bash scripts/verify-source.sh`: PASSED (`v6.3.22 source verification PASSED`).
- `bash -n scripts/verify-source.sh`: PASSED.
- GitHub workflow YAML parsed successfully using Ruby's YAML parser.
- Negative guard test: reintroducing the old assertion causes `verify-source.sh` to fail as designed; restoring the correct assertion passes.
- Packaging checks: ZIP integrity and Git patch applicability verified separately.

## Remaining verification

A new GitHub build must run `Audit built APK` on its compiled and shrunk DEX. The previous run proves the production worker source has the diagnostic prefix, but its presence in the final R8-processed DEX could not be inspected locally without the APK itself. Once the audit passes, check Fingerprint, Upload, and Publish steps. GitHub's release-tag safety check may intentionally require a version bump if `v6.3.22` already points to another commit; do not remove the protection to silence such a failure. On-device inference/recording stability and compiled APK size still require the actual APK and a supported Android device.

## Non-blocking warnings observed

Six Kotlin warnings (unused parameter, deprecated memory trim constant, redundant `lateinit`/initializer, unused UI parameter) and several upstream native-library compiler warnings remain; none prevented the successful release compilation. No app-runtime optimization was attempted in this fix because the uploaded failure was confined to a CI assertion.
