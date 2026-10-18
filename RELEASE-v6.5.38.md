# WhisperRecorder v6.5.38 — Qwen decoder efficiency A/B and memory diagnostics

Base: v6.5.37. Android `versionName=6.5.38`, `versionCode=65038`.
The debug flavor installs alongside the regular app as `6.5.38-dev`.

## Included

1. Qwen NPU encoder and original FP32 CPU decoder are unchanged by default.
   The known-good decoder input-tensor allocation remains selected until the
   user explicitly enables `Qwen reusable step buffers` in the model card.
2. Experimental CPU token-step path reuses direct float/long input buffers and
   ORT tensors per *segment* and reuses byte/float arrays for embedding rows.
   ORT's returned KV-cache tensors are retained only until the following step
   completes, exactly as in the baseline. There is no NPU decoder offload.
3. Read-only worker PSS measurements before decoder load, after decoder_init,
   after decoder_step and after prefill; first-step time and count of steps
   above 250ms. Numeric-only counters appear in automatic ZIP diagnostics.
4. 2/4-thread A/B, optional digest-gated K2A FP16 embeddings, Qwen cleanup,
   automatic `Whisperapp/Logs` diagnostics, and all other backends are preserved.

## Phone test — one build, multiple comparisons

Use the already installed verified Qwen Stage I model. Do NOT download it again,
remove fallback weights, or use the model-deletion controls. Prefer four decoder
threads and keep FP16 embeddings OFF for the initial A/B.

- Baseline: **reusable step buffers OFF**, transcribe the same 220.534-second clip,
  save the auto-generated ZIP and compare token/text and timings.
- Experimental: **reusable step buffers ON**, repeat after comparable idle/cool
  state. Save ZIP, compare complete transcript (not merely token count), runtime
  memory, prefill, first-step and decode timing.
- If a report shows failure, incorrect output, or repeated slowdown, set the
  option OFF immediately. Preserve both reports for auditing.
- FP16 embedding K2A trial remains OFF unless its separately generated,
  exactly SHA256-verified companion is available. It is not a substitute for
  the original manifest-listed FP32 embeddings.

## Stop / go

After two comparable A/B pairs, drop decoder-buffer reuse if there is no
repeatable >=5% total-time gain and no clear memory/thermal improvement.
Do not infer that Qwen runs fully on the NPU; only the encoder uses QNN HTP.

## Verification scope

Offline source verification and isolated JVM logic checks are run before
release packaging. Full Gradle APK compilation and on-device correctness and
performance checks remain the GitHub Actions / physical S26 gates.
