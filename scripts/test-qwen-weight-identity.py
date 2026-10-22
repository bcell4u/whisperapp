#!/usr/bin/env python3
"""Self-contained positive/negative controls for 6.6.20 tensor identity audit.

Tiny, entirely invented 10-layer tensors test the GGUF/safetensors parsers,
BF16 representation, suffix mapping and fail-closed checksum contracts.
These controls are NOT evidence that public HF and GGUF checkpoints match.
"""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile

spec=importlib.util.spec_from_file_location('weight_audit',Path(__file__).with_name('audit-qwen-gguf-hf-weights.py'))
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)


def u32(v):return struct.pack('<I',v)
def u64(v):return struct.pack('<Q',v)
def s(v):
    b=v.encode();return u64(len(b))+b


def make_files(root):
    tensors=[]
    for layer in range(10):
        for g,h in [('attn_v.weight','self_attn.v_proj.weight'),
                    ('ffn_down.weight','mlp.down_proj.weight'),
                    ('attn_norm.weight','input_layernorm.weight')]:
            name=f'blk.{layer}.{g}'
            hf=f'model.language_model.layers.{layer}.{h}'
            # 2x4 BF16, each nonzero and specific per layer.
            vals=[0x3f00+layer*8+i for i in range(8)]
            data=struct.pack('<8H',*vals)
            tensors.append((name,hf,data))
    hf_data=bytearray();hf_header={}
    for _,hf,data in tensors:
        st=len(hf_data);hf_data.extend(data)
        hf_header[hf]={'dtype':'BF16','shape':[2,4],'data_offsets':[st,len(hf_data)]}
    header=json.dumps(hf_header).encode()
    hpath=root/'synthetic.safetensors'
    hpath.write_bytes(u64(len(header))+header+hf_data)
    metadata=s('general.architecture')+u32(8)+s('qwen3vl')
    # v3 GGUF: magic version tensor_count kv_count, followed by key-value table.
    raw=b'GGUF'+u32(3)+u64(len(tensors))+u64(1)+metadata
    packed=bytearray();
    for name,_,data in tensors:
        raw+=s(name)+u32(2)+u64(4)+u64(2)+u32(30)+u64(len(packed))
        packed.extend(data)
        packed.extend(b'\0'*((-len(packed))%32))
    raw+=b'\0'*((-len(raw))%32)
    gpath=root/'synthetic.gguf';gpath.write_bytes(raw+packed)
    return gpath,hpath


with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    gguf,hf=make_files(root)
    report=a.audit(gguf,hf,verify_hashes=False)
    assert report['status']=='AUDITED_TENSORS_BYTE_IDENTICAL',report
    assert report['compatible_audited_tensors']==30,report
    assert report['byte_identical_tensors']==30,report
    assert report['audited_layer_count']==10,report
    with gguf.open('r+b') as file:
        import mmap
        with mmap.mmap(file.fileno(),0,access=mmap.ACCESS_READ) as raw:
            last=a.parse_gguf(raw)[2]['blk.9.attn_norm.weight']['offset']
        file.seek(last+3);v=file.read(1);file.seek(last+3);file.write(bytes([v[0]^0x10]))
    report=a.audit(gguf,hf,verify_hashes=False)
    assert report['status']=='AUDITED_TENSORS_DIFFER',report
    assert report['different_tensors']==1,report
    assert len(report['different_examples'])==1,report
    try:a.audit(gguf,hf,verify_hashes=True)
    except ValueError as e:assert 'PINNED_MODEL_CHECKSUM_MISMATCH' in str(e)
    else:raise AssertionError('pinned checksums must fail closed')
    # A recognizable same-name tensor in F16 is incompatible, not a BF16 value mismatch.
    gguf2,hf2=make_files(root)
    with hf2.open('rb') as f:
        raw=f.read()
    hlen=struct.unpack_from('<Q',raw,0)[0]
    table=json.loads(raw[8:8+hlen])
    key='model.language_model.layers.0.self_attn.v_proj.weight'
    table[key]['dtype']='F16'
    updated=json.dumps(table).encode()
    hf2.write_bytes(u64(len(updated))+updated+raw[8+hlen:])
    report=a.audit(gguf2,hf2,verify_hashes=False)
    assert report['status']=='INCONCLUSIVE_INCOMPATIBLE_NAMED_TENSORS',report
    assert report['incompatible_named_tensors']==1,report
    with gguf.open('r+b') as f:f.seek(0);f.write(b'BAD!')
    try:a.audit(gguf,hf,verify_hashes=False)
    except ValueError as e:assert 'GGUF_MAGIC_MISMATCH' in str(e)
    else:raise AssertionError('bad GGUF magic accepted')
print('PASS 6.6.20 weight identity audit: synthetic exact, mismatched weight, hash contract, corrupt header')
