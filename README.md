# WhisperRecorder Qwen GGUF Lab 6.6.21 - GGUF metadata parser repair

The uploaded 6.6.20 GitHub Actions logs show that both SHA-256 pinned public BF16 checkpoints were downloaded successfully, but the weight audit failed before reading model tensors: its GGUF metadata parser rejected an array longer than 100,000 entries (`GGUF_EXCESSIVE_ARRAY_LENGTH`). This is a host diagnostic parser bug, **not** a model-weight mismatch or evidence of a phone-decoder bug.

Release 6.6.21 repairs large GGUF tokenizer metadata handling with bounded zero-allocation skips, explicit malformed-file checks, and new tests using synthetic 151,936-entry string/numeric tokenizer arrays. It reruns the same independent `-hf` versus BF16 GGUF direct-tensor-identity test with the same pinned public hashes. Tensor mapping remains deliberately limited: Q/K rotary-permuted weights, unaudited layers, model conversion and official multi-axis M-RoPE are not certified.

The separate Android Lab application, QNN audio encoder and optimized ARM64 Q8_0 4-thread decoder are unchanged. No Flash Attention, model download, overlap, microphone or production Parakeet setting is changed. No weights, user audio, transcripts or credentials are packaged.

Push the complete source to Lab `main`; the Lab APK workflow and host-only weight audit start automatically. Upload `qwen-6.6.21-hf-gguf-weight-identity-*`. See `LAB_6_6_21_WEIGHT_PARSER_REPAIR.md` for evidence and scope.
