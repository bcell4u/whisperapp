#!/usr/bin/env python3
"""6.6.15: same-host Q8_0 vs BF16 batch-sensitivity comparison.

Within-precision comparisons isolate prefill/microbatch behavior. Between-model
Q8/BF16 logits are *not* expected to match and are reported descriptively.
This is not official Qwen or independent M-RoPE parity.
"""
import argparse
import importlib.util
import json
from pathlib import Path

src = Path(__file__).with_name('evaluate-qwen-chunk-isolation.py')
spec = importlib.util.spec_from_file_location('chunk6615', src)
chunk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(chunk)

PRECISIONS = ('q8', 'bf16')
KINDS = ('orig', 'text')
MODES = ('unified', 'three_stage')
UBATCHES = (128, 256)
CASES = tuple(f'{precision}_{kind}_{mode}_u{u}' for precision in PRECISIONS for kind in KINDS for mode in MODES for u in UBATCHES)
REPLAYS = tuple(f'{precision}_orig_unified_u256_replay' for precision in PRECISIONS)
STEPS = 5
BF16_SHA256 = '12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50'
Q8_SHA256 = 'bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971'


def classify(rows):
    if len(rows) != STEPS:
        return 'INCONCLUSIVE_INCOMPLETE', 'Five complete numerical checkpoints are required.'
    if any(not r[f'replay_{p}']['byte_identical'] for r in rows for p in PRECISIONS):
        return 'FAIL_REPLAY', 'A same-model and same-configuration replay differed.'
    # The S26/Q8 numerical difference is established; require this real Q8 control.
    q8 = [r[f'q8_{kind}_{mode}_microbatch']['rmse'] for r in rows for kind in KINDS for mode in MODES]
    bf16 = [r[f'bf16_{kind}_{mode}_microbatch']['rmse'] for r in rows for kind in KINDS for mode in MODES]
    if any(not isinstance(v,(int,float)) or v < 0 or v != v for v in q8+bf16):
        return 'INCONCLUSIVE_METRICS', 'Invalid numerical comparison metrics.'
    if max(q8) < .01:
        return 'INCONCLUSIVE_Q8_CONTROL', 'Previously observed Q8 batch effect did not reproduce.'
    q8_avg = sum(q8)/len(q8)
    bf16_avg = sum(bf16)/len(bf16)
    if bf16_avg < .01:
        return 'BF16_ALMOST_INVARIANT', 'BF16 batch sensitivity is small relative to prior Q8 variation; quantization/kernel-specific effects warrant examination, but this does not prove causality.'
    if q8_avg >= 2*bf16_avg and q8_avg-bf16_avg >= .05:
        return 'Q8_MORE_BATCH_SENSITIVE', 'Q8 batch variation is at least twice BF16 in mean RMSE; inspect quantized compute kernels next.'
    if bf16_avg >= 2*q8_avg and bf16_avg-q8_avg >= .05:
        return 'BF16_MORE_BATCH_SENSITIVE', 'BF16 batch variation is at least twice Q8 in mean RMSE; do not attribute the discrepancy to Q8 quantization.'
    return 'BOTH_PRECISIONS_BATCH_SENSITIVE', 'Batch dependence persists with BF16 weights; investigate generic llama.cpp prefill/attention execution before changing Android.'


def evaluate(fixtures, paths, revision):
    hashes = chunk.legacy.verified_fixture(Path(fixtures))
    if set(paths) != set(CASES+REPLAYS):
        raise ValueError('MISSING_OR_EXTRA_PRECISION_CASES')
    paths = {key:Path(value) for key,value in paths.items()}
    for key,path in paths.items():
        if not path.is_dir():
            raise ValueError('MISSING_OUTPUT_DIRECTORY:'+key)
    phone = chunk.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows = []
    for step in range(STEPS):
        filename = f'logits-step-{step:02d}.f32le'
        vectors = {key:chunk.legacy.read(path/filename) for key,path in paths.items()}
        row = {'step':step}
        for precision in PRECISIONS:
            row[f'replay_{precision}'] = chunk.comparison(vectors[f'{precision}_orig_unified_u256'],vectors[f'{precision}_orig_unified_u256_replay'])
            for kind in KINDS:
                for mode in MODES:
                    row[f'{precision}_{kind}_{mode}_microbatch'] = chunk.comparison(vectors[f'{precision}_{kind}_{mode}_u128'],vectors[f'{precision}_{kind}_{mode}_u256'])
                for u in UBATCHES:
                    row[f'{precision}_{kind}_group_u{u}'] = chunk.comparison(vectors[f'{precision}_{kind}_unified_u{u}'],vectors[f'{precision}_{kind}_three_stage_u{u}'])
        for kind in KINDS:
            for mode in MODES:
                for u in UBATCHES:
                    row[f'quantization_{kind}_{mode}_u{u}'] = chunk.comparison(vectors[f'q8_{kind}_{mode}_u{u}'],vectors[f'bf16_{kind}_{mode}_u{u}'])
        if step == 0:
            for mode in MODES:
                for u in UBATCHES:
                    row[f's26_vs_q8_orig_{mode}_u{u}'] = chunk.comparison(phone,vectors[f'q8_orig_{mode}_u{u}'])
        rows.append(row)
    status,conclusion = classify(rows)
    mean = {p:sum(r[f'{p}_{kind}_{mode}_microbatch']['rmse'] for r in rows for kind in KINDS for mode in MODES)/(STEPS*len(KINDS)*len(MODES)) for p in PRECISIONS}
    return {
        'schema':1,'release':'6.6.15','status':status,'conclusion':conclusion,
        'model_sha256':{'q8':Q8_SHA256,'bf16':BF16_SHA256},
        'precision_kinds':{'q8':'public Qwen3-ASR 0.6B Q8_0 GGUF','bf16':'public Qwen3-ASR 0.6B BF16 GGUF'},
        'mean_microbatch_rmse_across_5_steps':mean,
        'experiment':'Same pinned llama.cpp and CPU for original embeddings and 156-token text control, unified and three-stage prompt, n_ubatch 128 vs 256, model GGUF Q8_0 vs BF16, four threads.',
        'limitations':'Q8 and BF16 logits are different models by numerical representation and are NOT expected to match. A precision sensitivity finding does not certify which implementation is correct or official Qwen M-RoPE equivalence.',
        'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED',
        'upstream_revision':revision,'fixture_sha256':hashes,'steps':rows,
    }


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--fixtures',required=True)
    p.add_argument('--llama-revision',required=True)
    p.add_argument('--output',required=True)
    for key in CASES+REPLAYS:
        p.add_argument('--'+key.replace('_','-'),dest=key,required=True)
    args=p.parse_args()
    outfile=Path(args.output);outfile.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,{key:getattr(args,key) for key in CASES+REPLAYS},args.llama_revision)
    except (OSError,ValueError,OverflowError,ZeroDivisionError) as err:
        report={'schema':1,'release':'6.6.15','status':'INCONCLUSIVE','reason':str(err),
                'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    outfile.write_text(json.dumps(report,indent=2)+'\n')
    print('precision_batch_status='+report['status'])
    if 'mean_microbatch_rmse_across_5_steps' in report:
        print('mean_microbatch_rmse='+repr(report['mean_microbatch_rmse_across_5_steps']))
    if report['status'].startswith(('INCONCLUSIVE','FAIL')):
        raise SystemExit(2)

if __name__=='__main__':
    main()
