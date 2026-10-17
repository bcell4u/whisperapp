#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
fail() { echo "NPU interface verification failed: $*" >&2; exit 1; }
require_file() { test -f "$1" || fail "missing file: $1"; }
require_text() { grep -Fq -- "$1" "$2" || fail "missing $1 in ${2##*/}"; }
forbid_text() { if grep -Fq -- "$1" "$2"; then fail "forbidden $1 in ${2##*/}"; fi; }

# Guard against the Kotlin unresolved-reference and cross-layer signature failures
# observed in GitHub Actions build 102185496559 (2026-10-08).
NPU_WORKER="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeWorkerService.kt"
NPU_GRAPH="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuGraphContracts.kt"
NPU_CLIENT="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/NpuRuntimeClient.kt"
NPU_SESSION="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/AsrSession.kt"
require_text 'private fun configureDspSearchPath()' "$NPU_WORKER"
require_text 'private fun deviceJson(device: OrtEpDevice)' "$NPU_WORKER"
# ONNX Runtime's string maps cannot be passed to Android JSONObject(Map<Any?, Any?>)
# on Kotlin 1.9: the compiler rejects the Java generic signature.
require_text 'private fun jsonStringMap(entries: Map<String, String>): JSONObject' "$NPU_WORKER"
require_text 'jsonStringMap(device.epMetadata)' "$NPU_WORKER"
require_text 'jsonStringMap(device.epOptions)' "$NPU_WORKER"
forbid_text 'JSONObject(device.epMetadata)' "$NPU_WORKER"
forbid_text 'JSONObject(device.epOptions)' "$NPU_WORKER"
require_text 'private fun validateSessionNames(session: OrtSession, contract: NpuGraphContract)' "$NPU_WORKER"
require_text 'private fun elapsedMs(startedNs: Long)' "$NPU_WORKER"
require_text 'internal fun encoderPaddedMaxAbs(encoded: FloatArray, validEncodedFrames: Int)' "$NPU_GRAPH"
require_text 'fun transcribePreparedChunk(preparedId: Long): JSONObject' "$NPU_CLIENT"
require_text 'client.transcribePreparedChunk(chunk.id)' "$NPU_SESSION"
forbid_text 'client.transcribePreparedChunk(chunk.id,' "$NPU_SESSION"
require_file "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/npu/NpuEncoderPaddingTest.kt"
# A successful NPU inference may decode to blank on a silence/decoder edge case.
# Never make one blank chunk fatal to an hour-long recording again.
forbid_text 'require(text.isNotBlank()) { "NPU returned an empty transcript" }' "$NPU_SESSION"
require_text 'blankTranscript=${text.isBlank()}' "$NPU_SESSION"
require_text 'action=retry-inline' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt"
require_text 'action=marked-untranscribed' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt"
require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/NpuBlankTranscriptRecovery.kt"

require_text 'SESSION partial transcript saved' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt"

# v6.5.8: timing must never jeopardize successful NPU speech recognition.
require_text 'onTokenEmitted(token, frameIndex)' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/npu/ParakeetTdtController.kt"
require_text 'decodeTimedWords(execution.emittedTokenIds, execution.emittedFrames' "$NPU_WORKER"
require_text '.put("timing_error"' "$NPU_WORKER"
require_text 'timingStatus=' "$NPU_SESSION"
require_text 'npu word-sync chunk=' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/TranscriptionService.kt"
require_text 'saveSpeaker(context, item.id, timeline)' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt"

# v6.5.9: a stale speaker timeline must not hide available NPU word timing.
require_file "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/transcribe/SpeakerWordSync.kt"
require_text 'SpeakerWordSync.choose(transcript, speakerCues, regularCues)' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/TranscriptViewerScreen.kt"
require_text 'speakerRegularTimingPath = recording.transcriptTimingPath' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/SpeakerTranscriptMenu.kt"
require_file "$PROJECT_DIR/app/src/test/java/com/example/whisperrecorder/transcribe/SpeakerWordSyncTest.kt"

# v6.5.10: mixed word/phrase timing must remain readable.
require_text 'val wordCues = timeline.cues.count' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt"
require_text 'SpeakerTranscript.groupWords(labeled)' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt"
require_text 'speaker transcript formatting cues=' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt"
forbid_text 'timeline.cues.all { cue ->' "$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/speaker/OfflineSpeakerService.kt"
# v6.5.11: the highlight is foreground-only; rapid words must not flash a backdrop.
HIGHLIGHT="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/TranscriptLazyContent.kt"
COMPACT="$PROJECT_DIR/app/src/main/java/com/example/whisperrecorder/ui/SynchronizedAudioPlayer.kt"
require_text 'fade.animateTo(1f, tween(135' "$HIGHLIGHT"
require_text 'outgoingStrength = fade.value' "$HIGHLIGHT"
require_text 'tint(previous, outgoingStrength * (1f - fade.value))' "$HIGHLIGHT"
forbid_text 'background = lerp(' "$HIGHLIGHT"
forbid_text 'activeHighlight' "$HIGHLIGHT"
forbid_text 'Crossfade(' "$COMPACT"
require_text 'CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)' "$COMPACT"
