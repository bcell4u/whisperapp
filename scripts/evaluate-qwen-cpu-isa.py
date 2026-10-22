#!/usr/bin/env python3
"""6.6.16: synthetic-only same-GGUF CPU instruction-set build comparison.

Cross-ISA comparisons are diagnostic only: neither build is an official reference.
Changing numerical output does not establish a kernel bug or improved recognition.
"""
import argparse
import importlib.util
import json
import math
from pathlib import Path

spec = importlib.util.spec_from_file_location('qwen_prev_eval', Path(__file__).with_name('evaluate-qwen-precision-batch.py'))
previous = importlib.util.module_from_spec(spec)
spec.loader.exec_module(previous)
chunk = previous.chunk
PRECS = ('q8','bf16')
ISAS = ('simd','portable')
MODES = ('unified','three_stage')
UBATCHES = (128,256)
STEPS = 5
CASES = tuple(f'{p}_{isa}_{m}_u{u}' for p in PRECS for isa in ISAS for m in MODES for u in UBATCHES)
REPLAYS = tuple(f'{p}_{isa}_unified_u256_replay' for p in PRECS for isa in ISAS)


def mean(rows, metric):
    vals = [float(r[metric]['rmse']) for r in rows]
    if any(not math.isfinite(x) or x < 0 for x in vals):
        raise ValueError('INVALID_METRIC:'+metric)
    return sum(vals)/len(vals)


def classify(rows):
    if len(rows) != STEPS:
        return 'INCONCLUSIVE_INCOMPLETE', 'Five complete decoder checkpoints are required.'
    if any(not r[f'{p}_{isa}_replay']['byte_identical'] for r in rows for p in PRECS for isa in ISAS):
        return 'FAIL_REPLAY', 'An identical host configuration produced different logits on repeat.'
    baseline = sum(mean(rows, f'q8_simd_{mode}_microbatch') for mode in MODES)/len(MODES)
    portable = sum(mean(rows, f'q8_portable_{mode}_microbatch') for mode in MODES)/len(MODES)
    cross = sum(mean(rows, f'q8_{mode}_u{u}_isa') for mode in MODES for u in UBATCHES)/4
    if baseline < .01:
        return 'INCONCLUSIVE_Q8_CONTROL', 'The previously observed Q8 microbatch effect did not reproduce in the SIMD build.'
    if portable <= .7*baseline and baseline-portable >= .03:
        return 'Q8_BATCH_EFFECT_REDUCED_ON_PORTABLE', 'The portable CPU build reduces the Q8 batch variation; examine instruction-set-dependent computation, without treating either path as official truth.'
    if portable >= 1.5*baseline and portable-baseline >= .03:
        return 'Q8_BATCH_EFFECT_INCREASED_ON_PORTABLE', 'The portable CPU build increases Q8 batch variation; instruction-set effects exist but are not a correctness verdict.'
    if cross >= .01:
        return 'CPU_ISA_VARIANTS_DIFFER_BATCH_EFFECT_PERSISTS', 'CPU builds produce different logits but the Q8 batch sensitivity persists across them; isolate the affected operation next.'
    return 'CPU_ISA_VARIANTS_CLOSE_BATCH_EFFECT_PERSISTS', 'Switching CPU instruction sets did not materially change the logit outputs or resolve the batch-dependent variation.'


