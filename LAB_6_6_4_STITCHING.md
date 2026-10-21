# WhisperRecorder Qwen GGUF Lab v6.6.4 — conservative stitching experiment

Based on the already on-device-tested 6.6.3 Lab implementation. This update changes **text cleanup only**, not the 12-second NPU encoder windows (optional one-second overlap), decoder, tokenizer, QNN runtime, model weight hashes or CPU thread count.

## Reused from production Parakeet v6.5.22

- Up to **24 exact matching suffix/prefix words** (was 8), ASCII word tokenization, case-insensitive matching without fuzzy substitutions.
- For exactly **two matching words**, require at least **12 characters** of matched prefix as in production `TranscriptCleaner`.
- Use the original `TranscriptCleaner.cleanChunk()` for each overlapping Qwen segment. It removes only substantial text repeated at least **three consecutive times**; ordinary single/double repetition remains.
- Only trim duplicates on documented boundary evidence. No automatic medication-name substitutions, timing-based word deletion or inferred audio alignment.
- Overlap disabled retains the old verbatim join and skips Parakeet cleanup, preserving an exact baseline.

## Added metrics and checks

`stitched_repetition_copies_removed` is an integer in the existing privacy-safe diagnostics allowlist, alongside boundaries and duplicate words. No transcript, model paths or audio in the diagnostic log. Offline JUnit tests exercise three-word and 24-word overlap, two-word safeguard, genuine spoken repetition, triple repetition cleanup, medical-word disagreement and overlap-off baseline.

## Interpreting the experiment

The prior 6.6.3 run took ~32.0s for 20 segments, stitched six of 19 boundaries and removed 24 duplicate words; it did **not** establish a word error rate improvement. New rules can change text stitching, not the raw model token output. Compare the saved or copied 6.6.4 transcript with the previous 6.6.3 and non-overlap transcripts before claiming accuracy improvements. M-RoPE parity remains unqualified.

The automated workflow publishes a *Lab prerelease only after* actual GitHub Android build/unit tests and APK checks. This source update has not been phone-tested; see VALIDATION_6_6_4.md for compile status.
