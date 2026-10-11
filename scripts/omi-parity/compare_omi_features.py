#!/usr/bin/env python3
"""Compare Kotlin/Android-generated Omi Mel features to an INDEPENDENT reference.

Reference directory must contain {case}.features.f32le and masks with identical
128x1201 channel-major layout and float32 little-endian dtype.

A NumPy cross-check mode can validate our mathematical port independently, but
DOES NOT establish canonical NeMo or model parity. No patient audio is included.
"""
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np

CASES = ('silence_1s', 'impulse_1s', 'tone_12s', 'noise_partial')
PHYSICAL = 1201


def load(base, name, suffix):
    filename = base / f'{name}.{suffix}.f32le'
    if not filename.is_file():
        raise FileNotFoundError(filename)
    return np.fromfile(filename, dtype='<f4')


def stats(a, b, valid):
    if a.shape != b.shape:
        raise ValueError(f'shape differs: {a.shape} vs {b.shape}')
    if not np.isfinite(a).all() or not np.isfinite(b).all():
        raise ValueError('Nonfinite feature detected')
    active = (a.reshape(128, PHYSICAL)[:, :valid].astype(np.float64) -
              b.reshape(128, PHYSICAL)[:, :valid].astype(np.float64))
    d = np.abs(active)
    index = np.unravel_index(int(d.argmax()), d.shape)
    return {'mae': float(d.mean()), 'rmse': float(np.sqrt(np.mean(active * active))),
            'max_abs': float(d.max()), 'worst_mel': int(index[0]),
            'worst_frame': int(index[1]), 'equal_exact': bool(np.all(active == 0))}


def compare(android, reference):
    if android.resolve() == reference.resolve():
        raise ValueError('Android and reference directories must be independent')
    manifest_path = reference / 'reference-manifest.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.is_file() else None
    report = {}
    for name in CASES:
        pcm = load(android, name, 'pcm')
        if not np.isfinite(pcm).all():
            raise ValueError(f'{name}: nonfinite PCM')
        if manifest is not None:
            expected_hash = manifest.get('cases', {}).get(name, {}).get('pcm_sha256')
            if expected_hash is not None:
                actual_hash = hashlib.sha256((android / f'{name}.pcm.f32le').read_bytes()).hexdigest()
                if expected_hash != actual_hash:
                    raise ValueError(f'{name}: reference built from different PCM bytes')
        valid = min(PHYSICAL-1, pcm.size // 160)
        current = load(android, name, 'features')
        ref = load(reference, name, 'features')
        if current.size != 128 * PHYSICAL or ref.size != 128 * PHYSICAL:
            raise ValueError(f'{name}: incorrect feature tensor length')
        current_view = current.reshape(128, PHYSICAL)
        pad_nonzero = int(np.count_nonzero(current_view[:, valid:]))
        ref_pad_nonzero = int(np.count_nonzero(ref.reshape(128, PHYSICAL)[:, valid:]))
        masks = {}
        for stage, size in (('mask1', 601), ('mask2', 301), ('mask3', 151)):
            x, y = load(android, name, stage), load(reference, name, stage)
            if x.size != size or y.size != size:
                raise ValueError(f'{name}/{stage}: mask length mismatch')
            masks[stage] = {'mismatch_count': int(np.count_nonzero(x != y))}
        report[name] = {'samples': int(pcm.size), 'valid_frames': valid,
                        'features': stats(current, ref, valid),
                        'android_pad_nonzero': pad_nonzero, 'reference_pad_nonzero': ref_pad_nonzero,
                        'mask': masks}
    return report, (manifest or {'reference_kind': 'unverified_external_no_manifest',
                                 'canonical_model_checked': False})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--android-dir', required=True, type=Path)
    parser.add_argument('--reference-dir', required=True, type=Path,
                        help='Canonical reference output in the same tensor/byte contract')
    parser.add_argument('--out', type=Path)
    args = parser.parse_args()
    cases, provenance = compare(args.android_dir, args.reference_dir)
    result = {'qualification': 'independent-feature-math-comparison-only',
              'canonical_model_parity': 'NOT VERIFIED',
              'medical_transcription_accuracy': 'NOT MEASURED',
              'reference': str(args.reference_dir),
              'reference_kind': provenance.get('reference_kind', 'unknown'),
              'reference_claims_canonical_checked': provenance.get('canonical_model_checked', False),
              'cases': cases}
    content = json.dumps(result, indent=2, sort_keys=True)
    if args.out:
        args.out.write_text(content + '\n')
    print(content)


if __name__ == '__main__':
    main()
