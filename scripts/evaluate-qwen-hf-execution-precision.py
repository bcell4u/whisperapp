#!/usr/bin/env python3
"""6.6.26 numerical response alignment without arbitrary correctness thresholds."""
import argparse
import importlib.util
import json
import math
from pathlib import Path
import numpy as np

SRC = Path(__file__).with_name('evaluate-qwen-cross-backend-positions.py')
spec = importlib.util.spec_from_file_location('reference_6626', SRC)
ref = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ref)
MODES = ref.MODES
ATTENTION = ref.ATTENTION
BRANCHES = ('bf16', 'fp32-rounded-input', 'fp32-exact-audio')
STATUS = 'SAME_WEIGHTS_EXECUTION_PRECISION_COMPARISON_COMPLETE'


def delta_metrics(a, base, b, base_b):
    """Compare a position response to a reference position response."""
    da = a-base
    db = b-base_b
    na = float(np.linalg.norm(da))
    nb = float(np.linalg.norm(db))
    if na == 0 and nb == 0:
        cosine = 1.0
    elif na == 0 or nb == 0:
        cosine = None
    else:
        cosine = float(np.dot(da,db)/(na*nb))
    return {'response_residual_rmse': float(np.sqrt(np.mean((da-db)**2))),
            'response_cosine': cosine,
            'a_position_delta_rmse': float(np.sqrt(np.mean(da**2))),
            'b_position_delta_rmse': float(np.sqrt(np.mean(db**2)))}


def evaluate(root):
    root = Path(root)
    report = {
        'schema': 1, 'release': '6.6.26',
        'status': 'INCONCLUSIVE_MISSING_OR_MALFORMED_BRANCH',
        'reference_kind': 'SAME_HF_BF16_CHECKPOINT_BF16_VS_FP32_RUNTIME',
        'model_checksums': {'bf16_gguf': ref.BF16_SHA,
                            'hf_safetensors': ref.HF_SHA, 'features': ref.FEATURE_SHA},
        'synthetic_only': True, 'android_inference_modified': False,
        'official_asr_mrope_parity': 'NOT_VERIFIED',
        'limitations': 'FP32 reuses BF16-rounded stored weights. Audio positional grids, '
                       'phone runtime, and recognition accuracy are not validated.',
        'results': {}, 'response_alignment': {}, 'missing_branches': [],
    }
    missing = report['missing_branches']
    hf = {}
    host = {}
    meta = None
    try:
        meta = json.loads((root/'hf'/'HF_EXECUTION_PRECISION_METADATA.json').read_text())
        if (meta.get('hf_revision') != '6aa69c382e2b426eee1f5870d4c95859a74b6445'
            or meta.get('hf_safetensors_sha256') != ref.HF_SHA
            or meta.get('features_sha256') != ref.FEATURE_SHA
            or meta.get('release') != '6.6.26'
            or set(meta.get('position_modes', {})) != set(BRANCHES)):
            raise ValueError('PRECISION_METADATA_CONTRACT_MISMATCH')
    except (FileNotFoundError,ValueError,TypeError) as e:
        missing.append('hf/metadata:' + type(e).__name__)
    for mode in MODES:
        report['results'][mode] = {'hf': {}, 'host': {}, 'hf_cross_dtype': {},
                                   'host_against_hf': {}}
        r = report['results'][mode]
        for branch in BRANCHES:
            try:
                v, sha = ref.read_vector(root/'hf'/branch/f'hf-{mode}.f32le')
                if meta is not None:
                    expected = meta['position_modes'][branch][mode]
                    if sha != expected['sha256'] or int(np.argmax(v)) != expected['top1_token_id']:
                        raise ValueError('VECTOR_METADATA_SHA_OR_TOP1_MISMATCH')
                hf[mode,branch] = v
                r['hf'][branch] = {'sha256': sha, 'top1_token_id': int(np.argmax(v))}
            except (FileNotFoundError,ValueError,KeyError,TypeError) as e:
                missing.append('hf/'+branch+'/'+mode+':'+type(e).__name__)
        for attn in ATTENTION:
            try:
                v,sha = ref.read_vector(root/'host'/mode/attn/'logits-step-00.f32le')
                host[mode,attn] = v
                r['host'][attn] = {'sha256':sha, 'top1_token_id':int(np.argmax(v))}
            except (FileNotFoundError,ValueError) as e:
                missing.append('host/'+mode+'/'+attn+':'+type(e).__name__)
        if (mode,'bf16') in hf:
            for branch in BRANCHES[1:]:
                if (mode,branch) in hf:
                    r['hf_cross_dtype'][branch] = ref.metrics(hf[mode,branch],hf[mode,'bf16'])
        if (mode,'fp32-rounded-input') in hf and (mode,'fp32-exact-audio') in hf:
            r['audio_input_rounding_effect'] = ref.metrics(hf[mode,'fp32-rounded-input'],hf[mode,'fp32-exact-audio'])
        for attn in ATTENTION:
            if (mode,attn) not in host: continue
            r['host_against_hf'][attn] = {}
            for branch in BRANCHES:
                if (mode,branch) in hf:
                    r['host_against_hf'][attn][branch] = ref.metrics(host[mode,attn], hf[mode,branch])
    for mode in MODES:
        if mode == 'pos-linear': continue
        r = report['response_alignment'][mode] = {}
        for branch in BRANCHES:
            if all(key in hf for key in ((mode,branch),('pos-linear',branch))):
                r[branch] = {}
                for attn in ATTENTION:
                    if (mode,attn) in host and ('pos-linear',attn) in host:
                        r[branch][attn] = delta_metrics(hf[mode,branch],hf['pos-linear',branch],
                                                         host[mode,attn],host['pos-linear',attn])
    if not missing:
        report['status'] = STATUS
    return report


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--artifact-dir', required=True)
    p.add_argument('--output', required=True)
    args = p.parse_args()
    report = evaluate(args.artifact_dir)
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, indent=2, sort_keys=True)+'\n')
    print('EXECUTION_PRECISION',report['status'],'missing',len(report['missing_branches']))
    if report['missing_branches']:
        raise SystemExit(3)


if __name__ == '__main__': main()
