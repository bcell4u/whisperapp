#!/usr/bin/env python3
"""Offline unit tests for log QA and feature-comparison safety checks."""
import tempfile
import unittest
from pathlib import Path

from analyze_omi_log import summarize
from compare_omi_features import compare


class LogSummaryTests(unittest.TestCase):
    def test_chooses_latest_completed_omi_run_and_checks_guards(self):
        lines = [
            '2026-01-01 SESSION start recording=x engine=omi_npu model=Omi',
            '2026-01-01 inference complete engine=omi_npu chunk=1/1 stop=frames-exhausted symbolGuards=9 zeroDurations=2 featureNonFinite=0 paddingNonzero=0',
            '2026-01-01 npu word-sync engine=omi_npu chunk=1/1 mode=native reason=exact',
            '2026-01-01 SESSION complete recording=x audioDurationMs=12000 wallTimeMs=900 chunks=1',
            '2026-01-02 SESSION start recording=y engine=omi_npu model=Omi',
            '2026-01-02 inference complete engine=omi_npu chunk=1/1 stop=frames-exhausted symbolGuards=0 zeroDurations=0 featureNonFinite=0 paddingNonzero=0',
            '2026-01-02 npu word-sync engine=omi_npu chunk=1/1 mode=native-partial-overlap reason=exact',
            '2026-01-02 SESSION complete recording=y audioDurationMs=10000 wallTimeMs=600 chunks=1',
            '2026-01-03 SESSION start recording=z engine=omi_npu model=Omi',
        ]
        report = summarize(lines)
        self.assertEqual(report['wall_time_ms'], 600)
        self.assertEqual(report['symbol_limit_advances'], 0)
        self.assertEqual(report['word_sync_modes']['native-partial-overlap'], 1)
        self.assertFalse(report['warnings'])

    def test_partial_or_missing_sync_not_silently_accepted(self):
        lines = [
            'SESSION start recording=z engine=omi_npu model=Omi',
            'inference complete engine=omi_npu chunk=1/2 stop=frames-exhausted symbolGuards=0 zeroDurations=0 featureNonFinite=0 paddingNonzero=0',
            'SESSION complete audioDurationMs=22000 wallTimeMs=800 chunks=2',
        ]
        report = summarize(lines)
        self.assertEqual(report['missing_chunks'], [2])
        self.assertIn('missing_or_inconsistent_inference_chunks', report['warnings'])
        self.assertIn('missing_word_sync_records', report['warnings'])


class FeatureSafetyTests(unittest.TestCase):
    def test_does_not_allow_same_directory_for_reference(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, 'independent'):
                compare(Path(tmp), Path(tmp))


if __name__ == '__main__':
    unittest.main()
