# WhisperRecorder Lab 6.6.18 — attention graph audit and synthetic greedy decoding

## Grounded results: user-provided 6.6.17 artifact

The uploaded `qwen-6.6.17-host-attention-path-10.zip` contains `ATTENTION_PATH_REPORT.json`, a pinned upstream revision, SHA-verified synthetic fixtures and Q8_0/BF16 checkpoints. The report's status is `FLASH_OFF_REDUCES_Q8_BATCH_SENSITIVITY`. Across two prompt groupings, `n_ubatch=128/256` and five teacher-forced checkpoints, microbatch RMSE averaged **0.000000** when Flash Attention was requested OFF for both Q8_0 and BF16. When requested ON, mean RMSE was **0.198695** (Q8_0) and **0.052178** (BF16). Repeated identical configurations were bitwise identical. At the prefill position, Q8 Flash OFF's RMSE against the previously exported S26 checkpoint was **0.392917**; Q8 Flash ON / unified / u256 was **0.313287**; neither is independently established as correct.

**Additional graph evidence found in the uploaded inference logs:** Flash-enabled runs log `llama_context: flash_attn = enabled` and multiple `(FLASH_ATTN_EXT)` graph nodes with no `(SOFT_MAX)` attention nodes. Flash-disabled runs log `llama_context: flash_attn = disabled`, contain `(SOFT_MAX)` attention nodes and no `(FLASH_ATTN_EXT)` nodes. This corroborates distinct runtime graph constructions, beyond merely accepting a context flag. The logged graph operators do **not** independently prove which low-level CPU instructions execute, establish official-model numerical parity, or show that one path is more accurate.

## 6.6.18 experiment

The host decoder gains an **explicit opt-in** `greedy-24` synthetic-only mode. The original five teacher-forced checkpoints remain the default and are unchanged. The new mode, after the same 171-position prefix/audio/suffix prefill, repeatedly chooses the highest-logit token, emits only numeric token IDs and top-2 score margins, feeds that token back into the same KV-cache state, and stops at the first model end token or 24 tokens. It does not decode text or use any user's audio.

GitHub builds pinned llama.cpp v0.6.0 (revision prefix `d812350`) on the same host CPU and downloads/validates the fixed public Q8_0 and BF16 GGUFs. It evaluates:

- Q8_0: Flash ON/OFF, unified vs three-stage, u128 vs u256 (8 cases).
- BF16: Flash ON/OFF, unified prompt, u128 vs u256 (4 cases).
- Q8_0 identical-configuration replay in Flash ON and OFF modes (2 cases).

Each inference log undergoes a **fail-closed graph audit**: the requested/printed attention mode must match the graph operator type, `(FLASH_ATTN_EXT)` for ON or `(SOFT_MAX)` for OFF. The evaluator reads all 12 initial 151,936-logit vectors, validates the fixed feature hash, checks generated ID sequence and numeric margins, checks exact replay IDs and initial logits, and reports the earliest divergent greedy-token position between attention, microbatch and prompt-grouping configurations. Only the uploaded S26 *first-token logits* are used for cross-device numerical comparison. Any missing/corrupted/mismatched artifact is `INCONCLUSIVE` or `FAIL_REPLAY` rather than a success.

### Interpretation

If the generated token IDs differ, report which control flips first and how much each contender's top-two margin changed; this demonstrates synthetic *decision* impact and prioritizes an independent correctness oracle before any phone change. If all tokens agree through model EOS or 24 steps, this **does not prove** that the differences are harmless to other inputs, particularly medical speech. In either case, microbatch invariance under Flash OFF is not proof that Flash OFF is the correct Qwen computation. Qwen's four-axis M-RoPE map `{pos,pos,pos,pos}` remains provisional and UNQUALIFIED; there is still no official PyTorch oracle for this synthetic fixture.

## Scope and release

The normal S26 QNN encoder, ARM64 dot-product Q8 decoder, four-thread worker, optional overlap/stitching logic, downloadable models, signing rules and production Parakeet remain **unchanged**. This is **only a host diagnostic and Lab metadata update**, v6.6.18 / versionCode 66018. No recording, transcript, clinical term list or model weights are distributed in the source. The GitHub host workflow publishes `qwen-6.6.18-host-generation-impact-*`, containing `GENERATION_IMPACT_REPORT.json`, `PROVENANCE.txt`, requested path logs, five-variable numerical comparison and numeric greedy-token IDs.

The authoring environment validates the source and tests with stubs but cannot compile the Android APK or execute the 805 MB model locally. A GitHub run is necessary. No phone test is required yet.
