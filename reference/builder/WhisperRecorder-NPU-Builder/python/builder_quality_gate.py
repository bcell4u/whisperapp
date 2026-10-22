#!/usr/bin/env python3
"""Offline Builder preflight, regression-test and postflight gate.

Standard-library only in quick/postflight modes. Never downloads, runs models,
changes originals, uses network/ADB, or initiates Qualcomm/paid operations.
Full mode runs the already-shipped regression tests, without another service.
Receipts are deliberately path/audio/token-free, including the shareable ZIP.
"""
from __future__ import annotations
import argparse
import ast
from datetime import datetime, timezone
import hashlib
import importlib.util
import os
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile

# Single mode registry for the CLI and the GUI wiring audit. Explicit approval for
# paid model-weight cloud work is never granted by any quality-gate mode.
QUALITY_GATE_MODES = (
    'quick', 'full', 'postflight', 'postflight-j13', 'postflight-j14',
    'postflight-j16', 'postflight-j19', 'postflight-j21', 'postflight-j22',
    'postflight-j25', 'postflight-j28', 'postflight-j31', 'postflight-j34',
    'postflight-j37', 'postflight-j40', 'postflight-j42', 'postflight-j43',
    'postflight-j45', 'postflight-j46', 'postflight-j48', 'postflight-j49',
    'postflight-j50', 'postflight-j53', 'postflight-j56',
    'postflight-j57', 'postflight-j59', 'postflight-j62',
    'postflight-j63', 'postflight-j65', 'postflight-j66', 'postflight-j68',
    'postflight-j69', 'postflight-j71', 'postflight-j72', 'postflight-j73', 'postflight-j74', 'postflight-j75', 'postflight-j77', 'postflight-j79', 'postflight-j80', 'postflight-j81', 'postflight-j83', 'postflight-j84', 'postflight-j85', 'postflight-j86',
)

ROOT = Path(__file__).resolve().parents[1]
PYTHON_DIR = ROOT / 'python'
MAX_RECEIPT_BYTES = 2 * 1024 * 1024
VERSION = '0.4.96'

class GateError(Exception):
    def __init__(self, code, public_context=None):
        self.code = code
        # Only bounded test identifiers/counts, never raw output, paths or user data.
        self.public_context = public_context or {}
        super().__init__(code)


def check(code, predicate):
    if not predicate:
        raise GateError(code)


def read_json(path):
    check('QG_RECEIPT_NOT_FILE', path.is_file() and not path.is_symlink())
    check('QG_RECEIPT_TOO_LARGE', path.stat().st_size <= MAX_RECEIPT_BYTES)
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except (OSError, ValueError, UnicodeError) as exc:
        raise GateError('QG_RECEIPT_INVALID_JSON') from exc


def syntax_gate(root=ROOT):
    src = root / 'python'
    check('QG_PYTHON_SOURCES_MISSING', src.is_dir())
    names = sorted(src.glob('*.py'))
    check('QG_PYTHON_SOURCES_EMPTY', len(names) >= 10)
    for path in names:
        check('QG_PYTHON_SYMLINK_BLOCKED', not path.is_symlink())
        try:
            ast.parse(path.read_text(encoding='utf-8'), filename=path.name)
        except (SyntaxError, UnicodeError):
            raise GateError('QG_PYTHON_SYNTAX_ERROR')
    return len(names)



def bridge_review_interface_gate(root=ROOT):
    """Statically validate the cross-file PCM review dependency, without ADB.

    This catches a partial/source-only installation that otherwise compiles
    cleanly but fails on first use of the audio-bound transcript preflight.
    """
    mel = root / 'python' / 'qwen_bridge_mel.py'
    review = root / 'python' / 'qwen_bridge_ground_truth.py'
    check('QG_BRIDGE_REVIEW_SOURCE_MISSING',
          mel.is_file() and review.is_file() and
          not mel.is_symlink() and not review.is_symlink())
    try:
        mel_ast = ast.parse(mel.read_text(encoding='utf-8'))
        review_ast = ast.parse(review.read_text(encoding='utf-8'))
    except (SyntaxError, UnicodeError):
        raise GateError('QG_BRIDGE_REVIEW_INTERFACE_INVALID')
    exported = {n.name for n in mel_ast.body
                if isinstance(n,(ast.FunctionDef,ast.AsyncFunctionDef,ast.ClassDef))}
    required = {'decode_first_12s_pcm','pcm_window_evidence','BridgeMelError'}
    check('QG_BRIDGE_REVIEW_INTERFACE_MISSING', required <= exported)
    imports = {a.name for n in review_ast.body if isinstance(n,ast.Try)
               for b in n.body if isinstance(b,ast.ImportFrom) and b.module=='qwen_bridge_mel'
               for a in b.names}
    check('QG_BRIDGE_REVIEW_IMPORT_CONTRACT', required <= imports)
    return 'review_pcm_import_contract_present'


def powershell_startup_declaration_gate(gui):
    """Fail on malformed script-scope declarations before a Windows GUI release.

    This static guard catches the exact v0.4.26 '$$script:' parse failure
    even when PowerShell is absent from the development/test environment.
    Windows bootstrap still performs authoritative PowerShell AST parsing.
    """
    check('QG_POWERSHELL_SCOPE_PREFIX_INVALID',
          re.search(r'(?m)^\s*\${2,}(?:script|global|local|private):', gui) is None)
    for name, target in (
        ('QwenAsrStageJ72J73Path', 'qwen3_asr_stage_j72_j73.py'),
        ('QwenAsrStageJ74Path', 'qwen3_asr_stage_j74.py'),
        ('QwenAsrStageJ75J76Path', 'qwen3_asr_stage_j75_j76.py'),
        ('QwenAsrStageJ77J79Path', 'qwen3_asr_stage_j77_j79.py'),
    ):
        pattern = (r"(?m)^\$script:" + re.escape(name) +
                   r"\s*=\s*Join-Path\s+\$Root\s+'python\\" +
                   re.escape(target) + r"'\s*$")
        check('QG_POWERSHELL_STAGE_PATH_DECLARATION_INVALID',
              len(re.findall(pattern, gui)) == 1)
    return True

def powershell_j78_optional_receipt_gate(gui):
    """J78 must not dereference absent cloud URLs from a no-upload JSON receipt.

    PowerShell 5.1 StrictMode rejects `$old.compile_job_url` on the legitimate
    `J77_FP16_PACKAGE_CHANGED` receipt. This source guard protects every
    release even when native PowerShell is unavailable on the test machine.
    """
    parts = gui.split('$btnQwenJ78.Add_Click({', 1)
    check('QG_J78_GUI_ACTION_MISSING', len(parts) == 2)
    parts = parts[1].split('$btnQwenStageJDeleteAuditZips=', 1)
    check('QG_J78_GUI_END_MISSING', len(parts) == 2)
    block = parts[0]
    sections = block.split('$noUpload=(', 1)
    check('QG_J78_PREUPLOAD_CHECK_MISSING', len(sections) == 2)
    checks = sections[1].split('$knownCompileFailure=(', 1)
    check('QG_J78_FAILED_COMPILE_CHECK_MISSING', len(checks) == 2)
    preupload = checks[0]
    for name in ('compile_job_url', 'link_job_url'):
        check('QG_J78_OPTIONAL_JOB_URL_UNSAFE',
              '$old.' + name not in preupload and
              "$old.PSObject.Properties.Name -contains '" + name + "'" in preupload)
    check('QG_J78_RETRY_CONSENT_MISSING',
          'J78 model weight upload approval' in checks[1] and
          'J78 Qualcomm cloud compute approval' in checks[1])
    return True


def wiring_gate(root=ROOT, expected_version=VERSION):
    version = (root / 'VERSION.txt').read_text(encoding='utf-8').strip()
    gui = (root / 'Start-NpuBuilder.ps1').read_text(encoding='utf-8-sig')
    batch = (root / 'python' / 'qwen3_asr_stage_j_batch.py').read_text(encoding='utf-8')
    check('QG_RELEASE_VERSION_MISMATCH', version == expected_version and
          "$script:AppVersion = '" + expected_version + "'" in gui)
    for required in ('qwen3_asr_stage_j_batch.py', 'builder_quality_gate.py',
                     'Invoke-UiAction', '--output', '--audio', '--onnx-dir', 'qwen3_asr_stage_j12_j13.py', 'qwen3_asr_stage_j14.py', 'qwen3_asr_stage_j15_j16.py', 'qwen3_asr_stage_j17_j19.py', 'qwen3_asr_stage_j20_j22.py', 'qwen3_asr_stage_j23_j25.py', 'qwen3_asr_stage_j29_j31.py', 'qwen3_asr_stage_j32_j34.py', 'qwen3_asr_stage_j35_j37.py', 'qwen3_asr_stage_j38_j40.py', 'qwen3_asr_stage_j41_j43.py', 'qwen3_asr_stage_j44_j46.py', 'qwen3_asr_stage_j47_j49.py', 'postflight-j48', 'postflight-j49', 'postflight-j50', 'qwen3_asr_stage_j50.py', 'qwen3_asr_stage_j51_j53.py', 'qwen3_asr_stage_j54_j56.py', 'qwen3_asr_stage_j57_j59.py', 'qwen3_asr_stage_j60_j62.py', 'qwen3_asr_stage_j63_j65.py', 'qwen3_asr_stage_j66_j68.py', 'qwen3_asr_stage_j69_j71.py', 'qwen3_asr_stage_j72_j73.py', 'qwen3_asr_stage_j74.py', 'qwen3_asr_stage_j75_j76.py', 'qwen3_asr_stage_j82_j83.py', 'qwen3_asr_stage_j84_j85.py', 'qwen3_asr_stage_j86.py', 'qwen3_asr_stage_k1.py',
                     'postflight-j69', 'postflight-j71', 'postflight-j72', 'postflight-j73', 'postflight-j74', 'postflight-j75', 'postflight-j77', 'postflight-j79', 'postflight-j80', 'postflight-j81', 'postflight-j83', 'postflight-j84', 'postflight-j85', 'postflight-j86', 'postflight-j66', 'postflight-j68', 'postflight-j63', 'postflight-j65', 'postflight-j53','postflight-j56','postflight-j57','postflight-j59','postflight-j62', 'QwenStageActionsResponsiveGrid'):
        check('QG_GUI_WIRING_MISSING', required in gui)
    # New compact-model audit must have its own page, stay offline, and
    # publish the report in a dedicated share-to-chat directory.
    check('QG_K1_WIRING_MISSING',
          "$tabQwenOptimize.Text = 'Qwen ASR - Optimize'" in gui and
          '$script:QwenAsrStageK1Path' in gui and
          '$script:QwenAsrStageK2Path' in gui and
          '$script:QwenAsrStageK3Path' in gui and
          '$script:QwenAsrStageK4Path' in gui and
          '$btnQwenK4.Add_Click' in gui and
          '$btnQwenK3.Add_Click' in gui and
          '$btnQwenK2A.Add_Click' in gui and
          '$btnQwenK2B.Add_Click' in gui and
          "'--package'" in gui and 'Qwen-K1-audit-' in gui and
          'explorer.exe' in gui and 'stage-k-model-optimization' in gui and
          (root / 'python' / 'qwen3_asr_stage_k1.py').is_file() and
          (root / 'python' / 'qwen3_asr_stage_k2.py').is_file() and
          (root / 'python' / 'qwen3_asr_stage_k3.py').is_file())
    # Phone Benchmarks must preserve no-APK execution, explicit diagnostic ZIP,
    # isolated staging and never use the WhisperRecorder model directory.
    phone = root / 'python' / 'llama_phone_benchmark.py'
    check('QG_PHONE_BENCH_SOURCE_MISSING', phone.is_file())
    check('QG_PHONE_BENCH_GUI_MISSING',
          all(marker in gui for marker in (
              "Phone Benchmarks", '$btnPhoneRun.Add_Click',
              '$btnPhoneCleanup.Add_Click', 'DIAGNOSTIC_ZIP=',
              'llama_phone_benchmark.py')))
    source = phone.read_text(encoding='utf-8')
    check('QG_PHONE_BENCH_ISOLATION_MISSING',
          'BENCH_ROOT = "/data/local/tmp/wr-llama-bench-v1"' in source and
          'cleanup(ns)' in source and 'phone_run_cleanup' in source and
          'safe_unpack_cpu' in source and 'sha256sum' in source)
    # Regression: official b11430 Android ARM64 CPU asset is TAR.GZ, not ZIP.
    check('QG_PHONE_BENCH_TAR_GZ_REGRESSION',
          'llama-{tag}-bin-android-arm64.tar.gz' in source and
          'tarfile.open(archive, mode="r:gz")' in source and
          'tar_target_member' in source and
          (root / 'python' / 'test_llama_phone_benchmark_0448.py').is_file())
    # v0.4.52: b11430 interactive mtmd CLI kept a stale multimodal batch after
    # /clear and aborted on the second audio. Run one pinned llama-server process
    # with independent HTTP transcription requests; never silently fall back.
    check('QG_PHONE_PERSISTENT_HTTP_AND_CLEANUP',
          all(mark in source for mark in (
              'persistent_http_model_one_request_per_segment',
              'llama-server', 'server_transcribe', 'wait_server_ready',
              'forwarded_base', 'adb_forward_cleanup', 'owned_native_alive',
              'stop_owned_native_process', 'cleanup_phone_benchmark(phone)',
              'phone_all_files_cleanup', 'WR_BENCH_REMOVED',
              'process_launches')) and
          'persistent_commands' not in source and
          'persistent_transcriptions' not in source and
          'llama-mtmd-cli -m MODEL' not in source and
          '--simple-io' not in source and
          (root / 'python' / 'test_llama_phone_benchmark_0449.py').is_file() and
          (root / 'python' / 'test_llama_phone_benchmark_0450.py').is_file() and
          (root / 'python' / 'test_llama_phone_benchmark_0452.py').is_file() and
          'Full recording (single model, auto 12s segments)' in gui and
          'removes ALL benchmark files from phone' in gui)
    # 0.4.53: bridge readiness must never masquerade as a native hybrid test.
    qualifier = root / 'python' / 'qwen_hybrid_qualifier.py'
    check('QG_QWEN_HYBRID_QUALIFIER_MISSING', qualifier.is_file())
    qual_src = qualifier.read_text(encoding='utf-8')
    check('QG_QWEN_HYBRID_READINESS_GATE',
          all(mark in qual_src for mark in ('native_bridge_execution',
              'NOT_IMPLEMENTED', 'mrope_audio_position_parity',
              'phone_all_files_cleanup', 'reference_audio_duration_matches',
              'DIAGNOSTIC_ZIP=', 'GGUF')) and
          all(mark in gui for mark in ('Qwen hybrid readiness',
              '$btnQwenBridgeAudit.Add_Click', '$script:QwenHybridQualifierPath',
              'qwen_hybrid_qualifier.py', 'No extra downloads')) and
          (root / 'python' / 'test_qwen_hybrid_qualifier_0453.py').is_file())
    # 0.4.54 actual QNN feature -> native GGUF execution must not be confused
    # with existing read-only readiness audit or bypass verified scoped cleanup.
    bridge_src = root / 'python' / 'qwen_native_bridge_phone.py'
    check('QG_QWEN_NATIVE_BRIDGE_EXECUTION', bridge_src.is_file() and
          all(x in bridge_src.read_text(encoding='utf-8') for x in (
            'engine.run(role', 'parse_native_metrics', 'scoped_cleanup_receipt',
            'mrope_parity_verified', 'NPU_ENCODER_TO_GGUF_DECODER_EXECUTED_UNQUALIFIED',
            'phone_all_files_cleanup', 'DIAGNOSTIC_ZIP=')) and
          all(x in gui for x in (
            'Qwen NPU Bridge', '$btnBridgeTest.Add_Click', '$btnBridgeRecover.Add_Click',
            'QwenBridgePhonePath', 'RUN instrumented parity + ALL qualification gates')) and
          (root / '.github' / 'workflows' / 'qwen-native-bridge-probe.yml').is_file() and
          (root / 'native' / 'qwen_bridge' / 'qwen_npu_llama_probe.cpp').is_file() and
          (root / 'python' / 'test_qwen_native_bridge_phone_0454.py').is_file() and
          (root / 'python' / 'qwen_bridge_mel.py').is_file() and
          (root / 'python' / 'test_qwen_bridge_mel_0454.py').is_file())
    # v0.4.64: the main qualification path MUST actually call the native
    # instrumented probe, run original ONNX on Windows after phone cleanup,
    # and then write exactly one final gate-audit ZIP. Existing offline audit
    # remains available for repeated analysis without staging phone files.
    compare = root / 'python' / 'qwen_bridge_reference.py'
    native = root / 'native' / 'qwen_bridge' / 'qwen_npu_llama_probe.cpp'
    check('QG_QWEN_INSTRUMENTED_PARITY',
          compare.is_file() and native.is_file() and
          (root / 'python' / 'test_qwen_bridge_instrumentation_0464.py').is_file() and
          all(x in compare.read_text(encoding='utf-8') for x in (
             'reference_tokens', 'private_trace', 'compare_in_memory',
             'qwen3_asr_stage_g', 'provisional_linear_4d')) and
          all(x in native.read_text(encoding='utf-8') for x in (
             'WR_BRIDGE_PRIVATE_TRACE=', '--private-trace',
             'const llama_pos p[4]', 'llama_batch_ext_set_pos(batch, idx, p)')) and
          all(x in gui for x in (
             '$btnBridgeInstrument.Add_Click', '$btnBridgeBatch.Add_Click',
             '--onnx-export', 'Get-QwenBridgePython -RequireOnnx',
             '$script:QwenBridgeReferencePath', 'RUN instrumented parity + ALL qualification gates')) and
          all(x in bridge_src.read_text(encoding='utf-8') for x in (
             'run_comparison', 'onnx_parity', 'deps-parity', 'scoped_cleanup_receipt')))
    # v0.4.55: no browse-first Qwen bridge setup; use existing Stage I/QNN
    # runtime and probe downloads without touching phone or model files.
    discover_src = root / 'python' / 'qwen_bridge_autodiscover.py'
    check('QG_QWEN_BRIDGE_AUTODISCOVERY_MISSING',
          discover_src.is_file() and
          all(mark in discover_src.read_text(encoding='utf-8') for mark in (
              'find_model_folder', 'validate_runtime', 'runtime_files_under',
              'make_owned_manifest', 'valid_probe', 'MAX_DIRS')) and
          all(mark in gui for mark in (
              'QwenBridgeDiscoveryPath', 'Find-QwenBridgeExistingFiles',
              '$btnBridgeAutoFind.Add_Click', 'BridgeAutofindTried',
              'Audio preprocessing uses existing FFmpeg + NumPy automatically')) and
          (root / 'python' / 'test_qwen_bridge_autodiscover_0455.py').is_file())
    # v0.4.56: with StrictMode Latest the WinForms tab callback must never
    # read an undeclared $script:QwenBridgeAutofindTried.  Validate the
    # explicit False initializer occurs BEFORE registering the callback.
    # v0.4.55 failed on the first tab switch despite passing syntax preflight.
    startup_flag = '$script:QwenBridgeAutofindTried = $false'
    tab_callback = '$tabs.Add_SelectedIndexChanged({'
    flag_uses = re.findall(r'\$script:QwenBridgeAutofindTried\b', gui)
    check('QG_QWEN_BRIDGE_AUTOFIND_STRICTMODE_INIT',
          gui.count(startup_flag) == 1 and len(flag_uses) == 3 and
          gui.index(startup_flag) < gui.index(tab_callback) and
          gui.index(tab_callback) < gui.index('if ($tabs.SelectedTab -eq $tabQwenBridge -and -not $script:QwenBridgeAutofindTried)'))
    # v0.4.57: PS 5.1 command construction drops blank elements before Python
    # argparse sees them.  A blank Browse field MUST mean an omitted OPTIONAL
    # switch, never --context followed by --runtime or another flag.
    find_start = gui.index('function Find-QwenBridgeExistingFiles {')
    find_end = gui.index('$btnBridgeAutoFind.Add_Click({', find_start)
    find_body = gui[find_start:find_end]
    check('QG_QWEN_BRIDGE_DISCOVERY_EMPTY_ARGS',
          "'discover', '--root', $Root" in find_body and
          "IsNullOrWhiteSpace($txtBridgeQnnContext.Text)" in find_body and
          "IsNullOrWhiteSpace($txtBridgeQnnRuntime.Text)" in find_body and
          "IsNullOrWhiteSpace($txtBridgeProbe.Text)" in find_body and
          "IsNullOrWhiteSpace($txtRuntimeRoot.Text)" in find_body and
          "@('--context', $txtBridgeQnnContext.Text)" in find_body and
          "@('--runtime', $txtBridgeQnnRuntime.Text)" in find_body and
          "@('--probe', $txtBridgeProbe.Text)" in find_body and
          "@('--runtime-root', $txtRuntimeRoot.Text)" in find_body and
          "'--context',$txtBridgeQnnContext.Text" not in find_body)
    # v0.4.58: the actual bridge runner must never use bare portable CPython
    # without testing its NumPy/QNN helper imports. The recovery path must
    # remain runnable with standard-library Python even if NumPy is missing.
    test_begin = gui.index('$btnBridgeTest.Add_Click({')
    test_end = gui.index('$btnBridgeRecover.Add_Click({', test_begin)
    bridge_action = gui[test_begin:test_end]
    deps_begin = gui.index('function Get-QwenBridgePython {')
    deps_end = gui.index('function Find-QwenBridgeExistingFiles {', deps_begin)
    deps_block = gui[deps_begin:deps_end]
    recover_begin = gui.index('$btnBridgeRecover.Add_Click({')
    recover_end = gui.index('# -------------------- Logs --------------------', recover_begin)
    recover_action = gui[recover_begin:recover_end]
    runner_src = bridge_src.read_text(encoding='utf-8')
    check('QG_QWEN_BRIDGE_PYTHON_DEPENDENCY_PREFLIGHT',
          '$py = Get-QwenBridgePython' in bridge_action and
          '$py = Get-LocalBuilderPython' not in bridge_action and
          "'deps'" in deps_block and
          r"tools\qai-hub-env\Scripts\python.exe" in deps_block and
          'BRIDGE_DEPENDENCIES=ready' in deps_block and
          '$py = Get-LocalBuilderPython' in recover_action and
          "sub.add_parser('deps'" in runner_src and
          'def check_host_dependencies()' in runner_src and
          "report['missing_python_module']" in runner_src)
    # v0.4.59: the actual reported failure occurred before ADB staging in
    # audio_preprocess. Always expose the bounded, path-free reason code.
    # Never include raw FFmpeg stderr, user audio paths, or sample values.
    mel_src = (root / 'python' / 'qwen_bridge_mel.py').read_text(encoding='utf-8')
    check('QG_QWEN_BRIDGE_AUDIO_PREPROCESS_REPORTING',
          'class BridgeMelError(ValueError)' in mel_src and
          'MAX_CODEC_OVERHANG_SAMPLES = 2048' in mel_src and
          "raise BridgeMelError('FFMPEG_DECODE_ERROR')" in mel_src and
          "raise BridgeMelError('FFMPEG_UNEXPECTED_SAMPLE_COUNT')" in mel_src and
          "report['audio_preprocess_failure_code']" in runner_src and
          "'UNEXPECTED_NUMERIC_VALUE_ERROR'" in runner_src and
          (root / 'python' / 'test_qwen_bridge_audio_preflight_0459.py').is_file())
    # v0.4.60: the native ELF reports its FINAL generation cursor in the
    # legacy `prompt_positions` field. Normalize only when independently
    # consistent with the EOS flag and token steps; otherwise fail closed.
    check('QG_QWEN_BRIDGE_NATIVE_CURSOR_ACCOUNTING',
          'legacy_final_generation_cursor' in runner_src and
          'NATIVE_POSITION_ACCOUNTING_MISMATCH' in runner_src and
          'native_reported_final_position' in runner_src and
          'decoder_failure_code' in runner_src and
          (root / 'python' / 'test_qwen_bridge_native_cursor_0460.py').is_file())
    # v0.4.61: this release's primary workflow must be selected and keyboard-
    # focused on launch, not left behind the standalone Phone Benchmarks tab.
    # Do not change tab order or invoke any phone-side action during startup.
    startup_tail = gui.split('# -------------------- Startup --------------------', 1)
    check('QG_QWEN_BRIDGE_STARTUP_TAB_AND_FOCUS',
          len(startup_tail) == 2 and
          '$tabs.SelectedTab = $tabQwenBridge' in startup_tail[1] and
          '$tabs.SelectedTab = $tabPhoneBench' not in startup_tail[1] and
          'Update-WorkflowNavigation' in startup_tail[1] and
          '$form.Add_Shown({' in startup_tail[1] and
          '[void]$tabs.Focus()' in startup_tail[1] and
          startup_tail[1].index('$form.Add_Shown({') < startup_tail[1].index('[void]$form.ShowDialog()'))
    # v0.4.62: batch all downstream bridge qualification gates in one offline
    # receipt. Not even a successful short probe can assert transcript parity.
    batch_path = root / 'python' / 'qwen_bridge_batch_qualification.py'
    batch_src = batch_path.read_text(encoding='utf-8') if batch_path.is_file() else ''
    check('QG_QWEN_BRIDGE_BATCH_QUALIFICATION',
          "$script:QwenBridgeBatchPath = Join-Path $Root 'python\\qwen_bridge_batch_qualification.py'" in gui and
          "$btnBridgeBatch.Add_Click({" in gui and
          'Offline audit existing evidence (no phone test)' in gui and
          "'qualifies_android_integration':bool(trusted and all_pass)" in batch_src and
          "trusted=False" in batch_src and
          'phone_modified_by_this_batch' in batch_src and
          'mrope_positions' in batch_src and 'token_parity' in batch_src and
          'fair_speed' in batch_src and 'cleanup_all_runs' in batch_src and
          (root / 'python' / 'test_qwen_bridge_batch_qualification_0462.py').is_file())
    # v0.4.63: optional comparison fields start empty on a clean install.
    # Test-Path -LiteralPath '' throws on Windows PowerShell 5.1. Verify both
    # non-empty guards precede ANY Test-Path in the batch click handler.
    batch_action = gui.split('$btnBridgeBatch.Add_Click({', 1)
    check('QG_QWEN_BRIDGE_BATCH_HANDLER_MISSING', len(batch_action) == 2)
    batch_action = batch_action[1].split('$btnBridgeTest.Add_Click({', 1)
    check('QG_QWEN_BRIDGE_BATCH_HANDLER_BOUNDARY_MISSING', len(batch_action) == 2)
    batch_action = batch_action[0]
    a = batch_action.find('IsNullOrWhiteSpace($historicalStandalone)')
    b = batch_action.find('IsNullOrWhiteSpace($historicalHybrid)')
    c = batch_action.find('Test-Path -LiteralPath $historicalStandalone')
    d = batch_action.find('Test-Path -LiteralPath $historicalHybrid')
    check('QG_QWEN_BRIDGE_BATCH_OPTIONAL_PATHS_UNGUARDED',
          -1 not in (a,b,c,d) and a < b < c < d and
          'Test-Path -LiteralPath $txtQwenBridgeBenchmark.Text' not in batch_action and
          'Test-Path -LiteralPath $txtQwenBridgeHybrid.Text' not in batch_action and
          "'--standalone-zip', $historicalStandalone" in batch_action and
          "'--hybrid-zip', $historicalHybrid" in batch_action)
    powershell_startup_declaration_gate(gui)
    powershell_j78_optional_receipt_gate(gui)
    # This independently compares EVERY GUI --mode invocation with argparse's
    # allow-list and ensures each postflight mode has a real validation handler.
    # A missing parser entry caused J54-J56 to succeed on Windows but J56 to
    # fail with argparse exit 2 in v0.4.10, despite 924 passing source tests.
    ui_modes = set(re.findall(r"'--mode'\s*,\s*'([^']+)'", gui))
    check('QG_GUI_QUALITY_MODE_MISSING', bool(ui_modes) and
          'postflight-j56' in ui_modes and ui_modes.issubset(QUALITY_GATE_MODES))
    handlers = set(re.findall(r"if args\.mode == '([^']+)'", (root / 'python' / 'builder_quality_gate.py').read_text(encoding='utf-8')))
    check('QG_GUI_QUALITY_MODE_HANDLER_MISSING',
          all(m in handlers for m in ui_modes if m.startswith('postflight')))
    # J47 uses only ONNX/ORT/Numpy; importing the entire PyTorch/Qwen export
    # stack in its GUI preflight caused the user's Windows 75-second timeout.
    j47 = gui.split('$btnQwenJ47.Add_Click({', 1)
    check('QG_J47_HANDLER_MISSING', len(j47) == 2)
    offline = j47[1].split('# J49 has TWO separate positive approvals.', 1)[0]
    check('QG_J47_HEAVY_EXPORT_PREFLIGHT_REGRESSION',
          'Ensure-QwenAsrOnnxRuntimePython' in offline and
          'Ensure-QwenAsrExportPython' not in offline and
          'qwen_onnx_runtime_preflight.py' in gui and
          (root / 'python' / 'qwen_onnx_runtime_preflight.py').is_file())
    for stage in ('J1', 'J2', 'J3', 'J4', 'J5', 'J6', 'J7', 'J8', 'J9', 'J10', 'J11'):
        check('QG_J_STAGE_MISSING', stage in batch)
    check('QG_STAGE_J_COMPILER_GUARD_MISSING', 'compile_submission_allowed' in batch)
    return True


