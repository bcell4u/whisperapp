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
engine_ids = (base / 'transcribe/AsrEngine.kt').read_text()
workflow = (root / '.github/workflows/build.yml').read_text()

# Qwen is intentionally a third independent NPU card.  The experimental
# notice belongs to Qwen alone; do not weaken the previous Parakeet/Omi UI
# guards by stripping that notice or re-labeling the working models.
parakeet_start = models.find('id = NpuPackageStore.DOWNLOAD_ID')
omi_start = models.find('id = OmiNpuPackageStore.DOWNLOAD_ID')
qwen_start = models.find('id = Qwen3HybridPackageStore.DOWNLOAD_ID')
qwen_end = models.find('item { SectionLabel("GPU")', qwen_start)
cards_ordered = 0 <= parakeet_start < omi_start < qwen_start < qwen_end
parakeet_npu_card = models[parakeet_start:omi_start] if cards_ordered else ''
omi_npu_card = models[omi_start:qwen_start] if cards_ordered else ''
qwen_npu_card = models[qwen_start:qwen_end] if cards_ordered else ''

def only_qwen_experimental(source):
    """Allow experimental notices about Qwen, not unrelated model labels."""
    return all('experimental' not in line.lower() or 'qwen' in line.lower()
               for line in source.splitlines())

checks = {
    'APK checks concrete NPU backend IDs, not removed UI labels':
        all(f"require_dex_string '{engine}'" in workflow
            for engine in ('parakeet_npu', 'omi_npu', 'qwen3_hybrid'))
        and all(f'"{engine}"' in engine_ids
                for engine in ('parakeet_npu', 'omi_npu', 'qwen3_hybrid'))
        and "require_dex_string 'Use NPU'" not in workflow,
    'one shared model card': card.count('Card(Modifier.fillMaxWidth())') == 1
        and 'ModelCatalogCard(' in card and 'ModelCatalogCard(' in models,
    'three independent NPU cards share model card layout': models.count('ModelCatalogCard(') >= 5
        and cards_ordered and 'LocalAsrModelCard(' in models
        and 'ModelCatalogCard(' in card,
    'NPU download IDs preserved': 'id = NpuPackageStore.DOWNLOAD_ID' in models
        and 'id = OmiNpuPackageStore.DOWNLOAD_ID' in models,
    'download/cancel/use/model/remove controls': all(s in card for s in
        ['"Use"', '"Download"', '"Resume"', '"Cancel"', '"Model"', 'ModelRemovalAction(']),
    'both menus grouped NPU GPU CPU': all(
        'item { SectionLabel("NPU") }' in src and
        'item { SectionLabel("GPU") }' in src and
        'item { SectionLabel("CPU") }' in src and
        src.index('SectionLabel("NPU")') < src.index('SectionLabel("GPU")') < src.index('SectionLabel("CPU")') and
        'NVIDIA PARAKEET' not in src and 'ADVANCED LOCAL ASR' not in src
        for src in (models, setup)),
    'all model cards share component': models.count('ModelCatalogCard(') >= 5
        and setup.count('ModelCatalogCard(') >= 5,
    'compact Qwen branch and v1 rollback': all(term in (base / 'npu/Qwen3HybridPackageStore.kt').read_text()
        for term in ['private const val COMPACT_REVISION = "main"','decoder_weights.data','schemaVersion == 2',
                     'removeLegacyAfterUpgrade','legacyFolder(context)', 'verifiedFolder']),
    'no Parakeet verify button': 'Verify files' not in models and 'Verify files' not in setup,
    'automatic package verification retained': 'verifyArchived' in omi
        and 'validateManifest' in omi,
    'concise model guidance': 'NPU models offer the best speed and battery efficiency.' in models
        and 'GPU models provide good speed' in models
        and 'CPU models are the slowest.' in models
        and 'Whisperapp/Models' in models,
    'experimental label scoped to Qwen only': cards_ordered
        and 'Experimental hybrid, not clinically validated' in qwen_npu_card
        and 'experimental' not in parakeet_npu_card.lower()
        and 'experimental' not in omi_npu_card.lower()
        and all(only_qwen_experimental(x) for x in
            [settings.replace('ExperimentalMaterial3Api', ''),
             (base / 'transcribe/TranscriptionService.kt').read_text(),
             (base / 'MainActivity.kt').read_text().replace('ExperimentalFoundationApi', '').replace('ExperimentalMaterial3Api', ''),
             omi.split('const val DISPLAY_NAME = ')[1].split('\n')[0]]),
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
    'Qwen availability always refreshed independently of other download IDs':
        'LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {\n        refreshQwenState()' in models
        and 'LaunchedEffect(Unit, modelDownload.modelId, modelDownload.active, modelDownload.completedPath, modelDownload.error) {\n        refreshQwenState()' in setup
        and 'modelDownload.modelId == null || (modelDownload.modelId == Qwen3HybridPackageStore.DOWNLOAD_ID' not in models,
    'Qwen download guarded against already installed package in both menus':
        all('val actual = refreshQwenState()' in x and
            'if (actual == NpuArchiveState.READY)' in x and
            'onStartModelDownload(Qwen3HybridPackageStore.DOWNLOAD_ID)' in x
            for x in (models, setup)),
    'Qwen detection stays off main thread and can be manually rescanned':
        'withContext(Dispatchers.IO) {\n                Qwen3HybridPackageStore.state(context) to Qwen3HybridPackageStore.visibleArchiveReady(context)' in models
        and 'withContext(Dispatchers.IO) {\n                Qwen3HybridPackageStore.state(context) to Qwen3HybridPackageStore.visibleArchiveReady(context)' in setup
        and 'Recheck installed Qwen model' in models
        and 'qwenStateChecking' in models and 'qwenStateChecking' in setup,
    'model package identity and pinned revision preserved':
        'const val REVISION = "qnn-2.50-experimental"' in omi
        and 'whisperrecorder_omi_med_v2_sm8850_v81_experimental_qnn' in omi,
}
for name, ok in checks.items():
    print(f'{"PASS" if ok else "FAIL"}: {name}')
if not all(checks.values()):
    raise SystemExit(1)
print(f'Model catalog UI source checks PASSED ({len(checks)}/{len(checks)})')
