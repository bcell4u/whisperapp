# WhisperRecorder Lab 6.6.25 — matched-position independent numerical comparison

The 6.6.24 public-model job validated 2D HF position forwarding, 254 directly/cast-equal BF16 decoder weights, and 56 Q/K matrices. Direct HF 3D/4D positions produced extra attention dimensions, so rejected shapes are not a model-correctness test.

## Question

When the independent Qwen3-ASR Hugging Face BF16 decoder receives position IDs `[1,171]` and the pinned llama.cpp BF16 GGUF receives four **identical** positions per item, do their output logits remain equally close for identical position maps? This isolates a remaining possible position-application disagreement without assuming that a small or zero numerical gap certifies correctness.

## GitHub experiment

1. Independently load the exact pinned published Hugging Face BF16 checkpoint (`6aa69c3…`) and public GGUF BF16 (`12b289…`), verifying full downloaded weight checksums.
2. Replace the same 156 audio embeddings with the existing SHA-pinned synthetic S26 fixture, preserving prefix/suffix tokens, 171 positions and 151,936 logits.
3. Run **four matched position maps**: sequential, uniform `+1`, uniform `+17`, and suffix gap `+11` starting at index 165. HF receives standard 2D input; GGUF gets four identical values per position. All tested maps preserve strictly increasing positions and token order; none constitutes a validated official ASR segmentation grid.
4. Run llama.cpp on the pinned host revision `d812350` with Flash Attention requested OFF and ON (4 threads, unified batch, microbatch 256). Record logits and diagnostic logs; don't assume kernel-path tracing solely from a requested mode.
5. Compare matched-position FP32LE vectors using raw RMSE, mean-centered RMSE, maximum difference, cosine and token top-1. Separate incomplete branches from complete findings. Save synthetic vectors only; no weights, audio, user transcripts or private file paths.

Interpretation: systematic error changes across different *matched* maps may indicate position-dependent kernel differences; persistently similar errors may point elsewhere in numerical execution. Neither pattern alone identifies a bug or justifies an Android code modification. Even perfect matching of these four controls would NOT certify official Qwen ASR position grids, audio encoder parity or clinical transcription quality.

The Android QNN encoder, CPU Q8_0 decoder, four-thread optimization, Flash Attention behavior, model downloads, and production app remain untouched.
