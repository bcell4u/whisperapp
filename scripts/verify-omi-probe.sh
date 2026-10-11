#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONTRACT="$ROOT/app/src/main/java/com/example/whisperrecorder/npu/OmiNpuProbeContracts.kt"
WORKER="$ROOT/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"
UI="$ROOT/app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt"
AIDL="$ROOT/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"
for item in graph_z1sbzhs3 graph_3c5bjib1 graph_ey_zztqn 1_216_129_360L "output_0"; do
  grep -Fq "$item" "$CONTRACT" || { echo "Omi context contract missing $item" >&2; exit 1; }
done
for item in 'probeOmiContexts' 'session.disable_cpu_ep_fallback' 'locate-installed-omi-package' 'sha256' 'run-inference' 'staging-storage-check'; do
  grep -Fq "$item" "$WORKER" || { echo "Omi probe worker missing $item" >&2; exit 1; }
done
grep -Fq 'Omi Med NPU compatibility' "$UI"
grep -Fq 'probeOmiContexts(boolean includeEncoder)' "$AIDL"
PACKAGE="$ROOT/app/src/main/java/com/example/whisperrecorder/npu/OmiNpuPackageStore.kt"
DOWNLOAD="$ROOT/app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt"
MODELS="$ROOT/app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt"
for item in 'drinnird/omi-med-stt-v1-snapdragon-sm8850-v81' 'model-manifest.json' 'downloadResumable' 'verifyArchived' 'Whisperapp/Models'; do
  grep -Fq "$item" "$PACKAGE" || { echo "Omi package downloader missing $item" >&2; exit 1; }
done
grep -Fq 'OmiNpuPackageStore.DOWNLOAD_ID' "$DOWNLOAD"
grep -Fq 'onDownload = { onStartModelDownload(OmiNpuPackageStore.DOWNLOAD_ID) }' "$MODELS"
[ -f "$ROOT/app/src/test/java/com/example/whisperrecorder/npu/OmiNpuPackageStoreTest.kt" ]
if grep -Fq 'OpenDocumentTree()' "$UI"; then echo 'Manual Omi folder picker still present' >&2; exit 1; fi
grep -Fq 'runtime-worker.log'  "$ROOT/app/src/main/java/com/example/whisperrecorder/diagnostics/DiagnosticLogger.kt"
echo 'Omi QNN compatibility probe source checks PASSED'
