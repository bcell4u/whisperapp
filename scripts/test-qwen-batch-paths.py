#!/usr/bin/env python3
"""Mandatory positive/negative classification and f32 comparison checks for 6.6.9."""
import importlib.util
import math
import tempfile
from pathlib import Path
import struct

path = Path(__file__).with_name('evaluate-qwen-batch-paths.py')
spec=importlib.util.spec_from_file_location('batchpaths',path)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def metrics(rmse): return {'mean_centered_rmse':rmse}
assert m.infer(True, metrics(0.4),metrics(0.2),True)[0]=='BATCH_PATH_IDENTICAL'
assert m.infer(False, metrics(0.4),metrics(0.1),True)[0]=='BATCH_PATH_DIFFERENCE_MIXED_CLOSER_TO_S26'
assert m.infer(False, metrics(0.1),metrics(0.4),True)[0]=='BATCH_PATH_DIFFERENCE_SPLIT_CLOSER_TO_S26'
assert m.infer(False, metrics(0.3),metrics(0.28),True)[0]=='BATCH_PATH_DIFFERENCE_NO_CLEAR_S26_WINNER'
assert m.infer(False, metrics(0.3),metrics(0.28),False)[0]=='FAIL_MIXED_REPLAY'
a=([1.,2.,3.],'a')
b=([2.,3.,4.],'b')
assert m.centered_rmse(a,b)['mean_centered_rmse']<1e-10
assert abs(m.centered_rmse(a,b)['mean_left_minus_right']+1)<1e-10
c=([1.,2.,4.],'c')
assert m.centered_rmse(a,c)['mean_centered_rmse']>0
fx=Path(__file__).resolve().parents[1]/'verification/fixtures'
assert len(m.LEGACY.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as td:
    tmp=Path(td)
    for name in ('split','mixed','replay'):
        (tmp/name).mkdir()
    try:
        m.evaluation(fx,tmp/'split',tmp/'mixed',tmp/'replay','pinned')
        raise AssertionError('Accepted absent logits')
    except FileNotFoundError:
        pass
    p=tmp/'split'/'logits-step-00.f32le'
    p.write_bytes(b'abc')
    try:
        m.LEGACY.read(p)
        raise AssertionError('Accepted truncation')
    except ValueError:
        pass
    p.write_bytes(struct.pack('<f', math.nan)+b'\x00'*(m.LEGACY.VOCAB*4-4))
    try:
        m.LEGACY.read(p)
        raise AssertionError('Accepted NaN')
    except ValueError:
        pass
print('PASS: same-host comparison, offset invariance, decision branches, missing data, truncated and NaN rejection')
