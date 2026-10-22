#!/usr/bin/env python3
"""6.6.19 independent-model evaluator controls; no HF model download in static tests."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import zipfile

source=Path(__file__).with_name('evaluate-qwen-independent-reference.py')
spec=importlib.util.spec_from_file_location('hf_eval',source)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
prod=Path(__file__).with_name('produce-qwen-hf-reference.py')
spec2=importlib.util.spec_from_file_location('hf_prod',prod)
p=importlib.util.module_from_spec(spec2);spec2.loader.exec_module(p)
fixture=Path(__file__).parents[1]/'verification/fixtures'
assert len(p.verify_features(fixture/'s26-synthetic-features.f32le'))==156*1024
assert len(p.PREFIX)==9 and len(p.SUFFIX)==6 and p.VOCAB==m.VOCAB
try: p.verify_features(fixture/'s26-prefill-logits.f32le')
except ValueError: pass
else: raise AssertionError('feature hash guard must fail')

# The real uploaded 6.6.18 host vectors exercise the full-size comparator without
# claiming to have executed PyTorch. The copied BF16 vector is a TEST DOUBLE ONLY.
archive=Path('/mnt/data/qwen-6.6.18-host-generation-impact-11.zip')
if archive.is_file():
  with tempfile.TemporaryDirectory() as td,zipfile.ZipFile(archive) as z:
    root=Path(td); hf=root/'hf';hf.mkdir()
    bf_off=z.read('bf16_flash_off_unified_u256/logits-step-00.f32le')
    hf_file=hf/'hf-prefill-logits.f32le';hf_file.write_bytes(bf_off)
    meta={'reference_kind':'INDEPENDENT_HF_TRANSFORMERS_PREFILL','resolved_revision':'6aa69c3_FAKE_FOR_TEST',
          'features_sha256':p.FEATURE_SHA,'attention_implementation':'eager',
          'logits_sha256':hashlib.sha256(bf_off).hexdigest(),'top1_token_id':11528,
          'simulation_only':True}
    (hf/'HF_REFERENCE_METADATA.json').write_text(json.dumps(meta))
    dirs={}
    for k,zpath in [('q8_flash_on','q8_flash_on_unified_u256'),
                    ('bf16_flash_off','bf16_flash_off_unified_u256'),
                    ('bf16_flash_on','bf16_flash_on_unified_u256')]:
      path=root/k;path.mkdir();(path/'logits-step-00.f32le').write_bytes(z.read(zpath+'/logits-step-00.f32le'))
      dirs[k]=path
    out=m.compare(fixture,hf,dirs)
    assert out['status']=='HF_REFERENCE_COMPARED'
    assert out['comparisons']['hf_vs_bf16_flash_off']['rmse']==0
    assert out['bf16_host_closer_to_hf_than_q8']
    assert not out['comparisons']['hf_vs_q8_flash_on']['byte_identical']
    hf_file.write_bytes(bf_off[:-4]);
    try: m.compare(fixture,hf,dirs)
    except ValueError as e:assert 'INVALID_LOGIT_LENGTH' in str(e)
    else:raise AssertionError('short HF output must fail')
    hf_file.write_bytes(bf_off)
    (hf/'HF_REFERENCE_METADATA.json').write_text(json.dumps({**meta,'top1_token_id':12345}))
    try:m.compare(fixture,hf,dirs)
    except ValueError as e:assert 'HF_TOP1_METADATA_MISMATCH' in str(e)
    else:raise AssertionError('inconsistent HF argmax must fail')
print('PASS 6.6.19 independent evaluator: fixture contract, actual host vector controls, corrupt and mismatched reference rejection')
