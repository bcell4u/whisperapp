#!/usr/bin/env python3
"""Host-only contract controls, no model downloads required."""
import importlib.util
from pathlib import Path
import torch
sp=Path(__file__).with_name('probe-qwen-hf-position-axes.py')
spec=importlib.util.spec_from_file_location('position_6624',sp)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
v=m.position_variants(torch)
assert v['linear_2d'].shape==(1,171)
assert v['equal_3_axis'].shape==(3,1,171)
assert v['equal_4_axis'].shape==(4,1,171)
assert torch.equal(v['equal_3_axis'][0],v['linear_2d'])
assert torch.all(v['suffix_gap_11'][0,:165]==torch.arange(165))
assert torch.all(v['suffix_gap_11'][0,165:]==torch.arange(165,171)+11)
assert torch.all(v['asymmetric_3_axis_control'][1,0,9:165]==torch.arange(9,165)+1)
assert torch.equal(v['asymmetric_3_axis_control'][0,0,:],torch.arange(171))
assert m.assess({'implicit_2d':{'status':'EXECUTED','vs_explicit_2d':{'max_abs':0.0}},
        'suffix_gap_11':{'status':'EXECUTED'}},None,{})=='HF_2D_POSITION_CONTRACT_VERIFIED'
assert m.assess({'implicit_2d':{'status':'EXECUTED','vs_explicit_2d':{'max_abs':.1}},
        'suffix_gap_11':{'status':'EXECUTED'}},None,{})=='INCONCLUSIVE_EXPLICIT_IMPLICIT_2D_DIFFER'
assert m.assess({'implicit_2d':{'status':'UNSUPPORTED_OR_FAILED'}},None,{})=='INCONCLUSIVE_2D_POSITION_CONTROL_FAILED'
try:
    raise ValueError('expected 2D position_ids got (3,1,171) /internal/path')
except ValueError as exc:
    r=m.error_record(exc)
    assert r['status']=='UNSUPPORTED_OR_FAILED' and r['error_type']=='ValueError'
    assert '/internal/path' not in r['error_message']
print('PASS 6.6.24 HF 2D/rank/rotary position contract, failure classification and sanitization')
