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
check(version == 'releaseVersion=6.6.11\nversionName=6.6.11\nversionCode=66011\n',
      '6.6.11 version metadata mismatch or not synchronized')
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


# Version-pinned Kotlin/AIDL inventory, captured from the complete v6.6.11 Lab
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
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabPublicModels.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDiagnosticLog.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleaner.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptStitcher.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDecoderVerifier.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabVerificationLog.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabSyntheticFeatureExport.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabLogitReplay.kt
app/src/main/java/com/example/whisperrecorder/qwen/QwenLabLogitExport.kt
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
public_source = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabPublicModels.kt')
for marker in ['Download/SFSU/Whisperapp/Models/', 'MediaStore.Downloads',
               'IS_PENDING', 'DocumentFile.fromTreeUri', 'QwenLabEncoderAssets.importFolder',
               'QwenLabModelDownloader.DECODER_SHA256', 'backupVerified', 'restore(']:
    check(marker in public_source, f'portable Models backup contract missing: {marker}')
check('QwenLabPublicModels.backupVerified' in ui and 'QwenLabPublicModels.restore' in ui,
      'Lab UI must offer model backup and restore')

log_source = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDiagnosticLog.kt')
cleaner = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleaner.kt')
for marker in ['MediaStore.Downloads', 'MediaStore.MediaColumns.RELATIVE_PATH',
               'MediaStore.MediaColumns.IS_PENDING', 'SFSU/Whisperapp/Logs',
               'contains_transcript=false', 'resolver.delete(uri, null, null)']:
    check(marker in log_source, f'public Downloads diagnostics contract missing: {marker}')
check('transcript' not in log_source.split('private val numericMetrics =')[1].split('internal fun render')[0],
      'transcript might be included in diagnostic metrics allowlist')
read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabTranscriptCleanerTest.kt')
read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabTranscriptStitcherTest.kt')
verification_test = read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabDecoderVerifierTest.kt')
read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabLogitReplayTest.kt')
verifier = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabDecoderVerifier.kt')
verify_log = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabVerificationLog.kt')
export = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabSyntheticFeatureExport.kt')
for marker in ['PASS_INTERNAL_ONLY', 'PASS_REFERENCE_TOKENS', 'FAIL_REFERENCE_TOKENS',
               'FAIL_INTERNAL_REPLAY', 'INCONCLUSIVE_NO_EOS',
               'INCONCLUSIVE_REFERENCE_INPUT_MISMATCH', 'selfTestGate']:
    check(marker in verifier and marker in verification_test or marker == 'selfTestGate' and marker in verifier,
          f'verification fail-closed branch/test missing: {marker}')
for marker in ['verifyDecoderFixture', 'qnn_feature_repeat_identical', 'model_sha256',
               'feature_sha256', 'FAIL_QNN_FEATURE_REPLAY', 'native_sha256']:
    check(marker in worker, f'worker decoder verification gate missing: {marker}')
check('String verifyDecoderFixture();' in aidl and 'verifyDecoderFixture()' in ui,
      'decoder verification AIDL/UI missing')
check('contains_raw_tokens=false' in verify_log and 'contains_transcript=false' in verify_log and
      'reference_authenticity_verified=false' in verify_log and
      'Download/SFSU/Whisperapp/Verification/' in export and
      'golden-synthetic-mel-12s-f32.raw' in worker,
      'synthetic-only verification exports or privacy guards missing')
read('scripts/create-qwen-verification-reference.py')
read('app/src/test/java/com/example/whisperrecorder/qwen/QwenLabOverlapSegmentCountTest.kt')
check('QwenLabTranscriptCleaner.cleanSegment' in worker and 'QwenLabDiagnosticLog.save' in ui,
      'transcript cleaning or automatic post-run log missing')
check('language' in cleaner and '<asr_text>' in cleaner,
      'Qwen special-header cleanup missing')
stitcher = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenLabTranscriptStitcher.kt')
for marker in ['matchedCount', 'MAX_SEAM_WORDS', 'max downTo 2', 'overlapSeconds == 0']:
    check(marker in stitcher, f'Overlap stitcher missing conservative gate: {marker}')
