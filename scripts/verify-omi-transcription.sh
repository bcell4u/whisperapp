#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/app/src/main/java/com/example/whisperrecorder"
for item in 'OMI_NPU("omi_npu")' 'this != PARAKEET_NPU && this != OMI_NPU && this != QWEN3_HYBRID'; do
  grep -Fq "$item" "$BASE/transcribe/AsrEngine.kt"
done
for item in 'fun prepareOmiRuntime()' 'fun transcribeOmiChunk(' 'experimental_encoder_numeric_gate_not_passed' 'stagedOnly = true' 'omi transcription chunk FAIL stage=' 'OmiSentencePiece.fromModel' 'OmiTdtController.decode'; do
  grep -Fq "$item" "$BASE/npu/NpuRuntimeWorkerService.kt"
done
for item in 'String prepareOmiRuntime()' 'String transcribeOmiChunk(' ; do
  grep -Fq "$item" "$ROOT/app/src/main/aidl/com/example/whisperrecorder/npu/INpuRuntimeService.aidl"
done
for item in 'AsrEngine.OMI_NPU -> OmiNpuSession(context)' 'override fun transcribe(samples: FloatArray'; do
  grep -Fq "$item" "$BASE/transcribe/AsrSession.kt"
done
grep -Fq 'OmiNpuPackageStore.selectForTranscription(context)' "$BASE/ui/models/ModelsScreen.kt"
grep -Fq 'OMI_NPU_MAX_SECONDS = 12' "$BASE/transcribe/SegmentationPolicy.kt"
grep -Fq 'AsrEngine.OMI_NPU, AsrEngine.QWEN3_HYBRID -> 1' "$BASE/transcribe/SegmentationPolicy.kt"
grep -Fq 'model_qualification' "$BASE/npu/NpuRuntimeWorkerService.kt"
test -f "$ROOT/app/src/test/java/com/example/whisperrecorder/npu/OmiTdtControllerTest.kt"
test -f "$ROOT/app/src/test/java/com/example/whisperrecorder/npu/OmiPreprocessorTest.kt"
test -f "$ROOT/app/src/test/java/com/example/whisperrecorder/npu/OmiSentencePieceTest.kt"
test -f "$ROOT/app/src/test/java/com/example/whisperrecorder/transcribe/OmiNpuSegmentationTest.kt"
echo 'Omi NPU transcription source guards PASSED'
