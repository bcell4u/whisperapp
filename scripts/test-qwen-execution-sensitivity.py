#!/usr/bin/env python3
"""Positive, negative, replay, and corruption controls for v6.6.12."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile

path = Path(__file__).with_name('evaluate-qwen-execution-sensitivity.py')
spec = importlib.util.spec_from_file_location('es_6612', path)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def make_rows(base=(1,0), micro=(1,0), thread=(1,0), fail_replay=False):
    def cmp(a,b):
        return {'byte_identical':a==b, 'rmse':abs(a-b)}
    a,b = base; c,d=micro; e,f=thread
    x={'base_grouping':cmp(a,b),
       'microbatch_effect_unified':cmp(a,c),
       'microbatch_effect_three_stage':cmp(b,d),
       'thread_effect_unified':cmp(a,e),
       'thread_effect_three_stage':cmp(b,f)}
    for key in ('unified_base_replay','three_stage_base_replay','unified_micro64_replay'):
        x[key]={'byte_identical':not fail_replay}
    return [x.copy() for _ in range(m.old.legacy.N)]

assert m.status_of(make_rows())[0] == 'NO_MICROBATCH_OR_THREAD_EFFECT'
assert m.status_of(make_rows(micro=(2,0)))[0] == 'MICROBATCH_SENSITIVE'
assert m.status_of(make_rows(thread=(2,0)))[0] == 'THREAD_SENSITIVE'
assert m.status_of(make_rows(micro=(2,0),thread=(2,0)))[0] == 'BOTH_MICROBATCH_AND_THREAD_SENSITIVE'
assert m.status_of(make_rows(base=(0,0)))[0] == 'INCONCLUSIVE_BASELINE_CHANGED'
assert m.status_of(make_rows(fail_replay=True))[0] == 'FAIL_REPLAY'
assert m.status_of(make_rows()[:4])[0] == 'INCONCLUSIVE_INCOMPLETE'
fx = Path(__file__).resolve().parents[1] / 'verification' / 'fixtures'
assert len(m.old.legacy.verified_fixture(fx)) == 2
with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp)
    paths={key:root/key for key in m.CASES}
    for folder in paths.values(): folder.mkdir()
    try:
        m.evaluate(fx, paths,'pinned')
        raise AssertionError('missing checkpoints accepted')
    except (OSError,ValueError): pass
    p=paths['unified_base']/'logits-step-00.f32le'
    for blob in (b'bad',struct.pack('<f',math.inf)+bytes(m.old.legacy.VOCAB*4-4)):
        p.write_bytes(blob)
        try:
            m.old.legacy.read(p)
            raise AssertionError('invalid logits accepted')
        except ValueError: pass
print('PASS 6.6.12: 7 decision classes/controls, fixture integrity, missing/truncated/nonfinite vectors')
