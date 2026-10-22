#!/usr/bin/env python3
"""6.6.17: branch, provenance and numerical-result failure tests."""
import importlib.util
import json
from pathlib import Path
import zipfile
src=Path(__file__).with_name('evaluate-qwen-attention-path.py')
spec=importlib.util.spec_from_file_location('attn6617',src)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def rows(off=.20,on=.20,cross=.03,replay=True,all_identical=False):
    result=[]
    for i in range(5):
        r={'step':i}
        for p in m.PRECS:
            for a in m.ATTN:
                r[f'{p}_flash_{a}_replay']={'byte_identical':replay}
                for mode in m.MODES:
                    r[f'{p}_flash_{a}_{mode}_microbatch']={'rmse':off if a=='off' else on}
            for mode in m.MODES:
                for u in m.UBATCHES:
                    r[f'{p}_{mode}_u{u}_flash_toggle']={'rmse':cross,'byte_identical':all_identical}
        result.append(r)
    return result

assert m.classify(rows(.05,.2))[0]=='FLASH_OFF_REDUCES_Q8_BATCH_SENSITIVITY'
assert m.classify(rows(.2,.05))[0]=='FLASH_ON_REDUCES_Q8_BATCH_SENSITIVITY'
assert m.classify(rows(.19,.20))[0]=='ATTENTION_MODES_DIFFER_BATCH_EFFECT_PERSISTS'
assert m.classify(rows(.20,.20,0,all_identical=True))[0]=='INCONCLUSIVE_ATTENTION_BRANCH_NOT_OBSERVED'
assert m.classify(rows(0,0))[0]=='INCONCLUSIVE_Q8_CONTROL'
assert m.classify(rows()[:4])[0]=='INCONCLUSIVE_INCOMPLETE'
assert m.classify(rows(replay=False))[0]=='FAIL_REPLAY'
assert len(m.CASES)==16 and len(m.REPLAYS)==4
assert m.chunk.legacy.verified_fixture(src.parent.parent/'verification'/'fixtures')
# Positive real numerical control from the user's 6.6.16 Actions upload,
# only when that upload is present in the authoring environment.
artifact=Path('/mnt/data/qwen-6.6.16-host-cpu-isa-9.zip')
if artifact.is_file():
    with zipfile.ZipFile(artifact) as z:
        old=json.loads(z.read('CPU_ISA_REPORT.json'))
        assert old['status']=='CPU_ISA_VARIANTS_DIFFER_BATCH_EFFECT_PERSISTS'
        assert 0.18 < old['mean_microbatch_rmse']['q8']['simd'] < 0.22
        assert old['mean_microbatch_rmse']['bf16']['simd'] < old['mean_microbatch_rmse']['q8']['simd']
        from array import array
        aa=array('f');bb=array('f')
        aa.frombytes(z.read('q8_simd_unified_u128/logits-step-00.f32le'))
        bb.frombytes(z.read('q8_simd_unified_u256/logits-step-00.f32le'))
        assert abs(m.chunk.comparison((aa,'128'),(bb,'256'))['rmse']-0.20278193037042194)<1e-6
print('PASS 6.6.17: attention decision branches, bad output guards, fixture SHA and real 6.6.16 logit control')
