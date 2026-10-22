#!/usr/bin/env python3
"""6.6.22 read-only Q/K weight layout audit against the pinned HF decoder.

Tests direct BF16 storage AND two explicit per-head rotary permutations.
Candidate matching is evidence of layout equivalence only, not RoPE
position-axis parity or attention-kernel correctness. Reports no weight data.
"""
import argparse, importlib.util, json, mmap, math, re
from pathlib import Path
import numpy as np
spec=importlib.util.spec_from_file_location('weight_audit', Path(__file__).with_name('audit-qwen-gguf-hf-weights.py'))
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)

def compare_gg_hf(gg, hs, g, h, heads):
    if g['type'] != 30 or h['dtype'] != 'BF16' or len(h['shape']) != 2 or \
            len(g['shape']) != 2 or list(reversed(g['shape'])) != h['shape'] or \
            g['byte_length'] != h['byte_length']:
        return 'INCOMPATIBLE_LAYOUT_OR_DTYPE'
    output_dim, input_dim = h['shape']
    left=np.frombuffer(gg, dtype='<u2', count=output_dim*input_dim, offset=g['offset'])
    right=np.frombuffer(hs, dtype='<u2', count=output_dim*input_dim, offset=h['offset'])
    if np.array_equal(left,right):return 'RAW_BF16_IDENTICAL'
    if not heads or output_dim%heads:return 'INCONCLUSIVE_NO_VALID_HEAD_GEOMETRY'
    dim=output_dim//heads
    if dim%2:return 'INCONCLUSIVE_ODD_HEAD_DIM'
    # Two opposite explicit LLaMA/GPT-NeoX-type rotary row shuffles.
    w=right.reshape(heads,dim,input_dim)
    first=w.reshape(heads,2,dim//2,input_dim).transpose(0,2,1,3).reshape(-1)
    second=w.reshape(heads,dim//2,2,input_dim).transpose(0,2,1,3).reshape(-1)
    if np.array_equal(left,first):return 'PER_HEAD_SPLIT_TO_INTERLEAVED_IDENTICAL'
    if np.array_equal(left,second):return 'PER_HEAD_INTERLEAVED_TO_SPLIT_IDENTICAL'
    return 'NO_TESTED_LAYOUT_MATCH'

def audit(gguf,hf,verify_hashes=True):
    gguf,hf=Path(gguf),Path(hf)
    gs=a.sha256_file(gguf); hs_sha=a.sha256_file(hf)
    if verify_hashes and (gs!=a.GGUF_SHA or hs_sha!=a.HF_SHA):
        raise ValueError('PINNED_MODEL_CHECKSUM_MISMATCH')
    with gguf.open('rb') as gf,hf.open('rb') as hf_file:
        with mmap.mmap(gf.fileno(),0,access=mmap.ACCESS_READ) as gg, mmap.mmap(hf_file.fileno(),0,access=mmap.ACCESS_READ) as hs:
            _,kv,gt=a.parse_gguf(gg); ht=a.parse_safetensors(hs)
            # Do not guess heads from output dimensions. Require published
            # GGUF metadata or explicitly classify the permutation as unknown.
            head_count={}
            for family,k in [('q','attention.head_count'),('k','attention.head_count_kv')]:
                found=[value for name,value in kv.items() if name.endswith('.'+k) and isinstance(value,int)]
                head_count[family]=found[0] if len(found)==1 and 0<found[0]<=256 else None
            records=[]
            for name,g in sorted(gt.items()):
                m=re.fullmatch(r'blk\.(\d+)\.attn_([qk])\.weight',name)
                if not m: continue
                layer=int(m.group(1)); family=m.group(2)
                expected=f'model.language_model.layers.{layer}.self_attn.{family}_proj.weight'
                if expected not in ht:
                    records.append({'tensor':name,'layer':layer,'kind':family,'status':'INCONCLUSIVE_MISSING_HF_TENSOR'})
                    continue
                result=compare_gg_hf(gg,hs,g,ht[expected],head_count[family])
                records.append({'tensor':name,'layer':layer,'kind':family,'status':result,
                                'head_count_from_gguf':head_count[family],
                                'elements':g['elements']})
            ok={'RAW_BF16_IDENTICAL','PER_HEAD_SPLIT_TO_INTERLEAVED_IDENTICAL','PER_HEAD_INTERLEAVED_TO_SPLIT_IDENTICAL'}
            results={name:sum(x['status']==name for x in records) for name in sorted({x['status'] for x in records})}
            all_ok=(len(records)==56 and all(x['status'] in ok for x in records))
            return {'schema':1,'release':'6.6.22','reference_kind':'PINNED_HF_GGUF_QK_LAYOUT_CANDIDATES',
                    'status':'ALL_QK_LAYOUTS_MATCH_TESTED_CANDIDATES' if all_ok else 'INCONCLUSIVE_QK_LAYOUT_COVERAGE',
                    'qk_tensor_count':len(records),'qk_layers_covered':len({x['layer'] for x in records}),
                    'match_counts':results,'head_counts_from_gguf':head_count,
                    'unmatched_examples':[x for x in records if x['status'] not in ok][:16],
                    'record_by_layer':records,
                    'gguf_bf16_sha256':gs,'hf_safetensors_sha256':hs_sha,
                    'official_mrope_position_parity':'NOT_VERIFIED','synthetic_only':True,
                    'scope':'Q/K matrix BF16 storage layout only; RoPE application and axis mapping are not established'}

def main():
    p=argparse.ArgumentParser(); p.add_argument('--gguf',required=True);p.add_argument('--hf',required=True);p.add_argument('--output',required=True)
    o=p.parse_args();result=audit(o.gguf,o.hf)
    Path(o.output).parent.mkdir(parents=True,exist_ok=True)
    Path(o.output).write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    print('QK_LAYOUT',result['status'],result['match_counts'])
if __name__=='__main__':main()
