#!/usr/bin/env python3
"""One bounded real-phone, persistent QNN -> llama.cpp multi-segment session.

A candidate benchmark, NOT a production qualification: authoritative four-axis
M-RoPE, full-recording WER, calibrated battery joules and controlled PSS remain
unmeasured. No long-lived phone storage or user app data is modified.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import math
import statistics
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import uuid
import zipfile

from qwen_bridge_version import builder_version
from qwen_bridge_quant import verify_quant_model, compare_quant_trials, QuantModelError
from qwen_native_bridge_phone import (BridgeError, MODEL_NAME, MODEL_SHA, PROBE_NAME,
    FEATURE_SHAPE, FEATURE_BYTES, digest, read_model, runtime_paths,
    validate_encoder_context, validate_features, extract_probe, scoped_cleanup_receipt,
    classify_native_failure)

MULTI_PROTOCOL = b'WR_BRIDGE_PROBE_PROTOCOL_0482_MULTI_V1'
CPU_TUNE_PROTOCOL = b'WR_BRIDGE_PROBE_PROTOCOL_0488_CPU_TUNE_V1'
DOTPROD_PROTOCOL = b'WR_BRIDGE_PROBE_PROTOCOL_0496_DOTPROD_V1'
DUAL_TUNE_PROTOCOL = b'WR_BRIDGE_PROBE_PROTOCOL_0492_DUAL_THREAD_V1'
# Baseline -> lighter CPU -> baseline control; single QNN encoding pass.
THREAD_SWEEP = (4, 3, 2, 4)
MATCHED_SWEEP = ((4,4),(3,3),(2,2),(4,4))  # decode, prefill; order remains a confound
# Host-only newline repair v0.4.85; earlier diagnostics v0.4.84: the existing 0.4.82 AArch64 ELF remains valid.
PREFIX = 'WhisperRecorder-Qwen-MultiSegment-Benchmark-'
MAX_SEGMENTS = 20
SAMPLES_PER_SEGMENT = 192000


def decode_bounded_pcm(audio: Path, ffmpeg: Path, seconds: int) -> bytes:
    """Decode once from t=0, trim by exact sample index; no silence removal."""
    if not audio.is_file() or audio.is_symlink() or not ffmpeg.is_file():
        raise BridgeError('MULTI_AUDIO_OR_FFMPEG_NOT_AVAILABLE')
    command = [str(ffmpeg), '-nostdin', '-hide_banner', '-loglevel', 'error',
               '-i', str(audio), '-map', '0:a:0', '-t', str(seconds),
               '-ac', '1', '-ar', '16000', '-f', 's16le', '-acodec', 'pcm_s16le', 'pipe:1']
    try:
        done = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              timeout=180, check=False)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise BridgeError('MULTI_AUDIO_DECODE_FAILED') from exc
    if done.returncode or len(done.stdout) % 2 or not (SAMPLES_PER_SEGMENT * 2 < len(done.stdout) <= 240 * 16000 * 2):
        raise BridgeError('MULTI_AUDIO_DECODE_INVALID_DURATION')
    return done.stdout


def segment_pcm(pcm: bytes, np):
    """Exact nonoverlapping windows; final partial segment pads for the fixed QNN graph."""
    from qwen_bridge_mel import compute_pcm
    assert len(pcm) % 2 == 0
    samples = np.frombuffer(pcm, dtype='<i2')
    if not SAMPLES_PER_SEGMENT < samples.size <= MAX_SEGMENTS * SAMPLES_PER_SEGMENT:
        raise BridgeError('MULTI_AUDIO_OUTSIDE_BOUNDED_WINDOW')
    segments = []
    for offset in range(0, samples.size, SAMPLES_PER_SEGMENT):
        chunk = samples[offset:offset+SAMPLES_PER_SEGMENT]
        audio_f32 = chunk.astype(np.float32) / np.float32(32768.0)
        mel = compute_pcm(audio_f32, np)
        rms = float(np.sqrt(np.mean(np.square(audio_f32.astype(np.float64)))))
        segments.append((mel, {'index':len(segments),
                     'start_s':round(offset / 16000, 3),
                     'duration_s':round(len(chunk) / 16000, 3),
                     'zero_padded_to_12s':bool(len(chunk) < SAMPLES_PER_SEGMENT),
                     'rms_dbfs':round(20*math.log10(max(rms, 1e-10)), 2)}))
    return segments


def parse_multi_metrics(output: str, segment_count: int, *, expected_threads: int | None = None, expected_batch_threads: int = 4) -> tuple[list[dict], dict]:
    """Reject missing, duplicate, contradicted or unbounded results; no text/token output."""
    prefix = 'WR_BRIDGE_MULTI_METRIC='
    summary_prefix = 'WR_BRIDGE_MULTI_SUMMARY='
    rows = [line[len(prefix):] for line in output.splitlines() if line.startswith(prefix)]
    summaries = [line[len(summary_prefix):] for line in output.splitlines() if line.startswith(summary_prefix)]
    if len(rows) != segment_count or len(summaries) != 1 or not 2 <= segment_count <= MAX_SEGMENTS:
        raise BridgeError('MULTI_INCOMPLETE_NATIVE_RECEIPT')
    keys = {'index','prefill_ms','generation_ms','generation_steps','eos_reached','rss_peak_kb','token_hash_fnv64'}
    parsed = []
    for i, line in enumerate(rows):
        if len(line) > 4096:
            raise BridgeError('MULTI_METRIC_OVERSIZE')
        try:
            r = json.loads(line)
        except ValueError as exc:
            raise BridgeError('MULTI_METRIC_INVALID_JSON') from exc
        if (type(r) is not dict or set(r) != keys or type(r['index']) is not int or r['index'] != i
                or type(r['eos_reached']) is not bool or
                not re.fullmatch('[0-9a-f]{16}', str(r['token_hash_fnv64'])) or
                any(type(r[k]) is not int or not 0 <= r[k] <= 600000
                    for k in ('prefill_ms','generation_ms')) or
                type(r['rss_peak_kb']) is not int or not 0 <= r['rss_peak_kb'] <= 32_000_000 or
                type(r['generation_steps']) is not int or not 1 <= r['generation_steps'] <= 96):
            raise BridgeError('MULTI_METRIC_CONTRACT_FAILURE')
        parsed.append(r)
    if len(summaries[0]) > 4096:
        raise BridgeError('MULTI_SUMMARY_OVERSIZE')
    try:
        summary = json.loads(summaries[0])
    except ValueError as exc:
        raise BridgeError('MULTI_SUMMARY_INVALID_JSON') from exc
    expected = {'segments','model_loads','model_load_ms','decoder_segments_wall_ms','rss_peak_kb','mrope_parity_verified'}
    if expected_threads is not None:
        expected |= {'decode_threads','batch_threads','process_cpu_ms'}
    if (type(summary) is not dict or set(summary) != expected or
            type(summary['segments']) is not int or summary['segments'] != segment_count or
            type(summary['model_loads']) is not int or summary['model_loads'] != 1 or
            summary['mrope_parity_verified'] is not False or
            any(type(summary[k]) is not int or not 0 <= summary[k] <= 10000000
                for k in ('model_load_ms','decoder_segments_wall_ms','rss_peak_kb')) or
            summary['rss_peak_kb'] < max(r['rss_peak_kb'] for r in parsed) or
            (expected_threads is not None and (
                type(summary['decode_threads']) is not int or summary['decode_threads'] != expected_threads or
                type(summary['batch_threads']) is not int or summary['batch_threads'] != expected_batch_threads or
                type(summary['process_cpu_ms']) is not int or not 0 <= summary['process_cpu_ms'] <= 40_000_000))):
        raise BridgeError('MULTI_SUMMARY_CONTRACT_FAILURE')
    return parsed, summary




def summarize_decoder_trials(trials: list[dict], *, required: int = 3) -> dict:
    """Validate multiple complete native replays of identical phone feature files.

    No raw tokens, transcripts, model bytes, path, or personal metadata leave
    the phone. The private native FNV64 hashes are combined into a bounded
    SHA256 digest solely to detect divergence between repetitions.
    """
    if not 1 <= required <= 4 or len(trials) != required:
        raise BridgeError('MULTI_REPEAT_TRIAL_COUNT_MISMATCH')
    durations=[]
    peak=[]
    fingerprints=[]
    expected_segments=None
    for i,t in enumerate(trials):
        if type(t) is not dict or t.get('trial_index') != i+1:
            raise BridgeError('MULTI_REPEAT_TRIAL_ORDER_INVALID')
        if type(t.get('segment_count')) is not int or not 2 <= t['segment_count'] <= MAX_SEGMENTS:
            raise BridgeError('MULTI_REPEAT_SEGMENT_COUNT_INVALID')
        if expected_segments is None:
            expected_segments = t['segment_count']
        if t['segment_count'] != expected_segments or t.get('eos_segments') != expected_segments:
            raise BridgeError('MULTI_REPEAT_SEGMENTS_OR_EOS_INCOMPLETE')
        ms=t.get('decoder_segments_wall_ms')
        rss=t.get('rss_peak_kb')
        digest_value=t.get('segment_token_hashes_sha256')
        if (type(ms) is not int or not 0 < ms <= 10000000 or
                type(rss) is not int or not 0 < rss <= 32000000 or
                not isinstance(digest_value,str) or not re.fullmatch(r'[0-9a-f]{64}',digest_value)):
            raise BridgeError('MULTI_REPEAT_TRIAL_METRICS_INVALID')
        durations.append(ms)
        peak.append(rss)
        fingerprints.append(digest_value)
    average=statistics.mean(durations)
    return {'trials_completed':len(trials), 'segments_per_trial':expected_segments,
            'decoder_segments_wall_ms_min':min(durations),
            'decoder_segments_wall_ms_median':round(statistics.median(durations),2),
            'decoder_segments_wall_ms_max':max(durations),
            'decoder_wall_cv_pct':round(100*statistics.pstdev(durations)/average,2),
            'native_rss_peak_kb_max':max(peak),
            'all_segment_token_hashes_consistent':len(set(fingerprints)) == 1,
            'status':('MEASURED_THREAD_SWEEP_NOT_SAME_CONFIGURATION_REPEATABILITY'
                      if len({t.get('decode_threads',4) for t in trials}) > 1 else
                      'MEASURED_REPEATABILITY' if required >= 3 else 'ONE_TRIAL_ONLY')}


def summarize_thread_sweep(trials: list[dict], *, matched: bool = False) -> dict:
    """Same QNN features, bounded 4/3/2/4 comparison; thermal order confounds power.

    Never infer watts or energy from process CPU time or battery temperature.
    """
    expected = MATCHED_SWEEP if matched else tuple((n,4) for n in THREAD_SWEEP)
    received = tuple((t.get('decode_threads'),t.get('batch_threads')) for t in trials)
    if len(trials) != 4 or received != expected:
        raise BridgeError('CPU_THREAD_SWEEP_ORDER_INVALID')
    if len({t['segment_token_hashes_sha256'] for t in trials}) != 1:
        raise BridgeError('CPU_THREAD_SWEEP_TOKEN_MISMATCH')
    profiles = []
    for t in trials:
        wall=t['decoder_segments_wall_ms']
        cpu=t['process_cpu_ms']
        if type(cpu) is not int or cpu <= 0 or type(wall) is not int or wall <= 0:
            raise BridgeError('CPU_THREAD_SWEEP_CPU_TIME_UNAVAILABLE')
        profiles.append({'decode_threads':t['decode_threads'],
                         'prefill_threads':t['batch_threads'],
                         'decoder_wall_s':round(wall/1000,3),
                         'process_cpu_s':round(cpu/1000,3),
                         'cpu_core_equivalents_average':round(cpu/wall,2),
                         'tokens':t['tokens_generated'],
                         'battery_before':t['battery_before'],
                         'battery_after':t['battery_after'],
                         'thermal_before':t['thermal_before'],
                         'thermal_after':t['thermal_after']})
    return {'status':'MEASURED_PROFILE_COMPARISON_NOT_ENERGY_QUALIFICATION',
            'run_order':('matched_4_4__3_3__2_2__4_4_control' if matched else '4_threads_3_threads_2_threads_4_threads_control'),
            'four_thread_control_drift_pct':round(100*(trials[3]['decoder_segments_wall_ms']/trials[0]['decoder_segments_wall_ms']-1),2),
            'all_token_hashes_identical':True,
            'profiles':profiles,
            'matched_prefill_and_generation_threads':matched,
            'interpretation':'Thread order, phone warming and scheduler effects confound profiles. CPU seconds and battery temperature are proxies, not joules. No optimal power profile is declared.'}


def trial_phase_breakdown(parsed: list[dict], total_wall_ms: int) -> dict:
    """Private native segment metrics -> bounded aggregates, not token traces.

    These are the exact SAME 12s segment positions for every replay, so their
    early/late and prefill/generation breakdowns are useful for diagnosing
    slowdowns without exporting transcripts or per-token information.
    """
    if not 2 <= len(parsed) <= MAX_SEGMENTS:
        raise BridgeError('MULTI_PHASE_SEGMENT_COUNT_INVALID')
    p = sum(int(r['prefill_ms']) for r in parsed)
    g = sum(int(r['generation_ms']) for r in parsed)
    steps = sum(int(r['generation_steps']) for r in parsed)
    if (type(total_wall_ms) is not int or not 0 < total_wall_ms <= 10_000_000
            or p < 0 or g < 0 or not 1 <= steps <= 1920
            or p+g > total_wall_ms+5000):
        raise BridgeError('MULTI_PHASE_AGGREGATE_INVALID')
    window=min(5,len(parsed))
    def window_ms(chunk):
        return sum(int(r['prefill_ms'])+int(r['generation_ms']) for r in chunk)
    return {'prefill_total_ms':p,'generation_total_ms':g,
            'first_segments_count':window,'first_segments_sum_ms':window_ms(parsed[:window]),
            'last_segments_count':window,'last_segments_sum_ms':window_ms(parsed[-window:]),
            'generation_tokens_per_s':round(1000.0*steps/g,2) if g else None,
            'native_phase_coverage_pct':round(100.0*(p+g)/total_wall_ms,2)}


def thermal_snapshot(engine) -> dict:
    """Optional Android platform thermal severity, never export raw dumpsys.

    Android labels: 0 NONE, 1 LIGHT, 2 MODERATE, 3 SEVERE, 4 CRITICAL,
    5 EMERGENCY, 6 SHUTDOWN. Some devices expose no parsable status.
    This is a sparse AFTER/BEFORE snapshot, not CPU frequency telemetry
    or proof that a given inference call was throttled.
    """
    labels=('NONE','LIGHT','MODERATE','SEVERE','CRITICAL','EMERGENCY','SHUTDOWN')
    try:
        raw = engine.adb_call(['shell','dumpsys','thermalservice'],timeout=20).stdout
        if not isinstance(raw,str):
            return {'status':None,'label':None,'method':'unavailable'}
        raw=raw[:50000]
        m=re.search(r'(?im)^\s*(?:thermal\s+status|mStatus)\s*[:=]\s*([0-6])\s*$',raw)
        if not m:
            return {'status':None,'label':None,'method':'android_thermalservice_status_unavailable'}
        n=int(m.group(1))
        return {'status':n,'label':labels[n],
                'method':'android_dumpsys_thermalservice_snapshot_not_cpu_throttle_trace'}
    except Exception:
        return {'status':None,'label':None,'method':'unavailable'}


def summarize_thermal_observations(trials: list[dict]) -> dict:
    """Correlation/triage only. No causation or calibrated energy claims."""
    if not trials:
        return {'status':'NOT_MEASURED','thermal_throttling_proven':False}
    first=trials[0]
    last=trials[-1]
    times=[t['decoder_segments_wall_ms'] for t in trials]
    def value(t,key,field):
        entry=t.get(key)
        v=entry.get(field) if isinstance(entry,dict) else None
        return v if isinstance(v,(float,int)) and not isinstance(v,bool) else None
    t0=value(first,'battery_before','battery_temp_c')
    t1=value(last,'battery_after','battery_temp_c')
    temp_rise=round(t1-t0,2) if t0 is not None and t1 is not None else None
    slowdown=round(100*(times[-1]/times[0]-1),2) if len(times)>=2 and times[0]>0 else None
    severity=[]
    for tr in trials:
        for fld in ('thermal_before','thermal_after'):
            st=value(tr,fld,'status')
            if st is not None:severity.append(st)
    return {'status':'THERMAL_SNAPSHOTS_RECORDED' if severity else 'BATTERY_PROXY_ONLY',
            'first_to_last_decoder_slowdown_pct':slowdown,
            'first_to_last_battery_temperature_change_c':temp_rise,
            'highest_reported_android_thermal_status':max(severity) if severity else None,
            'thermal_throttling_proven':False,
            'warming_and_slowdown_cooccurred':bool(
                slowdown is not None and temp_rise is not None and slowdown>15 and temp_rise>2),
            'interpretation':'Platform thermal snapshot is not a continuous CPU throttling trace; correlation is not causation.'}


def collect_trial_metadata(index: int, parsed: list[dict], summary: dict, wall: float,
                           battery: dict, *, battery_before=None, thermal_before=None,
                           thermal_after=None) -> dict:
    """Bounded, privacy-safe trial summary; no per-token arrays or words."""
    count=len(parsed)
    if not 2 <= count <= MAX_SEGMENTS or summary.get('segments') != count or summary.get('model_loads') != 1:
        raise BridgeError('MULTI_REPEAT_TRIAL_INCOMPLETE')
    concatenated = ''.join(r['token_hash_fnv64'] for r in parsed)
    if any(not r['eos_reached'] for r in parsed):
        raise BridgeError('MULTI_REPEAT_EOS_MISSING')
    return {'trial_index':index,'segment_count':count,'eos_segments':count,
            'tokens_generated':sum(r['generation_steps'] for r in parsed),
            'decoder_segments_wall_ms':summary['decoder_segments_wall_ms'],
            'model_load_ms':summary['model_load_ms'], 'model_loads':1,
            'rss_peak_kb':summary['rss_peak_kb'],
            'native_command_wall_s_including_adb':wall,
            'segment_token_hashes_sha256':hashlib.sha256(concatenated.encode('ascii')).hexdigest(),
            'battery_before':battery_before,
            'battery_after':battery,
            'thermal_before':thermal_before,
            'thermal_after':thermal_after,
            'phase_breakdown':trial_phase_breakdown(parsed,summary['decoder_segments_wall_ms']),
            'decode_threads':summary.get('decode_threads',4),
            'batch_threads':summary.get('batch_threads',4),
            'process_cpu_ms':summary.get('process_cpu_ms'),
            'process_cpu_ms_per_generated_token':(round(summary['process_cpu_ms']/max(1,sum(r['generation_steps'] for r in parsed)),2)
                if summary.get('process_cpu_ms') is not None else None)}


def safe_partial_metric_count(output: str, count: int) -> int:
    """Count only valid, ordered bounded metadata; never preserve raw native output."""
    complete = 0
    prefix = 'WR_BRIDGE_MULTI_METRIC='
    fields = {'index','prefill_ms','generation_ms','generation_steps',
              'eos_reached','rss_peak_kb','token_hash_fnv64'}
    for line in output.splitlines():
        if not line.startswith(prefix):
            continue
        payload = line[len(prefix):]
        if len(payload) > 4096 or complete >= count:
            break
        try:
            r = json.loads(payload)
            if (type(r) is not dict or set(r) != fields or
                type(r['index']) is not int or r['index'] != complete or
                type(r['eos_reached']) is not bool or
                type(r['generation_steps']) is not int or not 1 <= r['generation_steps'] <= 128 or
                not re.fullmatch('[0-9a-f]{16}',str(r['token_hash_fnv64'])) or
                any(type(r[k]) is not int or not 0 <= r[k] <= 600000
                    for k in ('prefill_ms','generation_ms')) or
                type(r['rss_peak_kb']) is not int or not 0 <= r['rss_peak_kb'] <= 32000000):
                break
        except (ValueError, TypeError, KeyError):
            break
        complete += 1
    return complete


def native_failure_category(stderr: str, returncode: int | None) -> str:
    """Convert PRIVATE native stderr into an allow-listed error code."""
    allowed = {
        'MULTI_MANIFEST_UNAVAILABLE':'MULTI_MANIFEST_UNAVAILABLE',
        'MULTI_MANIFEST_INVALID':'MULTI_MANIFEST_INVALID',
        'MULTI_SEGMENTS_MISSING':'MULTI_SEGMENTS_MISSING',
        'Expected exactly 156x1024 Float32 audio values':'MULTI_FEATURE_FILE_SIZE',
        'Audio feature read failed':'MULTI_FEATURE_READ_FAILED',
        'Nonfinite audio feature':'MULTI_NONFINITE_FEATURE',
        'Could not initialize decoder context':'DECODER_CONTEXT_INIT_FAILED',
        'Adding token failed':'NATIVE_PROMPT_TOKEN_REJECTED',
        'Token pos failed':'NATIVE_PROMPT_POSITION_REJECTED',
        'Cannot request last-prompt logits':'NATIVE_PREFILL_LOGITS_REJECTED',
        'Generation input rejected':'NATIVE_GENERATION_INPUT_REJECTED',
        'Nonfinite logits':'NATIVE_LOGITS_NONFINITE',
    }
    for phrase, code in allowed.items():
        if phrase in stderr:
            return code
    recognized = classify_native_failure(RuntimeError(stderr[:6000]))
    if recognized != 'NATIVE_FAILURE_UNCLASSIFIED':
        return recognized
    if returncode in (137, -9):
        return 'NATIVE_EXIT_KILLED_POSSIBLE_MEMORY'
    if returncode in (139, -11):
        return 'NATIVE_EXIT_SEGFAULT'
    if returncode in (134, -6):
        return 'NATIVE_EXIT_ABORT'
    if returncode == 126:
        return 'NATIVE_NOT_EXECUTABLE'
    if returncode == 127:
        return 'NATIVE_EXECUTABLE_OR_LINKER_NOT_FOUND'
    return 'NATIVE_EXIT_NONZERO_UNCLASSIFIED'


def run_native_once(engine, command: list[str], expected: int, phase: str, row: dict,
                    *, timeout: int = 1800, expected_threads: int | None = None,
                    expected_batch_threads: int = 4) -> tuple[list[dict], dict]:
    """Never replay native inference after ADB errors. Scrub private stdout/stderr."""
    started = time.monotonic()
    stdout = ''
    stderr = ''
    exit_code = None
    try:
        if hasattr(engine, '_run_adb') and hasattr(engine, 'serial'):
            # Avoid adb_call's generic auto-retry: a lost ADB response must NOT
            # launch a second decoder while the first may still be running.
            result = engine._run_adb(['-s',engine.serial,*command],timeout=timeout)
        else:
            # Offline simulated engine only, no user phone in this path.
            result = engine.adb_call(command,timeout=timeout)
        stdout = str(getattr(result,'stdout',''))
        stderr = str(getattr(result,'stderr',''))
        exit_code = int(getattr(result,'returncode',0))
        if exit_code:
            raise RuntimeError('NATIVE_NONZERO_EXIT')
        parsed, summary = parse_multi_metrics(stdout,expected, expected_threads=expected_threads,
            expected_batch_threads=expected_batch_threads)
        row[phase+'_native_status'] = 'pass'
        row[phase+'_command_wall_s_including_adb'] = round(time.monotonic()-started,3)
        return parsed,summary
    except Exception as exc:
        # Fallback fake engines may expose only an exception containing the
        # last portion of ADB stderr. Never include it in the shareable ZIP.
        if not (stdout or stderr):
            stderr = str(exc)[-6000:]
        partial = safe_partial_metric_count(stdout,expected)
        row[phase+'_completed_native_segments'] = partial
        row[phase+'_command_wall_s_including_adb'] = round(time.monotonic()-started,3)
        if isinstance(exc, BridgeError):
            category = str(exc)
        elif isinstance(exc, subprocess.TimeoutExpired) or 'timed out' in str(exc).lower():
            category = 'NATIVE_EXECUTION_TIMEOUT'
        else:
            category = native_failure_category(stderr,exit_code)
        row[phase+'_native_failure_category'] = category
        row[phase+'_native_status'] = 'failed'
        raise BridgeError('MULTI_'+phase.upper()+'_'+category) from exc


def write_feature_list(scratch: Path, filename: str, names: list[str]) -> Path:
    """Write only Builder-generated feature filenames, no user paths or text."""
    if not 2 <= len(names) <= MAX_SEGMENTS or any(not re.fullmatch(r'feature-[0-9]{2}\.f32le',n) for n in names):
        raise BridgeError('MULTI_FEATURE_MANIFEST_INVALID')
    dest = scratch / filename
    # Native Android std::getline strips LF but NOT a Windows CR. Python text
    # mode expands LF to CRLF on Windows, causing MULTI_MANIFEST_INVALID before
    # the first GGUF model load. Write fixed ASCII bytes instead, regardless
    # of host platform. This also avoids a new ARM64 probe/GitHub build.
    payload = ("\n".join(names) + "\n").encode("ascii")
    if b"\r" in payload or payload.count(b"\n") != len(names):
        raise BridgeError('MULTI_MANIFEST_NEWLINE_CONTRACT_FAILED')
    dest.write_bytes(payload)
    if dest.read_bytes() != payload:
        raise BridgeError('MULTI_MANIFEST_LOCAL_VERIFY_FAILED')
    return dest

def battery_snapshot(engine) -> dict:
    """Coarse battery temperature/charge proxies; never report them as energy."""
    try:
        raw = engine.adb_call(['shell','dumpsys','battery'], timeout=20).stdout[:30000]
        def integer(key):
            found = re.search(r'(?m)^\s*'+key+r':\s*(\d+)\s*$', raw)
            return int(found.group(1)) if found else None
        temp = integer('temperature')
        level = integer('level')
        return {'battery_level_pct':level if level is not None and 0<=level<=100 else None,
                'battery_temp_c':round(temp / 10, 1) if temp is not None and 0<=temp<=1000 else None,
                'method':'android_dumpsys_battery_proxy_not_calibrated_energy'}
    except Exception:
        return {'battery_level_pct':None,'battery_temp_c':None,
                'method':'unavailable'}


def assert_phone_cool_enough(battery: dict, thermal: dict) -> None:
    """Fail safely between trials; not an in-process thermal governor.

    Android temperatures from dumpsys battery are rough thermistor proxies.
    """
    temperature = battery.get('battery_temp_c') if isinstance(battery,dict) else None
    status = thermal.get('status') if isinstance(thermal,dict) else None
    if isinstance(temperature,(float,int)) and not isinstance(temperature,bool) and temperature >= 42.0:
        raise BridgeError('MULTI_BATTERY_TEMPERATURE_SAFETY_STOP')
    if type(status) is int and status >= 4:
        raise BridgeError('MULTI_THERMAL_SEVERITY_SAFETY_STOP')


def cpu_frequency_snapshot(engine) -> dict:
    """Optional sysfs snapshot. Frequencies are point observations, NOT energy.

    Read only aggregate numeric policy frequencies. Paths and raw phone output
    never enter shareable diagnostics. Unavailable on some Android builds.
    """
    shell = ('for d in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do '
             'n=${d##*policy}; f=$(cat "$d/scaling_cur_freq" 2>/dev/null) || continue; '
             'echo "$n:$f"; done')
    try:
        out = str(engine.adb_call(['shell','sh','-c',shell],timeout=12).stdout)[:4096]
        seen = {}
        for line in out.splitlines():
            match = re.fullmatch(r'([0-9]{1,2}):([0-9]{3,8})',line.strip())
            if match:
                policy,khz = map(int, match.groups())
                if policy <= 15 and 100000 <= khz <= 10000000:
                    seen[policy]=khz
        if not seen:
            return {'status':'unavailable','method':'read_only_android_sysfs_snapshot'}
        return {'status':'observed','method':'read_only_android_sysfs_snapshot',
                'policy_freq_khz':[{'policy':k,'khz':seen[k]} for k in sorted(seen)]}
    except Exception:
        return {'status':'unavailable','method':'read_only_android_sysfs_snapshot'}


def wait_for_temperature_match(engine, baseline: dict, *, max_wait_s: int = 240,
                               interval_s: int = 15, clock=time.monotonic,
                               sleep=time.sleep, battery_reader=None,
                               thermal_reader=None) -> dict:
    """Bounded opt-in cooling between experiments, never an energy claim.

    Battery temperature is not SoC temperature. Do not claim truly equivalent
    thermal states based on one thermistor. Always enforce independent safety.
    """
    if not isinstance(baseline,dict) or type(baseline.get('battery_temp_c')) not in (float,int):
        raise BridgeError('MULTI_THERMAL_MATCH_BASELINE_UNAVAILABLE')
    if not (1 <= max_wait_s <= 900 and 1 <= interval_s <= 60):
        raise BridgeError('MULTI_THERMAL_MATCH_CONFIGURATION_INVALID')
    baseline_temp = float(baseline['battery_temp_c'])
    read_battery = battery_reader or battery_snapshot
    read_thermal = thermal_reader or thermal_snapshot
    deadline=clock()+max_wait_s
    start=clock()
    checks=0
    while True:
        battery=read_battery(engine)
        thermal=read_thermal(engine)
        assert_phone_cool_enough(battery,thermal)
        checks+=1
        temp=battery.get('battery_temp_c') if isinstance(battery,dict) else None
        if type(temp) not in (float,int):
            raise BridgeError('MULTI_THERMAL_MATCH_TEMPERATURE_UNAVAILABLE')
        # Missing Android thermal severity is recorded elsewhere and does not
        # invalidate a battery-temperature-only proxy, which remains qualified.
        if abs(float(temp)-baseline_temp) <= 0.5:
            return {'status':'battery_temperature_proxy_matched',
                    'wait_s':round(clock()-start,2),'checks':checks,
                    'baseline_battery_temp_c':round(baseline_temp,1),
                    'actual_battery_temp_c':round(float(temp),1),
                    'threshold_delta_c':0.5,
                    'not_soc_temperature_equivalence':True}
        remaining=deadline-clock()
        if remaining <= 0:
            raise BridgeError('MULTI_THERMAL_MATCH_COOLDOWN_TIMEOUT')
        sleep(min(float(interval_s),remaining))


def verify_existing_accuracy(results: Path) -> dict:
    """Evidence gate: already scored clips, no duplicate first-12s rerun."""
    from qwen_bridge_batch_qualification import collect_ground_truth_history
    rows = collect_ground_truth_history(results)
    return {'scored_audio_variants':len(rows),
            'adequate_to_run_experimental_benchmark':len(rows) >= 2,
            'accuracy_not_production_verified':True}


def report_zip(results: Path, row: dict) -> Path:
    dest = results / (PREFIX + datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S-') + uuid.uuid4().hex[:6]+'.zip')
    data = json.dumps(row, indent=2, sort_keys=True).encode()
    if len(data) > 180000:
        raise BridgeError('MULTI_REPORT_TOO_LARGE')
    with zipfile.ZipFile(dest, 'x', zipfile.ZIP_DEFLATED) as z:
        z.writestr('manifest.json',data)
        z.writestr('privacy.txt','No audio, transcript, paths, file names, raw token arrays, embeddings, models or phone identifiers.\n')
    return dest


def run(args, *, hooks=None) -> tuple[int, Path]:
    from omi_tdt_s26_transcribe import AdbQnn, INPUTS, OUTPUTS
    import numpy as np
    results = Path(args.results).resolve()
    results.mkdir(parents=True,exist_ok=True)
    if results.is_symlink():
        raise BridgeError('RESULTS_DIRECTORY_UNSAFE')
    recovery = results / 'qwen-native-bridge-recovery.json'
    row = {'version':builder_version(),'kind':'experimental_persistent_qnn_gguf_full_recording',
           'status':'BLOCKED_OR_FAILED','accuracy_provisionally_accepted':bool(args.ack_provisional_accuracy),
           'qualifies_android_integration':False, 'mrope_parity_verified':False,
           'full_recording_wer_measured':False, 'pss_measured':False,
           'calibrated_energy_measured':False,'phone_all_files_cleanup':'not_staged',
           'privacy_safe_report':True}
    engine = None
    stage = 'local_preflight'
    t0 = time.monotonic()
    try:
        if recovery.exists():
            raise BridgeError('PREVIOUS_UNVERIFIED_PHONE_STAGE_REQUIRES_RECOVERY')
        if not args.ack_provisional_accuracy:
            raise BridgeError('EXPLICIT_PROVISIONAL_BENCHMARK_CONSENT_REQUIRED')
        # Experimental efficiency work is explicitly opted in above. The
        # availability of local accuracy receipts is diagnostic only: users
        # may have validated their reference clips in an earlier Builder
        # workspace, or shareable history may intentionally be incomplete.
        # Keep production qualification independently BLOCKED, rather than
        # converting absent local receipts into a false accuracy PASS.
        row['prior_accuracy_evidence'] = verify_existing_accuracy(results)
        historical_evidence = bool(row['prior_accuracy_evidence'].get(
            'adequate_to_run_experimental_benchmark', False))
        row['experimental_performance_consent_basis'] = 'explicit_user_ack_provisional_accuracy'
        row['prior_accuracy_history_sufficient'] = historical_evidence
        row['prior_accuracy_history_missing_or_incomplete'] = not historical_evidence
        row['accuracy_production_qualified'] = False
        row['accuracy_history_not_a_phone_preflight_requirement'] = True
        model = Path(args.cache).resolve() / 'models' / MODEL_NAME
        row['model'] = read_model(model)
        quant_compare = bool(getattr(args,'quant_compare',False))
        q8_fast = bool(getattr(args,'q8_fast_baseline',False))
        if q8_fast and (quant_compare or getattr(args,'cpu_thread_sweep',False)
                        or getattr(args,'dual_thread_sweep',False)
                        or getattr(args,'temperature_match',False)
                        or getattr(args,'full_trials',1) != 1):
            raise BridgeError('Q8_FAST_BASELINE_MUST_BE_SINGLE_FOUR_THREAD_TRIAL')
        row['q8_fast_baseline_requested'] = q8_fast
        row['q8_fast_baseline_scope'] = ('single_full_decoder_4_4_with_cpu_time' if q8_fast
                                         else 'not_requested')
        quant_sources = []
        if quant_compare:
            if getattr(args,'cpu_thread_sweep',False) or getattr(args,'dual_thread_sweep',False) or getattr(args,'temperature_match',False):
                raise BridgeError('QUANT_COMPARE_CANNOT_COMBINE_WITH_THREAD_SWEEP')
            if not getattr(args,'quant_q4_model',None):
                raise BridgeError('QUANT_Q4_MODEL_REQUIRED_BEFORE_PHONE_STAGE')
            for name, attr, remote_name in (
                ('Q4_K_M','quant_q4_model','qwen-quant-q4.gguf'),
                ('Q5_K_M','quant_q5_model','qwen-quant-q5.gguf')):
                candidate=getattr(args,attr,None)
                if candidate:
                    try:
                        manifest=verify_quant_model(Path(candidate).absolute(),name,MODEL_SHA)
                    except (QuantModelError, OSError, ValueError) as exc:
                        code = str(exc) if isinstance(exc,QuantModelError) else 'QUANT_MODEL_PREFLIGHT_IO_FAILED'
                        raise BridgeError(code) from exc
                    quant_sources.append((Path(candidate).absolute(),remote_name,manifest))
            if len({m['sha256'] for _,_,m in quant_sources}) != len(quant_sources):
                raise BridgeError('QUANT_DUPLICATED_MODEL')
            row['quant_models']=[{'quant':'Q8_0','sha256':MODEL_SHA,'bytes':model.stat().st_size},
                                 *[m for _,_,m in quant_sources]]
            row['quant_comparison_requested']=True
            row['quantization_does_not_prove_accuracy']=True
        context = Path(args.qnn_context).resolve()
        row['encoder_context'] = validate_encoder_context(context)
        runtime = runtime_paths(Path(args.runtime_manifest).resolve())
        if (args.threads != 4 or args.steps != 96 or args.max_seconds not in (36,120,240)
                or getattr(args,'full_trials',1) not in (1,3)
                or type(getattr(args,'cpu_thread_sweep',False)) is not bool
                or type(getattr(args,'dual_thread_sweep',False)) is not bool
                or (getattr(args,'cpu_thread_sweep',False) and getattr(args,'dual_thread_sweep',False))
                or type(getattr(args,'temperature_match',False)) is not bool
                or type(getattr(args,'quant_compare',False)) is not bool
                or type(getattr(args,'q8_fast_baseline',False)) is not bool
                or (getattr(args,'temperature_match',False) and not (getattr(args,'cpu_thread_sweep',False) or getattr(args,'dual_thread_sweep',False)))):
            raise BridgeError('MULTI_UNSUPPORTED_PARAMETER')
        role = 'qwen_native_bridge_encoder'
        if role in INPUTS or role in OUTPUTS:
            raise BridgeError('QNN_ROLE_COLLISION')
        with tempfile.TemporaryDirectory(prefix='wr-qwen-full-', dir=results) as dirname:
            scratch = Path(dirname)
            probe = extract_probe(Path(args.probe_archive).resolve(), scratch)
            with probe.open('rb') as f:
                # Marker lookup is a capability check, not proof of upstream compilation.
                markers = f.read()
            # Safe on failed preflight too: only protocol booleans and SHA-256,
            # never the local artifact path or raw executable bytes.
            row['probe_has_multi_protocol'] = MULTI_PROTOCOL in markers
            row['probe_has_cpu_tune_protocol'] = CPU_TUNE_PROTOCOL in markers
            row['probe_has_dual_tune_protocol'] = DUAL_TUNE_PROTOCOL in markers
            row['native_probe_sha256'] = digest(probe)
            row['probe_has_arm_dotprod_protocol'] = DOTPROD_PROTOCOL in markers
            row['native_cpu_target'] = ('arm64_dotprod_candidate' if row['probe_has_arm_dotprod_protocol']
                                        else 'baseline_arm64')
            row['native_cpu_isa_optimization_phone_verified'] = False
            if not row['probe_has_multi_protocol']:
                raise BridgeError('NEW_MULTI_SEGMENT_ARM64_PROBE_REQUIRED_BEFORE_PHONE_STAGE')
            if (getattr(args,'cpu_thread_sweep',False) or quant_compare or q8_fast) and CPU_TUNE_PROTOCOL not in markers:
                raise BridgeError('NEW_CPU_TUNE_ARM64_PROBE_REQUIRED_BEFORE_PHONE_STAGE')
            if getattr(args,'dual_thread_sweep',False) and DUAL_TUNE_PROTOCOL not in markers:
                raise BridgeError('NEW_DUAL_THREAD_ARM64_PROBE_REQUIRED_BEFORE_PHONE_STAGE')
            row['native_probe_protocol'] = (DUAL_TUNE_PROTOCOL if getattr(args,'dual_thread_sweep',False) else CPU_TUNE_PROTOCOL if (getattr(args,'cpu_thread_sweep',False) or quant_compare or q8_fast) else MULTI_PROTOCOL).decode()
            stage = 'decode_and_segment'
            raw = decode_bounded_pcm(Path(args.audio), Path(args.ffmpeg), args.max_seconds)
            parts = segment_pcm(raw,np)
            row['audio_pcm_sha256'] = hashlib.sha256(raw).hexdigest()
            row['audio_segments_planned'] = len(parts)
            row['audio_duration_s'] = round(len(raw)/32000,3)
            row['segmentation'] = '0s_start_12s_fixed_nonoverlap_last_segment_padded'
            row['segments'] = [meta for _,meta in parts]
            if len(parts) < 2 or len(parts) > MAX_SEGMENTS:
                raise BridgeError('INVALID_SEGMENT_COUNT')
            INPUTS[role] = ['mel']
            OUTPUTS[role] = ['audio_features']
            try:
                engine = (hooks['engine'](context,runtime,scratch) if hooks else
                     AdbQnn(Path(args.adb),args.serial,{k:str(v) for k,v in runtime.items()},
                            {role:context},scratch,auto_clean=True))
                if not re.fullmatch(r'/data/local/tmp/whisperrecorder-omi-v2-[a-f0-9]{12}',engine.remote):
                    raise BridgeError('MULTI_REMOTE_STAGE_OUTSIDE_OWNED_SCOPE')
                # Path stays LOCAL in recovery file and never enters diagnostics.
                recovery.write_text(json.dumps({'remote':engine.remote,
                    'serial_sha256':hashlib.sha256(args.serial.encode()).hexdigest(),
                    'generated_by':'qwen-native-bridge-0.4.82'}),encoding='utf-8')
                stage='qnn_stage'
                device_info = engine.stage()
                row['device_model'] = 'SM-S942U' if 'SM-S942U' in str(device_info.get('model','')).upper() else 'UNEXPECTED_DEVICE'
                if row['device_model'] != 'SM-S942U':
                    raise BridgeError('MULTI_UNEXPECTED_DEVICE_MODEL')
                row['battery_before'] = battery_snapshot(engine)
                # Stage the exact native probe and cached model ONCE; then
                # challenge the persistent decoder with two REAL NPU segments
                # before spending time on the rest of the lecture.
                stage='native_preflight_transfer'
                for local in (probe,model):
                    engine.adb_call(['push',str(local),engine.remote+'/'+local.name],timeout=1200)
                # Each optional GGUF is transferred only after ALL local checks pass.
                # Fixed test-owned basenames prevent arbitrary on-device paths.
                if quant_compare: row['quant_model_transfer'] = []
                for local,remote_name,model_meta in quant_sources:
                    transfer_begin=time.monotonic()
                    engine.adb_call(['push',str(local),engine.remote+'/'+remote_name],timeout=1200)
                    row['quant_model_transfer'].append({'quant':model_meta['quant'],
                        'bytes':model_meta['bytes'],
                        'adb_push_wall_s':round(time.monotonic()-transfer_begin,3)})
                engine.adb_call(['shell','chmod','700',engine.remote+'/'+PROBE_NAME],timeout=20)
                stage='native_protocol_probe'
                probe_info=engine.adb_call(['shell','env','LD_LIBRARY_PATH='+engine.remote,
                            engine.remote+'/'+PROBE_NAME,'--probe-info'],timeout=35)
                if (probe_info.stdout.count('WR_BRIDGE_PROBE_INFO=') != 1 or
                        MULTI_PROTOCOL.decode() not in probe_info.stdout or
                        ((getattr(args,'cpu_thread_sweep',False) or quant_compare or q8_fast) and CPU_TUNE_PROTOCOL.decode() not in probe_info.stdout) or
                        (getattr(args,'dual_thread_sweep',False) and DUAL_TUNE_PROTOCOL.decode() not in probe_info.stdout)):
                    raise BridgeError('MULTI_NATIVE_PROBE_INFO_UNVERIFIED')
                # Do not infer runtime capability from the static ELF marker alone.
                # The dotprod native executable checks Android AT_HWCAP at startup.
                if row['probe_has_arm_dotprod_protocol']:
                    if (not getattr(probe_info, 'returncode', 0) == 0 or
                            DOTPROD_PROTOCOL.decode() not in probe_info.stdout or
                            '\"cpu_target\":\"arm64_dotprod\"' not in probe_info.stdout):
                        raise BridgeError('ARM_DOTPROD_NATIVE_PROBE_INFO_OR_DEVICE_UNSUPPORTED')
                    row['native_cpu_isa_optimization_phone_verified'] = True
                row['native_preflight_probe_info'] = 'verified_on_phone' 
                feature_names=[]
                row['qnn_encoder_graph_calls'] = 0
                row['native_canary'] = 'not_started'
                for i,(mel,metadata) in enumerate(parts):
                    stage='qnn_segment_'+str(i)
                    features = engine.run(role, {'mel':mel},{'audio_features':FEATURE_SHAPE},np,
                                          reference={'audio_features':np.zeros(FEATURE_SHAPE,dtype=np.float32)})[0]
                    validate_features(features,np)
                    blob = features.astype('<f4',copy=False).tobytes(order='C')
                    if len(blob) != FEATURE_BYTES:
                        raise BridgeError('MULTI_FEATURE_SIZE_MISMATCH')
                    name = 'feature-%02d.f32le' % i
                    local = scratch / name
                    local.write_bytes(blob)
                    engine.adb_call(['push',str(local),engine.remote+'/'+name],timeout=120)
                    feature_names.append(name)
                    metadata['feature_sha256'] = hashlib.sha256(blob).hexdigest()
                    metadata['qnn_cumulative_wall_s'] = round(float(engine.total_seconds[role]),3)
                    row['qnn_encoder_graph_calls'] = int(engine.calls[role])
                    if i == 1:
                        # In-session canary: one separate short model/context
                        # load, two segment decodes, strict receipts and cleanup
                        # shared with the later full pass. NOT a benchmark speed.
                        stage='native_persistent_canary'
                        canary_manifest = write_feature_list(scratch,'private-canary-list.txt',feature_names)
                        engine.adb_call(['push',str(canary_manifest),engine.remote+'/'+canary_manifest.name],timeout=30)
                        command=['shell','env','LD_LIBRARY_PATH='+engine.remote,
                            engine.remote+'/'+PROBE_NAME,engine.remote+'/'+MODEL_NAME,
                            '--multi',engine.remote+'/'+canary_manifest.name,str(args.steps)]
                        canary_metrics,canary_summary=run_native_once(engine,command,2,'canary',row,timeout=300)
                        row['native_canary'] = 'two_segments_passed'
                        row['native_canary_model_loads'] = canary_summary['model_loads']
                        row['native_canary_eos_segments'] = sum(int(r['eos_reached']) for r in canary_metrics)
                if row['qnn_encoder_graph_calls'] != len(parts) or int(engine.qnn_command_completed[role]) != len(parts):
                    raise BridgeError('MULTI_QNN_GRAPH_COUNT_MISMATCH')
                row['qnn_encoder_total_wall_s_including_io'] = round(float(engine.total_seconds[role]),3)
                stage='native_transfer'
                manifest = write_feature_list(scratch,'private-feature-list.txt',feature_names)
                engine.adb_call(['push',str(manifest),engine.remote+'/'+manifest.name],timeout=30)
                stage='native_persistent_run'
                trial_before_battery = battery_snapshot(engine)
                trial_before_thermal = thermal_snapshot(engine)
                assert_phone_cool_enough(trial_before_battery,trial_before_thermal)
                if getattr(args,'temperature_match',False) and type(trial_before_battery.get('battery_temp_c')) not in (float,int):
                    raise BridgeError('MULTI_THERMAL_MATCH_BASELINE_UNAVAILABLE')
                base_command=['shell','env','LD_LIBRARY_PATH='+engine.remote,
                            engine.remote+'/'+PROBE_NAME,engine.remote+'/'+MODEL_NAME,
                            '--multi',engine.remote+'/'+manifest.name,str(args.steps)]
                dual = bool(getattr(args,'dual_thread_sweep',False))
                tune = bool(getattr(args,'cpu_thread_sweep',False)) or dual
                plan = MATCHED_SWEEP if dual else tuple((t,4) for t in THREAD_SWEEP) if tune else ((4,4),) * (1+len(quant_sources) if quant_compare else getattr(args,'full_trials',1))
                row['cpu_thread_sweep'] = tune
                row['matched_prefill_generation_sweep'] = dual
                row['decoder_profiles_order'] = [str(d)+'/'+str(b) for d,b in plan]
                if quant_compare:
                    row['decoder_quant_order']=['Q8_0',*[m['quant'] for _,_,m in quant_sources]]
                    row['decoder_model_variants_requested']=len(plan)
                row['temperature_match_requested'] = bool(getattr(args,'temperature_match',False))
                row['temperature_match_method'] = 'battery_temperature_proxy_within_0_5C_before_each_trial_not_soc_temperature' if row['temperature_match_requested'] else 'not_requested'
                row['cpu_frequency_snapshots_scope'] = 'optional_sysfs_point_samples_not_continuous_and_not_energy'
                row['cpu_frequency_before_trial_1'] = cpu_frequency_snapshot(engine)
                def command_for(profile, quant_index=0):
                    d,b = profile
                    command=list(base_command)
                    if quant_index:
                        command[4]=engine.remote+'/'+quant_sources[quant_index-1][1]
                    return command + (['--decode-threads',str(d),'--batch-threads',str(b)] if dual else
                        ['--decode-threads',str(d)] if (tune or quant_compare or q8_fast) else [])
                parsed,summary = run_native_once(engine,command_for(plan[0]),len(parts),'full',row,
                    timeout=1800,expected_threads=plan[0][0] if (tune or quant_compare or q8_fast) else None,
                    expected_batch_threads=plan[0][1])
                for meta,metric in zip(row['segments'],parsed):
                    meta.update({k:v for k,v in metric.items() if k!='index'})
                row['native_decoder'] = summary
                row['eos_segments'] = sum(bool(v['eos_reached']) for v in parsed)
                row['native_total_tokens'] = sum(v['generation_steps'] for v in parsed)
                # After one successful full pass, run controlled *intentional*
                # repeatability trials on the SAME staged audio features. No
                # second QNN pass or new files on user storage. If any trial
                # fails, stop; never auto-retry an ambiguous ADB execution.
                requested = len(plan)
                row['repeatability_trial_count_requested'] = requested
                row['native_trials'] = [collect_trial_metadata(1,parsed,summary,
                    row['full_command_wall_s_including_adb'],battery_snapshot(engine),
                    battery_before=trial_before_battery,
                    thermal_before=trial_before_thermal,thermal_after=thermal_snapshot(engine))]
                row['native_trials'][0]['cpu_frequency_before'] = row['cpu_frequency_before_trial_1']
                row['native_trials'][0]['cpu_frequency_after'] = cpu_frequency_snapshot(engine)
                for trial_number in range(2,requested+1):
                    stage='native_repeat_trial_'+str(trial_number)
                    cooling={'status':'not_requested','wait_s':0}
                    if row['temperature_match_requested']:
                        stage='temperature_match_before_trial_'+str(trial_number)
                        cooling=wait_for_temperature_match(engine,row['native_trials'][0]['battery_before'])
                    stage='native_repeat_trial_'+str(trial_number)
                    freq_before=cpu_frequency_snapshot(engine)
                    trial_before_battery = battery_snapshot(engine)
                    trial_before_thermal = thermal_snapshot(engine)
                    assert_phone_cool_enough(trial_before_battery,trial_before_thermal)
                    extra,extra_summary = run_native_once(engine,command_for(plan[trial_number-1],trial_number-1 if quant_compare else 0),len(parts),
                        'repeat_trial_'+str(trial_number),row,timeout=1800,
                        expected_threads=plan[trial_number-1][0] if (tune or quant_compare or q8_fast) else None,
                        expected_batch_threads=plan[trial_number-1][1])
                    row['native_trials'].append(collect_trial_metadata(trial_number,
                        extra,extra_summary,row['repeat_trial_'+str(trial_number)+'_command_wall_s_including_adb'],
                        battery_snapshot(engine),battery_before=trial_before_battery,
                        thermal_before=trial_before_thermal,thermal_after=thermal_snapshot(engine)))
                    row['native_trials'][-1]['cooling_before'] = cooling
                    row['native_trials'][-1]['cpu_frequency_before'] = freq_before
                    row['native_trials'][-1]['cpu_frequency_after'] = cpu_frequency_snapshot(engine)
                if quant_compare:
                    try:
                        row['quantization_comparison'] = compare_quant_trials(row['native_trials'], row['quant_models'])
                    except QuantModelError as exc:
                        raise BridgeError(str(exc)) from exc
                    # Mixed model sizes are deliberately NOT repeatability trials.
                    row['repeatability'] = {'status':'NOT_MEASURED_MIXED_QUANT_MODELS'}
                else:
                    row['repeatability'] = summarize_decoder_trials(row['native_trials'],required=requested)
                row['thermal_observations'] = summarize_thermal_observations(row['native_trials'])
                if tune:
                    row['cpu_thread_sweep_comparison'] = summarize_thread_sweep(row['native_trials'], matched=dual)
                row['model_loads_for_session'] = 1+len(row['native_trials']) # one canary plus one per trial
                row['full_run_decoder_model_loads'] = summary['model_loads']
                row['battery_after'] = row['native_trials'][-1]['battery_after']
                row['status'] = 'MEASURED_EXPERIMENTAL_MULTI_SEGMENT_UNQUALIFIED'
            finally:
                INPUTS.pop(role,None)
                OUTPUTS.pop(role,None)
                if engine is not None:
                    if getattr(engine, 'staged', False) is False:
                        # If device preflight failed before mkdir succeeded, no phone
                        # files exist: never misreport a failed cleanup as staged.
                        row['phone_all_files_cleanup'] = 'not_staged'
                        recovery.unlink(missing_ok=True)
                    else:
                        if 'battery_after' not in row:
                            row['battery_after'] = battery_snapshot(engine)
                        if not scoped_cleanup_receipt(engine,recovery,row):
                            row['status'] = 'FAILED_PHONE_CLEANUP_UNVERIFIED'
    except Exception as exc:
        row['error_stage'] = re.sub(r'[^A-Za-z0-9_]', '_', stage)[:80]
        row['error_code'] = str(exc) if isinstance(exc,BridgeError) and re.fullmatch('[A-Z][A-Z0-9_]{4,100}',str(exc)) else type(exc).__name__
        if row['phone_all_files_cleanup'] == 'UNVERIFIED_REQUIRES_RECOVERY':
            row['status'] = 'FAILED_PHONE_CLEANUP_UNVERIFIED'
        else:
            row['status'] = 'BLOCKED_OR_FAILED'
    finally:
        row['host_workflow_wall_s'] = round(time.monotonic()-t0,3)
        if row.get('native_trials') and 'qnn_encoder_total_wall_s_including_io' in row:
            explicitly_timed = (float(row['qnn_encoder_total_wall_s_including_io']) +
                float(row.get('canary_command_wall_s_including_adb',0)) +
                sum(float(t['native_command_wall_s_including_adb']) for t in row['native_trials']))
            row['other_host_workflow_time_s_estimate'] = round(max(0.0,
                row['host_workflow_wall_s'] - explicitly_timed),3)
            row['other_host_workflow_time_scope'] = ('Unattributed Windows orchestration, '
                'preprocessing, transfer and cleanup; NOT integrated production latency')
        row['comparison_boundaries'] = {'qnn_wall_includes_adb_io':True,
             'native_wall_includes_adb_shell':True,
             'native_decoder_segments_excludes_initial_model_load':True,
             'host_wall_includes_staging_and_cleanup':True}
        row['remaining_unqualified']=['full_text_accuracy','authoritative_four_axis_mrope',
            'controlled_PSS','calibrated_energy','continuous_cpu_thermal_throttling_trace',
            'apples_to_apples_baseline']
        output = report_zip(results,row)
        print('DIAGNOSTIC_ZIP='+str(output),flush=True)
        print('MULTI_BENCH_STATUS='+row['status'],flush=True)
        # Safe, bounded receipt summary for the Windows UI. Never emit raw ADB
        # stderr, paths, audio, token IDs or model data on this status channel.
        print('MULTI_BENCH_ERROR_STAGE='+str(row.get('error_stage','none')),flush=True)
        print('MULTI_BENCH_ERROR_CODE='+str(row.get('error_code','none')),flush=True)
        print('MULTI_BENCH_CLEANUP='+str(row['phone_all_files_cleanup']),flush=True)
    return (0 if row['status']=='MEASURED_EXPERIMENTAL_MULTI_SEGMENT_UNQUALIFIED' and
                  row['phone_all_files_cleanup']=='verified' else 2),output


def main(argv=None):
    p=argparse.ArgumentParser(description='Experimental persistent QNN+GGUF full-recording benchmark')
    for name in ('adb','serial','ffmpeg','audio','qnn-context','runtime-manifest','cache','probe-archive','results'):
        p.add_argument('--'+name,required=True)
    p.add_argument('--max-seconds',type=int,choices=(36,120,240),default=240)
    p.add_argument('--threads',type=int,default=4)
    p.add_argument('--steps',type=int,default=96)
    p.add_argument('--full-trials',type=int,choices=(1,3),default=3)
    p.add_argument('--q8-fast-baseline',action='store_true',help='One Q8 decoder trial, 4/4 threads, CPU time and safety receipts; no thermal repeats')
    p.add_argument('--cpu-thread-sweep',action='store_true',help='One session 4/3/2/4 decoder threads, batch threads fixed at 4')
    p.add_argument('--dual-thread-sweep',action='store_true',help='One session matching decoder/prefill 4/4,3/3,2/2,4/4')
    p.add_argument('--temperature-match',action='store_true',help='Opt-in bounded cooling until the battery temperature proxy returns to within 0.5 C of trial 1 (4 minute cap per interval)')
    p.add_argument('--quant-compare',action='store_true',help='Compare pinned Q8 vs selected Q4_K_M and optional Q5_K_M using identical NPU features')
    p.add_argument('--quant-q4-model',help='Local GGUF Q4_K_M decoder, never a standalone Qwen3 text-only model')
    p.add_argument('--quant-q5-model',help='Optional local GGUF Q5_K_M decoder')
    p.add_argument('--ack-provisional-accuracy',action='store_true')
    args=p.parse_args(argv)
    return run(args)[0]


if __name__ == '__main__':
    sys.exit(main())
