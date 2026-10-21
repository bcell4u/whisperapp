# v6.5.12 — Recoverable recording/transcript deletion

## Issue

In v6.5.11, multi-select **Delete transcripts too** deleted audio first. If deleting the regular or speaker transcript failed, the app retained the transcript-only entry but returned a nominally successful result, so the batch UI hid the failure. A later transcript-only retry stopped immediately when speaker-transcript deletion failed, without trying the regular transcript. Additionally, storage reconciliation ignored speaker-only entries when both audio and regular transcript were gone.

The original device failure can involve a missing SAF document, provider refusal, lost workspace access or a stale URI. There is no device diagnostic log for this incident, so a particular provider-level cause is not confirmed.

## Fix

- Independently attempt requested app-owned audio, regular transcript, and speaker transcript deletions; one failure no longer skips the other files. External-source audio is **never** deleted.
- Treat a confirmed already-missing document as deleted, making retry idempotent. Unknown provider errors and permission denials remain failures, preserving file references.
- Atomically update recording metadata for documents actually removed; keep and expose the remaining document references. Speaker-only entries remain visible and deletable, including during workspace reconciliation.
- Expose batch errors clearly at the top of the Library; `partial=true` can no longer masquerade as success. Keep per-entry exception handling in the batch loop.
- Record a per-document status (`deleted`, `already-missing`, `provider-refused`, or error class), and a final summary in the existing privately stored, user-exportable transcription diagnostics. Logs include recording ID and document *type*, never filenames, document URIs, audio, or transcription text.
- No changes to ASR, NPU, diarization models, playback or transcript contents.

## Important limitations

File deletion is not atomic at the Android SAF provider level. If the provider denies permission or refuses deletion, some requested files may already be removed; the Library preserves pointers to remaining files so deletion can be retried. If access to the selected storage folder was revoked, restore access in app setup/Settings or remove the remaining file manually via the Files app, then reopen the Library. **Remove from Library only** does not delete files.

## Verification

- `scripts/verify-source.sh` passes.
- 10 standalone compiled JVM tests cover complete deletes, partially removed audio, regular/speaker failures, speaker-only recovery, keep-transcript semantics, and retries.
- GitHub workflow syntax and source package/patch integrity tested locally.
- **No Android SDK/build or physical-device deletion test is available locally.** Confirm on GitHub Actions and test SAF deletion on your device.
