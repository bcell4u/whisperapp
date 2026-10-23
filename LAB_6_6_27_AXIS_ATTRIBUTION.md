# WhisperRecorder Lab 6.6.27 — GGUF RoPE axis attribution

## Prior evidence: 6.6.26 completed

The GitHub artifact `qwen-6.6.26-hf-execution-precision-19.zip` was inspected locally. All 12 HF and eight GGUF logits (151,936 FP32LE values each) matched the reported SHA-256 and argmax token 11528; all 20 branches completed.

The pinned Hugging Face checkpoint used identical **stored BF16 weights** for BF16 and FP32-runtime execution. Recasting the weights to FP32 shrank the GGUF Flash-ON comparison error, e.g. normal 0.18004→0.08233 and uniform-plus17 0.27797→0.03768 RMSE. Audio-input BF16 rounding alone contributed ~0.0023 RMSE for the normal input. With FP32 HF arithmetic a uniform +17 position shift changed logits only ~0.00002 RMSE, whereas GGUF Flash-ON changed ~0.08442, Flash-OFF ~0.07468. This is a real implementation/precision response difference on the **synthetic fixture**, not a proof of an incorrect positional decoder or of recognition mistakes.

## 6.6.27 experimental design

Keep pinned llama.cpp `d81235049384534c167caea52b85a694f6103d14`, GGUF BF16 SHA-256 `12b2894d9a7c98cd8f26670f5a47ab738f42bcc98df21e109be493870c71ba50`, fixture SHA-256 `e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee`, threads four, ubatch 256, 171 positions, unified prompt. Compare Flash Attention ON and OFF with graph-node verification.

Perform **24 first-token prefill evaluations** (12 position maps × two attention paths):

* Baseline `pos-linear` and `pos-plus1` (all axes).
* Uniform global +17 applied to **all four axes**, or separately to just axis 0, 1, 2, or 3.
* Prefix/audio-to-suffix gap +11 on **all axes**, or separately on just axis 0, 1, 2, or 3.

Each axis begins with its normal strictly increasing linear position map. There is no zeroing, overlap, inverted index, or relabeling of token order. A single-axis offset is a **controlled perturbation**, not a valid Qwen audio M-RoPE grid. All modes use the same public synthetic feature matrix and input tokens.

Compute full 151,936-dimensional response vectors relative to the same-path linear baseline, each axis's independent RMSE and response cosine, summation of individual-axis responses versus the actual all-axis response, nonlinear interaction residual, first-token IDs, and cross-attention comparisons. A large interaction residual means single-axis perturbations do not add linearly; it is not automatically a bug. If any branch, checksum length, position-label log, or attention-graph node fails, report `INCONCLUSIVE_INCOMPLETE` and preserve completed evidence.

## Explicit limitations

* The HF BF16→FP32 results from 6.6.26 are prior verified context, not re-executed in 6.6.27.
* `llama_batch_ext_set_pos` addresses four backend position axes; their numerical attribution does not establish the official Qwen-ASR acoustic grid.
* No evidence of altered speech-recognition tokens, Android correctness, medical vocabulary improvement or audio encoder changes.
* Host code only. No Android QNN/ARM64 decoder, Flash Attention setting, threading, memory, transcript or production app changes.
* Downloaded model weights and user data do not appear in artifacts. No real lecture audio, transcripts or private paths are used.

Artifact: `qwen-6.6.27-host-rope-axis-attribution-*`, with `ROPE_AXIS_ATTRIBUTION_REPORT.json`, per-branch first-token FP32LE vectors, inference graph logs, `PROVENANCE.txt`. No phone test needed.
