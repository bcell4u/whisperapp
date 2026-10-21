# v6.5.11 — Non-flashing synchronized word emphasis

## Reported behavior
v6.5.10 full viewers paint a translucent color block under every current word and animate both previous and current blocks for 220 ms. Rapid word changes retrigger the animation, making backgrounds appear to strobe. The compact Now Playing card also cross-fades overlapping word labels.

## Changes
- `TranscriptBlockText` removes the changing background and fades only foreground text color to Material `primary` (135 ms). Keeps existing lazy block layout and per-cue tap-to-seek.
- Track the interrupted transition's current intensity, so an incoming word does not make an outgoing partially tinted word jump to full intensity.
- Keep `CurrentTranscriptCue` card background stable and show a single label, avoiding overlay of two fast-changing words.
- All existing timing, speaker attribution, and diagnostic logging are preserved; no new per-word logging or models.

## Tests and limitations
Static source regression gate guards no per-word backdrop or overlapping compact-label cross-fade. Run the repository source verifier and JVM unit tests. Full Android compilation and real-device visual evaluation remain necessary, including night mode, rapid speech, and Pause/Seek.
