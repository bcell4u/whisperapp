# WhisperRecorder Lab 6.6.26 — same-weight execution precision isolation

## Why this experiment

The published 6.6.25 GitHub artifact completed all four matched positional maps for HF BF16 and GGUF BF16 (Flash ON/OFF). The 12 original output vectors were independently validated against the report SHA-256 digests. All 12 output branches selected first token 11528.

Measured **within-backend** change relative to `pos-linear`, full-vector RMSE:

| Map | HF BF16 | GGUF BF16 Flash ON | GGUF BF16 Flash OFF |
|---|---:|---:|---:|
| pos-plus1 | 0.197896 | 0.131181 | 0.093879 |
| pos-plus17 | 0.357697 | 0.084422 | 0.074684 |
| pos-suffix-gap11 | 0.494386 | 0.448301 | 0.431258 |

For suffix-gap11, the direction of HF versus GGUF Flash-ON positional response has cosine 0.842; for plus1 it is -0.051. The vector differences are repeatable, but no model is established as correct. In pure mathematical RoPE a uniform global shift often preserves relative attention phases, **but real models may use additional positional conventions and numerical rounding**. Do not treat these shifts as an official ASR correctness oracle.

## Main independent variable

HF model: pin `Qwen/Qwen3-ASR-0.6B-hf` at commit `6aa69c382e2b426eee1f5870d4c95859a74b6445`, SHA-256 `d3f212dd20abecd315d830bc54ae3865e56ebfc3276484e57b771288ba27fd35`. Construct the 171-position input from pinned synthetic feature SHA `e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee`.

Execute three HF branches, each on the *same already-loaded BF16 checkpoint*:

1. `bf16`: BF16 model weights / runtime arithmetic / BF16-rounded input token embeddings and audio features.
2. `fp32-rounded-input`: cast that loaded model to FP32, retaining numerically identical BF16 source weights, while casting the original BF16-rounded embeddings to FP32. This controls numerical execution without changing the input information.
3. `fp32-exact-audio`: same FP32 model and text input embeddings, but place the original synthetic FP32 audio features back into the 156 positions. This measures the **separate** contribution of BF16 audio-input rounding.

For each branch evaluate `pos-linear`, `pos-plus1`, `pos-plus17`, and `pos-suffix-gap11` under HF eager attention. GGUF BF16 repeats all four maps with Flash ON and OFF, pinned llama.cpp tag v0.6.0, CPU threads 4, unified prefill, ubatch 256. GGUF axes are four identical scalar positions; HF input is conventional 2D positions. **Neither is certified as the official Qwen ASR audio M-RoPE grid.**

The comparator measures raw and mean-centered vector RMSE, cosine, first-token agreement, cross-dtype precision effects, and **position response differences relative to each implementation's own `pos-linear`**. A raw HF-to-GGUF gap alone is not a correctness result; response residuals indicate whether matching positional changes behave similarly.

## Safety / caveats

- The GGUF source weight match from 6.6.23/24 covers 254 direct/cast-comparable tensors plus the 56 Q/K projections; no model-weight mismatch was confirmed. A small number of model tensors remain outside strict end-to-end validation.
- FP32 runtime is not an alternate FP32 source checkpoint. It casts BF16 checkpoint weights, providing an arithmetic-precision experiment only.
- The HF wrapper and GGUF backends retain independent mathematical/implementation differences. This run cannot certify official Qwen audio M-RoPE, encoder parity or transcript accuracy.
- The Android QNN encoder, original Q8_0 dot-product decoder, model downloads, threading, Flash Attention settings, transcription code, and production app are unchanged.
- Artifact contains synthetic logits and provenance only: **never** publish private audio, lecture transcripts, real paths, checkpoint weights or downloaded model files.
- Missing branches and metadata/hash mismatches are explicitly `INCONCLUSIVE`, never hidden by averaging partial results.

## GitHub artifact

Push the complete 6.6.26 Lab source ZIP to `main`. The action `Qwen same-weight execution precision controls` will publish `qwen-6.6.26-hf-execution-precision-*`, including `EXECUTION_PRECISION_REPORT.json`, `hf/HF_EXECUTION_PRECISION_METADATA.json`, 12 HF and 8 GGUF prefill vectors, GGUF execution logs and provenance. APK compilation remains a separate GitHub Actions job. No phone test is required for this host-only diagnostic.
