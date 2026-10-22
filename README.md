# Lab 6.6.18: Verified attention graphs and synthetic greedy-token impact (host-only)

The user-provided 6.6.17 host experiment showed **exactly zero** Q8_0 and BF16 microbatch variation with Flash Attention requested OFF, and mean microbatch RMSE **0.198695** (Q8_0) / **0.052178** (BF16) with it ON. The existing 6.6.17 inference logs also show actual computational graph operations: `(FLASH_ATTN_EXT)` in the ON runs versus `(SOFT_MAX)` in OFF runs. This narrows the numerical discrepancy to the attention execution graph, but does **not** prove which path is official-Qwen-correct.

6.6.18 asks the next practical question: do those differences change **greedily generated token IDs** (up to 24 tokens or model EOS) on the same synthetic 156×1024-feature input? Q8 has eight attention/grouping/microbatch cases, BF16 has four, and Q8 has two repeat runs. Each result is graph-audited from inference logs and checked for deterministic replay and complete numeric checkpoints. The generated output contains token IDs and margins only; no transcripts or speech are included.

See [LAB_6_6_18_ATTENTION_GRAPH_GENERATION.md](LAB_6_6_18_ATTENTION_GRAPH_GENERATION.md) and [LAB_6_6_18_VALIDATION.md](LAB_6_6_18_VALIDATION.md). The Android QNN encoder, four-thread ARM64 decoder, production ASR and signing remain unchanged. Official Qwen numerical/M-RoPE parity remains unverified.

Push full source to the separate Lab `main` branch. GitHub Actions produces the test artifact `qwen-6.6.18-host-generation-impact-*` and the Lab APK prerelease. No phone test is needed for this diagnostic.
