#!/usr/bin/env python3
"""Source-only regression guards for model cards and maintenance invariants."""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
base = root / 'app/src/main/java/com/example/whisperrecorder'
models = (base / 'ui/models/ModelsScreen.kt').read_text()
card = (base / 'ui/models/LocalAsrModelCard.kt').read_text()
setup = (base / 'ui/setup/SetupScreen.kt').read_text()
omi = (base / 'npu/OmiNpuPackageStore.kt').read_text()
settings = (base / 'ui/settings/SettingsSubscreens.kt').read_text()
parakeet = (base / 'npu/NpuPackageStore.kt').read_text()
transcript = (base / 'ui/TranscriptLazyContent.kt').read_text()
version = dict(line.split('=', 1) for line in (root / 'version.properties').read_text().splitlines() if '=' in line)

checks = {
    'APK audit checks engine identifiers instead of removed UI text':
        all(f"require_dex_string '{engine}'" in (root / '.github/workflows/build.yml').read_text()
            for engine in ('parakeet_npu', 'omi_npu', 'qwen3_hybrid'))
        and "require_dex_string 'Use NPU'" not in (root / '.github/workflows/build.yml').read_text()
        and all(f'"{engine}"' in (base / 'transcribe/AsrEngine.kt').read_text()
            for engine in ('parakeet_npu', 'omi_npu', 'qwen3_hybrid')),
    'one shared model card': card.count('Card(Modifier.fillMaxWidth())') == 1
        and 'ModelCatalogCard(' in card and 'ModelCatalogCard(' in models,
    'NPU cards share GPU card layout': models.count('ModelCatalogCard(') >= 3
        and 'LocalAsrModelCard(' in models and 'ModelCatalogCard(' in card,
    'NPU download IDs preserved': 'id = NpuPackageStore.DOWNLOAD_ID' in models
        and 'id = OmiNpuPackageStore.DOWNLOAD_ID' in models,
    'download/cancel/use/model/remove controls': all(s in card for s in
        ['"Use"', '"Download"', '"Resume"', '"Cancel"', '"Model"', 'ModelRemovalAction(']),
    'no Parakeet verify button': 'Verify files' not in models and 'Verify files' not in setup,
    'automatic package verification retained': 'verifyArchived' in omi
        and 'validateManifest' in omi,
    'concise model guidance': 'NPU models offer the best speed and battery efficiency.' in models
        and 'GPU models provide good speed' in models
        and 'CPU models are the slowest.' in models
        and 'Whisperapp/Models' in models,
    'Qwen experimental label scoped to Qwen': 'experimental' in models.lower() and 'experimental' not in omi.split('const val DISPLAY_NAME = ')[1].split('\n')[0].lower(),
    'Omi removal is confirmed and scoped': 'omiDeleteConfirm' in models
        and 'fun deleteArchive(context: Context): Boolean' in omi
        and 'SafStorage.deleteDocument(context, packageDir)' in omi
        and 'AppPrefs.setTranscriptionEngine(context, "whisper")' in omi,
    'model path resolution stays off the library UI thread':
        'val modelPath = ModelSelectionResolver.activeModelPath(this@MainActivity)' not in (base / 'MainActivity.kt').read_text()
        and (base / 'MainActivity.kt').read_text().count('value = withContext(Dispatchers.IO) { ModelSelectionResolver.activeModelPath(this@MainActivity) }') == 2,
    'inventory discovery stays off the Compose thread':
        'mutableStateOf(ModelManager.installed(context))' not in models + setup
        and 'mutableStateOf<Set<String>>(LocalAsrModelManager.installedIds(context))' not in models + setup
        and 'ModelInventoryLoader.load(context, restoreWhisper)' in models + setup,
    'model operations are mutually exclusive':
        'npuActionRunning = true' in models and 'npuActionRunning = false' in models
        and 'localRestoringId != null || npuActionRunning' in models,
    'NPU removal preserves sibling staging':
        'File(context.filesDir, "npu-runtime").deleteRecursively()' not in parakeet
        and 'npu-runtime/$REVISION' in parakeet
        and 'npu-runtime/omi-$REVISION' in omi,
    'long transcript cue search uses a monotonic cursor':
        'val mergedSpans' in transcript and 'var cueCursor = 0' in transcript
        and 'sorted.firstOrNull' not in transcript,
    'release documentation matches version.properties':
        f"Current source release: v{version['releaseVersion']}" in (root / 'README.md').read_text()
        and f"This source targets v{version['releaseVersion']}" in (root / 'VERSIONING.md').read_text(),
    'model package identity and pinned revision preserved':
        'const val REVISION = "qnn-2.50-experimental"' in omi
        and 'whisperrecorder_omi_med_v2_sm8850_v81_experimental_qnn' in omi,
}
for name, ok in checks.items():
    print(f'{"PASS" if ok else "FAIL"}: {name}')
if not all(checks.values()):
    raise SystemExit(1)
print(f'Model catalog UI source checks PASSED ({len(checks)}/{len(checks)})')
