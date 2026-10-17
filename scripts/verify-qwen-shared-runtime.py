#!/usr/bin/env python3
"""Source regressions for the shared Qwen hybrid: SAF master, no silent duplication, reversible trial."""
from pathlib import Path
p = Path(__file__).resolve().parent.parent
store = (p/'app/src/main/java/com/example/whisperrecorder/npu/Qwen3HybridPackageStore.kt').read_text()
runner = (p/'app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt').read_text()
menu = (p/'app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt').read_text()
policy = (p/'app/src/main/java/com/example/whisperrecorder/npu/QwenDecoderPerformancePolicy.kt').read_text()
version = (p/'version.properties').read_text()
checks = {
    'version tracks current release': 'versionName=6.5.36' in version and 'versionCode=65036' in version,
    'shared model master stays in user selected workspace': 'WorkspaceStorage.modelsDirectory(context, tree)' in store and 'VISIBLE_PATH = "Whisperapp/Models/qwen3-asr-0.6b"' in store,
    'direct SAF FD loading with seekability probe': 'openSharedRuntime(context: Context)' in store and 'Os.lseek(handle.fileDescriptor' in store and 'Os.symlink("/proc/self/fd/${handle.fd}"' in store,
    'hold FDs while ORT runs': 'private val handles: List<ParcelFileDescriptor>' in store and 'runtimeAssets.close()' in runner and 'closeModels()' in runner,
    'SHA256 verification before direct bridge': 'archiveDigest(context, source.uri) { false }.equals(asset.sha256' in store,
    'only own temporary dirs cleaned': 'qwen3-shared-fd-runtime' in store and 'qwen3-verified-transient-runtime' in store and 'directory.deleteRecursively()' in store,
    'no archive deletion during reclaim': 'fun reclaimPrivateDuplicate(context: Context)' in store and 'getString("proven_$stage", null)' in store and 'doc.size == asset.bytes && archiveDigest' in store,
    'bridge success must actually transcribe': 'noteSuccessfulSharedTranscription(applicationContext, runtimeAssets.stage)' in runner and 'ensureDecoder()' in runner,
    'safe fallback no network': 'openFallbackRuntime(applicationContext)' in runner and 'ModelFileOps.restoreDocument(context, docs.getValue(asset.name).uri, target)' in store,
    'no copy on selecting model': 'fun select(context: Context)' in store and 'AppPrefs.setTranscriptionEngine(context, "qwen3_hybrid")' in store,
    'existing visible package avoids private restore on download': 'Qwen already saved in Whisperapp/Models; no download or private copy needed' in store,
    'fast profile is opt-in': 'getBoolean(KEY, false)' in policy and 'if (fast(context)) 4 else 2' in policy,
    'thermal and per-step telemetry preserved': '"thermal_start"' in runner and '"decoder_step_ms"' in runner and '"model_storage_mode"' in runner,
    'manual reclaim confirmation and no automatic deletion': 'qwenReclaimConfirm' in menu and 'Reclaim private duplicate' in menu and 'reclaimPrivateDuplicate(context)' in menu,
}
for label, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + label)
assert all(checks.values()), 'Qwen SAF runtime regression checks failed'
print(f'Qwen SAF runtime checks PASS ({len(checks)})')
