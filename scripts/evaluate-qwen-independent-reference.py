#!/usr/bin/env python3
"""Fail-closed numerical comparisons to a separate Qwen Transformers decoder.

This is a checkpoint consistency test, not proof of original Qwen M-RoPE/audio
parity, QNN feature correctness, or clinical recognition quality.
"""
import argparse
import array
import hashlib
import json
import math
from pathlib import Path
import sys

VOCAB=151936
FIXTURE_HASHES={
 's26-prefill-logits.f32le':'9951cd0ed4143a97f4181c3fe0b87086aaf7f77b8571c63ea41d5597ef41a8e2',
 's26-synthetic-features.f32le':'e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee'}
EXPECTED_MODELS={'q8':'bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971',
                 'bf16':'12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50'}

def verified(path,sha=None):
    data=Path(path).read_bytes()
    if len(data)!=VOCAB*4:raise ValueError('INVALID_LOGIT_LENGTH:'+Path(path).name)
    digest=hashlib.sha256(data).hexdigest()
    if sha is not None and digest!=sha:raise ValueError('LOGIT_HASH_MISMATCH:'+Path(path).name)
    values=array.array('f');values.frombytes(data)
    if sys.byteorder!='little':values.byteswap()
    if len(values)!=VOCAB or not all(math.isfinite(x) for x in values):
        raise ValueError('INVALID_LOGIT_VALUES:'+Path(path).name)
    return values,digest

def metrics(left,right):
    if len(left)!=len(right) or not left:raise ValueError('MISMATCHED_LOGIT_VECTORS')
    n=len(left)
    delta_sum=delta_sq=dot=sa=sb=mx=0.
    top_a=top_b=-1; max_a=max_b=-math.inf
    for i,(a,b) in enumerate(zip(left,right)):
        if not (math.isfinite(a) and math.isfinite(b)):raise ValueError('NONFINITE_LOGIT')
        d=float(a)-float(b)
        delta_sum+=d;delta_sq+=d*d;mx=max(mx,abs(d))
        dot+=float(a)*float(b);sa+=float(a)*float(a);sb+=float(b)*float(b)
        if a>max_a:max_a=float(a);top_a=i
        if b>max_b:max_b=float(b);top_b=i
    shift=delta_sum/n
    return {'rmse':math.sqrt(delta_sq/n),'max_abs':mx,
            'cosine':dot/math.sqrt(sa*sb) if sa>0 and sb>0 else None,
            'top1_left':top_a,'top1_right':top_b,'top1_equal':top_a==top_b,
            'mean_left_minus_right':shift,
            'mean_centered_rmse':math.sqrt(max(0.,delta_sq/n-shift*shift)),
            'byte_identical':left.tobytes()==right.tobytes()}

