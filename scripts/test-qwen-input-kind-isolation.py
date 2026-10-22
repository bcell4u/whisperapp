#!/usr/bin/env python3
"""6.6.14: unit controls plus real 6.6.13 vector regression test."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile
from array import array

p=Path(__file__).with_name('evaluate-qwen-input-kind-isolation.py')
s=importlib.util.spec_from_file_location('input6614',p)
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

def fake_rows(orig=True,zero=False,text=False,replay=False):
    flags={'orig':orig,'zero':zero,'text':text}
    rows=[]
    for step in range(m.STEP_COUNT):
        r={'step':step}
        for kind in m.KINDS:
            for mode in m.MODES:
                r[f'{kind}_{mode}_microbatch']={'byte_identical':not flags[kind], 'rmse':0.2 if flags[kind] else 0.0}
            r[f'replay_{kind}']={'byte_identical':not(replay and kind=='zero')}
        rows.append(r)
    return rows

for args,expected in [
    ({'orig':True,'zero':True,'text':True},'GENERAL_INPUT_BATCH_SENSITIVE'),
    ({'orig':True,'zero':True,'text':False},'EMBEDDING_PATH_SENSITIVE'),
    ({'orig':True,'zero':False,'text':False},'FEATURE_VALUE_SENSITIVE'),
    ({'orig':True,'zero':False,'text':True},'MIXED_INPUT_SENSITIVITY'),
    ({'orig':False,'zero':True,'text':True},'INCONCLUSIVE_ORIGINAL_EFFECT_MISSING'),
    ({'orig':True,'zero':True,'text':True,'replay':True},'FAIL_REPLAY'),
]:
    assert m.classify(fake_rows(**args))[0]==expected,(args,expected)
assert m.classify(fake_rows()[:4])[0]=='INCONCLUSIVE_INCOMPLETE'
fx=Path(__file__).resolve().parents[1]/'verification'/'fixtures'
assert len(m.base.legacy.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp)
    paths={k:root/k for k in m.CASES+m.REPLAYS}
    for folder in paths.values():folder.mkdir()
    try:m.evaluate(fx,paths,'pinned')
    except (ValueError,OSError):pass
    else:raise AssertionError('Accepted missing checkpoint')
    for content in (b'broken',struct.pack('<f',math.nan)+bytes(m.base.legacy.VOCAB*4-4)):
        f=paths['orig_unified_u128']/'logits-step-00.f32le';f.write_bytes(content)
        try:m.base.legacy.read(f)
        except (ValueError,OSError):pass
        else:raise AssertionError('Accepted corrupt or nonfinite checkpoint')
# Compare real available 6.6.13 numerical vectors if the previous run artifact is mounted.
prior=Path('/mnt/data/qwen-6.6.13-host-microbatch-threshold-6.zip')
if prior.exists():
    import zipfile
    with zipfile.ZipFile(prior) as z:
        a=array('f');b=array('f')
        a.frombytes(z.read('unified_u128/logits-step-00.f32le'))
        b.frombytes(z.read('unified_u256/logits-step-00.f32le'))
        assert len(a)==len(b)==m.base.legacy.VOCAB
        assert m.base.comparison((a,'real_prior_a'),(b,'real_prior_b'))['rmse']>0.2
print('PASS 6.6.14: modality decisions, replay guards, incomplete/NaN controls, real prior-vector comparison when available')
