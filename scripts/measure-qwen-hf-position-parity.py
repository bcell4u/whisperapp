#!/usr/bin/env python3
"""Pinned, independent Transformers prefill vectors for paired GGUF position maps.

Synthetic deterministic embeddings ONLY. This does not exercise official ASR
position-grid construction or prove M-RoPE parity in real audio.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

REFERENCE=Path(__file__).with_name('produce-qwen-hf-reference.py')
spec=importlib.util.spec_from_file_location('reference_6625',REFERENCE)
ref=importlib.util.module_from_spec(spec);spec.loader.exec_module(ref)
REV='6aa69c382e2b426eee1f5870d4c95859a74b6445'
HF_SHA='d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'
MODES=('pos-linear','pos-plus1','pos-plus17','pos-suffix-gap11')

def positions(torch,mode):
    p=torch.arange(171,dtype=torch.long).view(1,171)
    if mode=='pos-plus1':return p+1
    if mode=='pos-plus17':return p+17
    if mode=='pos-suffix-gap11':
        p=p.clone();p[:,165:]+=11;return p
    if mode=='pos-linear':return p
    raise ValueError('UNKNOWN_POSITION_MODE')

def guard_checkpoint(checkpoint):
    checkpoint=Path(checkpoint)
    if checkpoint.name!=REV or not (checkpoint/'config.json').is_file():
        raise ValueError('HF_PINNED_REVISION_MISMATCH')
    p=checkpoint/'model.safetensors';h=hashlib.sha256()
    with p.open('rb') as f:
        for chunk in iter(lambda:f.read(4*1024*1024),b''):h.update(chunk)
    if h.hexdigest()!=HF_SHA:raise ValueError('HF_SHA256_MISMATCH')

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--checkpoint',required=True)
    ap.add_argument('--features',required=True)
    ap.add_argument('--output-dir',required=True)
    args=ap.parse_args()
    import torch,numpy as np,transformers
    from transformers import Qwen3ASRForConditionalGeneration
    torch.set_num_threads(4)
    features=ref.verify_features(args.features)
    guard_checkpoint(args.checkpoint)
    model=Qwen3ASRForConditionalGeneration.from_pretrained(args.checkpoint,dtype=torch.bfloat16,
          attn_implementation='eager',local_files_only=True,low_cpu_mem_usage=True).eval().to('cpu')
    ids=list(ref.PREFIX)+[151676]*ref.AUDIO_FRAMES+list(ref.SUFFIX)
    ref.verify_contract(model,ids)
    ids=torch.tensor(ids,dtype=torch.long).view(1,171)
    language=model.model.language_model
    out=Path(args.output_dir);out.mkdir(parents=True,exist_ok=True)
    reports={}
    with torch.inference_mode():
        embeds=language.get_input_embeddings()(ids)
        embeds[:,9:165,:]=torch.tensor(features,dtype=torch.float32).view(1,156,1024).to(embeds.dtype)
        for mode in MODES:
            p=positions(torch,mode)
            logits=model(inputs_embeds=embeds,attention_mask=torch.ones_like(ids),position_ids=p,
                         use_cache=False,logits_to_keep=1,return_dict=True).logits[0,-1].float().cpu().contiguous()
            if tuple(logits.shape)!=(ref.VOCAB,) or not bool(torch.isfinite(logits).all()):
                raise ValueError('HF_LOGIT_CONTRACT_MISMATCH')
            payload=logits.numpy().astype('<f4',copy=False).tobytes()
            if len(payload)!=ref.VOCAB*4:raise ValueError('HF_LOGIT_BYTES_MISMATCH')
            (out/f'hf-{mode}.f32le').write_bytes(payload)
            reports[mode]={'sha256':hashlib.sha256(payload).hexdigest(),
                           'top1_token_id':int(logits.argmax().item()),
                           'position_min':int(p.min()),'position_max':int(p.max())}
    (out/'HF_POSITION_PARITY_METADATA.json').write_text(json.dumps({
        'schema':1,'release':'6.6.25','status':'INDEPENDENT_HF_VECTORS_EXPORTED',
        'hf_revision':REV,'hf_safetensors_sha256':HF_SHA,'feature_sha256':ref.FEATURE_SHA,
        'torch_version':torch.__version__,'transformers_version':transformers.__version__,
        'dtype':'BF16','attention':'eager','position_modes':reports,'synthetic_only':True,
        'official_audio_mrope_parity':'NOT_VERIFIED'},indent=2,sort_keys=True)+'\n')
    print('INDEPENDENT_HF_POSITION_PARITY',','.join(MODES))

if __name__=='__main__':main()
