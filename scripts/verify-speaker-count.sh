#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/app/src/main/java/com/example/whisperrecorder"
SPEAKER="$APP/speaker"
MAIN="$APP/MainActivity.kt"
PREFS="$APP/data/AppPrefs.kt"
SETTINGS="$APP/ui/settings/SettingsSubscreens.kt"
PICKER="$APP/ui/SpeakerCountPicker.kt"
for f in "$SPEAKER/SpeakerClusteringPolicy.kt" "$SPEAKER/SpeakerAttribution.kt" "$PICKER"     "$ROOT/app/src/test/java/com/example/whisperrecorder/speaker/SpeakerClusteringPolicyTest.kt"; do
  test -s "$f" || { echo "Missing speaker count source: $f" >&2; exit 1; }
done
for term in 'numClusters = clusterCount' 'computeConfidence = clusterCount != 1' 'SpeakerClusteringPolicy.normalize(expectedSpeakers)' 'expectedSpeakers: Int'; do
  grep -Fq "$term" "$SPEAKER/OfflineSpeakerService.kt" || { echo "Missing native count mapping: $term" >&2; exit 1; }
done
for pair in "$MAIN|AppPrefs.speakerExpectedCount(this@MainActivity)"             "$MAIN|AppPrefs.setSpeakerExpectedCount(this@MainActivity, count)"             "$SETTINGS|AppPrefs.setSpeakerExpectedCount(context, count)"             "$PREFS|SpeakerClusteringPolicy.normalize(prefs(context).getInt(KEY_SPEAKER_COUNT"             "$PICKER|SpeakerClusteringPolicy.choices"; do
  filename="${pair%%|*}"; needle="${pair#*|}"
  grep -Fq "$needle" "$filename" || { echo "Missing saved speaker count mapping: $needle" >&2; exit 1; }
done
grep -Eq 'SpeakerAttribution\.bestSpeaker\(cue, (intervals|nearby)\)' "$SPEAKER/SpeakerTranscript.kt" || { echo 'Missing overlap-safe speaker attribution' >&2; exit 1; }
echo 'Speaker count/attribution source guards PASSED'
