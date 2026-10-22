#!/usr/bin/env python3
"""6.6.12: same-host controlled 2x3 microbatch and CPU-thread comparison.

Separate the size of the llama.cpp physical microbatch from its CPU threading.
This diagnoses execution-schedule sensitivity, not official transformer correctness.
Only checkpoint 0 is comparable to the existing S26 checkpoint.
"""
import argparse
import importlib.util
import json
from pathlib import Path

module = Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec = importlib.util.spec_from_file_location('qwen_6612_previous', module)
old = importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)

# n_batch remains fixed at 256 and all other inference parameters are unchanged.
CASES = ('unified_base', 'three_stage_base',
         'unified_micro64', 'three_stage_micro64',
         'unified_thread1', 'three_stage_thread1',
         'unified_base_replay', 'three_stage_base_replay',
         'unified_micro64_replay')
PATHS = ('unified', 'three_stage')


def compare(a, b):
    return old.comparison(a, b)


def status_of(rows):
    if len(rows) != old.legacy.N:
        return 'INCONCLUSIVE_INCOMPLETE', 'Five checkpoints required.'
    if not all(row['unified_base_replay']['byte_identical'] and
               row['three_stage_base_replay']['byte_identical'] and
               row['unified_micro64_replay']['byte_identical'] for row in rows):
        return 'FAIL_REPLAY', 'A repeated host run was not numerically identical.'
    if all(row['base_grouping']['byte_identical'] for row in rows):
        return 'INCONCLUSIVE_BASELINE_CHANGED', 'The original unified vs three-stage difference did not reproduce.'
    ub = any(not row['microbatch_effect_unified']['byte_identical'] or
             not row['microbatch_effect_three_stage']['byte_identical'] for row in rows)
    th = any(not row['thread_effect_unified']['byte_identical'] or
             not row['thread_effect_three_stage']['byte_identical'] for row in rows)
    if ub and th:
        return 'BOTH_MICROBATCH_AND_THREAD_SENSITIVE', 'Both interventions change host logits; neither proves a model error.'
    if ub:
        return 'MICROBATCH_SENSITIVE', 'Microbatch size changes host logits under fixed threading.'
    if th:
        return 'THREAD_SENSITIVE', 'Thread count changes host logits under fixed microbatch size.'
    return 'NO_MICROBATCH_OR_THREAD_EFFECT', 'Grouping discrepancy persists but neither tested knob changes logits.'


def evaluate(fixtures, paths, revision):
    hashes = old.legacy.verified_fixture(Path(fixtures))
    dirs = {key: Path(paths[key]) for key in CASES}
    for name, folder in dirs.items():
        if not folder.is_dir():
            raise ValueError('MISSING_OUTPUT_DIRECTORY:' + name)
    phone = old.legacy.read(Path(fixtures) / 's26-prefill-logits.f32le')
    rows = []
    for index in range(old.legacy.N):
        name = f'logits-step-{index:02d}.f32le'
        x = {key: old.legacy.read(folder / name) for key, folder in dirs.items()}
        row = {
            'step': index,
            'base_grouping': compare(x['unified_base'], x['three_stage_base']),
            'micro64_grouping': compare(x['unified_micro64'], x['three_stage_micro64']),
            'thread1_grouping': compare(x['unified_thread1'], x['three_stage_thread1']),
            'microbatch_effect_unified': compare(x['unified_base'], x['unified_micro64']),
            'microbatch_effect_three_stage': compare(x['three_stage_base'], x['three_stage_micro64']),
            'thread_effect_unified': compare(x['unified_base'], x['unified_thread1']),
            'thread_effect_three_stage': compare(x['three_stage_base'], x['three_stage_thread1']),
            'unified_base_replay': compare(x['unified_base'], x['unified_base_replay']),
            'three_stage_base_replay': compare(x['three_stage_base'], x['three_stage_base_replay']),
            'unified_micro64_replay': compare(x['unified_micro64'], x['unified_micro64_replay']),
        }
        if index == 0:
            for case in ('unified_base', 'three_stage_base', 'unified_micro64',
                         'three_stage_micro64', 'unified_thread1', 'three_stage_thread1'):
                row['s26_vs_' + case] = compare(phone, x[case])
        rows.append(row)
    status, detail = status_of(rows)
    return {
        'schema': 1,
        'release': '6.6.12',
        'status': status,
        'conclusion': detail,
        'experiment': 'same-host n_ubatch and CPU-thread sensitivity with fixed n_batch 256',
        'configurations': {
            'base': {'n_batch': 256, 'n_ubatch': 256, 'n_threads': 4},
            'micro64': {'n_batch': 256, 'n_ubatch': 64, 'n_threads': 4},
            'thread1': {'n_batch': 256, 'n_ubatch': 256, 'n_threads': 1},
        },
        'modes': {'unified': '[prefix+audio+suffix]', 'three_stage': '[prefix][audio][suffix]'},
        'model_math_independent': False,
        'official_qwen_parity': 'NOT_VERIFIED',
        'mrope_parity': 'UNQUALIFIED',
        'phone_only_step00': True,
        'upstream_revision': revision,
        'fixture_sha256': hashes,
        'steps': rows,
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--fixtures', required=True)
    for key in CASES:
        p.add_argument('--'+key.replace('_','-'), dest=key, required=True)
    p.add_argument('--llama-revision', required=True)
    p.add_argument('--output', required=True)
    args = p.parse_args()
    dest = Path(args.output)
    dest.parent.mkdir(parents=True, exist_ok=True)
    try:
        report = evaluate(args.fixtures, {key: getattr(args,key) for key in CASES}, args.llama_revision)
    except (OSError, ValueError, OverflowError) as exc:
        report = {'schema':1, 'release':'6.6.12', 'status':'INCONCLUSIVE',
                  'reason':str(exc), 'model_math_independent':False,
                  'official_qwen_parity':'NOT_VERIFIED', 'mrope_parity':'UNQUALIFIED'}
    dest.write_text(json.dumps(report, indent=2) + '\n')
    print('execution_sensitivity_status='+report['status'])
    if report.get('steps'):
        print('baseline_grouping_step00_rmse='+str(report['steps'][0]['base_grouping']['rmse']))
        for x in ('unified_base','three_stage_base','unified_micro64','three_stage_micro64','unified_thread1','three_stage_thread1'):
            print('s26_'+x+'_step00_rmse='+str(report['steps'][0]['s26_vs_'+x]['rmse']))
    if report['status'].startswith(('INCONCLUSIVE','FAIL')):
        raise SystemExit(2)

if __name__ == '__main__':
    main()
