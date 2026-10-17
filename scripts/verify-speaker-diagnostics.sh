#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAIN="$ROOT/app/src/main/java/com/example/whisperrecorder/MainActivity.kt"
SPEAKER="$ROOT/app/src/main/java/com/example/whisperrecorder/speaker"
WORKFLOW="$ROOT/.github/workflows/build.yml"
for name in SpeakerDiagnostics.kt SpeakerFailureText.kt; do
  test -s "$SPEAKER/$name" || { echo "Missing speaker diagnostic implementation: $name" >&2; exit 1; }
done
for symbol in 'SpeakerDiagnostics.capture' 'SpeakerDiagnostics.read' 'SpeakerDiagnostics.clear' 'SpeakerFailureText.summary' 'Share speaker diagnostics'; do
  { grep -Fq "$symbol" "$MAIN" || grep -Fq "$symbol" "$SPEAKER/SpeakerDiarizationService.kt"; } || { echo "Missing speaker report flow: $symbol" >&2; exit 1; }
done
grep -Fq 'View speaker diagnostics' "$ROOT/app/src/main/java/com/example/whisperrecorder/ui/SpeakerTranscriptMenu.kt"
grep -Fq 'Class.forName(name, false, context.classLoader)' "$SPEAKER/SpeakerDiagnostics.kt"
grep -Fq 'SpeakerFailureText.exceptionChain(error)' "$SPEAKER/SpeakerDiagnostics.kt"
grep -Fq 'libsherpa_ort.so' "$SPEAKER/SpeakerDiagnostics.kt"
grep -Fq 'scripts/audit-speaker-dex.sh' "$WORKFLOW"
test -s "$ROOT/scripts/audit-speaker-dex.sh"
grep -Eq '^versionCode=[1-9][0-9]*$' "$ROOT/version.properties"
echo 'Speaker diagnostics source guards PASSED'
