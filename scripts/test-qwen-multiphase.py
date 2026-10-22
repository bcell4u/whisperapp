#!/usr/bin/env python3
"""Self-contained contracts for Q/K layout candidates and multiphase result failure modes."""
import importlib.util,json,struct,tempfile
from pathlib import Path
import numpy as np
root=Path(__file__).parent

def module(filename):
    spec=importlib.util.spec_from_file_location(filename.replace('-','_').replace('.py',''),root/filename)
    x=importlib.util.module_from_spec(spec);spec.loader.exec_module(x);return x

qk=module('audit-qwen-qk-layout.py'); multi=module('evaluate-qwen-multiphase.py')
# Two BF16 matrices: 2 heads, 6 rotary dimensions and 2 input features.
# The two row-permutation candidates now produce DIFFERENT byte orders.
arr=np.arange(1,25,dtype='<u2').reshape(12,2)
hbytes=arr.tobytes()
g0=arr.tobytes()
g1=arr.reshape(2,2,3,2).transpose(0,2,1,3).copy().tobytes()
g2=arr.reshape(2,3,2,2).transpose(0,2,1,3).copy().tobytes()
assert g1 != g2
meta_g={'type':30,'shape':[2,12],'byte_length':48,'offset':0}
meta_h={'dtype':'BF16','shape':[12,2],'byte_length':48,'offset':0}
assert qk.compare_gg_hf(g0,hbytes,meta_g,meta_h,2)=='RAW_BF16_IDENTICAL'
assert qk.compare_gg_hf(g1,hbytes,meta_g,meta_h,2)=='PER_HEAD_SPLIT_TO_INTERLEAVED_IDENTICAL'
assert qk.compare_gg_hf(g2,hbytes,meta_g,meta_h,2)=='PER_HEAD_INTERLEAVED_TO_SPLIT_IDENTICAL'
corrupt=bytearray(g1);corrupt[0]^=1
assert qk.compare_gg_hf(corrupt,hbytes,meta_g,meta_h,2)=='NO_TESTED_LAYOUT_MATCH'
assert qk.compare_gg_hf(g0,hbytes,{**meta_g,'type':0},meta_h,2)=='INCOMPATIBLE_LAYOUT_OR_DTYPE'
assert qk.compare_gg_hf(corrupt,hbytes,meta_g,meta_h,None)=='INCONCLUSIVE_NO_VALID_HEAD_GEOMETRY'
with tempfile.TemporaryDirectory() as tmp:
    p=Path(tmp);r=multi.evaluate(p)
    assert r['status']=='PARTIAL_PHASE_FAILURE' and all(not x['available'] for x in r['phases'].values())
    for phase,file in multi.NAMES.items():
        status={'storage':'AUDITED_TENSORS_EQUAL_WITH_STORAGE_CONVERSION','qk':'ALL_QK_LAYOUTS_MATCH_TESTED_CANDIDATES','positions':'EQUAL_AXIS_REPRESENTATIONS_AGREE'}[phase]
        (p/file).write_text(json.dumps({'release':'6.6.23','status':status}))
    assert multi.evaluate(p)['status']=='MULTIPHASE_CONTROLS_COMPLETED'
    (p/multi.NAMES['storage']).write_text(json.dumps({'release':'6.6.23','status':'AUDITED_TENSORS_DIFFER'}))
    assert multi.evaluate(p)['status']=='CONFIRMED_AUDITED_STORAGE_DIFFERENCE'
    (p/multi.NAMES['storage']).write_text(json.dumps({'release':'6.6.23','status':'AUDITED_TENSORS_EQUAL_WITH_STORAGE_CONVERSION'}))
    (p/multi.NAMES['qk']).write_text(json.dumps({'release':'6.6.23','status':'INCONCLUSIVE_QK_LAYOUT_COVERAGE'}))
    assert multi.evaluate(p)['status']=='COMPLETED_WITH_UNRESOLVED_CONTROLS'
    (p/multi.NAMES['positions']).write_text('{broken')
    assert multi.evaluate(p)['status']=='PARTIAL_PHASE_FAILURE'
print('PASS 6.6.23 multiphase layout candidates, malformed reports and fail-closed classification')
