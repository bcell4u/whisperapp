#!/usr/bin/env python3
"""Precise 6.6.15 classification, filesystem, and real historical vector controls."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile
import zipfile

script=Path(__file__).with_name('evaluate-qwen-precision-batch.py')
spec=importlib.util.spec_from_file_location('precision6615',script)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def rows(q8=.20, bf16=.20, replay=False):
    out=[]
    for step in range(m.STEPS):
        row={'step':step}
        for p,v in [('q8',q8),('bf16',bf16)]:
            row[f'replay_{p}']={'byte_identical':not(replay and p=='bf16')}
            for kind in m.KINDS:
                for mode in m.MODES:
                    row[f'{p}_{kind}_{mode}_microbatch']={'rmse':v,'byte_identical':v==0}
        out.append(row)
    return out

for q,b,want in [(0.2,0,'BF16_ALMOST_INVARIANT'),(.3,.08,'Q8_MORE_BATCH_SENSITIVE'),(.08,.3,'BF16_MORE_BATCH_SENSITIVE'),(.2,.2,'BOTH_PRECISIONS_BATCH_SENSITIVE'),(0,.2,'INCONCLUSIVE_Q8_CONTROL')]:
    assert m.classify(rows(q,b))[0]==want,(q,b,want)
assert m.classify(rows()[:-1])[0]=='INCONCLUSIVE_INCOMPLETE'
assert m.classify(rows(replay=True))[0]=='FAIL_REPLAY'
fx=script.resolve().parents[1]/'verification'/'fixtures'
assert len(m.chunk.legacy.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as t:
    base=Path(t)
    paths={key:base/key for key in m.CASES+m.REPLAYS}
    for path in paths.values():path.mkdir()
    try:m.evaluate(fx,paths,'pinned')
    except (OSError,ValueError):pass
    else:raise AssertionError('Missing vector accepted')
    f=paths['q8_orig_unified_u128']/'logits-step-00.f32le'
    for content in [b'invalid',struct.pack('<f',math.inf)+bytes(m.chunk.legacy.VOCAB*4-4)]:
        f.write_bytes(content)
        try:m.chunk.legacy.read(f)
        except (OSError,ValueError):pass
        else:raise AssertionError('Invalid vector accepted')
prior=Path('/mnt/data/qwen-6.6.14-host-input-kind-7.zip')
if prior.is_file():
    with zipfile.ZipFile(prior) as z:
        import array
        a=array.array('f');b=array.array('f')
        a.frombytes(z.read('text_unified_u128/logits-step-00.f32le'))
        b.frombytes(z.read('text_unified_u256/logits-step-00.f32le'))
        assert len(a)==len(b)==m.chunk.legacy.VOCAB
        assert m.chunk.comparison((a,'text-u128'),(b,'text-u256'))['rmse'] > .20
print('PASS 6.6.15: decision branches, replay and missing/nonfinite controls, real 6.6.14 text vectors')
