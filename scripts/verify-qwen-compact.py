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
    'v2 compact HF main': 'private const val COMPACT_REVISION = "main"' in src,
    'shared external decoder weights': 'decoder_weights.data' in src and 'decoder_init.onnx.data' in src,
    'two distinct versioned model stores': 'stage-h-compact-0.6b' in src and 'stage-g-0.6b' in src,
    'manifest both schema versions': 'schemaVersion == 2' in src and 'localState(legacyFolder(context), 1)' in src,
    'lossless decoder smoke required': 'exact_cpu_onnx_decoder_token_smoke_pass' in src,
    'strict file hash verification': 'ModelFileOps.verify(File(dir, asset.name)' in src,
    'v2 assets only for fresh downloads': 'expected(inspected, 2)' in src and 'url(asset.name)' in src,
    'same-directory external data loaded by ORT': 'env.createSession(File(modelDir,"decoder_init.onnx").absolutePath' in runner and 'env.createSession(File(modelDir,"decoder_step.onnx").absolutePath' in runner,
    'hardware CPU fallback blocked': 'session.disable_cpu_ep_fallback' in runner,
    'old package deletion is opt-in': 'qwenLegacyDeleteConfirm' in model and 'removeLegacyAfterUpgrade' in model,
    'old package deletion requires full checksum pass': 'require(verifiedFolder(context) == compactFolder(context))' in src,
    'runtime JSON contract checked': 'checkContract(JSONObject(contractFile.readText()), modelManifest, 2)' in src,
}
if len(sys.argv) > 1:
    manifest = json.loads(Path(sys.argv[1]).read_text())
    names = {e['path'] for e in manifest['files']}
    checks['optional real Stage H manifest'] = (manifest['schema_version'] == 2
        and 'decoder_weights.data' in names
        and 'decoder_init.onnx.data' not in names
        and 'decoder_step.onnx.data' not in names)
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + name)
if not all(checks.values()):
    raise SystemExit(1)
print('Qwen Stage H Android source guards PASS')
