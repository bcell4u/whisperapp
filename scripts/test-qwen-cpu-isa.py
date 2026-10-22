#!/usr/bin/env python3
"""Contract, negative-path and historical numerical regression for 6.6.16."""
import importlib.util
import json
import math
from pathlib import Path
import zipfile

root=Path(__file__).resolve().parent
src=root/'evaluate-qwen-cpu-isa.py'
spec=importlib.util.spec_from_file_location('qwen6616',src)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def rows(simd=.21, portable=.21, cross=.03, replay=True):
    result=[]
    for i in range(5):
        r={'step':i}
        for p in m.PRECS:
            for isa in m.ISAS:
                r[f'{p}_{isa}_replay']={'byte_identical':replay}
                for mode in m.MODES:
                    r[f'{p}_{isa}_{mode}_microbatch']={'rmse':simd if isa=='simd' else portable}
            for mode in m.MODES:
                for u in m.UBATCHES:
                    r[f'{p}_{mode}_u{u}_isa']={'rmse':cross}
        result.append(r)
    return result

for a,b,c,expected in [
    (.21,.05,.13,'Q8_BATCH_EFFECT_REDUCED_ON_PORTABLE'),
    (.21,.39,.13,'Q8_BATCH_EFFECT_INCREASED_ON_PORTABLE'),
    (.21,.20,.13,'CPU_ISA_VARIANTS_DIFFER_BATCH_EFFECT_PERSISTS'),
    (.21,.21,.002,'CPU_ISA_VARIANTS_CLOSE_BATCH_EFFECT_PERSISTS'),
    (0,.20,.13,'INCONCLUSIVE_Q8_CONTROL')]:
    assert m.classify(rows(a,b,c))[0]==expected,(a,b,c)
assert m.classify(rows()[:4])[0]=='INCONCLUSIVE_INCOMPLETE'
assert m.classify(rows(replay=False))[0]=='FAIL_REPLAY'
assert len(m.CASES)==16 and len(m.REPLAYS)==4
assert m.chunk.legacy.verified_fixture(root.parent/'verification'/'fixtures')
# Reference artifact from user (optional on CI; available in our authoring environment).
prior=Path('/mnt/data/qwen-6.6.15-host-precision-batch-8.zip')
if prior.is_file():
    with zipfile.ZipFile(prior) as z:
        old=json.loads(z.read('PRECISION_BATCH_REPORT.json'))
        assert old['status']=='Q8_MORE_BATCH_SENSITIVE'
        assert old['mean_microbatch_rmse_across_5_steps']['q8'] > 3*old['mean_microbatch_rmse_across_5_steps']['bf16']
        import array
        a=array.array('f');b=array.array('f')
        a.frombytes(z.read('q8_orig_unified_u128/logits-step-00.f32le'))
        b.frombytes(z.read('q8_orig_unified_u256/logits-step-00.f32le'))
        assert abs(m.chunk.comparison((a,'u128'),(b,'u256'))['rmse']-.20278193037)<1e-6
assert abs(m.mean(rows(), 'q8_simd_unified_microbatch') - .21) < 1e-12
try:
    m.mean([{'bad':{'rmse':math.inf}}], 'bad')
except ValueError: pass
else: raise AssertionError('nonfinite metric accepted')
print('PASS 6.6.16: ISA decision branches, replay failure, missing checkpoints, fixture hashes, real 6.6.15 logits')
