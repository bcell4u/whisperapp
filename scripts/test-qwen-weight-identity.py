#!/usr/bin/env python3
"""Self-contained positive/negative controls for 6.6.22 tensor identity audit.

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


def make_files(root, tokenizer_entries=0, numeric_entries=0, norms_f32=False):
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
            if norms_f32 and g=='attn_norm.weight':
                f32=struct.pack('<8I', *(v<<16 for v in vals))
                tensors.append((name,hf,f32,0,data))
            else:
                tensors.append((name,hf,data,30,data))
    hf_data=bytearray();hf_header={}
    for _,hf,_,_,data in tensors:
        st=len(hf_data);hf_data.extend(data)
        hf_header[hf]={'dtype':'BF16','shape':([8] if any(n==hf and typ==0 for _,n,_,typ,_ in tensors) else [2,4]),'data_offsets':[st,len(hf_data)]}
    header=json.dumps(hf_header).encode()
    hpath=root/'synthetic.safetensors'
    hpath.write_bytes(u64(len(header))+header+hf_data)
    metadata=s('general.architecture')+u32(8)+s('qwen3vl')
    metadata_count=1
    if tokenizer_entries:
        # LARGE_REALISTIC_TOKENIZER_ARRAY: real Qwen vocabulary arrays exceed
        # 100,000 strings. Verify successful skip AND subsequent KV alignment.
        metadata+=s('tokenizer.ggml.tokens')+u32(9)+u32(8)+u64(tokenizer_entries)
        metadata+=(u64(1)+b'a')*tokenizer_entries
        metadata_count+=1
    if numeric_entries:
        metadata+=s('tokenizer.ggml.token_type')+u32(9)+u32(4)+u64(numeric_entries)
        metadata+=u32(1)*numeric_entries
        metadata_count+=1
    if tokenizer_entries or numeric_entries:
        metadata+=s('general.alignment')+u32(4)+u32(32)
        metadata_count+=1
    # v3 GGUF: magic version tensor_count kv_count, followed by key-value table.
    raw=b'GGUF'+u32(3)+u64(len(tensors))+u64(metadata_count)+metadata
    packed=bytearray();
    for name,_,data,gg_type,_ in tensors:
        raw+=s(name)+(u32(1)+u64(8) if gg_type==0 else u32(2)+u64(4)+u64(2))+u32(gg_type)+u64(len(packed))
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
    # Actual 6.6.21 evidence: 57 GGUF FP32 normalization weights could not
    # be compared to corresponding HF BF16 tensors by raw byte identity.
    conv,ref=make_files(root,norms_f32=True)
    report=a.audit(conv,ref,verify_hashes=False)
    assert report['schema']==2,report
    assert report['status']=='AUDITED_TENSORS_EQUAL_WITH_STORAGE_CONVERSION',report
    assert report['direct_bf16_tensors']==20 and report['byte_identical_tensors']==20,report
    assert report['converted_f32_norm_tensors']==10,report
    assert report['storage_conversion_equivalent_tensors']==10,report
    assert report['equivalent_audited_tensors']==30 and report['different_tensors']==0,report
    # Exact BF16->F32 conversion includes all bits and must detect even a
    # one-unit F32 change that a fuzzy floating-point tolerance would ignore.
    with conv.open('r+b') as file:
        import mmap
        with mmap.mmap(file.fileno(),0,access=mmap.ACCESS_READ) as raw:
            pos=a.parse_gguf(raw)[2]['blk.9.attn_norm.weight']['offset']
        file.seek(pos+2);orig=file.read(1)
        file.seek(pos+2);file.write(bytes([orig[0]^1]))
    report=a.audit(conv,ref,verify_hashes=False)
    assert report['status']=='AUDITED_TENSORS_DIFFER',report
    assert report['different_tensors']==1,report
    assert report['different_examples'][0]['comparison']=='HF_BF16_CAST_TO_GGUF_F32',report
    assert report['different_examples'][0]['first_mismatched_element']==0,report
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
    # Regression for the real 6.6.20 CI failure. Tokenizer arrays can be
    # larger than 100k entries and must be streamed/skipped without allocation.
    large,hlarge=make_files(root,tokenizer_entries=151936,numeric_entries=151936)
    with large.open('rb') as file:
        import mmap
        with mmap.mmap(file.fileno(),0,access=mmap.ACCESS_READ) as binary:
            version,meta,tensors=a.parse_gguf(binary)
            assert version==3
            assert meta['tokenizer.ggml.tokens']==['ARRAY_COUNT',151936]
            assert meta['tokenizer.ggml.token_type']==['ARRAY_COUNT',151936]
            assert meta['general.alignment']==32
            assert len(tensors)==30
    report=a.audit(large,hlarge,verify_hashes=False)
    assert report['status']=='AUDITED_TENSORS_BYTE_IDENTICAL',report
    assert report['byte_identical_tensors']==30,report
    # Malformed claimed counts and truncated data must still fail closed.
    corrupted=bytearray(large.read_bytes())
    target=u32(9)+u32(8)+u64(151936)
    pos=corrupted.index(target)
    corrupted[pos+8:pos+16]=u64(a.LIMIT_ARRAY+1)
    try:a.parse_gguf(corrupted)
    except ValueError as e:assert 'GGUF_EXCESSIVE_ARRAY_LENGTH' in str(e),e
    else:raise AssertionError('oversized metadata count passed')
    # The skip logic must enforce file bounds, even for large scalar arrays.
    corrupt_scalar=b'GGUF'+u32(3)+u64(0)+u64(1)+s('numeric')+u32(9)+u32(4)+u64(1000)+u32(1)
    try:a.parse_gguf(corrupt_scalar)
    except ValueError as e:assert 'INVALID_OR_TRUNCATED_BINARY_HEADER' in str(e),e
    else:raise AssertionError('truncated scalar array passed')
    # Large string arrays must not use 1e5 temporary decoded strings.
    corrupt_string=b'GGUF'+u32(3)+u64(0)+u64(1)+s('tokenizer.ggml.tokens')+u32(9)+u32(8)+u64(17)+u64(1)+b'a'
    try:a.parse_gguf(corrupt_string)
    except ValueError as e:assert 'INVALID_OR_TRUNCATED_BINARY_HEADER' in str(e),e
    else:raise AssertionError('truncated string array passed')

    with gguf.open('r+b') as f:f.seek(0);f.write(b'BAD!')
    try:a.audit(gguf,hf,verify_hashes=False)
    except ValueError as e:assert 'GGUF_MAGIC_MISMATCH' in str(e)
    else:raise AssertionError('bad GGUF magic accepted')
print('PASS 6.6.22 weight audit: exact BF16/F32 norm conversion, real-sized tokenizer metadata, malformed-array guards, checksums')
