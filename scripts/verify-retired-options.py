#!/usr/bin/env python3
"""Keep removed synthetic Omi probe out while preserving normal Omi transcription."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
read=lambda p:(root/p).read_text()
ui=read('app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt')
worker=read('app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt')
client=read('app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeClient.kt')
aidl=read('app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl')
models=read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt')
for filename, content in [('settings',ui),('npu worker',worker),('client',client),('AIDL',aidl)]:
    for legacy in ('probeOmiContexts','omi-npu-probe','Omi Med NPU compatibility'):
        assert legacy not in content, f'{filename} retains {legacy}'
for legacy in ('qwenLegacyDeleteConfirm','qwenCompactDeleteConfirm','Upgrade Qwen to smaller Stage I FP16-storage model','Qwen reusable step buffers'):
    assert legacy not in models, f'retired model control remains: {legacy}'
assert 'qwenAdvancedExpanded' in models and 'Qwen CPU arena' in models
assert 'prepareOmiRuntime()' in aidl and 'transcribeOmiChunk' in aidl
assert 'fun prepareOmiRuntime()' in client and 'fun transcribeOmiChunk' in client
assert 'OmiNpuGraphContracts.ALL' in worker and 'fun prepareOmiRuntime()' in worker
assert 'Omi Med NPU' in models
assert not (root/'app/src/main/java/com/example/whisperrecorder/npu/OmiNpuProbeContracts.kt').exists()
assert not (root/'app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt').exists()
assert 'DiagnosticReports' in ui and 'Automatic diagnostic ZIPs' in ui
print('Retired controls removed; Omi production inference and ZIP diagnostics retained')
