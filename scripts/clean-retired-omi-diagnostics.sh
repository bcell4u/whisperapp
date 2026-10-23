#!/usr/bin/env bash
# Remove ONLY obsolete Omi NPU compatibility diagnostic artifacts left behind
# when updated source files were copied over an existing Git checkout.
# This does not modify the production Omi ASR implementation.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
paths=(
  "scripts/verify-omi-probe.sh"
  "app/src/main/java/com/example/whisperrecorder/npu/OmiNpuProbeContracts.kt"
)
for rel in "${paths[@]}"; do
  if [ -e "$ROOT/$rel" ] || [ -L "$ROOT/$rel" ]; then
    rm -f -- "$ROOT/$rel"
    echo "::warning::Removed obsolete Omi diagnostic from build checkout: $rel. Remove and commit its deletion in the Git repository too."
  fi
done
# Do not conceal an old Settings screen or a leftover IPC implementation.
if grep -Fq 'Omi Med NPU compatibility' "$ROOT/app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt"; then
  echo 'Retired Omi diagnostics UI remains in SettingsSubscreens.kt; apply the updated source files.' >&2
  exit 1
fi
