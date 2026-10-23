#!/usr/bin/env python3
"""Fail-closed static audit for the v6.7 Qwen3-ASR 0.6B production integration.

Executed locally and in automatic push/manual GitHub CI before NDK/Gradle build.
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
    'version 6.7.5 / 67005': version == {'releaseVersion':'6.7.5','versionName':'6.7.5','versionCode':'67005'},
    'microphone probe checks RECORD_AUDIO at AudioRecord constructor boundary': all(s in read('app/src/main/java/com/example/whisperrecorder/audio/MicrophoneCapabilityProbe.kt') for s in (
        'checkPcm(context, source, noiseSuppressor, aec)',
        'private fun checkPcm(context: Context',
        'context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)',
        'throw SecurityException("Microphone permission is required for a WAV input check")')),
    'recording paths recheck RECORD_AUDIO before opening microphone': all(s in read('app/src/main/java/com/example/whisperrecorder/audio/WavPcmRecorder.kt') for s in (
        'private val context: Context', 'private fun createRecorder(source: Int, bufferSize: Int)',
        'context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)')) and all(s in read('app/src/main/java/com/example/whisperrecorder/audio/RecordingService.kt') for s in (
        'WavPcmRecorder(this, requireNotNull(tempFile), processingConfig)',
        'checkSelfPermission(Manifest.permission.RECORD_AUDIO)',
        'private fun createCompressedRecorder(audioSource: Int): MediaRecorder')),
    'download status is shown in its own model card':
        'if (status.isNotBlank()) Text(status' not in read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt')
        and 'modelDownload.status.isNotBlank()' in read('app/src/main/java/com/example/whisperrecorder/ui/models/LocalAsrModelCard.kt')
        and 'modelDownload.modelId == details.id' in read('app/src/main/java/com/example/whisperrecorder/ui/models/LocalAsrModelCard.kt')
        and 'modelDownload.status.isNotBlank()' in read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt')
        and 'snackbarHost = { SnackbarHost(snackbarHostState) }' in read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt'),
    'verified package downloads auto-select before marking completed':
        all(x in read('app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt') for x in (
            'private fun selectInstalledPackage(target: DownloadTarget)',
            'NpuPackageStore.DOWNLOAD_ID -> NpuPackageStore.selectForTranscription(this)',
            'OmiNpuPackageStore.DOWNLOAD_ID -> OmiNpuPackageStore.selectForTranscription(this)',
            'LocalAsrModelManager.select(this, model)',
            'selectInstalledPackage(target)',
        ))
        and 'select(context, model)' in read('app/src/main/java/com/example/whisperrecorder/advanced/LocalAsrModelManager.kt')
        and 'AppPrefs.setTranscriptionEngine(context, "whisper")' in read('app/src/main/java/com/example/whisperrecorder/models/ModelManager.kt'),
    'automatic push builds + optional manual dispatch': bool(re.search(r"(?m)^on:\s*\n  push:\s*\n    branches:\s*\n      - '\*\*'\s*\n  workflow_dispatch:", wf)) and 'Publish tested APK to GitHub Releases' in wf,
    'lint failures yield visible issue summaries and a failed build job': all(m in wf for m in (
        'scripts/summarize-android-lint.py', 'Enforce Android lint release gate',
        'android-lint-build.log', 'exit 1', 'Lint failed; will not publish'))
        and wf.index('Upload installable APK and build proof') < wf.index('Enforce Android lint release gate') < wf.index('  publish:'),
    'published debug builds are clearly prereleases': all(m in wf for m in (
        'gh release create', '--prerelease', 'needs: build', 'contents: write',
        'android_lint_exit=0', 'sha256sum --check --status')),
    'diagnostic-only Omi probe removed': all('probeOmiContexts' not in read(p) for p in (
        'app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl',
        'app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt',
        'app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeClient.kt')),
    'model section titles and exact GPU/Qwen routing': all(m in read('app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt') for m in (
        'NPU ACCELERATED', 'GPU ACCELERATED', 'HYBRID (CPU/NPU)',
        'LocalAsrModelManager.gpuModels', 'LocalAsrModelManager.hybridModels'))
        and 'it.engine == AsrEngine.PARAKEET || it.engine == AsrEngine.OMI' in models
        and 'it.engine == AsrEngine.QWEN06' in models,
    'window-anchored Qwen approximate phrase timing': all(m in read('app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptTimeline.kt') for m in (
        'cuesForQwenWindows', 'splitEstimatedPhrases(window.text.trim(), maxWords = 3)')),
    'auto build can never select signed production release': all(s in wf for s in (
        "if: github.event_name == 'workflow_dispatch' && inputs.build_variant == 'release'",
        "if: github.event_name == 'workflow_dispatch' && inputs.build_variant == 'release' && env.HAS_RELEASE_SIGNING == 'true'",
        'if [ "${{ github.event_name }}" = "workflow_dispatch" ] && [ "${{ inputs.build_variant }}" = "release" ]; then')),
    'NDK HWCAP_ASIMDDP macro collision prevented': all(s in native for s in ('#include <asm/hwcap.h>', '#ifndef HWCAP_ASIMDDP', '#define HWCAP_ASIMDDP (1UL << 20)')) and 'static constexpr unsigned long HWCAP_ASIMDDP' not in native,
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
    'QNN component is never shown as a Whisper .bin model': all(x in read('app/src/main/java/com/example/whisperrecorder/models/ModelManager.kt') for x in (
        'isWhisperModelCandidate', 'encoder-qnn-', 'filter { isWhisperModelCandidate(it.name) }')),
    'Omi diagnostic probe cleanup is complete': not (root / 'scripts/verify-omi-probe.sh').exists()
        and not (app / 'npu/OmiNpuProbeContracts.kt').exists()
        and 'Omi Med NPU compatibility' not in read('app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt')
        and 'OmiNpuProbeContracts' not in read('app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt'),
    'retired Omi probe Kotlin test absent and cleaned before JVM tests':
        not (root / 'app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt').exists()
        and 'app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt' in read('scripts/clean-retired-omi-diagnostics.sh')
        and 'OmiNpuProbeContracts' not in '\n'.join(
            p.read_text(encoding='utf-8', errors='replace')
            for source_dir in ('app/src/main', 'app/src/test')
            for p in (root / source_dir).rglob('*') if p.is_file() and p.suffix in ('.kt', '.java', '.aidl')),
    'model removed with own QNN encoder': 'encoderDir.deleteRecursively()' in models,
    'model assets downloaded only on request': 'downloadMissing(' in read('app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt'),
    'exact S26 qualifier displayed': 'SM-S942U only' in models,
    'Qwen window progress uses encoder completions and indeterminate decoding':
        all(marker in read('app/src/main/java/com/example/whisperrecorder/transcribe/Qwen06Session.kt') for marker in (
            'estimatedWindowCount', 'onWindowProgress?.invoke(count, totalWindows, "Encoding")',
            'onWindowProgress?.invoke(count, totalWindows, "Decoding")'))
        and all(marker in read('app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt') for marker in (
            'session.onWindowProgress', 'indeterminate = current.qwenWindowsTotal > 0',
            'if (indeterminate) builder.setProgress(0, 0, true)'))
        and 'assertEquals(22, Qwen06Session.estimatedWindowCount(240 * 16_000))' in read('app/src/test/java/com/example/whisperrecorder/transcribe/QwenWindowProgressTest.kt')
        and all(marker in read('app/src/main/java/com/example/whisperrecorder/MainActivity.kt') for marker in (
            'tx.qwenWindowsTotal > 0', 'Decoding ${tx.qwenWindowsTotal} Qwen audio windows',
            'Encoded ${tx.qwenWindowsDone} of ${tx.qwenWindowsTotal} Qwen audio windows')),
    'Qwen explicit choice does not change automatic selection': 'AsrEngine.QWEN06' not in read('app/src/main/java/com/example/whisperrecorder/transcribe/AccelerationPolicy.kt').split('fun preferredAvailable')[1],
    'QNN backup recovery is fail-closed': all(token in read('app/src/main/java/com/example/whisperrecorder/qwen/QwenEncoderAssets.kt') for token in ('recoverInterruptedInstall(folder, backup)', 'verify(backup)', 'backup.renameTo(folder)')),
    'cancel before native decode': 'private fun finish(): JSONObject {\n        check(!cancelRequested)' in worker,
    'QNN progress never decreases': 'highestCompletedBytes = maxOf(highestCompletedBytes, estimate)' in read('app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt'),
    'retired diagnostics cleaned before source validation': all(t in wf for t in (
        'Clean retired Omi diagnostic leftovers', 'Verify source', 'scripts/clean-retired-omi-diagnostics.sh'))
        and wf.index('Clean retired Omi diagnostic leftovers') < wf.index('Verify source'),
    'debug side-by-side is default even when signing secrets exist': all(t in wf for t in ('default: debug', "if: github.event_name == 'workflow_dispatch' && inputs.build_variant == 'release'", 'if [ "${{ github.event_name }}" = "workflow_dispatch" ] && [ "${{ inputs.build_variant }}" = "release" ]; then' ))
}
for item, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ' ' + item)
if not all(checks.values()):
    stale = [str(p.relative_to(root)) for p in (root / 'scripts/verify-omi-probe.sh', app / 'npu/OmiNpuProbeContracts.kt', root / 'app/src/test/java/com/example/whisperrecorder/npu/OmiNpuProbeContractsTest.kt') if p.exists()]
    if stale:
        print('Remove obsolete tracked files: ' + ', '.join(stale))
        print('Run bash scripts/clean-retired-omi-diagnostics.sh in your checkout, commit deletions, then push.')
    raise SystemExit('Production Qwen static audit FAILED')
print(f'Qwen production static audit PASSED ({len(checks)}/{len(checks)})')
