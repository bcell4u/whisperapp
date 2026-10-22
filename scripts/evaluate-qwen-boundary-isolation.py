#!/usr/bin/env python3
"""6.6.11: same llama.cpp API/CPU/model, isolate the TWO prompt batch cuts.

Does not prove model mathematical correctness, native ARM64 parity, or official M-RoPE.
Only first checkpoint can be compared with S26's existing logit reference.
"""
import argparse
import importlib.util
import json
from pathlib import Path

MODULE = Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec = importlib.util.spec_from_file_location('qwen_6610_isolation', MODULE)
prev = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prev)
legacy = prev.legacy

PATHS = ('split','unified','three_stage','prefix_audio','audio_suffix',
         'unified_replay','three_stage_replay','prefix_audio_replay','audio_suffix_replay')


def compare(a,b):
    return prev.comparison(a,b)


def status_of(rows):
    if not rows or len(rows) != legacy.N:
        return 'INCONCLUSIVE_INCOMPLETE', 'Expected exactly five checkpoints.'
    if not all(all(row[mode+'_replay']['byte_identical'] for mode in
                   ('unified','three_stage','prefix_audio','audio_suffix')) for row in rows):
        return 'FAIL_REPLAY', 'Host replay mismatch: grouping comparison is not trustworthy.'
    if not all(row['split_vs_three_stage']['byte_identical'] for row in rows):
        return 'INCONCLUSIVE_REFERENCE_CHANGED', 'Standard and extended 3-stage paths no longer agree on all checkpoints.'
    eq = lambda key: all(row[key]['byte_identical'] for row in rows)
    if eq('unified_vs_three_stage'):
        return 'ALL_GROUPINGS_IDENTICAL', 'The original unified vs staged discrepancy is not reproduced.'
    ua = eq('unified_vs_prefix_audio')
    ub = eq('unified_vs_audio_suffix')
    ta = eq('three_stage_vs_prefix_audio')
    tb = eq('three_stage_vs_audio_suffix')
    if ua and tb:
        return 'PREFIX_AUDIO_CUT_DOMINANT', 'Grouping after prefix explains this host discrepancy: modes without that cut equal unified and modes with it equal staged.'
    if ub and ta:
        return 'AUDIO_SUFFIX_CUT_DOMINANT', 'Grouping after audio explains this host discrepancy: modes without that cut equal unified and modes with it equal staged.'
    if ta and tb:
        return 'EITHER_SINGLE_CUT_SUFFICIENT', 'Either one of the two cuts reproduces the 3-stage result.'
    if ua and ub:
        return 'BOTH_CUTS_REQUIRED', 'Both single-cut paths equal unified, while only 3-stage differs.'
    return 'GROUPING_INTERACTION_UNRESOLVED', 'One- and two-cut paths differ; inspect numerical distances without inferring mathematical correctness.'


def evaluate(fixtures, paths, revision):
    hashes=legacy.verified_fixture(Path(fixtures))
    dirs={key:Path(paths[key]) for key in PATHS}
    for key,path in dirs.items():
        if not path.is_dir():
            raise ValueError('MISSING_OUTPUT_DIRECTORY:'+key)
    phone=legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows=[]
    for index in range(legacy.N):
        name=f'logits-step-{index:02d}.f32le'
        data={key:legacy.read(path/name) for key,path in dirs.items()}
        row={'step':index,
            'split_vs_three_stage':compare(data['split'],data['three_stage']),
            'unified_vs_three_stage':compare(data['unified'],data['three_stage']),
            'unified_vs_prefix_audio':compare(data['unified'],data['prefix_audio']),
            'unified_vs_audio_suffix':compare(data['unified'],data['audio_suffix']),
            'three_stage_vs_prefix_audio':compare(data['three_stage'],data['prefix_audio']),
            'three_stage_vs_audio_suffix':compare(data['three_stage'],data['audio_suffix']),
        }
        for key in ('unified','three_stage','prefix_audio','audio_suffix'):
            row[key+'_replay']=compare(data[key],data[key+'_replay'])
            if index==0:
                row['s26_vs_'+key]=compare(phone,data[key])
        rows.append(row)
    code,detail=status_of(rows)
    return {'schema':1,'release':'6.6.11','status':code,'conclusion':detail,
            'experimental_scope':'same-host 4 extended batching groupings, plus standard split control',
            'grouping_modes':{'unified':'[prefix+audio+suffix]',
                              'prefix_audio':'[prefix+audio][suffix]',
                              'audio_suffix':'[prefix][audio+suffix]',
                              'three_stage':'[prefix][audio][suffix]'},
            'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED',
            'mrope_parity':'UNQUALIFIED','phone_only_step00':True,
            'upstream_revision':revision,'fixtures_sha256':hashes,'steps':rows}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--fixtures',required=True)
    for key in PATHS:
        p.add_argument('--'+key.replace('_','-'),dest=key,required=True)
    p.add_argument('--llama-revision',required=True)
    p.add_argument('--output',required=True)
    args=p.parse_args()
    output=Path(args.output)
    output.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,{key:getattr(args,key) for key in PATHS},args.llama_revision)
    except (OSError,ValueError,OverflowError) as err:
        report={'schema':1,'release':'6.6.11','status':'INCONCLUSIVE',
                'reason':str(err),'model_math_independent':False,
                'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    output.write_text(json.dumps(report,indent=2)+'\n')
    print('boundary_isolation_status='+report['status'])
    if report.get('steps'):
        for key in ('unified','three_stage','prefix_audio','audio_suffix'):
            print('s26_'+key+'_step00_rmse='+str(report['steps'][0]['s26_vs_'+key]['rmse']))
    if report['status'].startswith(('FAIL_','INCONCLUSIVE')):
        raise SystemExit(2)

if __name__=='__main__':
    main()
