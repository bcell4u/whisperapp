#!/usr/bin/env python3
"""6.6.13: isolate numerical sensitivity around 156 audio / 171 prompt tokens.

All 13 executions use the same host CPU, same GGUF, same synthetic features,
same prompt, and four decoding threads. Threshold effects do NOT independently
validate official Qwen model mathematics or M-RoPE.
"""
import argparse
import importlib.util
import json
from pathlib import Path

path=Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec=importlib.util.spec_from_file_location('ref6613_base',path)
old=importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)

UBATCHES=(64,128,160,192,256)
MODES=('unified','three_stage')
KEYS=tuple(f'{mode}_u{size}' for mode in MODES for size in UBATCHES)
REPLAYS=('unified_u256_replay','three_stage_u256_replay','unified_u160_replay')
STEP_COUNT=5


def cmp(a,b):
    return old.comparison(a,b)


def classify(rows):
    if len(rows)!=STEP_COUNT:
        return 'INCONCLUSIVE_INCOMPLETE','Five complete numerical checkpoints required.'
    if any(not row[key]['byte_identical'] for row in rows
           for key in ('replay_unified_256','replay_three_stage_256','replay_unified_160')):
        return 'FAIL_REPLAY','An identical same-host execution did not reproduce.'
    if all(row['group_256']['byte_identical'] for row in rows):
        return 'INCONCLUSIVE_ORIGINAL_GROUPING_NOT_REPRODUCED','Original grouping discrepancy did not reproduce.'
    effects={ (mode,a,b):any(not r[f'{mode}_{a}_vs_{b}']['byte_identical'] for r in rows)
              for mode in MODES for a,b in zip(UBATCHES,UBATCHES[1:]) }
    if not any(effects.values()):
        return 'INCONCLUSIVE_MICROBATCH_EFFECT_NOT_REPRODUCED','No microbatch effect on this runner.'
    unified_prompt=effects['unified',160,192]
    split_audio=effects['three_stage',128,160]
    split_prompt=effects['three_stage',160,192]
    if unified_prompt and split_audio and not split_prompt:
        return 'UNIFIED_AND_AUDIO_THRESHOLD_SENSITIVE','Both hypothesized threshold transitions affect logits; no reference correctness claim.'
    if unified_prompt and not split_prompt:
        return 'UNIFIED_PROMPT_THRESHOLD_SENSITIVE','Unified 160->192 differs but three-stage 160->192 does not.'
    if split_audio and not split_prompt:
        return 'THREE_STAGE_AUDIO_THRESHOLD_SENSITIVE','Three-stage 128->160 differs while three-stage 160->192 does not.'
    return 'MICROBATCH_RESPONSE_NOT_ISOLATED','Microbatch effects present but not isolated to a single 156/171 threshold.'


def evaluate(fixtures,folders,revision):
    hashes=old.legacy.verified_fixture(Path(fixtures))
    paths={key:Path(folder) for key,folder in folders.items()}
    if set(paths)!=set(KEYS+REPLAYS):
        raise ValueError('MISSING_OR_EXTRA_CASES')
    for key,folder in paths.items():
        if not folder.is_dir(): raise ValueError('MISSING_OUTPUT_DIRECTORY:'+key)
    phone=old.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows=[]
    for step in range(STEP_COUNT):
        name=f'logits-step-{step:02d}.f32le'
        data={key:old.legacy.read(folder/name) for key,folder in paths.items()}
        row={'step':step}
        for size in UBATCHES:
            row[f'group_{size}']=cmp(data[f'unified_u{size}'],data[f'three_stage_u{size}'])
        for mode in MODES:
            for a,b in zip(UBATCHES,UBATCHES[1:]):
                row[f'{mode}_{a}_vs_{b}']=cmp(data[f'{mode}_u{a}'],data[f'{mode}_u{b}'])
        row['replay_unified_256']=cmp(data['unified_u256'],data['unified_u256_replay'])
        row['replay_three_stage_256']=cmp(data['three_stage_u256'],data['three_stage_u256_replay'])
        row['replay_unified_160']=cmp(data['unified_u160'],data['unified_u160_replay'])
        if step==0:
            for size in UBATCHES:
                for mode in MODES:
                    key=f'{mode}_u{size}'
                    row['s26_vs_'+key]=cmp(phone,data[key])
        rows.append(row)
    status,conclusion=classify(rows)
    return {
      'schema':1,'release':'6.6.13','status':status,'conclusion':conclusion,
      'experiment':'same host n_ubatch=64,128,160,192,256; four threads; n_batch=256; 156 audio / 171 prompt positions',
      'interpretation_limit':'Microbatch size can change kernel scheduling and internal chunking; numerical closeness to S26 is not authoritative correctness.',
      'configurations':{str(i):{'n_batch':256,'n_ubatch':i,'n_threads':4,'n_threads_batch':4} for i in UBATCHES},
      'modes':{'unified':'[prefix+audio+suffix]','three_stage':'[prefix][audio][suffix]'},
      'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED',
      'phone_only_step00':True,'upstream_revision':revision,'fixture_sha256':hashes,'steps':rows,
    }


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixtures',required=True)
    for k in KEYS+REPLAYS:parser.add_argument('--'+k.replace('_','-'),dest=k,required=True)
    parser.add_argument('--llama-revision',required=True)
    parser.add_argument('--output',required=True)
    args=parser.parse_args()
    out=Path(args.output)
    out.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,{k:getattr(args,k) for k in KEYS+REPLAYS},args.llama_revision)
    except (OSError,ValueError,OverflowError) as e:
        report={'schema':1,'release':'6.6.13','status':'INCONCLUSIVE','reason':str(e),
                'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    out.write_text(json.dumps(report,indent=2)+'\n')
    print('microbatch_threshold_status='+report['status'])
    if report.get('steps'):
        r=report['steps'][0]
        for size in UBATCHES:
            print(f'group_{size}_step00_rmse='+str(r[f'group_{size}']['rmse']))
    if report['status'].startswith(('INCONCLUSIVE','FAIL')):raise SystemExit(2)

if __name__=='__main__':main()
