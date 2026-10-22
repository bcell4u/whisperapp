"""Regression for v0.4.94 quant benchmark rejecting a valid CPU-tuned probe.

The Q8/Q4/Q5 experiment holds four batch and decoder threads fixed. It
requires the CPU_TUNE receipt, NOT the dual-thread CLI protocol; the matched
4/4,3/3,2/2,4/4 experiment retains the stronger requirement.
"""
import argparse
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import qwen_bridge_full_benchmark as bench


class QuantProbeContract(unittest.TestCase):
    def _check_before_staging(self, *, quant=False, dual=False, marker=b''):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            (root/'results').mkdir()
            (root/'cache'/'models').mkdir(parents=True)
            (root/'cache'/'models'/bench.MODEL_NAME).write_bytes(b'GGUF')
            probe=root/'native-probe';probe.write_bytes(marker)
            q4=root/'q4.gguf';q4.write_bytes(b'GGUF')
            args=argparse.Namespace(adb='adb',serial='PRIVATE',ffmpeg='ffmpeg',
                audio='audio',qnn_context='context',runtime_manifest='runtime',
                cache=str(root/'cache'),probe_archive=str(probe),results=str(root/'results'),
                max_seconds=36,threads=4,steps=96,full_trials=3,
                cpu_thread_sweep=False,dual_thread_sweep=dual,temperature_match=False,
                quant_compare=quant,quant_q4_model=str(q4) if quant else None,
                quant_q5_model=None,ack_provisional_accuracy=True)
            def must_not_stage(*a):
                raise AssertionError('Phone staging must not occur during probe preflight')
            quant_meta={'quant':'Q4_K_M','sha256':'b'*64,'gguf_file_type':15,
                        'gguf_architecture':'qwen3vl','bytes':400000000,'metadata_verified':True}
            with patch.object(bench,'verify_existing_accuracy',return_value={}),\
                 patch.object(bench,'read_model',return_value={'sha256_verified':True}),\
                 patch.object(bench,'verify_quant_model',return_value=quant_meta),\
                 patch.object(bench,'validate_encoder_context',return_value={'bytes':100}),\
                 patch.object(bench,'runtime_paths',return_value={}),\
                 patch.object(bench,'extract_probe',return_value=probe):
                code,report=bench.run(args,hooks={'engine':must_not_stage})
            self.assertEqual(code,2)
            with zipfile.ZipFile(report) as zipf:
                row=json.loads(zipf.read('manifest.json'))
            self.assertEqual(row['phone_all_files_cleanup'],'not_staged')
            self.assertEqual(row['error_stage'],'local_preflight')
            return row

    def test_quant_rejects_multi_only_probe_no_phone_staging(self):
        row=self._check_before_staging(quant=True,marker=bench.MULTI_PROTOCOL)
        self.assertEqual(row['error_code'],'NEW_CPU_TUNE_ARM64_PROBE_REQUIRED_BEFORE_PHONE_STAGE')
        self.assertFalse(row['probe_has_cpu_tune_protocol'])

    def test_dual_sweep_still_requires_dual_protocol(self):
        row=self._check_before_staging(dual=True, marker=bench.MULTI_PROTOCOL+bench.CPU_TUNE_PROTOCOL)
        self.assertEqual(row['error_code'],'NEW_DUAL_THREAD_ARM64_PROBE_REQUIRED_BEFORE_PHONE_STAGE')
        self.assertFalse(row['probe_has_dual_tune_protocol'])

    def test_version_and_local_runner_contract(self):
        root=Path(__file__).resolve().parent.parent
        self.assertEqual((root/'VERSION.txt').read_text().strip(),'0.4.96')
        src=(root/'python/qwen_bridge_full_benchmark.py').read_text()
        self.assertIn("(tune or quant_compare or q8_fast) else [])",src)
        self.assertIn('if dual else',src)
        self.assertIn("or quant_compare or q8_fast) and CPU_TUNE_PROTOCOL",src)

if __name__ == '__main__':
    unittest.main()