def dependencies_gate(names=('numpy', 'onnx', 'onnxruntime', 'soundfile')):
    missing = [name for name in names if importlib.util.find_spec(name) is None]
    check('QG_PYTHON_DEPENDENCIES_MISSING', not missing)
    return len(names)


def inputs_gate(onnx_dir=None, audio=None, ffmpeg=None, exporter=None, provenance=None):
    if onnx_dir is None:
        return 'not_requested'
    root = Path(onnx_dir)
    check('QG_ONNX_DIRECTORY_MISSING', root.is_dir())
    for name in ('encoder.onnx', 'decoder_init.onnx', 'decoder_step.onnx',
                 'decoder_init.onnx.data', 'decoder_step.onnx.data'):
        path = root / name
        check('QG_REQUIRED_ONNX_FILE_MISSING', path.is_file() and not path.is_symlink())
        check('QG_ONNX_FILE_EMPTY', path.stat().st_size > 0)
    check('QG_RECORDING_NOT_SELECTED', bool(audio))
    recording = Path(audio)
    check('QG_RECORDING_UNAVAILABLE', recording.is_file() and not recording.is_symlink())
    check('QG_RECORDING_UNSUPPORTED', recording.suffix.lower() in ('.ogg', '.wav'))
    for value, code in ((ffmpeg, 'QG_FFMPEG_UNAVAILABLE'),
                        (exporter, 'QG_EXPORTER_UNAVAILABLE'),
                        (provenance, 'QG_PROVENANCE_UNAVAILABLE')):
        check(code, bool(value) and Path(value).exists())
    check('QG_PROVENANCE_INVALID', read_json(Path(provenance)).get('exporter_commit') ==
          '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50')
    return 'verified_file_presence_only'


def postflight_gate(path):
    data = read_json(Path(path))
    latest = data.get('stage') == 'J1+J2+J3+J4+J5+J6+J7+J8+J9+J10+J11'
    extended = latest or data.get('stage') == 'J1+J2+J3+J4+J5+J6+J7+J8+J9'
    check('QG_COMBINED_STAGE_MISMATCH', extended or data.get('stage') == 'J1+J2+J3+J4+J5+J6+J7')
    stages = data.get('stages')
    check('QG_COMBINED_STAGE_MISMATCH', isinstance(stages, list) and
          [item.get('id') for item in stages] == (['J1','J2','J3','J4','J5','J6','J7','J8','J9','J10','J11'] if latest else
           ['J1','J2','J3','J4','J5','J6','J7','J8','J9'] if extended else ['J1','J2','J3','J4','J5','J6','J7']))
    check('QG_CLOUD_OR_PHONE_UNEXPECTED', data.get('execution',{}).get('cloud_jobs') == 0
          and data['execution'].get('phone_changes') == 0
          and data['execution'].get('source_modifications') == 0)
    check('QG_COMPILER_UNLOCKED', all(data.get(name, {}).get('compile_submission_allowed') is False
                                     for name in (('j5','j6','j7','j8','j9','j10','j11') if latest else ('j5','j6','j7','j8','j9') if extended else ('j5','j6','j7'))))
    check('QG_J7_PARITY_NOT_PASSED', data.get('status') ==
          ('offline_j11_delta_parity_pass_compilation_not_started' if latest else 'offline_j9_cache_plan_pass_compilation_not_started' if extended else 'offline_j7_prefill_parity_pass_compilation_not_started') and
          (data['stages'][6].get('result') if extended else data['stages'][-1].get('result')) == 'fp32_prefill_parity_passed' and
          data.get('j7', {}).get('comparison', {}).get('parity_passed') is True and
          data['j7']['comparison'].get('token_exact') is True)
    cmp = data['j7']['comparison']
    check('QG_LOGITS_PARITY_REGRESSION', isinstance(cmp.get('logits_max_abs'), (int,float)) and
          0 <= cmp['logits_max_abs'] <= 2e-4)
    cache = cmp.get('cache_max_abs')
    check('QG_KV_PARITY_REGRESSION', cmp.get('cache_shapes_exact') is True and
          isinstance(cache,dict) and set(cache) == {'keys','values'} and
          all(isinstance(v,(int,float)) and 0 <= v <= 2e-4 for v in cache.values()))
    check('QG_MODEL_MODIFICATION_UNEXPECTED', data['j7'].get('execution',{}).get('original_graph_modified') is False
          and data['j7']['execution'].get('weight_sidecars_modified') is False)
    check('QG_J6_WARNING_LOST', (data.get('j6',{}).get('onnx_checker_passed') is True or
                                 data['j6'].get('onnx_checker_warning_code') == 'J6_ONNX_CHECKER_REJECTED')
          and data['j7'].get('compiler_ready') is False)
    if extended:
        step=data.get('j8',{})
        check('QG_J8_STEP_REPLAY_NOT_PASSED', data['stages'][7].get('result') == 'fp32_original_step_replay_passed'
              and step.get('status') == 'fp32_original_step_contract_and_replay_passed'
              and step.get('comparison',{}).get('repeat',{}).get('repeat_passed') is True
              and step['comparison']['repeat'].get('token_exact') is True
              and step.get('comparison',{}).get('append',{}).get('cache_shapes_exact') is True
              and step.get('compiler_ready') is False)
        plan=data.get('j9',{})
        check('QG_J9_CACHE_SIMULATION_NOT_PASSED', data['stages'][8].get('result') == 'bounded_cache_safety_model_passed'
              and plan.get('capacity_tokens') == 256
              and plan.get('simulation',{}).get('overflow_blocked') is True
              and plan['simulation'].get('cancellation_reset') is True
              and plan.get('npu_resident_kv_implemented') is False)
    if latest:
        rewrite=data.get('j10',{})
        parity=data.get('j11',{})
        comparison=parity.get('comparison',{})
        check('QG_J10_DELTA_GRAPH_INVALID', data['stages'][9].get('result') == 'delta_candidate_created'
              and rewrite.get('source_step_sha256') == data['j8'].get('source_step_sha256')
              and rewrite.get('sidecar_copies') == 0
              and rewrite.get('new_kv_fp32_bytes') == 229376
              and rewrite.get('compiler_ready') is False)
        check('QG_J11_DELTA_PARITY_FAILURE', data['stages'][10].get('result') == 'fp32_delta_step_parity_passed'
              and parity.get('status') == 'fp32_delta_step_parity_passed'
              and comparison.get('parity_passed') is True
              and comparison.get('token_exact') is True
              and all(isinstance(comparison.get(k),(int,float)) and 0 <= comparison[k] <= 2e-4
                  for k in ('logits_max_abs','keys_max_abs','values_max_abs'))
              and parity.get('npu_resident_kv_implemented') is False)
    # Verify the existing shareable archive using a strict field rather than
    # scanning user text (which can produce false positives).
    shared_zip = Path(path).with_suffix('.zip')
    check('QG_DIAGNOSTIC_ZIP_MISSING', shared_zip.is_file())
    with zipfile.ZipFile(shared_zip) as archive:
        check('QG_DIAGNOSTIC_ZIP_CORRUPT', archive.testzip() is None)
        check('QG_DIAGNOSTIC_SUMMARY_MISSING', 'combined-status.json' in archive.namelist())
        safe = json.loads(archive.read('combined-status.json'))
        check('QG_DIAGNOSTIC_PRIVACY_LEAK', not any(k in safe for k in
              ('audio_filenames_local_only', 'local_receipts', 'audio_sha256_local_only')) and
              'weight_identity_local_only' not in safe.get('j7',{}) and
              'audio_sha256_local_only' not in safe.get('j7',{}) and
              'onnx_checker_message_local_only' not in safe.get('j6',{}) and
              'audio_sha256_local_only' not in safe.get('j8',{}) and
              'weight_identity_local_only' not in safe.get('j8',{}))
        check('QG_SHARED_PARITY_MISMATCH', safe.get('j7',{}).get('comparison') == cmp)
    if extended:
        check('QG_SHARED_STEP_MISMATCH', safe.get('j8',{}).get('comparison') == data['j8']['comparison'])
        check('QG_SHARED_CACHE_MISMATCH', safe.get('j9',{}).get('simulation') == data['j9']['simulation'])
    if latest:
        check('QG_SHARED_DELTA_MISMATCH', safe.get('j11',{}).get('comparison') == data['j11']['comparison'])
        check('QG_J10_J11_LOCAL_DATA_LEAK', 'onnx_checker_message_local_only' not in safe.get('j10',{})
              and 'audio_sha256_local_only' not in safe.get('j11',{})
              and 'weight_identity_local_only' not in safe.get('j11',{}))
        return 'j11_delta_fp32_parity_privacy_verified_compiler_locked'
    return 'j7_j9_parity_safety_privacy_verified_compiler_locked' if extended else 'j7_parity_and_privacy_verified_compiler_locked'


def postflight_j13_gate(path):
    """Independent stage J12/J13 safety and diagnostic privacy check."""
    import math
    data=read_json(Path(path))
    check('QG_J13_STAGE_INVALID', data.get('stage')=='J12+J13'
          and data.get('status')=='offline_j13_multistep_parity_pass_compilation_not_started'
          and [x.get('id') for x in data.get('stages',[])]==['J12','J13'])
    execution=data.get('execution',{})
    check('QG_J13_EXTERNAL_SIDE_EFFECT', execution.get('cloud_jobs')==0
          and execution.get('phone_changes')==0 and execution.get('model_modifications')==0)
    j12=data.get('j12',{});j13=data.get('j13',{});cmp=j12.get('comparison',{})
    check('QG_J12_PARITY_FAILED', j12.get('status')=='fp32_multistep_delta_parity_passed'
          and cmp.get('token_exact') is True and cmp.get('cache_append_exact') is True
          and isinstance(cmp.get('steps'),int) and 2<=cmp['steps']<=12
          and isinstance(cmp.get('max_abs'),dict)
          and set(cmp['max_abs'])=={'logits','keys','values'}
          and all(isinstance(v,(int,float)) and math.isfinite(v) and 0<=v<=2e-4
                  for v in cmp['max_abs'].values()))
    count=cmp['steps'];cost=2*28*8*128*4;prefill=171
    check('QG_J13_TRANSFER_PLAN_INVALID',j13.get('status')=='offline_cache_transport_decision_compiler_locked'
          and j13.get('measured_onnx_decoder_steps')==count
          and j13.get('cache_capacity_tokens_provisional')==256
          and j13.get('original_full_kv_input_bytes_fp32_theoretical')==sum((prefill+i)*cost for i in range(count))
          and j13.get('original_full_kv_output_bytes_fp32_theoretical')==sum((prefill+i+1)*cost for i in range(count))
          and j13.get('delta_kv_output_bytes_fp32_theoretical')==count*cost
          and j13.get('new_delta_graph_still_requires_full_kv_input') is True)
    check('QG_J13_COMPILER_UNLOCKED',all(x.get('compile_submission_allowed') is False
          and x.get('compiler_ready') is False for x in (data,j12,j13)))
    path=Path(path).with_suffix('.zip')
    check('QG_J13_ZIP_MISSING',path.is_file())
    with zipfile.ZipFile(path) as archive:
        check('QG_J13_ZIP_CORRUPT',archive.testzip() is None)
        check('QG_J13_ZIP_MEMBERS_UNEXPECTED',set(archive.namelist())=={'stage-j-followup-status.json','README.txt'})
        safe=json.loads(archive.read('stage-j-followup-status.json'))
        check('QG_J13_PRIVATE_DATA_LEAK', not any(key in safe for key in ('prior_receipt_local_only','failure_details_local_only'))
              and not any(key.endswith('_local_only') for key in safe.get('j12',{})))
        check('QG_J13_SHARED_MISMATCH',safe.get('j12',{}).get('comparison')==cmp
              and safe.get('j13',{}).get('original_full_kv_input_bytes_fp32_theoretical')==j13['original_full_kv_input_bytes_fp32_theoretical'])
    return 'j12_multistep_fp32_parity_and_j13_transport_privacy_verified_compiler_locked'



def postflight_j14_gate(path):
    """Independent J14 integrity, locked compiler and private-data checks."""
    from builder_receipt_privacy import contains_local_field, sanitize
    receipt=read_json(Path(path))
    check('QG_J14_STATUS_INVALID',receipt.get('stage')=='J14' and
          receipt.get('status')=='kv_residency_interface_audited_compiler_locked')
    check('QG_J14_TARGET_WRONG',receipt.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J14_COMPILER_UNLOCKED',all(receipt.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','npu_resident_kv_implemented')))
    gate=receipt.get('gates',{})
    check('QG_J14_FALSE_RESIDENCY_CLAIM',gate.get('real_cpu_multistep_parity') is True and
          gate.get('graph_delta_output_shape_audited') is True and
          all(gate.get(k) is False for k in ('verified_device_side_cache_owner',
          'bounded_onnx_update_graph_implemented','qairt_v81_operator_compatibility',
          'fp16_logits_and_tokens_parity','s26_qnn_decoder_execution')))
    io=receipt.get('graph_contract',{})
    check('QG_J14_IO_CONTRACT_INVALID',io.get('full_past_cache_inputs') is True and
          io.get('delta_cache_outputs') is True and io.get('source_weight_data_unmodified') is True and
          set(['past_keys','past_values']).issubset(io.get('input_names',[])) and
          set(['new_keys','new_values']).issubset(io.get('output_names',[])))
    check('QG_J14_OVERFLOW_GUARD_MISSING',receipt.get('bounded_plan',{}).get('capacity_tokens_provisional')==256 and
          receipt.get('bounded_plan',{}).get('overflow_policy')=='fail_closed_then_use_proven_hybrid_without_wrap_or_drop')
    check('QG_J14_EXTERNAL_SIDE_EFFECT',all(receipt.get('execution',{}).get(k)==0 for k in
          ('cloud_jobs','phone_changes','model_modifications')))
    archive=Path(path).with_suffix('.zip')
    check('QG_J14_ZIP_MISSING',archive.is_file())
    with zipfile.ZipFile(archive) as z:
        check('QG_J14_ZIP_CORRUPT',z.testzip() is None)
        check('QG_J14_ZIP_CONTENTS_UNEXPECTED',set(z.namelist())=={'stage-j14-status.json','README.txt'})
        info=z.getinfo('stage-j14-status.json')
        check('QG_J14_ZIP_TOO_LARGE',info.file_size<=MAX_RECEIPT_BYTES)
        safe=json.loads(z.read(info))
    check('QG_J14_PRIVATE_DATA_LEAK',not contains_local_field(safe))
    check('QG_J14_SHARED_REPORT_MISMATCH',safe==sanitize(receipt))
    return 'j14_real_graph_io_audited_residency_not_proven_compiler_locked'



