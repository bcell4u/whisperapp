#!/usr/bin/env python3
"""6.6.18: synthetic greedy-token impact and logged attention graph attestation.

This distinguishes observable token decisions from FP32 logit drift. It does NOT
establish official Qwen3-ASR model correctness, M-RoPE parity, or speech accuracy.
"""
import argparse
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import re

spec=importlib.util.spec_from_file_location('prior_attention_eval', Path(__file__).with_name('evaluate-qwen-attention-path.py'))
prior=importlib.util.module_from_spec(spec)
spec.loader.exec_module(prior)

Q8_CASES=tuple(f'q8_flash_{fa}_{mode}_u{u}' for fa in ('off','on') for mode in ('unified','three_stage') for u in (128,256))
BF16_CASES=tuple(f'bf16_flash_{fa}_unified_u{u}' for fa in ('off','on') for u in (128,256))
CASES=Q8_CASES+BF16_CASES
REPLAYS=('q8_flash_off_unified_u256_replay','q8_flash_on_unified_u256_replay')
MAX_TOKENS=24
MARKER=re.compile(r'\((FLASH_ATTN_EXT|SOFT_MAX)\)')


def audit_graph(text, requested):
    actual=re.findall(r'llama_context:\s*flash_attn\s*=\s*(enabled|disabled)',text)
    ops=MARKER.findall(text)
    flash=ops.count('FLASH_ATTN_EXT')
    soft=ops.count('SOFT_MAX')
    expected='enabled' if requested=='on' else 'disabled'
    valid=actual and all(x==expected for x in actual) and (
        flash>0 and soft==0 if requested=='on' else flash==0 and soft>0)
    return {'valid':bool(valid), 'requested':requested,'runtime_reported':sorted(set(actual)),
            'flash_attention_graph_nodes':flash,'softmax_graph_nodes':soft,
            'graph_operation_attested':bool(valid),
            'hardware_kernel_execution_verified':False}


def load_case(path, key):
    path=Path(path)
    if not path.is_dir(): raise ValueError('MISSING_CASE_DIR:'+key)
    data=json.loads((path/'greedy-decisions.json').read_text())
    if data.get('schema')!=1 or data.get('requested_max_tokens')!=MAX_TOKENS:
        raise ValueError('BAD_GREEDY_SCHEMA:'+key)
    decisions=data.get('decisions')
    if not isinstance(decisions,list) or not 1<=len(decisions)<=MAX_TOKENS:
        raise ValueError('EMPTY_OR_LONG_DECISIONS:'+key)
    ids=[];margins=[]
    for obj in decisions:
        if not isinstance(obj,dict) or type(obj.get('token_id')) is not int or not 0<=obj['token_id']<151936:
            raise ValueError('INVALID_TOKEN_ID:'+key)
        margin=obj.get('top2_margin')
        if type(margin) not in (float,int) or not math.isfinite(margin) or margin<0:
            raise ValueError('INVALID_MARGIN:'+key)
        ids.append(obj['token_id']);margins.append(float(margin))
    ended=ids[-1] in (151645,151643)
    if data.get('stopped_on_end_token') is not ended:
        raise ValueError('END_FLAG_MISMATCH:'+key)
    if len(ids)<MAX_TOKENS and not ended: raise ValueError('EARLY_STOP_WITHOUT_EOS:'+key)
    text=(path/'inference.log').read_text(errors='replace')
    a='on' if '_flash_on_' in key else 'off'
    graph=audit_graph(text,a)
    vec=prior.chunk.legacy.read(path/'logits-step-00.f32le')
    sha=hashlib.sha256((path/'greedy-decisions.json').read_bytes()).hexdigest()
    return {'ids':ids,'min_top2_margin':min(margins),'max_top2_margin':max(margins),
            'stopped_on_end_token':ended,'graph':graph,'decisions_sha256':sha,'logits':vec}


def first_divergence(a,b):
    for i,(x,y) in enumerate(zip(a,b)):
        if x!=y:return i
    if len(a)!=len(b):return min(len(a),len(b))
    return None


