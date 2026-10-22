# Lab 6.6.17: Attention execution-path control (host-only)

The completed 6.6.16 host experiment showed that optimized and portable x86-64 CPU backends produce different logits, but **both retain microbatch-dependent numerical variation** (Q8 SIMD mean RMSE 0.1987; Q8 portable 0.1876; BF16 SIMD 0.0522; BF16 portable 0.0461). 6.6.17 tests requested Flash Attention **disabled vs enabled** under identical pinned host model/CPU parameters, across Q8/BF16, unified/three-stage, `n_ubatch=128/256`, five synthetic numerical checkpoints and replay tests.

This is a diagnostic control, **not proof of which path is correct or that the backend used distinct kernels**. See [LAB_6_6_17_ATTENTION_PATH_ISOLATION.md](LAB_6_6_17_ATTENTION_PATH_ISOLATION.md) and [LAB_6_6_17_VALIDATION.md](LAB_6_6_17_VALIDATION.md). Official M-RoPE/numerical parity remains unverified. The Android QNN, ARM64 dot-product decoder, stitcher and production code are unchanged. GitHub Actions must compile/run the actual experiment and Lab APK.

Push source to separate Lab `main`; retrieve `qwen-6.6.17-host-attention-path-*` from Actions and share it. No phone installation or private lecture upload is required.

---

# Lab 6.6.16: CPU instruction-set host diagnostic

Version 6.6.16 adds a synthetic-only host experiment comparing AVX2/FMA-enabled and portable x86-64 llama.cpp builds for the same Q8_0 and BF16 GGUFs. It does **not** change Android transcription or establish official Qwen M-RoPE parity. See [LAB_6_6_16_CPU_ISA_ISOLATION.md](LAB_6_6_16_CPU_ISA_ISOLATION.md).

# WhisperRecorder 6.6.15 — Q8 vs BF16 precision/batching isolation (host-only)

The successful 6.6.14 test showed that 128-vs-256 microbatch changes cause
reproducible differences for original audio embeddings, zero embeddings and
156 plain text tokens. The effect is **not unique to audio embeddings**.
This release compares the SAME host llama.cpp v0.6.0, CPU threads and synthetic
prompt under Q8_0 and published BF16 decoder GGUF weights, using only the
meaningful 128/256 and unified/three-stage configurations. Within-model
microbatch differences are meaningful; **Q8 and BF16 logits are not expected to
match**. See `LAB_6_6_15_PRECISION_ISOLATION.md`.

Push to `main`; GitHub workflow **Qwen host precision and batch isolation**
produces `qwen-6.6.15-host-precision-batch-*` with `PRECISION_BATCH_REPORT.json`
and `PROVENANCE.txt`. The GitHub runner temporarily downloads two SHA-pinned
public decoder weights (Q8 ~805 MB and BF16 ~1.51 GB); neither model is packaged
in the Android app, source archive or output artifact. No phone test necessary.
Existing Android native decoder and production Parakeet paths are unchanged.
Official Qwen3-ASR numerical/M-RoPE parity is NOT YET VERIFIED.

---

# WhisperRecorder 6.6.14 — input-kind isolation (host-only)

**Android transcription is unchanged.** The real 6.6.13 GitHub artifact showed
microbatch thresholds at the 156-frame middle input and 171-position complete
prompt. This release runs three synthetic-only input controls (original features,
zero audio embeddings, ordinary text filler tokens), using fixed 128/256
microbatches, the same Q8 GGUF, one pinned llama.cpp revision and 4 CPU threads.

The GitHub workflow **Qwen host input modality isolation** automatically runs
when the updated project is pushed to `main`; upload the
`qwen-6.6.14-host-input-kind-*` artifact for analysis. No phone test is needed.
See `LAB_6_6_14_INPUT_KIND_ISOLATION.md` for controls and limits. The runner is
not an independent implementation of model mathematics, and M-RoPE remains
unqualified. The full Android build still requires successful GitHub Actions.

---

