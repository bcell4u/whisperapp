# Qwen GGUF Lab v6.6.0 — engineering validation receipt

Date: 2026-10-19 (source handoff lineage); experiment based on original `6.5.22`.

## Offline checks performed in this assistant environment

| Test | Result | Limitations |
|---|---|---|
| Companion kit baseline ZIP SHA-256 | PASS — matches original handoff (`08f3475c...b3f8`) | Does not verify builds |
| Separate source copy and baseline diff | PASS — underlying v6.5.22 main files preserved except explicit Lab deltas | Cannot guarantee third-party runtime compatibility |
| `python3 scripts/verify-lab-source.py` | PASS — version 6.6.0/code 66000, application-ID separation, QNN/decoder markers, private manifest | Static inspection only |
| XML parse across entire `app/src/main` tree | PASS | Resource compilation still requires Android Gradle |
| GitHub Actions YAML parse | PASS — main push + manual dispatch; GitHub prerelease after successful validation | No actual Actions run here |
| `bash -n` on all workflow shell `run` steps | PASS | Does not exercise network operations or native tools |
| Native decoder CMake/C++ source marker inspection | PASS — ARM64 dotprod/512-context/4-thread/private token protocol present | Native binary not compiled here |
| Pure Kotlin `Qwen3Mel` JVM smoke | PASS — partial 7 s PCM produces finite 128x1200 mel tensor | Synthetic golden parity fixture **not included**; not a numerical reference comparison |
| Original 6.5.22 GitHub production publishing workflow removal | PASS — Lab only includes separate 6.6 prerelease workflow | Original production project remains untouched |
| Production signing-key / credentials leak prevention | PASS — ZIP contains no `.jks` or user model files; signing secrets read at CI time | User must configure desired stable signing key |

## Evidence NOT collected — do not infer a pass

- **Android compile and APK:** Not run. No Android SDK/NDK/Gradle native dependency installation
  or authenticated GitHub Actions runner was available in this assistant container.
- **Native ARM64 executable and APK aapt/apksigner:** Not run. CI will build the pinned
  llama.cpp v0.6.0 `d812350` source and inspect ELF type, flags, package ID, signer,
  QNN backend libs and packaged decoder before publishing.
- **Phone runtime and real audio:** Not run. A single authorized Samsung S26 on-device test
  is required after a successful CI build. Android SELinux/exec compatibility, genuine
  QNN use and script-to-phone latency are not guaranteed by source or ELF inspections.
- **Audio/text accuracy:** Not established. Qwen3-ASR 4-axis position interpretation
  remains provisional; compare transcript privately without exporting it.
- **End-to-end speed/memory:** Not measured. Prior 16.90 s native decoder results from
  v0.4.96 are from the existing benchmark, *not* from this integrated APK.

## CI verification sequence and expected proof

1. Commit the source ZIP contents at repository root on `main` of the separate account.
2. `Qwen GGUF Lab 6.6 Android APK + Auto Release` runs automatically. Resolve any failed
   source, dependencies, native, Gradle, APK layout or signing gate **before** testing a phone.
3. A successful GitHub run will upload the debug APK, its SHA-256 and `BUILD_PROOF.txt`
   as Actions artifacts; only then will it attach identical files to a
   `v6.6.0-lab.*` prerelease.
4. If that APK can be installed and preflight passes on S26, import the user's own
   compatible QNN encoder files and Q8_0 GGUF. Choose the private ~220 s lecture
   using the Android audio picker, then run once. Inspect transcript on the phone.
   Record only non-sensitive counts, timings, 1 model load, 19 or 20 EOS segments,
   peak RSS, NPU proof, and cleanup result. No repeated CPU/thermal sweeps.

## Unblockers

- **First external unblocker:** successful GitHub Actions build receipt (or error logs
  from its first failing compilation/verification step). The workflow may need
  additional dependency/ABI fixes; offline syntax gates cannot anticipate all of them.
- **Second external unblocker:** one eligible S26 with user-owned QNN encoder package,
  exact Q8_0 GGUF and private audio. Model weights and audio are *not* in this ZIP.
- If Android prevents launching a bundled PIE in the nativeLibraryDir, the correct
  fix is a native JNI conversion/re-link with proper lifecycle, **not** a fake success,
  executable copy to writable storage, hidden CPU fallback or repeated benchmark sweep.
