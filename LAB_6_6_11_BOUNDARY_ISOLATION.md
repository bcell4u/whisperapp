# Qwen GGUF Lab v6.6.11 — prefill boundary isolation

**Scope:** GitHub Actions synthetic-only same-host diagnostics. No Android inference logic, model, quantization, overlap or logging changes. Android versionName 6.6.11-dev, versionCode 66011.

## Evidence from actual 6.6.10 GitHub artifact

`qwen-6.6.10-host-chunk-isolation-3.zip` was inspected on October 21, 2026. It reported `CHUNK_GROUPING_EXPLAINS_DIFFERENCE` for a pinned `llama.cpp` revision `d81235049384534c167caea52b85a694f6103d14` with exact GGUF and synthetic feature hashes. At all five teacher-forced checkpoints, standard three-stage and extended three-stage logits were **byte-identical**, while unified extended differs. For first-token logits: three-stage vs unified RMSE 0.2855397426; S26 vs unified RMSE 0.3132871340; S26 vs three-stage RMSE 0.4713255973. Both host extended modes replay identically. This is a grouping observation, not a ground-truth correctness verdict.

## New question: which cut matters?

The same `verification/host/qwen_reference_mixed.cpp` extended API now accepts four prefill groupings (same total 171 positions, 4-axis M-RoPE coordinates, fixed teacher-forced tokens, GGUF hash, CPU settings, KV-cache lifetime):

| `qwen-mixed-reference` mode | Prefill `llama_process()` calls |
| --- | --- |
| `unified` | `[prefix + audio + suffix]` |
| `prefix-audio` | `[prefix + audio]` then `[suffix]` |
| `audio-suffix` | `[prefix]` then `[audio + suffix]` |
| `three-stage` | `[prefix]` then `[audio]` then `[suffix]` |

A standard `llama_decode()` three-stage host reference is retained as a control. Both two-stage modes and both baseline extended modes are run twice. The test checks four replay pairs, the equality of standard vs extended three-stage logits, all five checkpoint pairs, and S26 vs each grouping at **step 00 only**. The phone did not export steps 01–04; those reference tokens are fixed host-only synthetic constants.

## Interpretation (not official model parity)

`BOUNDARY_ISOLATION_REPORT.json` labels positive, ambiguous and failed outcomes. When exact equality holds at every checkpoint:

- `PREFIX_AUDIO_CUT_DOMINANT`: `[prefix+audio][suffix]` equals unified; `[prefix][audio+suffix]` equals three-stage.
- `AUDIO_SUFFIX_CUT_DOMINANT`: `[prefix][audio+suffix]` equals unified; `[prefix+audio][suffix]` equals three-stage.
- `EITHER_SINGLE_CUT_SUFFICIENT`: either two-stage option agrees with three-stage.
- `BOTH_CUTS_REQUIRED`: both two-stage options agree with unified, while three-stage differs.
- `GROUPING_INTERACTION_UNRESOLVED`: the grouping changes interact, or agreement is close but not exact.
- `ALL_GROUPINGS_IDENTICAL`: the original discrepancy has not reproduced.
- `FAIL_REPLAY`, `INCONCLUSIVE_REFERENCE_CHANGED`, `INCONCLUSIVE`: don't draw conclusions, inspect the log and source parity first.

Exact agreement is deliberately used for categorical grouping conclusions; RMSE, centered RMSE and top-token agreement remain available for intermediate outcomes. **Being closer to the S26 does not prove mathematical correctness.** This is a controlled host comparison sharing underlying llama.cpp math; official Qwen PyTorch numerical parity and correct M-RoPE formulation remain unverified.

## Instructions

Push v6.6.11 complete Lab source to the isolated GitHub test repo `main`. Both existing Android auto-release and **Qwen host boundary isolation** workflows are configured to run on push. The latter retrieves the SHA-pinned ~805 MB public GGUF into ephemeral GitHub runner storage, never into Git or APK. Download the action artifact `qwen-6.6.11-host-boundary-isolation-*` and share it for the next diagnostic step. No phone transcript or audio is needed.

Look first at `BOUNDARY_ISOLATION_REPORT.json`, `PROVENANCE.txt` and full host vectors. Do not alter Android decoder output until causal mechanism and correctness are established. A failed workflow must create an explicit `INCONCLUSIVE` report rather than a false PASS.
