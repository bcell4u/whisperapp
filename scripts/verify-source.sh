#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${PROJECT_DIR:-$(cd "$SCRIPT_DIR/.." && pwd)}"
PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"

fail() { echo "source verification failed: $*" >&2; exit 1; }
require_file() { test -f "$1" || fail "missing required file: $1"; }
require_text() { grep -Fq -- "$1" "$2" || fail "missing expected text in ${2#$PROJECT_DIR/}: $1"; }
forbid_text() { if grep -Fq -- "$1" "$2"; then fail "forbidden text in ${2#$PROJECT_DIR/}: $1"; fi; }
forbid_path() { if [ -e "$1" ] || [ -L "$1" ]; then fail "forbidden path exists: ${1#$PROJECT_DIR/}"; fi; }

VERSION_FILE="$PROJECT_DIR/version.properties"
APP_BUILD="$PROJECT_DIR/app/build.gradle.kts"
WORKFLOW="$PROJECT_DIR/.github/workflows/build.yml"
MANIFEST="$PROJECT_DIR/app/src/main/AndroidManifest.xml"
MAIN="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/MainActivity.kt"
LOCAL_MODELS="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/advanced/LocalAsrModelManager.kt"
MODEL_MANAGER="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/models/ModelManager.kt"
MODEL_INVENTORY="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/models/ModelInventory.kt"
MODEL_SELECTION="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/models/ModelSelectionResolver.kt"
DOWNLOAD_SERVICE="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/models/ModelDownloadService.kt"
TX="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt"
PCM="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/PcmDecoder.kt"
TIMELINE="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptTimeline.kt"
PLAYBACK="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptPlayback.kt"
VIEWER="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/TranscriptViewerScreen.kt"
LAZY_TRANSCRIPT="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/TranscriptLazyContent.kt"
SETUP="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/setup/SetupScreen.kt"
MODELS_SCREEN="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt"
SETTINGS_SUB="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/settings/SettingsSubscreens.kt"
RECORDING_SERVICE="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/audio/RecordingService.kt"
WORKSPACE="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/data/WorkspaceStorage.kt"
STORE="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/data/RecordingStore.kt"
DELETE_MANAGER="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/data/RecordingDeletionManager.kt"
PROMPTS="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/prompts/PromptStore.kt"
PROMPTS_UI="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/prompts/PromptsScreen.kt"
WAVEFORM="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/RecordingWaveform.kt"
AUDIO_PLAYER="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/AudioPlayer.kt"
SIGNING_DOC="$PROJECT_DIR/SIGNING.md"
GITIGNORE="$PROJECT_DIR/.gitignore"
PARAKEET_NATIVE_CMAKE="$PROJECT_DIR/parakeetnative/src/main/cpp/CMakeLists.txt"
PARAKEET_NATIVE_JNI="$PROJECT_DIR/parakeetnative/src/main/cpp/parakeet_jni.cpp"
PARAKEET_BACKEND="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/advanced/ParakeetCppBackend.kt"
FIND_OPENCL="$PROJECT_DIR/cmake/FindOpenCL.cmake"

for f in "$VERSION_FILE" "$APP_BUILD" "$WORKFLOW" "$MANIFEST" "$MAIN" "$LOCAL_MODELS" "$MODEL_MANAGER" "$MODEL_INVENTORY" "$MODEL_SELECTION" "$DOWNLOAD_SERVICE" "$TX" "$PCM" "$TIMELINE" "$PLAYBACK" "$VIEWER" "$LAZY_TRANSCRIPT" "$SETUP" "$MODELS_SCREEN" "$SETTINGS_SUB" "$RECORDING_SERVICE" "$WORKSPACE" "$STORE" "$DELETE_MANAGER" "$PROMPTS" "$PROMPTS_UI" "$WAVEFORM" "$AUDIO_PLAYER" "$SIGNING_DOC" "$GITIGNORE"; do
  require_file "$f"
done
for f in "$PARAKEET_NATIVE_CMAKE" "$PARAKEET_NATIVE_JNI" "$PARAKEET_BACKEND" "$FIND_OPENCL"; do
  require_file "$f"
done

