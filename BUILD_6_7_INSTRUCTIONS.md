# Build 6.7 and verify before replacing production

1. Copy the full source ZIP contents (or apply the v6.5.22→v6.7 patch) into your `whisperapp` Git checkout. Preserve signing secrets, the original installed app, and recordings.
2. Commit and push the v6.7 source to any branch. **GitHub Actions automatically starts the debug `.dev` APK build on push.** The **Actions → Build Android APK → Run workflow** button also supports optional manual runs; leave `build_variant=debug` (safe default). Only a manual `release` selection with signing secrets builds a production-package candidate. The audited debug `.dev` APK is automatically published as a **GitHub prerelease** after CI passes, with `build-proof.txt`. An explicit signed manual release build publishes the stable version tag if it does not already exist.
3. The workflow fetches pinned llama.cpp v0.6.0 and builds the ARM64 dotprod decoder, runs existing production source/JVM tests and the new Qwen production source gate, then builds Android. It produces a debug `.dev` APK by default even when release signing secrets exist. Choosing `build_variant=release` explicitly requires the original signing secrets and produces a signed production-package release candidate (not auto-published).
4. Download `WhisperRecorder-build-<run>` from Actions artifacts. Verify the APK, `build-proof.txt`, `android_lint_exit`, and the attached lint reports, then perform the UI, model download and S26 smoke tests in `INTEGRATION_AUDIT_6_7.md`.
5. **First use debug `.dev`** so v6.5.22 stays installed and untouched. Do not distribute or upgrade production until real CI and device checks pass.

The Qwen3-ASR 0.6B card downloads the ~805 MB GGUF decoder plus device-specific ~391 MB QNN context/tokenizer on demand. Model weights are not bundled with the APK. The QNN encoder is currently targeted at Snapdragon S26 model SM-S942U.


### Version 6.7.3 Git overlay and lint-reporting

If previous source updates were pasted over GitHub without deleting retired
files, execute `bash scripts/clean-retired-omi-diagnostics.sh` in the repository
root and **commit the deletions**. The updated cleaner targets exactly the old
Omi probe shell script, Kotlin contract, and Kotlin *test*; the actual Omi ASR
engine and normal Omi tests remain untouched. A successful source audit is
not a substitute for a completed Android APK build.


### v6.7.3 lint failure investigation

The 6.7.2 debug APK compiled, was APK-audited, and uploaded as Actions
`WhisperRecorder-build-34`. The Release job did not publish it because lint
returned exit 1. That log archive does **not** contain the captured Gradle
lint log, so the actual lint error is still unknown.

Use the run's **Artifacts** area to download `WhisperRecorder-build-34` and
inspect `android-lint-build.log` and `lint-results-debug.xml`. With this 6.7.3
source pushed, a new Actions run will also print the relevant lint issue IDs
and a bounded log excerpt directly in the Actions console. It still uploads the
APK and complete reports, then fails the build job if lint fails. The Release
job runs only after lint passes. **Do not force publication with a failed lint.**

### v6.7.4 permission lint fix

The uploaded `android-lint-build.log` reported **7 errors and 9 warnings** on
`:app:lintDebug`, but only printed the *first* finding, a `MissingPermission`
error at the AudioRecord constructor in `MicrophoneCapabilityProbe.kt` line 49.
The v6.7.4 code now checks `RECORD_AUDIO` at that constructor's path and
similarly at WAV and compressed recording entry points. No lint baseline or
suppression was added. A new CI run is required to determine whether the
remaining six errors persist.

Apply the `v6.7.3 -> v6.7.4` source update patch with
`git apply --check <patch-file>` followed by `git apply <patch-file>`, or
copy the changed-file ZIP's project-relative file structure into your checkout.
Do not add the ZIP's helper files to the repository. Keep the existing deleted
Omi diagnostic files deleted. Run `bash scripts/verify-source.sh` and
`python3 scripts/audit-qwen-production.py` before pushing. Every push builds
a debug `.dev` APK and will publish a debug prerelease only after tests,
Android lint, and APK audits pass.

To diagnose any outstanding lint findings, upload the complete
`app/build/reports/lint-results-debug.xml` from the GitHub Actions artifact;
`android-lint-build.log` alone prints only the first issue in this older run.

### v6.7.5 model-download user experience

Model download status now stays in each model card, including Qwen's 805 MB GGUF stage. After a successful, verified download, the foreground service selects newly installed Qwen and NPU packages. Whisper/local GPU downloads already select their model. On any failure or cancellation, do not claim selection. Pushing a commit automatically runs the debug build, then publishes a prerelease only if all CI gates including lint pass.