check('TranscriptCleaner.cleanChunk' in stitcher and 'MAX_SEAM_WORDS = 24' in stitcher and
      'TWO_WORD_MIN_MATCH_CHARS = 12' in stitcher and 'count == 2' in stitcher and
      'cleaned.sumOf { it.repetitionsRemoved }' in stitcher and
      'stitched_repetition_copies_removed' in log_source,
      'Parakeet-style conservative stitching/loop cleanup missing')
check('source.open16kSegmentStream(12, overlapSeconds)' in ui and
      'timedChunk.endMs - source.info.audioStartMs' in ui and
      'overlapControl.isChecked' in ui,
      'Lab 12s/1s overlap selection, actual PCM length gate, or UI missing')
check('QwenLabTranscriptStitcher.stitch(cleanSegments, overlapSeconds)' in worker and
      'segments < 24' in worker and 'segments in 2..24' in worker and
      'feature_paths.size() >= 24' in read('native/qwen_bridge/qwen_npu_llama_probe.cpp'),
      'Worker/native overlap stitching or max-segment cap not synchronized')
check('overlap_seconds' in log_source and 'stitched_duplicate_words' in log_source,
      'Privacy-safe numerical overlap diagnostics missing')
check('http://' not in downloader and 'Runtime.getRuntime' not in downloader and
      'ProcessBuilder' not in downloader, 'Download path has insecure network or ADB/subprocess code')

cpp = read('native/qwen_bridge/qwen_npu_llama_probe.cpp')
for marker in ['WR_BRIDGE_PROBE_PROTOCOL_0496_DOTPROD_V1',
               'WR_BRIDGE_APP_PRIVATE_PROTOCOL_0001', 'cp.n_ctx = 512',
               'llama_memory_clear(', 'cp.n_threads = decode_threads',
               'model_loads\\":1', 'LLAMA_ROPE_TYPE_MROPE',
               'WR_QWEN_PROMPT_ROLE_8948_872_V1', 'WR_QWEN_SYNTHETIC_LOGITS_V1',
               'prefix[] = {151644,8948,198,151645,198,151644,872,198,151669}']:
    check(marker in cpp, f'native protocol/performance contract missing: {marker}')
check('WR_QWEN_PROMPT_ROLE_8948_872_V1' in read('app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt'), 'worker does not check corrected native prompt protocol')
check('WR_QWEN_SYNTHETIC_LOGITS_V1' in read('app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt'), 'worker does not check synthetic logits protocol')
cmake = read('native/qwen_bridge/CMakeLists.txt')
check('WR_ENABLE_ARM_DOTPROD' in cmake and '-march=armv8.2-a+dotprod' in cmake,
      'dot-product source configuration missing')

yml = read('.github/workflows/qwen-gguf-lab-6.6-autorelease.yml')
for marker in ['  push:', '    branches: [main]', 'workflow_dispatch:',
               'contents: write', 'gh release create', '--prerelease --latest=false',
               'v6.6.11-lab.', 'app/build/outputs/apk/debug/app-debug.apk',
               'ANDROID_SIGNING_CERT_SHA256', 'libonnxruntime_providers_qnn.so',
               'libQnnHtp.so', 'qwengguflab.dev', "versionCode='66011'",
               "versionName='6.6.11-dev'", 'apksigner', 'WR_LAB_STORE_FILE', 'WR_QWEN_PROMPT_ROLE_8948_872_V1', 'WR_QWEN_SYNTHETIC_LOGITS_V1', 'scripts/test-qwen-prompt-contract.py', 'scripts/test-qwen-logit-comparison.py', 'scripts/test-qwen-synthetic-logit-contract.py', 'scripts/test-qwen-external-reference.py']:
    check(marker in yml, f'CI auto-release/provenance gate missing: {marker}')

# 6.6.11 independent-process host diagnostic must remain synthetically scoped.
host_workflow = read('.github/workflows/qwen-host-reference-verification.yml')
for marker in ('workflow_dispatch:', 'test-qwen-external-reference.py', 'qwen-sliced-reference', 'qwen-mixed-reference', 'evaluate-qwen-boundary-isolation.py',
               'evaluate-qwen-chunk-isolation.py', 'sha256sum -c -',
               'bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971',
               'reference_kind=HOST_SAME_CPU_GROUPING_BOUNDARY_ISOLATION', 'mrope_parity=UNQUALIFIED'):
    check(marker in host_workflow, f'host reference provenance/check missing: {marker}')