def postflight_j16_gate(path):
    """Independent bounded-host candidate parity, side effects and ZIP privacy."""
    from builder_receipt_privacy import contains_local_field,sanitize
    report=read_json(Path(path))
    check('QG_J16_STATUS_INVALID', report.get('stage')=='J15+J16' and
          report.get('status')=='fp32_bounded_host_interface_parity_passed_compiler_locked')
    check('QG_J16_TARGET_MISMATCH',report.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J16_COMPILER_UNLOCKED',all(report.get(x) is False for x in
          ('compiler_ready','compile_submission_allowed','npu_resident_kv_implemented')))
    check('QG_J16_SIDE_EFFECT',all(report.get('execution',{}).get(k)==0 for k in
          ('cloud_jobs','phone_changes','model_modifications')))
    stage=report.get('j15',{})
    check('QG_J16_HOST_CONTRACT_MISSING',stage.get('host_buffer_shape_each')==[28,1,8,256,128] and
          stage.get('cache_length_dtype')=='INT64' and
          stage.get('dynamic_slice_inside_graph') is True and
          stage.get('full_host_cache_input_still_required') is True and
          stage.get('sidecar_copies')==0 and
          stage.get('npu_resident_kv_implemented') is False and
          stage.get('compile_submission_allowed') is False)
    comparison=report.get('j16',{})
    check('QG_J16_PARITY_INVALID',comparison.get('token_exact') is True and
          comparison.get('cache_append_exact') is True and
          isinstance(comparison.get('steps'),int) and 2<=comparison['steps']<=8)
    errors=comparison.get('max_abs',{})
    check('QG_J16_NUMERICAL_DRIFT',set(errors)=={'logits','keys','values'} and
          all(isinstance(errors[k],(int,float)) and 0<=errors[k]<=2e-4 for k in errors))
    bytes_per_step=2*28*8*256*128*4
    check('QG_J16_BYTES_INVALID',comparison.get('full_bounded_input_bytes_fp32_theoretical')==
          bytes_per_step*comparison['steps'])
    check('QG_J16_CACHE_OVERFLOW',isinstance(comparison.get('peak_valid_tokens'),int) and
          comparison['peak_valid_tokens']<=256)
    archive=Path(path).with_suffix('.zip')
    check('QG_J16_ZIP_MISSING',archive.is_file())
    with zipfile.ZipFile(archive) as zipf:
        check('QG_J16_ZIP_CORRUPT',zipf.testzip() is None)
        check('QG_J16_ZIP_CONTENTS_UNEXPECTED',set(zipf.namelist())=={'stage-j15-j16-status.json','README.txt'})
        info=zipf.getinfo('stage-j15-j16-status.json')
        check('QG_J16_ZIP_TOO_LARGE',info.file_size<=MAX_RECEIPT_BYTES)
        shared=json.loads(zipf.read(info))
    check('QG_J16_PRIVATE_DATA_LEAK',not contains_local_field(shared))
    check('QG_J16_SHARE_MISMATCH',shared==sanitize(report))
    return 'j15_bounded_host_graph_and_j16_fp32_steps_verified_device_residency_unproven_compiler_locked'

def postflight_j19_gate(path):
    """Independent offline QNN ABI & mock cache lifecycle gate; must stay locked."""
    from builder_receipt_privacy import sanitize,contains_local_field
    data=read_json(Path(path))
    check('QG_J19_STATUS_INVALID',data.get('stage')=='J17+J18+J19' and
          data.get('status')=='offline_qnn_interface_prototype_pass_compiler_locked')
    check('QG_J19_TARGET_INVALID',data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J19_COMPILER_UNLOCKED',all(data.get(key) is False for key in
          ('compiler_ready','compile_submission_allowed','npu_resident_kv_implemented','fp16_parity_passed')))
    check('QG_J19_SIDE_EFFECT',all(data.get('execution',{}).get(key)==0 for key in
          ('cloud_jobs','phone_changes','model_modifications')))
    abi=data.get('j17',{}).get('bounded_host_graph',{})
    check('QG_J19_HOST_GRAPH_MISLABELED',abi.get('full_cache_host_input_still_required') is True and
          abi.get('device_resident_aliasing_proved') is False and
          abi.get('host_cache_shape_each')==[28,1,8,256,128] and
          abi.get('cache_length_dtype')=='INT64')
    mock=data.get('j18',{})
    check('QG_J19_MOCK_INVALID',mock.get('mock_registered_buffer_test')=='passed' and
          mock.get('mock_kv_valid_length_test')=='passed' and
          mock.get('mock_overflow_test')=='passed' and
          mock.get('mock_cancel_zeroize_test')=='passed' and
          mock.get('mock_registration_failure_test')=='passed' and
          mock.get('kv_buffer_bytes_fp16')==29360128 and
          mock.get('allocated_buffers_per_session')==1)
    check('QG_J19_FALSE_DEVICE_PROOF',mock.get('native_qnn_registration_executed') is False and
          mock.get('npu_resident_kv_implemented') is False and
          mock.get('full_host_cache_transfer_eliminated') is False and
          mock.get('device_graph_append_implemented') is False and
          mock.get('portable_cpp',{}).get('native_qnn_adapter_verified') is False)
    check('QG_J19_UNSAFE_SUBMISSION',data.get('j19',{}).get('submission_enabled') is False and
          data.get('j19',{}).get('requested_device')=='Samsung Galaxy S26')
    archive=Path(path).with_suffix('.zip')
    check('QG_J19_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J19_ZIP_CORRUPT',z.testzip() is None)
        check('QG_J19_ZIP_CONTENTS_UNEXPECTED',set(z.namelist())=={'stage-j17-j19-status.json','README.txt'})
        check('QG_J19_ZIP_TOO_LARGE',z.getinfo('stage-j17-j19-status.json').file_size<=MAX_RECEIPT_BYTES)
        shared=json.loads(z.read('stage-j17-j19-status.json'))
    check('QG_J19_PRIVATE_DATA_LEAK',not contains_local_field(shared))
    check('QG_J19_SHARED_DATA_MISMATCH',shared==sanitize(data))
    return 'j17_qnn_abi_j18_native_mock_j19_plan_verified_compiler_locked'


