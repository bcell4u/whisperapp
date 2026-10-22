#!/usr/bin/env python3
"""Decision and corruption controls for the 6.6.13 threshold verifier."""
import importlib.util
import math
from pathlib import Path
import struct
import tempfile
p=Path(__file__).with_name('evaluate-qwen-microbatch-threshold.py')
s=importlib.util.spec_from_file_location('eval6613',p)
m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

# Create artificial checkpoint comparisons, not simulated model inference.
def rows(u160192=False,t128160=False,t160192=False,replay_bad=False,group_bad=True):
    def c(same):return {'byte_identical':same,'rmse':0. if same else 0.1}
    row={'group_256':c(not group_bad),
         'unified_160_vs_192':c(not u160192),
         'three_stage_128_vs_160':c(not t128160),
         'three_stage_160_vs_192':c(not t160192),
         'replay_unified_256':c(not replay_bad),
         'replay_three_stage_256':c(True),
         'replay_unified_160':c(True)}
    for mode in m.MODES:
        for a,b in zip(m.UBATCHES,m.UBATCHES[1:]):
            row.setdefault(f'{mode}_{a}_vs_{b}',c(True))
    # Reproduce the historical 256-vs-64 effect for fallback branch.
    if not (u160192 or t128160 or t160192):
        row['unified_64_vs_128']=c(False)
    return [dict(row) for _ in range(m.STEP_COUNT)]

assert m.classify(rows(u160192=True,t128160=True))[0]=='UNIFIED_AND_AUDIO_THRESHOLD_SENSITIVE'
assert m.classify(rows(u160192=True))[0]=='UNIFIED_PROMPT_THRESHOLD_SENSITIVE'
assert m.classify(rows(t128160=True))[0]=='THREE_STAGE_AUDIO_THRESHOLD_SENSITIVE'
assert m.classify(rows(t160192=True))[0]=='MICROBATCH_RESPONSE_NOT_ISOLATED'
assert m.classify(rows(replay_bad=True))[0]=='FAIL_REPLAY'
assert m.classify(rows(group_bad=False))[0]=='INCONCLUSIVE_ORIGINAL_GROUPING_NOT_REPRODUCED'
assert m.classify(rows()[:4])[0]=='INCONCLUSIVE_INCOMPLETE'
# Purely identical rows are inconclusive, never parity-pass.
neutral=rows()
neutral[0]['unified_64_vs_128']={'byte_identical':True}
for r in neutral:r['unified_64_vs_128']={'byte_identical':True}
assert m.classify(neutral)[0]=='INCONCLUSIVE_MICROBATCH_EFFECT_NOT_REPRODUCED'
fx=Path(__file__).resolve().parents[1]/'verification'/'fixtures'
assert len(m.old.legacy.verified_fixture(fx))==2
with tempfile.TemporaryDirectory() as t:
    root=Path(t)
    paths={name:root/name for name in m.KEYS+m.REPLAYS}
    for path in paths.values():path.mkdir()
    try:m.evaluate(fx,paths,'pinned')
    except (OSError,ValueError):pass
    else:raise AssertionError('Accepted missing files')
    v=paths['unified_u256']/'logits-step-00.f32le'
    for content in (b'broken',struct.pack('<f',math.nan)+bytes(m.old.legacy.VOCAB*4-4)):
        v.write_bytes(content)
        try:m.old.legacy.read(v)
        except ValueError:pass
        else:raise AssertionError('Accepted invalid checkpoint')
print('PASS 6.6.13: threshold decision, replay, baseline, fixture and corrupt-input controls')
