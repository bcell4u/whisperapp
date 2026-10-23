# Permanent Android release signing

Whisper Recorder uses one permanent release key for update-compatible APKs. Android only accepts a downloaded APK as an in-place update when the package name is unchanged, the new `versionCode` is higher, and the APK is signed by the same certificate as the installed app.

Permanent release signing was introduced in v5.23. v5.25 hardens the native Windows PowerShell helper so it can recover from a partially completed run, safely capture harmless `keytool` stderr on Windows PowerShell 5.1, and preserve an already-created signing identity. v5.43 leaves that signing contract unchanged.

## One-time setup on Windows

Open PowerShell in the repository root and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-release-keystore.ps1
```

The helper behaves conservatively:

- If `keytool` is already available, it uses the existing installation and changes nothing.
- If `keytool` is missing, it downloads a portable Eclipse Temurin JDK 21 from the Adoptium API into a unique Windows temporary directory.
- The downloaded JDK ZIP is checked against the SHA-256 checksum returned by Adoptium before it is extracted.
- No administrator access, installer, PATH modification, registry change, or system-wide Java installation is used.
- If `whisper-recorder-release.jks` already exists, the helper never overwrites it. It resumes from that existing key, asks for its keystore password, reads its certificate SHA-256 fingerprint, and copies the keystore's one-line base64 value to the Windows clipboard when clipboard access is available.
- If no keystore exists yet, the helper creates `whisper-recorder-release.jks` and then performs the same fingerprint/base64 preparation.
- Harmless `keytool` warnings written to stderr are captured and judged by the native exit code, avoiding the Windows PowerShell 5.1 `NativeCommandError` behavior that can otherwise interrupt a successful run.
- The temporary JDK directory and ZIP are deleted in a `finally` block whether key creation succeeds or fails.

The `.jks` signing key itself is intentionally retained. It is the permanent identity required for future app updates and must not be deleted with the temporary dependency.

On a new key, the script prompts you to enter and confirm both the keystore password and key password. On a recovery/resume run, it asks only for the existing keystore password; use the original key password you chose when creating the key for `ANDROID_KEY_PASSWORD`. Keep both passwords with secure backups of the `.jks` file.

## macOS/Linux helper

On a machine that already has a JDK/keytool installed, you can instead run:

```bash
scripts/create-release-keystore.sh
```

## GitHub Actions secrets

In GitHub, open **Settings -> Secrets and variables -> Actions -> New repository secret** and create these five repository secrets:

- `ANDROID_KEYSTORE_BASE64` - base64 of the complete `.jks` file as one line. The Windows helper copies this value to the clipboard automatically when possible.
- `ANDROID_KEYSTORE_PASSWORD` - the keystore password.
- `ANDROID_KEY_ALIAS` - the key alias. Both helpers default to `whisper-recorder`.
- `ANDROID_KEY_PASSWORD` - the key-entry password.
- `ANDROID_SIGNING_CERT_SHA256` - the SHA-256 certificate fingerprint printed by the helper/keytool. Colons are optional.

For the 6.7.0 integration candidate, the workflow runs only when manually triggered from the GitHub Actions page. With all five signing secrets, it creates and audits a permanently signed release APK. Otherwise it uploads a debug APK (`com.example.whisperrecorder.dev`). It does **not** publish a GitHub Release automatically. If the keystore fingerprint differs from `ANDROID_SIGNING_CERT_SHA256`, it fails closed. Do not install a release candidate over the working production installation before a confirmed device test and backup.

## First install with the permanent key

An app installed from an older debug/ephemeral key cannot be updated directly by a permanently signed release because Android considers the signatures different. Uninstall that old build once and install a permanently signed release such as v5.23 or v5.25. After that, every higher-`versionCode` APK signed with this same permanent key can update the existing installation normally.

The application package must remain `com.example.whisperrecorder` and the permanent keystore must never be replaced or lost.

## Release behavior

When the 6.7.0 workflow is manually dispatched with permanent signing secrets configured, GitHub Actions:

- reconstructs the keystore only inside the temporary runner;
- verifies the keystore certificate against the pinned SHA-256 fingerprint secret;
- runs `assembleRelease` with the permanent signing configuration;
- verifies the completed APK with Android `apksigner` and checks the certificate again;
- records the signer fingerprint in `build-proof.txt`;
- uploads the signed APK as an Actions artifact.

No GitHub Release or stable tag is created automatically for 6.7.0.

The private keystore is never committed to the repository or uploaded as a build artifact.

## Backups

Keep at least two private backups of the `.jks` file plus the alias/password information in separate secure locations. If the permanent signing key is lost, a newly generated key cannot update existing installations signed with the old certificate.
