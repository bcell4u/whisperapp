#!/usr/bin/env python3
"""Positive/negative checks for synthetic artifact verification and report semantics."""
import importlib.util
from pathlib import Path
import struct
import tempfile

spec = importlib.util.spec_from_file_location('qref', Path(__file__).with_name('evaluate-qwen-reference.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
fixtures = Path(__file__).resolve().parents[1] / 'verification/fixtures'
assert len(m.verified_fixture(fixtures)) == 2
assert m.summarize(([2.0] * m.VOCAB,'abc'),([2.0]*m.VOCAB,'abc'))['byte_identical']
diff = m.summarize(([2.0]*m.VOCAB,'abc'),([1.0]*m.VOCAB,'def'))
assert not diff['byte_identical'] and diff['max_abs_delta']==1
with tempfile.TemporaryDirectory() as tmp:
    p = Path(tmp)/'logits.f32le'
    p.write_bytes(b'\x00'*12)
    try:
        m.read(p)
        raise AssertionError('accepted truncated logits')
    except ValueError as e:
        assert 'invalid_logit_length' in str(e)
    p.write_bytes(struct.pack('<f',float('nan'))+b'\x00'*(m.VOCAB*4-4))
    try:
        m.read(p)
        raise AssertionError('accepted nonfinite logits')
    except ValueError as e:
        assert 'nonfinite' in str(e)
    bad = Path(tmp)/'fixtures'
    bad.mkdir()
    for name in m.EXPECT:
        (bad/name).write_bytes(b'not the phone fixture')
    try:
        m.verified_fixture(bad)
        raise AssertionError('accepted wrong reference fixture')
    except ValueError as e:
        assert 'sha256_mismatch' in str(e)
print('PASS: fixture origin checks, replay agreement, divergence, truncation, NaN, corrupted SHA')
