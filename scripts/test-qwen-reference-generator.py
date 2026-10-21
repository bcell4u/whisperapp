#!/usr/bin/env python3
"""CI contract regression cases: success, malformed reference, missing EOS, bad SHA."""
from pathlib import Path
from tempfile import TemporaryDirectory
import json
import subprocess
import sys

builder = Path(__file__).with_name('create-qwen-verification-reference.py')
with TemporaryDirectory() as temp:
    base = Path(temp)
    tokens = base / 'tokens.json'
    output = base / 'reference.json'
    args = [sys.executable, str(builder), '--gguf-sha256', 'a'*64,
            '--feature-sha256', 'b'*64, '--token-ids-file', str(tokens),
            '--origin', 'independent-test-runner', '--output', str(output)]
    tokens.write_text(json.dumps([99, 151645]))
    good = subprocess.run(args, capture_output=True, text=True)
    assert good.returncode == 0, good.stderr
    manifest = json.loads(output.read_text())
    assert manifest['kind'] == 'qwen3-asr-0.6b-fixture-tokens'
    assert manifest['token_ids'] == [99, 151645]
    assert manifest['gguf_sha256'] == 'a'*64
    # No generated reference overwrite; avoids confusing stale/replaced baselines.
    assert subprocess.run(args, capture_output=True).returncode != 0
    output.unlink()
    tokens.write_text(json.dumps([99, 151644]))
    assert subprocess.run(args, capture_output=True).returncode != 0
    assert not output.exists()
    tokens.write_text(json.dumps([99, 151936, 151645]))
    assert subprocess.run(args, capture_output=True).returncode != 0
    tokens.write_text(json.dumps([99, 151645]))
    badsha = args.copy()
    badsha[badsha.index('--gguf-sha256')+1] = 'x'*64
    assert subprocess.run(badsha, capture_output=True).returncode != 0
    assert not output.exists()
print('PASS: reference generator valid, duplicate, missing EOS, invalid token and bad-hash controls')
