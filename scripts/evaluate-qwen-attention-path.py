#!/usr/bin/env python3
"""6.6.17: fail-closed within-model Flash Attention request control (host only).

A difference between requested FA modes is not proof that either math path is
correct or equivalent to the official Qwen3-ASR implementation. An absence of a
difference does NOT establish the backend selected distinct kernels.
"""
import argparse
import importlib.util
import json
import math
from pathlib import Path

spec = importlib.util.spec_from_file_location('isa_eval', Path(__file__).with_name('evaluate-qwen-cpu-isa.py'))
isa = importlib.util.module_from_spec(spec)
spec.loader.exec_module(isa)
chunk = isa.previous.chunk
PRECS = ('q8', 'bf16')
ATTN = ('off', 'on')
MODES = ('unified', 'three_stage')
UBATCHES = (128, 256)
STEPS = 5
CASES = tuple(f'{p}_flash_{a}_{m}_u{u}' for p in PRECS for a in ATTN for m in MODES for u in UBATCHES)
REPLAYS = tuple(f'{p}_flash_{a}_unified_u256_replay' for p in PRECS for a in ATTN)


def mean(rows, key):
    nums = [r[key]['rmse'] for r in rows]
    if not nums or any(not isinstance(v, (int,float)) or not math.isfinite(v) or v < 0 for v in nums):
        raise ValueError('INVALID_METRIC:'+key)
    return sum(nums)/len(nums)


def classify(rows):
    if len(rows) != STEPS or sorted(r.get('step') for r in rows) != list(range(STEPS)):
        return ('INCONCLUSIVE_INCOMPLETE','Five ordered checkpoints required.')
    if any(not r[f'{p}_flash_{a}_replay']['byte_identical'] for r in rows for p in PRECS for a in ATTN):
        return ('FAIL_REPLAY','An identical configuration produced different logits on replay.')
    q8off = sum(mean(rows,f'q8_flash_off_{mode}_microbatch') for mode in MODES)/2
    q8on = sum(mean(rows,f'q8_flash_on_{mode}_microbatch') for mode in MODES)/2
    cross = [r[f'{p}_{mode}_u{u}_flash_toggle'] for r in rows for p in PRECS for mode in MODES for u in UBATCHES]
    if max(q8off,q8on) < .01:
        return ('INCONCLUSIVE_Q8_CONTROL','Prior Q8 microbatch effect did not reproduce.')
    # When every output is byte-identical, toggling the requested flag may have
    # left the actual attention kernels unchanged. Do not call it a negative.
    if all(comp['byte_identical'] for comp in cross):
        return ('INCONCLUSIVE_ATTENTION_BRANCH_NOT_OBSERVED',
                'Requested attention modes yielded identical logits. Kernel selection was not independently verified.')
    if q8off <= .7*q8on and (q8on-q8off) >= .03:
        return ('FLASH_OFF_REDUCES_Q8_BATCH_SENSITIVITY',
                'Disabling Flash Attention lowers Q8 batch variation, but does not establish correctness.')
    if q8on <= .7*q8off and (q8off-q8on) >= .03:
        return ('FLASH_ON_REDUCES_Q8_BATCH_SENSITIVITY',
                'Enabling Flash Attention lowers Q8 batch variation, but does not establish correctness.')
    return ('ATTENTION_MODES_DIFFER_BATCH_EFFECT_PERSISTS',
            'Attention-mode requests produce different logits but do not remove Q8 microbatch sensitivity.')