# WhisperRecorder 6.6.13 — microbatch thresholds around audio and prompt lengths

The 6.6.12 GitHub artifact showed microbatch-sensitive logits but no sensitivity
to one versus four CPU threads. This release holds thread count at four and
compares 64/128/160/192/256 microbatches around the 156-frame audio tensor and
171-position synthetic prompt. The **Android decoder remains unchanged**.
Automatic host workflow output: `qwen-6.6.13-host-microbatch-threshold-*`.
Read `LAB_6_6_13_MICROBATCH_THRESHOLD.md` for controls and limitations.
Independent official Qwen/M-RoPE parity remains unqualified.

---

# WhisperRecorder 6.6.12 — same-host execution sensitivity

**The on-device Android ASR path remains unchanged.** 6.6.11 demonstrated that all four prompt prefill arrangements produce distinct host logits with one fixed four-thread / 256-token microbatch configuration; the four arrangements agreed on top-1 tokens and were repeatable, but none were numerically identical to the S26. The new 6.6.12 GitHub-hosted test compares *unified* versus *three-stage* input batching under **three fixed compute configurations**:

- Baseline: n_batch=256, n_ubatch=256, threads=4.
- Microbatch control: n_batch=256, n_ubatch=64, threads=4.
- Thread control: n_batch=256, n_ubatch=256, threads=1.

The exact same SHA-pinned Q8 model, S26 synthetic features, prompt, positions, and teacher-forced tokens are used; 5 checkpoints per execution mode, plus baseline and unified/micro64 replay controls. Output: `EXECUTION_SENSITIVITY_REPORT.json` in artifact `qwen-6.6.12-host-execution-sensitivity-*` generated by the **Qwen host execution sensitivity** workflow.

Tests diagnose *numerical execution sensitivity*, not Qwen correctness. If the host changes under these controls, that may reflect different kernels, floating-point reduction orders, or model-runtime behavior. It is **not** a proof that the Android calculation or M-RoPE is wrong. Independent official-model parity and the source of the S26-vs-host discrepancy remain unverified. No model weights, lecture audio, transcript, or signing materials are packaged.

See `LAB_6_6_12_EXECUTION_SENSITIVITY.md`. Commit this source to your Lab repository's `main`: Android autorelease is still configured and the synthetic host workflow triggers on changed diagnostic source.

---

# WhisperRecorder 6.6.11 — decoder prompt boundary isolation

The 6.6.10 host result demonstrated that extended three-stage prefill and standard three-stage prefill were byte-identical for all five synthetic checkpoints, while unified prefill differed. **6.6.11 isolates each of the two internal cut points** using additional extended-API modes `[prefix+audio][suffix]` and `[prefix][audio+suffix]`. A GitHub-hosted reference workflow compares five teacher-forced checkpoints, replay consistency, and the first-token checkpoint from the S26. Normal Android inference is unchanged. Report: `BOUNDARY_ISOLATION_REPORT.json`. This is not official Qwen or M-RoPE parity. See `LAB_6_6_11_BOUNDARY_ISOLATION.md`.

# WhisperRecorder 6.6.10 — same-host chunking vs API isolation

The 6.6.9 CI result found numerically different logits between standard **three-stage** batch handling and extended **unified** batch handling. The extended unified path was closer to the S26 first-token checkpoint (RMSE 0.3133 vs 0.4713) but neither established official correctness.

**New in 6.6.10:** execute the extended batch API in **three stages** with the exact same prefix/audio/suffix boundaries as the standard API, then compare all three paths on the same host with the same model, parameters, teacher-forced tokens and synthetic features. The GitHub workflow starts automatically after pushing diagnostic changes to `main`. See [LAB_6_6_10_CHUNK_ISOLATION.md](LAB_6_6_10_CHUNK_ISOLATION.md).

**Normal Android transcription remains unchanged.** No S26 test or new model download is necessary. Host comparisons share llama.cpp mathematics and **cannot** prove official Qwen M-RoPE parity.

