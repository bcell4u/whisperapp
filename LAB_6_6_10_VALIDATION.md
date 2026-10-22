# WhisperRecorder Qwen GGUF Lab 6.6.10 — validation receipt

## Baseline evidence (GitHub 6.6.9)

- Host same-CPU standard split vs extended unified checkpoint 00: RMSE 0.2855397426, cosine 0.9985463503, identical argmax; 5 of 5 checkpoint logits differed numerically, but extended batch replay was byte-identical.
- S26 synthetic logits vs host standard split RMSE 0.4713255973; versus host unified extended RMSE 0.3132871340. This is similarity, **not** proof of official decoder correctness.
- Source artifact provenance pinned llama.cpp revision `d81235049384534c167caea52b85a694f6103d14`, model SHA-256 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`, synthetic features SHA-256 `e03a26e5e9253b05d4fad885db48dd38cb2e19e5a843e27d6d8d2e95c6cbe0ee`.

## 6.6.10 release checks actually run

- Full project static gate: PASS (`python3 scripts/verify-lab-source.py`).
- Host numerical 6.6.9 regression tests and new 6.6.10 unit/negative control tests: PASS.
- Additional prompt, synthetic numerical export, reference integrity tests: PASS.
- C++ standard and extended host decoder syntax: PASS with an **API-only test stub** (not a real llama.cpp build).
- Reused **actual 6.6.9 vectors** to simulate third path exactly equal to the standard split result: PASS classification `CHUNK_GROUPING_EXPLAINS_DIFFERENCE`.
- Reused actual 6.6.9 vectors to simulate third path exactly equal to the unified extended result: PASS classification `API_PATH_EXPLAINS_DIFFERENCE`.
- Git patch from original v6.6.9 source and complete 6.6.10 build tree: checked for byte-for-byte equality after application.
- Android `native/qwen_bridge` decoder source: unchanged from 6.6.9.
- Android app source outside version metadata, README/docs, source verifier, CI workflows, and verification tooling: unchanged.

## NOT yet done

- New three-stage extended path **has not executed with model weights**; no new numerical inference findings are claimed.
- Actual Android APK compilation / GitHub prerelease publishing: **not run in this environment**. The workflow performs those if the pushed source passes GitHub Actions.
- The test remains based on shared llama.cpp transformer mathematics; official Qwen3-ASR M-RoPE correctness and numerical parity are still **unverified**.

## Next action

Push complete v6.6.10 source to the separate GitHub Lab repository `main`. The existing APK workflow and the updated `Qwen host chunking isolation` workflow are configured to start automatically. Download the `qwen-6.6.10-host-chunk-isolation-*` Actions artifact and share the ZIP. No S26 app install, transcript or phone recordings are needed for this host-only experiment.
