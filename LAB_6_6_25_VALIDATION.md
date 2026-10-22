# WhisperRecorder Lab 6.6.25 — validation and evidence limits

## Input findings (actual GitHub 6.6.24 artifact)

The artifact `qwen-6.6.24-hf-gguf-weight-identity-17.zip` was inspected before implementation. It contains SHA-pinned provenance and five stage files. The positions report states `HF_2D_POSITION_CONTRACT_VERIFIED`: explicit and implicit 2D logits have RMSE 0; suffix `+11` gives RMSE 0.4701298 and uniform `+17` gives RMSE 0.2560655 against baseline, with the same argmax token ID 11528. Rank-3/rank-4 direct decoder positions fail inside `repeat_kv`, while calling the rotary module directly returns extra leading dimensions. These observations do not establish valid official ASR M-RoPE grids.

The same artifact reports 254 directly/cast-equal storage tensors (141 BF16 exact + 113 F32 normalization BF16 casts), plus 56 exact Q/K tensors checked separately, all over 28 decoder layers. These audits overlap the same GGUF architecture but do not certify the official audio pipeline.

## v6.6.25 experiment

Compare HF BF16 2D positions against the same positions supplied to all four llama.cpp GGUF M-RoPE axes on SHA-pinned synthetic embeddings. Four modes: `pos-linear`, `pos-plus1`, `pos-plus17`, `pos-suffix-gap11`; two BF16 GGUF attention modes requested, `flash-on` / `flash-off`; all at unified `n_ubatch=256`, 4 CPU threads. Host and HF weights are separately SHA-verified. Each report includes exact hash/provenance, max difference, RMSE, mean-centered RMSE, cosine, and first-token argmax. Branch failures are explicitly INCONCLUSIVE; no thresholds are presented as model correctness or accuracy judgments.

## Local validation completed

- All **21** `scripts/test-qwen-*.py` scripts passed, including the new 4-position x 2-attention control tests and negative cases for corrupt/missing vectors.
- `scripts/verify-lab-source.py` passed, including Android isolation and release metadata.
- Both GitHub workflows parsed as YAML and the new host job's shape and runner-context restrictions were checked.
- Updated mixed host runner passed `g++ -std=c++17 -fsyntax-only` **against a minimal API-shaped test stub**. This does not replace compilation against actual pinned llama.cpp.
- No Android decoder, QNN, Android native bridge, speaker service or transcription implementation sources were modified.
- Complete source archive and 6.6.24 → 6.6.25 Git patch are verified to reconstruct the same project bytes independently (see final package verification).

## Pending external checks

The actual HF BF16 and GGUF forward passes and the Android Gradle/NDK APK compilation must execute in GitHub Actions. Internet access to download source/model weights was not available in this local workspace. Host C++ compilation against the **real pinned upstream llama.cpp header** is therefore also pending GitHub. Requested attention modes do not alone prove which kernels executed. No private recordings, user transcript, medical reference text, model weights or signing keys are bundled. This is synthetic evidence only; official ASR M-RoPE and phone accuracy remain UNVERIFIED.
