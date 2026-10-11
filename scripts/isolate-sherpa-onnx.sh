#!/usr/bin/env bash
# Isolate sherpa-onnx's ONNX Runtime by editing original ELF dynstr bytes
# in place. patchelf's string-table relocation corrupted DT_VERNEED on Android.
set -euo pipefail

AAR="${1:?usage: isolate-sherpa-onnx.sh path/to/sherpa-onnx.aar}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
for command_name in unzip zip python3 readelf; do
  command -v "$command_name" >/dev/null || { echo "Missing required tool: $command_name" >&2; exit 1; }
done
[[ -f "$AAR" ]] || { echo "Missing sherpa AAR: $AAR" >&2; exit 1; }

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
mkdir -p "$work_dir/extracted"
unzip -q "$AAR" -d "$work_dir/extracted"
lib_dir="$work_dir/extracted/jni/arm64-v8a"
[[ -d "$lib_dir" ]] || { echo "Missing arm64 native libraries" >&2; exit 1; }

# Strictly fail on ABI/ELF incompatibilities rather than choosing ORT-QNN
# or altering the model or decoder runtime.
python3 "$SCRIPT_DIR/patch-sherpa-elf.py" "$lib_dir"
python3 "$SCRIPT_DIR/patch-sherpa-elf.py" --verify "$lib_dir"
readelf -d "$lib_dir/libsherpa-onnx-jni.so" | grep -Fq 'Shared library: [libsherpa_ort.so]' || {
  echo "Missing sherpa JNI DT_NEEDED entry" >&2; exit 1;
}
readelf -d "$lib_dir/libsherpa_ort.so" | grep -Fq 'Library soname: [libsherpa_ort.so]' || {
  echo "Missing isolated runtime SONAME" >&2; exit 1;
}
# This app currently packages arm64 only.
find "$work_dir/extracted/jni" -mindepth 1 -maxdepth 1 -type d ! -name arm64-v8a -exec rm -rf -- {} +
(
  cd "$work_dir/extracted"
  zip -qr "$work_dir/sherpa-isolated.aar" .
)
unzip -tq "$work_dir/sherpa-isolated.aar" >/dev/null
mv "$work_dir/sherpa-isolated.aar" "$AAR"
echo "Isolated sherpa ONNX Runtime with validated ELF version requirements (arm64-v8a)"
