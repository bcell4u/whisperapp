# WhisperRecorder Qwen GGUF Lab 6.6.0 - Kotlin source recovery

## Actual GitHub failure

GitHub run `107118080094` reached `:app:compileDebugKotlin`, after the
ARM64 dot-product bridge had compiled, and reported these missing UI
symbols: `PromptsScreen`, `ModelsScreen`, `AccelerationNoticeDialog`,
`LocalAsrModelCard` (nine Kotlin unresolved-reference diagnostics).
The complete 6.6.0 source ZIP already had these files; the repository
checkout did not. The build did **not** produce an APK or a GitHub Release.

## Smallest reliable repair

Merge the contents of `WhisperRecorder-Lab-v6.6.0-Kotlin-UI-Fix.zip` into the
repository **root**, preserving directories. Commit the complete
`app/src/main/java/com/example/whisperrecorder/ui/models/` and
`.../ui/prompts/` trees, and updated `scripts/verify-lab-source.py`.
The updated verifier checks all 121 expected app Kotlin and AIDL files at
once, plus the previous manifest/resources/version/isolation gates.

From your cloned GitHub repository root, use:

```bash
python3 scripts/verify-lab-source.py
# Confirm new files are tracked; -A stages required previously omitted files:
git add -A app/src/main/java/com/example/whisperrecorder/ui/models \
           app/src/main/java/com/example/whisperrecorder/ui/prompts \
           scripts/verify-lab-source.py LAB_6_6_KOTLIN_RECOVERY.md
git status --short
git commit -m "Restore 6.6.0 Models and Prompts Kotlin UI sources"
git push origin main
```

If other source files still fail verification, use the complete updated
source ZIP and commit via local git CLI (`git add -A`, `git status`, `git commit`,
`git push`), rather than hand-uploading selected folders through the GitHub
web editor. Do not re-add the old `.github/workflows/build.yml`.

The workflow uses the 6.6.0 version metadata, isolated Lab package ID,
commit-triggered GitHub prereleases, and existing signing-variable names.
Nothing here replaces the application ID or signing key used by production.

## Evidence and limitations

- **Real CI:** ARM64 dot-product bridge compiled; Android resources processed.
- **Real CI:** `:app:compileDebugKotlin` FAILED on missing UI source files.
- **Offline:** the full package contains the missing UI files, and the
  updated source verifier passes in the full source tree.
- **Unverified:** a new Gradle compile, completed APK, Release upload, and
  on-device QNN/native transcription; require another GitHub Actions run.
