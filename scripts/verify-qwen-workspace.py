#!/usr/bin/env python3
"""Offline guard against regressing Qwen back to invisible app-private-only storage."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
src=(root/'app/src/main/java/com/example/whisperrecorder/npu/Qwen3HybridPackageStore.kt').read_text()
menu=(root/'app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt').read_text()
setup=(root/'app/src/main/java/com/example/whisperrecorder/ui/setup/SetupScreen.kt').read_text()
checks={
    'same selected Whisperapp/Models as Parakeet and Omi': 'WorkspaceStorage.modelsDirectory(context, tree)' in src and 'VISIBLE_PATH = "Whisperapp/Models/qwen3-asr-0.6b"' in src,
    'archive stage directory created with SAF': 'SafStorage.getOrCreateChildDirectory(context, tree, root, stage)' in src,
    'new downloads archived rather than private-only': 'val location = saveInstalledToWhisperappModels(context, cancelled, progress)' in src,
    'offline archive migration menu': 'Save existing Qwen to Whisperapp/Models (no download)' in menu and 'saveInstalledToWhisperappModels(context)' in menu,
    'archived model available for selection without HTTP': 'visibleArchiveReady(context)' in src and 'restoreFromVisibleArchive(context, stage, version)' in src,
    'runtime local copy required for ORT external weights': 'ensureLocalRuntime(context)' in src and 'fun verifiedFolder(context: Context)' in src,
    'archive READY written last': 'val files = listOf(MANIFEST, CONTRACT) + assets.map { it.name } + READY' in src,
    'SAF copy is byte checked': 'archiveDigest(context, copied, cancelled).equals(digest, true)' in src,
    'restore validates SHA256': 'ModelFileOps.restoreDocument(context, doc.uri, file)' in src and 'ModelFileOps.verify(incoming, doc.size, "SHA-256", expectedHash)' in src,
    'Qwen visible archive is not auto-deleted': 'SafStorage.deleteDocument(context' not in src,
    'both cards reflect persistent storage': 'persistent = qwenVisibleSaved' in menu and 'persistent = qwenVisibleSaved' in setup,
    'version synchronized': 'versionName=' + (root/'version.properties').read_text().split('releaseVersion=')[1].splitlines()[0] in (root/'version.properties').read_text(),
}
for label, ok in checks.items(): print(('PASS' if ok else 'FAIL')+': '+label)
if not all(checks.values()): raise SystemExit(1)
print(f'Qwen shared workspace safeguards passed ({len(checks)})')
