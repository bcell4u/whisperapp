# WhisperRecorder Lab 6.6.6: corrected Qwen3-ASR role-token candidate

## Finding

The 6.6.5 native mixed-embedding Qwen decoder used `151644,9125,198,151645,198,151644,882,198,151669` as its prefix. Independent Qwen3-ASR implementations encode the `system` and `user` role labels as token IDs `8948` and `872`, respectively. The previous tokens were not the role names. This is a concrete prompt-construction discrepancy, **not** proof of a decoder arithmetic or M-RoPE failure.

Supporting sources:
- https://github.com/antirez/qwen-asr/blob/main/qwen_asr.c (Qwen3-ASR prompt role constants)
- https://huggingface.co/rhasspy/qwen3-asr-0.6b-onnx-int4 (documented role-token pitfall)
- https://github.com/QwenLM/Qwen3-ASR/blob/main/qwen_asr/core/transformers_backend/modeling_qwen3_asr.py (authoritative position-index implementation)

## Change scope

- Changes only the system/user role token IDs in the **active native bridge**, from 9125/882 to 8948/872. Does **not** rewrite the archived Builder source.
- Keeps all source native dot-product math, four-thread settings, 512-token context, Q8_0 GGUF and QNN encoder unchanged.
- Bumps Lab to 6.6.6 / 66006 with matching GitHub prerelease metadata.
- Hardens native `--probe-info`, worker's bridge protocol check, CI ELF/APK payload checks, and static verifier to require the corrected prompt contract.
- Adds positive/negative regression controls that reject the old IDs, wrong suffix, and missing native prefix.
- App data / Downloads backups, transcript, existing log path and signing conventions are unchanged.

## Important accuracy qualification

This is a **candidate fix, not an accuracy-qualified model**. The 6.6.5 `PASS_INTERNAL_ONLY` replay established determinism with the older, incorrect prompt. It cannot certify the corrected prompt; its four synthetic tokens and any token-reference manifests must not be reused as if they were the corrected decoder output.

When GitHub actually compiles and releases this version, make **one** S26 run on the same saved lecture with the same 1 s overlap as 6.6.4/6.6.5; compare medical terms and boundary repetitions side-by-side. Expect token counts to change. Capture the normal privacy-safe log; copy the transcript from the screen. **Do not delete the working 6.6.5 source/release** until the accuracy comparison is reviewed. A compiled Android APK and S26 accuracy improvement are NOT verified by these static tests.

Independent numerical M-RoPE/logit validation is still a separate step after fixing the prompt. The official implementation's three 1-D position axes are consistent in shape with a repeated linear position, but their four-axis ggml mapping and resulting numerical logits remain unqualified.
