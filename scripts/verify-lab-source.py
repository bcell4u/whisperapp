#!/usr/bin/env python3
"""Fail closed source-level safety gate, not a claim of APK compilation."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
def check(ok, msg):
    if not ok:
        errors.append(msg)
def read(path):
    p = root / path
    check(p.is_file(), f'MISSING {path}')
    return p.read_text(encoding='utf-8') if p.is_file() else ''

version = read('version.properties')
check(version == 'releaseVersion=6.6.1\nversionName=6.6.1\nversionCode=66001\n',
      '6.6.1 version metadata mismatch or not synchronized')
gradle = read('app/build.gradle.kts')
check('applicationId = "com.example.whisperrecorder.qwengguflab"' in gradle, 'lab application ID missing')
check('applicationIdSuffix = ".dev"' in gradle, 'debug suffix missing')
check('versionName = appVersionName' in gradle and 'versionCode = appVersionCode' in gradle,
      'Gradle version no longer sourced from version.properties')
check('QWEN_GGUF_LAB", "true"' in gradle, 'lab guard missing')
check('WR_LAB_STORE_FILE' in gradle and 'WHISPER_RELEASE_STORE_FILE' in gradle,
      'lab signing or original signing-variable convention missing')
check('aidl = true' in gradle, 'AIDL build disabled')
check('useLegacyPackaging = true' in gradle, 'native ELF extraction not enabled')
check(not (root / '.github/workflows/build.yml').exists(), 'PRODUCTION release workflow copied into lab repository')

manifest = read('app/src/main/AndroidManifest.xml')
try:
    tree = ET.fromstring(manifest)
    android = '{http://schemas.android.com/apk/res/android}'
    app = tree.find('application')
    check(app is not None, 'Android application node missing')
    if app is not None:
        check(app.get(android + 'allowBackup') == 'false', 'lab backup must be off')
        check(app.get(android + 'label') == 'Qwen GGUF Lab', 'lab label wrong')
        activities = app.findall('activity')
        launch = []
        for act in activities:
            for filt in act.findall('intent-filter'):
                actions = [x.get(android+'name') for x in filt.findall('action')]
                cats = [x.get(android+'name') for x in filt.findall('category')]
                if 'android.intent.action.MAIN' in actions and 'android.intent.category.LAUNCHER' in cats:
                    launch.append(act.get(android+'name'))
        check(launch == ['.qwen.QwenGgufLabActivity'], f'incorrect lab launcher: {launch}')
        check(any(s.get(android+'name') == '.qwen.Qwen3HybridWorkerService'
                  and s.get(android+'process') == ':qwenlab'
                  and s.get(android+'exported') == 'false' for s in app.findall('service')),
              'private Qwen worker/service/process invalid')
        check(any(p.get(android+'authorities') == '${applicationId}.fileprovider'
                  for p in app.findall('provider')), 'file provider not app-ID scoped')
except ET.ParseError as error:
    errors.append(f'AndroidManifest.xml invalid XML: {error}')


# Version-pinned Kotlin/AIDL inventory, captured from the complete v6.6.1 Lab
# source ZIP. A partial web upload can pass spot checks but fail at Gradle.
# Report ALL absent source files before downloading SDKs or dependencies.
# This checks file presence, not whether the Kotlin code compiles.
expected_kotlin_source_files = """
app/src/main/aidl/com/example/whisperrecorder/gpu/IParakeetOpenClService.aidl
app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl
app/src/main/aidl/com/example/whisperrecorder/qwen/IQwen3HybridService.aidl
app/src/main/java/com/example/whisperrecorder/MainActivity.kt
app/src/main/java/com/example/whisperrecorder/advanced/LocalAsrModelManager.kt
app/src/main/java/com/example/whisperrecorder/advanced/ParakeetCppBackend.kt
app/src/main/java/com/example/whisperrecorder/advanced/TranscribeCppBackend.kt
app/src/main/java/com/example/whisperrecorder/audio/MicrophoneCapabilityProbe.kt
app/src/main/java/com/example/whisperrecorder/audio/MicrophoneProcessing.kt
app/src/main/java/com/example/whisperrecorder/audio/RecordingService.kt
app/src/main/java/com/example/whisperrecorder/audio/WavPcmRecorder.kt
app/src/main/java/com/example/whisperrecorder/data/AppPrefs.kt
app/src/main/java/com/example/whisperrecorder/data/RecordingDeletionManager.kt
app/src/main/java/com/example/whisperrecorder/data/RecordingDeletionPolicy.kt
app/src/main/java/com/example/whisperrecorder/data/RecordingStore.kt
app/src/main/java/com/example/whisperrecorder/data/SafStorage.kt
app/src/main/java/com/example/whisperrecorder/data/WorkspaceStorage.kt
app/src/main/java/com/example/whisperrecorder/diagnostics/DiagnosticLogger.kt
app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClProcessClient.kt
app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClWorkerService.kt
app/src/main/java/com/example/whisperrecorder/media/MediaSource.kt
app/src/main/java/com/example/whisperrecorder/models/ModelDownloadRuntime.kt
app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt
app/src/main/java/com/example/whisperrecorder/models/ModelFileOps.kt
app/src/main/java/com/example/whisperrecorder/models/ModelInventory.kt
app/src/main/java/com/example/whisperrecorder/models/ModelManager.kt
app/src/main/java/com/example/whisperrecorder/models/ModelSelectionResolver.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuEpContextModel.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuGraphContracts.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuPackageFormat.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuPackageStore.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuPerformancePolicy.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeClient.kt
app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt
app/src/main/java/com/example/whisperrecorder/npu/OmiNpuPackageStore.kt
app/src/main/java/com/example/whisperrecorder/npu/OmiNpuProbeContracts.kt
app/src/main/java/com/example/whisperrecorder/npu/OmiParityDiagnostics.kt
app/src/main/java/com/example/whisperrecorder/npu/OmiSentencePiece.kt
app/src/main/java/com/example/whisperrecorder/npu/OmiTdtController.kt
app/src/main/java/com/example/whisperrecorder/npu/ParakeetPreprocessor.kt
app/src/main/java/com/example/whisperrecorder/npu/ParakeetTdtController.kt
app/src/main/java/com/example/whisperrecorder/npu/ParakeetTokenizer.kt
app/src/main/java/com/example/whisperrecorder/npu/Qwen3ByteTokenizer.kt
app/src/main/java/com/example/whisperrecorder/npu/Qwen3Mel.kt
app/src/main/java/com/example/whisperrecorder/npu/QwenDecoderRunConfig.kt
app/src/main/java/com/example/whisperrecorder/prompts/PromptStore.kt
app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridClient.kt
app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenGgufLabActivity.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabEncoderAssets.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabModelDownloader.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDiagnosticLog.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleaner.kt
app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerAttribution.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerClusteringPolicy.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerDiagnostics.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerDiarizationService.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerFailureText.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerModels.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerTranscript.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerWindowAudio.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerWindowMerge.kt
app/src/main/java/com/example/whisperrecorder/speaker/SpeakerWindowStitcher.kt
app/src/main/java/com/example/whisperrecorder/transcribe/AccelerationPolicy.kt
app/src/main/java/com/example/whisperrecorder/transcribe/AsrEngine.kt
app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt
app/src/main/java/com/example/whisperrecorder/transcribe/NpuBlankTranscriptRecovery.kt
app/src/main/java/com/example/whisperrecorder/transcribe/PcmDecoder.kt
app/src/main/java/com/example/whisperrecorder/transcribe/SegmentationPolicy.kt
app/src/main/java/com/example/whisperrecorder/transcribe/SpeakerWordSync.kt
app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptCleaner.kt
app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptPlayback.kt
app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptTimeline.kt
app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionRuntime.kt
app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt
app/src/main/java/com/example/whisperrecorder/transcribe/WhisperBridge.kt
app/src/main/java/com/example/whisperrecorder/ui/AppTheme.kt
app/src/main/java/com/example/whisperrecorder/ui/AudioPlayer.kt
app/src/main/java/com/example/whisperrecorder/ui/RecordingWaveform.kt
app/src/main/java/com/example/whisperrecorder/ui/ShareManager.kt
app/src/main/java/com/example/whisperrecorder/ui/SpeakerCountPicker.kt
app/src/main/java/com/example/whisperrecorder/ui/SpeakerTranscriptMenu.kt
app/src/main/java/com/example/whisperrecorder/ui/SynchronizedAudioPlayer.kt
app/src/main/java/com/example/whisperrecorder/ui/TranscriptLazyContent.kt
app/src/main/java/com/example/whisperrecorder/ui/TranscriptViewerScreen.kt
app/src/main/java/com/example/whisperrecorder/ui/UiComponents.kt
app/src/main/java/com/example/whisperrecorder/ui/UiFormatters.kt
app/src/main/java/com/example/whisperrecorder/ui/models/AccelerationNoticeDialog.kt
app/src/main/java/com/example/whisperrecorder/ui/models/LocalAsrModelCard.kt
app/src/main/java/com/example/whisperrecorder/ui/models/ModelRemovalAction.kt
app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt
app/src/main/java/com/example/whisperrecorder/ui/prompts/PromptsScreen.kt
app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt
app/src/main/java/com/example/whisperrecorder/ui/setup/SetupScreen.kt
app/src/test/java/com/example/whisperrecorder/advanced/LocalAsrCatalogTest.kt
app/src/test/java/com/example/whisperrecorder/audio/MicrophoneProcessingSourcesTest.kt
app/src/test/java/com/example/whisperrecorder/data/RecordingDeletionPolicyTest.kt
app/src/test/java/com/example/whisperrecorder/npu/NpuEncoderPaddingTest.kt
app/src/test/java/com/example/whisperrecorder/npu/NpuPackageFormatTest.kt
app/src/test/java/com/example/whisperrecorder/npu/NpuPerformancePolicyTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiNpuPackageStoreTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiParityDiagnosticsTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiPreprocessorReferenceTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiPreprocessorTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiSentencePieceTest.kt
app/src/test/java/com/example/whisperrecorder/npu/OmiTdtControllerTest.kt
app/src/test/java/com/example/whisperrecorder/npu/ParakeetPreprocessorTest.kt
app/src/test/java/com/example/whisperrecorder/npu/ParakeetTdtControllerTest.kt
app/src/test/java/com/example/whisperrecorder/npu/ParakeetTokenizerTimingTest.kt
app/src/test/java/com/example/whisperrecorder/speaker/SpeakerClusteringPolicyTest.kt
app/src/test/java/com/example/whisperrecorder/speaker/SpeakerFailureTextTest.kt
app/src/test/java/com/example/whisperrecorder/speaker/SpeakerTranscriptTest.kt
app/src/test/java/com/example/whisperrecorder/speaker/SpeakerWindowStitcherTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/AccelerationPolicyTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/NpuBlankTranscriptRecoveryTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/OmiNpuSegmentationTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/OmiNpuTimelineAlignmentTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/SegmentationPolicyTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/SpeakerWordSyncTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/TranscriptPlaybackTest.kt
app/src/test/java/com/example/whisperrecorder/transcribe/TranscriptWordTimingTest.kt
app/src/test/java/com/example/whisperrecorder/ui/TranscriptRenderBlocksTest.kt
""".strip().splitlines()
for relative in expected_kotlin_source_files:
    read(relative)

# Check the complete Android resources tree in one run. Earlier web-based
# repository uploads omitted res/ entirely, causing a misleading one-file error.
resource_root = root / 'app/src/main/res'
expected_resources = [
    'drawable/ic_arrow_back.xml',
    'drawable/ic_edit.xml',
    'drawable/ic_launcher_foreground.xml',
    'drawable/ic_launcher_monochrome.xml',
    'drawable/ic_notification_mic.xml',
    'drawable/ic_notification_pause.xml',
    'drawable/ic_notification_whisper.xml',
    'drawable/ic_qwen_gguf_lab.xml',
    'drawable/ic_settings.xml',
    'drawable/ic_share_media.xml',
    'drawable/ic_share_transcript.xml',
    'drawable/ic_share_video.xml',
    'mipmap-anydpi-v26/ic_launcher.xml',
    'mipmap-anydpi-v26/ic_launcher_round.xml',
    'mipmap-anydpi-v33/ic_launcher.xml',
    'mipmap-anydpi-v33/ic_launcher_round.xml',
    'values-night/themes.xml',
    'values/colors.xml',
    'values/strings.xml',
    'values/themes.xml',
    'xml/backup_rules.xml',
    'xml/data_extraction_rules.xml',
    'xml/file_paths.xml',
    'xml/lab_data_extraction_rules.xml',
]
for relative in expected_resources:
    path = f'app/src/main/res/{relative}'
    contents = read(path)
    if contents:
        try:
            ET.fromstring(contents)
        except ET.ParseError as error:
            errors.append(f'Invalid Android resource XML {path}: {error}')
debug_label = read('app/src/debug/res/values/strings.xml')
if debug_label:
    try:
        ET.fromstring(debug_label)
    except ET.ParseError as error:
        errors.append(f'Invalid debug labels XML: {error}')
for relative in ['LOCAL_ASR_NOTICE.txt', 'PARAKEET_NOTICE.txt', 'SPEAKER_NOTICE.txt']:
    read(f'app/src/main/assets/{relative}')
aidl = read('app/src/main/aidl/com/example/whisperrecorder/qwen/IQwen3HybridService.aidl')
for name in ['prepareGgufLab', 'encodeGgufLab', 'finishGgufLab', 'cancelCurrent', 'closeRuntime']:
    check(name in aidl, f'AIDL missing {name}')
worker = read('app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt')
for marker in ['session.disable_cpu_ep_fallback', 'QNNExecutionProvider',
               'Qwen3Mel.verifyGolden', 'WR_BRIDGE_APP_PRIVATE_PROTOCOL_0001',
               '156 * 1024', 'ByteOrder.LITTLE_ENDIAN',
               'libwrqwengguflab.so', 'model_loads', 'cacheDir, "qwen-gguf-lab-worker"']:
    check(marker in worker, f'QNN bridge or safety protocol missing: {marker}')
check('CpuExecutionProvider' not in worker and 'decoderSession' not in worker,
      'worker contains decoder/fallback path')
check('Log.' not in worker and 'printStackTrace' not in worker,
      'possible private data logging in worker')
for f in ['QwenGgufLabActivity.kt', 'Qwen3HybridClient.kt', 'QwenLabEncoderAssets.kt',
          'QwenLabModelDownloader.kt']:
    read('app/src/main/java/com/example/whisperrecorder/qwen/' + f)
for f in ['Qwen3Mel.kt', 'Qwen3ByteTokenizer.kt', 'QwenDecoderRunConfig.kt']:
    read('app/src/main/java/com/example/whisperrecorder/npu/' + f)

# Direct Lab setup must not rely on the Windows Builder or on the production app.
downloader = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabModelDownloader.kt')
encoder_assets = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabEncoderAssets.kt')
ui = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenGgufLabActivity.kt')
for marker in ['ggml-org/Qwen3-ASR-0.6B-GGUF/resolve/928ab95',
               'bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971',
               '804_749_248L', 'HttpsURLConnection', 'MessageDigest',
               'connection?.disconnect()', 'part.renameTo(destination)',
               'ensureActive(cancelled)', 'decoder-q8.gguf']:
    check(marker in downloader, f'Lab download verification missing: {marker}')
check('downloadOfficial' in encoder_assets and 'model-manifest.json' in encoder_assets and
      'HF_ROOT + name' in encoder_assets and 'QwenLabModelDownloader.downloadFile' in encoder_assets,
      'Hugging Face encoder download/manifest verification missing')
check('Download missing encoder + Q8 GGUF from Hugging Face' in ui and
      'downloadCancelled.set(true)' in ui and 'downloadModels()' in ui,
      'Lab UI download/cancel action missing')
log_source = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDiagnosticLog.kt')
cleaner = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleaner.kt')
for marker in ['MediaStore.Downloads', 'MediaStore.MediaColumns.RELATIVE_PATH',
               'MediaStore.MediaColumns.IS_PENDING', 'SFSU/Whisperapp/Logs',
               'contains_transcript=false', 'resolver.delete(uri, null, null)']:
    check(marker in log_source, f'public Downloads diagnostics contract missing: {marker}')
check('transcript' not in log_source.split('private val numericMetrics =')[1].split('internal fun render')[0],
      'transcript might be included in diagnostic metrics allowlist')
read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleanerTest.kt')
check('QwenLabTranscriptCleaner.cleanSegment' in worker and 'QwenLabDiagnosticLog.save' in ui,
      'transcript cleaning or automatic post-run log missing')
check('language' in cleaner and '<asr_text>' in cleaner,
      'Qwen special-header cleanup missing')
check('http://' not in downloader and 'Runtime.getRuntime' not in downloader and
      'ProcessBuilder' not in downloader, 'Download path has insecure network or ADB/subprocess code')

cpp = read('native/qwen_bridge/qwen_npu_llama_probe.cpp')
for marker in ['WR_BRIDGE_PROBE_PROTOCOL_0496_DOTPROD_V1',
               'WR_BRIDGE_APP_PRIVATE_PROTOCOL_0001', 'cp.n_ctx = 512',
               'llama_memory_clear(', 'cp.n_threads = decode_threads',
               'model_loads\\":1', 'LLAMA_ROPE_TYPE_MROPE']:
    check(marker in cpp, f'native protocol/performance contract missing: {marker}')
cmake = read('native/qwen_bridge/CMakeLists.txt')
check('WR_ENABLE_ARM_DOTPROD' in cmake and '-march=armv8.2-a+dotprod' in cmake,
      'dot-product source configuration missing')

yml = read('.github/workflows/qwen-gguf-lab-6.6-autorelease.yml')
for marker in ['  push:', '    branches: [main]', 'workflow_dispatch:',
               'contents: write', 'gh release create', '--prerelease --latest=false',
               'v6.6.1-lab.', 'app/build/outputs/apk/debug/app-debug.apk',
               'ANDROID_SIGNING_CERT_SHA256', 'libonnxruntime_providers_qnn.so',
               'libQnnHtp.so', 'qwengguflab.dev', "versionCode='66001'",
               "versionName='6.6.1-dev'", 'apksigner', 'WR_LAB_STORE_FILE']:
    check(marker in yml, f'CI auto-release/provenance gate missing: {marker}')

# Fail if unbuilt/private assets are accidentally shipped in the source tree.
for name in ['decoder-q8.gguf', 'encoder-qnn-fp16.bin', 'golden-synthetic-pcm-12s-f32.raw']:
    check(not list(root.rglob(name)), f'PRIVATE/large model asset in source: {name}')
check(not list(root.rglob('*.jks')), 'keystore appears in source archive')
if errors:
    for issue in errors:
        print('FAIL:', issue, file=sys.stderr)
    sys.exit(1)
print('PASS: isolated v6.6.1 source/workflow static gates (NOT an APK compile)')
