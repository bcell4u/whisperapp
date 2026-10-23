#!/usr/bin/env python3
"""6.6.26 independent same-weight BF16 versus FP32 Qwen HF numerical controls.

The FP32 model comes from FP32-casting the *same loaded BF16 weights*, not
from a different checkpoint. Test BF16-rounded input separately from exact FP32
synthetic audio features. No audio encoder, Android inference, or user data.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

REFERENCE = Path(__file__).with_name('produce-qwen-hf-reference.py')
spec = importlib.util.spec_from_file_location('reference_6626', REFERENCE)
ref = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ref)
POSITION_SCRIPT = Path(__file__).with_name('measure-qwen-hf-position-parity.py')
spec2 = importlib.util.spec_from_file_location('position_6626', POSITION_SCRIPT)
pos = importlib.util.module_from_spec(spec2)
spec2.loader.exec_module(pos)
MODES = pos.MODES
BRANCHES = ('bf16', 'fp32-rounded-input', 'fp32-exact-audio')
REV = pos.REV
HF_SHA = pos.HF_SHA


def verify_same_weight_precision(model, torch, expected_dtype):
    """Fail closed instead of treating an unconverted model as an FP32 run."""
    dtypes = {p.dtype for p in model.parameters() if p.is_floating_point()}
    if dtypes != {expected_dtype}:
        raise ValueError('HF_EXECUTION_DTYPE_MISMATCH:' + repr(dtypes))


def make_input_controls(torch, model, features, ids):
    # Construct token embeddings and audio replacements only once, in BF16.
    # This ensures the FP32-rounded branch consumes *identical* input values.
    embeds = model.model.language_model.get_input_embeddings()(ids).detach().clone()
    if embeds.dtype != torch.bfloat16 or tuple(embeds.shape) != (1, 171, 1024):
        raise ValueError('BF16_EMBEDDINGS_WRONG_DTYPE_OR_SHAPE')
    audio_exact = torch.tensor(features, dtype=torch.float32).reshape(1, 156, 1024)
    embeds[:, 9:165, :] = audio_exact.to(torch.bfloat16)
    return embeds, audio_exact


def write_branch(torch, model, tensor, ids, branch, dest, reports):
    out = Path(dest)/branch
    out.mkdir(parents=True, exist_ok=True)
    branch_reports = {}
    for mode in MODES:
        p = pos.positions(torch, mode)
        logits = model(inputs_embeds=tensor,
                       attention_mask=torch.ones_like(ids), position_ids=p,
                       use_cache=False, logits_to_keep=1, return_dict=True).logits[0, -1].float().cpu().contiguous()
        if tuple(logits.shape) != (ref.VOCAB,) or not bool(torch.isfinite(logits).all()):
            raise ValueError('NONFINITE_OR_WRONG_HF_LOGITS:' + branch + ':' + mode)
        payload = logits.numpy().astype('<f4', copy=False).tobytes()
        if len(payload) != ref.VOCAB*4:
            raise ValueError('INCOMPLETE_HF_VECTOR')
        (out/f'hf-{mode}.f32le').write_bytes(payload)
        branch_reports[mode] = {
            'sha256': hashlib.sha256(payload).hexdigest(),
            'top1_token_id': int(logits.argmax().item()),
            'position_min': int(p.min()), 'position_max': int(p.max()),
        }
        print('HF_EXECUTION_BRANCH', branch, mode, 'top1', branch_reports[mode]['top1_token_id'], flush=True)
    reports[branch] = branch_reports


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--checkpoint', required=True)
    ap.add_argument('--features', required=True)
    ap.add_argument('--output-dir', required=True)
    args = ap.parse_args()
    import torch
    import transformers
    from transformers import Qwen3ASRForConditionalGeneration
    torch.set_num_threads(4)
    features = ref.verify_features(args.features)
    pos.guard_checkpoint(args.checkpoint)
    model = Qwen3ASRForConditionalGeneration.from_pretrained(
        args.checkpoint, dtype=torch.bfloat16, attn_implementation='eager',
        local_files_only=True, low_cpu_mem_usage=True).eval().to('cpu')
    ids_list = list(ref.PREFIX)+[151676]*ref.AUDIO_FRAMES+list(ref.SUFFIX)
    ref.verify_contract(model, ids_list)
    ids = torch.tensor(ids_list, dtype=torch.long).view(1, 171)
    out = Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)
    reports = {}
    with torch.inference_mode():
        verify_same_weight_precision(model, torch, torch.bfloat16)
        bf16_embeds, audio_exact = make_input_controls(torch, model, features, ids)
        write_branch(torch, model, bf16_embeds, ids, 'bf16', out, reports)
        # All stored HF checkpoint tensors stay identical. Only runtime dtype changes.
        model = model.to(dtype=torch.float32).eval()
        verify_same_weight_precision(model, torch, torch.float32)
        fp32_rounded = bf16_embeds.float()
        write_branch(torch, model, fp32_rounded, ids, 'fp32-rounded-input', out, reports)
        fp32_exact = fp32_rounded.clone()
        fp32_exact[:, 9:165, :] = audio_exact
        write_branch(torch, model, fp32_exact, ids, 'fp32-exact-audio', out, reports)
    (out/'HF_EXECUTION_PRECISION_METADATA.json').write_text(json.dumps({
        'schema': 1, 'release': '6.6.26', 'status': 'SAME_BF16_WEIGHTS_MULTI_DTYPE_EXPORTED',
        'hf_revision': REV, 'hf_safetensors_sha256': HF_SHA,
        'features_sha256': ref.FEATURE_SHA,
        'torch_version': torch.__version__, 'transformers_version': transformers.__version__,
        'attention_implementation': 'eager', 'runtime_branches': {
            'bf16': 'BF16 arithmetic and BF16-rounded inputs',
            'fp32-rounded-input': 'FP32 arithmetic, BF16 source model weights and BF16-rounded inputs',
            'fp32-exact-audio': 'FP32 arithmetic, BF16 source model weights and unrounded FP32 audio features',
        }, 'position_modes': reports, 'synthetic_only': True,
        'official_audio_mrope_parity': 'NOT_VERIFIED',
        'android_inference_changed': False,
    }, indent=2, sort_keys=True)+'\n')
    print('PASS: independent BF16/FP32 same-weight execution-precision controls', flush=True)


if __name__ == '__main__':
    main()
