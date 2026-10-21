# WhisperRecorder Qwen GGUF Lab 6.6.0 — GitHub prerelease source candidate

**Build status: NOT COMPILED in this handoff.** Local static gates pass; Android Gradle, native
ARM64 link, APK signing/aapt verification, real S26 QNN runtime and transcription remain **UNTESTED**
until GitHub Actions succeeds and a single consented phone run is reviewed. This is not a
production-ready app or a tested APK.

## Baseline and identity

- Clean **whisperapp-6.5.22.zip** is the original and sole application baseline, SHA-256
  `08f3475cbb697f9e9dc4c4dccba728dfa4ddeae8242fcdbaa077f07e3b26b3f8`.
- This lab sets `releaseVersion=6.6.0`, `versionName=6.6.0`, `versionCode=66000` in
  the same original `version.properties` file. The Android debug APK displays
  `6.6.0-dev`. Its package ID is `com.example.whisperrecorder.qwengguflab.dev`,
  which cannot overwrite `com.example.whisperrecorder` even if signed with the same key.
- Main production UI/recording/transcription classes and transcribenative/parakeetnative
  source remain based on 6.5.22. Only the Lab activity is launchable in this variant;
  its files, service process (`:qwenlab`), FileProvider authority and cache are separate.
- The first attempt retains the *benchmarked* 4-thread ARM64 dot-product GGUF decoder
  v0.4.96 C++ source, pinned `llama.cpp v0.6.0` (`d812350`). It uses a 512-token context,
  one model load for up to 20 chunks, and per-chunk KV reset. **Its 4-axis M-RoPE
  correctness is provisional**; benchmark performance must not be presented as
  integrated-app time or as clinical transcription accuracy.
- Encoder components (mel, tokenizer, QNN contract) are specifically taken from the
  v6.5.43 QNN donor, while ONNX CPU decoding is *not* used for this Lab.

## Publishing an APK from the separate GitHub account

1. Create an **empty repository** under the separate GitHub account, and upload or
   commit **the contents of this directory to repository root** on the `main` branch.
   Do not nest everything inside a second folder. Do not copy this repository's
   original 6.5.22 `.github/workflows/build.yml`: it is purposely absent.
   **Already using an existing GitHub repository?** The old `build.yml` must be
   removed **from that repository**; merely uploading the new source ZIP does not
   delete files already tracked by Git. Run `bash scripts/activate-lab-workflow.sh`
   after copying the Lab source, or delete `.github/workflows/build.yml` through
   the GitHub web editor. See `LAB_GITHUB_MIGRATION.md` for the exact one-file fix.
