#!/usr/bin/env python3
"""Host-free v6.6.26 controls, including incomplete and fraudulent reports."""
import importlib.util
import json
import tempfile
from pathlib import Path
import numpy as np

scripts=Path(__file__).resolve().parent

def load(name,alias):
    spec=importlib.util.spec_from_file_location(alias,scripts/name)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

m=load('evaluate-qwen-hf-execution-precision.py','precision_6626')
h=load('measure-qwen-hf-execution-precision.py','hf_measure_6626')
assert m.MODES==h.MODES
assert m.BRANCHES==h.BRANCHES
assert m.BRANCHES==('bf16','fp32-rounded-input','fp32-exact-audio')

with tempfile.TemporaryDirectory() as directory:
    root=Path(directory)
    meta={
        'schema':1,'release':'6.6.26',
        'hf_revision':h.REV,'hf_safetensors_sha256':h.HF_SHA,
        'features_sha256':h.ref.FEATURE_SHA,
        'position_modes':{},
    }
    base=np.zeros(m.ref.VOCAB,dtype='<f4')
    base[11528]=5
    for branch_idx,branch in enumerate(m.BRANCHES):
        meta['position_modes'][branch]={}
        for mode_idx,mode in enumerate(m.MODES):
            v=base.copy()
            v[0:4]=np.float32((mode_idx+1)*(branch_idx+1))/10
            raw=v.tobytes()
            f=root/'hf'/branch/f'hf-{mode}.f32le'
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_bytes(raw)
            import hashlib
            meta['position_modes'][branch][mode]={
                'sha256':hashlib.sha256(raw).hexdigest(),
                'top1_token_id':11528,
            }
    for attn_idx,attn in enumerate(m.ATTENTION):
        for mode_idx,mode in enumerate(m.MODES):
            v=base.copy()
            v[0:4]=np.float32((mode_idx+1)*(attn_idx+1))/10
            f=root/'host'/mode/attn/'logits-step-00.f32le'
            f.parent.mkdir(parents=True,exist_ok=True)
            f.write_bytes(v.tobytes())
    meta_file=root/'hf'/'HF_EXECUTION_PRECISION_METADATA.json'
    meta_file.write_text(json.dumps(meta))
    report=m.evaluate(root)
    assert report['status']==m.STATUS and not report['missing_branches']
    assert report['results']['pos-linear']['host_against_hf']['flash-off']['bf16']['rmse']==0
    assert report['results']['pos-linear']['hf_cross_dtype']['fp32-rounded-input']['rmse']>0
    assert report['response_alignment']['pos-plus1']['bf16']['flash-off']['response_residual_rmse']==0
    assert report['results']['pos-linear']['audio_input_rounding_effect']['rmse']>0
    assert len(report['response_alignment'])==3
    f=root/'hf'/'fp32-rounded-input'/'hf-pos-plus17.f32le'
    prior=f.read_bytes()
    f.write_bytes(prior[:-4]);assert any('fp32-rounded-input/pos-plus17' in x for x in m.evaluate(root)['missing_branches'])
    f.write_bytes(prior)
    f.write_bytes(b'\x00\x00\xc0\x7f'+prior[4:]);assert any('fp32-rounded-input/pos-plus17' in x for x in m.evaluate(root)['missing_branches'])
    f.write_bytes(prior)
    meta['position_modes']['fp32-rounded-input']['pos-plus17']['sha256']='0'*64
    meta_file.write_text(json.dumps(meta))
    assert any('fp32-rounded-input/pos-plus17' in x for x in m.evaluate(root)['missing_branches'])
    meta['position_modes']['fp32-rounded-input']['pos-plus17']['sha256']=__import__('hashlib').sha256(prior).hexdigest()
    meta_file.write_text(json.dumps(meta))
    assert m.evaluate(root)['status']==m.STATUS
    host=root/'host'/'pos-plus1'/'flash-on'/'logits-step-00.f32le'
    host.unlink()
    assert any('host/pos-plus1/flash-on' in x for x in m.evaluate(root)['missing_branches'])

# A real output-vector contract from v6.6.25. Do not mistake storage aliases for model parity.
assert m.ref.VOCAB==151936
assert m.ref.HF_SHA=='d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'
print('PASS: BF16/FP32 precision comparison, response deltas, checksums, missing/corrupt/nonfinite rejection')
