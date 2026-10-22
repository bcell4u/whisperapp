# Lab v6.6.19 — Independent Hugging Face Qwen decoder prefill reference

## Input evidence

The 6.6.18 host artifact reported `NO_SYNTHETIC_GREEDY_DIVERGENCE_WITHIN_24_TOKENS` but **every one of 12 runs stopped at the same EOS after four tokens**, IDs `[11528,2240,151704,151645]`. Every graph mode, Q8/BF16 precision, and grouping was deterministic. The condition has **not** been validated for longer or difficult clinical speech, and the name of the previous result must not be misread as 24 observed tokens.

## Independent test

`produce-qwen-hf-reference.py` downloads public `Qwen/Qwen3-ASR-0.6B-hf` at pinned HF revision prefix `6aa69c3`, records the resolved full commit and SHA-256 for the official checkpoint's safetensors files, and executes **PyTorch/Transformers eager decoder math** on CPU. It constructs exact 171-position synthetic prefill inputs: 9 native token embeddings, 156 FP32 audio embeddings converted to model BF16 dtype, 6 native token embeddings. All positions are sequential `0…170`; this is ONLY an equal-axis fixture. The audio encoder is **not** re-executed.

CI independently recompiles pinned llama.cpp v0.6.0 `d81235049384534c167caea52b85a694f6103d14` and regenerates first-token checkpoints using public GGUF BF16 Flash OFF/ON and Q8 Flash ON, all at unified microbatch 256. SHA-256 model contracts and the existing S26 FP32 logits are checked. Comparisons cover raw and offset-centered RMSE, maximum error, cosine similarity, and argmax. Any provenance, structural or artifact failure becomes `INCONCLUSIVE`.

## Interpretation

The **HF -hf checkpoint and ggml-org GGUF are not tensor-by-tensor proven identical**. A substantial disagreement could arise from weight mapping, PyTorch BF16 rounding, GGUF quantization, the positional encoding or the prompt construction. It is not automatically evidence of a bug in the phone decoder. The original Qwen configuration declares 3-axis interleaved M-RoPE; the -hf text decoder uses 2D linear positions, equivalent to our synthetic diagonal axes only if the angle mapping is otherwise preserved. Therefore official multi-axis M-RoPE and full ASR correctness remain `UNQUALIFIED`.

No real audio, private paths, transcripts, credentials, QNN encoder weights, or model checkpoints are bundled. Normal Android inference remains unchanged: 4 ARM64 dotprod threads, 12s QNN encoder, Q8_0 GGUF, original decoder and overlap settings.

## Artifacts

`qwen-6.6.19-hf-independent-prefill-*/INDEPENDENT_REFERENCE_REPORT.json`, `hf/HF_REFERENCE_METADATA.json`, `hf/hf-prefill-logits.f32le`, three host `logits-step-00.f32le` vectors, `PROVENANCE.txt` and execution logs. Error paths deliberately emit an inconclusive report.

## Gate

Source, YAML, Python evaluator and negative-case tests are local checks. They are not confirmation that the published HF model loaded or that the Android APK compiled. The full numerical comparison and the Android APK build occur only on GitHub Actions after pushing this source.