2. Make sure repository Actions have permission to create Releases: GitHub
   Settings -> Actions -> General -> Workflow permissions -> **Read and write**
   (or grant equivalent permissions through the organization's policy). The
   workflow itself sets `permissions: contents: write`.
3. **Each push to `main`** automatically triggers
   `.github/workflows/qwen-gguf-lab-6.6-autorelease.yml`. It downloads pinned
   build tools, compiles the ARM64 dot-product bridge and the Android debug APK,
   verifies APK package ID, version, native binaries and signing, and uploads
   an Actions artifact. **Only if all checks pass**, it creates a **GitHub
   prerelease** and attaches the APK, `SHA256SUMS.txt` and `BUILD_PROOF.txt`.
   Example distinct tag: `v6.6.0-lab.42.1.g123456789a`. A production `v6.6.0`
   tag is never consumed, overwritten or reused by this Lab workflow.
4. Builds also support manual **Run workflow** dispatch on `main`. No production
   signing key, recordings, GGUF, QNN model package, or app-private data should
   be committed or uploaded in the source repository.

## Signing compatibility with the 6.5.22 project

The workflow understands **exactly the existing v6.5.22 secret names**:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`, `ANDROID_SIGNING_CERT_SHA256`.

If all are available, CI reconstructs the key in the temporary runner, verifies
its certificate SHA-256, and signs the isolated debug APK using the **same
certificate**. Never include a `.jks` file in the ZIP or Git history. It is
fine to use a new stable experimental key in the separate account; doing so
will not force any change to the production repository.

If **none** are supplied, CI uses its ephemeral debug key instead. Such APKs
are installable but **cannot reliably update each other**, since GitHub runner
signing certificates may change between runs; uninstall the older Lab first.
If the keystore secret is present but the other signing secrets are incomplete,
the CI run **fails** rather than silently falling back. The APK verifier checks
the actual signer against the expected fingerprint whenever a stable key is used.

**Moving back to production later:** merely moving a Lab APK to the production
GitHub Releases page does **not** make it an update of your production Android
app, because Android matches BOTH package ID and signing certificate. To offer
an in-place upgrade from v6.5.22, port qualified changes into the production
`com.example.whisperrecorder` application ID, build a `6.6.0` APK with code
`66000` using the **permanent production keystore**, and use production's normal
`v6.6.0` tag and workflow. Changing the package ID or key after installation
requires a one-time uninstall/reinstall and does not automatically migrate data.

## Private files needed on the phone (not included here)

- An exported **Qwen3-ASR-0.6B QNN/HTP encoder package** compatible with the S26
  QAIRT 2.50 context and 156x1024 `float32` encoder outputs. Import an extracted
  folder or ZIP using the Lab pickers. Required basenames are:
  `encoder-qnn-fp16.bin`, `tokenizer.json`,
  `golden-synthetic-pcm-12s-f32.raw` (768000 bytes) and
  `golden-synthetic-mel-12s-f32.raw` (614400 bytes). An optional matching
  `model-manifest.json` validates file SHA-256 values if supplied.
- The matching **Qwen3-ASR-0.6B Q8_0 decoder GGUF** for the above encoder,
  imported into Lab-private model storage. This is *not* bundled in the APK.
- The user's own roughly 220-second audio recording chosen in Android's picker.
  The app uses bounded 12-second streaming audio chunks and enforces max 20.

The model import, audio, features, runtime data and transcript are never
copied out of the Lab app or into a production installation. The Lab displays
private decoded text on screen and suppresses native stderr and private paths
from errors. The only cleanup recursively deletes paths known to be inside
this Lab's **own private storage/cache**. Uninstalling Lab deletes its models;
the user's original picker media remains untouched.

## Outstanding qualification gates and limitations

- A GitHub Actions green build is needed to confirm Gradle dependencies, Kotlin,
  AIDL, native compilation, ELF packaging and full APK aapt/signing checks.
  Local static checks do **not** demonstrate any of these.
- Packaging an Android PIE executable under the `libwrqwengguflab.so` name is
  an **experimental Android compatibility technique**, not JNI. The app checks
  `--probe-info` before inference and will fail closed if it cannot execute.
  Native library extraction, SELinux and `exec` permission **must** be checked
  on the actual S26. If the system rejects this, replace with a correctly
  linked JNI service; do not infer permission from the ZIP or ELF build alone.
- Actual QNN/HTP availability and vendor library compatibility are still
  device-dependent. The ORT-QNN session explicitly sets
  `session.disable_cpu_ep_fallback=1` and requires real 156x1024 output.
- The original pinned Kotlin mel frontend is computationally expensive, and
  source benchmarks do **not** include the end-to-end Android preprocess / GUI
  path; measure this only in the first useful phone test, not another CPU sweep.
- Native 4-axis M-RoPE mapping and decoded transcript accuracy remain
  provisional even if the APK compiles and QNN executes.
- Never publish personal clinical recordings or transcripts into GitHub Actions,
  logs, Issues or release assets. A single on-device run with only non-sensitive
  performance receipts is enough before deciding on any further change.

## Locally available checks

```bash
python3 scripts/verify-lab-source.py
python3 -m compileall -q scripts/verify-lab-source.py
```

The old `scripts/verify-source.sh` belongs to **production** and expects its
`build.yml` production release workflow. For the isolated Lab, use the new
verifier rather than editing or bypassing production's source verifier.
