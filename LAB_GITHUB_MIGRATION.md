# Repair: old production workflow triggered instead of the 6.6.0 Lab workflow

## Evidence (Actions run 107107939836, 2026-10-21 UTC)

- GitHub checked out `main` and read `releaseVersion=6.6.0`, `versionName=6.6.0`, `versionCode=66000`.
- It ran the preexisting **Build Android APK** / `.github/workflows/build.yml` workflow.
- The old `scripts/verify-source.sh` reported `FAIL: release documentation matches version.properties` and exited 1.
- **No Android or native compilation was reached.** The `ccache: command not found` output appeared in an `|| true` post-failure cleanup step; it did not fail the job.
- The Lab's `.github/workflows/qwen-gguf-lab-6.6-autorelease.yml` specifically requires the old production workflow to be absent. Updating a version document in the old verifier is therefore the wrong repair.

## Recommended one-file deletion (existing Git repository)

Run from a clone or GitHub Codespaces checkout containing the **Lab** source:

```bash
bash scripts/activate-lab-workflow.sh
git status --short
git commit -m "Activate 6.6.0 Lab-only auto releases"
git push origin main
```

The script checks the Lab application ID and Lab workflow before acting. It stages the removal of **only** `.github/workflows/build.yml`, runs the Lab's source verifier, and does not commit or push itself. If the script has not yet been uploaded, the equivalent targeted command is:

```bash
git rm -- .github/workflows/build.yml
python3 scripts/verify-lab-source.py
git commit -m "Disable obsolete production build in Lab repo"
git push origin main
```

If using **GitHub's web file editor**, navigate to `.github/workflows/build.yml`, choose **Delete this file**, and commit the deletion to `main`. Do not delete `.github/workflows/qwen-gguf-lab-6.6-autorelease.yml`.

This deletion affects only the repository where you commit it. It does not uninstall, update, resign or modify the installed production Android app, and does not remove any private data.

## What the next run must show

After the commit, **Qwen GGUF Lab 6.6 Android APK + Auto Release** should run. Its first step invokes `python3 scripts/verify-lab-source.py`. The job must continue through toolchain setup, native compile, Gradle APK compilation, package/signature verification and release publishing. If a later step fails, use the *Lab workflow's* logs; no APK is verified until the compile/check/publish steps complete.

The old production workflow should no longer appear as a runnable workflow on the new commit. If it still runs, double-check the branch's `.github/workflows/build.yml` file really was deleted. A stopped or skipped old historical run is not an APK compilation result.

## Build identity and signing

The Lab remains version `6.6.0`, code `66000`, package `com.example.whisperrecorder.qwengguflab.dev`. No key is included or changed. The separate Lab workflow uses the existing `ANDROID_*` secret names when configured, or ephemeral debug signing otherwise. Its successful `main` build publishes a *prerelease* with its own `v6.6.0-lab.*` tag, rather than changing a production tag.
