# v6.5.7 - Long-form speaker diarization and speaker-specific recording menu

## What changed

**UI:** The ordinary Recording page has only the **Create speaker transcript** entry for speaker functionality. That entry opens a distinct **Speaker transcript** page, using the same Material 3/top-bar, recording title, date, audio playback, and transcript card arrangement as the regular detail page. The new page contains: expected speaker count, Create / Regenerate speaker transcript, View full speaker transcript, Share speaker transcript, Rename speaker labels, Share audio/video, and error diagnostics. These controls are no longer on the parent Recording page. Original ASR sharing and re-transcription remain in the normal page. Renaming a label refreshes the speaker transcript preview. The speaker viewer has no ASR timing linkage because its generated text differs from the original word cues; playback is still available independently.

**Duration:** Removed the old 15-minute hard stop. The source is decoded once as a stream of 180-second windows with 30-second overlap (150-second stride) at 16 kHz mono. No full-recording PCM array is constructed, and the window samples are discarded after the matching and native inference step. The `OfflineSpeakerDiarization` and `SpeakerEmbeddingExtractor` instances are reused across windows. Speaker-model weights are unchanged.

**Cross-window identity continuity:** Each diarizer window starts with local speaker numbers. v6.5.7 extracts a bounded, continuous voiced sample (1.5-9 seconds) per local identity with the existing ERes2Net model, matches normalized embeddings against running centroids using cosine similarity, and also uses agreements within the 30-second shared interval. Matches with weak or competing evidence remain unknown instead of being guessed. Fixed Expected speakers (e.g. 2) caps global identities at that number. In Automatic mode up to 12 global identities are tracked as a safety guard, not a promise of correct speaker counting. The overlap is cut once at its midpoint so it doesn't duplicate words or artificially boost attribution. All original per-cue confidence-aware alignment from v6.5.6 is preserved.

**Background reliability:** A new, non-exported Android foreground `dataSync` service owns long-running speaker processing, progress notifications, and a timed partial wake lock. This prevents normal activity navigation or screen locking from canceling processing. On success it updates the existing separate speaker transcript URI; failures still generate the private per-recording diagnostics report, available via the speaker page. Model inference remains **CPU-only**, not NPU. Parakeet NPU and GPU codepaths were not changed.

## Constraints and limitations

- Every sherpa process call is capped at three minutes of input PCM, but long recordings may still take a significant amount of wall-clock time on CPU. Android may stop foreground services under device/system restrictions; the wake lock has a two-hour safety limit. There is no automatic pause/resume checkpoint after process death yet.
- If a voice has no clean long enough utterance or a safe cross-window match cannot be established, attribution is **Unknown speaker** rather than an invented label. Exactly two speakers does not guarantee every turn is classified correctly. Fast cross-talk and changing acoustics remain challenging.
- Input requires completed source ASR with saved timestamp cues; no new ASR is performed. Transcript text may be inaccurately assigned near cue-level speaker changes, because word-level forced alignment is not available.
- Native JNI compatibility needs a GitHub release build and on-device test. Pure-Kotlin test success cannot guarantee sherpa embedding / foreground service behavior on a Galaxy S26.
- Memory is bounded with respect to decoded PCM, but output intervals and transcript cues are accumulated in RAM. Multi-hour stress tests are still recommended.
- The current UI may show an older completed speaker file while a regeneration is running. It creates a uniquely named result, and does not overwrite the original ASR transcript.

## Device acceptance tests

1. Upgrade using the same signing key; `versionName=6.5.7`, `versionCode=65007`.
2. A short two-person interview: set Expected speakers = 2; compare speaker labels with v6.5.6; check timestamps Off/On.
3. A 20-minute recording with speakers exchanging turns across 3-minute window boundaries: confirm speaker IDs are stable and there are no duplicate statements around 02:45, 05:15, etc.
4. The previously recorded 67-minute lecture: keep screen unlocked for first run and repeat while screen locked; check status notification, gap/Unknown labels and independent output file.
5. Share transcript, rename speakers and verify preview, share audio, reopen full speaker transcript, and navigate Back to the normal Recording page. Ensure none of the speaker-specific controls remain on that parent page.
6. Run Parakeet NPU transcription after speaker processing to check that ORT-QNN isolation remains intact.
7. Re-run model install verification and test speaker diagnostic copy/share after a deliberate invalid model or unavailable audio failure.
