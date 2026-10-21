#!/usr/bin/env python3
"""Static prompt contract regression gate. Does NOT assert transcription accuracy."""
from pathlib import Path
import re
import unittest

SOURCE = Path(__file__).resolve().parents[1] / 'native/qwen_bridge/qwen_npu_llama_probe.cpp'
GOOD = [151644,8948,198,151645,198,151644,872,198,151669]
SUFFIX = [151670,151645,198,151644,77091,198]


def ids(source: str, name: str) -> list[int]:
    match = re.search(r'constexpr\s+llama_token\s+' + name +
                      r'\[\]\s*=\s*\{([^}]+)\}', source)
    if not match:
        raise ValueError('missing native ' + name)
    return [int(value.strip()) for value in match.group(1).split(',')]


def valid(source: str) -> bool:
    return ids(source, 'prefix') == GOOD and ids(source, 'suffix') == SUFFIX


class PromptContractTest(unittest.TestCase):
    def test_current_source(self):
        src = SOURCE.read_text(encoding='utf-8')
        self.assertTrue(valid(src), 'Qwen prompt differs from canonical chat roles')
        self.assertIn('WR_QWEN_PROMPT_ROLE_8948_872_V1', src)

    def test_legacy_invalid_roles_are_rejected(self):
        src = SOURCE.read_text(encoding='utf-8')
        mutant = src.replace('151644,8948,198,151645,198,151644,872,198,151669',
                             '151644,9125,198,151645,198,151644,882,198,151669')
        self.assertNotEqual(src, mutant)
        self.assertFalse(valid(mutant))

    def test_wrong_suffix_rejected(self):
        src = SOURCE.read_text(encoding='utf-8')
        mutant = src.replace('151670,151645,198,151644,77091,198',
                             '151670,151645,198,151644,77091,199')
        self.assertFalse(valid(mutant))

    def test_missing_prefix_is_rejected(self):
        with self.assertRaises(ValueError):
            valid('no prompt here')

if __name__ == '__main__':
    unittest.main(verbosity=2)