# WhisperRecorder 6.6.9 — same-host batching path numerical isolation

**Android ASR decoding is unchanged.** 6.6.9 adds an independent hosted extended mixed-batch decoder in addition to the 6.6.8 standard split-batch decoder. A GitHub Actions workflow automatically runs both paths on the same CPU, llama.cpp commit, GGUF and synthetic input when test code changes on `main`. It exports `BATCH_PATH_REPORT.json` and failure data, explicitly leaving independent model mathematics and official M-RoPE unverified. See [LAB_6_6_9_SAME_HOST_BATCH_PATHS.md](LAB_6_6_9_SAME_HOST_BATCH_PATHS.md). No new on-device test is needed.

# WhisperRecorder 6.6.8 — hosted reference decoder diagnostic

This update adds a **separate manually triggered GitHub Actions reference runner**. Normal on-device QNN encoding and four-thread dotprod Q8 decoding are byte-for-byte unchanged from 6.6.7. The Lab APK is still produced automatically on a push to `main`. The host reference is a second implementation of the **bridge/input batching** and uses the standard `llama_batch`/`llama_decode` API, **but uses the same underlying upstream llama.cpp mathematical kernels**. It cannot by itself certify official Qwen M-RoPE or numerical correctness.

To run once: GitHub repo > **Actions > Qwen host decoder reference > Run workflow**; choose `v0.6.0` first. It downloads the exact public Q8 GGUF into the ephemeral runner, verifies SHA-256, compiles a standalone CPU host program, computes 5 logits checkpoints (prefill + 4 fixed synthetic teacher-forced tokens), tests repeatability and perturbed audio position axes, compares the prefills to the S26 export and saves `REFERENCE_REPORT.json`, vectors, and `PROVENANCE.txt` to an Actions artifact. The 800MB GGUF is never added to the repository or artifact.

See `LAB_6_6_8_HOST_REFERENCE.md` for details and interpretation. No new phone transcription is needed.

# 6.6.7 experimental numerical decoder checkpoint

The new **synthetic-only** decoder-verification path exports the first-token FP32 logit vector and tests bitwise numerical replay across a KV-cache reset. It keeps lecture inference and the existing one-second-overlap toggle unchanged. See [LAB_6_6_7_LOGIT_CHECKPOINTS.md](LAB_6_6_7_LOGIT_CHECKPOINTS.md). Independent M-RoPE/logit parity is NOT established.

## Previous changes

# 6.6.6 experimental prompt correction

See `LAB_6_6_6_PROMPT_AUDIT.md` for the corrected Qwen `system` / `user` prompt tokens, safety gates, and one-run accuracy comparison. v6.6.5 remains the working baseline.

# Experimental Qwen GGUF Lab v6.6.7 (from clean v6.5.22)

**New in 6.6.5:** A synthetic-only **VERIFY DECODER** mode checks QNN feature determinism, native two-pass KV reset and decoder output reproducibility, and optional independent token-reference agreement. It exports synthetic FP32LE features to `Download/SFSU/Whisperapp/Verification` and writes token-free results to `Download/SFSU/Whisperapp/Logs`. It never claims M-RoPE/logit parity without authoritative numerical comparisons. See [LAB_6_6_5_DECODER_VERIFICATION.md](LAB_6_6_5_DECODER_VERIFICATION.md) for positive and negative outcomes and the one-run test plan. This source is not a locally compiled APK.

**Resource upload repair (CI run 107112949945):** If GitHub reports `MISSING app/src/main/res/xml/lab_data_extraction_rules.xml`, restore the complete `app/src/main/res` folder from this source ZIP; see [LAB_6_6_RESOURCE_RECOVERY.md](LAB_6_6_RESOURCE_RECOVERY.md). The Lab verifier checks all 24 XML resources together.