host_source = read('verification/host/qwen_reference_sliced.cpp')
for marker in ('llama_decode(', 'positions.resize(4 * n)', 'positions[axis*n + i]',
               'kTeacher = {11528,4128,102064,13806}', '"logits-step-00.f32le"'):
    check(marker in host_source, f'host reference numeric/position contract missing: {marker}')
check('qwen_npu_llama_probe.cpp' not in host_source,
      'Android decoder cannot be embedded in independent host runner')
# The new host extended API path cannot accidentally become a self-comparison.
mixed_host = read('verification/host/qwen_reference_mixed.cpp')
for marker in ('llama_batch_ext_add_token(', 'llama_batch_ext_add_embd(',
               'llama_batch_ext_set_pos(', 'llama_process(',
               'kTeacher = {11528,4128,102064,13806}', 'position == 171', 'CHUNKED_PREFIX_FAILED', 'CHUNKED_AUDIO_FAILED', 'CHUNKED_SUFFIX_FAILED'):
    check(marker in mixed_host, f'missing host mixed batching contract: {marker}')
check('cut_after_prefix' in mixed_host and 'cut_after_audio' in mixed_host and 'llama_batch_ext_clear(batch.value)' in mixed_host, 'host boundary paths must clear input buffer without resetting KV state')
check('qwen_npu_llama_probe.cpp' not in mixed_host and
      'native/qwen_bridge' not in mixed_host, 'host mixed reference cannot link Android probe')
cmake_host = read('verification/host/CMakeLists.txt')
check('qwen_reference_mixed.cpp' in cmake_host and 'qwen_reference_sliced.cpp' in cmake_host,
      'both reference bridge paths must link same upstream library')
eval_host = read('scripts/evaluate-qwen-batch-paths.py')
check('FAIL_MIXED_REPLAY' in eval_host and 'BATCH_PATH_IDENTICAL' in eval_host and
      'INCONCLUSIVE' in eval_host and 'model_math_independent' in eval_host,
      'host must distinguish parity, replay failure and unavailable data')
check('scripts/test-qwen-batch-paths.py' in host_workflow and
      'artifact/BOUNDARY_ISOLATION_REPORT.json' in host_workflow,
      'CI must execute chunk-path tests and export their numerical report')

# Fail if unbuilt/private assets are accidentally shipped in the source tree.
for name in ['decoder-q8.gguf', 'encoder-qnn-fp16.bin', 'golden-synthetic-pcm-12s-f32.raw']:
    check(not list(root.rglob(name)), f'PRIVATE/large model asset in source: {name}')
check(not list(root.rglob('*.jks')), 'keystore appears in source archive')
check('FAIL_LOGIT_REPLAY' in verify_log and 'INCONCLUSIVE_LOGIT_CHECKPOINT' in verify_log, 'logit replay failure statuses missing')
check('QwenLabLogitReplay.compareFiles' in worker and 'QwenLabLogitExport.save' in worker and '"--synthetic-logits", "fixture"' in worker, 'synthetic logits replay/export not connected')
check('synthetic_logits' in cpp and 'qwen-prefill-logits-' in cpp, 'native synthetic logits export unavailable')
check('CHUNK_GROUPING_SIMILAR_TO_SPLIT' in read('scripts/evaluate-qwen-chunk-isolation.py'), 'new three-path comparison evaluator absent')
check('three-stage' in host_workflow and 'host-three-stage' in host_workflow and 'host-prefix-audio' in host_workflow and 'host-audio-suffix' in host_workflow, 'new boundary isolation paths absent')
check('scripts/test-qwen-boundary-isolation.py' in host_workflow and 'evaluate-qwen-boundary-isolation.py' in host_workflow, 'new boundary evaluator and negative controls absent')
check('scripts/test-qwen-chunk-isolation.py' in host_workflow, 'new chunk comparison regression tests not in CI')
if errors:
    for issue in errors:
        print('FAIL:', issue, file=sys.stderr)
    sys.exit(1)
print('PASS: isolated v6.6.11 source/workflow static gates (NOT an APK compile)')
