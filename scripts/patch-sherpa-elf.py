#!/usr/bin/env python3
"""Losslessly retarget sherpa's AArch64 ONNX Runtime ELF dependency.

Changing DT_NEEDED with patchelf can invalidate Android ELF version needs
(DT_VERNEED / vn_file). This tool changes only already allocated .dynstr
bytes, leaving section offsets, program headers, and loaded layout intact.
"""
import argparse
from pathlib import Path
import struct
import sys

OLD = b"libonnxruntime.so"
NEW = b"libsherpa_ort.so"  # Must fit in OLD's .dynstr allocation.
PT_LOAD = 1
PT_DYNAMIC = 2
DT_NULL = 0
DT_NEEDED = 1
DT_STRTAB = 5
DT_STRSZ = 10
DT_SONAME = 14
DT_VERNEED = 0x6FFFFFFE
DT_VERNEEDNUM = 0x6FFFFFFF


class ElfError(ValueError):
    pass


class Elf:
    def __init__(self, path):
        self.path = path
        self.data = bytearray(path.read_bytes())
        b = self.data
        if len(b) < 64 or b[:6] != b"\x7fELF\x02\x01":
            raise ElfError("Expected little-endian 64-bit ELF")
        e_type, e_machine = struct.unpack_from("<HH", b, 16)
        if e_type != 3 or e_machine != 183:
            raise ElfError("Expected AArch64 ET_DYN library")
        e_phoff = struct.unpack_from("<Q", b, 32)[0]
        e_phentsize, e_phnum = struct.unpack_from("<HH", b, 54)
        if e_phentsize < 56 or e_phoff + e_phnum * e_phentsize > len(b):
            raise ElfError("Invalid program header bounds")
        self.loads = []
        dynamic = None
        for i in range(e_phnum):
            pos = e_phoff + i * e_phentsize
            p_type, _, p_offset, p_vaddr, _, p_filesz, _ = struct.unpack_from("<IIQQQQQ", b, pos)
            if p_offset + p_filesz > len(b):
                raise ElfError("Program header exceeds file")
            if p_type == PT_LOAD:
                self.loads.append((p_vaddr, p_filesz, p_offset))
            if p_type == PT_DYNAMIC:
                if dynamic is not None:
                    raise ElfError("More than one PT_DYNAMIC")
                dynamic = (p_offset, p_filesz)
        if dynamic is None:
            raise ElfError("Missing PT_DYNAMIC")
        start, size = dynamic
        if size % 16:
            raise ElfError("Malformed PT_DYNAMIC length")
        self.tags = {}
        for pos in range(start, start + size, 16):
            tag, val = struct.unpack_from("<qQ", b, pos)
            if tag == DT_NULL:
                break
            self.tags.setdefault(tag, []).append(val)
        if DT_STRTAB not in self.tags or DT_STRSZ not in self.tags:
            raise ElfError("No dynamic string table")
        self.strtab = self.va_to_offset(self.one(DT_STRTAB), self.one(DT_STRSZ))
        self.strsz = self.one(DT_STRSZ)
        self.needed = [self.getstr(v) for v in self.tags.get(DT_NEEDED, [])]
        self.soname = self.getstr(self.one(DT_SONAME)) if DT_SONAME in self.tags else None
        self.verneed = self.get_verneed()

    def one(self, tag):
        vals = self.tags.get(tag, [])
        if len(vals) != 1:
            raise ElfError(f"Expected one dynamic tag {tag:#x}; got {len(vals)}")
        return vals[0]

    def va_to_offset(self, va, amount):
        for base, length, pos in self.loads:
            if base <= va and va + amount <= base + length:
                return pos + (va - base)
        raise ElfError(f"Cannot map ELF virtual address {va:#x}")

    def getstr(self, index):
        if index >= self.strsz:
            raise ElfError(f"Dynamic string index outside DT_STRSZ: {index}")
        start = self.strtab + index
        end = self.data.find(b"\0", start, self.strtab + self.strsz)
        if end < 0:
            raise ElfError("Unterminated dynamic string")
        return bytes(self.data[start:end])

    def get_verneed(self):
        has = DT_VERNEED in self.tags
        if has != (DT_VERNEEDNUM in self.tags):
            raise ElfError("Incomplete DT_VERNEED / DT_VERNEEDNUM pair")
        if not has:
            return []
        count = self.one(DT_VERNEEDNUM)
        if count > 4096:
            raise ElfError("Invalid DT_VERNEEDNUM")
        base = self.one(DT_VERNEED)
        offsets = []
        cursor = 0
        for i in range(count):
            pos = self.va_to_offset(base + cursor, 16)
            ver, _, name, aux, nxt = struct.unpack_from("<HHIII", self.data, pos)
            if ver != 1:
                raise ElfError("Unsupported VERNEED version")
            if aux == 0:
                raise ElfError("Malformed VERNEED auxiliary offset")
            offsets.append((name, self.getstr(name)))
            if i != count - 1 and nxt == 0:
                raise ElfError("VERNEED entries terminate early")
            cursor += nxt
        return offsets

    def assert_valid_dependencies(self):
        needed_set = set(self.needed)
        for _, name in self.verneed:
            if name not in needed_set:
                raise ElfError(
                    f"ELF version requirement {name!r} does not match a DT_NEEDED dependency"
                )

    def replace(self, old, new):
        if len(new) > len(old):
            raise ElfError("Replacement would enlarge .dynstr: refused")
        offsets = set()
        for idx in self.tags.get(DT_NEEDED, []):
            if self.getstr(idx) == old:
                offsets.add(idx)
        for idx in self.tags.get(DT_SONAME, []):
            if self.getstr(idx) == old:
                offsets.add(idx)
        for idx, name in self.verneed:
            if name == old:
                offsets.add(idx)
        for idx in offsets:
            start = self.strtab + idx
            self.data[start:start + len(old) + 1] = (
                new + b"\0" * (len(old) - len(new) + 1)
            )
        return len(offsets)


