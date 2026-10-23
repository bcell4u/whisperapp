#!/usr/bin/env python3
"""Fail-closed static audit for the v6.7 Qwen3-ASR 0.6B production integration.

Executed locally and in the manual production Android CI before NDK/Gradle build.
Only source-level assurances: not a substitute for Android compiler/device checks.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/com/example/whisperrecorder'
read = lambda p: (root / p).read_text(encoding='utf-8')
version = dict(line.split('=', 1) for line in read('version.properties').splitlines() if '=' in line)
wf = read('.github/workflows/build.yml')
models = read('app/src/main/java/com/example/whisperrecorder/advanced/LocalAsrModelManager.kt')
native = read('native/qwen_bridge/qwen_asr_decode.cpp')
worker = read('app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt')
downloader = read('app/src/main/java/com/example/whisperrecorder/qwen/QwenModelDownloader.kt')
all_files = [x for x in root.rglob('*') if x.is_file() and not any(q in x.parts for q in ('build', '.gradle', '.native-deps', 'scripts'))]
checks = {
    'version 6.7.0 / 67000': version == {'releaseVersion':'6.7.0','versionName':'6.7.0','versionCode':'67000'},
    'manual build (no autonomous release)': bool(re.search(r'(?m)^on:\s*\n\s*workflow_dispatch:\s*\n', wf)) and 'Publish GitHub Release' not in wf,
    'production package unchanged': 'applicationId = "com.example.whisperrecorder"' in read('app/build.gradle.kts'),
    'native executable staged and audited': all(marker in wf for marker in ('wr-qwen06-decoder', "require_apk_entry 'lib/arm64-v8a/libwrqwen06.so'", 'd81235049384534c167caea52b85a694f6103d14')),
    'legacy 1.7B card and native module removed': 'Qwen3-ASR 1.7B' not in models and 'qwen3-asr-1.7b-q4km' not in models and not (root / 'transcribenative').exists() and not (app / 'advanced/TranscribeCppBackend.kt').exists(),
    'no transcribe.cpp runtime or 1.7B engine routing': all('QWEN3' not in read(p) for p in ('app/src/main/java/com/example/whisperrecorder/transcribe/AsrEngine.kt', 'app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt')) and 'transcribenative' not in read('app/build.gradle.kts') and 'transcribenative' not in read('settings.gradle.kts'),
    'APK audit forbids retired native library/model card': all(s in wf for s in ("forbid_apk_entry_contains 'lib/arm64-v8a/libtranscribe_cpp_jni.so'", "grep -F 'Qwen3-ASR 1.7B'")),
    'retired model preference never keeps incompatible GGUF path': all(s in read('app/src/main/java/com/example/whisperrecorder/data/AppPrefs.kt') for s in ('if (saved == "qwen3")', '.remove(KEY_TRANSCRIPTION_ENGINE)', '.remove(KEY_MODEL)')),
    'no obsolete recursive Qwen retry path': 'transcribeWithAdaptiveQwenRetry' not in read('app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt'),
    '0.6B model card shares established UI': 'Qwen3-ASR 0.6B' in models and 'LocalAsrModelCard(' in read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt'),
    '0.6B model uses new worker': 'AsrEngine.QWEN06 -> Qwen06Session(context)' in read('app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt'),
    '0.6B two-component downloader': all(x in downloader for x in ('ggml-org/Qwen3-ASR-0.6B-GGUF','DECODER_SHA256','QwenEncoderAssets.downloadOfficial','downloadFile(')),
    'bounded and authenticated model download': all(x in downloader for x in ('HttpsURLConnection','MessageDigest.getInstance("SHA-256")','download truncated','Only authenticated HTTPS downloads are allowed','part.renameTo(destination)')),
    'QNN refuses CPU fallback': 'session.disable_cpu_ep_fallback' in worker and 'QNNExecutionProvider' in worker,
    'four decoder threads/greedy output': all(x in native for x in ('ctx_params.n_threads=4','ctx_params.n_threads_batch=4','greedy(','n_ctx=512','AUDIO_FRAMES = 156')),
    'one model load for multiple chunks': 'for (size_t segment=0; segment<files.size(); ++segment)' in native and native.count('llama_model_load_from_file') == 1,
    'private runtime not exported to user logs': 'DiagnosticLogger' not in worker and 'Log.' not in worker and 'Log.' not in native,
    'no Lab source tree/fixtures': all(not any(part in {'verification','fixtures','lab'} for part in p.relative_to(root).parts) for p in all_files)
        and not any('s26-synthetic' in p.name.lower() for p in all_files),
    'no downloaded model weights committed': not any(p.suffix.lower() in {'.gguf','.onnx','.bin','.safetensors'} for p in all_files),
    'model removed with own QNN encoder': 'encoderDir.deleteRecursively()' in models,
    'model assets downloaded only on request': 'downloadMissing(' in read('app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt'),
    'exact S26 qualifier displayed': 'SM-S942U only' in models,
    'Qwen explicit choice does not change automatic selection': 'AsrEngine.QWEN06' not in read('app/src/main/java/com/example/whisperrecorder/transcribe/AccelerationPolicy.kt').split('fun preferredAvailable')[1],
    'QNN backup recovery is fail-closed': all(token in read('app/src/main/java/com/example/whisperrecorder/qwen/QwenEncoderAssets.kt') for token in ('recoverInterruptedInstall(folder, backup)', 'verify(backup)', 'backup.renameTo(folder)')),
    'cancel before native decode': 'private fun finish(): JSONObject {\n        check(!cancelRequested)' in worker,
    'QNN progress never decreases': 'highestCompletedBytes = maxOf(highestCompletedBytes, estimate)' in read('app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt'),
    'debug side-by-side is default even when signing secrets exist': all(t in wf for t in ('default: debug', "if: inputs.build_variant == 'release'", 'if [ "${{ inputs.build_variant }}" = "release" ]; then'))
}
for item, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ' ' + item)
if not all(checks.values()): raise SystemExit('Production Qwen static audit FAILED')
print(f'Qwen production static audit PASSED ({len(checks)}/{len(checks)})')
