#!/usr/bin/env python3
"""No-network source/manifest guards for compact Qwen 0.6B Android support."""
from pathlib import Path
import json
import sys

root = Path(__file__).resolve().parent.parent
src = (root / 'app/src/main/java/com/example/whisperrecorder/npu/Qwen3HybridPackageStore.kt').read_text()
runner = (root / 'app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt').read_text()
model = (root / 'app/src/main/java/com/example/whisperrecorder/ui/models/ModelsScreen.kt').read_text()
checks = {
    'new downloads pinned to Hugging Face main': 'private const val COMPACT_REVISION = "main"' in src,
    'all v1, v2, v3 layouts recognized': all(x in src for x in
        ('stage-g-0.6b','stage-h-compact-0.6b','stage-i-fp16-0.6b',
         'decoder_init.onnx.data','decoder_weights.data','decoder_weights_fp16.data')),
    'manifest schema v3 required for fresh downloads':
        'expected(inspected, 3)' in src and 'expected(modelManifest, 3)' in src,
    'only FP16 storage and real-speech parity accepted': all(x in src for x in
        ('shared_fp16_storage_fp32_cast','one_real_speech_and_synthetic_exact_token_parity',
         'original_stage_h_manifest_sha256','experimental_fp16_storage')),
    'contract requires FP16 packed file and cast': all(x in src for x in
        ('shared_fp16_storage_with_fp32_cast','FP16_STORAGE_FP32_COMPUTE',
         'checkContract(JSONObject(contractFile.readText()), modelManifest, 3)')),
    'strict integrity check and per-version selection':
        'ModelFileOps.verify(File(dir, asset.name)' in src and
        'localState(fp16Folder(context), 3)' in src and
        'localState(compactFolder(context), 2)' in src and
        'localState(legacyFolder(context), 1)' in src,
    'older v2 deletion requires full v3 validation':
        'fun removeCompactAfterFp16(context: Context)' in src and
        'require(verifiedFolder(context) == fp16Folder(context))' in src and
        'qwenCompactDeleteConfirm' in model,
    'previous large v1 deletion still confirmed':
        'qwenLegacyDeleteConfirm' in model and 'removeLegacyAfterUpgrade' in src,
    'encoder CPU fallback blocked': 'session.disable_cpu_ep_fallback' in runner,
    'decoder models loaded with same-directory external data':
        'env.createSession(File(modelDir,"decoder_init.onnx").absolutePath' in runner and
        'env.createSession(File(modelDir,"decoder_step.onnx").absolutePath' in runner,
}
if len(sys.argv) > 1:
    manifest = json.loads(Path(sys.argv[1]).read_text())
    names = {e['path'] for e in manifest['files']}
    checks['optional real Stage I v3 manifest'] = (manifest['schema_version'] == 3
        and 'decoder_weights_fp16.data' in names
        and 'decoder_weights.data' not in names
        and 'decoder_init.onnx.data' not in names
        and 'decoder_step.onnx.data' not in names)
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + name)
if not all(checks.values()):
    raise SystemExit(1)
print('Qwen Stage I Android source guards PASS')
