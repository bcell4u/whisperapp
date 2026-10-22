#!/usr/bin/env python3
"""6.6.14: distinguish generic prefill batching from audio-embedding effects.

The same pinned model/CPU/API, position sequence, and 171-position prompt is
used. Only the middle 156 positions are changed: original synthetic embeddings,
zero FP32 embeddings, or ordinary token IDs 198. This is NOT official Qwen parity.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

p=Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec=importlib.util.spec_from_file_location('chunk_6614',p)
base=importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

KINDS=('orig','zero','text')
MODES=('unified','three_stage')
UBATCHES=(128,256)
CASES=tuple(f'{kind}_{mode}_u{ub}' for kind in KINDS for mode in MODES for ub in UBATCHES)
REPLAYS=tuple(f'{kind}_unified_u256_replay' for kind in KINDS)
STEP_COUNT=5


def classify(rows):
    if len(rows)!=STEP_COUNT:
        return 'INCONCLUSIVE_INCOMPLETE', 'Five full steps required.'
    if any(not r[f'replay_{kind}']['byte_identical'] for r in rows for kind in KINDS):
        return 'FAIL_REPLAY', 'Identical host config produced different FP32 logits.'
    # Require 6.6.13 original-feature microbatch sensitivity as a positive control.
    by_kind={kind:any(not r[f'{kind}_{mode}_microbatch']['byte_identical']
                       for r in rows for mode in MODES) for kind in KINDS}
    if not by_kind['orig']:
        return 'INCONCLUSIVE_ORIGINAL_EFFECT_MISSING', 'Previously observed original-feature batching effect did not reproduce.'
    if by_kind['zero'] and by_kind['text']:
        return 'GENERAL_INPUT_BATCH_SENSITIVE', 'Both zero embeddings and ordinary tokens are sensitive: not specific to audio embeddings.'
    if by_kind['zero'] and not by_kind['text']:
        return 'EMBEDDING_PATH_SENSITIVE', 'Both original and zero embedding paths are sensitive; ordinary text-token control is invariant.'
    if not by_kind['zero'] and not by_kind['text']:
        return 'FEATURE_VALUE_SENSITIVE', 'Only real synthetic embedding values are sensitive; zero and text controls are invariant.'
    return 'MIXED_INPUT_SENSITIVITY', 'Text-token control is sensitive while zero embeddings are invariant; investigate attention/kernel behavior.'


def evaluate(fixtures,folders,revision):
    verified=base.legacy.verified_fixture(Path(fixtures))
    if set(folders)!=set(CASES+REPLAYS):
        raise ValueError('INPUT_CASES_MISSING_OR_EXTRA')
    paths={k:Path(v) for k,v in folders.items()}
    for k,v in paths.items():
        if not v.is_dir():raise ValueError('MISSING_OUTPUT_DIRECTORY:'+k)
    phone=base.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows=[]
    for step in range(STEP_COUNT):
        name=f'logits-step-{step:02d}.f32le'
        vectors={key:base.legacy.read(path/name) for key,path in paths.items()}
        row={'step':step}
        for kind in KINDS:
            for mode in MODES:
                row[f'{kind}_{mode}_microbatch']=base.comparison(vectors[f'{kind}_{mode}_u128'],vectors[f'{kind}_{mode}_u256'])
            for ubatch in UBATCHES:
                row[f'{kind}_group_u{ubatch}']=base.comparison(vectors[f'{kind}_unified_u{ubatch}'],vectors[f'{kind}_three_stage_u{ubatch}'])
            row[f'replay_{kind}']=base.comparison(vectors[f'{kind}_unified_u256'],vectors[f'{kind}_unified_u256_replay'])
        if step==0:
            for mode in MODES:
                for ubatch in UBATCHES:
                    row[f's26_vs_orig_{mode}_u{ubatch}']=base.comparison(phone,vectors[f'orig_{mode}_u{ubatch}'])
        rows.append(row)
    status,conclusion=classify(rows)
    return {
      'schema':1,'release':'6.6.14','status':status,'conclusion':conclusion,
      'experiment':'Same host: original synthetic audio, zero audio embeddings, text-token filler in 156 positions; 128/256 microbatch; unified/three-stage prefill, 4 threads.',
      'input_kinds':{'orig':'original synthetic 156x1024 FP32 embeddings',
                     'zero':'all-zero 156x1024 FP32 embedding matrix',
                     'text':'156 ordinary text tokens (ID 198); control only, not actual speech'},
      'filler_token_id':198,
      'zero_matrix_sha256':hashlib.sha256(bytes(156*1024*4)).hexdigest(),
      'modes':{'unified':'[prefix + middle + suffix]', 'three_stage':'[prefix][middle][suffix]'},
      'configurations':{str(n):{'n_ctx':512,'n_batch':256,'n_ubatch':n,'n_threads':4,'n_threads_batch':4} for n in UBATCHES},
      'interpretation_limit':'A change with text tokens implicates generic batching, not definitive root cause; absence with text is not proof of an M-RoPE defect. Host model math is llama.cpp in all conditions.',
      'phone_only_step00':True,'model_math_independent':False,
      'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED',
      'upstream_revision':revision,'fixture_sha256':verified,'steps':rows,
    }


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixtures',required=True)
    for key in CASES+REPLAYS:
        parser.add_argument('--'+key.replace('_','-'),dest=key,required=True)
    parser.add_argument('--llama-revision',required=True)
    parser.add_argument('--output',required=True)
    args=parser.parse_args()
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True)
    try:
        result=evaluate(args.fixtures,{key:getattr(args,key) for key in CASES+REPLAYS},args.llama_revision)
    except (OSError,ValueError,OverflowError) as ex:
        result={'schema':1,'release':'6.6.14','status':'INCONCLUSIVE','reason':str(ex),
                'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    out.write_text(json.dumps(result,indent=2)+'\n')
    print('input_kind_status='+result['status'])
    if 'steps' in result:
        r=result['steps'][0]
        for kind in KINDS:
            print(kind+'_unified_microbatch_step00_rmse='+str(r[f'{kind}_unified_microbatch']['rmse']))
            print(kind+'_three_stage_microbatch_step00_rmse='+str(r[f'{kind}_three_stage_microbatch']['rmse']))
    if result['status'].startswith(('INCONCLUSIVE','FAIL')):raise SystemExit(2)

if __name__=='__main__':main()
