#!/usr/bin/env python3
"""v6.6.25 synthetic BF16 matched-position host-vs-HF numerical comparator.

Never equate an RMSE threshold with correctness or medical accuracy.
Missing branches are marked INCONCLUSIVE, never silently dropped.
"""
import argparse,hashlib,json
from pathlib import Path
import numpy as np

MODES=('pos-linear','pos-plus1','pos-plus17','pos-suffix-gap11')
ATTENTION=('flash-off','flash-on')
VOCAB=151936
FEATURE_SHA='e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee'
BF16_SHA='12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50'
HF_SHA='d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'

def read_vector(path):
    raw=Path(path).read_bytes()
    if len(raw)!=VOCAB*4:raise ValueError('INVALID_LOGIT_VECTOR_BYTES:'+Path(path).name)
    v=np.frombuffer(raw,dtype='<f4').astype(np.float64)
    if not np.isfinite(v).all():raise ValueError('NONFINITE_LOGIT_VECTOR:'+Path(path).name)
    return v,hashlib.sha256(raw).hexdigest()

def metrics(a,b):
    d=a-b
    ac=a-a.mean();bc=b-b.mean();
    return {'rmse':float(np.sqrt(np.mean(d*d))),
            'centered_rmse':float(np.sqrt(np.mean(((a-a.mean())-(b-b.mean()))**2))),
            'max_abs':float(np.max(np.abs(d))),
            'cosine':float(np.dot(a,b)/(np.linalg.norm(a)*np.linalg.norm(b))),
            'top1_equal':bool(np.argmax(a)==np.argmax(b))}

def evaluate(root):
    root=Path(root)
    report={'schema':1,'release':'6.6.25','reference_kind':'MATCHED_POSITION_INDEPENDENT_HF_VS_GGUF_BF16',
            'model_checksums':{'bf16_gguf':BF16_SHA,'hf_safetensors':HF_SHA,'features':FEATURE_SHA},
            'synthetic_only':True,'official_asr_rope_parity':'NOT_VERIFIED',
            'device_inference_modified':False,'results':{},'limitations':
            '4-axis GGUF equal-position controls are compared to 2D HF text positions. '
            'The model may retain kernel/dtype differences; logits alone cannot verify official audio position grids.'}
    missing=[]
    hf={}
    for mode in MODES:
        try:
            v,sha=read_vector(root/'hf'/f'hf-{mode}.f32le');hf[mode]=v
            report['results'][mode]={'hf_sha256':sha,'hf_top1':int(np.argmax(v)),'comparisons':{}}
        except (FileNotFoundError,ValueError) as e:
            missing.append(f'hf/{mode}:{type(e).__name__}')
    for mode in MODES:
        if mode not in hf:continue
        for attn in ATTENTION:
            path=root/'host'/mode/attn/'logits-step-00.f32le'
            try:
                v,sha=read_vector(path)
                report['results'][mode]['comparisons'][attn]={
                    'host_sha256':sha,'host_top1':int(np.argmax(v)),
                    'vs_hf':metrics(v,hf[mode])}
            except (FileNotFoundError,ValueError) as e:
                missing.append(f'host/{mode}/{attn}:{type(e).__name__}')
        if len(report['results'][mode]['comparisons'])==2:
            a,_=read_vector(root/'host'/mode/'flash-off'/'logits-step-00.f32le')
            b,_=read_vector(root/'host'/mode/'flash-on'/'logits-step-00.f32le')
            report['results'][mode]['attention_path_difference']=metrics(a,b)
    if 'pos-linear' in hf:
        for mode in MODES:
            if mode in hf:
                report['results'][mode]['hf_vs_hf_linear']=metrics(hf[mode],hf['pos-linear'])
    report['missing_branches']=missing
    report['status']='INCONCLUSIVE_INCOMPLETE_BRANCHES' if missing else 'MATCHED_POSITION_CROSS_BACKEND_COMPARISON_COMPLETE'
    return report

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--artifact-dir',required=True);ap.add_argument('--output',required=True)
    args=ap.parse_args();report=evaluate(args.artifact_dir)
    p=Path(args.output);p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(report,indent=2,sort_keys=True)+'\n')
    print('CROSS_BACKEND_PARITY',report['status'],report['missing_branches'])
    if report['missing_branches']:raise SystemExit(3)

if __name__=='__main__':main()