**Existing GitHub repository migration:** If `.github/workflows/build.yml` remains from production, remove it with `bash scripts/activate-lab-workflow.sh` in a Git clone/Codespaces checkout (or delete the file in GitHub's web editor). See [LAB_GITHUB_MIGRATION.md](LAB_GITHUB_MIGRATION.md). This prevents the production build from running instead of the new Lab auto-release workflow.

**This repository is a separately installable experimental Lab source project.** See [LAB_6_6_AUTORELEASE.md](LAB_6_6_AUTORELEASE.md) for setup, automatic GitHub prereleases, original signing-secret compatibility, required user-supplied models, and unverified Android/phone gates. The historical documentation below describes the clean production v6.5.22 baseline rather than the experimental launcher. The 6.6.0 Lab has produced one on-device 19-segment transcript; the new 6.6.1 changes are not yet phone verified.

## What's new in 6.6.1

- Removes only the literal `language English<asr_text>`-style Qwen protocol header from the beginning of each separately decoded segment. Transcription inference, four-thread dot-product decoder, Q8 GGUF and QNN model paths are unchanged.
- Automatically writes a diagnostics-only `.log` after completed, failed, or cancelled transcription attempts to **Internal storage/Download/SFSU/Whisperapp/Logs** through Android MediaStore Downloads. The app shows the saved filename or a non-sensitive save error.
- Logs contain stage/timing/token/EOS/RSS/provider-reported metrics; **not** transcripts, recordings, model file paths or token IDs. There are no additional storage permissions.
- Bumps version to `6.6.1-dev` / code `66001`. A successful push to `main` runs GitHub Actions and publishes a new isolated prerelease APK. APK compilation and this new logging behavior must still be verified for 6.6.1.

See [LAB_6_6_1_LOGGING.md](LAB_6_6_1_LOGGING.md) for logs, setup and test checklist.

---

# WhisperRecorder

WhisperRecorder is an offline-first Android audio recorder and transcription app. **Current source release: v6.5.22**, Android `versionCode=65022`. The authoritative version is `version.properties`; do not infer the version from a historical document or ZIP filename.

## Transcription and acceleration

The app maintains these independent offline recognition backends:

| Model | Runtime | Acceleration |
| --- | --- | --- |
| Parakeet TDT 0.6B v3 FP16 | ONNX Runtime 1.27 / Qualcomm QNN 2.50 | Snapdragon NPU |
| Parakeet TDT 0.6B v3 Q8_0 | parakeet.cpp / Adreno OpenCL | GPU |
| Omi Med STT v1 Q8_0 | shared parakeet.cpp / OpenCL runtime | GPU |
| Omi Med STT v1 FP16 QNN (Experimental) | ONNX Runtime 1.27 / Qualcomm QNN 2.50 | Snapdragon NPU |
| Qwen3-ASR 1.7B | transcribe.cpp | CPU |
| Whisper | whisper.cpp | CPU |

For a first-time user without a manual engine preference, the app prefers a verified, installed NPU model, then an installed GPU model, then an installed CPU model. **Manual engine selections are never overridden.** This is model selection, not automatic cross-backend failover during inference. The GPU and CPU tiers each show a separate, persistent one-time in-app warning about processing temperature and battery use. No additional Automatic selection item is exposed in the model picker.

Parakeet NPU emits approximate token/frame timing during existing inference. The app derives word-sized timeline cues for regular and speaker transcript playback, word highlighting, and seeking. If native timing is missing or fails validation, timing falls back to the existing estimated cues and records a diagnostic event. The UI uses a calm active-word text-color transition with subtle size emphasis.

## Recording and speaker transcripts

Recordings, regular transcripts, speaker transcripts, and optional word-timing metadata are kept separately. A transcript-only or speaker-transcript-only Library entry remains available for deletion recovery. Audio and transcript deletion is attempted independently, and partial failures are reported and logged.

Optional, offline speaker diarization uses Pyannote Segmentation 3.0 INT8 and ERes2Net through sherpa-onnx 1.13.8 on CPU. Three-minute audio windows with 30-second overlap support longer recordings; speaker identity is linked across windows using embeddings and overlap evidence. Expected speaker count is Automatic or 1–6. Speaker changes can be attributed at word granularity when the regular transcript has word timing, but overlap, speaker boundaries, and timestamps remain approximate.

The regular Recording submenu displays transcript and media share icons in the upper-right of its transcript preview card. The dedicated speaker submenu shows only the speaker-transcript share icon on its preview card, with expected speaker count, regeneration, renaming, and diagnostics grouped under Speaker settings.

For the full technical details and caveats, see [NPU.md](NPU.md) and [SPEAKER_DIARIZATION.md](SPEAKER_DIARIZATION.md).

## GitHub Actions build

The complete source tree includes `.github/workflows/build.yml`, the Android Gradle modules, native modules, version metadata, and validation scripts. The workflow provisions the Android SDK/NDK, obtains and verifies pinned native dependencies, runs JVM tests, assembles an APK, audits it, and uses the configured permanent signing key for a signed production build. Review [SIGNING.md](SIGNING.md) and [VERSIONING.md](VERSIONING.md) before publishing.

Before building, run `bash scripts/verify-source.sh` if a local shell is available. The official Android build runs in GitHub Actions; passing static source checks alone is **not** proof of compilation or device operation.

### Updating GitHub without replacing every file

**Do not drag all files from a complete source ZIP onto an existing github.dev project for a routine update.** The editor offers to replace each overlapping filename even when its bytes are identical. Instead, use the accompanying `*.patch` with `git apply` in a local Git checkout or a GitHub Codespaces terminal; review the diff, then commit and push. If using github.dev alone, extract and copy only the files in the companion *changed-files-only* ZIP and perform any documented file deletions once. github.dev itself does not provide a terminal for `git apply`.

A full source ZIP is a recovery or fresh-clone snapshot. An incremental patch is the normal update mechanism. This avoids hundreds of file-overwrite confirmations and makes the real Git diff visible.

## Development and diagnostics

Transcription, speaker processing, acceleration selection, and individual Library deletion results emit diagnostic messages without intentionally recording transcript content. See the app's diagnostic export when troubleshooting a real device. Do not remove model or preference compatibility logic solely to reduce file count: older recordings and installed model packages need those paths.

Historical one-off CI fix notes and per-version Markdown files were removed from the release source tree for v6.5.14; the key ongoing requirements now live in these five reference documents. An archival copy can be maintained outside the build tree.

## Full transcript viewer (v6.5.14)

The full regular and speaker transcript viewers focus on playback and word-level highlighting. Copy, Share, and Prompts shortcuts are no longer displayed in the viewer toolbar. Transcript/audio sharing remains in the preview-card actions in the recording submenu; the speaker preview card shares only the speaker transcript. The signed Android build runs through the existing GitHub Actions workflow.

### Historical: Omi QNN package downloader and compatibility test (v6.5.16)

From **Models & downloads → Qualcomm NPU**, choose **Download Omi NPU assets**. The
existing foreground, resumable model-download service retrieves the model manifest,
`joint.bin`, `predictor.bin`, `encoder.bin`, `token_embedding.npy` and `tokenizer.model`
from `drinnird/omi-med-stt-v1-snapdragon-sm8850-v81`. The installed package is
stored at `Whisperapp/Models/omi-med-stt-v1-snapdragon-sm8850-v81/qnn-2.50-experimental/`.
The app validates the manifest against pinned sizes and SHA-256 checksums and
verifies copied SAF documents before marking the package ready. The neural contexts
total approximately **1.24 GB**, and temporary staging requires additional free space.
Downloads can resume. The chosen transcription model is **not changed**.

After installation, open **Settings → Diagnostics → Omi Med NPU compatibility**
and tap **Test downloaded Omi NPU assets**. The default probe tests the small
joint and predictor QNN graphs; enable the encoder checkbox to test the large
encoder separately. The test reads the installed package directly and no longer
opens the document-tree folder picker. It records exact per-graph failure stages,
provider/session errors and worker logs. It runs synthetic inputs only; no
Omi NPU transcription engine is enabled yet. That initial source release still required Android build and on-device validation; subsequent releases have been tested. Consult [NPU.md](NPU.md) for limitations.

### Historical: Omi Med NPU transcription (v6.5.17)

At v6.5.17, the QNN downloader and compatibility test were preserved. Installed Omi
NPU assets may now be selected as a **separate** experimental English medical
transcription backend in Models & downloads. It runs the encoder, predictor and
joint QNN context graphs on the Hexagon V81 NPU through existing ORT-QNN,
with 12-second windows and host TDT/SentencePiece decoding. **This was the historical v6.5.17 status, not the current release status.** Consult [NPU.md](NPU.md) for the model's failed FP16 numerical
acceptance gate and detailed limitations. Parakeet NPU and Omi GPU selections
remain unchanged and available for comparison.

### Omi NPU quality diagnostics (v6.5.18)

Native word cues are now aligned against the complete cleaned transcript **before** 1-second overlap
timestamps are clipped. Conservative exact-token subsequence alignment also retains surviving
native word times when repetitive clauses were removed from inside a chunk. Diagnostics report
real alignment strategy, discarded/trimmed overlap words, and text-free failure reasons rather
than guessing alignment from cue counts. Failed text matches still use speech-activity timing.
The Omi NPU worker records FP32 feature/PCM summary statistics, mask counts and decoder
termination/guard metrics **without including recognized transcript text or token IDs**.
These are aids to NeMo comparison, not numerical parity proof. Correctness of clinical
terms and units remains unqualified.

### Source audit and maintenance (v6.5.22)

Moves model inventory initialization and library/detail model-path checks off the Compose UI thread, guards NPU removal against
concurrent model operations, scopes NPU staging cleanup to the selected model, and
optimizes long-transcript cue partitioning. See `VERSIONING.md` for the change history.
The existing signed release workflow remains authoritative for Android compilation.

## Pending v6.6.2 model persistence update

Version `6.6.2` (code `66002`) stages an uninstall-resistant backup under
`Internal storage/Download/SFSU/Whisperapp/Models` for the downloaded QNN
encoder and official Q8 GGUF. The Lab includes explicit **Back up verified
models** and **Restore models from Downloads** actions. See
`LAB_6_6_2_MODEL_PERSISTENCE.md` for details, privacy/storage caveats and the
stable-signing requirement. This is source staged for a future CI build, NOT a
compiled or phone-tested v6.6.2 APK.


## Staged v6.6.3: optional one-second overlap accuracy experiment

Version `6.6.3` (code `66003`) retains the v6.6.2 public Downloads model backup
and 6.6.1 automatic privacy-safe diagnostic logs. The Lab UI enables a **one-second
overlap** checkbox by default; uncheck to reproduce the original 12-second,
zero-overlap segment path. Both paths use the identical QNN encoder and v0.4.96
four-thread ARM64 dot-product Q8_0 decoder. No model or quantization change.

After inference, an exact whole-word seam matcher drops duplicated phrases only
when two or more words match at the boundary; uncertain cases remain visible.
The Results screen and the automatic log record `overlap_seconds`,
`stitched_duplicate_words`, `stitched_boundaries`, and `unmatched_boundaries`
without logging audio, spoken words, transcripts, or private paths. Processing
longer than 12 seconds is still segmented, and the four-minute cap remains.
This is an **unvalidated accuracy experiment**, not a proven accuracy gain. See
`LAB_6_6_3_OVERLAP.md` for the design and one-run evaluation instructions.

## Staged v6.6.4: Parakeet-style conservative stitching

Version 6.6.4 (code 66004) keeps the full 6.6.3 hybrid pipeline and improves only optional overlap transcript stitching. It uses production Parakeet exact-word matching (24-word context, 12-character 2-word safeguard) and existing `TranscriptCleaner.cleanChunk` pathological repetition cleanup. See `LAB_6_6_4_STITCHING.md` and `VALIDATION_6_6_4.md` for checks and compile status.
