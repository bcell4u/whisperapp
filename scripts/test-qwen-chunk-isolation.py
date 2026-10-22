#!/usr/bin/env python3
"""Fail-closed decision, file, and negative controls for the 6.6.10 comparator."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile
p=Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec=importlib.util.spec_from_file_location('chunker',p)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def mk(split_unified,split_chunk,unified_chunk, replay=True):
    def d(v): return {'byte_identical':v==0, 'mean_centered_rmse':v}
    return {'split_vs_unified':d(split_unified),'split_vs_three_stage':d(split_chunk),
            'unified_vs_three_stage':d(unified_chunk),
            'unified_replay':{'byte_identical':replay},
            'three_stage_replay':{'byte_identical':replay}}

assert m.classify([mk(0,0,0)]*5)[0]=='ALL_THREE_IDENTICAL'
assert m.classify([mk(.2,0,.2)]*5)[0]=='CHUNK_GROUPING_EXPLAINS_DIFFERENCE'
assert m.classify([mk(.2,.2,0)]*5)[0]=='API_PATH_EXPLAINS_DIFFERENCE'
assert m.classify([mk(.2,.01,.2)]*5)[0]=='CHUNK_GROUPING_SIMILAR_TO_SPLIT'
assert m.classify([mk(.2,.2,.01)]*5)[0]=='CHUNK_GROUPING_SIMILAR_TO_UNIFIED'
assert m.classify([mk(.2,.17,.14)]*5)[0]=='THREE_WAY_DIFFERENCE_UNRESOLVED'
assert m.classify([mk(0,0,0,False)]*5)[0]=='FAIL_REPLAY'
a=([2.,3.,5.],'a'); b=([3.,4.,6.],'b')
assert abs(m.base.centered_rmse(a,b)['mean_centered_rmse']) < 1e-9
assert abs(m.base.centered_rmse(a,b)['mean_left_minus_right']+1) < 1e-9
fx=Path(__file__).resolve().parents[1]/'verification/fixtures'
assert len(m.legacy.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    for name in ('split','unified','stage','ur','sr'):
        (root/name).mkdir()
    try:
        m.evaluate(fx,root/'split',root/'unified',root/'stage',root/'ur',root/'sr','pinned')
        raise AssertionError('accepted missing files')
    except FileNotFoundError:
        pass
    f=root/'split'/'logits-step-00.f32le'
    f.write_bytes(b'few')
    try:
        m.legacy.read(f)
        raise AssertionError('accepted truncated file')
    except ValueError:
        pass
    f.write_bytes(struct.pack('<f', math.inf)+b'\x00'*(m.legacy.VOCAB*4-4))
    try:
        m.legacy.read(f)
        raise AssertionError('accepted infinity')
    except ValueError:
        pass
print('PASS 6.6.10: agreement branches, replay fail, logit shift, missing/truncated/nonfinite files')