def classify(rows):
    if not all(r['graph']['valid'] for r in rows.values()):
        return 'INCONCLUSIVE_GRAPH_PATH_UNVERIFIED'
    if any(rows[k]['decisions_sha256'] != rows[k+'_replay']['decisions_sha256']
           or first_divergence(rows[k]['ids'],rows[k+'_replay']['ids']) is not None
           for k in ('q8_flash_off_unified_u256','q8_flash_on_unified_u256')):
        return 'FAIL_REPLAY'
    # The two replays must also yield identical first-token logits, not just the same tokens.
    if any(not prior.chunk.comparison(rows[k]['logits'],rows[k+'_replay']['logits'])['byte_identical']
           for k in ('q8_flash_off_unified_u256','q8_flash_on_unified_u256')):
        return 'FAIL_REPLAY'
    batch=False;attention=False;group=False
    for model in ('q8','bf16'):
        modes=('unified','three_stage') if model=='q8' else ('unified',)
        for mode in modes:
            for fa in ('off','on'):
                batch |= first_divergence(rows[f'{model}_flash_{fa}_{mode}_u128']['ids'],rows[f'{model}_flash_{fa}_{mode}_u256']['ids']) is not None
            for u in (128,256):
                attention |= first_divergence(rows[f'{model}_flash_off_{mode}_u{u}']['ids'],rows[f'{model}_flash_on_{mode}_u{u}']['ids']) is not None
        if model=='q8':
            for u in (128,256):
                for fa in ('off','on'):
                    group |= first_divergence(rows[f'q8_flash_{fa}_unified_u{u}']['ids'],rows[f'q8_flash_{fa}_three_stage_u{u}']['ids']) is not None
    if batch or attention or group:
        return 'SYNTHETIC_GREEDY_TOKEN_DIVERGENCE'
    return 'NO_SYNTHETIC_GREEDY_DIVERGENCE_WITHIN_24_TOKENS'


def evaluate(paths, fixtures, revision):
    if set(paths)!=set(CASES+REPLAYS):raise ValueError('MISSING_OR_EXTRA_GENERATION_CASES')
    hashes=prior.chunk.legacy.verified_fixture(Path(fixtures))
    rows={key:load_case(path,key) for key,path in paths.items()}
    status=classify(rows)
    phone=prior.chunk.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    outputs={}
    for key,row in rows.items():
        outputs[key]={k:v for k,v in row.items() if k not in ('logits','ids')}
        outputs[key]['token_ids']=row['ids']
        outputs[key]['first_token_s26_comparison']= (prior.chunk.comparison(phone,row['logits']) if key.startswith('q8_') else None)
    diffs={}
    for model in ('q8','bf16'):
        modes=('unified','three_stage') if model=='q8' else ('unified',)
        for mode in modes:
            for fa in ('off','on'):
                k=f'{model}_flash_{fa}_{mode}'
                diffs[k+'_microbatch_128_vs_256']=first_divergence(rows[k+'_u128']['ids'],rows[k+'_u256']['ids'])
            for u in (128,256):
                diffs[f'{model}_{mode}_u{u}_attention_off_vs_on']=first_divergence(
                    rows[f'{model}_flash_off_{mode}_u{u}']['ids'],rows[f'{model}_flash_on_{mode}_u{u}']['ids'])
        if model=='q8':
            for fa in ('off','on'):
                for u in (128,256):
                    diffs[f'q8_flash_{fa}_u{u}_unified_vs_three_stage']=first_divergence(
                        rows[f'q8_flash_{fa}_unified_u{u}']['ids'],rows[f'q8_flash_{fa}_three_stage_u{u}']['ids'])
    return {'schema':1,'release':'6.6.18','status':status,'upstream_revision':revision,
            'fixture_sha256':hashes,'model_sha256':{'q8':prior.isa.previous.Q8_SHA256,'bf16':prior.isa.previous.BF16_SHA256},
            'max_greedy_tokens':MAX_TOKENS,'synthetic_only':True,
            'graph_attention_operator_verified':all(v['graph']['valid'] for v in rows.values()),
            'hardware_kernel_execution_verified':False,'official_qwen_parity':'NOT_VERIFIED',
            'mrope_parity':'UNQUALIFIED','first_divergent_position_zero_based':diffs,
            'cases':outputs,
            'limitations':'A synthetic greedy sequence, not a clinical accuracy test or independent model-math oracle; graph logs attest constructed operators but not hardware instruction execution. Flash-off invariance is not proof of official Qwen correctness.'}


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--fixtures',required=True);ap.add_argument('--llama-revision',required=True)
    ap.add_argument('--output',required=True)
    for k in CASES+REPLAYS:ap.add_argument('--'+k.replace('_','-'),dest=k,required=True)
    args=ap.parse_args()
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True)
    try:report=evaluate({k:getattr(args,k) for k in CASES+REPLAYS},args.fixtures,args.llama_revision)
    except (OSError,ValueError,TypeError,KeyError,OverflowError,ZeroDivisionError,json.JSONDecodeError) as e:
        report={'schema':1,'release':'6.6.18','status':'INCONCLUSIVE','reason':str(e),
                'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    out.write_text(json.dumps(report,indent=2)+'\n')
    print('generation_impact_status='+report['status'])
    if report['status'].startswith(('INCONCLUSIVE','FAIL')):raise SystemExit(2)

if __name__=='__main__':main()
