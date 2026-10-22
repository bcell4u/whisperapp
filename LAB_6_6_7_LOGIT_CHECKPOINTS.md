# WhisperRecorder 6.6.7: synthetic decoder numerical checkpoints

## Why this experiment

The S26 6.6.5 test established deterministic feature tensors and exact token replay for a short synthetic fixture, **not** independent Qwen model correctness. The 6.6.6 prompt-ID correction did not show a meaningful medical-term improvement. Running additional fixed-overlap sweeps or guessing at clinical corrections is not justified.

**6.6.7 leaves actual lecture transcription, 12-second windows, optional one-second overlap, QNN encoder, four-thread dot-product Q8 decoder, prompt, M-RoPE and KV-cache mathematics unchanged.** Only the explicit synthetic verification path changes.

## Added diagnostic, fail closed

When the user taps **VERIFY DECODER (synthetic fixture, no lecture)**, the app:

1. Verifies the bundled synthetic mel input and repeats the NPU encoder; tests feature equality exactly as in 6.6.5.
2. Feeds the *same* synthetic 156 x 1024 embedding to the decoder twice, keeping the Q8 model loaded once and clearing KV state between segments.
3. Captures the *full first generated-token logit vector* after each prefill, **151,936 x float32 little-endian = 607,744 bytes each**, into two temporary files inside Lab-private cache; rejects nonfinite logits.
4. Checks both logit vectors for bitwise equality, computes the SHA-256 of the first, the first differing float index and maximum absolute float difference. Token replay and EOS checks remain.
5. Saves a copy of the first **synthetic-only** logit vector under `Download/SFSU/Whisperapp/Verification/qwen-synthetic-prefill-logits-XXXXXXXX.f32le`, using Android MediaStore without broad storage permission. The log contains no logits, token IDs, audio or private paths.
6. Deletes temporary decoder inputs and scratch checkpoints through the existing Lab session-scoped cleanup. If checkpoint files are missing, truncated, or nonfinite, returns `INCONCLUSIVE_LOGIT_CHECKPOINT`, never PASS. If tokens match but logits do not, returns `FAIL_LOGIT_REPLAY`.

The regular transcription command never passes the new `--synthetic-logits fixture` diagnostic flag. Source and APK preflights require `WR_QWEN_SYNTHETIC_LOGITS_V1` to prevent mixing an older decoder with the newer verification UI.

## One useful S26 test after a successful CI APK build

Install the v6.6.7 Lab prerelease with the same certificate / application ID if you want to update without removing models. With the models already available, tap **VERIFY DECODER (synthetic fixture, no lecture)** **once**. No lecture recording or extra CPU benchmark is required.

From Samsung My Files:

- `Download/SFSU/Whisperapp/Logs/WhisperRecorder-QwenVerify-*.log` — share this small text report first.
- `Download/SFSU/Whisperapp/Verification/qwen-synthetic-prefill-logits-*.f32le` — export only if numerical reference analysis is required. This file is synthetic-only but derived from the publicly distributed model weights.
- Existing `qwen-synthetic-156x1024-*.f32le` — the exact encoder features, unchanged.

**Expected outcomes:**

| Outcome | What it really means | Next step |
| --- | --- | --- |
| `PASS_INTERNAL_ONLY` + `logit_replay_status=PASS_LOGIT_REPLAY` | Identical synthetic QNN features, first-token logits, generated tokens, EOS and one model load. | Acquire independently computed logits with identical GGUF, features, prompt and positions. Official parity still unqualified. |
| `PASS_REFERENCE_TOKENS` + `PASS_LOGIT_REPLAY` | The supplied token reference agreed and native logits replayed; provenance is not independently certified. | Compare with an authentic independent *numerical* decoder. |
| `FAIL_LOGIT_REPLAY` | Exact same input gave different first-token numerical outputs. | Inspect KV reset, batch handling, backend nondeterminism and one-model reuse; do not change ASR prompt. |
| `INCONCLUSIVE_LOGIT_CHECKPOINT` | Missing/truncated/nonfinite checkpoint or unable to read it. | Inspect new native `--probe-info` marker, scratch writes, and SELinux/storage behavior. |
| Existing `FAIL_INTERNAL_REPLAY`, `FAIL_QNN_FEATURE_REPLAY`, `FAIL_REFERENCE_TOKENS`, `INCONCLUSIVE_*` | Previous failure pathways remain and override inappropriate success. | Follow the 6.6.5 verification guide, not a repeated full lecture benchmark. |
| `logit_checkpoint_export_status=FAILED` | Numerical replay may pass, but its public export failed. | Check Downloads availability / MediaStore. No model correctness claim. |

## Independent reference numerical comparator (desktop, later)

The source includes `scripts/compare-qwen-prefill-logits.py`. An **independent** decoder (not the same `qwen_npu_llama_probe.cpp` bridge under test) must be configured to load the exact same Q8_0 model weights, receive the exact 156x1024 FP32 audio embedding, use the exact ASR prompt and 4-axis mapping and produce the first prefill logits as FP32LE, in the same vocabulary order.

```bash
python3 scripts/compare-qwen-prefill-logits.py \
  qwen-synthetic-prefill-logits-from-S26.f32le \
  independently-produced-prefill-logits.f32le \
  --candidate-sha256 <SHA_FROM_VERIFICATION_LOG>
```

Reports RMSE, maximum absolute delta, cosine similarity, top-1 equality and top-5 overlap. It validates input shape/finite values. It does **not** generate the independent reference, invent tolerances, confirm reference authenticity or declare official Qwen equivalence. A future controlled teacher-forced multi-token/logit comparison is required to qualify position handling and KV history beyond the first token.

## Build and compatibility

- `version.properties`: versionName `6.6.7`, Android code `66007`; debug label `6.6.7-dev`.
- Isolated Lab app ID and original 6.5.22 signing secret **names** retained. No production key bundled.
- Push to `main` triggers GitHub Actions, native/Gradle compilation, APK inspections and automatic experimental prerelease only after success.
- Added positive/negative native-contract tests and numerical comparison tests; Kotlin/JUnit tests for identical, divergent, nonfinite and truncated logit vectors.
- Nothing here qualifies the decoder against authoritative official M-RoPE logits. The historical `UNQUALIFIED` status must remain until that separate evidence exists.