def postflight_j21_gate(path):
    """Rehash every tiny model and independently inspect sanitized probe receipts."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J21_STATUS',data.get('stage')=='J20+J21' and
          data.get('status')=='synthetic_operator_cpu_parity_pass_compiler_locked')
    check('QG_J21_TARGET',data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J21_FALSE_DECODER',data.get('decoder_compiler_ready') is False and
          data.get('compile_submission_allowed') is False and data.get('npu_resident_kv_implemented') is False)
    check('QG_J21_SIDE_EFFECTS',data.get('execution')=={'cloud_jobs':0,'phone_changes':0,'model_modifications':0})
    expected=('int64_gather_fp16','fixed_masked_attention_fp16','scatter_elements_fp16')
    check('QG_J21_PROBE_SCHEMA',data.get('probe_contract_revision')==3)
    probes=data.get('j20_j21',{}).get('probes',[])
    check('QG_J21_PROBES',len(probes)==3 and set(p.get('name') for p in probes)==set(expected))
    src=Path(path).parent/(Path(path).stem+'-probes')
    for entry in probes:
        check('QG_J21_NUMERIC_NOT_PASSED',entry.get('onnx_checker_passed') is True and
              entry.get('ort_cpu_parity_passed') is True and entry.get('qnn_compiled') is False and
              entry.get('external_weight_files')==0)
        check('QG_J21_PARITY_CASES',type(entry.get('ort_cpu_parity_cases')) is int and
              entry['ort_cpu_parity_cases']==(3 if entry['name']=='fixed_masked_attention_fp16' else 1))
        check('QG_J21_PARITY_DRIFT',type(entry.get('ort_cpu_max_abs_diff')) in (int,float) and
              0<=entry['ort_cpu_max_abs_diff']<=0.002)
        if entry['name']!='fixed_masked_attention_fp16':
            check('QG_J21_EXACT',entry.get('ort_cpu_exact') is True)
        file=src/(entry['name']+'.onnx')
        check('QG_J21_GRAPH_CHANGED',file.is_file() and not file.is_symlink() and
              file.stat().st_size==entry['bytes'] and 0<file.stat().st_size<128*1024 and
              hashlib.sha256(file.read_bytes()).hexdigest()==entry['sha256'])
    archive=Path(path).with_suffix('.zip')
    check('QG_J21_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J21_ZIP_CORRUPT',z.testzip() is None)
        check('QG_J21_ZIP_CONTENTS',set(z.namelist())=={'stage-j20-j21-status.json','README.txt'}|{'synthetic-probes/'+n+'.onnx' for n in expected})
        public=json.loads(z.read('stage-j20-j21-status.json'))
        check('QG_J21_ZIP_GRAPH_MISMATCH',all(hashlib.sha256(z.read('synthetic-probes/'+e['name']+'.onnx')).hexdigest()==e['sha256'] for e in probes))
    check('QG_J21_PRIVACY',not contains_local_field(public) and public==sanitize(data))
    return 'synthetic_fp16_onnx_operator_probes_cpu_parity_passed_cloud_not_started'

def postflight_j25_gate(path):
    """Read-only real-block FP32 gate. Never qualifies FP16/NPU/cloud."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J25_STAGE',data.get('stage')=='J23+J24+J25' and data.get('target')==
          {'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J25_OFFLINE_ONLY', data.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0})
    check('QG_J25_FALSE_READY',data.get('decoder_compiler_ready') is False and
          data.get('compile_submission_allowed') is False and
          data.get('npu_resident_kv_implemented') is False)
    check('QG_J25_PARITY',data.get('status')=='real_block_fp32_parity_passed_compilation_locked' and
          data.get('j24',{}).get('fp32_parity_passed') is True)
    j23=data.get('j23',{})
    check('QG_J25_REAL_GRAPH', j23.get('real_qwen_graph_nodes') is True and
          j23.get('source_file_mutations')==0 and j23.get('sidecar_copies')==0 and
          type(j23.get('layer0_node_count')) is int and j23['layer0_node_count']>=8 and
          j23.get('candidate_weight_dtype_fp16') is False)
    check('QG_J25_CANDIDATE_HASH',all(isinstance(j23.get(k),str) and
          bool(re.fullmatch('[0-9a-f]{64}',j23[k]))
          for k in ('source_graph_sha256','candidate_graph_sha256')))
    check('QG_J25_BOUNDARIES',bool(j23.get('boundary_inputs')) and
          bool(j23.get('boundary_outputs')))
    j24=data['j24']
    check('QG_J25_FP32_DELTA',isinstance(j24.get('fp32_max_abs'),(int,float)) and
          0<=j24['fp32_max_abs']<=2e-4 and
          j24.get('outputs_compared')==len(j23['boundary_outputs']))
    check('QG_J25_FP16_NOT_MISREPRESENTED',j24.get('fp16_weight_parity_verified') is False and
          j24.get('fp16_qnn_graph_executed') is False and
          j24.get('fp16_boundary_perturbation_only') is True)
    j25=data.get('j25',{})
    check('QG_J25_CLOUD_LOCK',j25.get('status')=='real_block_cloud_compilation_locked' and
          j25.get('cloud_submission_allowed') is False and
          j25.get('npu_resident_kv_implemented') is False and
          j25.get('real_decoder_compiled') is False and
          'MODEL_WEIGHT_UPLOAD_REQUIRES_SEPARATE_APPROVAL' in j25.get('blockers',[]))
    archive=Path(path).with_suffix('.zip')
    check('QG_J25_ZIP_EXISTS',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J25_ZIP_INTEGRITY',z.testzip() is None)
        check('QG_J25_ZIP_NAMES',set(z.namelist())=={'stage-j23-j25-status.json','README.txt'})
        public=json.loads(z.read('stage-j23-j25-status.json'))
    check('QG_J25_REDACTION', public==sanitize(data) and not contains_local_field(public))
    return 'real_qwen_layer_block_fp32_reference_passed_fp16_and_qnn_locked'

def postflight_j28_gate(path):
    """Independent offline FP16 numerical & privacy gate; no cloud or device IO."""
    from builder_receipt_privacy import sanitize, contains_local_field
    import math
    data=read_json(Path(path))
    check('QG_J28_STAGE',data.get('stage')=='J26+J27+J28' and
          data.get('status')=='real_fp16_block_cpu_parity_passed_compilation_locked' and
          data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J28_SIDE_EFFECTS',data.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0})
    check('QG_J28_UNQUALIFIED',data.get('decoder_compiler_ready') is False and
          data.get('compile_submission_allowed') is False and
          data.get('npu_resident_kv_implemented') is False)
    j26=data.get('j26',{});j27=data.get('j27',{});j28=data.get('j28',{})
    check('QG_J26_REAL_FP16',j26.get('checker_passed') is True and
          j26.get('fp16_compute_graph') is True and
          j26.get('original_weight_files_modified') is False and
          j26.get('cloud_submission_allowed') is False and
          j26.get('external_weights_are_private_new_fp16_copy') is True and
          type(j26.get('fp16_weight_tensor_count')) is int and 1<=j26['fp16_weight_tensor_count']<=32 and
          type(j26.get('fp16_weight_bytes')) is int and 0<j26['fp16_weight_bytes']<=512*1024*1024 and
          type(j26.get('source_initializer_count')) is int and 1<=j26['source_initializer_count']<=32 and
          all(type(j26.get(k)) is int and j26[k]>=0 for k in
              ('converted_fp32_weight_tensor_count','existing_fp16_weight_tensor_count',
               'preserved_nonfloat_initializer_count')) and
          j26['converted_fp32_weight_tensor_count']+j26['existing_fp16_weight_tensor_count']==j26['fp16_weight_tensor_count'] and
          j26['fp16_weight_tensor_count']+j26['preserved_nonfloat_initializer_count']==j26['source_initializer_count'])
    check('QG_J26_SHAPE_CONSTANTS_INLINE',
          j26.get('candidate_format_revision')==2 and
          type(j26.get('inline_nonfloat_initializer_count')) is int and
          j26['inline_nonfloat_initializer_count']==j26.get('preserved_nonfloat_initializer_count') and
          type(j26.get('inline_nonfloat_bytes')) is int and
          0<=j26['inline_nonfloat_bytes']<=2*1024*1024)
    check('QG_J26_SHA',all(isinstance(j26.get(k),str) and
          re.fullmatch('[0-9a-f]{64}',j26[k])
          for k in ('source_block_sha256','candidate_graph_sha256','candidate_weight_sha256')))
    check('QG_J27_PARITY',j27.get('fp16_cpu_parity_passed') is True and
          j27.get('fp16_compute_and_weights_tested') is True and
          j27.get('nonzero_kv_tested') is True and
          j27.get('actual_qualcomm_runtime_executed') is False and
          j27.get('real_speech_replay') is False and
          j27.get('positions_tested')==[1,4,8])
    check('QG_J27_SAMPLES',len(j27.get('samples',[]))==3 and
          all(item.get('position')==position and item.get('past_tokens')==position and
          item.get('nonzero_kv') is True and item.get('within_tolerance') is True and
          item.get('mismatched_output_count')==0 and
          type(item.get('outputs_compared')) is int and item['outputs_compared']>0 and
          isinstance(item.get('max_abs'),(int,float)) and math.isfinite(item['max_abs'])
          for item,position in zip(j27['samples'],[1,4,8])))
    check('QG_J28_CLOUD_LOCK',j28.get('status')=='real_fp16_block_compiler_review_locked' and
          j28.get('compile_submission_allowed') is False and
          j28.get('remote_weight_upload_allowed') is False and
          j28.get('actual_qnn_compile_link_completed') is False and
          j28.get('npu_resident_kv_implemented') is False and
          'MODEL_DERIVED_WEIGHTS_REQUIRE_EXPLICIT_SEPARATE_APPROVAL' in j28.get('blockers',[]))
    archive=Path(path).with_suffix('.zip')
    check('QG_J28_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J28_ZIP_INTEGRITY',z.testzip() is None)
        check('QG_J28_ZIP_CONTENTS',set(z.namelist())=={'stage-j26-j28-status.json','README.txt'})
        public=json.loads(z.read('stage-j26-j28-status.json'))
    check('QG_J28_PRIVACY', public==sanitize(data) and not contains_local_field(public))
    return 'real_qwen_fp16_layer_block_cpu_numerics_passed_qnn_and_cloud_locked'

def postflight_j31_gate(path):
    """Independent check of J29-J31 numerical evidence, exact privacy, upload lock."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J31_STAGE',data.get('stage')=='J29+J30+J31' and
          data.get('status')=='real_fp16_rewrite_parity_passed_compilation_locked' and
          data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J31_NO_SIDE_EFFECTS',data.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          data.get('compile_submission_allowed') is False and
          data.get('decoder_compiler_ready') is False and
          data.get('npu_resident_kv_implemented') is False)
    a=data.get('j29',{});b=data.get('j30',{});c=data.get('j31',{})
    check('QG_J29_HASHES', all(isinstance(a.get(k),str) and re.fullmatch('[a-f0-9]{64}',a[k])
          for k in ('source_fp16_graph_sha256','candidate_graph_sha256','weight_sha256')) and
          re.fullmatch(r'layer0\.j29-[a-f0-9]{16}\.onnx',a.get('candidate_graph_basename','')) and
          a.get('checker_passed') is True and a.get('sidecar_copied') is False and
          a.get('original_files_modified') is False)
    check('QG_J29_RISK_ACCOUNTING',type(a.get('rewrite_count')) is int and
          0<=a['rewrite_count']<=a.get('original_risk_node_count',-1) and
          isinstance(a.get('remaining_risk_ops'),dict) and
          all(k in {'Slice','Expand','Range','ScatterND','Trilu'} and
              type(v) is int and v>0 for k,v in a['remaining_risk_ops'].items()) and
          a.get('dynamic_slice_converter_safe') == ('Slice' not in a['remaining_risk_ops']))
    check('QG_J30_PARITY',b.get('status')=='exact_fp16_graph_parity_passed' and
          b.get('parity_passed') is True and b.get('positions_tested')==[1,4,8] and
          len(b.get('samples',[]))==3 and all(x.get('position')==p and
          x.get('past_tokens')==p and x.get('nonzero_kv') is True and
          x.get('exact_fp16_match') is True and x.get('mismatched_output_count')==0 and
          x.get('max_abs')==0.0 and type(x.get('outputs_compared')) is int and
          x['outputs_compared']>0 for p,x in zip([1,4,8],b['samples'])) and
          b.get('qualcomm_runtime_executed') is False and
          b.get('real_speech_replay') is False)
    check('QG_J31_LOCK', c.get('status')=='real_weighted_block_compilation_preparation_only' and
          c.get('remaining_converter_risks')==a.get('remaining_risk_ops') and
          c.get('candidate_graph_sha256')==a.get('candidate_graph_sha256') and
          c.get('model_weight_sha256')==a.get('weight_sha256') and
          c.get('separate_model_weight_approval_required') is True and
          c.get('explicit_cloud_upload_approval_received') is False and
          c.get('remote_weight_upload_allowed') is False and
          c.get('compile_submission_allowed') is False and
          c.get('cloud_jobs')==0 and c.get('actual_qairt_compile_link_completed') is False and
          c.get('real_decoder_qualified') is False and
          c.get('device_resident_kv_implemented') is False and
          'MODEL_WEIGHT_UPLOAD_REQUIRES_SEPARATE_APPROVAL' in c.get('blockers',[]))
    archive=Path(path).with_suffix('.zip')
    check('QG_J31_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J31_ZIP_INTEGRITY',z.testzip() is None)
        check('QG_J31_ZIP_NAMES',set(z.namelist())=={'stage-j29-j31-status.json','README.txt'})
        public=json.loads(z.read('stage-j29-j31-status.json'))
    check('QG_J31_PRIVACY',public==sanitize(data) and not contains_local_field(public))
    return 'real_qwen_fp16_static_graph_rewrite_parity_passed_real_qnn_cloud_locked'

def postflight_j34_gate(path):
    """Independent J32-J34 structural, exact-parity, privacy and upload-lock gate."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J34_STAGE',data.get('stage')=='J32+J33+J34' and
          data.get('status')=='real_block_contracts_fp16_parity_passed_compilation_locked' and
          data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J34_SIDE_EFFECTS',data.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          data.get('decoder_compiler_ready') is False and
          data.get('compile_submission_allowed') is False and
          data.get('npu_resident_kv_implemented') is False)
    a=data.get('j32',{});b=data.get('j33',{});c=data.get('j34',{})
    check('QG_J32_HASHES',all(isinstance(a.get(k),str) and re.fullmatch('[a-f0-9]{64}',a[k])
          for k in ('source_graph_sha256','candidate_graph_sha256','weight_sha256')) and
          a.get('candidate_graph_basename')=='layer0.j32-'+a['source_graph_sha256'][:16]+'.onnx' and
          a.get('checker_passed') is True and a.get('sidecar_copied') is False and
          a.get('original_files_modified') is False)
    nodes=a.get('inspected_nodes',[])
    check('QG_J32_NODE_ACCOUNTING',isinstance(nodes,list) and len(nodes)<=128 and
          type(a.get('risk_node_count')) is int and a['risk_node_count']==len(nodes) and
          type(a.get('rewrites')) is int and
          a['rewrites']==sum(n.get('decision')!='unresolved' for n in nodes if isinstance(n,dict)) and
          all(type(n.get('node_index')) is int and n['node_index']>=0 and
              n.get('op') in ('Slice','Expand','Range','ScatterND','Trilu') and
              n.get('decision') in ('unresolved','identity','gather') and
              (n['decision']!='identity' or n['op']=='Expand') and
              (n['decision']!='gather' or n['op']=='Slice') and
              isinstance(n.get('reason'),str) and len(n['reason'])<80
              for n in nodes) and
          len({n['node_index'] for n in nodes})==len(nodes))
    remain=a.get('remaining_risk_ops',{})
    check('QG_J32_REMAINING_RISKS',isinstance(remain,dict) and
          all(k in ('Slice','Expand','Range','ScatterND','Trilu') and type(v) is int and v>0
              for k,v in remain.items()) and
          remain=={op:sum(n['op']==op and n['decision']=='unresolved' for n in nodes)
                   for op in sorted(remain)} and
          sum(remain.values())+a['rewrites']==len(nodes) and
          a.get('converter_qualification') is False)
    check('QG_J33_PARITY',b.get('status')=='exact_fp16_graph_parity_passed' and
          b.get('parity_passed') is True and b.get('positions_tested')==[1,4,8] and
          len(b.get('samples',[]))==3 and
          all(s.get('position')==p and s.get('past_tokens')==p and
              s.get('nonzero_kv') is True and s.get('outputs_compared')==8 and
              s.get('mismatched_output_count')==0 and s.get('max_abs')==0.0 and
              s.get('exact_fp16_match') is True for p,s in zip([1,4,8],b['samples'])) and
          b.get('qualcomm_runtime_executed') is False and b.get('real_speech_replay') is False)
    check('QG_J34_CLOUD_LOCK',c.get('status')=='decoder_block_qnn_readiness_locked' and
          c.get('target')==data['target'] and
          c.get('exact_fp16_parity_passed') is True and
          c.get('remaining_converter_risks')==remain and
          c.get('candidate_graph_sha256')==a.get('candidate_graph_sha256') and
          c.get('weight_sha256')==a.get('weight_sha256') and
          c.get('cloud_jobs')==0 and
          c.get('model_weight_upload_approved') is False and
          c.get('remote_weight_upload_allowed') is False and
          c.get('compile_submission_allowed') is False and
          c.get('actual_qairt_compile_link_completed') is False and
          c.get('npu_resident_kv_implemented') is False)
    archive=Path(path).with_suffix('.zip')
    check('QG_J34_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J34_ZIP_INTEGRITY',z.testzip() is None)
        check('QG_J34_ZIP_NAMES',set(z.namelist())=={'stage-j32-j34-status.json','README.txt'})
        public=json.loads(z.read('stage-j32-j34-status.json'))
    check('QG_J34_PRIVACY',public==sanitize(data) and not contains_local_field(public))
    return 'real_qwen_decoder_node_contracts_exact_fp16_cpu_parity_passed_qnn_cloud_locked'


def postflight_j37_gate(path):
    """Independent FP16 shape-proof, numerical equality and privacy gate."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J37_STAGE',data.get('stage')=='J35+J36+J37' and
          data.get('status')=='real_fp16_shape_proof_parity_passed_cloud_locked' and
          data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J37_SIDE_EFFECTS',data.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          all(data.get(k) is False for k in ('decoder_compiler_ready','compile_submission_allowed','npu_resident_kv_implemented')))
    a=data.get('j35',{});b=data.get('j36',{});c=data.get('j37',{})
    check('QG_J35_HASHES',all(isinstance(a.get(k),str) and re.fullmatch('[a-f0-9]{64}',a[k])
          for k in ('source_graph_sha256','candidate_graph_sha256','weight_sha256')) and
          a.get('candidate_graph_basename')=='layer0.j35-'+a['source_graph_sha256'][:16]+'.onnx' and
          a.get('checker_passed') is True and a.get('sidecar_copied') is False and
          a.get('original_files_modified') is False and a.get('converter_qualification') is False and
          a.get('tile_qairt_qualified') is False)
    nodes=a.get('inspected_nodes',[])
    check('QG_J35_NODE_PROOFS',isinstance(nodes,list) and len(nodes)<=128 and
          type(a.get('risk_node_count')) is int and a['risk_node_count']==len(nodes) and
          all(isinstance(n,dict) and type(n.get('node_index')) is int and n['node_index']>=0 and
              n.get('op') in ('Slice','Expand') and
              n.get('decision') in ('unchanged','tile','identity') and
              (n['op']!='Slice' or n['decision']=='unchanged') and
              (n['decision']=='unchanged' or n['op']=='Expand') and
              isinstance(n.get('reason'),str) and len(n['reason'])<100 and
              (n['decision']!='tile' or (
                  n.get('reason')=='all_broadcast_axes_symbolically_proven' and
                  isinstance(n.get('repeats'),list) and len(n['repeats'])<=32 and
                  all(type(x) is int and 1<=x<=1024 for x in n['repeats']) and
                  any(x>1 for x in n['repeats']))) for n in nodes) and
          len(set(n['node_index'] for n in nodes))==len(nodes))
    check('QG_J35_COUNTS',a.get('rewrites')==sum(n['decision']!='unchanged' for n in nodes) and
          a.get('rewrite_types')=={'tile_expand':sum(n['decision']=='tile' for n in nodes),
                                   'identity_expand':sum(n['decision']=='identity' for n in nodes)} and
          a.get('constant_slice_count')==sum(n['op']=='Slice' and n.get('parameters_constant') is True for n in nodes) and
          a.get('dynamic_slice_count')==sum(n['op']=='Slice' and n.get('parameters_constant') is False for n in nodes) and
          all(type(n.get('parameters_constant')) is bool for n in nodes if n['op']=='Slice'))
    risks=a.get('remaining_risk_ops',{})
    check('QG_J35_REMAINING',isinstance(risks,dict) and
          all(k in ('Slice','Expand','Tile') and type(v) is int and v>0 for k,v in risks.items()) and
          risks.get('Slice',0)==sum(n['op']=='Slice' for n in nodes) and
          risks.get('Expand',0)==sum(n['op']=='Expand' and n['decision']=='unchanged' for n in nodes) and
          risks.get('Tile',0)>=sum(n['decision']=='tile' for n in nodes))
    check('QG_J36_PARITY',b.get('status')=='exact_fp16_graph_parity_passed' and
          b.get('parity_passed') is True and b.get('positions_tested')==[1,4,8] and
          len(b.get('samples',[]))==3 and
          all(v.get('position')==i and v.get('past_tokens')==i and
              v.get('nonzero_kv') is True and v.get('outputs_compared')==8 and
              v.get('mismatched_output_count')==0 and v.get('max_abs')==0.0 and
              v.get('exact_fp16_match') is True for i,v in zip([1,4,8],b['samples'])) and
          b.get('qualcomm_runtime_executed') is False and b.get('real_speech_replay') is False)
    check('QG_J37_CLOUD_LOCK',c.get('status')=='decoder_block_converter_review_locked' and
          c.get('target')==data['target'] and c.get('exact_fp16_parity_passed') is True and
          c.get('constant_slice_converter_candidate')==a.get('constant_slice_count') and
          c.get('dynamic_slice_converter_risk')==a.get('dynamic_slice_count') and
          c.get('tile_qualcomm_support_verified') is False and
          c.get('remaining_converter_risks')==risks and
          c.get('candidate_graph_sha256')==a.get('candidate_graph_sha256') and
          c.get('weight_sha256')==a.get('weight_sha256') and
          c.get('cloud_jobs')==0 and all(c.get(k) is False for k in
            ('model_weight_upload_approved','remote_weight_upload_allowed','compile_submission_allowed',
             'actual_qairt_compile_link_completed','npu_resident_kv_implemented')))
    archive=Path(path).with_suffix('.zip')
    check('QG_J37_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J37_ZIP_INTEGRITY',z.testzip() is None)
        check('QG_J37_ZIP_NAMES',set(z.namelist())=={'stage-j35-j37-status.json','README.txt'})
        public=json.loads(z.read('stage-j35-j37-status.json'))
    check('QG_J37_PRIVACY',public==sanitize(data) and not contains_local_field(public))
    return 'real_fp16_symbolic_shape_proof_exact_cpu_parity_passed_qualcomm_locked'


def postflight_j40_gate(path):
    """Independent, no-device/no-cloud validation of bounded J38-J40 output."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J40_STAGE',d.get('stage')=='J38+J39+J40' and
          d.get('status')=='real_fp16_expand_provenance_parity_passed_cloud_locked' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J40_SIDE_EFFECTS',d.get('execution')==
          {'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('decoder_compiler_ready','compile_submission_allowed','npu_resident_kv_implemented')))
    a=d.get('j38',{});b=d.get('j39',{});c=d.get('j40',{});plan=d.get('j40_plan',{})
    check('QG_J38_SOURCE_HASHES',all(isinstance(a.get(k),str) and
          re.fullmatch('[a-f0-9]{64}',a[k]) for k in ('source_graph_sha256','candidate_graph_sha256','weight_sha256')) and
          a.get('candidate_graph_basename')=='layer0.j38-'+a['source_graph_sha256'][:16]+'.onnx' and
          a.get('checker_passed') is True and a.get('original_files_modified') is False and
          a.get('sidecar_copied') is False and a.get('converter_qualification') is False and
          a.get('tile_qairt_qualified') is False)
    nodes=a.get('inspected_nodes');traces=a.get('expand_shape_traces')
    check('QG_J38_NODE_COUNTS',isinstance(nodes,list) and len(nodes)<=128 and
          a.get('risk_node_count')==len(nodes) and
          all(type(n.get('node_index')) is int and n['node_index']>=0 and
              n.get('op') in ('Slice','Expand') and n.get('decision') in ('unchanged','tile','identity')
              for n in nodes) and
          a.get('rewrites')==sum(n['decision']!='unchanged' for n in nodes) and
          a.get('rewrite_types')=={
            'tile_expand':sum(n['decision']=='tile' for n in nodes),
            'identity_expand':sum(n['decision']=='identity' for n in nodes)})
    check('QG_J38_TRACE_BOUNDED',isinstance(traces,list) and len(traces)==sum(n['op']=='Expand' for n in nodes) and
          all(type(t.get('node_index')) is int and
              (t.get('input_rank') is None or type(t.get('input_rank')) is int) for t in traces) and
          all(isinstance(t.get('shape_chain'),list) and len(t['shape_chain'])<=40 and
              t.get('proof_complete') in (True,False) and
              t.get('proof_blocker') in ('none','input_dimensions_unresolved','runtime_target_expression_unresolved') and
              all(type(x.get('node_index')) is int and x.get('op') in
                  ('Concat','Gather','Unsqueeze','Squeeze','Reshape','Identity','Cast','Slice',
                   'Shape','Add','Mul','Sub','Div','Constant','ConstantOfShape','Expand') and
                  type(x.get('depth')) is int and 0<=x['depth']<=12 and
                  isinstance(x.get('inputs'),list) and len(x['inputs'])<=12 and
                  type(x.get('shape_constant_inputs')) is int for x in t['shape_chain'])
              for t in traces))
    check('QG_J38_GRAPH_RISKS',isinstance(a.get('remaining_risk_ops'),dict) and
          all(k in ('Slice','Expand','Tile') and type(v) is int and 0<v<=128
              for k,v in a['remaining_risk_ops'].items()) and
          a['remaining_risk_ops'].get('Slice',0)==sum(n['op']=='Slice' for n in nodes) and
          a['remaining_risk_ops'].get('Expand',0)==sum(n['op']=='Expand' and n['decision']=='unchanged' for n in nodes))
    check('QG_J39_PARITY',b.get('status')=='exact_fp16_graph_parity_passed' and
          b.get('parity_passed') is True and b.get('positions_tested')==[1,4,8] and
          len(b.get('samples',[]))==3 and
          all(v.get('position')==pos and v.get('past_tokens')==pos and
              v.get('nonzero_kv') is True and v.get('outputs_compared')==8 and
              v.get('mismatched_output_count')==0 and v.get('max_abs')==0.0 and
              v.get('exact_fp16_match') is True
              for pos,v in zip([1,4,8],b['samples'])) and
          b.get('qualcomm_runtime_executed') is False and b.get('real_speech_replay') is False)
    check('QG_J40_LOCAL_ONLY',c.get('status') in ('not_available','converter_passed','converter_failed','converter_timeout','converter_unavailable') and
          all(c.get(k) is False for k in ('actual_qairt_compile_link_completed','qualcomm_runtime_executed')) and
          c.get('cloud_jobs')==0 and c.get('weight_uploads')==0 and
          c.get('converter_found') is (c['status']!='not_available') and
          c.get('local_conversion_attempted') is (c['status']!='not_available') and
          c.get('local_conversion_passed') is (c['status']=='converter_passed') and
          (c['status']=='not_available' or c.get('failure_category') in
             ('none','QAIRT_RUNTIME_SLICE_INPUT_UNSUPPORTED','QAIRT_EXPAND_UNSUPPORTED',
              'QAIRT_INT64_BOUNDARY_ERROR','QAIRT_UNSUPPORTED_OPERATION',
              'LOCAL_CONVERTER_EXIT_OR_MISSING_DLC','LOCAL_CONVERTER_TIMEOUT','LOCAL_CONVERTER_LAUNCH_FAILED')))
    check('QG_J40_CLOUD_LOCK',plan.get('status')=='real_decoder_local_converter_review_locked' and
          plan.get('target')==d['target'] and plan.get('exact_fp16_parity_passed') is True and
          plan.get('parity_verified_rewrite') is (a['rewrites']>0) and
          plan.get('remaining_converter_risks')==a['remaining_risk_ops'] and
          plan.get('local_converter_status')==c['status'] and
          plan.get('candidate_graph_sha256')==a['candidate_graph_sha256'] and
          plan.get('weight_sha256')==a['weight_sha256'] and plan.get('cloud_jobs')==0 and
          all(plan.get(k) is False for k in
              ('remote_weight_upload_allowed','model_weight_upload_approved','compile_submission_allowed',
               'decoder_compiler_ready','actual_qairt_compile_link_completed','npu_resident_kv_implemented')))
    archive=Path(path).with_suffix('.zip')
    check('QG_J40_ARCHIVE_EXISTS',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J40_ARCHIVE_INTEGRITY',z.testzip() is None)
        check('QG_J40_ARCHIVE_NAMES',set(z.namelist())=={'stage-j38-j40-status.json','README.txt'})
        public=json.loads(z.read('stage-j38-j40-status.json'))
    check('QG_J40_ARCHIVE_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_fp16_expand_shape_provenance_exact_parity_local_converter_status_cloud_locked'


def postflight_j42_gate(path):
    """Independent synthetic graph-only parity evidence; no cloud actions."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path)); a=d.get('j41',{});b=d.get('j42',{})
    check('QG_J42_IDENTITY',d.get('stage')=='J41+J42' and
          d.get('status')=='weight_free_shared_expand_cpu_passed_cloud_unsubmitted' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('probe_contract_revision')==1)
    check('QG_J42_CLOUD_LOCK',d.get('execution')=={'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('decoder_compiler_ready','compile_submission_allowed','npu_resident_kv_implemented')))
    check('QG_J42_PROBE',a.get('status')=='weight_free_structural_probe_created' and
          a.get('graph_basename')=='j41-shared-expand-shape-r1.onnx' and
          isinstance(a.get('graph_sha256'),str) and re.fullmatch('[a-f0-9]{64}',a['graph_sha256']) and
          isinstance(a.get('bytes'),int) and 0<a['bytes']<=131072 and
          a.get('source_provenance',{}).get('synthetic_constants_not_model_derived') is True and
          a.get('source_provenance',{}).get('node_indices')==[46,50] and
          a.get('model_weight_files')==0 and a.get('source_model_files_loaded')==0 and
          a.get('actual_qwen_shape_constants_used') is False and
          a.get('operator_pattern')==['Cast','Cast','Add','Reshape','Concat','Unsqueeze','Expand'])
    cases=b.get('cases',[])
    check('QG_J42_PARITY',b.get('status')=='cpu_structural_probe_exact_passed' and
          b.get('parity_passed') is True and
          b.get('invalid_lengths_rejected') is True and b.get('invalid_batch_rejected') is True and
          b.get('valid_length_bound_exclusive')==256 and
          [x.get('valid_length') for x in cases]==[0,1,4,8,127,255] and
          all(x.get('exact') is True and x.get('max_abs')==0.0 and
              x.get('shape')==[1,1,2,x['valid_length']+1,4] for x in cases) and
          b.get('qualcomm_runtime_executed') is False)
    archive=Path(path).with_suffix('.zip')
    check('QG_J42_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J42_ZIP_VALID',z.testzip() is None and set(z.namelist())=={'j41-j42-status.json','README.txt'})
        public=json.loads(z.read('j41-j42-status.json'))
    check('QG_J42_REDACTED',public==sanitize(d) and not contains_local_field(public))
    return 'shared_expand_weight_free_cpu_parity_passed_cloud_unsubmitted'


def postflight_j43_gate(path):
    """Cloud results may be failed: verify report integrity, not artificial success."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J43_IDENTITY',d.get('stage')=='J43' and d.get('probe_contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J43_FALSE_DEVICE',d.get('decoder_compiler_ready') is False and
          d.get('npu_resident_kv_implemented') is False and
          d.get('real_decoder_qualified') is False and
          d.get('model_derived_weight_upload_allowed') is False)
    ex=d.get('execution',{})
    check('QG_J43_SIDE_EFFECT',ex.get('phone_changes')==0 and ex.get('model_modifications')==0 and
          type(ex.get('cloud_jobs')) is int and 0<=ex['cloud_jobs']<=2)
    cs=d.get('compile_status',{});ls=d.get('link_status',{})
    success=d.get('status')=='synthetic_shared_expand_compile_and_link_verified'
    if success:
        check('QG_J43_VERIFIED_BOTH',ex['cloud_jobs']==2 and
              d.get('cloud_submission_attempted') is True and
              cs.get('verified_success') is True and cs.get('code')=='SUCCESS' and
              ls.get('verified_success') is True and ls.get('code')=='SUCCESS')
    else:
        check('QG_J43_FALSE_SUCCESS',cs.get('verified_success') is not True or
              ls.get('verified_success') is not True)
    archive=Path(path).with_suffix('.zip')
    check('QG_J43_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J43_ZIP_VALID',z.testzip() is None and set(z.namelist())=={'j43-status.json','README.txt'})
        public=json.loads(z.read('j43-status.json'))
    check('QG_J43_REDACTED',public==sanitize(d) and not contains_local_field(public))
    return 'shared_expand_synthetic_qualcomm_compile_link_status_and_privacy_checked'



def postflight_j45_gate(path):
    """Independent fixed-mask FP16 CPU evidence, no real decoder qualification."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path));a=d.get('j44',{});b=d.get('j45',{})
    check('QG_J45_IDENTITY',d.get('stage')=='J44+J45' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('status')=='int64_free_fixed_mask_cpu_passed_cloud_unsubmitted' and
          d.get('probe_contract_revision')==1)
    check('QG_J45_CLOUD_LOCK',d.get('execution')=={'cloud_jobs':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('decoder_compiler_ready','compile_submission_allowed',
                                          'real_decoder_qualified','npu_resident_kv_implemented')))
    check('QG_J45_PROBE',a.get('status')=='fixed_capacity_fp16_mask_probe_created' and
          a.get('graph_basename')=='j44-fixed-capacity-masked-broadcast-r1.onnx' and
          isinstance(a.get('graph_sha256'),str) and re.fullmatch('[a-f0-9]{64}',a['graph_sha256']) and
          type(a.get('bytes')) is int and 0<a['bytes']<=131072 and
          a.get('operator_pattern')==['Mul','Cast','Add','Less','Where'] and
          a.get('no_int64_tensors') is True and a.get('all_shapes_fixed') is True and
          a.get('max_cache_tokens')==256 and a.get('model_weight_files')==0 and
          a.get('source_model_files_loaded')==0 and a.get('not_a_real_decoder_rewrite') is True and
          a.get('source_failure',{}).get('linker_rejected_int64') is True and
          a.get('source_failure',{}).get('compiler_passed') is True)
    cases=b.get('cases',[])
    check('QG_J45_PARITY',b.get('status')=='cpu_masked_prefix_and_padding_exact_passed' and
          b.get('parity_passed') is True and b.get('invalid_lengths_rejected') is True and
          [x.get('valid_length') for x in cases]==[0,1,4,8,127,255] and
          all(x.get('prefix_exact') is True and x.get('padding_zero') is True and
              x.get('output_shape')==[1,1,2,256,4] and x.get('max_abs')==0.0 for x in cases) and
          b.get('qualcomm_runtime_executed') is False)
    archive=Path(path).with_suffix('.zip')
    check('QG_J45_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J45_ZIP_VALID',z.testzip() is None and set(z.namelist())=={'j44-j45-status.json','README.txt'})
        public=json.loads(z.read('j44-j45-status.json'))
    check('QG_J45_REDACTION',public==sanitize(d) and not contains_local_field(public))
    return 'fixed_256_fp16_masked_broadcast_no_int64_cpu_prefix_exact_cloud_locked'


def postflight_j48_gate(path):
    """No real model bytes, paths or automatic uploads in shared J47/J48."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J48_IDENTITY',d.get('stage')=='J47+J48' and
          d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('status')=='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked')
    check('QG_J48_CLOUD_LOCKED',all(d.get(x) is False for x in (
          'decoder_compiler_ready','real_decoder_qualified','npu_resident_kv_implemented',
          'compile_submission_allowed','model_weight_upload_approved')) and
          d.get('execution')=={'cloud_jobs':0,'phone_changes':0,'model_modifications':0})
    a=d.get('j47',{});b=d.get('j48',{})
    check('QG_J47_STAGING_EVIDENCE',a.get('status')=='real_weighted_block_staged_privately' and
          a.get('source_graph_hash_verified') is True and a.get('source_weights_modified') is False and
          a.get('external_weight_count')==11 and
          a.get('synthetic_j46_compile_link_verified') is True and
          a.get('real_decoder_compile_link_verified') is False and
          a.get('real_decoder_rewritten') is False and
          type(a.get('weight_bytes')) is int and 0<a['weight_bytes']<=64*1024*1024 and
          type(a.get('graph_bytes')) is int and 0<a['graph_bytes']<=4*1024*1024 and
          re.fullmatch(r'j47-real-fp16-layer0-[0-9a-f]{16}\.onnx',a.get('package_basename','')) is not None and
          all(re.fullmatch('[0-9a-f]{64}',a.get(k,'')) is not None for k in ('graph_sha256','weight_sha256')))
    check('QG_J48_ACTUAL_BOUNDARY_PROFILE',b.get('status')=='real_layer0_position8_input_profile_passed' and
          b.get('profile_position')==8 and b.get('outputs_checked')==8 and
          b.get('runtime_cpu_executed') is True and b.get('qnn_runtime_executed') is False and
          b.get('staged_graph_vs_source_fp16_outputs_exact') is True and
          b.get('int64_io_truncation_safe') is True and b.get('cloud_compile_approval_separate') is True and
          b.get('converter_risk_ops')=={'Slice':4,'Expand':2} and
          b.get('dynamic_expand_unresolved') is True)
    specs=b.get('input_specs',{})
    check('QG_J48_INPUT_SPECS',isinstance(specs,dict) and len(specs)==8)
    for key,info in specs.items():
        check('QG_J48_INPUT_SPEC_UNSAFE',isinstance(key,str) and len(key)<=128 and
              isinstance(info,dict) and info.get('dtype') in ('float16','float32','int32','int64') and
              isinstance(info.get('shape'),list) and len(info['shape'])<=6 and
              all(type(x) is int and 0<x<=262144 for x in info['shape']))
    archive=Path(path).with_suffix('.zip')
    check('QG_J48_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J48_ZIP_INTEGRITY',z.testzip() is None and set(z.namelist())=={'j47-j48-status.json','README.txt'})
        public=json.loads(z.read('j47-j48-status.json'))
    check('QG_J48_REDACTION',public==sanitize(d) and not contains_local_field(public) and
          not any('\\' in str(x) for x in (public.get('j47',{}).get('package_basename',''),)))
    return 'real_fp16_layer0_private_package_and_measured_input_profile_cloud_locked'


def postflight_j49_gate(path):
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J49_IDENTITY',d.get('stage')=='J49' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J49_NO_RUNTIME_CLAIMS',all(d.get(k) is False for k in
          ('real_decoder_qualified','npu_resident_kv_implemented','decoder_compiler_ready')))
    check('QG_J49_APPROVAL',d.get('model_derived_weight_upload_approved') is True)
    ex=d.get('execution',{})
    check('QG_J49_SIDE_EFFECTS',ex.get('phone_changes')==0 and ex.get('model_modifications')==0 and
          type(ex.get('cloud_jobs')) is int and 0<=ex['cloud_jobs']<=2)
    success=d.get('status')=='real_fp16_layer0_compile_link_verified_runtime_unqualified'
    if success:
        check('QG_J49_SUCCESS_NOT_FORGED',ex['cloud_jobs']==2 and d.get('cloud_submission_attempted') is True and
              d.get('compile_status',{}).get('code')=='SUCCESS' and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('code')=='SUCCESS' and
              d.get('link_status',{}).get('verified_success') is True and
              d.get('weight_bytes',0)>0 and
              all(re.fullmatch('[0-9a-f]{64}',d.get(k,'')) for k in ('graph_sha256','weight_sha256')))
    else:
        check('QG_J49_FAILED_OR_PENDING',d.get('status') in
              ('blocked','compile_failed','link_failed','link_job_missing') and
              not (d.get('compile_status',{}).get('verified_success') is True and
                   d.get('link_status',{}).get('verified_success') is True))
    archive=Path(path).with_suffix('.zip')
    check('QG_J49_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J49_ZIP_INTEGRITY',z.testzip() is None and set(z.namelist())=={'j49-status.json','README.txt'})
        public=json.loads(z.read('j49-status.json'))
    check('QG_J49_REDACTION',public==sanitize(d) and not contains_local_field(public))
    return 'real_fp16_model_derived_weight_upload_explicit_approval_compile_link_verified_separately'


def postflight_j53_gate(path):
    """Independent proof of profile-limited, offline-only real FP16 candidate."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J53_IDENTITY',d.get('stage')=='J51+J52+J53' and
          (d.get('contract_revision')==2 or
           (d.get('contract_revision')==1 and d.get('status')=='blocked')) and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J53_CLOUD_AND_MODEL_LOCK',d.get('execution')=={
          'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in (
          'decoder_compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
          'real_decoder_qualified','npu_resident_kv_implemented')))
    passed=d.get('status')=='position8_specialized_fp16_cpu_parity_passed_cloud_locked'
    check('QG_J53_STATUS',passed or d.get('status')=='blocked')
    if passed:
        a=d.get('j51',{});b=d.get('j52',{});c=d.get('j53',{})
        shape=a.get('target_dimensions',[])
        shapes=a.get('target_dimensions_by_expand',[])
        def bounded_shape(v):
            product=1
            if not isinstance(v,list) or len(v)!=5:return False
            for n in v:
                if type(n) is not int or n<=0 or n>262144 or product>16777216//n:return False
                product*=n
            return True
        check('QG_J53_REAL_GRAPH_PROVENANCE',a.get('candidate_basename')=='layer0.j51-static-expand-pos8-r2.onnx' and
              all(re.fullmatch('[0-9a-f]{64}',a.get(k,'')) for k in
              ('source_graph_sha256','candidate_graph_sha256','weight_sha256')) and
              a.get('source_graph_sha256')!=a.get('candidate_graph_sha256') and
              a.get('expand_rewrites')==2 and a.get('constant_target_rank')==5 and
              bounded_shape(shape) and isinstance(shapes,list) and len(shapes)==2 and
              shape==shapes[0] and all(bounded_shape(v) for v in shapes) and
              a.get('original_files_modified') is False and
              a.get('runtime_shape_source')=='actual_position8_decoder_replay')
        check('QG_J53_EXACT_PROFILE_LIMITED_PARITY',
              b.get('status')=='exact_fp16_same_profile_parity_passed' and
              b.get('position')==8 and b.get('same_profile_cases')==3 and
              b.get('outputs_compared_per_case')==8 and
              b.get('maximum_absolute_difference')==0.0 and
              b.get('shape_changes_allowed') is False and
              b.get('general_cache_lengths_qualified') is False and
              b.get('real_speech_replay') is False)
        check('QG_J53_NO_FALSE_COMPILATION',c.get('status')=='position8_specialized_candidate_cloud_locked' and
              c.get('fatal_j49_error')=='dynamic_expand_shape_unsupported' and
              c.get('actual_qualcomm_compile_link_completed') is False and
              c.get('model_weight_upload_approved') is False and
              c.get('specialization_requires_exact_input_profile') is True)
    archive=Path(path).with_suffix('.zip')
    check('QG_J53_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J53_ZIP_INTEGRITY',z.testzip() is None and set(z.namelist())=={'j51-j53-status.json','README.txt'})
        public=json.loads(z.read('j51-j53-status.json'))
    check('QG_J53_REDACTION',public==sanitize(d) and not contains_local_field(public))
    return 'position8_only_real_fp16_expand_specialization_cpu_exact_parity_cloud_locked'



def postflight_j56_gate(path):
    """Independent receipt gate; never promotes static parity into compiler success."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d = read_json(Path(path))
    check('QG_J56_IDENTITY', d.get('stage') == 'J54+J55+J56' and
          d.get('contract_revision') == 1 and
          d.get('target') == {'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J56_LOCKS', d.get('execution') == {
          'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in (
              'compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
              'real_decoder_qualified','npu_resident_kv_implemented')))
    passed = d.get('status') == 'position8_real_graph_structurally_audited_cloud_locked'
    check('QG_J56_STATUS', passed or d.get('status') == 'blocked')
    if passed:
        a,b,c = d.get('j54',{}), d.get('j55',{}), d.get('j56',{})
        check('QG_J56_REAL_ARTIFACT_IDENTITY',
              a.get('status') == 'verified_real_static_expand_artifact' and
              a.get('candidate_basename') == 'layer0.j51-static-expand-pos8-r2.onnx' and
              a.get('position') == 8 and
              a.get('source_profile') == 'real_j51_position8' and
              a.get('exact_j52_parity_previously_verified') is True and
              type(a.get('graph_size_bytes')) is int and 0 < a['graph_size_bytes'] <= 4194304 and
              type(a.get('weight_size_bytes')) is int and 0 < a['weight_size_bytes'] <= 67108864 and
              all(isinstance(a.get(k),str) and re.fullmatch('[0-9a-f]{64}',a[k]) for k in
                  ('source_graph_sha256','candidate_graph_sha256','weight_sha256')) and
              a['source_graph_sha256'] != a['candidate_graph_sha256'])
        check('QG_J56_STRUCTURAL_PROOF',
              b.get('status') == 'verified_two_edge_mutation_and_liveness' and
              b.get('expand_edges_checked') == 2 and
              b.get('static_target_shape') == [1,8,2,9,128] and
              b.get('new_constant_count') in (1,2) and
              type(b.get('new_targets_identical')) is bool and
              b.get('new_targets_identical') is (b['new_constant_count'] == 1) and
              b.get('model_boundaries_unchanged') is True and
              b.get('source_graph_exactly_recoverable') is True and
              isinstance(b.get('original_shape_target_live_to_output'),list) and
              len(b['original_shape_target_live_to_output']) == 2 and
              all(type(x) is bool for x in b['original_shape_target_live_to_output']) and
              b.get('dynamic_shape_may_remain_in_public_output') is any(b['original_shape_target_live_to_output']) and
              isinstance(b.get('candidate_operator_counts'),dict) and
              b['candidate_operator_counts'].get('Expand') == 2 and
              all(type(v) is int and 0 <= v <= 80 for v in b['candidate_operator_counts'].values()) and
              sum(b['candidate_operator_counts'].values()) <= 80 and
              b.get('previous_j49_fatal') == 'dynamic_expand_shape_unsupported' and
              b.get('reciprocal_version_warning_remains_unresolved') is True and
              b.get('local_qairt_conversion_performed') is False and
              b.get('converter_compatibility_proven') is False and
              b.get('general_positions_qualified') is False)
        check('QG_J56_NO_CLOUD_APPROVAL',
              c.get('status') == 'privacy_verified_offline_cloud_locked' and
              c.get('next_step') == 'separately_approve_one_exact_profile_real_weight_qualcomm_compile_link_trial' and
              all(c.get(k) is False for k in ('cloud_cost_permission_granted',
                    'model_weight_upload_permission_granted','original_weight_files_modified',
                    'compile_and_link_success_claimed')))
    archive = Path(path).with_suffix('.zip')
    check('QG_J56_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J56_ZIP_INTEGRITY',z.testzip() is None and
              set(z.namelist()) == {'j54-j56-status.json','README.txt'})
        public = json.loads(z.read('j54-j56-status.json'))
    check('QG_J56_REDACTION',public == sanitize(d) and not contains_local_field(public))
    return 'actual_position8_onnx_two_edge_delta_and_output_liveness_cloud_locked'

def postflight_j57_gate(path):
    """Independent offline receipt proof; cannot grant cloud consent."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J57_IDENTITY',d.get('stage')=='J57' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J57_LOCKS',d.get('execution')=={
          'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in (
              'compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
              'real_decoder_qualified','npu_resident_kv_implemented')))
    passed=d.get('status')=='position8_verified_private_upload_package_cloud_locked'
    check('QG_J57_STATUS',passed or d.get('status')=='blocked')
    if passed:
        a,b,c=d.get('j57',{}),d.get('j58',{}),d.get('j59',{})
        check('QG_J57_GRAPH_PROVENANCE',
              a.get('status')=='real_position8_compile_package_locally_verified' and
              a.get('package_basename')=='j57-static-pos8-ee96300b7217ff90.onnx' and
              a.get('onnx_basename')=='layer0.fp16.onnx' and
              a.get('weight_basename')=='layer0.fp16.data' and
              a.get('graph_sha256')=='ee96300b7217ff9056e01f76570a576023b886b914b0e60f335e3f7b9424b6df' and
              a.get('weight_sha256')=='944192ed2fb3ee58b779d863f05fc73cbde873a12f4f18581ffc6d8cb4761b12' and
              a.get('position')==8 and a.get('expand_shape')==[1,8,2,9,128] and
              type(a.get('graph_bytes')) is int and 0<a['graph_bytes']<4194304 and
              type(a.get('weight_bytes')) is int and 0<a['weight_bytes']<67108864 and
              a.get('source_models_unchanged') is True and
              a.get('dynamic_output_liveness_risk') is True and
              a.get('reciprocal_opset_risk_unresolved') is True and
              a.get('source_output_count')==8 and
              a.get('action') in ('created_isolated_private_package','reused_verified_private_package'))
        specs=a.get('input_specs',{})
        check('QG_J57_INPUT_PROFILE',isinstance(specs,dict) and len(specs)==8 and
              all(isinstance(k,str) and len(k)<=128 and isinstance(v,dict) and
                  v.get('dtype') in ('float16','float32','int32','int64') and
                  isinstance(v.get('shape'),list) and 0<=len(v['shape'])<=6 and
                  all(type(n) is int and 0<n<=262144 for n in v['shape'])
                  for k,v in specs.items()))
        check('QG_J57_NO_CLOUD',b.get('status')=='requires_new_model_weight_upload_and_paid_cloud_approvals' and
              b.get('consent_granted') is False and b.get('cost_known') is False and
              b.get('cloud_jobs_submitted')==0 and
              c=={'status':'not_compiled_or_linked','device_execution_tested':False})
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J57_ZIP',z.testzip() is None and set(z.namelist())=={'j57-status.json','README.txt'})
        public=json.loads(z.read('j57-status.json'))
    check('QG_J57_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_position8_profiled_private_upload_package_approved_cloud_locked'


def postflight_j59_gate(path):
    """Validate failed or successful REAL compile/link separately; no device claim."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J59_IDENTITY',d.get('stage')=='J58+J59' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('paid_cloud_approved_for_this_job') is True)
    check('QG_J59_NO_DEVICE_CLAIMS',all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed',
           'real_decoder_qualified','npu_resident_kv_implemented')))
    e=d.get('execution',{})
    check('QG_J59_EFFECT_BOUND',e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          type(e.get('cloud_jobs')) is int and 0<=e['cloud_jobs']<=2 and
          type(e.get('weight_uploads')) is int and 0<=e['weight_uploads']<=1)
    success=d.get('status')=='position8_real_fp16_layer0_compile_link_verified_runtime_unqualified'
    check('QG_J59_STATUS',success or d.get('status') in
          ('blocked','compile_failed','link_failed','link_job_missing'))
    if d.get('cloud_submission_attempted'):
        check('QG_J59_GRAPH',d.get('graph_sha256')=='ee96300b7217ff9056e01f76570a576023b886b914b0e60f335e3f7b9424b6df' and
              d.get('weight_sha256')=='944192ed2fb3ee58b779d863f05fc73cbde873a12f4f18581ffc6d8cb4761b12' and
              d.get('position8_only') is True and
              d.get('dynamic_public_output_risk_open') is True and
              d.get('reciprocal_opset_risk_open') is True)
    if success:
        check('QG_J59_REAL_BOTH_FINAL',d.get('cloud_submission_attempted') is True and
              e['cloud_jobs']==2 and e['weight_uploads']==1 and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('compile_status',{}).get('code')=='SUCCESS' and
              d.get('link_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('code')=='SUCCESS')
    else:
        check('QG_J59_NO_FORGED_PASS',not (d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J59_ZIP',z.testzip() is None and set(z.namelist())=={'j58-j59-status.json','README.txt'})
        public=json.loads(z.read('j58-j59-status.json'))
    check('QG_J59_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'new_paid_model_weight_approval_compile_and_link_results_no_runtime_claims'



def postflight_j69_gate(path):
    """Independent offline rank-zero scalar Add acceptance + privacy gate."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j69_j71 import PREVIOUS_GRAPH_SHA,EXPECTED_WEIGHT_SHA,NEW_FILE
    d=read_json(Path(path))
    check('QG_J69_IDENTITY',d.get('stage')=='J69' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J69_CLOUD_LOCK',d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,
          'phone_changes':0,'model_modifications':0} and all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
           'real_decoder_qualified','npu_resident_kv_implemented')))
    passed=d.get('status')=='position8_integer_scalar_add_cpu_parity_passed_cloud_locked'
    check('QG_J69_STATUS',passed or d.get('status')=='blocked')
    if passed:
        a=d.get('j69',{});f=d.get('j69_failure',{});inventory=a.get('scalar_add_inventory')
        count=a.get('scalar_adds_frozen')
        check('QG_J69_PROVENANCE',f.get('compile_job_id')=='j5wyyqj4g' and
              f.get('link_job_id')=='jg9oow6mg' and f.get('fatal_node')=='node_add_250' and
              f.get('error_class')=='rank_zero_add_validation' and
              isinstance(f.get('link_log_sha256'),str) and
              re.fullmatch(r'[0-9a-f]{64}',f['link_log_sha256']) is not None and
              a.get('source_graph_sha256')==PREVIOUS_GRAPH_SHA and
              a.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              a.get('weight_bytes')==12587526 and
              a.get('candidate_basename')==NEW_FILE and
              isinstance(a.get('candidate_graph_sha256'),str) and
              re.fullmatch(r'[a-f0-9]{64}',a['candidate_graph_sha256']) is not None and
              a['candidate_graph_sha256']!=PREVIOUS_GRAPH_SHA)
        check('QG_J69_BOUNDED_ADD',type(count) is int and 1<=count<=12 and
              type(a.get('total_add_nodes')) is int and count<=a['total_add_nodes']<=32 and
              isinstance(inventory,list) and len(inventory)==a['total_add_nodes'] and
              all(isinstance(i,dict) and type(i.get('index')) is int and
                  type(i.get('rank')) is int and i.get('dtype') in
                  ('float16','float32','float64','int32','int64') and
                  type(i.get('scalar_integer')) is bool and
                  type(i.get('name_matches_linker')) is bool for i in inventory) and
              sum(i['scalar_integer'] for i in inventory)==count and
              a.get('mapping_method') in ('exact_source_node_name',
                  'converter_node_name_not_source_confirmed'))
        check('QG_J69_PARITY',a.get('status')=='rank_zero_integer_adds_frozen_three_case_exact_parity' and
              a.get('position')==8 and a.get('same_profile_cases')==3 and
              a.get('outputs_compared_per_case')==8 and
              a.get('max_absolute_difference')==0.0 and
              a.get('source_output_count')==8 and
              a.get('original_source_unchanged') is True and
              a.get('general_positions_qualified') is False and
              a.get('qa_rt_link_compatibility_proven') is False)
        specs=a.get('input_specs')
        check('QG_J69_PROFILE',isinstance(specs,dict) and len(specs)==8 and
              all(isinstance(v,dict) and v.get('dtype') in
                  ('float16','float32','int64','int32') and isinstance(v.get('shape'),list) and
                  len(v['shape'])<=6 and all(type(z) is int and 0<z<=262144 for z in v['shape'])
                  for v in specs.values()))
        name=a.get('private_package_basename')
        check('QG_J69_PRIVATE',isinstance(name,str) and
              re.fullmatch(r'j69-scalar-add-pos8-[a-f0-9]{16}\.onnx',name) and
              name.endswith(a['candidate_graph_sha256'][:16]+'.onnx') and
              a.get('candidate_action') in ('created_new_candidate','reused_verified_candidate') and
              a.get('private_package_action') in
                 ('created_verified_private_package','reused_verified_private_package'))
    else:
        check('QG_J69_NO_FALSE_PASS',d.get('j69',{}).get('status')!=
              'rank_zero_integer_adds_frozen_three_case_exact_parity')
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J69_ZIP',z.testzip() is None and set(z.namelist())=={'j69-status.json','README.txt'})
        public=json.loads(z.read('j69-status.json'))
    check('QG_J69_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_position8_rankzero_integer_add_exact_parity_cloud_locked'


def postflight_j71_gate(path):
    """Separate new approved Qualcomm compile/link outcomes, no runtime claims."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j69_j71 import PREVIOUS_GRAPH_SHA,EXPECTED_WEIGHT_SHA
    d=read_json(Path(path))
    check('QG_J71_IDENTITY',d.get('stage')=='J70+J71' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('cloud_compute_approved_for_this_job') is True)
    check('QG_J71_NO_RUNTIME',all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    e=d.get('execution',{})
    check('QG_J71_EFFECTS',e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          type(e.get('cloud_jobs')) is int and 0<=e['cloud_jobs']<=2 and
          type(e.get('weight_uploads')) is int and 0<=e['weight_uploads']<=1)
    success=d.get('status')=='position8_scalar_add_real_fp16_compile_link_passed_runtime_unqualified'
    check('QG_J71_STATUS',success or d.get('status') in
          ('blocked','compile_failed','link_failed','link_job_missing'))
    if d.get('cloud_submission_attempted'):
        check('QG_J71_GRAPH',isinstance(d.get('graph_sha256'),str) and
              re.fullmatch(r'[a-f0-9]{64}',d['graph_sha256']) and
              d['graph_sha256']!=PREVIOUS_GRAPH_SHA and
              d.get('previous_graph_sha256')==PREVIOUS_GRAPH_SHA and
              d.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              d.get('weight_bytes')==12587526 and
              d.get('source_profile_position')==8 and d.get('position8_only') is True)
    if success:
        check('QG_J71_FINAL',d.get('cloud_submission_attempted') is True and
              e['cloud_jobs']==2 and e['weight_uploads']==1 and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True and
              d.get('compile_status',{}).get('code')=='SUCCESS' and
              d.get('link_status',{}).get('code')=='SUCCESS')
    else:
        check('QG_J71_NO_FALSE_SUCCESS',not
              (d.get('compile_status',{}).get('verified_success') is True and
               d.get('link_status',{}).get('verified_success') is True))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J71_ZIP',z.testzip() is None and set(z.namelist())=={'j70-j71-status.json','README.txt'})
        public=json.loads(z.read('j70-j71-status.json'))
    check('QG_J71_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'new_consent_qualcomm_rankzero_add_compile_link_no_runtime_claim'



def postflight_j72_gate(path):
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j72_j73 import GRAPH_SHA, LINK_JOB, COMPILE_JOB
    d=read_json(Path(path))
    check('QG_J72_IDENTITY',d.get('stage')=='J72' and d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('compile_job_id')==COMPILE_JOB and d.get('link_job_id')==LINK_JOB and d.get('position8_only') is True and
          d.get('real_decoder_qualified') is False and d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0})
    success=d.get('status')=='linked_qnn_context_downloaded_private_runtime_unqualified'
    check('QG_J72_STATUS',success or d.get('status')=='blocked')
    if success:
        check('QG_J72_HASH',d.get('graph_sha256')==GRAPH_SHA and isinstance(d.get('context_sha256'),str) and
              re.fullmatch('[a-f0-9]{64}',d['context_sha256']) and
              type(d.get('context_bytes')) is int and 4096<=d['context_bytes']<=268435456 and
              d.get('context_basename')=='qwen-layer0-position8-v81.bin' and
              isinstance(d.get('input_specs'),list) and len(d['input_specs'])==8 and
              isinstance(d.get('output_specs'),list) and len(d['output_specs'])==8)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J72_ZIP',z.testzip() is None and set(z.namelist())=={'j72-status.json','README.txt'})
        public=json.loads(z.read('j72-status.json'))
    check('QG_J72_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'existing_successful_link_job_download_no_compile_no_phone_changes'


def postflight_j73_gate(path):
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j72_j73 import GRAPH_SHA
    d=read_json(Path(path))
    e=d.get('execution',{})
    check('QG_J73_SCOPE',d.get('stage')=='J73' and d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('position8_only') is True and d.get('real_decoder_qualified') is False and
          d.get('npu_resident_kv_implemented') is False and
          e.get('cloud_jobs')==e.get('weight_uploads')==e.get('model_modifications')==0 and
          e.get('phone_changes') in (0,1))
    check('QG_J73_CLEANUP',e.get('phone_changes')==0 or d.get('device_staging_removed') is True)
    success=d.get('status')=='position8_single_layer_s26_htp_provisional_numeric_pass_not_full_decoder'
    check('QG_J73_STATUS',success or d.get('status')=='blocked')
    if success:
        a=d.get('j73',{})
        check('QG_J73_EXEC',d.get('device_verified') is True and d.get('device_staging_removed') is True and
              a.get('graph_sha256')==GRAPH_SHA and a.get('qnn_graph_calls')==1 and
              a.get('requested_backend')=='libQnnHtp.so' and a.get('cpu_backend_requested') is False and
              a.get('same_profile_position')==8 and a.get('output_count')==8 and a.get('provisional_numeric_pass') is True and
              len(a.get('metrics',[]))==8 and type(a.get('device_run_wall_ms_including_adb')) in (int,float))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J73_ZIP',z.testzip() is None and set(z.namelist())=={'j73-status.json','README.txt'})
        public=json.loads(z.read('j73-status.json'))
    check('QG_J73_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_single_layer_htp_fp16_numeric_screen_or_blocked_cleanup_and_privacy'

def postflight_j75_gate(path):
    """J75 is an offline original-weight two-layer graph fidelity gate, NOT QNN."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j75_j76 import STATUS, BASELINE_SHA
    d=read_json(Path(path))
    e=d.get('execution',{})
    check('QG_J75_CONTRACT',d.get('stage')=='J75' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          all(d.get(x) is False for x in ('compiler_ready','compile_submission_allowed',
                'model_weight_upload_allowed','real_decoder_qualified','npu_resident_kv_implemented')) and
          e=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0})
    success=d.get('status')==STATUS
    check('QG_J75_STATUS',success or d.get('status')=='blocked')
    if success:
        a=d.get('j75',{});b=d.get('j76',{})
        bench=a.get('hardware_baseline',{});cache=a.get('kv_transfer_budget',{})
        parity=b.get('cpu_parity',{})
        check('QG_J75_PROVENANCE',bench.get('prior_graph_sha256')==BASELINE_SHA and
              bench.get('full_decoder_speed_projection_valid') is False and
              cache.get('fp16_all_layers_kv_bytes_per_token')==114688 and
              cache.get('persistent_npu_residency_implemented') is False and
              cache.get('real_qnn_buffer_registration_tested') is False)
        check('QG_J75_FUSION',b.get('fused_layer_indices')==[0,1] and
              type(b.get('fused_node_count')) is int and b['fused_node_count']>=60 and
              type(b.get('external_weight_count')) is int and b['external_weight_count']>=8 and
              b.get('no_weight_copy') is True and b.get('no_original_model_modifications') is True and
              b.get('converter_ready') is False and b.get('fp16_weights_generated') is False and
              b.get('fp16_candidate',{}).get('fp16_compute_graph') is True and
              b.get('fp16_candidate',{}).get('source_model_weights_untouched') is True and
              b.get('fp16_candidate',{}).get('cloud_submission_allowed') is False and
              re.fullmatch(r'[0-9a-f]{64}',b.get('fp16_candidate',{}).get('candidate_graph_sha256','')) is not None and
              re.fullmatch(r'[0-9a-f]{64}',b.get('fp16_candidate',{}).get('candidate_weight_sha256','')) is not None and
              re.fullmatch(r'[0-9a-f]{64}',b.get('candidate_graph_sha256','')) is not None and
              re.fullmatch(r'[0-9a-f]{64}',b.get('source_graph_sha256','')) is not None)
        cases=parity.get('cases',[])
        check('QG_J75_PARITY',parity.get('test_positions')==[1,4,8] and
              parity.get('nonzero_kv_inputs') is True and parity.get('all_cases_passed') is True and
              parity.get('fp16_npu_numerical_parity') is False and
              parity.get('native_qnn_execution') is False and
              parity.get('fp16_all_cases_passed') is True and
              parity.get('fp16_atol')==0.15 and parity.get('fp16_rtol')==0.10 and
              len(parity.get('fp16_cases',[]))==3 and
              all(c.get('passed') is True and c.get('outputs_compared')==b['boundary_output_count']
                  for c in parity['fp16_cases']) and len(cases)==3 and all(c.get('passed') is True and
                  c.get('boundary_outputs_compared')==b['boundary_output_count'] and
                  type(c.get('maximum_absolute_difference')) in (float,int) and
                  0<=c['maximum_absolute_difference']<0.5 for c in cases))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J75_ZIP',z.testzip() is None and set(z.namelist())=={'j75-status.json','README.txt'})
        public=json.loads(z.read('j75-status.json'))
    check('QG_J75_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_two_layer_fp32_reference_parity_or_blocked_offline_privacy'



def postflight_j77_gate(path):
    """No cloud work or general-position assertions; check actual exact FP16 output parity."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j77_j79 import STATUS
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J77_SCOPE',d.get('stage')=='J77' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          e=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('compiler_ready','compile_submission_allowed',
              'model_weight_upload_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    success=d.get('status')==STATUS
    check('QG_J77_STATUS',success or d.get('status')=='blocked')
    if success:
        j=d.get('j77',{})
        check('QG_J77_GRAPH',j.get('boundary_input_count')==10 and
              j.get('boundary_output_count')==10 and j.get('sample_position')==8 and
              j.get('tested_same_profile_cases')==3 and
              j.get('max_fp16_specialization_difference')==0.0 and
              j.get('remaining_dynamic_expand_reshape_inputs')==0 and
              j.get('source_graph_unchanged') is True and
              j.get('original_weights_unchanged') is True and
              j.get('not_general_positions') is True and
              j.get('qnn_compile_unverified') is True and
              j.get('npu_device_execution_unverified') is True and
              re.fullmatch(r'[0-9a-f]{64}',j.get('candidate_graph_sha256','')) is not None and
              re.fullmatch(r'[0-9a-f]{64}',j.get('candidate_weight_sha256','')) is not None)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J77_ZIP',z.testzip() is None and set(z.namelist())=={'j77-status.json','README.txt'})
        public=json.loads(z.read('j77-status.json'))
    check('QG_J77_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'fused01_exact_fp16_position8_shape_closure_or_blocked_offline_privacy'



def postflight_j80_gate(path):
    """J80: strict offline QNN input contract and sanitizer verification."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j77_j79 import J80_STATUS
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J80_SCOPE',d.get('stage')=='J80' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          e=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('compiler_ready','compile_submission_allowed',
              'model_weight_upload_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    success=d.get('status')==J80_STATUS
    check('QG_J80_STATUS',success or d.get('status')=='blocked')
    if success:
        j=d.get('j80',{});p=j.get('input_specs',{})
        check('QG_J80_PROFILE',len(p)==10 and p.get('sym_size_int_180')==
              {'shape':[],'dtype':'int64'} and p.get('val_0')==
              {'shape':[1],'dtype':'int64'} and
              j.get('corrected_shapes')==[{'input':'sym_size_int_180',
                                          'old_shape':[1],'onnx_shape':[]}] and
              j.get('source_j77_exact_fp16_parity') is True and
              j.get('model_graph_or_weights_modified') is False and
              j.get('qualcomm_scalar_input_support_unverified') is True and
              j.get('qnn_compile_unverified') is True and
              j.get('position8_only') is True and
              re.fullmatch('[0-9a-f]{64}',j.get('candidate_graph_sha256','')) is not None and
              re.fullmatch('[0-9a-f]{64}',j.get('candidate_weight_sha256','')) is not None)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J80_ZIP',z.testzip() is None and set(z.namelist())=={'j80-status.json','README.txt'})
        public=json.loads(z.read('j80-status.json'))
    check('QG_J80_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'onnx_declared_scalar_input_profile_corrected_offline_or_blocked'



def postflight_j81_gate(path):
    """Independent J81 parity, exact-node scope and private-receipt sanitization."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j81 import STATUS, SOURCE_SHA, NODE_NAME
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J81_SCOPE',d.get('stage')=='J81' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          e=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('compiler_ready','compile_submission_allowed',
            'model_weight_upload_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    success=d.get('status')==STATUS
    check('QG_J81_STATUS',success or d.get('status')=='blocked')
    if success:
        j=d.get('j81',{})
        check('QG_J81_PARITY',j.get('source_graph_sha256')==SOURCE_SHA and
              re.fullmatch(r'[a-f0-9]{64}',j.get('candidate_graph_sha256','')) is not None and
              re.fullmatch(r'[a-f0-9]{64}',j.get('weight_sha256','')) is not None and
              j.get('changed_source_node')==NODE_NAME and
              j.get('original_op_type')=='Add' and
              j.get('replacement_op_type')=='Constant' and
              j.get('rank_zero_int64_constant')==9 and
              j.get('sample_position')==8 and j.get('tested_same_profile_cases')==3 and
              j.get('boundary_output_count')==10 and
              j.get('max_absolute_fp16_difference')==0.0 and
              j.get('original_source_untouched') is True and
              j.get('new_cloud_submissions_allowed') is False and
              j.get('qualcomm_compile_unverified') is True and
              j.get('qualcomm_link_unverified') is True and
              j.get('s26_execution_unverified') is True and
              j.get('position8_only') is True)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J81_ZIP',z.testzip() is None and set(z.namelist())=={'j81-status.json','README.txt'})
        public=json.loads(z.read('j81-status.json'))
    check('QG_J81_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'fused01_position8_exact_scalar_add_or_blocked_offline_cloud_locked'

def postflight_j83_gate(path):
    """J83: independent one-shot cloud receipt audit (not an NPU device proof)."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j82_j83 import STATUS,EXPECTED_GRAPH_SHA,EXPECTED_WEIGHT_SHA
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J83_SCOPE',d.get('stage')=='J82+J83' and
          d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('real_decoder_qualified') is False and
          d.get('npu_resident_kv_implemented') is False and
          d.get('position8_only') is True and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('cloud_compute_approved_for_this_job') is True and
          e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          e.get('cloud_jobs') in (0,1,2) and e.get('weight_uploads') in (0,1))
    check('QG_J83_STATUS',d.get('status') in ('blocked','compile_failed','link_failed',
          'link_job_missing','submission_pending_manual_status_review',STATUS))
    if d.get('status')==STATUS:
        check('QG_J83_SUCCESS',e.get('cloud_jobs')==2 and e.get('weight_uploads')==1 and
              d.get('graph_sha256')==EXPECTED_GRAPH_SHA and
              d.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              d.get('rank0_scalar_add_replaced') is True and
              d.get('verified_scalar_input_rank')==0 and
              d.get('source_j81_exact_fp16_parity') is True and
              d.get('fused_layer_indices')==[0,1] and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J83_ZIP',z.testzip() is None and
              set(z.namelist())=={'j82-j83-status.json','README.txt'})
        public=json.loads(z.read('j82-j83-status.json'))
    check('QG_J83_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'one_shot_j81_fused_compile_link_status_no_device_or_kv_claims'


def postflight_j84_gate(path):
    """Qualcomm existing-context download; success only if hashed private binary verified."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j84_j85 import DOWNLOAD_STATUS,EXPECTED_GRAPH_SHA,LINK_JOB,CONTEXT_FILE
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J84_SCOPE',d.get('stage')=='J84' and d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('position8_only') is True and d.get('real_decoder_qualified') is False and
          d.get('npu_resident_kv_implemented') is False and
          e.get('cloud_jobs')==e.get('weight_uploads')==e.get('phone_changes')==e.get('model_modifications')==0)
    check('QG_J84_STATUS',d.get('status') in ('blocked',DOWNLOAD_STATUS))
    if d.get('status')==DOWNLOAD_STATUS:
        check('QG_J84_LINK',d.get('link_job_id')==LINK_JOB and
              d.get('graph_sha256')==EXPECTED_GRAPH_SHA and
              d.get('context_basename')==CONTEXT_FILE and
              isinstance(d.get('context_sha256'),str) and
              re.fullmatch('[a-f0-9]{64}',d['context_sha256']) and
              isinstance(d.get('context_bytes'),int) and
              4096<=d['context_bytes']<=512*1024*1024 and
              len(d.get('input_specs',[]))==len(d.get('output_specs',[]))==10 and
              next((i['shape'] for i in d['input_specs'] if i['name']=='sym_size_int_180'),None)==[])
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J84_ZIP',z.testzip() is None and
              set(z.namelist())=={'j84-status.json','README.txt'})
        public=json.loads(z.read('j84-status.json'))
    check('QG_J84_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'existing_linked_context_private_no_cloud_jobs_or_device_claims'


def postflight_j85_gate(path):
    """Physical-S26 fixed-position benchmark: require 48*10 native parity calls."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j84_j85 import DEVICE_STATUS,EXPECTED_GRAPH_SHA,COUNTS
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J85_SCOPE',d.get('stage')=='J85' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('position8_only') is True and d.get('real_decoder_qualified') is False and
          d.get('npu_resident_kv_implemented') is False and
          e.get('cloud_jobs')==e.get('weight_uploads')==e.get('model_modifications')==0 and
          e.get('phone_changes') in (0,1))
    check('QG_J85_STATUS',d.get('status') in ('blocked',DEVICE_STATUS))
    if d.get('status')==DEVICE_STATUS:
        x=d.get('j85',{});perf=x.get('performance',{});runs=x.get('runs',{})
        check('QG_J85_COMPLETE',d.get('device_verified') is True and
              d.get('device_staging_removed') is True and
              x.get('graph_sha256')==EXPECTED_GRAPH_SHA and
              x.get('fused_layer_indices')==[0,1] and
              x.get('qnn_graph_calls')==48 and
              x.get('tested_positions')==[8] and
              x.get('same_position8_inputs_repeated') is True and
              x.get('all_48_calls_ten_outputs_provisional_pass') is True and
              x.get('all_integer_outputs_exact') is True and
              x.get('npu_resident_kv') is False and
              x.get('full_asr_speedup_verified') is False and
              x.get('warm_kernel_timing_isolated') is False and
              perf.get('full_decoder_speed_projection_valid') is False and
              perf.get('actual_full_decoder_benchmark') is False and
              len(runs)==2 and all(runs.get(str(n),{}).get('qnn_graph_calls')==n and
              runs.get(str(n),{}).get('all_ten_outputs_provisional_pass') is True
              for n in COUNTS))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J85_ZIP',z.testzip() is None and
              set(z.namelist())=={'j85-status.json','README.txt'})
        public=json.loads(z.read('j85-status.json'))
    check('QG_J85_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return '48_fused01_s26_position8_graph_calls_all_ten_outputs_provisional_check'


def postflight_j86_gate(path):
    """Read-only original CPU token-step tuning; NEVER claim Android speedup."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j86 import STATUS
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J86_SCOPE',d.get('stage')=='J86' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('real_decoder_qualified') is False and
          d.get('android_runtime_modified') is False and
          d.get('cloud_submission_allowed') is False and
          all(e.get(k)==0 for k in ('cloud_jobs','weight_uploads','phone_changes','model_modifications')))
    check('QG_J86_STATUS',d.get('status') in ('blocked',STATUS))
    if d.get('status')==STATUS:
        x=d.get('j86',{});ref=d.get('reference',{})
        check('QG_J86_BASELINE',ref.get('recording_replayed_in_j86') is False and
              ref.get('parakeet_comparable_end_to_end_measurement_available') is False and
              ref.get('npu_kernel_time_isolated') is False and
              .01<ref.get('s26_j74_position8_single_layer_qnn_incremental_ms',-1)<1000 and
              .01<ref.get('s26_j85_position8_fused_layers01_qnn_incremental_ms',-1)<1000)
        check('QG_J86_METHOD',x.get('model')=='original_fp32_28_layer_decoder_step' and
              x.get('host')=='windows_builder_cpu_not_android' and
              x.get('settings_applied_to_android') is False and
              x.get('synthetic_profile_only') is True and
              x.get('real_transcription_speedup_verified') is False and
              x.get('no_parakeet_performance_equivalence_claim') is True)
        for name,threads in (('reference_threads_2',2),('experiment_threads_4',4)):
            block=x.get(name,{})
            check('QG_J86_THREADS',block.get('threads')==threads and
                  set(block.get('positions',{}))=={'8','64'})
            for item in block['positions'].values():
                check('QG_J86_METRICS',isinstance(item.get('median_ms'),(int,float)) and
                      .001<item['median_ms']<3600000 and
                      len(item.get('samples_ms',[]))==3 and
                      item.get('synthetic_zero_kv') is True and
                      len(item.get('max_fp32_abs_diffs',[]))==3)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J86_ZIP',z.testzip() is None and
              set(z.namelist())=={'j86-status.json','README.txt'})
        public=json.loads(z.read('j86-status.json'))
    check('QG_J86_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'windows_cpu_step_tuning_only_no_app_or_phone_change'


def postflight_j79_gate(path):
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j77_j79 import CLOUD_STATUS
    d=read_json(Path(path));e=d.get('execution',{})
    check('QG_J79_SCOPE',d.get('stage')=='J78+J79' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('real_decoder_qualified') is False and d.get('npu_resident_kv_implemented') is False and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('cloud_compute_approved_for_this_job') is True and
          e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          e.get('cloud_jobs') in (0,1,2) and e.get('weight_uploads') in (0,1))
    check('QG_J79_STATUS',d.get('status') in ('blocked','compile_failed','link_failed',
          'link_job_missing','submission_pending_manual_status_review',CLOUD_STATUS))
    if d.get('status')==CLOUD_STATUS:
        check('QG_J79_SUCCESS',e.get('cloud_jobs')==2 and e.get('weight_uploads')==1 and
              d.get('fused_layer_indices')==[0,1] and d.get('position8_only') is True and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True)
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J79_ZIP',z.testzip() is None and set(z.namelist())=={'j78-j79-status.json','README.txt'})
        public=json.loads(z.read('j78-j79-status.json'))
    check('QG_J79_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'fused01_cloud_compile_link_status_and_privacy_no_phone_claims'


def postflight_j74_gate(path):
    """Independent, source-only hardware benchmark scope and privacy audit."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j72_j73 import GRAPH_SHA
    d=read_json(Path(path)); e=d.get('execution',{})
    check('QG_J74_SCOPE', d.get('stage')=='J74' and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('position8_only') is True and d.get('real_decoder_qualified') is False and
          d.get('npu_resident_kv_implemented') is False and
          e.get('cloud_jobs')==e.get('weight_uploads')==e.get('model_modifications')==0 and
          e.get('phone_changes') in (0,1))
    check('QG_J74_CLEANUP',e.get('phone_changes')==0 or d.get('device_staging_removed') is True)
    success=d.get('status')=='repeated_position8_s26_htp_benchmark_and_sampled_parity_pass_not_full_decoder'
    check('QG_J74_STATUS',success or d.get('status')=='blocked')
    if success:
        x=d.get('j74',{});p=x.get('performance',{});runs=x.get('benchmark_runs',{})
        check('QG_J74_EXEC',d.get('device_verified') is True and d.get('device_staging_removed') is True and
              x.get('graph_sha256')==GRAPH_SHA and x.get('requested_backend')=='libQnnHtp.so' and
              x.get('cpu_backend_requested') is False and x.get('qnn_graph_calls')==48 and
              x.get('position')==8 and x.get('profile_content_repeated_identically') is True and
              x.get('all_sampled_outputs_pass_provisional_tolerance') is True and
              x.get('warm_npu_kernel_latency_verified') is False and
              x.get('general_positions_qualified') is False and set(runs)=={'8','40'} and
              all(runs[str(n)].get('graph_calls')==n and
                  runs[str(n)].get('all_outputs_provisional_numeric_pass') is True and
                  runs[str(n)].get('integer_outputs_exact') is True for n in (8,40)))
        check('QG_J74_LIMITS',p.get('recurrent_kv_used') is False and
              p.get('full_decoder_speed_projection_valid') is False and
              p.get('profiles_tested')==[8] and
              p.get('timing_scope','').startswith('device wall clock:') and
              type(p.get('run_8_device_wall_ms_including_context_and_io')) in (int,float) and
              type(p.get('run_40_device_wall_ms_including_context_and_io')) in (int,float))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J74_ZIP',z.testzip() is None and set(z.namelist())=={'j74-status.json','README.txt'})
        public=json.loads(z.read('j74-status.json'))
    check('QG_J74_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'same_profile_repeated_htp_wall_time_provisional_parity_cleanup_privacy_not_kv'

def postflight_j66_gate(path):
    """Independently audit all-Reshape offline numerical/safety receipt."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j63_j65 import NEW_SHA as J61_SHA
    from qwen3_asr_stage_j57_j59 import EXPECTED_WEIGHT_SHA
    d=read_json(Path(path))
    check('QG_J66_IDENTITY',d.get('stage')=='J66' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J66_CLOUD_LOCK', d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,
          'phone_changes':0,'model_modifications':0} and all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
           'real_decoder_qualified','npu_resident_kv_implemented')))
    passed=d.get('status')=='position8_all_dynamic_reshape_edges_cpu_parity_passed_cloud_locked'
    check('QG_J66_STATUS',passed or d.get('status')=='blocked')
    if passed:
        x=d.get('j66',{});f=d.get('j66_failure',{})
        n=x.get('edges_rewritten');shapes=x.get('measured_shapes',[])
        check('QG_J66_PROVENANCE',f.get('compile_job_id')=='jprx0m30p' and
              f.get('fatal_node')=='node__unsafe_view_1' and
              f.get('fatal_operator')=='Reshape' and
              isinstance(f.get('compiler_log_sha256'),str) and
              re.fullmatch(r'[0-9a-f]{64}',f['compiler_log_sha256']) is not None and
              x.get('source_graph_sha256')==J61_SHA and
              x.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              x.get('weight_bytes')==12587526 and
              x.get('candidate_basename')=='layer0.j66-all-static-reshape-pos8-r1.onnx' and
              isinstance(x.get('candidate_graph_sha256'),str) and
              re.fullmatch(r'[0-9a-f]{64}',x['candidate_graph_sha256']) is not None and
              x['candidate_graph_sha256']!=J61_SHA)
        check('QG_J66_RESHAPE_COVERAGE',type(n) is int and 1<=n<=8 and
              type(x.get('total_reshape_nodes')) is int and n<=x['total_reshape_nodes']<=80 and
              x.get('remaining_dynamic_reshape_edges')==0 and
              isinstance(shapes,list) and len(shapes)==n and all(
                  isinstance(shape,list) and 1<=len(shape)<=8 and
                  all(type(v) is int and -1<=v<=262144 for v in shape) and
                  shape.count(-1)<=1 for shape in shapes))
        check('QG_J66_PARITY',x.get('status')=='all_remaining_dynamic_reshape_edges_frozen_and_exact_parity' and
              x.get('position')==8 and x.get('same_profile_cases')==3 and
              x.get('outputs_compared_per_case')==8 and x.get('max_absolute_difference')==0.0 and
              x.get('source_output_count')==8 and x.get('original_source_unchanged') is True and
              x.get('general_positions_qualified') is False and
              x.get('qa_rt_compatibility_proven') is False and
              x.get('reciprocal_opset_risk_open') is True)
        profile=x.get('input_specs')
        check('QG_J66_PROFILE',isinstance(profile,dict) and len(profile)==8 and all(
              isinstance(k,str) and 0<len(k)<=128 and isinstance(v,dict) and
              v.get('dtype') in ('float16','float32','int64','int32') and
              isinstance(v.get('shape'),list) and len(v['shape'])<=6 and
              all(type(z) is int and 0<z<=262144 for z in v['shape'])
              for k,v in profile.items()))
        check('QG_J66_PRIVATE_PACKAGE',isinstance(x.get('private_package_basename'),str) and
              re.fullmatch(r'j66-all-reshape-pos8-[0-9a-f]{16}\.onnx',x['private_package_basename']) and
              x['private_package_basename'].split('-')[-1]==x['candidate_graph_sha256'][:16]+'.onnx' and
              x.get('candidate_action') in ('created_new_candidate','reused_verified_candidate') and
              x.get('private_package_action') in ('created_verified_private_package','reused_verified_private_package'))
    else:
        check('QG_J66_NO_FALSE_PASS',d.get('j66',{}).get('max_absolute_difference') != 0.0 or
              d.get('j66',{}).get('status')!='all_remaining_dynamic_reshape_edges_frozen_and_exact_parity')
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J66_ZIP',z.testzip() is None and set(z.namelist())=={'j66-status.json','README.txt'})
        public=json.loads(z.read('j66-status.json'))
    check('QG_J66_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'real_position8_all_reshape_three_case_exact_parity_cloud_locked'


def postflight_j68_gate(path):
    """Separate authorized cloud result; compile/link must each finish successfully."""
    from builder_receipt_privacy import sanitize, contains_local_field
    from qwen3_asr_stage_j63_j65 import NEW_SHA as J61_SHA
    from qwen3_asr_stage_j57_j59 import EXPECTED_WEIGHT_SHA
    d=read_json(Path(path))
    check('QG_J68_IDENTITY',d.get('stage')=='J67+J68' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('cloud_compute_approved_for_this_job') is True)
    check('QG_J68_NO_RUNTIME_CLAIMS',all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    e=d.get('execution',{})
    check('QG_J68_BOUND_EFFECTS',e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          type(e.get('cloud_jobs')) is int and 0<=e['cloud_jobs']<=2 and
          type(e.get('weight_uploads')) is int and 0<=e['weight_uploads']<=1)
    success=d.get('status')=='position8_all_reshape_real_fp16_compile_link_passed_runtime_unqualified'
    check('QG_J68_STATUS',success or d.get('status') in ('blocked','compile_failed','link_failed','link_job_missing'))
    if d.get('cloud_submission_attempted'):
        check('QG_J68_GRAPH',isinstance(d.get('graph_sha256'),str) and
              re.fullmatch(r'[0-9a-f]{64}',d['graph_sha256']) and
              d['graph_sha256']!=J61_SHA and
              d.get('previous_graph_sha256')==J61_SHA and
              d.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              d.get('weight_bytes')==12587526 and
              d.get('source_profile_position')==8 and d.get('position8_only') is True)
    if success:
        check('QG_J68_FINAL_STATES',d.get('cloud_submission_attempted') is True and
              e['cloud_jobs']==2 and e['weight_uploads']==1 and
              d.get('compile_status',{}).get('code')=='SUCCESS' and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('code')=='SUCCESS' and
              d.get('link_status',{}).get('verified_success') is True)
    else:
        check('QG_J68_NO_FALSE_SUCCESS',not (
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J68_ZIP',z.testzip() is None and set(z.namelist())=={'j67-j68-status.json','README.txt'})
        public=json.loads(z.read('j67-j68-status.json'))
    check('QG_J68_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'new_graph_separate_consent_qualcomm_compile_link_no_runtime_claim'

def postflight_j63_gate(path):
    """Independently validate offline private package provenance and privacy."""
    from builder_receipt_privacy import sanitize,contains_local_field
    d=read_json(Path(path))
    check('QG_J63_IDENTITY',d.get('stage')=='J63' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J63_NO_NETWORK',d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,
          'phone_changes':0,'model_modifications':0} and all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
           'real_decoder_qualified','npu_resident_kv_implemented')))
    success=d.get('status')=='j61_position8_candidate_privately_staged_cloud_locked'
    check('QG_J63_STATUS',success or d.get('status')=='blocked')
    if success:
        from qwen3_asr_stage_j63_j65 import NEW_SHA,PRIOR_SHA,EXPECTED_WEIGHT_SHA,NAME
        stage=d.get('j63',{}); c=d.get('j64',{}); nxt=d.get('j65',{})
        specs=stage.get('input_specs')
        check('QG_J63_REAL_GRAPH',stage.get('status')=='j61_position8_candidate_staged_privately' and
              stage.get('graph_sha256')==NEW_SHA and stage.get('previous_graph_sha256')==PRIOR_SHA and
              stage.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              stage.get('package_basename')==NAME and
              stage.get('onnx_basename')=='layer0.fp16.onnx' and
              stage.get('weight_basename')=='layer0.fp16.data' and
              type(stage.get('graph_bytes')) is int and 0<stage['graph_bytes']<=10000000 and
              stage.get('weight_bytes')==12587526 and stage.get('position')==8 and
              stage.get('expand_shape')==[1,8,2,9,128] and
              stage.get('reshape_shape')==[1,16,9,128] and
              stage.get('source_output_count')==8 and
              stage.get('dynamic_public_output_risk_open') is True and
              stage.get('reciprocal_opset_risk_open') is True and
              stage.get('source_models_unchanged') is True and
              stage.get('action') in ('created_isolated_private_package','reused_verified_private_package'))
        check('QG_J63_PROFILE',isinstance(specs,dict) and len(specs)==8 and all(
              isinstance(k,str) and 0<len(k)<=128 and isinstance(v,dict) and
              v.get('dtype') in ('float16','float32','int32','int64') and
              isinstance(v.get('shape'),list) and len(v['shape'])<=6 and
              all(type(n) is int and 0<n<=262144 for n in v['shape']) for k,v in specs.items()))
        check('QG_J63_CLOUD_LOCKED',c.get('status')=='separate_fresh_upload_and_cloud_compute_consent_required' and
              c.get('consent_granted') is False and c.get('cloud_jobs_submitted')==0 and
              nxt.get('status')=='not_compiled_or_linked' and nxt.get('device_execution_tested') is False)
    else:
        check('QG_J63_NO_FALSE_POSITIVE',not d.get('j63',{}).get('status')=='j61_position8_candidate_staged_privately')
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J63_SAFE_ZIP',z.testzip() is None and set(z.namelist())=={'j63-status.json','README.txt'})
        public=json.loads(z.read('j63-status.json'))
    check('QG_J63_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'j61_verified_local_private_package_cloud_locked'


def postflight_j65_gate(path):
    """Report compile/link independently; do not infer HTP runtime qualification."""
    from builder_receipt_privacy import sanitize,contains_local_field
    from qwen3_asr_stage_j63_j65 import NEW_SHA,PRIOR_SHA,EXPECTED_WEIGHT_SHA
    d=read_json(Path(path))
    check('QG_J65_IDENTITY',d.get('stage')=='J64+J65' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('model_derived_weight_upload_approved_for_this_job') is True and
          d.get('cloud_compute_approved_for_this_job') is True)
    check('QG_J65_NO_DEVICE_CLAIMS',all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','real_decoder_qualified','npu_resident_kv_implemented')))
    e=d.get('execution',{})
    check('QG_J65_EFFECT_LIMIT',e.get('phone_changes')==0 and e.get('model_modifications')==0 and
          type(e.get('cloud_jobs')) is int and 0<=e['cloud_jobs']<=2 and
          type(e.get('weight_uploads')) is int and 0<=e['weight_uploads']<=1)
    success=d.get('status')=='j61_position8_real_fp16_layer_compile_link_verified_runtime_unqualified'
    check('QG_J65_STATUS',success or d.get('status') in
          ('blocked','compile_failed','link_failed','link_job_missing'))
    if d.get('cloud_submission_attempted'):
        check('QG_J65_PROFILE',d.get('graph_sha256')==NEW_SHA and
              d.get('previous_graph_sha256')==PRIOR_SHA and
              d.get('weight_sha256')==EXPECTED_WEIGHT_SHA and
              d.get('weight_bytes')==12587526 and
              d.get('source_profile_position')==8 and d.get('position8_only') is True and
              d.get('dynamic_public_output_risk_open') is True and
              d.get('reciprocal_opset_risk_open') is True)
    if success:
        check('QG_J65_FINAL_COMPILE_LINK',d.get('cloud_submission_attempted') is True and
              e['cloud_jobs']==2 and e['weight_uploads']==1 and
              d.get('compile_status',{}).get('code')=='SUCCESS' and
              d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('code')=='SUCCESS' and
              d.get('link_status',{}).get('verified_success') is True)
    else:
        check('QG_J65_NOT_FALSE_SUCCESS',not (d.get('compile_status',{}).get('verified_success') is True and
              d.get('link_status',{}).get('verified_success') is True))
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J65_SAFE_ZIP',z.testzip() is None and
              set(z.namelist())=={'j64-j65-status.json','README.txt'})
        public=json.loads(z.read('j64-j65-status.json'))
    check('QG_J65_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'new_distinct_graph_consent_compile_and_link_status_no_runtime_claims'


def postflight_j62_gate(path):
    """Offline parity/identity gate; no QNN, general-position or upload claim."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J62_IDENTITY',d.get('stage')=='J60+J61+J62' and
          d.get('contract_revision')==2 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J62_NO_CLOUD', d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,
          'phone_changes':0,'model_modifications':0} and all(d.get(k) is False for k in
          ('compiler_ready','compile_submission_allowed','model_weight_upload_allowed',
           'real_decoder_qualified','npu_resident_kv_implemented')))
    passed=d.get('status')=='position8_reshape_boundary_cpu_parity_passed_cloud_locked'
    check('QG_J62_STATUS',passed or d.get('status')=='blocked')
    if passed:
        j60,j61,j62=d.get('j60',{}),d.get('j61',{}),d.get('j62',{})
        shape=j61.get('measured_reshape_shape')
        check('QG_J62_REAL_FATAL',j60.get('status')=='dynamic_reshape_target_confirmed' and
              j60.get('compile_job_id')=='jp3o9rxmp' and
              j60.get('fatal_node')=='node__unsafe_view' and
              j60.get('dynamic_shape_tensor')=='output_7' and
              j60.get('prior_compile_status')=='FAILED' and
              j60.get('qa_rt_version')=='2.50.0')
        check('QG_J62_SOURCE_SHA',j61.get('status')=='position8_unsafe_view_shape_edge_frozen' and
              j61.get('source_graph_sha256')=='ee96300b7217ff9056e01f76570a576023b886b914b0e60f335e3f7b9424b6df' and
              j61.get('weight_sha256')=='944192ed2fb3ee58b779d863f05fc73cbde873a12f4f18581ffc6d8cb4761b12' and
              isinstance(j61.get('candidate_graph_sha256'),str) and
              re.fullmatch('[a-f0-9]{64}',j61['candidate_graph_sha256']) is not None and
              j61['candidate_graph_sha256']!=j61['source_graph_sha256'] and
              j61.get('candidate_basename')=='layer0.j60-static-reshape-pos8-r2.onnx' and
              j61.get('edges_rewritten')==1 and j61.get('output_count_preserved')==8 and
              j61.get('original_source_unchanged') is True and
              j61.get('position')==8 and
              isinstance(j61.get('shape_input'),str) and 0<len(j61['shape_input'])<=192 and
              j61.get('log_shape_tensor')=='output_7' and
              j61.get('source_shape_matches_logged_tensor') is (j61['shape_input']=='output_7') and
              j61.get('discovery_method') in ('exact_fatal_source_node_name',
                  'unique_original_shape_input_fallback') and
              j61.get('matched_source_node_name') is (j61['discovery_method']=='exact_fatal_source_node_name') and
              (j61['matched_source_node_name'] or j61['shape_input']=='output_7') and
              j61.get('target_operator')=='Reshape' and
              isinstance(shape,list) and 1<=len(shape)<=8 and
              j61.get('measured_shape_rank')==len(shape) and
              all(type(n) is int and -1<=n<=262144 for n in shape) and
              shape.count(-1)<=1)
        check('QG_J62_PARITY',j62.get('status')=='three_case_exact_fp16_parity_and_offline_safety_passed' and
              j62.get('same_profile_cases')==3 and j62.get('outputs_compared_per_case')==8 and
              j62.get('maximum_absolute_difference')==0.0 and
              j62.get('general_positions_qualified') is False and
              j62.get('qa_rt_converter_compatibility_proven') is False)
    else:
        inventory=d.get('j61_discovery')
        if inventory is not None:
            check('QG_J62_BOUND_DIAGNOSTIC', isinstance(inventory,dict) and
                  type(inventory.get('reshape_count')) is int and 0<=inventory['reshape_count']<=80 and
                  isinstance(inventory.get('reshape_edges'),list) and
                  len(inventory['reshape_edges'])==inventory['reshape_count'] and
                  all(set(x)=={'index','node_name_matches_log','shape_name_matches_log',
                       'shape_origin','shape_producer_op','shape_edge_count'} and
                      type(x['index']) is int and 0<=x['index']<=80 and
                      type(x['node_name_matches_log']) is bool and
                      type(x['shape_name_matches_log']) is bool and
                      x['shape_origin'] in ('initializer','produced','graph_input','missing') and
                      isinstance(x['shape_producer_op'],str) and len(x['shape_producer_op'])<=40 and
                      type(x['shape_edge_count']) is int and 0<=x['shape_edge_count']<=8
                      for x in inventory['reshape_edges']))
        check('QG_J62_NO_FORGED_PASS',not d.get('j62',{}).get('maximum_absolute_difference')==0.0 or
              d.get('j62',{}).get('status')!='three_case_exact_fp16_parity_and_offline_safety_passed')
    with zipfile.ZipFile(Path(path).with_suffix('.zip')) as z:
        check('QG_J62_ZIP',z.testzip() is None and
              set(z.namelist())=={'j60-j62-status.json','README.txt'})
        public=json.loads(z.read('j60-j62-status.json'))
    check('QG_J62_PRIVACY',public==sanitize(d) and not contains_local_field(public))
    return 'j58_fatal_reshape_specific_measured_fp16_parity_or_blocked_cloud_locked'


def postflight_j50_gate(path):
    """Validate read-only compiler diagnosis and prevent fabricated success."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J50_IDENTITY',d.get('stage')=='J50' and d.get('contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'} and
          d.get('original_stage')=='J49' and d.get('original_compile_status')=='FAILED')
    check('QG_J50_READ_ONLY',d.get('execution')=={'cloud_jobs':0,'weight_uploads':0,'phone_changes':0,'model_modifications':0} and
          all(d.get(k) is False for k in ('compiler_ready','real_decoder_qualified','npu_resident_kv_implemented',
                                          'compile_submission_allowed','model_weight_upload_approved_for_new_job')))
    check('QG_J50_PROVENANCE',bool(re.fullmatch(r'j[a-z0-9]{8,16}',d.get('compile_job_id',''))) and
          all(re.fullmatch('[0-9a-f]{64}',d.get(k,'')) for k in ('graph_sha256','weight_sha256')) and
          d.get('failure_category') in (
          'not_available','dynamic_slice_requires_constant_parameters','int64_link_boundary_unsupported',
          'int64_io_option_required','expand_dynamic_shape_unsupported','static_shape_required',
          'unsupported_operator','fp16_converter_type_rejected','shape_inference_failed',
          'converter_out_of_memory','unclassified_converter_failure'))
    success=d.get('status')=='compiler_log_diagnosed'
    check('QG_J50_STATUS_CONSISTENT',d.get('status') in ('compiler_log_diagnosed','compiler_log_unavailable') and
          (not success or (d.get('failure_category')!='not_available' and
                            bool(re.fullmatch('[0-9a-f]{64}',d.get('log_sha256',''))) and
                            type(d.get('log_bytes')) is int and 0<d['log_bytes']<=8*1024*1024)) and
          (success or d.get('failure_category')=='not_available'))
    archive=Path(path).with_suffix('.zip')
    check('QG_J50_ZIP_MISSING',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J50_ZIP_INTEGRITY',z.testzip() is None and set(z.namelist())=={'j50-status.json','README.txt'})
        public=json.loads(z.read('j50-status.json'))
    check('QG_J50_REDACTION',public==sanitize(d) and not contains_local_field(public))
    return 'read_only_existing_real_qwen_compile_log_categorized_no_new_cloud_job'

def postflight_j46_gate(path):
    """Verify cloud compile AND link status separately, including failures."""
    from builder_receipt_privacy import sanitize, contains_local_field
    d=read_json(Path(path))
    check('QG_J46_IDENTITY',d.get('stage')=='J46' and d.get('probe_contract_revision')==1 and
          d.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J46_NO_DEVICE_CLAIMS',all(d.get(k) is False for k in (
          'decoder_compiler_ready','npu_resident_kv_implemented','real_decoder_qualified',
          'model_derived_weight_upload_allowed')))
    ex=d.get('execution',{})
    check('QG_J46_SIDE_EFFECT',ex.get('phone_changes')==0 and ex.get('model_modifications')==0 and
          type(ex.get('cloud_jobs')) is int and 0<=ex['cloud_jobs']<=2)
    cs=d.get('compile_status',{});ls=d.get('link_status',{})
    success=d.get('status')=='fixed_masked_fp16_compile_and_link_verified'
    if success:
        check('QG_J46_VERIFIED_BOTH',ex['cloud_jobs']==2 and
              d.get('cloud_submission_attempted') is True and
              cs.get('verified_success') is True and cs.get('code')=='SUCCESS' and
              ls.get('verified_success') is True and ls.get('code')=='SUCCESS')
    else:
        check('QG_J46_FALSE_SUCCESS',cs.get('verified_success') is not True or
              ls.get('verified_success') is not True)
    archive=Path(path).with_suffix('.zip')
    check('QG_J46_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J46_ZIP_VALID',z.testzip() is None and set(z.namelist())=={'j46-status.json','README.txt'})
        public=json.loads(z.read('j46-status.json'))
    check('QG_J46_REDACTED',public==sanitize(d) and not contains_local_field(public))
    return 'int64_free_fixed_mask_synthetic_qualcomm_compile_link_status_and_privacy_checked'

def postflight_j22_gate(path):
    """Independent, read-only J22 status/privacy check; NEVER submits cloud jobs."""
    from builder_receipt_privacy import sanitize, contains_local_field
    data=read_json(Path(path))
    check('QG_J22_STAGE',data.get('stage')=='J22')
    check('QG_J22_TARGET',data.get('target')=={'model':'SM-S942U','soc':'SM8850','htp':'V81'})
    check('QG_J22_FALSE_DECODER',data.get('decoder_compiler_ready') is False and
          data.get('npu_resident_kv_implemented') is False)
    check('QG_J22_PROBE_SCHEMA',data.get('probe_contract_revision')==3)
    check('QG_J22_COMPILE_FLAG',data.get('compile_options_truncate_64bit_io') is True and
          data.get('synthetic_boundary_values_fit_int32') is True)
    execution=data.get('execution',{})
    check('QG_J22_NO_MODEL_CHANGES',execution.get('phone_changes')==0 and execution.get('model_modifications')==0)
    check('QG_J22_JOB_COUNT',type(execution.get('cloud_jobs')) is int and 0<=execution['cloud_jobs']<=6)
    probes=data.get('probes',[])
    check('QG_J22_PROBES',isinstance(probes,list) and len(probes)<=3)
    expected=('int64_gather_fp16','fixed_masked_attention_fp16','scatter_elements_fp16')
    check('QG_J22_PROBE_ORDER',all(isinstance(p,dict) and p.get('probe')==expected[i] for i,p in enumerate(probes)))
    completed=0
    for index,entry in enumerate(probes):
        outcome=entry.get('outcome')
        check('QG_J22_SAFE_INT64',entry.get('int64_boundary_int32_safe') is True)
        assert_dtypes=({'position':'int64'},{'valid_len':'int32'},{'indices':'int32'})[index]
        check('QG_J22_INPUT_DTYPES',entry.get('boundary_dtypes')==assert_dtypes)
        if entry.get('reused_previous_jobs'):
            check('QG_J22_REUSED_NOT_RECHECKED',index==0 and entry.get('remote_status_rechecked') is True)
        if outcome=='compile_and_link_status_verified':
            for name in ('compile_status','link_status'):
                status=entry.get(name,{})
                check('QG_J22_STATUS_NOT_VERIFIED',status.get('verified_success') is True and status.get('code')=='SUCCESS')
            check('QG_J22_MISSING_JOB_URLS',len(entry.get('compile_job_urls',[]))==1 and
                  bool(entry.get('link_job_url')))
            completed+=1
        else:
            check('QG_J22_FAILED_PROBE_NOT_LAST',index==len(probes)-1)
            check('QG_J22_ERROR_OUTCOME',outcome in ('submission_attempted','compile_failed','link_failed','link_job_missing',
                                                    'cloud_compile_or_link_failed_unverified'))
    reused=sum(1 for x in probes if x.get('reused_previous_jobs') is True)
    check('QG_J22_REUSE_COUNT',reused<=1 and (reused==0 or data.get('reused_prior_qualcomm_jobs')==2))
    if data.get('status')=='all_synthetic_probes_compiled_and_linked_decoder_still_unqualified':
        check('QG_J22_FALSE_SUCCESS',len(probes)==3 and completed==3 and execution['cloud_jobs']==6-2*reused)
    else:
        check('QG_J22_FALSE_SUCCESS',completed!=3)
    archive=Path(path).with_suffix('.zip')
    check('QG_J22_ZIP',archive.is_file() and not archive.is_symlink())
    with zipfile.ZipFile(archive) as z:
        check('QG_J22_ZIP_CORRUPT',z.testzip() is None)
        check('QG_J22_ZIP_CONTENTS',set(z.namelist())=={'stage-j22-cloud-status.json','README.txt'})
        public=json.loads(z.read('stage-j22-cloud-status.json'))
    check('QG_J22_PRIVACY',public==sanitize(data) and not contains_local_field(public))
    return 'j22_checked_compile_and_link_actual_job_statuses_no_decoder_claim'


def extract_unit_test_summary(output, return_code):
    """Allowlisted pytest/unittest IDs only; raw output stays exclusively local."""
    # Pytest normally prints '<N> passed, <M> skipped in <seconds>'.
    # Keep legacy unittest parsing for existing historical diagnostic receipts.
    summary_line = None
    for line in reversed(output.splitlines()):
        if (re.search(r'\b(?:passed|failed|errors?|skipped)\b', line) and
                re.search(r'\d+\s+(?:passed|failed|errors?|skipped)', line)):
            summary_line = line
            break
    pytest_counts = {}
    if summary_line and 'Ran ' not in summary_line:
        for count, name in re.findall(
                r'(\d+)\s+(passed|failed|errors?|skipped|xfailed|xpassed)\b', summary_line):
            name = 'errors' if name == 'error' else name
            pytest_counts[name] = int(count)
    is_pytest = bool(pytest_counts)
    if is_pytest:
        count = sum(pytest_counts.values())
        # Suppress fragile output originating from pytest plugins and avoid
        # reporting subtests as separate top-level test cases.
        failures = re.findall(
            r'(?m)^(?:FAILED|ERROR)\s+(test_[A-Za-z0-9_]+\.py(?:::[A-Za-z0-9_]+)+)(?:\s+-|$)',
            output)
        names = [('PYTEST:' + f) for f in failures]
        counts = pytest_counts
    else:
        found = re.search(r'Ran\s+(\d+)\s+tests', output)
        count = int(found.group(1)) if found else None
        status = re.search(r'FAILED\s*\(([^\r\n)]*)\)', output)
        labels = re.findall(r'(?m)^(FAIL|ERROR):\s+(test_[A-Za-z0-9_]+)\s+\(([A-Za-z0-9_.]+)\)', output)
        names = [f'{category}:{module}.{method}' for category, method, module in labels]
        counts = {}
        if status:
            for key, value in re.findall(r'(failures|errors|skipped|expected failures|unexpected successes)=(\d+)', status.group(1)):
                counts[key.replace(' ', '_')] = int(value)
    return {
        'runner': 'pytest' if is_pytest else 'unittest_or_no_result',
        'runner_exit_code': int(return_code),
        'tests_run': count,
        'counts': counts,
        'failed_test_ids': names[:20],
        'failed_test_ids_truncated': len(names) > 20,
        'output_captured_locally': True,
    }


def test_suite_gate(root, python=sys.executable, local_log=None, timeout_seconds=300):
    # Most Stage G/H/I and Omi safety tests use pytest functions/fixtures; the
    # old unittest discover runner silently skipped many of those functions.
    # Require the real runner; never classify missing pytest as a model error.
    if importlib.util.find_spec('pytest') is None and Path(python).resolve() == Path(sys.executable).resolve():
        raise GateError('QG_PYTEST_MISSING', {'required_test_dependency': 'pytest==8.4.2'})
    command = [str(python), '-m', 'pytest', '-q', '-p', 'no:cacheprovider', '--tb=short',
               '--disable-warnings', '--maxfail=20']
    # Absolute paths avoid relying on shell glob expansion on Windows.
    command += [str(p) for p in sorted((root/'python').glob('test_*.py'))]
    check('QG_PYTEST_TEST_FILES_MISSING', len(command) > 7)
    env = os.environ.copy()
    env['PYTEST_DISABLE_PLUGIN_AUTOLOAD'] = '1'
    try:
        process = subprocess.run(command, cwd=root/'python', capture_output=True,
                                 text=True, errors='replace', env=env,
                                 timeout=timeout_seconds, check=False)
        output = (process.stdout or '') + '\n' + (process.stderr or '')
    except subprocess.TimeoutExpired as exc:
        if local_log is not None:
            Path(local_log).write_text('Pytest runner timed out; partial output follows.\n' +
                (exc.stdout.decode('utf-8', errors='replace') if isinstance(exc.stdout, bytes)
                 else (exc.stdout or '')) + '\n' +
                (exc.stderr.decode('utf-8', errors='replace') if isinstance(exc.stderr, bytes)
                 else (exc.stderr or '')), encoding='utf-8')
        raise GateError('QG_UNIT_TEST_TIMEOUT', {'timeout_seconds': timeout_seconds}) from exc
    except OSError as exc:
        if local_log is not None:
            Path(local_log).write_text('Pytest runner launch failed; check the selected Python environment locally.\n', encoding='utf-8')
        raise GateError('QG_UNIT_TEST_RUNNER_FAILED') from exc
    if local_log is not None:
        Path(local_log).write_text(output, encoding='utf-8')
    summary = extract_unit_test_summary(output, process.returncode)
    if process.returncode != 0:
        if process.returncode == 1 and 'No module named pytest' in output:
            raise GateError('QG_PYTEST_MISSING', {'required_test_dependency':'pytest==8.4.2'})
        raise GateError('QG_UNIT_TESTS_FAILED', summary)
    if summary['runner'] != 'pytest' or summary['tests_run'] is None or summary['tests_run'] < 390:
        raise GateError('QG_UNIT_TEST_COUNT_LOW', summary)
    return summary

def run(args, *, root=ROOT):
    report = {'stage':'BUILDER_QUALITY_GATE','version':VERSION,
              'mode':args.mode,'status':'failed','checks':[],
              'execution':{'cloud_jobs':0,'phone_changes':0,'model_modifications':0},
              'compiler_ready':False,'compile_submission_allowed':False}
    def item(name, func):
        try:
            result = func()
            report['checks'].append({'name':name,'status':'passed','result':result})
        except (GateError, OSError, KeyError, TypeError, ValueError, zipfile.BadZipFile) as exc:
            code = exc.code if isinstance(exc, GateError) else 'QG_UNEXPECTED_VALIDATION_ERROR'
            failed = {'name':name,'status':'failed','code':code}
            if isinstance(exc, GateError) and exc.public_context:
                failed['test_summary'] = exc.public_context
                report['test_summary'] = exc.public_context
            report['checks'].append(failed)
            report['failure_code'] = code
            raise
    try:
        item('source_python_syntax',lambda: syntax_gate(root))
        item('version_and_pipeline_wiring',lambda: wiring_gate(root, expected_version=VERSION))
        if args.mode in ('quick','full'):
            item('bridge_review_module_interface',lambda: bridge_review_interface_gate(root))
            item('python_decoder_dependencies', dependencies_gate if args.onnx_dir is not None else lambda: 'not_requested')
            item('selected_model_and_audio',lambda: inputs_gate(args.onnx_dir,args.audio,args.ffmpeg,
                                                                  args.exporter,args.exporter_receipt))
        if args.mode == 'full':
            item('stage_j_regressions',lambda: test_suite_gate(root, local_log=Path(args.output).with_suffix('.local-test-details.txt')))
        if args.mode == 'postflight':
            item('combined_j7_parity_privacy_and_safety',lambda: postflight_gate(args.combined_receipt))
        if args.mode == 'postflight-j13':
            item('combined_j12_j13_parity_privacy_and_safety',lambda: postflight_j13_gate(args.combined_receipt))
        if args.mode == 'postflight-j14':
            item('j14_real_onnx_kv_interface_and_privacy',lambda: postflight_j14_gate(args.combined_receipt))
        if args.mode == 'postflight-j16':
            item('j15_j16_bounded_host_parity_and_privacy',lambda: postflight_j16_gate(args.combined_receipt))
        if args.mode == 'postflight-j19':
            item('j17_j19_qnn_protocol_and_privacy',lambda: postflight_j19_gate(args.combined_receipt))
        if args.mode == 'postflight-j21':
            item('j20_j21_synthetic_operator_parity_and_privacy',lambda: postflight_j21_gate(args.combined_receipt))
        if args.mode == 'postflight-j22':
            item('j22_compile_link_status_and_privacy',lambda: postflight_j22_gate(args.combined_receipt))
        if args.mode == 'postflight-j25':
            item('j23_j25_real_block_fp32_safety_and_privacy',lambda: postflight_j25_gate(args.combined_receipt))
        if args.mode == 'postflight-j31':
            item('j29_j31_real_fp16_graph_parity_and_cloud_lock',lambda: postflight_j31_gate(args.combined_receipt))
        if args.mode == 'postflight-j42':
            item('j41_j42_weight_free_shared_expand_parity_cloud_lock',lambda: postflight_j42_gate(args.combined_receipt))
        if args.mode == 'postflight-j43':
            item('j43_qualcomm_synthetic_compile_link_status_privacy',lambda: postflight_j43_gate(args.combined_receipt))
        if args.mode == 'postflight-j45':
            item('j44_j45_int64_free_masked_broadcast_parity',lambda: postflight_j45_gate(args.combined_receipt))
        if args.mode == 'postflight-j48':
            item('j47_j48_real_weighted_block_private_packaging',lambda: postflight_j48_gate(args.combined_receipt))
        if args.mode == 'postflight-j53':
            item('j51_j53_profile_limited_real_fp16_exact_parity_cloud_lock',lambda: postflight_j53_gate(args.combined_receipt))
        if args.mode == 'postflight-j56':
            item('j54_j56_read_only_static_graph_liveness_cloud_lock',lambda: postflight_j56_gate(args.combined_receipt))
        if args.mode == 'postflight-j57':
            item('j57_local_pos8_package_cloud_lock',lambda: postflight_j57_gate(args.combined_receipt))
        if args.mode == 'postflight-j59':
            item('j58_j59_real_qualcomm_status_and_privacy',lambda: postflight_j59_gate(args.combined_receipt))
        if args.mode == 'postflight-j69':
            item('j69_rankzero_integer_add_parity',lambda: postflight_j69_gate(args.combined_receipt))
        if args.mode == 'postflight-j71':
            item('j70_j71_link_results_privacy',lambda: postflight_j71_gate(args.combined_receipt))
        if args.mode == 'postflight-j72':
            item('j72_private_linked_context_download',lambda: postflight_j72_gate(args.combined_receipt))
        if args.mode == 'postflight-j73':
            item('j73_s26_htp_run_numeric_cleanup_privacy',lambda: postflight_j73_gate(args.combined_receipt))
        if args.mode == 'postflight-j77':
            item('j77_fused01_fp16_shape_closure',lambda: postflight_j77_gate(args.combined_receipt))
        if args.mode == 'postflight-j83':
            item('j82_j83_fused_scalar_correction_cloud_status',lambda: postflight_j83_gate(args.combined_receipt))
        if args.mode == 'postflight-j84':
            item('j84_existing_linked_fused_context_download',lambda: postflight_j84_gate(args.combined_receipt))
        if args.mode == 'postflight-j85':
            item('j85_real_s26_fused01_parity_and_benchmark',lambda: postflight_j85_gate(args.combined_receipt))
        if args.mode == 'postflight-j86':
            item('j86_original_qwen_hybrid_cpu_experiment',lambda: postflight_j86_gate(args.combined_receipt))
        if args.mode == 'postflight-j81':
            item('j81_exact_rankzero_add_offline_parity',lambda: postflight_j81_gate(args.combined_receipt))
        if args.mode == 'postflight-j80':
            item('j80_onnx_scalar_input_profile_audit',lambda: postflight_j80_gate(args.combined_receipt))
        if args.mode == 'postflight-j79':
            item('j78_j79_fused01_compile_link_status',lambda: postflight_j79_gate(args.combined_receipt))
        if args.mode == 'postflight-j75':
            item('j75_two_layer_reference_parity_and_privacy',lambda: postflight_j75_gate(args.combined_receipt))
        if args.mode == 'postflight-j74':
            item('j74_repeated_s26_htp_benchmark_scope_privacy',lambda: postflight_j74_gate(args.combined_receipt))
        if args.mode == 'postflight-j66':
            item('j66_all_reshape_offline_safety',lambda: postflight_j66_gate(args.combined_receipt))
        if args.mode == 'postflight-j68':
            item('j67_j68_real_qualcomm_status',lambda: postflight_j68_gate(args.combined_receipt))
        if args.mode == 'postflight-j63':
            item('j63_local_j61_candidate_safety',lambda: postflight_j63_gate(args.combined_receipt))
        if args.mode == 'postflight-j65':
            item('j64_j65_new_qualcomm_status_privacy',lambda: postflight_j65_gate(args.combined_receipt))
        if args.mode == 'postflight-j62':
            item('j60_j62_real_dynamic_reshape_parity_privacy',lambda: postflight_j62_gate(args.combined_receipt))
        if args.mode == 'postflight-j50':
            item('j50_read_only_prior_compiler_failure_and_privacy',lambda: postflight_j50_gate(args.combined_receipt))
        if args.mode == 'postflight-j49':
            item('j49_real_weighted_block_model_upload_consent_and_cloud_status',lambda: postflight_j49_gate(args.combined_receipt))
        if args.mode == 'postflight-j46':
            item('j46_int64_free_synthetic_compile_link_status_privacy',lambda: postflight_j46_gate(args.combined_receipt))
        if args.mode == 'postflight-j40':
            item('j38_j40_local_shape_provenance_exact_parity_cloud_lock',lambda: postflight_j40_gate(args.combined_receipt))
        if args.mode == 'postflight-j37':
            item('j35_j37_proven_shape_exact_fp16_cloud_lock',lambda: postflight_j37_gate(args.combined_receipt))
        if args.mode == 'postflight-j34':
            item('j32_j34_real_node_contracts_exact_fp16_cloud_lock',lambda: postflight_j34_gate(args.combined_receipt))
        if args.mode == 'postflight-j28':
            item('j26_j28_real_fp16_block_cpu_safety_and_privacy',lambda: postflight_j28_gate(args.combined_receipt))
        report['status']='passed'
    except Exception:
        if 'failure_code' not in report:
            report['failure_code'] = 'QG_UNEXPECTED_GATE_ERROR'
    finally:
        output = Path(args.output)
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps(report,indent=2,ensure_ascii=True)+'\n',encoding='utf-8')
        with zipfile.ZipFile(output.with_suffix('.zip'),'w',compression=zipfile.ZIP_DEFLATED) as z:
            z.writestr('quality-gate.json',json.dumps(report,indent=2,ensure_ascii=True)+'\n')
            z.writestr('README.txt','Offline test/diagnostic receipt. Failed test identifiers/counts only; raw pytest stdout/stderr and local system paths remain in the separate .local-test-details.txt on the PC. No recordings, weights, tokens, audio filenames or credentials are included.\n')
    print('QUALITY_GATE_RESULT=' + json.dumps({k: report.get(k) for k in ('mode','status','failure_code','test_summary') if k in report}, separators=(',',':'),ensure_ascii=True))
    return 0 if report['status']=='passed' else 2


def main(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--mode', choices=QUALITY_GATE_MODES, required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--onnx-dir',type=Path)
    p.add_argument('--audio',type=Path)
    p.add_argument('--ffmpeg',type=Path)
    p.add_argument('--exporter',type=Path)
    p.add_argument('--exporter-receipt',type=Path)
    p.add_argument('--combined-receipt',type=Path)
    return run(p.parse_args(argv))

if __name__=='__main__':
    sys.exit(main())
