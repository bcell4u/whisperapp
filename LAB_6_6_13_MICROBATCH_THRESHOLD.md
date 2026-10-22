# Lab 6.6.13 — targeted microbatch threshold isolation

## Proven results from 6.6.12

The pinned-model same-host GitHub artifact finished with `MICROBATCH_SENSITIVE`.
At step 0, unified vs three-stage RMSE was **0.2855397** with `n_ubatch=256` and
**0.2216845** with `n_ubatch=64`. Within unified decoding, changing 256 to 64
shifted logits by RMSE **0.2027819**; within three-stage, **0.2341284**. Thread
count 4→1 changed neither baseline output, exactly, for five checkpoints.
All reported replays were identical. The S26 first-token vector was closest to
unified/256 among the 6.6.12 configurations (RMSE 0.3132871). That is not proof
of correctness. Official M-RoPE and PyTorch numerical parity remain unverified.

## New experiment

Keep same Q8 GGUF SHA, same synthetic FP32 features, prompt, linear four-axis
position map, pinned llama.cpp tag, CPU backend and 4 threads. Sweep only
`n_ubatch ∈ {64,128,160,192,256}` with `n_batch=256`, for unified and
three-stage extended batches. These values deliberately surround **156 audio
frames** and **171 total prompt positions**. In the three-stage mode, an audio
segment of 156 fits in 160, but not 128; in the unified mode, a 171-position
prefill fits in 192, but not 160. This is a *hypothesized* internal split
threshold, not a claim that upstream kernels divide precisely there. Compare
five teacher-forced checkpoints per configuration, plus replay of unified/256,
three-stage/256, and unified/160. Emit raw and mean-centered RMSE, top-token
agreement and hashes, and S26 comparisons only for step 0.

### Outcomes

- `UNIFIED_AND_AUDIO_THRESHOLD_SENSITIVE`: changes observed at hypothesized
  audio and full-prompt thresholds; investigate actual microbatch boundaries and
  attention-mask/position handling before altering the Android bridge.
- `UNIFIED_PROMPT_THRESHOLD_SENSITIVE`: unified 160→192 changes while
  three-stage 160→192 does not.
- `THREE_STAGE_AUDIO_THRESHOLD_SENSITIVE`: three-stage 128→160 changes while
  three-stage 160→192 does not.
- `MICROBATCH_RESPONSE_NOT_ISOLATED`: sensitivity is more complex; inspect actual
  upstream microbatch partitions and RoPE/cache position application.
- `FAIL_REPLAY` / `INCONCLUSIVE_*`: fail closed on nondeterminism, missing files,
  corrupted inputs or missing baseline effect. Never report verified parity.

## Running

Push complete source to separate Lab `main`; two GitHub workflows run. The host
workflow produces artifact `qwen-6.6.13-host-microbatch-threshold-*` with
`MICROBATCH_THRESHOLD_REPORT.json`, `PROVENANCE.txt`, and synthetic-only logits.
Do not bundle the ~805 MB GGUF or any lecture files. The Lab APK and Android
four-thread dot-product decoder are unchanged apart from version metadata.
No S26 phone test is needed for this diagnostic release.

This still compares **llama.cpp to itself under different schedules**, not the
official higher-precision Qwen reference. Do not infer a medically accurate
transcription from a closer numerical match to S26.
