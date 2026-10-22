#!/usr/bin/env python3
"""Host-free regression and negative controls for v6.6.25 cross-backend parity."""
import importlib.util, tempfile
from pathlib import Path
import numpy as np
root=Path(__file__).parent
s=importlib.util.spec_from_file_location('cross_backend',root/'evaluate-qwen-cross-backend-positions.py')
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
p=importlib.util.spec_from_file_location('hf_positions',root/'measure-qwen-hf-position-parity.py')
h=importlib.util.module_from_spec(p);p.loader.exec_module(h)
assert m.MODES==h.MODES
import torch
for mode in m.MODES:
    p=h.positions(torch,mode)
    assert list(p.shape)==[1,171]
    assert p.min().item()>=0
assert h.positions(torch,'pos-linear')[0,165].item()==165
assert h.positions(torch,'pos-suffix-gap11')[0,164].item()==164
assert h.positions(torch,'pos-suffix-gap11')[0,165].item()==176
assert h.positions(torch,'pos-plus17')[0,0].item()==17
assert h.positions(torch,'pos-plus1')[0,170].item()==171
with tempfile.TemporaryDirectory() as temp:
    base=Path(temp)
    vectors=np.zeros(m.VOCAB,dtype='<f4');vectors[11528]=10
    altered=vectors.copy();altered[11528]=9
    for mode in m.MODES:
        hp=base/'hf'/f'hf-{mode}.f32le';hp.parent.mkdir(parents=True,exist_ok=True);hp.write_bytes(vectors.tobytes())
        for attn in m.ATTENTION:
            f=base/'host'/mode/attn/'logits-step-00.f32le';f.parent.mkdir(parents=True,exist_ok=True)
            f.write_bytes((altered if attn=='flash-on' else vectors).tobytes())
    result=m.evaluate(base)
    assert result['status']=='MATCHED_POSITION_CROSS_BACKEND_COMPARISON_COMPLETE'
    assert result['results']['pos-linear']['comparisons']['flash-off']['vs_hf']['rmse']==0
    assert result['results']['pos-linear']['comparisons']['flash-on']['vs_hf']['rmse']>0
    assert all(row['hf_top1']==11528 for row in result['results'].values())
    (base/'host'/'pos-plus1'/'flash-on'/'logits-step-00.f32le').unlink()
    r=m.evaluate(base);assert r['status']=='INCONCLUSIVE_INCOMPLETE_BRANCHES' and len(r['missing_branches'])==1
    (base/'hf'/'hf-pos-plus17.f32le').write_bytes(b'bad')
    assert len(m.evaluate(base)['missing_branches'])>=2
print('PASS: paired positional mapping, 8-vector equivalence, perturbations, incomplete branches')
