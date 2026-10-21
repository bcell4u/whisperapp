# WhisperRecorder 6.6.3 — one-second overlapping-segment accuracy experiment

## Why

S26 v6.6.1 achieved a 30,521 ms SUCCESS transcription for 19 segments of the
~220.54-second lecture, but terms near 12-second boundaries may be clipped.
Overlap is being tested for **transcription quality**, not speed. Improvement is
not guaranteed; no word-error-rate reference or gold transcript was supplied.
M-RoPE equivalence remains unqualified and model/token parity not proven.

## What is implemented

- On-screen checkbox (default ON) adds one second of PCM overlap between adjacent
  **12-second windows** using the pre-existing PcmDecoder streaming interface.
  No longer NPU mel inputs and no change to the QNN encoder or GGUF weights.
- Expected windows: 220.54 seconds -> 20 (was 19), 240.00 seconds -> 22 (was 20).
  The worker and native manifest limits are raised together from 20 to 24;
  native 512-token context, four CPU threads, dotprod instructions, and one
  model load remain unchanged. Decoded audio duration is checked even when
  a source omits metadata; overlap must not inflate this duration check.
- Worker removes Qwen protocol prefixes individually, then conservatively merges
  adjacent transcripts. It strips only a suffix/prefix of **2–8 exact whole
  matching words**, treating punctuation and letter case as nonsemantic.
  Single matches, ambiguous phrases, and approximate medical terms are preserved.
  No hallucinated medical-term substitutions, no fuzzy word matching.
- OFF means the original 0-second overlap and ordinary space-joined segments.
- Public diagnostic logs: overlap length, duplicate words removed, stitched
  seams and unmatched seams (NUMBERS ONLY), along with existing timings.
  No transcript/audio/medical wording/filenames are added to exported logs.
- Model backup/restore in Download/SFSU/Whisperapp/Models and log storage in
  Download/SFSU/Whisperapp/Logs remain intact.

## Validation and one useful S26 run

Run a single transcription of the **same existing lecture** with overlap ON.
Compare the visible transcript at the previously problematic phrase boundaries
against the saved v6.6.1 transcript and (where possible) the private audio.
Look for recovered split words, duplicated phrases, lost speech, incorrect
medical terms, and total wall time / decoder time. Inspect the new numerical
log in the existing Downloads Logs directory and keep the transcript private.
The previous baseline was 19 segments / 765 tokens / 20,773 ms decoder /
30,521 ms total; 20 overlapping segments may be a little slower. One run
with overlap OFF is optional only if a direct A/B is necessary.

## Gates

Local Kotlin/JVM seam-stitcher smoke tests and source/workflow gates pass.
No v6.6.3 APK has been compiled or run on the S26 in this session. GitHub Actions
still handles compilation and auto-publishes a prerelease only after its build,
signing, APK checks, and unit tests pass. Version code 66003. The workflow's
signing key must be stable for updates in place; backups survive uninstall.
