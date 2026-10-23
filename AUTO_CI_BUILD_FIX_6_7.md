# WhisperRecorder 6.7.0 — automatic GitHub Actions debug builds and NDK compiler repair

## Uploaded CI run investigated

GitHub Actions run **108120053894** (`logs_108120053894.zip`) completed the 27 preexisting Qwen source checks and reached the ARM64 llama.cpp dotprod decoder native build. At `[71/76]`, clang++ failed compiling `native/qwen_bridge/qwen_asr_decode.cpp`, line 20, due to `HWCAP_ASIMDDP` being declared as a C++ constant while Android NDK's `<asm/hwcap.h>` already supplies it as a preprocessor macro. The resulting `expected unqualified-id` and `expected ')'` errors stopped the native build before Android Gradle unit tests, APK assembly, post-build lint and packaging. **The uploaded run did not produce a verified APK.** CMake deprecation warnings in the same log were not the stopping failure.

## Repairs in this source revision

1. `native/qwen_bridge/qwen_asr_decode.cpp` explicitly includes `<asm/hwcap.h>` for AArch64 and uses a guarded `#ifndef HWCAP_ASIMDDP` fallback. No constant is redeclared over the NDK macro. The actual dotprod CPU-capability check, llama.cpp model parameters and four decoder threads are unchanged.
2. `.github/workflows/build.yml` runs **automatically on every branch push** via `on.push.branches: ['**']`. Manual `workflow_dispatch` remains available. Every automatic run builds **debug `.dev`** regardless of configured signing secrets. Signing setup and release compilation are additionally gated by `github.event_name == 'workflow_dispatch'` and an explicit `release` selection. No automatic release, tag or installation is added.
3. `scripts/audit-qwen-production.py` now checks automatic push triggering, manual-only release eligibility, and the NDK macro safeguard. Existing guards still prohibit the legacy 1.7B Qwen card and `transcribe.cpp` native module.
4. Build and signing documentation is updated to explain the automatic debug push workflow and the failed native build evidence.

## Checks completed in the packaging environment

- Production source verifier: **PASS**.
- Qwen-specific static audit: **29/29 PASS**.
- GitHub Actions YAML syntax/trigger parsing: **PASS** (`push` all branches and `workflow_dispatch`).
- Shell syntax and Python source compilation: **PASS**.
- Host C++ macro-compatibility excerpt: **PASS** with the macro defined in advance (NDK-like) and with the fallback path (not an NDK/llama.cpp compile).
- Android app source: byte-for-byte unchanged relative to the previous llama.cpp-only candidate.
- ZIP integrity and patch reconstruction against the pristine production 6.5.22 source: verified during packaging.

## Still requires the next actual GitHub run and S26 smoke test

The exact Android NDK 27.0.12077973 compiler/linker, llama.cpp native static linking, Gradle/AIDL/Kotlin compilation, app native-library packaging, device-specific QNN HTP runtime and ProcessBuilder execution of the ELF under Android policy have **not** been successfully retested in this environment. The next automatic push build is the required verification. Check the artifact, native `libwrqwen06.so`, debug app ID, `build-proof.txt`, lint and full test failures before installing side-by-side with the existing production app. Do not infer runtime performance from source checks.

This package contains **Qwen3-ASR 0.6B QNN + llama.cpp only**; the obsolete 1.7B Qwen model card and transcribe.cpp route remain removed. The historical Lab probes/experimental diagnosis series were not reintroduced.
