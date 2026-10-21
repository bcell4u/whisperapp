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
check(version == 'releaseVersion=6.6.0\nversionName=6.6.0\nversionCode=66000\n',
      '6.6.0 version metadata mismatch or not synchronized')
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
for f in ['QwenGgufLabActivity.kt', 'Qwen3HybridClient.kt', 'QwenLabEncoderAssets.kt']:
    read('app/src/main/java/com/example/whisperrecorder/qwen/' + f)
for f in ['Qwen3Mel.kt', 'Qwen3ByteTokenizer.kt', 'QwenDecoderRunConfig.kt']:
    read('app/src/main/java/com/example/whisperrecorder/npu/' + f)
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
               'v6.6.0-lab.', 'app/build/outputs/apk/debug/app-debug.apk',
               'ANDROID_SIGNING_CERT_SHA256', 'libonnxruntime_providers_qnn.so',
               'libQnnHtp.so', 'qwengguflab.dev', "versionCode='66000'",
               "versionName='6.6.0-dev'", 'apksigner', 'WR_LAB_STORE_FILE']:
    check(marker in yml, f'CI auto-release/provenance gate missing: {marker}')

# Fail if unbuilt/private assets are accidentally shipped in the source tree.
for name in ['decoder-q8.gguf', 'encoder-qnn-fp16.bin', 'golden-synthetic-pcm-12s-f32.raw']:
    check(not list(root.rglob(name)), f'PRIVATE/large model asset in source: {name}')
check(not list(root.rglob('*.jks')), 'keystore appears in source archive')
if errors:
    for issue in errors:
        print('FAIL:', issue, file=sys.stderr)
    sys.exit(1)
print('PASS: isolated v6.6.0 source/workflow static gates (NOT an APK compile)')
