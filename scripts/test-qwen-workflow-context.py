#!/usr/bin/env python3
"""Catch runner-only contexts where GitHub validates YAML before assigning a runner.

GitHub Actions allows `runner` in step env but not workflow-/job-level env.
We intentionally use RUNNER_TEMP at execution time, exporting HF_HOME via GITHUB_ENV.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOST = ROOT / '.github/workflows/qwen-host-reference-verification.yml'

def lint(source: str) -> list[str]:
    problems = []
    env_at = None
    for lineno, line in enumerate(source.splitlines(), 1):
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        indent = len(line) - len(line.lstrip(' '))
        if env_at is not None and indent <= env_at:
            env_at = None
        if line.strip() == 'env:' and indent in (0, 4):
            env_at = indent
        if env_at is not None and indent > env_at and '${{' in line and 'runner.' in line:
            problems.append(f'line {lineno}: runner context unavailable inside workflow/job env')
    if '      HF_HOME: ${{ runner.temp }}' in source:
        problems.append('original invalid HF_HOME job-level declaration present')
    if 'Initialize temporary directories on runner' not in source:
        problems.append('missing early runner cache initialization')
    if 'echo "HF_HOME=$RUNNER_TEMP/qwen-hf-cache" >> "$GITHUB_ENV"' not in source:
        problems.append('HF_HOME not exported through GITHUB_ENV')
    if '          python3 scripts/test-qwen-workflow-context.py' not in source:
        problems.append('CI has not enabled this guard')
    if "qwen-6.6.26-hf-execution-precision-" not in source:
        problems.append('incorrect host artifact version')
    return problems

def self_test():
    source=HOST.read_text()
    assert lint(source)==[], lint(source)
    invalid = source.replace('    env:\n',
        '    env:\n      HF_HOME: ${{ runner.temp }}/qwen-hf-cache\n', 1)
    assert len(lint(invalid))>=1, 'regression: invalid job-level context accepted'
    invalid_root = 'env:\n  SOME_PATH: ${{ runner.temp }}\n' + source
    assert len(lint(invalid_root))>=1, 'regression: invalid workflow-level context accepted'
    missing = source.replace(
        'echo "HF_HOME=$RUNNER_TEMP/qwen-hf-cache" >> "$GITHUB_ENV"',
        'echo "HF_HOME=/tmp/example" >> "$GITHUB_ENV"')
    assert len(lint(missing))>=1, 'regression: no safe runner env export accepted'
    # A runner expression in step-level env is allowed and must not cause a false alarm.
    legal = source.replace('      - name: Phase 1',
        '      - name: Step with legal runner context\n'
        '        env:\n          OK: ${{ runner.temp }}\n'
        '        run: echo test\n      - name: Phase 1')
    assert lint(legal)==[], lint(legal)
    print('PASS: workflow context scopes; rejects job/workflow runner.temp, allows step; runner-time HF_HOME export')

if __name__ == '__main__':
    self_test()
