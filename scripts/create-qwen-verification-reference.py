#!/usr/bin/env python3
"""Make a local comparison manifest from tokens produced by an INDEPENDENT decoder.

This does not execute or certify a reference decoder; do not feed it token IDs
from the Lab itself and then call the resulting comparison independent parity.
Only the reference JSON is imported into the Lab. Never upload private token IDs
or transcript material to a GitHub release or issue.
"""
import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--gguf-sha256', required=True)
parser.add_argument('--feature-sha256', required=True)
parser.add_argument('--token-ids-file', required=True, type=Path,
                    help='Private local JSON array of token IDs from independent decoder, including EOS')
parser.add_argument('--origin', required=True,
                    help='Local reference runner description; is NOT cryptographically authenticated')
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
for sha in (args.gguf_sha256, args.feature_sha256):
    if not re.fullmatch('[a-f0-9]{64}', sha): parser.error('hashes must be lowercase SHA-256')
if not 1 <= len(args.origin) <= 160: parser.error('invalid origin label')
tokens = json.loads(args.token_ids_file.read_text())
if not isinstance(tokens, list) or not 1 <= len(tokens) <= 128 or any(
        type(t) is not int or t < 0 or t >= 151936 for t in tokens):
    parser.error('expected a JSON list of 1..128 decoder token IDs')
if tokens[-1] not in (151643, 151645):
    parser.error('expected EOS at the final token; incomplete reference is ineligible')
if args.output.exists(): parser.error('refusing to overwrite existing reference')
manifest = dict(schema=1, kind='qwen3-asr-0.6b-fixture-tokens',
                gguf_sha256=args.gguf_sha256, feature_sha256=args.feature_sha256,
                origin=args.origin, token_ids=tokens)
args.output.write_text(json.dumps(manifest, separators=(',', ':')) + '\n')
print('Created local reference JSON (contains private token IDs). Do not share publicly.')
print('Agreement means token-level parity for identical model/features only; not M-RoPE/logits parity.')
