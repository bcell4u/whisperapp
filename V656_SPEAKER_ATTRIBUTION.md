# WhisperRecorder v6.5.6: speaker-attribution precision

## Scope

This is an optional, after-the-fact **speaker transcript** refinement. Existing Parakeet
NPU transcription, original TXT files, fixed speaker-count setting, offline model
weights, 15-minute safety limit and native library isolation are unchanged.

## Changes

- sherpa-onnx 1.13.8's optional per-segment **silhouette confidence** is enabled
  for automatic and multi-speaker clustering. This is not a calibrated
  probability, and unavailable confidence (`-2`) is neutral.
- The cue labeler now sweeps distinct time slices rather than summing all
  intersecting windows; a duplicated/overlapping interval for the same
  speaker no longer counts speech twice.
- When two speakers contribute significant evidence inside a single coarse
  ASR cue, it outputs `Unknown speaker` rather than claiming a precise
  word-level turn boundary which the ASR timing does not provide.
- For intermittent speech within a cue, a clear speaker may be assigned with
  at least 20% cue coverage and at least 150 ms of diarized audio, if the
  speaker has at least 45% of diarized activity and a strong majority
  of confidence-weighted evidence (65%). This replaces the older always-50%-of-entire-cue rule.
- The exported general diagnostics log records count, ambiguous/unlabeled
  cues and aggregate silhouette confidence, never transcript content.

## Trade-offs and limitations

The algorithm favors *precision over coverage*: some words close to rapid
speaker changes or interruptions may be labeled `Unknown speaker`. The
silhouette confidence computation has extra CPU work for more segments, so
check runtime on representative device recordings. No ground-truth labeled
interview audio has been supplied, so accuracy improvement is a **hypothesis**,
not a measured result. Compare the same recording in v6.5.5 and v6.5.6.

A longer-term improvement would be word-level timestamps / forced alignment,
so speaker boundaries can be applied inside an ASR cue rather than assigning
one voice to the entire cue. That is outside this conservative update.
