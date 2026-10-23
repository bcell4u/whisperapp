# Qualcomm NPU / ORT-QNN production integration

WhisperRecorder uses a maintained NPU path for Parakeet v3, plus a separately selectable Omi Med NPU path. The Omi user-facing card has no Experimental label, but its original encoder numerical qualification gate remains unresolved:

- durable Hugging Face package in `Whisperapp/Models`;
- existing model install/repair/verification flow;
- isolated `:npu` worker;
- ONNX Runtime 1.27.0 + Qualcomm QNN EP 2.6.0 + QNN runtime 2.50.0;
- CPU neural fallback disabled;
- encoder plus linked projector/predictor/joint sessions kept warm;
- read-only SharedMemory PCM IPC;
- predictor embedding mmap'd from the durable archive;
- exact Parakeet preprocessing, tokenizer, and TDT controller;
- no Direct-QNN/JNI or `qnn-net-run` production path.

## Storage

The package remains single-copy and uninstall-resistant under `Whisperapp/Models/parakeet-tdt-0.6b-v3-sm8850-v81-fp16/<revision>/`. When ORT-QNN cannot reopen a SAF-backed QNN context directly, one context is copied to app-private transient storage, loaded, and deleted immediately. Production startup requires zero persistent duplicate model bytes and zero staged context bytes after session creation.

## Full-recording path

The NPU backend is a normal selectable `AsrEngine` (`parakeet_npu`) using the existing `TranscriptionService` lifecycle:

1. `PcmDecoder` streams/resamples source media to mono 16 kHz;
2. Parakeet uses 40-second chunks with 2-second overlap;
3. the next chunk is decoded and preprocessed while the current chunk executes on HTP;
4. PCM crosses process boundaries through read-only SharedMemory;
5. encoder -> projector -> exact TDT predictor/joint -> tokenizer executes on the warm runtime;
6. `TranscriptCleaner` removes boundary duplication;
7. the existing transcript/timeline save path completes the job.

Cancellation is honored at graph/controller boundaries. If the isolated worker dies, the client rebuilds the runtime and retries the current chunk once.

## Performance policy

Production remains provider-default under normal thermal conditions and uses the existing conservative thermal fallback. The synthetic performance-mode matrix and lifecycle benchmark suite have been removed from the final application.

## Accepted decoder optimization

The accepted joint hot loop retains one reusable 8,198-float destination and a reusable FloatBuffer read view. Bulk transfer remains because direct indexed `FloatBuffer.get(index)` scanning was measured substantially slower. The redundant pre-controller finite scan is removed; the TDT argmax performs the single validation/selection pass and preserves first-maximum tie behavior.

v6.3.22 additionally removes the old diagnostic controller history arrays and SHA-256 generation. These were not part of token selection or state transitions and are no longer needed after performance validation.

## Final logging

Automatic benchmark/evidence ZIP generation is removed. Normal `DiagnosticLogger` text/error logging remains and records enough production state to troubleshoot real jobs: performance profile, decoder topology, shared-EP state, QNN context count, critical/preprocessing/encoder/projector/decoder timing, hot-loop mode, joint steps, emitted-token count, valid frames, and worker recovery status.

No audio, transcript content, or token IDs are added to normal diagnostic logs.

## Historical Omi Med STT v1 QNN package (v6.5.16)

Models & downloads uses the **existing `ModelDownloadService`** for the Omi QNN
package at `drinnird/omi-med-stt-v1-snapdragon-sm8850-v81`. The manifest and five
pinned runtime artifacts are downloaded with resume and SHA-256 verification, then
archived under `Whisperapp/Models/omi-med-stt-v1-snapdragon-sm8850-v81/qnn-2.50-experimental/`.
The 1.216 GB encoder is part of the download; allow space for both its staging copy
and the durable SAF archive until copying has finished. A READY marker is written
only after downloaded files and archived copies are verified. Interrupted downloads
retain partial files for resumption. Changing the manually selected ASR backend is
out of scope.

The separate Omi QNN synthetic compatibility probe was a historical 6.5.x
experiment and is **not part of the v6.7 application**. Its runtime, probe
button, and related unit test are retired. The published Omi encoder previously
failed its FP16 numerical acceptance gate; clinical transcription accuracy is
not qualified by those historical tests. This does not change the independent
Parakeet NPU or Omi OpenCL transcription paths.

## Historical Omi NPU transcription (v6.5.17)

Omi Med STT v1 QNN is an additional **explicitly selected**, English-only, experimental
transcription engine (`omi_npu`). After installing the 2.50/V81 assets with the
existing downloader, choose **Use** on the Omi Med NPU card under Models & downloads.
It runs in the same isolated `:npu` worker, **without a CPU neural fallback**,
using the three separately compiled QNN context graphs, 12-second audio windows
with 1-second overlap, host-side 1025×640 FP32 token embeddings and NeMo
SentencePiece decoding. The current Parakeet v3 NPU and Omi GPU runtime
implementations remain independent.

**Historical v6.5.17 limitations:** the encoder failed its original builder's
FP16 numerical qualification gate. Subsequent releases have passed on-device real-audio
transcription tests, but clinical-accuracy qualification remains outstanding. The CPU feature extractor uses the
app's 16kHz, 128-Mel, 25ms/10ms, 512-FFT, 0.97 preemphasis implementation
adapted to 1201/601/301/151 physical frame shapes; it is not yet proven
numerically identical to the **original Omi v2 NeMo preprocessor**. Recurrent
states reset on every 12-second audio window. Word timing is estimated from
80ms decoder frames, not independently aligned. Overlap cleanup may remove
repetitions or alter word cues. Do not rely on this engine for medication names,
doses, diagnoses or clinical decisions without careful independent review.

The new on-device runtime emits detailed `omi transcription prepare` and
`omi transcription chunk` records in exported Diagnostics (context-load stage,
Tensor contract, QNN session errors, stack traces, chunk duration, preprocessing,
encoder and decoder runtimes, token counts and timing status). The original
synthetic QNN compatibility probe is retained. Successful context loading does
**not** imply transcription fidelity.

## Omi NPU parity and timeline diagnostics (v6.5.18)

After confirmed 20/20 chunk end-to-end inference, the experimental Omi engine now exports
per-chunk preprocessing assumptions, PCM RMS/peak/clipping percentage, normalized feature
RMS/peak/mean, valid mel frames, three mask counts, nonfinite and nonzero-padding counts,
and TDT stop reason/symbol-limit advances. No decoded text or token IDs appear in diagnostics.
The full input feature array is not exported; these summary statistics alone cannot establish
parity with NeMo or correct the builder's failed FP16 encoder numerical gate.

Transcript timeline alignment now occurs on untrimmed native segments and saved cleaned text
before overlap clipping. Exact-prefix and guarded exact-subsequence preservation are supported;
true mismatches fall back to speech-activity timing with a text-free reason. Log fields include
mode, alignment strategy, native/aligned word count, overlap-discard/clip and dropped-cleanup count.
Word timestamps still inherit experimental 80ms TDT-frame timing uncertainty.

## Current maintenance status (v6.5.22)

The v6.5.19 decoder symbol-guard correction and v6.5.20 offline parity
test tooling are retained. The v6.5.21 unified model cards remain unchanged.
v6.5.22 limits deletion of Parakeet NPU staging files to that package's
revision directory, preserving the separate Omi staging directory; Omi
removal likewise cleans only its own staging files. No QNN graph, model
asset, decoder, or audio frontend behavior changes in this release.
