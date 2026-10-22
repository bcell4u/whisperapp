#!/usr/bin/env python3
"""v6.6.24: independent HF positional-interface diagnostic, synthetic inputs only.

Audits the publicly supported 2D interface, records why optional multi-axis
shapes fail, and probes the model's RoPE module without claiming official audio
M-RoPE parity. No user audio, transcript or model tensors are exported.
"""
import argparse
import hashlib
import importlib.util
import json
import re
import traceback
from pathlib import Path

SCRIPT=Path(__file__).with_name('produce-qwen-hf-reference.py')
spec=importlib.util.spec_from_file_location('qwen_hf_reference_6624',SCRIPT)
ref=importlib.util.module_from_spec(spec);spec.loader.exec_module(ref)
REV='6aa69c382e2b426eee1f5870d4c95859a74b6445'
WEIGHT_SHA='d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'
S26_SHA='9951cd0ed4143a97f4181c3fe0b87086aaf7f77b8571c63ea41d5597ef41a8e2'
SUPPORTED_EXCEPTIONS=(ValueError,RuntimeError,TypeError,IndexError,AssertionError)

def position_variants(torch, size=171, audio_start=9, audio_length=156):
    p=torch.arange(size,dtype=torch.long).view(1,-1)
    boundary=p.clone();boundary[:,audio_start+audio_length:]+=11
    asym3=p.unsqueeze(0).expand(3,-1,-1).clone()
    asym3[1,:,audio_start:audio_start+audio_length]+=1
    asym4=p.unsqueeze(0).expand(4,-1,-1).clone()
    asym4[1,:,audio_start:audio_start+audio_length]+=1
    return {'linear_2d':p,'uniform_plus_17':p+17,'suffix_gap_11':boundary,
            'equal_3_axis':p.unsqueeze(0).expand(3,-1,-1).clone(),
            'equal_4_axis':p.unsqueeze(0).expand(4,-1,-1).clone(),
            'asymmetric_3_axis_control':asym3,
            'asymmetric_4_axis_control':asym4}

def error_record(exc):
    # Diagnostic reason, sanitized to avoid shipping local model paths or buffers.
    msg=re.sub(r'(?:/[^\s:,]+)+','<path>',str(exc))[:240]
    frame=traceback.extract_tb(exc.__traceback__)
    return {'status':'UNSUPPORTED_OR_FAILED', 'error_type':type(exc).__name__,
            'error_message':msg,'origin_function':frame[-1].name if frame else None}

def shape_of(x):
    return list(x.shape) if hasattr(x,'shape') else None

def assess(variants, baseline_default, rotary):
    a=variants.get('implicit_2d',{})
    b=variants.get('suffix_gap_11',{})
    if a.get('status')!='EXECUTED' or b.get('status')!='EXECUTED':
        return 'INCONCLUSIVE_2D_POSITION_CONTROL_FAILED'
    if a.get('vs_explicit_2d',{}).get('max_abs')!=0:
        return 'INCONCLUSIVE_EXPLICIT_IMPLICIT_2D_DIFFER'
    # No requirement that a position change alters final argmax, nor that
    # unsupported 3D/4D inputs should be accepted at the ASR wrapper.
    return 'HF_2D_POSITION_CONTRACT_VERIFIED'

def compare(torch,a,b):
    a=a.float().flatten();b=b.float().flatten()
    diff=a-b
    return {'rmse':float(torch.sqrt(torch.mean(diff*diff)).item()),
            'max_abs':float(torch.abs(diff).max().item()),
            'cosine':float(torch.nn.functional.cosine_similarity(a.view(1,-1),b.view(1,-1)).item()),
            'top1_equal':int(a.argmax())==int(b.argmax())}

def sha_tensor(t):
    return hashlib.sha256(t.float().contiguous().cpu().numpy().astype('<f4',copy=False).tobytes()).hexdigest()

