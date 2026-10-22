# WhisperRecorder Qwen GGUF Lab v6.6.2 — Models backup for reinstall

## User request

After tapping **Download missing encoder + Q8 GGUF from Hugging Face**, retain
validated model files in both the app's private working storage and
**Internal storage/Download/SFSU/Whisperapp/Models**. This public Downloads folder
survives uninstall (unlike app-private data). Stage for the NEXT build; do not
trigger CI or claim APK compilation.

## Implementation

- `QwenLabPublicModels.kt`: a scoped-storage `MediaStore.Downloads` exporter, using
  `RELATIVE_PATH=Download/SFSU/Whisperapp/Models/` and `IS_PENDING` until written.
- After the *existing* private downloader completes successfully, the UI copies and
  verifies the manifest, four encoder assets and official Q8_0 GGUF into Downloads.
  It reports if backup fails rather than misreporting successfully installed private
  models as download failures. A separate **Back up verified models** button allows
  existing private models to be archived after an in-place app update.
- The exporter streams files (no 1.2 GB in-memory allocation), verifies byte count
  and SHA-256 during the copy, reuses readable matching existing public copies, and
  deletes only a pending MediaStore item created by the failing operation. Previously
  saved public files are never deleted during an upgrade or model backup.
- **Restore models from Downloads after reinstall** uses Android's directory picker;
  the user selects the `Models` subfolder (not the `Download` root). The encoder is
  staged and checked against its manifest; the GGUF is checked against the pinned
  official byte length and SHA-256 before being activated in private Lab storage.
  Only private models are used for transcription; no runtime dependency on external
  Downloads paths. Restore runs entirely on the phone without a PC or network.
- No broad file/storage permission or app data migration. This does NOT change the
  QNN execution provider, M-RoPE, dot-product four-thread decoder, logs, production
  application ID or signing environment variable conventions.

## Six public files

- `encoder-qnn-fp16.bin`
- `tokenizer.json`
- `golden-synthetic-pcm-12s-f32.raw`
- `golden-synthetic-mel-12s-f32.raw`
- `model-manifest.json`
- `Qwen3-ASR-0.6B-Q8_0.gguf`

This duplicate backup consumes approximately 1.2 GB of **additional** storage
on top of the normal private copies. Files in shared Downloads can be read or
removed by the user and by apps with appropriate access; they aren't private.

## Important install/signing caveat

The GitHub workflow currently uses an ephemeral runner debug certificate when
its five stable signing secrets are absent; that is why a new APK may require
uninstall. Configure `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `ANDROID_SIGNING_CERT_SHA256`
with a dedicated persistent Lab key to allow future in-place upgrades; keep the
same application ID and increase versionCode. An app installed with the old
certificate still requires one last uninstall when switching certificates.

This new code cannot rescue files already trapped in the private directory of a
previous APK that must be uninstalled before the update: do not uninstall your
current app expecting the v6.6.2 backup to exist already. On first installation
of this feature, download once (or restore any manually saved copies). Future
uninstalls should preserve the public copies.

On Android 10+, app-created Downloads files persist after uninstall, but the
reinstalled app may not have read access to those previously owned files. The
one-time **Restore models from Downloads** SAF directory selection is deliberate.

## Evidence and gates

- The user-supplied 6.6.1 log shows status=SUCCESS, 19/19 EOS, one model load,
  30,521 ms attempt, 2,010 ms QNN encoding, 20,773 ms decoder, and NPU reported.
  It says M-RoPE equivalence remains unqualified and model parity isn't verified.
- The v6.6.2 source and version/workflow static check passed locally.
- Kotlin/JVM syntax compilation of the downloader + encoder assets + model backup
  against representative Android stubs passed locally (NOT Android SDK compilation).
- **No 6.6.2 APK has been built, installed, or phone-tested.** Do not claim
  MediaStore/SAF device behavior is confirmed until a real one-run check.
