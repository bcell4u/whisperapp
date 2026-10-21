# WhisperRecorder Qwen GGUF Lab 6.6.5: decoder verification

## Purpose / what this build can and cannot prove

**This source adds an independent verification *path*, not a pre-certified reference**.

- Normal QNN + four-thread ARM64 dotprod Q8_0 transcription, 12-second windows,
  optional 1-second overlap and stitching are untouched.
- The VERIFY DECODER button runs the packaged synthetic 128x1200 mel fixture
  through the real QNN encoder **twice**, verifies exact 156x1024 FP32LE feature
  equality, then submits the first output twice to the pinned, existing native
  decoder as a two-segment manifest. The native implementation loads the model
  **once** and clears KV cache between segments. Private token traces are
  consumed in the worker and never written to the report.
- Model and native ELF SHA-256 hashes and the SHA-256 of the *exact* NPU feature
  tensor are recorded. A copy of the **synthetic-only** FP32LE tensor is exported
  to `Download/SFSU/Whisperapp/Verification` so an independent decoder can use
  byte-identical features. **No lecture audio or lecture embeddings are exported.**
- Logs go to `Download/SFSU/Whisperapp/Logs/WhisperRecorder-QwenVerify-*.log`.
  Logs have the first differing token INDEX, never the token values or transcript.
- Optional `Import independent reference token JSON` accepts a private file
  containing an independently generated token ID sequence. The app requires
  identical model and synthetic-feature SHA-256 hashes before comparing IDs.
  Merely claiming an origin in the JSON does NOT prove its authenticity.
- Even `PASS_REFERENCE_TOKENS` establishes token agreement for one fixture,
  not full logits/teacher-forced parity, upstream encoder parity or canonical
  4-axis M-RoPE correctness. Those remain UNQUALIFIED in every report.

## First device test (no extra lecture or thermal sweeps)

1. Install signed 6.6.5 APK (same application ID and signing identity as 6.6.4
   if you want to preserve private models; otherwise use Downloads model restore).
2. With encoder and Q8 GGUF installed, tap **VERIFY DECODER (synthetic fixture,
   no lecture)**. An audio recording is not required.
3. Inspect the screen and share **only the `QwenVerify-*.log` file**. Do not share
   models or personal recordings.
4. If asked to construct an independent reference, use the exported synthetic
   feature tensor and *the exact Q8 GGUF* on an independent reference runner;
   the native bridge under test must **not** be reused as the "reference".
   Obtain its exact token IDs (including EOS), then generate a local manifest:

   ```bash
   python3 scripts/create-qwen-verification-reference.py \
     --gguf-sha256 <64-character-SHA-from-first-log> \
     --feature-sha256 <64-character-SHA-from-first-log> \
     --token-ids-file independently-produced-token-ids.json \
     --origin 'independent runner/version/method' \
     --output qwen-fixture-reference.json
   ```

   The reference manifest contains token IDs; treat it as private. Import via
   the Lab's reference picker. Re-run VERIFY once and share only the new log.
   Official reference generation for the mixed-input Qwen3-ASR decoder remains
   a separate, currently unimplemented validation step.

## Result interpretation and next engineering action

| Report status | Meaning | Next action |
| --- | --- | --- |
| `PASS_INTERNAL_ONLY` | Deterministic QNN features, repeated native token sequence, EOS and one model load; **no reference**. | Obtain independent reference. Do not claim M-RoPE parity. |
| `PASS_REFERENCE_TOKENS` | Exact IDs agree with user-supplied reference for matching GGUF and tensor hashes. | Validate provenance and compare teacher-forced logits/M-RoPE numerically before qualifying decoder. |
| `FAIL_REFERENCE_TOKENS` | First mismatching token index reported. | Check reference provenance/quantization/prompt construction; instrument first prefill/teacher-forced logit divergence next. |
| `FAIL_INTERNAL_REPLAY` | Two native passes disagree on same tensor. | Investigate KV clearing, context reset, and native state before any accuracy changes. |
| `FAIL_QNN_FEATURE_REPLAY` | Two NPU outputs differ for identical synthetic mel input. | Check deterministic encoder execution, provider libraries, QNN context and device behavior. |
| `FAIL_NATIVE_RECEIPT` | Reported EOS disagrees with private native tokens. | Fix IPC/native metric correctness. |
| `INCONCLUSIVE_NO_EOS` | 128-token verification bound reached without EOS. | Use bounded known-EOS reference fixture or add a teacher-forced comparison; do not call this a failure of model accuracy. |
| `INCONCLUSIVE_REFERENCE_INPUT_MISMATCH` | GGUF or tensor SHA does not match reference. | Use exact matching weights and FP32LE feature input, then rerun once. |
| `INCONCLUSIVE_BAD_REFERENCE` | Reference manifest has invalid fields. | Regenerate with `create-qwen-verification-reference.py`. |
| `INCONCLUSIVE_RUNTIME_ERROR` | Encoder, native process, import, timeout or other execution error. | Inspect its exception *class* and phase; address the blocker before a repeat. |

The in-memory positive and negative controls (`decision_policy_selftest=true`)
exercise `PASS_REFERENCE_TOKENS`, token divergence, wrong model SHA, no external
reference, and replay mismatch. These are **comparator tests only**, not actual
reference parity. Additional JUnit tests cover mismatch positions and no EOS.

## Build gates

- CI runs `scripts/verify-lab-source.py`, the reference-generator positive /
  negative contract tests and `:app:assembleDebug :app:testDebugUnitTest`.
- Then verifies package ID, versionName/code, ARM64 dotprod ELF, QNN libraries and
  APK signing identity before publishing a unique `v6.6.5-lab.*` prerelease.
- If local SDK/NDK/CI access is absent, only source and JVM tests can be claimed.
- Do not upload private reference token IDs, audio, transcription or large model
  files to the repository, release artifacts or public issue reports.
