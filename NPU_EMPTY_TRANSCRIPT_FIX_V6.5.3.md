# WhisperRecorder v6.5.3 — Long-Lecture NPU Blank-Result Recovery

## Reproduced failure from uploaded diagnostic files

- Recording: `n450 wk 7 - EKG part 2` (recording identifier `6197c703-ec59-4592-a5ac-ceb41b80852f`).
- Length: 4,032,173 ms decoded (about 67 minutes 12 seconds), Opus in `.ogg`, 48 kHz mono.
- Engine: NVIDIA Parakeet TDT 0.6B v3 via Qualcomm NPU, 40-second overlapping chunks (2-second overlap).
- Chunks 1–94/107 completed and returned 50,933 raw characters in aggregate (before cleanup).
- Chunk 95 spans 3,572,000–3,612,000 ms (00:59:32–01:00:12) and terminated the job with `IllegalArgumentException: NPU returned an empty transcript` before the save stage. No completed transcript file was written for that session.
- The log does not include the raw text, the worker response JSON, or the PCM waveform for the failed chunk. Therefore it cannot distinguish silence from a transient TDT output/decoding issue.

## Root cause in app control flow

`AsrSession.kt` previously executed `require(text.isNotBlank())` after a successful NPU worker response. This made an empty decoded text string a fatal exception. `TranscriptionService.kt` wrote the transcript only after all chunks completed, so an empty result near the end discarded all prior recognized text from that session.

## Implementation

1. Keep a strict JSON response contract (`transcript` must be an actual string), but permit explicitly empty strings when the NPU worker reports `ok=true`.
2. On a blank NPU chunk, log worker telemetry plus PCM peak, RMS and nonzero-sample ratio.
3. Retry exactly once via direct inline PCM inference (avoids assuming that the prefetched path always behaves identically).
4. If the retry returns text, continue using that text normally.
5. If the retry is also blank, insert a visible `[No transcription returned for HH:MM:SS–HH:MM:SS]` marker and matching timestamp cue. Continue the remaining chunks; never invent speech.
6. Keep a count of unresolved segments in the completion UI, notification, and session log.
7. For **other fatal errors**, separately save the available recognized words as an explicitly incomplete and uniquely timestamped `*-partial-<timestamp>.txt` file in the Transcripts folder; do not replace the official transcript or attach partial results to the recording's completed transcript metadata.
8. The source version advances to `6.5.3` (`versionCode=65003`). The diagnostic-source guard no longer hardcodes an old versionCode.

Only the NPU response-validation and transcription orchestration were changed. Native graphs, compiled QNN contexts, tokenizer algorithm, standard processing settings, and speaker-diarization implementation are unchanged.

## Validation performed

- `scripts/verify-source.sh`: PASS, including NPU interfaces, feature gates and speaker diagnostic gates.
- Kotlin standalone marker tests: 6 PASS (time formatting, crossing an hour, invalid intervals).
- Kotlin standalone transcript-continuity tests: 4 PASS (earlier text preserved, gap noted, following speech retained).
- GitHub Actions workflow YAML parser: PASS.
- Source ZIP integrity and Git patch application: check before release.

**Still required:** The full Android build and a test on the target device. This environment lacks a complete Android SDK/Gradle toolchain and cannot validate native runtime execution. v6.5.3 fixes the app-side fatal-empty-result handling; it does **not** establish why the decoder returned blank for this particular lecture chunk. NPU worker failures (`ok=false`), malformed JSON and transport errors remain errors. The partial backup protects already recognized text when storage is writable, but is a best-effort fallback, not a guarantee.

## Recommended device regression check

Install a signed APK using the same signing key and a versionCode above the installed version, then re-transcribe the original lecture. Search its diagnostic log for `npu blank chunk=` and `SESSION complete`, confirm a saved transcript, check any visible gap markers around 00:59:32–01:00:12 and listen to the original audio there. If it still fails, inspect the `*-partial-*.txt` file and share the updated log. Do not uninstall the existing app or delete the `.ogg` recording.
