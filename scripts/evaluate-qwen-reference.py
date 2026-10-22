#!/usr/bin/env python3
"""Evaluate a separate host llama_batch/llama_decode path against S26 synthetic data.

IMPORTANT: compares execution pathways, not independent model mathematics. Identical
logits do not prove official Qwen M-RoPE. Never prints token sequences or raw logits.
"""
import argparse
import hashlib
import json
import math
import struct
from pathlib import Path

VOCAB = 151936
N = 5
EXPECT = {
    's26-prefill-logits.f32le': '9951cd0ed4143a97f4181c3fe0b87086aaf7f77b8571c63ea41d5597ef41a8e2',
    's26-synthetic-features.f32le': 'e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee',
}

def digest(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()

def verified_fixture(directory):
    root = Path(directory)
    got = {}
    for name, sha in EXPECT.items():
        p = root / name
        if not p.is_file() or digest(p) != sha:
            raise ValueError(f'fixture_missing_or_sha256_mismatch:{name}')
        got[name] = sha
    if (root / 's26-prefill-logits.f32le').stat().st_size != VOCAB * 4:
        raise ValueError('s26_logits_bad_length')
    if (root / 's26-synthetic-features.f32le').stat().st_size != 156 * 1024 * 4:
        raise ValueError('s26_features_bad_length')
    return got

def read(path):
    data = Path(path).read_bytes()
    if len(data) != VOCAB * 4:
        raise ValueError(f'invalid_logit_length:{Path(path).name}:{len(data)}')
    values = struct.unpack('<' + str(VOCAB) + 'f', data)
    if not all(math.isfinite(v) for v in values):
        raise ValueError(f'nonfinite_logits:{Path(path).name}')
    return values, hashlib.sha256(data).hexdigest()

def summarize(a, b):
    x, hx = a
    y, hy = b
    dot = sum(l*r for l, r in zip(x, y))
    aa = sum(l*l for l in x)
    bb = sum(r*r for r in y)
    err = sum((l-r)**2 for l, r in zip(x,y))
    order_x = sorted(range(VOCAB), key=lambda i: x[i], reverse=True)[:5]
    order_y = sorted(range(VOCAB), key=lambda i: y[i], reverse=True)[:5]
    return {
        'byte_identical': hx == hy,
        'rmse': math.sqrt(err / VOCAB),
        'max_abs_delta': max(abs(l-r) for l,r in zip(x,y)),
        'cosine': dot / math.sqrt(aa*bb) if aa > 0 and bb > 0 else None,
        'top1_equal': order_x[0] == order_y[0],
        'top5_overlap': len(set(order_x) & set(order_y)),
        'left_sha256': hx,
        'right_sha256': hy,
    }

def inspect(fixtures, primary, replay, shifted, library_rev):
    sources = verified_fixture(fixtures)
    primary, replay, shifted = map(Path, (primary, replay, shifted))
    for sub in (primary, replay, shifted):
        if not sub.is_dir():
            raise ValueError(f'reference_folder_missing:{sub.name}')
    results = []
    for i in range(N):
        name = f'logits-step-{i:02d}.f32le'
        original = read(primary/name)
        repeated = read(replay/name)
        sensitivity = read(shifted/name)
        row = {'step': i,
               'reference_replay': summarize(original, repeated),
               'audio_axis_shift_sensitivity': summarize(original, sensitivity)}
        if i == 0:
            row['s26_vs_reference_first_logits'] = summarize(
                read(Path(fixtures)/'s26-prefill-logits.f32le'), original)
        results.append(row)
    exact_replay = all(r['reference_replay']['byte_identical'] for r in results)
    any_axis_sensitivity = any(not r['audio_axis_shift_sensitivity']['byte_identical'] for r in results)
    return {
        'schema': 1,
        'status': 'DIAGNOSTIC_ONLY' if exact_replay else 'FAIL_REFERENCE_REPLAY',
        'comparison_scope': 'Separate host C++ bridge, standard llama_decode API, shared llama.cpp model core',
        'official_qwen_parity': 'NOT_VERIFIED',
        'official_mrope_parity': 'UNQUALIFIED',
        'llama_cpp_revision': library_rev,
        'fixture_sha256': sources,
        'host_reference_replay_bitwise_identical': exact_replay,
        'audio_axis_sensitivity_detected': any_axis_sensitivity,
        'note': ('Even a perfect first-step match with S26 establishes cross-runtime '
                 'execution parity only for supplied linear4 positions. '
                 'Later steps are reference-only until equivalent S26 teacher-forced '
                 'logit checkpoints exist. Axis shift is a negative-control '
                 'perturbation, not the official mapping.'),
        'positions': results,
    }

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixtures', required=True)
    parser.add_argument('--primary', required=True)
    parser.add_argument('--replay', required=True)
    parser.add_argument('--shifted', required=True)
    parser.add_argument('--llama-revision', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    try:
        result = inspect(args.fixtures,args.primary,args.replay,args.shifted,args.llama_revision)
    except Exception as e:
        result = {'schema':1, 'status':'INCONCLUSIVE', 'reason':str(e),
                  'official_qwen_parity':'NOT_VERIFIED', 'official_mrope_parity':'UNQUALIFIED'}
        Path(args.output).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output).write_text(json.dumps(result,indent=2)+'\n')
        print(result['status']+':'+str(result['reason']))
        raise SystemExit(2)
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(json.dumps(result,indent=2)+'\n')
    p = result['positions'][0]['s26_vs_reference_first_logits']
    print('reference_status='+result['status'])
    print('host_replay_identical='+str(result['host_reference_replay_bitwise_identical']).lower())
    print('s26_first_token_top1_equal='+str(p['top1_equal']).lower())
    print('s26_first_logit_rmse='+str(p['rmse']))
    print('s26_first_logit_max_abs_delta='+str(p['max_abs_delta']))
    print('audio_position_sensitivity='+str(result['audio_axis_sensitivity_detected']).lower())
    print('official_qwen_mrope=UNQUALIFIED')
    if not result['host_reference_replay_bitwise_identical']:
        raise SystemExit(1)

if __name__ == '__main__':
    main()
