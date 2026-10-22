#!/usr/bin/env python3
"""Compare synthetic-only prefill logits to independently generated logits.

Does NOT generate an independent reference or certify official Qwen parity.
No logits, transcript, model paths, or token IDs printed; only aggregate metrics.
"""
import argparse
import hashlib
import math
import struct
from pathlib import Path

VOCAB = 151936
BYTES = VOCAB * 4


def read_logits(path):
    data = Path(path).read_bytes()
    if len(data) != BYTES:
        raise ValueError(f'expected {BYTES} bytes, got {len(data)}')
    values = struct.unpack('<' + str(VOCAB) + 'f', data)
    if not all(map(math.isfinite, values)):
        raise ValueError('nonfinite logits')
    return values, hashlib.sha256(data).hexdigest()


def compare(a, b):
    assert len(a) == len(b) == VOCAB
    dot = sum(x*y for x,y in zip(a,b))
    aa = sum(x*x for x in a)
    bb = sum(y*y for y in b)
    sse = sum((x-y)**2 for x,y in zip(a,b))
    max_abs = max(abs(x-y) for x,y in zip(a,b))
    top_a = sorted(range(VOCAB), key=lambda i: a[i], reverse=True)[:5]
    top_b = sorted(range(VOCAB), key=lambda i: b[i], reverse=True)[:5]
    return dict(rmse=math.sqrt(sse/VOCAB), max_abs_delta=max_abs,
                cosine=dot/math.sqrt(aa*bb) if aa and bb else None,
                top1_equal=(top_a[0] == top_b[0]),
                top5_overlap=len(set(top_a)&set(top_b)))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('candidate', help='synthetic logits exported by S26')
    p.add_argument('independent_reference', help='from a DIFFERENT decoder, identical feature tensor and GGUF')
    p.add_argument('--candidate-sha256', help='expected S26 logit SHA-256')
    a = p.parse_args()
    left, left_sha = read_logits(a.candidate)
    right, right_sha = read_logits(a.independent_reference)
    if a.candidate_sha256 and left_sha != a.candidate_sha256.lower():
        p.error('candidate SHA-256 differs from S26 report')
    print('candidate_sha256=' + left_sha)
    print('reference_sha256=' + right_sha)
    for key, value in compare(left, right).items():
        print(f'{key}={value}')
    print('official_parity=UNQUALIFIED (model/input identity and independent origin must be validated separately)')


if __name__ == '__main__':
    main()
