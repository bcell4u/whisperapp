#!/usr/bin/env python3
"""6.6.11 positive, negative, and CI-artifact integrity tests."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile
p=Path(__file__).with_name('evaluate-qwen-boundary-isolation.py')
spec=importlib.util.spec_from_file_location('isolate',p)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def rows(U,PA,AS,T,failed_replay=False,failed_standard=False):
    def cmp(a,b): return {'byte_identical':a==b,'rmse':abs(a-b)}
    data={'unified_vs_three_stage':cmp(U,T),
          'unified_vs_prefix_audio':cmp(U,PA),
          'unified_vs_audio_suffix':cmp(U,AS),
          'three_stage_vs_prefix_audio':cmp(T,PA),
          'three_stage_vs_audio_suffix':cmp(T,AS),
          'split_vs_three_stage':{'byte_identical':not failed_standard}}
    for key in ('unified','three_stage','prefix_audio','audio_suffix'):
        data[key+'_replay']={'byte_identical':not failed_replay}
    return [data.copy() for _ in range(m.legacy.N)]

assert m.status_of(rows(0,0,0,0))[0]=='ALL_GROUPINGS_IDENTICAL'
assert m.status_of(rows(1,1,0,0))[0]=='PREFIX_AUDIO_CUT_DOMINANT'
assert m.status_of(rows(1,0,1,0))[0]=='AUDIO_SUFFIX_CUT_DOMINANT'
assert m.status_of(rows(1,0,0,0))[0]=='EITHER_SINGLE_CUT_SUFFICIENT'
assert m.status_of(rows(1,1,1,0))[0]=='BOTH_CUTS_REQUIRED'
assert m.status_of(rows(1,.3,.7,0))[0]=='GROUPING_INTERACTION_UNRESOLVED'
assert m.status_of(rows(1,1,0,0,failed_replay=True))[0]=='FAIL_REPLAY'
assert m.status_of(rows(1,1,0,0,failed_standard=True))[0]=='INCONCLUSIVE_REFERENCE_CHANGED'
assert m.status_of(rows(1,1,0,0)[:4])[0]=='INCONCLUSIVE_INCOMPLETE'
# Fixture must always be SHA-pinned, sizes and nonfinite values fail closed.
fx=Path(__file__).resolve().parents[1]/'verification/fixtures'
assert len(m.legacy.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    paths={key:root/key for key in m.PATHS}
    for folder in paths.values(): folder.mkdir()
    try:
        m.evaluate(fx,paths,'pinned')
        raise AssertionError('Accepted missing checkpoint')
    except (OSError,ValueError):pass
    f=paths['unified']/'logits-step-00.f32le'
    for blob in (b'short',struct.pack('<f',math.nan)+b'\0'*(m.legacy.VOCAB*4-4)):
        f.write_bytes(blob)
        try:
            m.legacy.read(f)
            raise AssertionError('Accepted corrupt logit vector')
        except ValueError:pass
print('PASS 6.6.11: 7 classification branches, missing checkpoint, truncated and nonfinite numerical outputs')
