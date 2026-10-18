#!/usr/bin/env python3
"""Qwen experiment safety: no worker-local performance preferences, no old hot loop."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
base=root/'app/src/main/java/com/example/whisperrecorder'
aidl=(root/'app/src/main/aidl/com/example/whisperrecorder/qwen/IQwen3HybridService.aidl').read_text()
worker=(base/'qwen/Qwen3HybridWorkerService.kt').read_text()
session=(base/'transcribe/Qwen3HybridSession.kt').read_text()
client=(base/'qwen/Qwen3HybridClient.kt').read_text()
menu=(base/'ui/models/ModelsScreen.kt').read_text()
reports=(base/'diagnostics/DiagnosticReports.kt').read_text()
assert 'String prepare(String configJson)' in aidl
assert 'QwenDecoderRunConfig.fromJson(configJson)' in worker
assert 'config.cpuArenaDisabled' in worker and 'setCPUArenaAllocator(false)' in worker
assert 'QwenDecoderPerformancePolicy.' not in worker
assert 'client.prepare(config)' in session and 'failed to acknowledge' in session
assert 'QwenDecoderPerformancePolicy.snapshot(app)' in client
assert 'Qwen reusable step buffers: ON' not in menu
assert 'QwenReusableStepInputs' not in worker
assert 'class QwenReusableStepInputs' not in (base/'qwen/QwenReusableStepInputs.kt').read_text()
assert 'decoder_pss_step_delta_kb' in reports
# These are host JVM tests, not instrumented Android tests. The Android stub
# JSONObject throws RuntimeException; keep a real JSON dependency scoped to tests.
build_file=(root/'app/build.gradle.kts').read_text()
assert 'testImplementation("org.json:json:20240303")' in build_file
assert 'implementation("org.json:json:20240303")' not in build_file.replace('testImplementation("org.json:json:20240303")', '')
config_test=(root/'app/src/test/java/com/example/whisperrecorder/npu/QwenDecoderRunConfigTest.kt').read_text()
assert 'configRoundTripAndVersionedHandshake' in config_test
assert 'rejectsUnsupportedParametersBeforeWorkerAllocation' in config_test
print('PASS: worker config handoff, retired buffer path, memory trial, JVM JSON test runtime and diagnostic allowlist')