def evaluate(fixtures, paths, revision):
    hashes = chunk.legacy.verified_fixture(Path(fixtures))
    if set(paths) != set(CASES+REPLAYS):
        raise ValueError('MISSING_OR_EXTRA_ISA_CASES')
    paths={name:Path(p) for name,p in paths.items()}
    if any(not p.is_dir() for p in paths.values()):
        raise ValueError('MISSING_OUTPUT_DIRECTORY')
    phone=chunk.legacy.read(Path(fixtures)/'s26-prefill-logits.f32le')
    rows=[]
    for step in range(STEPS):
        name=f'logits-step-{step:02d}.f32le'
        vectors={case:chunk.legacy.read(folder/name) for case,folder in paths.items()}
        row={'step':step}
        for p in PRECS:
            for isa in ISAS:
                row[f'{p}_{isa}_replay']=chunk.comparison(vectors[f'{p}_{isa}_unified_u256'],vectors[f'{p}_{isa}_unified_u256_replay'])
                for mode in MODES:
                    row[f'{p}_{isa}_{mode}_microbatch']=chunk.comparison(vectors[f'{p}_{isa}_{mode}_u128'],vectors[f'{p}_{isa}_{mode}_u256'])
                for u in UBATCHES:
                    row[f'{p}_{isa}_u{u}_group']=chunk.comparison(vectors[f'{p}_{isa}_unified_u{u}'],vectors[f'{p}_{isa}_three_stage_u{u}'])
            for mode in MODES:
                for u in UBATCHES:
                    row[f'{p}_{mode}_u{u}_isa']=chunk.comparison(vectors[f'{p}_simd_{mode}_u{u}'],vectors[f'{p}_portable_{mode}_u{u}'])
        if step == 0:
            for isa in ISAS:
                for mode in MODES:
                    for u in UBATCHES:
                        row[f's26_q8_{isa}_{mode}_u{u}']=chunk.comparison(phone,vectors[f'q8_{isa}_{mode}_u{u}'])
        rows.append(row)
    status,conclusion=classify(rows)
    batch_mean={p:{isa:sum(mean(rows,f'{p}_{isa}_{mode}_microbatch') for mode in MODES)/len(MODES) for isa in ISAS} for p in PRECS}
    cross_mean={p:sum(mean(rows,f'{p}_{mode}_u{u}_isa') for mode in MODES for u in UBATCHES)/4 for p in PRECS}
    return {
        'schema':1,'release':'6.6.16','status':status,'conclusion':conclusion,
        'experiment':'Same Q8_0 and BF16 GGUFs; CPU optimized AVX2/FMA/F16C/BMI2/SSE4.2 build versus portable x86-64 baseline; same pinned llama.cpp, four CPU threads, original 156x1024 synthetic embeddings; unified and three-stage batches with microbatch 128/256; five forced-token checkpoints.',
        'mean_microbatch_rmse':batch_mean,'mean_cross_isa_rmse':cross_mean,
        'model_sha256':{'q8':previous.Q8_SHA256,'bf16':previous.BF16_SHA256},
        'upstream_revision':revision,'fixture_sha256':hashes,
        'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED',
        'limitations':'Different CPU instruction sets can yield different floating-point reductions without implying a defect. BF16/Q8 are different weight representations. CPU x86 checks cannot establish ARM64 dot-product correctness or clinical WER.',
        'steps':rows,
    }


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixtures',required=True)
    parser.add_argument('--llama-revision',required=True)
    parser.add_argument('--output',required=True)
    for case in CASES+REPLAYS:
        parser.add_argument('--'+case.replace('_','-'),dest=case,required=True)
    args=parser.parse_args()
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True)
    try:
        report=evaluate(args.fixtures,{name:getattr(args,name) for name in CASES+REPLAYS},args.llama_revision)
    except (OSError,ValueError,OverflowError,ZeroDivisionError) as error:
        report={'schema':1,'release':'6.6.16','status':'INCONCLUSIVE','reason':str(error),
                'model_math_independent':False,'official_qwen_parity':'NOT_VERIFIED','mrope_parity':'UNQUALIFIED'}
    out.write_text(json.dumps(report,indent=2)+'\n')
    print('isa_comparison_status='+report['status'])
    if 'mean_microbatch_rmse' in report: print('mean_microbatch_rmse='+str(report['mean_microbatch_rmse']))
    if report['status'].startswith(('INCONCLUSIVE','FAIL')): raise SystemExit(2)

if __name__=='__main__':main()
