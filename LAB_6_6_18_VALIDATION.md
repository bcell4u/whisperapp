# 6.6.18 validation receipt

- Input artifact inspected: `qwen-6.6.17-host-attention-path-10.zip` (host Q8 and BF16 numerical paths, report, provenance, inference logs).
- Grounded finding: Flash OFF makes outputs identical across tested u128/u256 and unified/three-stage configurations at five numerical checkpoints. Flash ON retains Q8 mean RMSE 0.198695 / BF16 0.052178. The actual graph logs distinguish `FLASH_ATTN_EXT` from `SOFT_MAX`.
- Real S26 vs Q8 Flash OFF prefill RMSE ~0.392917; S26 vs Q8 Flash ON unified/u256 ~0.313287. Neither is an independent correctness check.
- New implementation: opted-in `greedy-24` host-only, deterministic argmax IDs, top-2 margins, model-EOS stop. Existing five teacher-forced checkpoint mode remains the default.
- New evaluator: graph-operation audit, first divergent-token localization, BF16 control, hash-verified fixture, Q8 replay and first-token S26 checkpoint comparisons.
- Local checks: source gate, all existing Qwen tests, new 6.6.18 branch tests, C++ syntax against API stub and PyYAML workflow parse (not a real llama.cpp build).
- End-to-end evaluator: actual 6.6.17 inference logs/logit checkpoints combined with **simulated** greedy-decision inputs; verified identical, divergent and graph-failure classification. Simulated tokens are NOT actual 6.6.18 GGUF inference.
- Git source patch and ZIP checks: completed at packaging time; see package checksum manifest.
- Outstanding: full GitHub host compilation/inference, full Android APK compilation, real greedy decisions, independent official Qwen3-ASR PyTorch oracle/M-RoPE parity, real nursing-lecture accuracy test.
- No changes to Android decoder, QNN encoder, phone flags, user audio or production app.
