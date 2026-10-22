#!/usr/bin/env python3
"""Synthetic-only positive/negative controls for 6.6.18 release."""
import importlib.util
import json
import tempfile
from pathlib import Path

path=Path(__file__).with_name('evaluate-qwen-generation-impact.py')
spec=importlib.util.spec_from_file_location('greedy_eval',path)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
assert len(m.CASES)==12 and len(m.REPLAYS)==2
real=Path('/mnt/data/qwen-6.6.17-host-attention-path-10.zip')
if real.is_file():
    import zipfile
    with zipfile.ZipFile(real) as z:
        for setting in ('off','on'):
            log=z.read(f'q8_flash_{setting}_unified_u256/inference.log').decode(errors='replace')
            audit=m.audit_graph(log,setting)
            assert audit['valid'],audit
            assert (audit['flash_attention_graph_nodes']>0)==(setting=='on')
        report=json.loads(z.read('ATTENTION_PATH_REPORT.json'))
        assert report['status']=='FLASH_OFF_REDUCES_Q8_BATCH_SENSITIVITY'
        assert report['mean_microbatch_rmse']['q8']['off']==0.0
        assert report['mean_microbatch_rmse']['bf16']['off']==0.0
        assert report['mean_microbatch_rmse']['q8']['on']>0.19
assert not m.audit_graph('llama_context: flash_attn = enabled\nnode (SOFT_MAX)', 'on')['valid']
assert not m.audit_graph('llama_context: flash_attn = disabled\nnode (FLASH_ATTN_EXT)', 'off')['valid']
assert m.first_divergence([10,11],[10,12])==1
assert m.first_divergence([10],[10,11])==1
assert m.first_divergence([10,11],[10,11]) is None
# A changed numeric score margin, even with unchanged token IDs, must fail replay.
def fake_row(sha):return {'ids':[11528,151645], 'decisions_sha256':sha, 'graph':{'valid':True}, 'logits':(None,None)}
assert fake_row('a')['decisions_sha256']!=fake_row('b')['decisions_sha256']
with tempfile.TemporaryDirectory() as tmp:
    p=Path(tmp);(p/'inference.log').write_text('llama_context: flash_attn = disabled\nnode (SOFT_MAX)')
    (p/'logits-step-00.f32le').write_bytes(b'bad')
    (p/'greedy-decisions.json').write_text(json.dumps({'schema':1,'requested_max_tokens':24,'decisions':[{'token_id':151645,'top2_margin':1.0}],'stopped_on_end_token':True}))
    try:m.load_case(p,'q8_flash_off_unified_u256')
    except ValueError:pass
    else:raise AssertionError('Must reject corrupt logits checkpoint')
print('PASS 6.6.18: real Flash/softmax graph evidence, divergence controls, malformed logits rejection')
