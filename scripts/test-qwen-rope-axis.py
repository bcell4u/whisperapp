#!/usr/bin/env python3
"""Positive/negative host-free axis attribution tests; synthetic vectors only."""
import importlib.util
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import numpy as np
p = Path(__file__).with_name('evaluate-qwen-rope-axis.py')
spec = importlib.util.spec_from_file_location('mrope_attribution', p)
m = importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
assert len(m.MAPS) == 12
with TemporaryDirectory() as t:
    r=Path(t)
    base=np.ones(m.N,dtype='<f4')
    for mode in m.MAPS:
        for attention in m.ATTN:
            d=r/'host'/mode/attention;d.mkdir(parents=True)
            val=0.0
            if mode=='pos-plus1':val=0.01
            if mode.startswith('pos-plus17'):val=0.175 if mode=='pos-plus17' else 0.017*(int(mode[-1])+1)
            if mode.startswith('pos-suffix-gap11'):val=0.113 if mode=='pos-suffix-gap11' else 0.011*(int(mode[-1])+1)
            v=base.copy();v+=val;v[11528]+=100
            (d/'logits-step-00.f32le').write_bytes(v.tobytes())
            marker='SOFT_MAX' if attention=='flash-off' else 'FLASH_ATTN_EXT'
            (d/'inference.log').write_text(f'node ({marker})\nHOST_EXTENDED_BATCH_EXECUTED pos_mode={mode}\n')
    report=m.analyze(r)
    assert report['status']==m.STATUS,report['missing']
    assert len(report['responses'])==4
    assert len(report['vectors'])==len(m.MAPS)*3
    response=report['responses']['pos-plus17/flash-on']
    assert response['all_axis_rmse']>0
    assert response['single_axis_rmse']['3']>response['single_axis_rmse']['0']
    assert response['nonadditive_residual_rmse']>0
    assert all('11528' not in str(e) for e in report['missing'])
    f=r/'host/pos-plus17-axis3/flash-on/logits-step-00.f32le'
    raw=f.read_bytes();f.write_bytes(raw[:-4]);assert m.analyze(r)['status']!=''+m.STATUS
    f.write_bytes(raw)
    f.write_bytes(b'\x00\x00\xc0\x7f'+raw[4:]);assert any('NONFINITE' in e for e in m.analyze(r)['missing']);f.write_bytes(raw)
    log=r/'host/pos-plus17-axis3/flash-on/inference.log'
    original=log.read_text();log.write_text(original.replace('FLASH_ATTN_EXT','SOFT_MAX'))
    assert any('GRAPH_PATH_NOT_ISOLATED' in e for e in m.analyze(r)['missing'])
    log.write_text(original.replace('pos_mode=pos-plus17-axis3','pos_mode=pos-plus17-axis0'))
    assert any('POSITION_UNVERIFIED' in e for e in m.analyze(r)['missing'])
    log.write_text(original)
    assert m.analyze(r)['status']==m.STATUS
print('PASS: 4-axis position attribution, response decomposition, path checks, truncation and nonfinite rejection')
