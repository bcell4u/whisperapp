#!/usr/bin/env python3
"""Source-only guard: no unverified FP16 weights or silent replacement."""
from pathlib import Path
r=Path(__file__).resolve().parent.parent
base=r/'app/src/main/java/com/example/whisperrecorder'
runner=(base/'qwen/Qwen3HybridWorkerService.kt').read_text()
store=(base/'npu/Qwen3HybridPackageStore.kt').read_text()
reader=(base/'npu/QwenEmbeddingReader.kt').read_text()
policy=(base/'npu/QwenDecoderPerformancePolicy.kt').read_text()
assert 'e80150119fa5f7e56e85aed64c3a02d5c78eb7a37cfdcb973d0987316f15bee2' in reader
assert '311164928L' in reader
assert 'archiveDigest(context, uri) { false }.equals(QwenEmbeddingReader.FP16_SHA256' in store
assert 'ModelFileOps.digest(candidate, "SHA-256")' in runner
assert 'useFp16EmbeddingTrial(applicationContext)' in runner and 'getBoolean(KEY_FP16, false)' in policy
assert '"embed_tokens.bin"' in runner and 'FP16_FILE' in runner
assert 'decoder_weights_fp16.data' in store
assert 'reclaimPrivateDuplicate' in store
print('Qwen FP16 trial checks passed; candidate optional and original protected')
