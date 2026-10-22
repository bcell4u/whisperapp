# v6.5.5 speaker clustering correction

## Reported problem

A two-person interview produced five speaker labels using automatic clustering
with threshold 0.5. This is over-segmentation of speakers, not evidence of five
independent speakers. Without the actual labeled audio and speaker transcript,
there is no basis to identify every incorrect turn.

## Change

- Add per-recording expected-speaker selector (Automatic, 1–6) and persistent
  default in Settings; choose 2 for an interview with two known speakers.
- Forward the selected count into sherpa-onnx v1.13.8
  `FastClusteringConfig(numClusters=..., threshold=...)`.
- Auto mode now uses 0.75 instead of 0.5 as a conservative starting heuristic,
  without asserting accuracy gains until tested on real recordings.
- Keep 15-minute guard, local models, timestamps-off default, filename behavior,
  original transcript preservation, and Parakeet/QNN implementation intact.

## Limitations and tests

No audio or reference speaker-turn annotations were provided, so correctness cannot
be assessed from a count alone. Fixed 2 may still misassign some turns, especially
with overlap, rapid exchanges or approximate transcript cue timestamps.
A successful GitHub build and two-person device evaluation are required.
Test automatic and fixed 2 on the same recording, compare individual turns with
the original audio, and check one-person recordings separately.