RELEASE_VERSION="$(sed -n 's/^releaseVersion=//p' "$VERSION_FILE")"
VERSION_NAME="$(sed -n 's/^versionName=//p' "$VERSION_FILE")"
VERSION_CODE="$(sed -n 's/^versionCode=//p' "$VERSION_FILE")"
[[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]] || fail "invalid releaseVersion"; test "$VERSION_NAME" = "$RELEASE_VERSION" || fail "versionName must equal releaseVersion"; [[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || fail "versionCode must be a positive integer"; (( VERSION_CODE <= 2100000000 )) || fail "versionCode exceeds Android release limit"

# One version source feeds Gradle, UI resource generation, CI auditing and release naming.
require_text 'rootProject.file("version.properties")' "$APP_BUILD"
require_text 'versionCode = appVersionCode' "$APP_BUILD"
require_text 'versionName = appVersionName' "$APP_BUILD"
require_text 'resValue("string", "app_release_version", "v$appReleaseVersion")' "$APP_BUILD"
require_text 'resValue("string", "app_version_name", appVersionName)' "$APP_BUILD"
require_text 'getString(R.string.app_version_name)' "$MAIN"
forbid_text 'BuildConfig.VERSION_NAME' "$MAIN"
require_text 'Load version metadata' "$WORKFLOW"
require_text 'APP_RELEASE_TAG=v$RELEASE_VERSION' "$WORKFLOW"
require_text "versionCode='\$APP_VERSION_CODE'" "$WORKFLOW"
require_text "versionName='\$EXPECTED_VERSION_NAME'" "$WORKFLOW"
require_text 'RELEASE_TAG="$APP_RELEASE_TAG"' "$WORKFLOW"
forbid_text 'versionCode = 44' "$APP_BUILD"
forbid_text 'versionName = "0.7.25"' "$APP_BUILD"
require_text 'applicationIdSuffix = ".dev"' "$APP_BUILD"
require_text 'android:authorities="${applicationId}.fileprovider"' "$MANIFEST"
require_text 'parakeet.cpp OpenCL production runtime' "$MAIN"

# v6.0 production Parakeet/OpenCL path.
require_text 'include(":app", ":transcribenative", ":parakeetnative")' "$PROJECT_DIR/settings.gradle.kts"
forbid_path "$PROJECT_DIR/ominative"
require_text 'implementation(project(":parakeetnative"))' "$APP_BUILD"
require_text 'aidl = true' "$APP_BUILD"
require_text 'GIT_TAG v0.5.0' "$PARAKEET_NATIVE_CMAKE"
require_text 'omi_adapter_runtime.patch' "$PARAKEET_NATIVE_CMAKE"
require_file "$PROJECT_DIR/parakeetnative/src/main/cpp/omi_adapter_runtime.patch"
require_text 'PARAKEET_GGML_VULKAN OFF' "$PARAKEET_NATIVE_CMAKE"
require_text 'GGML_OPENCL ON' "$PARAKEET_NATIVE_CMAKE"
require_text 'GGML_OPENCL_USE_ADRENO_KERNELS ON' "$PARAKEET_NATIVE_CMAKE"
require_text 'GGML_OPENCL_EMBED_KERNELS ON' "$PARAKEET_NATIVE_CMAKE"
require_text 'BUILD_SHARED_LIBS OFF' "$PARAKEET_NATIVE_CMAKE"
require_text 'add_library(parakeet_jni SHARED' "$PARAKEET_NATIVE_CMAKE"
require_text 'nativeTranscribeShared' "$PARAKEET_NATIVE_JNI"
require_text 'ASharedMemory_dupFromJava' "$PARAKEET_NATIVE_JNI"
require_text 'nativeResolveDevice' "$PARAKEET_BACKEND"
require_text 'ParakeetCppNativeBridge' "$PROJECT_DIR/app/proguard-rules.pro"
require_text 'id = AsrEngine.PARAKEET.id' "$LOCAL_MODELS"
require_text 'NVIDIA Parakeet TDT 0.6B v3 Q8_0 — Recommended' "$LOCAL_MODELS"
require_text 'tdt-0.6b-v3-q8_0.gguf' "$LOCAL_MODELS"
require_text '940_663_680L' "$LOCAL_MODELS"
require_text '4d69a4a6683f4f2d952bad794c1357ca6eb628027695b4699c5a9ad4cd07d757' "$LOCAL_MODELS"
require_text 'huggingface.co/nvidia/parakeet-tdt-0.6b-v3' "$LOCAL_MODELS"
forbid_text 'parakeet-tdt-1.1b-Q4_K_M.gguf' "$LOCAL_MODELS"
forbid_text 'benchmark-parakeet-cpp-v3-q6k' "$LOCAL_MODELS"
forbid_text 'benchmark-parakeet-cpp-v3-q5k' "$LOCAL_MODELS"
forbid_text 'benchmark-parakeet-cpp-v3-q4k' "$LOCAL_MODELS"
require_text 'ParakeetOpenClSession' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt"
require_text 'ParakeetOpenClProcessClient.connect' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt"
forbid_text 'OmiSession(' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt"
forbid_path "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/advanced/OmiBackend.kt"
require_text 'android:process=":opencl"' "$MANIFEST"
require_text 'ParakeetOpenClWorkerService' "$MANIFEST"
require_text 'MODEL_CACHE_IDLE_MS = 120_000L' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClWorkerService.kt"
require_text 'SharedMemory.create("parakeet-pcm"' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClProcessClient.kt"
require_text 'PARAKEET_MAX_SECONDS = 40' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/SegmentationPolicy.kt"
require_text 'OMI_MAX_SECONDS = 40' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/SegmentationPolicy.kt"
require_text 'production-opencl-fixed40' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/SegmentationPolicy.kt"
require_text 'prefetchedNext' "$TX"
require_text 'request.engine == AsrEngine.PARAKEET || request.engine == AsrEngine.PARAKEET_NPU || request.engine == AsrEngine.OMI' "$TX"
require_text 'android:name="libOpenCL.so"' "$MANIFEST"
require_text '**/libOpenCL.so' "$APP_BUILD"
# APK DEX auditing must use ASCII-safe markers; GNU strings splits the Unicode UI labels.
require_text "require_dex_string 'NVIDIA Parakeet TDT 0.6B v3'" "$WORKFLOW"; require_text "require_dex_string 'Omi Med STT v1 Q8_0 (0.6B)'" "$WORKFLOW"; require_text 'parakeet.cpp 0.5.0 OpenCL device=' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/gpu/ParakeetOpenClWorkerService.kt"; require_text "require_dex_string 'parakeet.cpp 0.5.0 OpenCL device='" "$WORKFLOW"; forbid_text "require_dex_string 'parakeet.cpp v0.5.0 OpenCL / Adreno'" "$WORKFLOW"
forbid_text "grep -F 'NVIDIA Parakeet TDT 0.6B v3 — Recommended' /tmp/whisper_dex_strings.txt" "$WORKFLOW"; forbid_text "grep -F 'Omi Med STT v1 Q8_0 (0.6B) — Medical' /tmp/whisper_dex_strings.txt" "$WORKFLOW"
# Release build remains lean and permanently signed.
test "$(grep -Fc 'isMinifyEnabled = true' "$APP_BUILD")" -ge 2 || fail "debug and release must enable R8"
test "$(grep -Fc 'isShrinkResources = true' "$APP_BUILD")" -ge 2 || fail "debug and release must shrink resources"
require_text 'WHISPER_RELEASE_STORE_FILE' "$APP_BUILD"
require_text 'Restore permanent release signing key' "$WORKFLOW"
require_text 'gradle --no-daemon assembleRelease' "$WORKFLOW"
require_text 'Permanent release signature verified' "$WORKFLOW"
require_text '*.jks' "$GITIGNORE"
if find "$PROJECT_DIR" -path '*/build/*' -prune -o -type f \( -name '*.jks' -o -name '*.keystore' \) -print | grep -q .; then
  fail "signing keystore committed to source tree"
fi

# CI now runs real JVM tests for pure policies instead of encoding all behavior in grep checks.
require_text 'testImplementation("junit:junit:4.13.2")' "$APP_BUILD"
require_text 'gradle --no-daemon testDebugUnitTest' "$WORKFLOW"
for test_file in \
  "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/transcribe/SegmentationPolicyTest.kt" \
  "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/transcribe/TranscriptPlaybackTest.kt" \
  "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/advanced/LocalAsrCatalogTest.kt" \
  "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/ui/TranscriptRenderBlocksTest.kt"; do
  require_file "$test_file"
done

# All GGUF engines use one catalog/lifecycle. Retired Parakeet implementations must stay deleted.
forbid_path "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/parakeet"
forbid_text 'PARAKEET_11' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrEngine.kt"
forbid_text 'benchmarkOnly' "$LOCAL_MODELS"
require_text 'id = AsrEngine.PARAKEET.id' "$LOCAL_MODELS"
require_text '929_205_888L' "$LOCAL_MODELS"
require_text 'Omi Med STT v1 Q8_0 (0.6B) — Medical' "$LOCAL_MODELS"
require_text 'OpenCL GPU automatic' "$LOCAL_MODELS"
require_text '1_319_830_496L' "$LOCAL_MODELS"
require_text 'LocalAsrModelManager.byId(modelId)' "$DOWNLOAD_SERVICE"
require_text 'selectedEngine in LocalAsrModelManager.engineIds' "$MODEL_INVENTORY"
require_text 'object ModelSelectionResolver' "$MODEL_SELECTION"
forbid_text 'ParakeetManager' "$TX"

# Whisper archive restores validate known built-in hashes, while custom .bin files remain supported.
require_text 'ModelFileOps.verify(restored, algorithm = "SHA-1", expectedDigest = builtIn.sha1)' "$MODEL_MANAGER"
require_text 'Saved model ${doc.name} failed checksum verification' "$MODEL_MANAGER"

# Long transcript rendering is bounded and lazy; short transcripts retain one-block rendering.
require_text 'LazyColumn(' "$VIEWER"
require_text 'TranscriptRenderBlocks.build' "$VIEWER"
require_text 'itemsIndexed(renderBlocks' "$VIEWER"
require_text 'LAZY_THRESHOLD_CHARS = 12_000' "$LAZY_TRANSCRIPT"
require_text 'TARGET_BLOCK_CHARS = 2_800' "$LAZY_TRANSCRIPT"
forbid_text '.verticalScroll(pageScroll)' "$VIEWER"
forbid_text 'append(transcript)' "$VIEWER"

# MainActivity is decomposed into feature screens rather than accumulating every settings/model path.
require_text 'com.example.whisperrecorder.ui.setup.SetupScreen' "$MAIN"
require_text 'com.example.whisperrecorder.ui.models.ModelsScreen' "$MAIN"
require_text 'com.example.whisperrecorder.ui.settings.AudioFormatSettingsScreen' "$MAIN"
main_lines="$(wc -l < "$MAIN")"
test "$main_lines" -lt 1600 || fail "MainActivity regressed to $main_lines lines; keep feature screens decomposed"

# Streaming media and warm-session transcription invariants.
require_text 'class PreparedSource' "$PCM"
require_text 'open16kSegmentStream' "$PCM"
require_text 'MediaExtractor()' "$PCM"
require_text 'PcmDecoder.prepare(this@TranscriptionService, request.mediaUri)' "$TX"
require_text 'warmSessionKey' "$TX"
require_text 'Keep one foreground-service lifecycle across queued recordings' "$TX"
require_text 'PowerManager.PARTIAL_WAKE_LOCK' "$TX"
require_text 'Process.THREAD_PRIORITY_FOREGROUND' "$TX"
forbid_text 'decodeToMonoPcm16File' "$PCM"
forbid_text 'ffmpeg' "$APP_BUILD"

# Recording visualization remains isolated from the slow-changing recording state.
require_text 'private val _liveLevel = MutableStateFlow(0f)' "$RECORDING_SERVICE"
require_text 'val liveLevel: StateFlow<Float> = _liveLevel' "$RECORDING_SERVICE"
require_text 'LaunchedEffect(level, paused)' "$WAVEFORM"
forbid_text 'delay(80)' "$WAVEFORM"

# Workspace/library integrity and user-controlled deletion remain intact.
require_text 'const val ROOT_DIR = "Whisperapp"' "$WORKSPACE"
require_text 'const val RECORDINGS_DIR = "Recordings"' "$WORKSPACE"
require_text 'const val TRANSCRIPTS_DIR = "Transcripts"' "$WORKSPACE"
require_text 'const val MODELS_DIR = "Models"' "$WORKSPACE"
require_text 'const val LOGS_DIR = "Logs"' "$WORKSPACE"
forbid_text 'BACKUPS_DIR' "$WORKSPACE"
require_text 'fun reconcileAvailability(context: Context): ReconcileResult' "$STORE"
require_text 'fun removeFromLibrary(context: Context, item: RecordingItem): Result' "$DELETE_MANAGER"
require_text 'Remove from Library only' "$MAIN"
require_text 'Transcribe all (' "$MAIN"
require_text '.sortedBy { it.createdAt }' "$MAIN"

# Prompt backup remains explicit through the system file picker.
require_text 'const val CUSTOM_CATEGORY = "Custom"' "$PROMPTS"
require_text 'BACKUP_SCHEMA = "whisper-recorder-custom-prompts"' "$PROMPTS"
require_text 'ActivityResultContracts.CreateDocument("application/json")' "$PROMPTS_UI"
require_text 'ActivityResultContracts.OpenDocument()' "$PROMPTS_UI"
# Playback sync remains user-adjustable with intuitive direction labels.
require_text '"Slower"' "$AUDIO_PLAYER"
require_text '"Faster"' "$AUDIO_PLAYER"
require_text 'Move left to slow the highlight, right to advance it.' "$AUDIO_PLAYER"

# Foreground services survive task removal; no process-kill exit path.
require_text 'finishAndRemoveTask()' "$MAIN"
forbid_text 'System.exit' "$MAIN"
test "$(grep -Fc 'android:stopWithTask="false"' "$MANIFEST")" -ge 3 || fail "foreground services must survive task removal"

# NPU stays on the proven ORT-QNN production path.
require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetPreprocessor.kt"; require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetTokenizer.kt"; require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuPerformancePolicy.kt"; require_file "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/npu/NpuPerformancePolicyTest.kt"
require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetTdtController.kt"; require_text 'listOf(MicrophoneProcessing.MODE_OFF to "Off", MicrophoneProcessing.MODE_ON to "On", MicrophoneProcessing.MODE_AUTO to "Auto")' "$SETTINGS_SUB"; require_text 'SOURCE_CAMCORDER to "Camcorder"' "$SETTINGS_SUB"; require_text 'SOURCE_CAMCORDER -> MediaRecorder.AudioSource.CAMCORDER' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/audio/MicrophoneProcessing.kt"; require_text 'setOf("mic", "voice_recognition", "camcorder")' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/data/AppPrefs.kt"
require_text 'fun runtimeArchiveInfo' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuPackageStore.kt"
require_text 'String transcribeChunk(in SharedMemory pcm, int sampleCount, long audioDurationMs);' "$PROJECT_DIR/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"; require_text 'String prepareChunk(in SharedMemory pcm, int sampleCount, long audioDurationMs);' "$PROJECT_DIR/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"; require_text 'String transcribePreparedChunk(long preparedId);' "$PROJECT_DIR/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"; require_text 'Prepared NPU double buffer exceeded two chunks' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text 'session.prepare(next.samples, language, threads)' "$TX"
require_text 'transient-staged' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text '"projector" -> "wr_projector"' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text '"predictor" -> "wr_predictor"' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text '"joint" -> "wr_joint"' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text 'ep.share_ep_contexts' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; require_text 'linked_decoder_shared_context' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; python3 "$PROJECT_DIR/scripts/verify-model-catalog-ui.py"; require_text 'ModelCatalogCard(' "$MODELS_SCREEN"; require_text 'NpuPackageStore.selectForTranscription(context)' "$MODELS_SCREEN"; forbid_text 'Verify files' "$MODELS_SCREEN"; require_text 'reused_view_bulk_copy_single_validation' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"
bash "$PROJECT_DIR/scripts/verify-npu-interfaces.sh"; bash "$PROJECT_DIR/scripts/verify-feature-gates.sh"
# Final cleanup keeps only production transcription/logging surfaces.
for p in "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/settings/BenchmarkScreen.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/benchmark/BenchmarkRunner.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/benchmark/BenchmarkWorkerService.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuDiagnosticRunner.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuLifecycleRunner.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuRealChunkRunner.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuDiagnosticAudio.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuDiagnosticArtifacts.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/diagnostics/NpuFullSessionEvidence.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuDiagnosticWorkerService.kt" "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuDiagnosticProcessClient.kt" "$PROJECT_DIR/app/src/main/aidl/com/example/whisperrecorder/npu/INpuDiagnosticService.aidl"; do forbid_path "$p"; done
for t in controllerSelfTest connectedSelfTest performanceModeBenchmark stabilityStressTest scheduleDiagnosticWorkerRestart; do forbid_text "$t" "$PROJECT_DIR/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"; done
forbid_text 'nativeTranscribeJson' "$PARAKEET_BACKEND"; forbid_text 'nativeTranscribeJson' "$PARAKEET_NATIVE_JNI"; forbid_text 'NpuChunkTelemetry' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt"; forbid_text 'feature_fingerprint' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"; forbid_text 'fun selfTest()' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetTdtController.kt"; forbid_text 'syntheticFixture' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetPreprocessor.kt"
forbid_text ':benchmark' "$MANIFEST"; forbid_text 'Parakeet production diagnostic' "$MAIN"; forbid_text 'Single-chunk NPU speech check' "$SETTINGS_SUB"; forbid_text 'Automated NPU lifecycle suite' "$SETTINGS_SUB"
require_text 'DiagnosticLogger' "$SETTINGS_SUB"; require_text 'productionProfile' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuPerformancePolicy.kt"
# Removed engines/model weights stay out of source and APK inputs.
if grep -RFi --exclude='verify-source.sh' --exclude='LOCAL_ASR_NOTICE.txt' 'Voxtral Mini 3B 2507' "$PROJECT_DIR/app/src/main" >/dev/null; then
  fail "removed Voxtral catalog entry returned"
fi
if find "$PROJECT_DIR" -path '*/build/*' -prune -o -type f \( -name '*.gguf' -o -name '*.onnx' -o -name 'tokens.txt' -o -name '*.bin' \) -print | grep -q .; then
  fail "model weights must remain download-only"
fi
# Shell/YAML-size regression: verifier should stay maintainable and workflow must call it normally.
verify_lines="$(wc -l < "$0")"
test "$verify_lines" -lt 260 || fail "verify-source.sh grew to $verify_lines lines; prefer unit tests over source-string guards"
require_text 'bash "$PROJECT_DIR/scripts/verify-source.sh"' "$WORKFLOW"
for f in SpeakerTranscript.kt SpeakerModels.kt OfflineSpeakerService.kt; do require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/$f"; done; require_text 'sha256sum --check --status' "$WORKFLOW"
bash "$PROJECT_DIR/scripts/verify-speaker-diagnostics.sh"; require_file "$PROJECT_DIR/scripts/isolate-sherpa-onnx.sh"; require_text 'scripts/isolate-sherpa-onnx.sh' "$WORKFLOW"; require_text "require_apk_entry 'lib/arm64-v8a/libsherpa_ort.so'" "$WORKFLOW"; require_file "$PROJECT_DIR/scripts/patch-sherpa-elf.py"; require_file "$PROJECT_DIR/scripts/test-sherpa-elf-isolation.sh"; require_text "scripts/test-sherpa-elf-isolation.sh" "$WORKFLOW"; require_text 'patch-sherpa-elf.py' "$PROJECT_DIR/scripts/isolate-sherpa-onnx.sh"; forbid_text 'patchelf --replace-needed' "$PROJECT_DIR/scripts/isolate-sherpa-onnx.sh"; forbid_text '"**/libonnxruntime.so"' "$APP_BUILD"; require_text "require_apk_entry 'lib/arm64-v8a/libsherpa-onnx-jni.so'" "$WORKFLOW"; require_text 'getBoolean(KEY_SPEAKER_TIMESTAMPS, false)' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/data/AppPrefs.kt"
bash "$PROJECT_DIR/scripts/verify-speaker-count.sh"; bash "$PROJECT_DIR/scripts/verify-long-speaker.sh"; bash "$PROJECT_DIR/scripts/verify-omi-probe.sh"; bash "$PROJECT_DIR/scripts/verify-omi-transcription.sh"
bash "$PROJECT_DIR/scripts/verify-qwen-integration.sh"; printf 'v%s source verification PASSED\n' "$RELEASE_VERSION"
require_text 'markVerified(target, model)' "$LOCAL_MODELS"
require_text 'repairLegacyRestoreMarker(file, model)' "$LOCAL_MODELS"
forbid_text 'markVerified(restored, model)' "$LOCAL_MODELS"
printf 'versionName=%s versionCode=%s MainActivityLines=%s verifierLines=%s\n' "$VERSION_NAME" "$VERSION_CODE" "$main_lines" "$verify_lines"
