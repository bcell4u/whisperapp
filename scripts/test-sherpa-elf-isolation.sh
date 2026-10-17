#!/usr/bin/env bash
# Build versioned AArch64 test libraries and verify correct in-place isolation.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
for c in clang python3 zip unzip readelf; do command -v "$c" >/dev/null || { echo "Missing fixture tool: $c" >&2; exit 1; }; done
WD="$(mktemp -d)"; trap 'rm -rf "$WD"' EXIT
mkdir -p "$WD/aar/jni/arm64-v8a"
cd "$WD/aar/jni/arm64-v8a"
printf '%s\n' 'int ort_foo(void) { return 42; }' > ort.c
printf '%s\n' 'ORT_1 { global: ort_foo; local: *; };' > ort.map
printf '%s\n' 'extern int ort_foo(void); int speaker_foo(void) { return ort_foo(); }' > jni.c
clang -target aarch64-linux-android35 -fuse-ld=lld -nostdlib -fPIC -shared \
  -Wl,-soname,libonnxruntime.so -Wl,--version-script,ort.map -o libonnxruntime.so ort.c
clang -target aarch64-linux-android35 -fuse-ld=lld -nostdlib -fPIC -shared \
  -Wl,-soname,libsherpa-onnx-jni.so -L. -o libsherpa-onnx-jni.so jni.c \
  -Wl,--no-as-needed -lonnxruntime
rm -f ort.c ort.map jni.c
readelf -V libsherpa-onnx-jni.so | grep -Fq 'File: libonnxruntime.so'
ORIGINAL_SIZE="$(stat -c '%s' libsherpa-onnx-jni.so)"
cd "$WD/aar"
zip -qr "$WD/fixture.aar" .
bash "$SCRIPT_DIR/isolate-sherpa-onnx.sh" "$WD/fixture.aar"
mkdir -p "$WD/repacked"
unzip -q "$WD/fixture.aar" -d "$WD/repacked"
LIBDIR="$WD/repacked/jni/arm64-v8a"
[[ "$(stat -c '%s' "$LIBDIR/libsherpa-onnx-jni.so")" == "$ORIGINAL_SIZE" ]]
readelf -V "$LIBDIR/libsherpa-onnx-jni.so" | grep -Fq 'File: libsherpa_ort.so'
readelf -d "$LIBDIR/libsherpa-onnx-jni.so" | grep -Fq 'Shared library: [libsherpa_ort.so]'
readelf -d "$LIBDIR/libsherpa_ort.so" | grep -Fq 'Library soname: [libsherpa_ort.so]'
python3 "$SCRIPT_DIR/patch-sherpa-elf.py" --verify "$LIBDIR"
# Invalid version dependency must fail verification even if DT_NEEDED is valid.
cp "$LIBDIR/libsherpa-onnx-jni.so" "$WD/uncorrupted.so"
python3 - "$LIBDIR/libsherpa-onnx-jni.so" "$SCRIPT_DIR/patch-sherpa-elf.py" <<'PY'
from pathlib import Path
import importlib.util, struct, sys
p = Path(sys.argv[1])
spec = importlib.util.spec_from_file_location("elf_isolation", sys.argv[2])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
e = module.Elf(p)
assert any(name == module.NEW for _, name in e.verneed)
version_name_index = e.data[e.strtab:e.strtab+e.strsz].find(b"ORT_1\0")
assert version_name_index >= 0
# Corrupt VERNEED.vn_file ONLY (not DT_NEEDED) as patchelf did on Android.
verneed_offset = e.va_to_offset(e.one(module.DT_VERNEED), 16)
struct.pack_into("<I", e.data, verneed_offset + 4, version_name_index)
p.write_bytes(e.data)
PY
if python3 "$SCRIPT_DIR/patch-sherpa-elf.py" --verify "$LIBDIR" >/dev/null 2>&1; then
  echo 'Corrupt versioned dependency unexpectedly passed validation' >&2; exit 1
fi
printf '%s\n' 'AArch64 versioned sherpa-ORT ELF isolation PASS (including corruption check)'
