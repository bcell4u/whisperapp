#!/usr/bin/env python3
"""Independent pinned HF Qwen decoder: positional-axes *contract* probe.

Tests equal 2D/3D/4D position representations and one deliberately non-equal
4-axis control. This synthetic fixture is NOT a real audio M-RoPE grid and
cannot independently establish official multi-axis ASR correctness.
"""
import argparse, hashlib, json, math, struct
from pathlib import Path

from importlib.util import spec_from_file_location, module_from_spec
refpath=Path(__file__).with_name('produce-qwen-hf-reference.py')
spec=spec_from_file_location('hf_reference',refpath)
ref=module_from_spec(spec);spec.loader.exec_module(ref)
PINNED_HF_SHA='d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'
PINNED_REV='6aa69c382e2b426eee1f5870d4c95859a74b6445'
PINNED_S26_SHA='9951cd0ed4143a97f4181c3fe0b87086aaf7f77b8571c63ea41d5597ef41a8e2'

def metrics(a,b):
    import torch
    delta=(a-b).float()
    return {'rmse':float(torch.sqrt(torch.mean(delta*delta)).item()),
            'max_abs':float(torch.max(torch.abs(delta)).item()),
            'top1_equal':bool(int(a.argmax())==int(b.argmax())),
            'cosine':float(torch.nn.functional.cosine_similarity(a.view(1,-1).float(),b.view(1,-1).float()).item())}

def run(features, checkpoint, s26_logits, output):
    import torch, transformers
    from transformers import Qwen3ASRForConditionalGeneration
    torch.set_num_threads(4)
    vals=ref.verify_features(features)
    import numpy as np
    s26_bytes=Path(s26_logits).read_bytes()
    if len(s26_bytes)!=ref.VOCAB*4 or hashlib.sha256(s26_bytes).hexdigest()!=PINNED_S26_SHA:
        raise ValueError('S26_LOGITS_LENGTH_OR_SHA256_MISMATCH')
    s26=torch.from_numpy(np.frombuffer(s26_bytes,dtype='<f4').copy())
    if not torch.isfinite(s26).all():
        raise ValueError('S26_REFERENCE_NONFINITE')
    checkpoint=Path(checkpoint)
    model_file=checkpoint/'model.safetensors'; config_file=checkpoint/'config.json'
    if not model_file.is_file() or not config_file.is_file() or checkpoint.name!=PINNED_REV:
        raise ValueError('HF_PINNED_CHECKPOINT_PATH_OR_FILES_MISSING')
    digest=hashlib.sha256()
    with model_file.open('rb') as f:
        for block in iter(lambda:f.read(4*1024*1024), b''):digest.update(block)
    if digest.hexdigest()!=PINNED_HF_SHA:
        raise ValueError('HF_CHECKPOINT_SHA256_MISMATCH')
    model=Qwen3ASRForConditionalGeneration.from_pretrained(str(checkpoint),dtype=torch.bfloat16,
                    attn_implementation='eager',local_files_only=True,low_cpu_mem_usage=True).eval().to('cpu')
    ids=list(ref.PREFIX)+[151676]*ref.AUDIO_FRAMES+list(ref.SUFFIX)
    ref.verify_contract(model,ids)
    ids=torch.tensor(ids,dtype=torch.long)[None,:]
    positions=torch.arange(171,dtype=torch.long)[None,:]
    mask=torch.ones_like(ids)
    with torch.inference_mode():
        emb=model.model.language_model.get_input_embeddings()(ids)
        emb[:,len(ref.PREFIX):len(ref.PREFIX)+ref.AUDIO_FRAMES,:]=torch.tensor(vals,dtype=torch.float32).view(1,156,1024).to(emb.dtype)
        def infer(pos):
            out=model(inputs_embeds=emb,attention_mask=mask,position_ids=pos,
                      use_cache=False,logits_to_keep=1,return_dict=True).logits[0,-1].float().cpu().contiguous()
            if out.numel()!=ref.VOCAB or not torch.isfinite(out).all().item():
                raise ValueError('HF_POSITION_PROBE_NONFINITE_OR_WRONG_SHAPE')
            return out
        baseline=infer(positions)
        variants={}
        failures={}
        positions3=positions.unsqueeze(0).expand(3,-1,-1).clone()
        positions4=positions.unsqueeze(0).expand(4,-1,-1).clone()
        positions_asym=positions4.clone()
        positions_asym[1,:,len(ref.PREFIX):len(ref.PREFIX)+ref.AUDIO_FRAMES]+=1
        for key,position in [('equal_3_axis',positions3),('equal_4_axis',positions4),('asymmetric_4_axis_control',positions_asym)]:
            try:
                variant=infer(position)
                variants[key]={'status':'EXECUTED','vs_equal_2d':metrics(variant,baseline),
                              'top1_token_id':int(variant.argmax().item()),
                              'logit_sha256':hashlib.sha256(variant.numpy().astype('<f4',copy=False).tobytes()).hexdigest()}
            except (ValueError,RuntimeError,TypeError,IndexError) as exc:
                # Unsupported variants do not establish equality or inequality.
                variants[key]={'status':'UNSUPPORTED_OR_FAILED','error_type':type(exc).__name__}
                failures[key]=type(exc).__name__
        status=('EQUAL_AXIS_REPRESENTATIONS_AGREE' if
                all(variants.get(k,{}).get('status')=='EXECUTED' and
                    variants[k]['vs_equal_2d']['max_abs']==0 for k in ('equal_3_axis','equal_4_axis'))
                else 'INCONCLUSIVE_EQUAL_AXIS_REPRESENTATIONS')
        # This synthetic offset should be meaningful when 4-axis is supported,
        # but any difference is not evidence of an app defect.
        report={'schema':1,'release':'6.6.23','status':status,'reference_kind':'INDEPENDENT_HF_POSITION_SHAPE_CONTROLS',
                'hf_revision':PINNED_REV,'hf_safetensors_sha256':PINNED_HF_SHA,
                'synthetic_features_sha256':ref.FEATURE_SHA,'model_dtype':'BF16','attention_mode':'eager',
                'transformers_version':transformers.__version__,'torch_version':torch.__version__,
                'baseline_2d_top1':int(baseline.argmax().item()),
                'original_s26_checkpoint_sha256':PINNED_S26_SHA,
                'hf_vs_original_s26_prefill':metrics(baseline,s26),
                'baseline_2d_sha256':hashlib.sha256(baseline.numpy().astype('<f4',copy=False).tobytes()).hexdigest(),
                'variants':variants,'official_mrope_grid_reference':'NOT_ESTABLISHED',
                'official_full_audio_encoder_parity':'NOT_VERIFIED','synthetic_only':True,
                'limitations':'2D and equal-3/4-axis controls; asymmetry intentionally artificial, not official speech position grid'}
    path=Path(output);path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(report,indent=2,sort_keys=True)+'\n')
    print('HF_POSITION_PROBE',status,'baseline_top1',report['baseline_2d_top1'])

def main():
    p=argparse.ArgumentParser();p.add_argument('--features',required=True);p.add_argument('--checkpoint',required=True);p.add_argument('--s26-logits',required=True);p.add_argument('--output',required=True)
    args=p.parse_args();run(args.features,args.checkpoint,args.s26_logits,args.output)
if __name__=='__main__':main()
