#!/usr/bin/env python3
"""6.6.10: same-host three-path prompt chunking versus llama.cpp API.

The model, CPU, checkpoint token sequence, and synthetic embeddings are shared.
Only prompt grouping or the public batching API changes. This does not establish
independent Qwen model math or correctness of official M-RoPE.
"""
import argparse
import importlib.util
import json
from pathlib import Path

_SOURCE = Path(__file__).with_name('evaluate-qwen-batch-paths.py')
_spec = importlib.util.spec_from_file_location('qwen_669_comparator', _SOURCE)
base = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(base)
legacy = base.LEGACY


def comparison(a, b):
    result = legacy.summarize(a, b)
    result.update(base.centered_rmse(a, b))
    return result


def classify(steps):
    """Use agreement and centered deltas to TRIAGE, not certify, a batching cause."""
    if not all(r['unified_replay']['byte_identical'] and r['three_stage_replay']['byte_identical'] for r in steps):
        return ('FAIL_REPLAY', 'Same-host execution was not deterministic. No numerical inference is justified.')
    su = [r['split_vs_unified'] for r in steps]
    sc = [r['split_vs_three_stage'] for r in steps]
    uc = [r['unified_vs_three_stage'] for r in steps]
    if all(x['byte_identical'] for x in su+sc+uc):
        return ('ALL_THREE_IDENTICAL', 'No batching disagreement reproduced on this run; investigate other runtime differences.')
    if all(x['byte_identical'] for x in sc):
        return ('CHUNK_GROUPING_EXPLAINS_DIFFERENCE', 'Three-stage extended and three-stage standard agree exactly, while unified differs. Grouping is the isolated factor on this host.')
    if all(x['byte_identical'] for x in uc):
        return ('API_PATH_EXPLAINS_DIFFERENCE', 'Both extended modes agree exactly, while the standard split mode differs. API semantics or its flags are the isolated factor on this host.')
    # Aggregate centered RMS across all five checkpoints, without treating a
    # constant logit shift as a probability-distorting difference.
    split_distance = sum(x['mean_centered_rmse']**2 for x in sc)
    unified_distance = sum(x['mean_centered_rmse']**2 for x in uc)
    if split_distance < unified_distance * 0.25:
        return ('CHUNK_GROUPING_SIMILAR_TO_SPLIT', 'Three-stage extended is numerically much closer to standard split. Inspect grouping and batch kernel dimensions; not official parity.')
    if unified_distance < split_distance * 0.25:
        return ('CHUNK_GROUPING_SIMILAR_TO_UNIFIED', 'Three-stage extended is numerically much closer to unified extended. Inspect API-specific input handling; not official parity.')
    return ('THREE_WAY_DIFFERENCE_UNRESOLVED', 'All paths differ; no isolated cause. Inspect batching, RoPE planes, and backend rounding before altering Android.')


def evaluate(fixtures, split, unified, three_stage, unified_replay, three_stage_replay, revision):
    hashes = legacy.verified_fixture(fixtures)
    paths = {name: Path(path) for name,path in {
        'split':split, 'unified':unified, 'three_stage':three_stage,
        'unified_replay':unified_replay, 'three_stage_replay':three_stage_replay,
    }.items()}
    for name,path in paths.items():
        if not path.is_dir():
            raise ValueError(f'MISSING_OUTPUT_DIRECTORY:{name}')
    phone = legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows = []
    for step in range(legacy.N):
        name = f'logits-step-{step:02d}.f32le'
        data = {key: legacy.read(path/name) for key,path in paths.items()}
        row = {'step':step,
            'split_vs_unified':comparison(data['split'], data['unified']),
            'split_vs_three_stage':comparison(data['split'], data['three_stage']),
            'unified_vs_three_stage':comparison(data['unified'],data['three_stage']),
            'unified_replay':comparison(data['unified'],data['unified_replay']),
            'three_stage_replay':comparison(data['three_stage'],data['three_stage_replay']),
        }
        if step==0:
            for key in ('split','unified','three_stage'):
                row['s26_vs_'+key] = comparison(phone,data[key])
        rows.append(row)
    code, detail = classify(rows)
    return {
        'schema':1, 'release':'6.6.10', 'status':code, 'conclusion':detail,
        'experimental_scope':'same-host standard three-stage vs extended unified vs extended three-stage',
        'model_math_independent':False, 'official_qwen_parity':'NOT_VERIFIED',
        'mrope_parity':'UNQUALIFIED', 'upstream_revision':revision,
        'fixtures_sha256':hashes,
        'all_unified_replays_identical':all(r['unified_replay']['byte_identical'] for r in rows),
        'all_three_stage_replays_identical':all(r['three_stage_replay']['byte_identical'] for r in rows),
        'steps':rows,
    }


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for key in ('fixtures','split','unified','three-stage','unified-replay','three-stage-replay','llama-revision','output'):
        parser.add_argument('--'+key, required=True)
    args=parser.parse_args()
    destination=Path(args.output)
    destination.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,args.split,args.unified,args.three_stage,args.unified_replay,args.three_stage_replay,args.llama_revision)
    except Exception as error:
        report={'schema':1,'release':'6.6.10', 'status':'INCONCLUSIVE',
                'reason':str(error),'model_math_independent':False,
                'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    destination.write_text(json.dumps(report,indent=2)+'\n')
    print('chunk_isolation_status='+report['status'])
    if 'steps' in report:
        first=report['steps'][0]
        for name in ('split_vs_unified','split_vs_three_stage','unified_vs_three_stage',
                     's26_vs_split','s26_vs_unified','s26_vs_three_stage'):
            print(name+'_rmse='+str(first[name]['rmse']))
    print('official_qwen_parity=NOT_VERIFIED')
    if report['status'] in ('FAIL_REPLAY','INCONCLUSIVE'):
        raise SystemExit(2)

if __name__=='__main__':
    main()
