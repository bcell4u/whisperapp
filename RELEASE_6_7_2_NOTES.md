# WhisperRecorder 6.7.2 — CI orphaned test cleanup

## Build evidence

GitHub Actions run **108138936323** (October 23, 2026 UTC) passed the
production source verifier and Qwen static audit, validated the Omi GPU
adapter, and compiled/linked the pinned **ARM64 llama.cpp dotprod decoder**.
The next stage, `gradle testDebugUnitTest`, failed at
`:app:compileDebugUnitTestKotlin` because a **tracked leftover test**
`app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt`
still references `OmiNpuProbeContracts` after its experimental diagnostics code
was deliberately removed. This is not an ASR decoder failure.

## Changes

- Bump releaseVersion/versionName to **6.7.2**, versionCode to **67002**.
- Add the exact retired test file to the existing CI checkout cleanup script,
  before the source verification and Kotlin unit-test compilation stages.
- Strengthen source audit: reject the old test and any remaining code/test
  references to the retired diagnostic type. Do not weaken the real Omi
  runtime or its production tests.
- Update versioned build documentation, remove obsolete diagnostic usage
  instructions from the README/NPU guides, and include a deletion manifest.
- Preserve GitHub auto-build on push; verified debug APKs are published as
  uniquely tagged prereleases **only after** all required gates pass.

## Important GitHub checkout cleanup

After copying the changed-files ZIP into the Git checkout root, run:

```bash
bash scripts/clean-retired-omi-diagnostics.sh
git status --short
git add -A
git commit -m "WhisperRecorder 6.7.2 orphaned test cleanup"
git push
```

The removed file is a retired diagnostic *test*; it is not needed by the Omi
transcription engines. A ZIP file cannot delete Git-tracked files automatically.

## Status

Native llama.cpp compilation **passed in the uploaded run**, but the workflow
failed before assembling an APK. The 6.7.2 cleanup is a source correction; its
Android build, APK audit, GitHub Release publication and S26 end-to-end behavior
are **not yet confirmed**.
