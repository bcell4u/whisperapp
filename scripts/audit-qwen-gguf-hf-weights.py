#!/usr/bin/env python3
"""Read-only tensor identity audit: pinned HF -hf safetensors vs public GGUF BF16.

Only directly comparable, non-RoPE-permuted decoder tensors are audited.
Tensor names and coverage are explicit; unmatched tensors are NOT presumed equal.
No tensor bytes are uploaded or printed. Never claims whole-model parity.
"""
import argparse
import hashlib
import json
import math
import mmap
from pathlib import Path
import re
import struct

GGUF_SHA = '12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50'
HF_SHA = 'd3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35'
HF_REV = '6aa69c382e2b426eee1f5870d4c95859a74b6445'
LIMIT_TENSORS = 10000
LIMIT_KV = 10000
LIMIT_ARRAY = 2000000  # GGUF tokenizer arrays can contain >150k entries


def sha256_file(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(4*1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()


class BinaryReader:
    def __init__(self, raw):
        self.raw = raw
        self.p = 0
    def read(self, n):
        if n < 0 or self.p+n > len(self.raw):
            raise ValueError('INVALID_OR_TRUNCATED_BINARY_HEADER')
        x = self.raw[self.p:self.p+n]
        self.p += n
        return x
    def unpack(self, fmt):
        return struct.unpack('<'+fmt, self.read(struct.calcsize('<'+fmt)))[0]
    def skip(self, n):
        # Skip metadata without creating large temporary strings or lists.
        if n < 0 or self.p+n > len(self.raw):
            raise ValueError('INVALID_OR_TRUNCATED_BINARY_HEADER')
        self.p += n
    def string_length(self):
        n = self.unpack('Q')
        if n > 1024*1024:
            raise ValueError('GGUF_EXCESSIVE_STRING_LENGTH')
        return n
    def text(self):
        return self.read(self.string_length()).decode('utf-8')
    def skip_text(self):
        self.skip(self.string_length())


# GGUF scalar metadata codes. Unknown codes must fail closed even inside
# skipped arrays; successful skipping must never desynchronize tensor offsets.
SCALAR_TYPES = {0:'b',1:'B',2:'h',3:'H',4:'i',5:'I',6:'f',7:'?',10:'Q',11:'q',12:'d'}


def array_header(r):
    kind, length = r.unpack('I'), r.unpack('Q')
    if length > LIMIT_ARRAY:
        raise ValueError('GGUF_EXCESSIVE_ARRAY_LENGTH')
    if kind not in SCALAR_TYPES and kind not in (8,9):
        raise ValueError('UNSUPPORTED_GGUF_METADATA_TYPE_'+str(kind))
    return kind, length


def skip_metadata_value(r, typ, depth=0):
    """Advance through large tokenizer metadata without allocating it."""
    if depth > 4:
        raise ValueError('GGUF_METADATA_DEPTH_EXCEEDED')
    if typ in SCALAR_TYPES:
        r.skip(struct.calcsize('<'+SCALAR_TYPES[typ]))
    elif typ == 8:
        r.skip_text()
    elif typ == 9:
        kind, length = array_header(r)
        if kind in SCALAR_TYPES:
            r.skip(struct.calcsize('<'+SCALAR_TYPES[kind])*length)
        else:
            for _ in range(length):
                skip_metadata_value(r, kind, depth+1)
    else:
        raise ValueError('UNSUPPORTED_GGUF_METADATA_TYPE_'+str(typ))


def metadata_value(r, typ, depth=0):
    if depth > 4:
        raise ValueError('GGUF_METADATA_DEPTH_EXCEEDED')
    if typ in SCALAR_TYPES:
        return r.unpack(SCALAR_TYPES[typ])
    if typ == 8:
        return r.text()
    if typ == 9:
        kind, length = array_header(r)
        # Only tiny arrays are ever retained. Tokenizer tokens/merges (often
        # ~150k strings) are checked and skipped without storing their bytes.
        if length > 16:
            if kind in SCALAR_TYPES:
                r.skip(struct.calcsize('<'+SCALAR_TYPES[kind])*length)
            else:
                for _ in range(length):
                    skip_metadata_value(r, kind, depth+1)
            return ['ARRAY_COUNT', length]
        return [metadata_value(r, kind, depth+1) for _ in range(length)]
    raise ValueError('UNSUPPORTED_GGUF_METADATA_TYPE_'+str(typ))

def parse_gguf(raw):
    r=BinaryReader(raw)
    if r.read(4)!=b'GGUF':
        raise ValueError('GGUF_MAGIC_MISMATCH')
    version, nt, nk = r.unpack('I'), r.unpack('Q'), r.unpack('Q')
    if version not in (2,3) or nt > LIMIT_TENSORS or nk > LIMIT_KV:
        raise ValueError('GGUF_HEADER_CONTRACT_FAILED')
    kv={}
    for _ in range(nk):
        name=r.text()
        typ=r.unpack('I')
        value=metadata_value(r,typ)
        if name in kv:
            raise ValueError('GGUF_DUPLICATE_METADATA')
        kv[name]=value
    tensors={}
    for _ in range(nt):
        name=r.text()
        nd=r.unpack('I')
        if nd < 1 or nd > 4:
            raise ValueError('GGUF_INVALID_N_DIMS')
        shape=[r.unpack('Q') for _ in range(nd)]
        typ=r.unpack('I')
        relative=r.unpack('Q')
        if name in tensors:
            raise ValueError('GGUF_DUPLICATE_TENSOR_NAME')
        tensors[name]={'shape':shape,'type':typ,'relative_offset':relative}
    align=kv.get('general.alignment',32)
    if not isinstance(align,int) or align <= 0 or align > 4096 or align & (align-1):
        raise ValueError('GGUF_ALIGNMENT_INVALID')
    base=(r.p+align-1)//align*align
    for name,t in tensors.items():
        count=math.prod(t['shape'])
        # BF16 = 30, F16 = 1, F32 = 0. Other quantized blocks not audited.
        width={30:2,1:2,0:4}.get(t['type'])
        t['elements']=count
        t['offset']=base+t['relative_offset']
        t['byte_length']=count*width if width else None
        if width and t['offset']+t['byte_length']>len(raw):
            raise ValueError('GGUF_TENSOR_OUT_OF_BOUNDS_'+name)
    return version,kv,tensors


def parse_safetensors(raw):
    if len(raw) < 8:
        raise ValueError('SAFETENSORS_TRUNCATED')
    header_len=struct.unpack_from('<Q',raw,0)[0]
    if header_len > 16*1024*1024 or 8+header_len > len(raw):
        raise ValueError('SAFETENSORS_INVALID_HEADER_LENGTH')
    obj=json.loads(raw[8:8+header_len])
    tensors={}
    for name,t in obj.items():
        if name=='__metadata__':
            continue
        start,end=t['data_offsets']
        shape=t['shape']
        if not isinstance(shape,list) or not (0 <= start <= end <= len(raw)-8-header_len):
            raise ValueError('SAFETENSORS_INVALID_TENSOR_OFFSET')
        tensors[name]={'shape':shape,'dtype':t['dtype'],'offset':8+header_len+start,
                       'byte_length':end-start,'elements':math.prod(shape)}
    return tensors


# Decoder layers only, exclude q/k projections (Q/K RoPE head permutations may differ).
LAYER_MAP = {
    'attn_v.weight': 'self_attn.v_proj.weight',
    'attn_output.weight': 'self_attn.o_proj.weight',
    'ffn_down.weight': 'mlp.down_proj.weight',
    'ffn_gate.weight': 'mlp.gate_proj.weight',
    'ffn_up.weight': 'mlp.up_proj.weight',
    'attn_norm.weight': 'input_layernorm.weight',
    'ffn_norm.weight': 'post_attention_layernorm.weight',
}


def hf_alias(gguf_name, available):
    m=re.fullmatch(r'blk\.(\d+)\.([a-z_]+\.weight)',gguf_name)
    if m and m.group(2) in LAYER_MAP:
        suffix='.layers.'+m.group(1)+'.'+LAYER_MAP[m.group(2)]
    elif gguf_name=='token_embd.weight':
        suffix='.embed_tokens.weight'
    elif gguf_name=='output_norm.weight':
        suffix='.norm.weight'
    elif gguf_name=='output.weight':
        suffix='.lm_head.weight'
    else:
        return None
    found=[name for name in available if name.endswith(suffix)]
    # If two candidate suffixes match, ambiguous rather than silently picking one.
    return found[0] if len(found)==1 else None


def identical_bytes(left, lpos, right, rpos, length, block=4*1024*1024):
    for i in range(0,length,block):
        n=min(block,length-i)
        if left[lpos+i:lpos+i+n]!=right[rpos+i:rpos+i+n]:
            return False
    return True


def sample_bf16_rmse(left,lpos,right,rpos,elements,samples=512):
    if elements==0: return None
    total=0.
    for j in range(min(samples,elements)):
        index=j*(elements-1)//max(1,min(samples,elements)-1)
        a=struct.unpack_from('<H',left,lpos+2*index)[0]
        b=struct.unpack_from('<H',right,rpos+2*index)[0]
        va=struct.unpack('<f',struct.pack('<I',a<<16))[0]
        vb=struct.unpack('<f',struct.pack('<I',b<<16))[0]
        if not (math.isfinite(va) and math.isfinite(vb)):
            return None
        total+=(va-vb)**2
    return math.sqrt(total/min(samples,elements))


def audit(gguf_path,hf_path,verify_hashes=True):
    gguf_path,hf_path=Path(gguf_path),Path(hf_path)
    gguf_sha=sha256_file(gguf_path)
    hf_sha=sha256_file(hf_path)
    if verify_hashes and (gguf_sha!=GGUF_SHA or hf_sha!=HF_SHA):
        raise ValueError('PINNED_MODEL_CHECKSUM_MISMATCH')
    with gguf_path.open('rb') as gf, hf_path.open('rb') as hf:
        with mmap.mmap(gf.fileno(),0,access=mmap.ACCESS_READ) as gg,\
             mmap.mmap(hf.fileno(),0,access=mmap.ACCESS_READ) as hs:
            version,kv,g_tensors=parse_gguf(gg)
            h_tensors=parse_safetensors(hs)
            compared=[]; issues=[]; unmatched=[]
            for name,g in sorted(g_tensors.items()):
                hf_name=hf_alias(name,h_tensors)
                if hf_name is None:
                    unmatched.append(name)
                    continue
                h=h_tensors[hf_name]
                compatible=(g['elements']==h['elements'] and g['type']==30
                            and h['dtype']=='BF16' and g['byte_length']==h['byte_length'])
                if not compatible:
                    issues.append({'gguf_tensor':name,'hf_tensor':hf_name,'reason':'SHAPE_OR_DTYPE_MISMATCH',
                                   'gguf_shape':g['shape'],'gguf_type':g['type'],
                                   'hf_shape':h['shape'],'hf_dtype':h['dtype']})
                    continue
                identical=identical_bytes(gg,g['offset'],hs,h['offset'],h['byte_length'])
                record={'gguf_tensor':name,'hf_tensor':hf_name,'elements':g['elements'],
                        'byte_identical':identical}
                if not identical:
                    record['sample_rmse_bf16']=sample_bf16_rmse(gg,g['offset'],hs,h['offset'],g['elements'])
                compared.append(record)
            exact=sum(x['byte_identical'] for x in compared)
            different=[x for x in compared if not x['byte_identical']]
            layers=sorted({int(m.group(1)) for x in compared if (m:=re.match(r'blk\.(\d+)\.',x['gguf_tensor']))})
            # A partial audit cannot establish identity for q/k RoPE permutations,
            # unselected tensors, or the entire original Qwen checkpoint.
            if len(compared)<20 or len(layers)<10:
                status='INCONCLUSIVE_INSUFFICIENT_TENSOR_COVERAGE'
            elif different:
                status='AUDITED_TENSORS_DIFFER'
            elif issues:
                status='INCONCLUSIVE_INCOMPATIBLE_NAMED_TENSORS'
            else:
                status='AUDITED_TENSORS_BYTE_IDENTICAL'
            metadata={k:v for k,v in kv.items() if k=='general.architecture' or
                      any(key in k for key in ('rope','head_count','embedding_length','block_count','attention'))}
            return {
                'schema':1,'release':'6.6.20','status':status,
                'reference_kind':'CHECKPOINT_TENSOR_IDENTITY_AUDIT',
                'hf_model':'Qwen/Qwen3-ASR-0.6B-hf','hf_revision':HF_REV,
                'gguf_model':'ggml-org/Qwen3-ASR-0.6B-GGUF/bf16',
                'hf_safetensors_sha256':hf_sha,'gguf_bf16_sha256':gguf_sha,
                'gguf_format_version':version,'gguf_tensor_count':len(g_tensors),
                'hf_tensor_count':len(h_tensors),
                'compatible_audited_tensors':len(compared),
                'byte_identical_tensors':exact,
                'different_tensors':len(different),
                'incompatible_named_tensors':len(issues),
                'audited_layer_count':len(layers),'audited_layers':layers,
                'different_examples':different[:24],
                'incompatible_examples':issues[:24],
                'unmapped_gguf_count':len(unmatched),
                'unmapped_gguf_examples':unmatched[:24],
                'gguf_attention_and_rope_metadata':metadata,
                'hf_vs_original_gguf_conversion':'NOT_ESTABLISHED_FOR_UNAUDITED_TENSORS',
                'qk_rope_permutation_covered':False,
                'official_mrope_parity':'UNQUALIFIED',
                'official_full_asr_parity':'NOT_VERIFIED',
                'synthetic_only':True,
                'note':'BF16 raw storage bytes compared for explicitly named, shape-compatible decoder tensors; this is not a proof of whole-checkpoint or position encoding equivalence.'
            }


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--gguf',required=True)
    p.add_argument('--hf',required=True)
    p.add_argument('--output',required=True)
    args=p.parse_args()
    result=audit(args.gguf,args.hf)
    Path(args.output).parent.mkdir(parents=True,exist_ok=True)
    Path(args.output).write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    print('WEIGHT_AUDIT',result['status'],'compatible',result['compatible_audited_tensors'],
          'identical',result['byte_identical_tensors'],'different',result['different_tensors'])

if __name__=='__main__':main()
