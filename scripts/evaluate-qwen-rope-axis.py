#!/usr/bin/env python3
"""GGUF-only 4-axis position-attribution, with strict vector and graph controls.

Compares each position-map response against the *same* attention-path baseline.
No HF model, Android inference, private audio, or official M-RoPE oracle is used.
"""
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np

N = 151936
PREFIXES = ('pos-plus17', 'pos-suffix-gap11')
ATTN = ('flash-off', 'flash-on')
MAPS = ('pos-linear', 'pos-plus1') + tuple(
    x for group in PREFIXES for x in ((group,) + tuple(f'{group}-axis{i}' for i in range(4)))
)
STATUS = 'MROPE_AXIS_ATTRIBUTION_COMPLETE'


def vector(path):
    if not path.is_file() or path.stat().st_size != N * 4:
        raise ValueError('MISSING_OR_WRONG_VECTOR_SIZE')
    blob = path.read_bytes()
    v = np.frombuffer(blob, dtype='<f4')
    if len(v) != N or not np.isfinite(v).all():
        raise ValueError('NONFINITE_OR_WRONG_VECTOR')
    return v.astype(np.float64), hashlib.sha256(blob).hexdigest()


def rmse(a):
    return float(np.sqrt(np.mean(a * a)))


def cosine(a, b):
    na = np.linalg.norm(a)
    nb = np.linalg.norm(b)
    return float(np.dot(a, b) / (na * nb)) if na > 0 and nb > 0 else None


def analyze(root):
    result = {
        'schema': 1, 'release': '6.6.27',
        'status': 'INCONCLUSIVE_INCOMPLETE',
        'experiment': 'GGUF_BF16_MROPE_PER_AXIS_POSITION_ATTRIBUTION',
        'official_audio_mrope_parity': 'NOT_VERIFIED',
        'synthetic_only': True, 'android_inference_changed': False,
        'model_sha256': '12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50',
        'features_sha256': 'e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee',
        'modes': list(MAPS), 'missing': [], 'vectors': {}, 'responses': {},
        'precision_context': {
            'hf_fp32_plus17_delta_rmse_v6626': 2.1776339249821997e-05,
            'hf_fp32_plus1_delta_rmse_v6626': 1.698788686987822e-05,
            'gguf_flash_on_plus17_delta_rmse_v6626': 0.08442166178509827,
            'gguf_flash_off_plus17_delta_rmse_v6626': 0.07468351354841225,
            'note': 'v6.6.26 comparison only, not re-executed or required for this artifact',
        },
    }
    vectors = {}
    for mode in MAPS:
        for attn in ATTN:
            fn = root / 'host' / mode / attn / 'logits-step-00.f32le'
            log = root / 'host' / mode / attn / 'inference.log'
            name = f'{mode}/{attn}'
            try:
                vec, sha = vector(fn)
                if not log.is_file(): raise ValueError('MISSING_EXECUTION_LOG')
                lt = log.read_text(errors='replace')
                if f'pos_mode={mode}' not in lt or 'HOST_EXTENDED_BATCH_EXECUTED' not in lt:
                    raise ValueError('HOST_RUNNER_COMPLETION_OR_POSITION_UNVERIFIED')
                marker = 'SOFT_MAX' if attn == 'flash-off' else 'FLASH_ATTN_EXT'
                absent = 'FLASH_ATTN_EXT' if attn == 'flash-off' else 'SOFT_MAX'
                if marker not in lt or absent in lt:
                    raise ValueError('GRAPH_PATH_NOT_ISOLATED')
                vectors[name] = vec
                result['vectors'][name] = {
                    'sha256': sha, 'top1_token_id': int(np.argmax(vec)),
                    'graph_operation': marker,
                }
            except (ValueError, OSError) as e:
                result['missing'].append(f'{name}: {e}')
    for attn in ATTN:
        base = vectors.get('pos-linear/' + attn)
        if base is None: continue
        for group in PREFIXES:
            all_shift = vectors.get(group + '/' + attn)
            axes = [vectors.get(f'{group}-axis{i}/{attn}') for i in range(4)]
            if all_shift is None or any(x is None for x in axes): continue
            delta_all = all_shift - base
            axis_deltas = [v - base for v in axes]
            added = np.sum(np.stack(axis_deltas), axis=0)
            residual = delta_all - added
            result['responses'][f'{group}/{attn}'] = {
                'all_axis_rmse': rmse(delta_all),
                'single_axis_rmse': {str(i): rmse(d) for i, d in enumerate(axis_deltas)},
                'single_axis_vs_all_cosine': {str(i): cosine(d, delta_all) for i, d in enumerate(axis_deltas)},
                'sum_of_single_axis_rmse': rmse(added),
                'nonadditive_residual_rmse': rmse(residual),
                'nonadditive_residual_to_all_ratio': rmse(residual) / rmse(delta_all) if rmse(delta_all) else None,
                'sum_vs_all_cosine': cosine(added, delta_all),
                'all_axis_top1': int(np.argmax(all_shift)),
                'baseline_top1': int(np.argmax(base)),
                'individual_top1': [int(np.argmax(v)) for v in axes],
            }
    for mode in MAPS:
        off = vectors.get(mode + '/flash-off')
        on = vectors.get(mode + '/flash-on')
        if off is not None and on is not None:
            result['vectors'][mode + '/cross-attention'] = {
                'flash_on_vs_off_rmse': rmse(on-off),
                'top1_equal': bool(int(np.argmax(off)) == int(np.argmax(on))),
            }
    if not result['missing'] and len(result['responses']) == 2 * len(PREFIXES):
        result['status'] = STATUS
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--artifact-dir', required=True)
    ap.add_argument('--output', required=True)
    args = ap.parse_args()
    obj = analyze(Path(args.artifact_dir))
    out = Path(args.output);out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(obj, sort_keys=True, indent=2, allow_nan=False)+'\n')
    print('MROPE_AXIS_ATTRIBUTION',obj['status'],'complete',len(obj['vectors']),'missing',len(obj['missing']))
    if obj['status'] != STATUS: raise SystemExit(2)

if __name__ == '__main__': main()
