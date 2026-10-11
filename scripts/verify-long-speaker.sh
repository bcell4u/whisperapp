#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SPEAKER="$ROOT/app/src/main/java/com/example/whisperrecorder/speaker"
MENU="$ROOT/app/src/main/java/com/example/whisperrecorder/ui/SpeakerTranscriptMenu.kt"
MAIN="$ROOT/app/src/main/java/com/example/whisperrecorder/MainActivity.kt"
for name in SpeakerWindowStitcher SpeakerWindowAudio SpeakerWindowMerge; do test -s "$SPEAKER/$name.kt"; done
test -s "$MENU"
test -s "$ROOT/app/src/test/java/com/example/whisperrecorder/speaker/SpeakerWindowStitcherTest.kt"
test -s "$SPEAKER/SpeakerDiarizationService.kt"
grep -Fq ".speaker.SpeakerDiarizationService" "$ROOT/app/src/main/AndroidManifest.xml"
grep -Fq 'SpeakerTranscriptMenu(' "$MAIN"
grep -Fq 'Create speaker transcript' "$MAIN"
for term in 'Share speaker transcript' 'Rename speaker labels' 'Expected speakers' 'Speaker settings'; do grep -Fq "$term" "$MENU"; done
# Sharing media is a regular transcript card action, not a speaker-menu action.
grep -Fq 'Share audio' "$MAIN"
grep -Fq 'ic_share_transcript' "$MENU"
! grep -Fq 'onShareAudio' "$MENU"
grep -Fq 'open16kSegmentStream(WINDOW_SECONDS, OVERLAP_SECONDS)' "$SPEAKER/OfflineSpeakerService.kt"
! grep -Fq 'MAX_SECONDS = 15 * 60' "$SPEAKER/OfflineSpeakerService.kt"
echo 'Long-form speaker and dedicated menu guards PASSED'
