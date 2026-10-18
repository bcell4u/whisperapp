#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
for name in verify-qwen-workspace.py verify-qwen-compact.py verify-qwen-shared-runtime.py verify-qwen-fp16-trial.py verify-qwen-configuration.py; do
  python3 "$root/scripts/$name"
done
qwen="$root/app/src/main/java/com/example/whisperrecorder"
grep -Fq 'DiagnosticReports.begin(this, request.engine.id' "$qwen/transcribe/TranscriptionService.kt"
grep -Fq 'diagnosticRun.workerMetric(' "$qwen/transcribe/TranscriptionService.kt"
grep -Fq 'qwen-worker-metrics.json' "$qwen/diagnostics/DiagnosticReports.kt"
grep -Fq 'android:process=":qwen"' "$root/app/src/main/AndroidManifest.xml"
grep -Fq 'Qwen3HybridSession' "$qwen/transcribe/AsrSession.kt"
echo 'Consolidated Qwen integration verification PASSED'
