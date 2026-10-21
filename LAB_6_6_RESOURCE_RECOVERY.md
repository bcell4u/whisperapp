# 6.6.0 Lab: restore missing Android resources in GitHub

## Evidence from GitHub Actions run 107112949945

- Workflow: `Qwen GGUF Lab 6.6 Android APK + Auto Release` (correct workflow).
- Checked-out commit: `6c03843863dab61c472f34db06d78869ccb3c7ff` on `main`.
- First failing step: `Verify lab rebase and 6.6 metadata`.
- Exact failure: `FAIL: MISSING app/src/main/res/xml/lab_data_extraction_rules.xml`.
- Compilation never started; **no APK was built or published**.
- The locally packaged 6.6.0 source **does** contain the missing file (and all 24 Android resource XML files). The committed GitHub checkout does not contain that one required file. Public GitHub file checks also did not resolve other resource paths, suggesting the resources directory was incompletely uploaded. This is a repository-content repair, not a decoder or signing change.

## Recommended fix (once, in a Git clone or Codespaces)

The **small resource recovery ZIP** contains these source paths at their proper locations:

- `app/src/main/res/**` (24 XML resource files)
- `app/src/debug/res/values/strings.xml`
- `app/src/main/assets/**` (three notices)
- `scripts/verify-lab-source.py` (reports every missing resource in a single run)
- `LAB_6_6_RESOURCE_RECOVERY.md` (these instructions)

Extract the repair ZIP **directly into the root of your existing Lab Git checkout**, not inside a new subdirectory. Then:

```bash
python3 scripts/verify-lab-source.py
git status --short
git add app/src/main/res app/src/debug/res app/src/main/assets scripts/verify-lab-source.py LAB_6_6_RESOURCE_RECOVERY.md
git commit -m "Restore missing Android resources for Lab 6.6.0"
git push origin main
```

Alternatively, use the complete updated **6.6.0 source ZIP**, extract all contents into your Lab repository root, and stage/commit the changes there. Do not restore `.github/workflows/build.yml` from the original production source. Do not remove the Lab auto-release workflow.

The existing workflow triggers automatically on the push; it only publishes an experimental `v6.6.0-lab.*` prerelease when **every** native/Gradle/APK/signing gate passes.

## Scope and unchanged behavior

- Version remains `6.6.0`, versionCode `66000`, debug appId `com.example.whisperrecorder.qwengguflab.dev`.
- Original v6.5.22 baseline, QNN + GGUF architecture, four-thread dotprod native settings and signing-secret names are unchanged.
- No production APK, private model, keystore, recording, or existing phone data is included.
- `.github/workflows/build.yml` must remain absent from the experimental repository.

## Verification performed offline

- Entire source tree passes `python3 scripts/verify-lab-source.py` after the repair.
- Removing the `app/src/main/res` directory from a temporary copied checkout causes the updated verifier to report **all 24 missing resource files** in one run, as intended; restoring it returns PASS.
- All 24 resource XML files and the debug labels parse correctly as XML.
- ZIP contents and SHA-256 files are checked after creation.
- This is **not** an Android APK compile, a QNN inference test, or an on-device execution validation.

If another failure occurs farther through the build, use the new Lab workflow log from the first failing step. Do not change the 6.6.0 version or signing merely to address missing source resources.