def inspect(directory):
    libs = sorted(directory.glob("*.so"))
    if not libs:
        raise ElfError("No native libraries in target directory")
    objects = {path.name: Elf(path) for path in libs}
    for item in objects.values():
        item.assert_valid_dependencies()
    return objects


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("lib_dir", type=Path, help="AAR's jni/arm64-v8a directory")
    parser.add_argument("--verify", action="store_true", help="Verify an already patched directory")
    args = parser.parse_args()
    if not args.lib_dir.is_dir():
        raise ElfError("Missing jni/arm64-v8a directory")
    objects = inspect(args.lib_dir)
    ort = objects.get("libonnxruntime.so" if not args.verify else NEW.decode())
    jni = objects.get("libsherpa-onnx-jni.so")
    if ort is None or jni is None:
        raise ElfError("Missing sherpa runtime or JNI dependency")
    if not args.verify:
        if ort.soname != OLD:
            raise ElfError(f"Expected original ONNX Runtime SONAME {OLD!r}; got {ort.soname!r}")
        if OLD not in jni.needed:
            raise ElfError("Expected JNI direct dependency on original ONNX Runtime")
        patches = 0
        for elf in objects.values():
            patches += elf.replace(OLD, NEW)
            # Only write after all linked dependency names have been validated.
        for elf in objects.values():
            elf.path.write_bytes(elf.data)
        oldpath = args.lib_dir / OLD.decode()
        oldpath.rename(args.lib_dir / NEW.decode())
        print(f"Changed {patches} ELF dynstr entries without resizing native binaries")
        objects = inspect(args.lib_dir)
    ort = objects.get(NEW.decode())
    jni = objects.get("libsherpa-onnx-jni.so")
    if ort is None or jni is None or ort.soname != NEW or NEW not in jni.needed:
        raise ElfError("Isolated SONAME / JNI dependency verification failed")
    for elf in objects.values():
        if OLD in elf.needed or elf.soname == OLD or any(name == OLD for _, name in elf.verneed):
            raise ElfError(f"Unpatched sherpa ONNX Runtime reference in {elf.path.name}")
        if NEW in elf.needed and elf.path.name == NEW.decode():
            raise ElfError("ONNX Runtime must not link to itself")
    print(f"Verified {len(objects)} ELF libraries, all VERNEED target names match DT_NEEDED")


if __name__ == "__main__":
    try:
        main()
    except (ElfError, OSError, struct.error) as exc:
        print(f"sherpa ELF isolation failed: {exc}", file=sys.stderr)
        sys.exit(1)