def run(features, checkpoint, s26_logits, output):
    import torch,numpy as np,transformers
    from transformers import Qwen3ASRForConditionalGeneration
    torch.set_num_threads(4)
    vals=ref.verify_features(features)
    raw=Path(s26_logits).read_bytes()
    if len(raw)!=ref.VOCAB*4 or hashlib.sha256(raw).hexdigest()!=S26_SHA:
        raise ValueError('S26_LOGITS_LENGTH_OR_SHA256_MISMATCH')
    s26=torch.from_numpy(np.frombuffer(raw,dtype='<f4').copy())
    if not bool(torch.isfinite(s26).all()): raise ValueError('S26_NONFINITE')
    checkpoint=Path(checkpoint)
    if checkpoint.name!=REV or not (checkpoint/'config.json').is_file():
        raise ValueError('HF_PINNED_REVISION_MISMATCH')
    digest=hashlib.sha256()
    with (checkpoint/'model.safetensors').open('rb') as stream:
        for block in iter(lambda:stream.read(4*1024*1024),b''):digest.update(block)
    if digest.hexdigest()!=WEIGHT_SHA:raise ValueError('HF_WEIGHT_SHA_MISMATCH')
    model=Qwen3ASRForConditionalGeneration.from_pretrained(str(checkpoint),dtype=torch.bfloat16,
                   attn_implementation='eager',local_files_only=True,low_cpu_mem_usage=True).eval().to('cpu')
    ids=list(ref.PREFIX)+[151676]*ref.AUDIO_FRAMES+list(ref.SUFFIX)
    ref.verify_contract(model,ids)
    ids=torch.tensor(ids,dtype=torch.long).view(1,-1)
    positions=position_variants(torch)
    mask=torch.ones_like(ids)
    language=model.model.language_model
    rotary=getattr(language,'rotary_emb',None)
    seen=[]
    hook=None
    if rotary is not None:
        def capture(_module,args,kwargs):
            p=kwargs.get('position_ids',None)
            if p is None and len(args)>1:p=args[1]
            if p is not None and len(seen)<20:seen.append(shape_of(p))
        hook=rotary.register_forward_pre_hook(capture,with_kwargs=True)
    with torch.inference_mode():
        emb=language.get_input_embeddings()(ids)
        emb[:,len(ref.PREFIX):len(ref.PREFIX)+ref.AUDIO_FRAMES,:]=torch.tensor(vals,dtype=torch.float32).view(1,156,1024).to(emb.dtype)
        def infer(p):
            result=model(inputs_embeds=emb,attention_mask=mask,position_ids=p,
                    use_cache=False,logits_to_keep=1,return_dict=True).logits[0,-1].float().cpu().contiguous()
            if result.numel()!=ref.VOCAB or not bool(torch.isfinite(result).all()):
                raise ValueError('INVALID_POSITION_LOGITS')
            return result
        baseline=infer(positions['linear_2d'])
        outcomes={}
        for key in ['implicit_2d','uniform_plus_17','suffix_gap_11',
                    'equal_3_axis','equal_4_axis','asymmetric_3_axis_control','asymmetric_4_axis_control']:
            p=None if key=='implicit_2d' else positions[key]
            try:
                logits=infer(p)
                outcomes[key]={'status':'EXECUTED','input_shape':shape_of(p),
                    'vs_explicit_2d':compare(torch,logits,baseline),
                    'top1_token_id':int(logits.argmax().item()),'logit_sha256':sha_tensor(logits)}
            except SUPPORTED_EXCEPTIONS as exc:
                outcomes[key]={**error_record(exc),'input_shape':shape_of(p)}
        if hook is not None: hook.remove()
        rope_probes={}
        if rotary is None:
            rope_probes['status']='ROTARY_MODULE_UNAVAILABLE'
        else:
            outputs={}
            for key in ['linear_2d','equal_3_axis','equal_4_axis','asymmetric_3_axis_control']:
                try:
                    a,b=rotary(emb,positions[key])
                    if not bool(torch.isfinite(a).all()) or not bool(torch.isfinite(b).all()):
                        raise ValueError('NONFINITE_ROTARY_OUTPUT')
                    outputs[key]=(a.float().cpu(),b.float().cpu())
                    rope_probes[key]={'status':'EXECUTED','cos_shape':shape_of(a),'sin_shape':shape_of(b)}
                except SUPPORTED_EXCEPTIONS as exc:
                    rope_probes[key]=error_record(exc)
            if 'linear_2d' in outputs:
                for key,(a,b) in outputs.items():
                    if key=='linear_2d':continue
                    base=outputs['linear_2d']
                    if a.shape==base[0].shape and b.shape==base[1].shape:
                        rope_probes[key]['max_cos_abs']=float((a-base[0]).abs().max().item())
                        rope_probes[key]['max_sin_abs']=float((b-base[1]).abs().max().item())
                    else:rope_probes[key]['comparison']='INCOMPATIBLE_OUTPUT_SHAPES'
            rope_probes['status']='ROTARY_SHAPE_PROBE_RECORDED'
    status=assess(outcomes,baseline,rope_probes)
    c=model.config.text_config
    report={'schema':2,'release':'6.6.24','status':status,
        'reference_kind':'PINNED_HF_POSITION_INTERFACE_AND_ROTARY_SHAPE_DIAGNOSTIC',
        'hf_revision':REV,'hf_safetensors_sha256':WEIGHT_SHA,
        'synthetic_features_sha256':ref.FEATURE_SHA,'original_s26_checkpoint_sha256':S26_SHA,
        'model_dtype':'BF16','attention_mode':'eager','torch_version':torch.__version__,
        'transformers_version':transformers.__version__,'baseline_2d_top1':int(baseline.argmax()),
        'baseline_2d_sha256':sha_tensor(baseline),'hf_vs_original_s26_prefill':compare(torch,baseline,s26),
        'hf_text_config_class':type(c).__name__,'hf_text_model_class':type(language).__name__,
        'hf_rotary_class':type(rotary).__name__ if rotary is not None else None,
        'hf_rope_parameters':str(getattr(c,'rope_parameters',{}))[:300],
        'actual_rotary_position_shapes':seen,'variants':outcomes,'direct_rotary':rope_probes,
        'gguf_rope_dimension_sections_observed_previous_run':[24,20,20,0],
        'official_mrope_grid_reference':'NOT_ESTABLISHED','official_full_asr_parity':'NOT_VERIFIED',
        'synthetic_only':True,
        'limitations':'2D and candidate-rank API controls on a synthetic fixture. A rejected rank is not evidence of a defective model. Real ASR positional grids, GGUF RoPE application and medical accuracy are unverified.'}
    out=Path(output);out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(report,indent=2,sort_keys=True)+'\n')
    print('HF_POSITION_CONTRACT',status,'top1',report['baseline_2d_top1'])

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--features',required=True);ap.add_argument('--checkpoint',required=True)
    ap.add_argument('--s26-logits',required=True);ap.add_argument('--output',required=True)
    a=ap.parse_args();run(a.features,a.checkpoint,a.s26_logits,a.output)
if __name__=='__main__':main()
