#!/usr/bin/env python3
"""Summarize only the latest completed omi_npu session from a mixed app log.

Does not export original transcript or audio. Counts are runtime QA, NOT WER,
word timing ground truth, canonical model parity, or medical validation.
"""
import argparse
import json
import re
from pathlib import Path

START = re.compile(r'SESSION start .*engine=omi_npu(?:\s|$)')
COMPLETE = re.compile(r'SESSION complete .*?audioDurationMs=(\d+).*?wallTimeMs=(\d+).*?chunks=(\d+)')
CHUNK = re.compile(r'inference complete engine=omi_npu chunk=(\d+)/(\d+)\s')
SYNC = re.compile(r'npu word-sync .*?chunk=(\d+)/(\d+) .*?mode=([^\s]+)')


def metric(line, name):
    match = re.search(r'(?:^|\s)' + re.escape(name) + r'=([^\s]+)', line)
    return match.group(1) if match else None


def summarize(lines):
    sessions = []
    current = None
    for line in lines:
        if START.search(line):
            current = {'start': line.strip(), 'chunks': {}, 'sync': {}, 'complete': None}
            continue
        if current is None:
            continue
        m = CHUNK.search(line)
        if m:
            idx, total = int(m.group(1)), int(m.group(2))
            if idx in current['chunks']:
                raise ValueError(f'duplicate inference chunk={idx}')
            current['chunks'][idx] = {
                'total': total,
                'symbol_guards': int(metric(line, 'symbolGuards') or -1),
                'zero_durations': int(metric(line, 'zeroDurations') or -1),
                'stop': metric(line, 'stop'),
                'feature_nonfinite': int(metric(line, 'featureNonFinite') or -1),
                'padding_nonzero': int(metric(line, 'paddingNonzero') or -1),
            }
            continue
        s = SYNC.search(line)
        if s:
            idx = int(s.group(1))
            current['sync'][idx] = s.group(3)
            continue
        c = COMPLETE.search(line)
        if c:
            current['complete'] = {'audio_duration_ms': int(c.group(1)),
                                   'wall_time_ms': int(c.group(2)),
                                   'reported_chunks': int(c.group(3))}
            sessions.append(current)
            current = None
    if not sessions:
        raise ValueError('No completed omi_npu session found; do not analyze older partial records')
    last = sessions[-1]
    info = last['complete']
    expected = info['reported_chunks']
    completed = last['chunks']
    sync = last['sync']
    missing = sorted(set(range(1, expected+1)) - set(completed))
    wrong_totals = sorted(k for k, val in completed.items() if val['total'] != expected)
    stops = {}
    modes = {}
    for chunk in completed.values():
        stops[chunk['stop']] = stops.get(chunk['stop'], 0) + 1
    for mode in sync.values():
        modes[mode] = modes.get(mode, 0) + 1
    warnings = []
    if missing or wrong_totals: warnings.append('missing_or_inconsistent_inference_chunks')
    if len(sync) != expected: warnings.append('missing_word_sync_records')
    if any('fallback' in mode for mode in sync.values()): warnings.append('timing_fallback')
    if stops != {'frames-exhausted': expected}: warnings.append('abnormal_stop')
    if any(c['symbol_guards'] < 0 or c['feature_nonfinite'] < 0 or c['padding_nonzero'] < 0
           for c in completed.values()): warnings.append('missing_required_telemetry')
    if any(c['feature_nonfinite'] > 0 or c['padding_nonzero'] > 0 for c in completed.values()):
        warnings.append('nonfinite_or_nonzero_padding')
    return {
        'engine': 'omi_npu', 'status': 'complete',
        'runtime_only_no_accuracy_or_parity_claim': True,
        'audio_duration_ms': info['audio_duration_ms'],
        'wall_time_ms': info['wall_time_ms'],
        'chunks_expected': expected, 'chunks_inferred': len(completed),
        'missing_chunks': missing, 'wrong_total_chunks': wrong_totals,
        'symbol_limit_advances': sum(c['symbol_guards'] for c in completed.values() if c['symbol_guards'] >= 0),
        'zero_durations': sum(c['zero_durations'] for c in completed.values() if c['zero_durations'] >= 0),
        'stop_reasons': stops, 'word_sync_modes': modes,
        'warnings': warnings,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('log_file', type=Path)
    ap.add_argument('--output', type=Path)
    args = ap.parse_args()
    report = summarize(args.log_file.read_text(errors='replace').splitlines())
    rendered = json.dumps(report, indent=2, sort_keys=True) + '\n'
    if args.output:
        args.output.write_text(rendered)
    print(rendered, end='')


if __name__ == '__main__':
    main()
