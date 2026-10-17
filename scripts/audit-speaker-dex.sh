#!/usr/bin/env bash
# Verify that the minified release APK actually contains the sherpa Java API.
# Checking only the .so files cannot detect missing/removed DEX classes.
set -euo pipefail
APK="${1:?usage: audit-speaker-dex.sh app.apk /path/to/dexdump}"
DEXDUMP="${2:?usage: audit-speaker-dex.sh app.apk /path/to/dexdump}"
test -f "$APK" || { echo "Missing APK: $APK" >&2; exit 1; }
test -x "$DEXDUMP" || { echo "Missing Android SDK dexdump: $DEXDUMP" >&2; exit 1; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
found=0
while IFS= read -r entry; do
  case "$entry" in
    classes.dex|classes[0-9]*.dex)
      unzip -p "$APK" "$entry" > "$tmp/current.dex"
      "$DEXDUMP" -f "$tmp/current.dex" >> "$tmp/classes.txt"
      found=$((found + 1))
      ;;
  esac
done < <(unzip -Z -1 "$APK")
test "$found" -gt 0 || { echo "APK contains no classes.dex" >&2; exit 1; }
for class in \
  'Lcom/k2fsa/sherpa/onnx/OfflineSpeakerDiarization;' \
  'Lcom/k2fsa/sherpa/onnx/OfflineSpeakerDiarizationConfig;' \
  'Lcom/k2fsa/sherpa/onnx/OfflineSpeakerSegmentationModelConfig;' \
  'Lcom/k2fsa/sherpa/onnx/SpeakerEmbeddingExtractorConfig;' \
  'Lcom/k2fsa/sherpa/onnx/SpeakerEmbeddingExtractor;' \
  'Lcom/k2fsa/sherpa/onnx/OnlineStream;'; do
  grep -Fq -- "'$class'" "$tmp/classes.txt" || {
    echo "APK missing required speaker runtime DEX class: $class" >&2
    exit 1
  }
done
echo "Speaker DEX class audit PASSED ($found DEX files)"