def evaluate(fixtures, paths, revision):
    hashes = chunk.legacy.verified_fixture(Path(fixtures))
    if set(paths) != set(CASES+REPLAYS):
        raise ValueError('MISSING_OR_EXTRA_ATTENTION_CASES')
    paths = {k:Path(v) for k,v in paths.items()}
    if any(not v.is_dir() for v in paths.values()):
        raise ValueError('MISSING_RESULT_DIRECTORY')
    phone = chunk.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows=[]
    for step in range(STEPS):
        file=f'logits-step-{step:02d}.f32le'
        vectors={k:chunk.legacy.read(v/file) for k,v in paths.items()}
        row={'step':step}
        for p in PRECS:
            for a in ATTN:
                row[f'{p}_flash_{a}_replay'] = chunk.comparison(
                    vectors[f'{p}_flash_{a}_unified_u256'], vectors[f'{p}_flash_{a}_unified_u256_replay'])
                for mode in MODES:
                    row[f'{p}_flash_{a}_{mode}_microbatch']=chunk.comparison(
                        vectors[f'{p}_flash_{a}_{mode}_u128'], vectors[f'{p}_flash_{a}_{mode}_u256'])
                for u in UBATCHES:
                    row[f'{p}_flash_{a}_u{u}_group']=chunk.comparison(
                        vectors[f'{p}_flash_{a}_unified_u{u}'], vectors[f'{p}_flash_{a}_three_stage_u{u}'])
            for mode in MODES:
                for u in UBATCHES:
                    row[f'{p}_{mode}_u{u}_flash_toggle']=chunk.comparison(
                        vectors[f'{p}_flash_off_{mode}_u{u}'], vectors[f'{p}_flash_on_{mode}_u{u}'])
        if step == 0:
            for a in ATTN:
                for mode in MODES:
                    for u in UBATCHES:
                        row[f's26_q8_flash_{a}_{mode}_u{u}']=chunk.comparison(
                            phone, vectors[f'q8_flash_{a}_{mode}_u{u}'])
        rows.append(row)
    status,conclusion=classify(rows)
    means={p:{a:sum(mean(rows,f'{p}_flash_{a}_{mode}_microbatch') for mode in MODES)/2
              for a in ATTN} for p in PRECS}
    cross={p:sum(mean(rows,f'{p}_{mode}_u{u}_flash_toggle') for mode in MODES for u in UBATCHES)/4
           for p in PRECS}
    return {
      'schema':1,'release':'6.6.17','status':status,'conclusion':conclusion,
      'experiment':'Host-only same-GGUF Flash Attention requested disabled/enabled, Q8 and BF16, 4 CPU threads, unified/three-stage prompt, n_ubatch 128/256, five teacher-forced checkpoints.',
      'mean_microbatch_rmse':means,'mean_attention_toggle_rmse':cross,
      'model_sha256':{'q8':isa.previous.Q8_SHA256,'bf16':isa.previous.BF16_SHA256},
      'upstream_revision':revision,'fixture_sha256':hashes,
      'attention_kernel_execution_independently_verified':False,
      'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED',
      'limitations':'Attention enable/disable is a requested context option, not an independently inspected kernel trace. CPU flash support may vary. No PyTorch oracle, ARM64 testing or clinical accuracy test.',
      'steps':rows,
    }


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--fixtures',required=True)
    ap.add_argument('--llama-revision',required=True)
    ap.add_argument('--output',required=True)
    for key in CASES+REPLAYS:
        ap.add_argument('--'+key.replace('_','-'),dest=key,required=True)
    args=ap.parse_args()
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,{k:getattr(args,k) for k in CASES+REPLAYS},args.llama_revision)
    except (OSError,ValueError,OverflowError,ZeroDivisionError,KeyError,TypeError) as e:
        report={'schema':1,'release':'6.6.17','status':'INCONCLUSIVE',
                'reason':str(e),'model_math_independent':False,
                'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    out.write_text(json.dumps(report,indent=2)+'\n')
    print('attention_path_status='+report['status'])
    if 'mean_microbatch_rmse' in report:
        print('mean_microbatch_rmse='+str(report['mean_microbatch_rmse']))
    if report['status'].startswith(('INCONCLUSIVE','FAIL')):raise SystemExit(2)

if __name__=='__main__':main()
