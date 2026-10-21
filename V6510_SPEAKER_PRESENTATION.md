# v6.5.10 speaker transcript presentation

## Reproduction

In v6.5.9, `OfflineSpeakerService.generate()` grouped word labels only when **every** source timing cue had no whitespace. A single phrase from the native NPU timing fallback disabled grouping across the entire interview, so `SpeakerTranscript.render()` emitted one newline per cue even though word-level playback metadata remained intact.

## Fix

- Count source word cues; if any exist, group adjacent same-speaker text while keeping the source cue list intact.
- Preserve hard speaker changes, silence gaps and sentence breaks; keep original speaker attribution and saved per-word `TranscriptTimeline` untouched.
- Export the reformatted readable speaker transcript to `-speaker.txt`; `TranscriptCueMapper` still maps each timed word to its character range, even across line boundaries.
- Add exportable aggregate diagnostics: `speaker transcript formatting cues=... wordCues=... groupedLines=... source=mixed-timing-safe`.
- Compose's `TranscriptBlockText` now fades the previously highlighted word out while fading the current word's text/accent in over 220 ms. No additional model computation, shader, per-word composables, or timeline writes. The compact player's surface/ink fades and active text cross-fades.

## Validation and use

Source verification passed. 26 independent Kotlin regression methods passed using `kotlinc`, including two new tests for mixed timed cues and readable timestamped groups. Normal ASR paths untouched. Native Android APK compilation and device presentation are not verified in this environment.

To repair already generated text, use **Regenerate speaker transcript** after installing v6.5.10. The existing regular transcript and its fine-grained timestamps are reused; do not re-transcribe the audio unnecessarily. The old speaker text file is retained by the usual name collision behavior.
