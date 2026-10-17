#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
setup="$root/app/src/main/java/com/example/whisperrecorder/ui/setup/SetupScreen.kt"
settings="$root/app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt"
probe="$root/app/src/main/java/com/example/whisperrecorder/audio/MicrophoneCapabilityProbe.kt"
wav="$root/app/src/main/java/com/example/whisperrecorder/audio/WavPcmRecorder.kt"
for file in "$setup" "$settings" "$probe" "$wav"; do test -f "$file" || { echo "Missing $file" >&2; exit 1; }; done
for item in 'SectionLabel("NPU")' 'NpuPackageStore.archiveState(context)' 'NpuPackageStore.selectForTranscription(context)' 'NpuPackageStore.DOWNLOAD_ID'; do
  grep -Fq "$item" "$setup" || { echo "Setup NPU guard missing: $item" >&2; exit 1; }
done
for item in 'MicrophoneCapabilityProbe.check(context, nextSource, format, nextNs, nextAec)' 'if (result.supported) onApproved()'; do
  grep -Fq "$item" "$settings" || { echo "Audio preflight UI guard missing: $item" >&2; exit 1; }
done
for item in 'context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)' 'RecordingRuntime.state.value.active' 'recorder.startRecording()' 'recorder.start()' 'effect.enabled = true' 'require(effect.enabled)'; do
  grep -Fq "$item" "$probe" || { echo "Audio probe guard missing: $item" >&2; exit 1; }
done
grep -Fq 'processing.preset == MicrophoneProcessing.PRESET_CUSTOM' "$wav"
echo 'feature-gate source checks PASSED'
