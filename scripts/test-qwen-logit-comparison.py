#!/usr/bin/env python3
"""Offline numerical comparison contract: positive, negative, invalid-size, NaN."""
import importlib.util
import pathlib
import struct
import tempfile
import unittest

p = pathlib.Path(__file__).with_name('compare-qwen-prefill-logits.py')
spec = importlib.util.spec_from_file_location('qwen_cmp', p)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

class Tests(unittest.TestCase):
    def values(self):
        return tuple(float(i % 23) / 13 for i in range(m.VOCAB))
    def test_equal(self):
        a = self.values()
        r = m.compare(a, a)
        self.assertEqual(r['rmse'], 0)
        self.assertEqual(r['max_abs_delta'], 0)
        self.assertTrue(r['top1_equal'])
    def test_changed_logits(self):
        a = self.values()
        b = list(a)
        b[0] += 50
        r = m.compare(a, b)
        self.assertGreater(r['rmse'], 0)
        self.assertFalse(r['top1_equal'])
    def test_truncated_and_nonfinite_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            path = pathlib.Path(td)/'bad.f32le'
            path.write_bytes(b'bad')
            with self.assertRaises(ValueError): m.read_logits(path)
            data = bytearray(m.BYTES)
            struct.pack_into('<f', data, 0, float('nan'))
            path.write_bytes(data)
            with self.assertRaises(ValueError): m.read_logits(path)

if __name__ == '__main__':
    unittest.main(verbosity=2)
