#!/usr/bin/env python3
"""Native/worker fail-closed source contract and negative mutations for synthetic-only logits."""
from pathlib import Path
import unittest

R = Path(__file__).resolve().parents[1]
NATIVE = (R/'native/qwen_bridge/qwen_npu_llama_probe.cpp').read_text()
WORKER = (R/'app/src/main/java/com/example/whisperrecorder/qwen/Qwen3HybridWorkerService.kt').read_text()


def check_contract(native, worker):
    return all(x in native for x in (
        'WR_QWEN_SYNTHETIC_LOGITS_V1',
        'argc == 8 && std::string(argv[2]) == "--multi"',
        'std::string(argv[6]) == "--synthetic-logits"',
        'std::string(argv[7]) == "fixture"',
        'qwen-prefill-logits-',
        'std::isfinite(prefill_logits[i])')) and all(x in worker for x in (
        'WR_QWEN_SYNTHETIC_LOGITS_V1',
        '"--synthetic-logits", "fixture"',
        'QwenLabLogitReplay.compareFiles',
        'QwenLabLogitExport.save'))


class ContractTest(unittest.TestCase):
    def test_positive(self): self.assertTrue(check_contract(NATIVE, WORKER))
    def test_missing_native_flag(self):
        self.assertFalse(check_contract(NATIVE.replace('"--synthetic-logits"', '"--not-supported"'), WORKER))
    def test_missing_worker_protocol(self):
        self.assertFalse(check_contract(NATIVE, WORKER.replace('WR_QWEN_SYNTHETIC_LOGITS_V1', 'BAD_PROTOCOL')))
    def test_missing_app_guard(self):
        self.assertFalse(check_contract(NATIVE, WORKER.replace('QwenLabLogitReplay.compareFiles', 'noReplayCheck')))

if __name__ == '__main__':
    unittest.main(verbosity=2)
