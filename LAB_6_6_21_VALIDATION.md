# WhisperRecorder Qwen GGUF Lab 6.6.21 - Validation

**Release:** 6.6.21 / Android versionCode 66021. This report describes local source/tests only; actual GitHub build and public model-weight identity audit have NOT run for this version.

## Trigger and evidence

Uploaded GitHub Actions run-13 logs for 6.6.20: pinned GGUF BF16 and HF `-hf` BF16 weights both downloaded and SHA-256 verified; host Python audit then aborted at `metadata_value` with `GGUF_EXCESSIVE_ARRAY_LENGTH` *before any tensor comparison*. Fail-closed fallback report was `INCONCLUSIVE`. The previous 100,000-element GGUF metadata cap was too low for large tokenizer arrays. The logs alone do not disclose the exact length of the offending array.

## Repair

Increase hard metadata-array cap to 2 million entries while skipping large strings and primitive/nested arrays via bounds-checked binary reader offsets, with no large temporary Python arrays or decoded strings. Preserve small metadata arrays and 32-byte/tensor-table alignment; continue rejecting bad magic, unsupported metadata types, truncated input, invalid counts and checksum mismatch. Underlying tensor comparison, model sources, pinned hashes and Q/K exclusion are not changed. The full model audit is still pending GitHub.

## Local tests completed

- `python3 scripts/verify-lab-source.py` - passed.
- `python3 scripts/test-qwen-weight-identity.py` - passed (151,936-element string and numeric GGUF arrays; alignment and byte-identical 30-tensor audit; mismatched/corrupt tensors; truncated/oversized array fail-closed; pinned hash failures).
- `python3 scripts/test-qwen-external-reference.py`, `test-qwen-batch-paths.py`, `test-qwen-chunk-isolation.py`, `test-qwen-boundary-isolation.py`, `test-qwen-execution-sensitivity.py`, `test-qwen-microbatch-threshold.py`, `test-qwen-input-kind-isolation.py`, `test-qwen-precision-batch.py`, `test-qwen-cpu-isa.py`, `test-qwen-attention-path.py`, `test-qwen-generation-impact.py`, `test-qwen-independent-reference.py`, `test-qwen-reference-generator.py`, `test-qwen-prompt-contract.py`, `test-qwen-logit-comparison.py`, and `test-qwen-synthetic-logit-contract.py` - passed.
- Python compile and GitHub workflow YAML validation - passed.
- Binary-safe patch reconstruction against the complete official 6.6.20 release ZIP - performed during packaging; new release source has no changed Android Kotlin/Java/C++/CMake source.

## Outstanding

The 1.4GB BF16 GGUF and published HF weights were not available locally for actual tensor audit. **No new model identity conclusion is established by these parser tests.** The independent Hugging Face vs GGUF mismatch, 4-axis provisional Android M-RoPE mapping, official position mapping and real nursing-lecture accuracy remain unqualified. No actual Android APK compilation was performed locally. GitHub workflows must run and return `qwen-6.6.21-hf-gguf-weight-identity-*` for the next conclusion.

No checkpoint weights, lecture audio, transcripts, device-private paths or credentials are included in the release archives.
