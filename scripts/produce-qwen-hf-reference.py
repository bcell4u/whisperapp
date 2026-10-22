#!/usr/bin/env python3
"""Host-only, independent HF Qwen ASR prefill logits from pinned synthetic embeddings.

No audio, real transcript, text decoding, network permissions, or Android linkage.
The HF checkpoint is a separate converted model; mismatch is an investigation, not
proof of an erroneous phone decoder. This fixture exercises only equal-axis positions.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import struct

AUDIO_FRAMES=156
DIM=1024
VOCAB=151936
PREFIX=(151644,8948,198,151645,198,151644,872,198,151669)
SUFFIX=(151670,151645,198,151644,77091,198)
FEATURE_SHA='e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee'
HF_MODEL='Qwen/Qwen3-ASR-0.6B-hf'
# Prefix verified on the Qwen HF model repository; exact HF commit resolved and recorded.
HF_REV='6aa69c3'


def verify_features(path):
    raw=Path(path).read_bytes()
    if len(raw)!=AUDIO_FRAMES*DIM*4 or hashlib.sha256(raw).hexdigest()!=FEATURE_SHA:
        raise ValueError('SYNTHETIC_INPUT_HASH_OR_LENGTH_MISMATCH')
    values=struct.unpack('<%df'%(AUDIO_FRAMES*DIM),raw)
    if not all(__import__('math').isfinite(v) for v in values):
        raise ValueError('NONFINITE_SYNTHETIC_FEATURE')
    return values


def verify_contract(model, full_ids):
    cfg=model.config
    t=cfg.text_config
    if t.hidden_size!=DIM or t.vocab_size!=VOCAB or t.num_hidden_layers!=28:
        raise ValueError('HF_ARCHITECTURE_MISMATCH')
    if getattr(t,'rope_parameters',{}).get('rope_theta')!=1000000:
        raise ValueError('HF_ROPE_BASE_MISMATCH')
    if len(full_ids)!=171 or full_ids[:9]!=list(PREFIX) or full_ids[-6:]!=list(SUFFIX):
        raise ValueError('PROMPT_CONTRACT_MISMATCH')
    if cfg.audio_token_id!=151676:
        raise ValueError('HF_AUDIO_TOKEN_ID_MISMATCH')


def run(args):
    import torch
    import transformers
    from huggingface_hub import snapshot_download
    from transformers import Qwen3ASRForConditionalGeneration

    torch.set_num_threads(4)
    values=verify_features(args.features)
    # Downloaded weights must be from one resolved, fixed checkpoint.
    snap=Path(snapshot_download(repo_id=HF_MODEL, revision=HF_REV,
                                allow_patterns=['config.json','*.safetensors','*.safetensors.index.json'],
                                local_dir=None))
    revision=snap.name
    if not revision.startswith(HF_REV):
        raise ValueError('HF_RESOLVED_REVISION_MISMATCH')
    weight_files=sorted(snap.glob('*.safetensors'))
    if not weight_files:raise ValueError('HF_CHECKPOINT_WEIGHTS_MISSING')
    checkpoint_shas={}
    for file in weight_files:
        sha=hashlib.sha256()
        with file.open('rb') as stream:
            for block in iter(lambda:stream.read(4*1024*1024),b''):
                sha.update(block)
        checkpoint_shas[file.name]=sha.hexdigest()
    model=Qwen3ASRForConditionalGeneration.from_pretrained(
        str(snap), dtype=torch.bfloat16, attn_implementation='eager',
        low_cpu_mem_usage=True, local_files_only=True).eval().to('cpu')
    ids=list(PREFIX)+[151676]*AUDIO_FRAMES+list(SUFFIX)
    verify_contract(model,ids)
    with torch.inference_mode():
        input_ids=torch.tensor(ids,device='cpu',dtype=torch.long).unsqueeze(0)
        # Text token embeddings and replacement audio features use published HF weights.
        embeds=model.model.language_model.get_input_embeddings()(input_ids)
        if tuple(embeds.shape)!=(1,171,DIM):
            raise ValueError('HF_EMBEDDING_SHAPE_MISMATCH')
        audio=torch.tensor(values,dtype=torch.float32).view(1,AUDIO_FRAMES,DIM)
        embeds[:,len(PREFIX):len(PREFIX)+AUDIO_FRAMES,:]=audio.to(embeds.dtype)
        # The current synthetic fixture has equal sequential positions on all axes.
        # Qwen -hf text decoder takes a single 2D linear index: NOT a full ASR M-RoPE test.
        positions=torch.arange(171,dtype=torch.long).unsqueeze(0)
        mask=torch.ones_like(input_ids)
        result=model(inputs_embeds=embeds, attention_mask=mask, position_ids=positions,
                     use_cache=False, logits_to_keep=1, return_dict=True)
        logits=result.logits[0,-1].float().contiguous().cpu()
        if logits.numel()!=VOCAB or not torch.isfinite(logits).all().item():
            raise ValueError('HF_LOGITS_SHAPE_OR_FINITE_CHECK_FAILED')
        out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
        # Cross-backend contract: exact vocab-sized little-endian FP32.
        import array,sys
        a=array.array('f',logits.tolist())
        if sys.byteorder!='little':a.byteswap()
        payload=a.tobytes()
        if len(payload)!=VOCAB*4:raise ValueError('HF_LOGIT_EXPORT_INCOMPLETE')
        (out/'hf-prefill-logits.f32le').write_bytes(payload)
        first=int(torch.argmax(logits).item())
    versions={'schema':1,'reference_kind':'INDEPENDENT_HF_TRANSFORMERS_PREFILL',
              'model_id':HF_MODEL,'requested_revision':HF_REV,'resolved_revision':revision,
              'weight_files_sha256':checkpoint_shas,
              'torch_version':torch.__version__,'transformers_version':transformers.__version__,
              'torch_threads':torch.get_num_threads(),'model_dtype':'bfloat16',
              'attention_implementation':'eager', 'audio_input_f32_converted_to_bfloat16':True,
              'audio_encoder_executed':False,'features_sha256':FEATURE_SHA,
              'positions':'linear_2d_equal_axis_fixture_only',
              'legacy_3_axis_and_gguf_4_axis_rope_parity':'UNQUALIFIED',
              'weights_same_as_gguf_verified':False,
              'logits_sha256':hashlib.sha256(payload).hexdigest(), 'top1_token_id':first,
              'synthetic_only':True,'official_full_asr_parity':'NOT_VERIFIED'}
    (out/'HF_REFERENCE_METADATA.json').write_text(json.dumps(versions,indent=2)+'\n')
    print('INDEPENDENT_HF_PREFILL_EXPORTED top1='+str(first)+' resolved_revision='+revision)

if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--features',required=True)
    ap.add_argument('--output',required=True)
    args=ap.parse_args();run(args)
