# WhisperRecorder Qwen GGUF Lab 6.6.3 — GitHub source recovery

## Actual failing action

The uploaded GitHub Actions run `107159083421` at commit
`03b60a466029c275ae6df6cc5390d19b2cf6e112` failed **before**
Kotlin/Gradle/native compilation in `Verify lab rebase and 6.6 metadata`.
The source verifier reports one absent Kotlin source file:

`app/src/main/java/com/example/whisperrecorder/qwen/QwenLabPublicModels.kt`

The 8 follow-up missing-contract messages and duplicated `MISSING` are a
cascade from this absent file; they do not establish additional defects.

## Fix (keep 6.6.3 / 66003)

Extract this archive into the **root of the existing Git checkout**,
retaining all directories. This ZIP contains the exact source file from the
verified complete v6.6.3 source snapshot. No model, binary, signing, workflow,
or decoder files are replaced.

In the **Git checkout / GitHub Codespaces** terminal:

```sh
python3 scripts/verify-lab-source.py
git add app/src/main/java/com/example/whisperrecorder/qwen/QwenLabPublicModels.kt
git ls-files --error-unmatch app/src/main/java/com/example/whisperrecorder/qwen/QwenLabPublicModels.kt
git diff --cached --stat
git commit -m "Restore 6.6.3 portable model storage source"
git push origin main
```

If `git diff --cached --stat` shows no changes, check `git status` and
`git ls-files` before committing; the file may already be staged/tracked.
Do not upload this ZIP itself as a repository file. In GitHub's web editor,
create the file under its complete `app/src/main/java/.../qwen/` path rather
than placing it at repository root.

The push launches the existing GitHub Actions workflow. A passing source
verifier is **not** proof of an APK build; review the new run for actual Gradle
and Android signing/release stages.

## Local validation

- v6.6.3 full source ZIP contained this file (10,799 bytes).
- SHA-256: `40fe1761aa66a4fff18a9652a8d13cf24281fd7f3e6eb483e4653ea897f82319`.
- The complete source snapshot passes `python3 scripts/verify-lab-source.py`.
- This repair changes neither model inference nor overlap stitching.
