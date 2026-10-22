#!/usr/bin/env python3
"""Numerical checks for v6.6.9 split vs mixed same-host llama.cpp batching.

Matches the exact S26 first-token checkpoint where present. Shared transformer
mathematics means this is a batching-path test, NOT official Qwen parity.
"""
import argparse
import importlib.util
import json
import math
from pathlib import Path

P = Path(__file__).with_name('evaluate-qwen-reference.py')
SPEC = importlib.util.spec_from_file_location('qwen_v668', P)
LEGACY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(LEGACY)


def centered_rmse(a, b):
    left = a[0]
    right = b[0]
    n = len(left)
    assert len(right) == n and n > 0
    offset = sum(x-y for x, y in zip(left, right))/n
    rmse = math.sqrt(sum(((x-y)-offset)**2 for x, y in zip(left,right))/n)
    return {'mean_left_minus_right': offset, 'mean_centered_rmse': rmse}


def infer(batch_exact, split_s26, mixed_s26, mixed_repeat):
    if not mixed_repeat:
        return ('FAIL_MIXED_REPLAY', 'Do not interpret a mixed-batch run that cannot replay deterministically.')
    if batch_exact:
        return ('BATCH_PATH_IDENTICAL', 'The two host batch APIs agree exactly. Investigate ARM64 vs x86 backend, rounding and data provenance before modifying Android.')
    split = split_s26['mean_centered_rmse']
    mixed = mixed_s26['mean_centered_rmse']
    if mixed < split*0.8:
        return ('BATCH_PATH_DIFFERENCE_MIXED_CLOSER_TO_S26', 'Mixed API is meaningfully closer to S26. Inspect split batching and position-plane semantics; do not modify Android without controlled confirmation.')
    if split < mixed*0.8:
        return ('BATCH_PATH_DIFFERENCE_SPLIT_CLOSER_TO_S26', 'Split API is meaningfully closer to S26. Inspect extended batching and logits access; do not modify Android without controlled confirmation.')
    return ('BATCH_PATH_DIFFERENCE_NO_CLEAR_S26_WINNER', 'Both host APIs differ and neither is clearly closer to S26. Inspect position maps, logit offsets and runtime optimizations.')


def evaluation(fixtures, split, mixed, replay, revision):
    ids = LEGACY.verified_fixture(fixtures)
    dirs = [Path(v) for v in (split, mixed, replay)]
    for d in dirs:
        if not d.is_dir():
            raise ValueError('missing_output_directory:'+d.name)
    s26 = LEGACY.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows = []
    for step in range(LEGACY.N):
        filename = f'logits-step-{step:02d}.f32le'
        a,b,c = (LEGACY.read(path/filename) for path in dirs)
        sb = LEGACY.summarize(a,b)
        sb.update(centered_rmse(a,b))
        replay_cmp = LEGACY.summarize(b,c)
        row = {'step':step,'split_vs_mixed':sb,'mixed_replay':replay_cmp}
        if step == 0:
            sa = LEGACY.summarize(s26,a)
            sa.update(centered_rmse(s26,a))
            sm = LEGACY.summarize(s26,b)
            sm.update(centered_rmse(s26,b))
            row['s26_vs_split']=sa
            row['s26_vs_mixed']=sm
        rows.append(row)
    is_repeat=all(row['mixed_replay']['byte_identical'] for row in rows)
    exact = all(row['split_vs_mixed']['byte_identical'] for row in rows)
    code, advice = infer(exact, rows[0]['s26_vs_split'], rows[0]['s26_vs_mixed'], is_repeat)
    return {
        'schema':1,'status':code,'conclusion':advice,
        'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED',
        'mrope_parity':'UNQUALIFIED',
        'experimental_scope':'Same CPU/model/revision: standard llama_decode split batches vs llama_process extended mixed batch',
        'upstream_revision':revision,
        'fixture_hashes':ids,
        'batch_paths_identical_at_all_five_steps':exact,
        'mixed_replay_identical_at_all_five_steps':is_repeat,
        'steps': rows,
    }


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    for p in ('fixtures','split','mixed','replay','llama-revision','output'):
        ap.add_argument('--'+p, required=True)
    args=ap.parse_args()
    output=Path(args.output)
    output.parent.mkdir(parents=True,exist_ok=True)
    try:
        r=evaluation(args.fixtures,args.split,args.mixed,args.replay,args.llama_revision)
    except Exception as e:
        r={'schema':1,'status':'INCONCLUSIVE','reason':str(e),
           'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED',
           'mrope_parity':'UNQUALIFIED'}
    output.write_text(json.dumps(r,indent=2)+'\n')
    print('batch_comparison_status='+r['status'])
    if 'steps' in r:
        print('first_host_batch_rmse='+str(r['steps'][0]['split_vs_mixed']['rmse']))
        print('s26_vs_mixed_rmse='+str(r['steps'][0]['s26_vs_mixed']['rmse']))
        print('s26_vs_split_rmse='+str(r['steps'][0]['s26_vs_split']['rmse']))
        print('mixed_replay_identical='+str(r['mixed_replay_identical_at_all_five_steps']).lower())
    print('official_qwen_parity=NOT_VERIFIED')
    if r['status'] in ('INCONCLUSIVE','FAIL_MIXED_REPLAY'):
        raise SystemExit(2)

if __name__=='__main__':
    main()