def compare(fixtures,hf_dir,host_dirs):
    for name,sha in FIXTURE_HASHES.items():
        p=Path(fixtures)/name
        if hashlib.sha256(p.read_bytes()).hexdigest()!=sha:raise ValueError('INVALID_FIXTURE_SHA:'+name)
    hf_dir=Path(hf_dir)
    meta=json.loads((hf_dir/'HF_REFERENCE_METADATA.json').read_text())
    if meta.get('reference_kind')!='INDEPENDENT_HF_TRANSFORMERS_PREFILL' or not str(meta.get('resolved_revision','')).startswith('6aa69c3'):
        raise ValueError('HF_MODEL_PROVENANCE_MISMATCH')
    if meta.get('features_sha256')!=FIXTURE_HASHES['s26-synthetic-features.f32le'] or meta.get('attention_implementation')!='eager':
        raise ValueError('HF_INPUT_OR_ATTENTION_INVALID')
    hf,hash_hf=verified(hf_dir/'hf-prefill-logits.f32le',meta['logits_sha256'])
    if int(meta.get('top1_token_id',-1))!=max(range(VOCAB),key=hf.__getitem__):
        raise ValueError('HF_TOP1_METADATA_MISMATCH')
    phone,hash_phone=verified(Path(fixtures)/'s26-prefill-logits.f32le',FIXTURE_HASHES['s26-prefill-logits.f32le'])
    rows={'hf_vs_s26':metrics(hf,phone)}
    hosthash={}
    for key,folder in host_dirs.items():
        host,sha=verified(Path(folder)/'logits-step-00.f32le')
        rows['hf_vs_'+key]=metrics(hf,host)
        hosthash[key]=sha
    # The Q8 ON host must retain its known 6.6.18 numerical fingerprint.
    if hosthash.get('q8_flash_on')!='73d668a9dbcb4b2f42620544aba42c325b0cf1897b3c946d4a372f9a24d81fa3':
        raise ValueError('HOST_Q8_BASELINE_NOT_REPRODUCED')
    for key,expected in {
       'bf16_flash_off':'52790bb135d4d0ec3b521aea9acfbfc30dcd78a471f69bc088987b11218a0f9d',
       'bf16_flash_on':'161802cad907784aa00211d3de9076933a263a262a487ccbfd07082a7ae13422',
    }.items():
        if hosthash.get(key)!=expected:
            raise ValueError('HOST_BF16_BASELINE_NOT_REPRODUCED:'+key)
    bf=rows['hf_vs_bf16_flash_off']['rmse']
    q8=rows['hf_vs_q8_flash_on']['rmse']
    status='HF_REFERENCE_COMPARED'
    if rows['hf_vs_s26']['top1_left']!=11528:
        status='REFERENCE_TOP1_DIFFERS_FROM_FIXED_SYNTHETIC_CONTROL'
    return {'schema':1,'release':'6.6.19','status':status,
            'independent_model_execution':True,'synthetic_only':True,
            'hf_reference':meta,'fixture_sha256':FIXTURE_HASHES,
            'host_model_sha256':EXPECTED_MODELS,'host_logits_sha256':hosthash,
            'comparisons':rows,
            'bf16_host_closer_to_hf_than_q8':bf<q8,
            'phone_first_token_matches_hf':rows['hf_vs_s26']['top1_equal'],
            'official_qwen_full_asr_parity':'NOT_VERIFIED',
            'mrope_parity':'UNQUALIFIED',
            'model_weight_identity':'NOT_VERIFIED',
            'limitations':'Distinct Qwen -hf checkpoint and llama GGUF conversion are not yet weight-matched tensor-by-tensor. BF16 PyTorch arithmetic differs from FP32 ggml. Linear equal-axis synthetic positions are not full Qwen multimodal M-RoPE; injected embeddings bypass audio tower; only first prefill token tested; no clinical audio.'}

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--fixtures',required=True)
    ap.add_argument('--hf-dir',required=True)
    for k in ('q8_flash_on','bf16_flash_off','bf16_flash_on'):
        ap.add_argument('--'+k.replace('_','-'),dest=k,required=True)
    ap.add_argument('--output',required=True)
    args=ap.parse_args();o=Path(args.output);o.parent.mkdir(parents=True,exist_ok=True)
    try: result=compare(args.fixtures,args.hf_dir,{k:getattr(args,k) for k in ('q8_flash_on','bf16_flash_off','bf16_flash_on')})
    except (ValueError,OSError,KeyError,TypeError,OverflowError,ZeroDivisionError) as e:
        result={'schema':1,'release':'6.6.19','status':'INCONCLUSIVE','reason':str(e),
                'independent_model_execution':False,'official_qwen_full_asr_parity':'NOT_VERIFIED',
                'mrope_parity':'UNQUALIFIED'}
    o.write_text(json.dumps(result,indent=2)+'\n')
    print('independent_hf_status='+result['status'])
    if result['status']=='INCONCLUSIVE':raise SystemExit(2)

if __name__=='__main__':main()
