#!/usr/bin/env python3
"""Independent NeMo-like feature reference; NOT the original Omi checkpoint.

Uses public NeMo FilterbankFeatures forward algorithm with PyTorch STFT and
Librosa's Slaney filterbank. This is useful for catching frontend port errors,
but model-specific preprocessor parameters must be checked against the true
checkpoint before these outputs can qualify the QNN encoder.

Dependencies: numpy, torch, librosa. Never uses a user's recordings by default.
"""
import argparse
import hashlib
import json
from pathlib import Path

import librosa
import numpy as np
import torch

CASES = ('silence_1s', 'impulse_1s', 'tone_12s', 'noise_partial')
FEATURES = 128
WIDTH = 1201
HOP = 160


def reference_one(pcm):
    """A stand-alone torch/librosa port of the documented NeMo math.

    Assumptions: 16kHz, 128 Slaney Mels, FFT512, window400, hop160,
    symmetric Hann, preemphasis0.97, log-add-2**-24, per-feature unbiased
    sample std plus 1e-5, center zero padding, eval dither disabled.
    These are *assumptions* until verified with canonical Omi NeMo config.
    """
    pcm = torch.from_numpy(np.asarray(pcm, dtype='<f4').copy())
    if pcm.numel() < 320 or pcm.numel() > 192000:
        raise ValueError('Input length must be between 320 and 192000 samples')
    emphasized = torch.cat((pcm[:1], pcm[1:] - 0.97 * pcm[:-1]))
    window = torch.hann_window(400, periodic=False)
    spectrum = torch.stft(emphasized, n_fft=512, hop_length=HOP,
                          win_length=400, window=window, center=True,
                          pad_mode='constant', return_complex=True)
    filters = torch.from_numpy(librosa.filters.mel(
        sr=16000, n_fft=512, n_mels=FEATURES, fmin=0, fmax=8000,
        norm='slaney').astype(np.float32))
    # Equivalent math to NeMo inference: magnitude squared, Mel, log,
    # unbiased per-channel normalization; discard final STFT guard frame.
    energy = torch.matmul(filters, spectrum.abs().pow(2))
    mel = torch.log(energy + 2**-24)
    valid = min(WIDTH - 1, pcm.numel() // HOP)
    mel = mel[:, :valid]
    # Keep float32 torch operations here (including rounding behavior on
    # silence). Do not silently modify the reference to fit Kotlin results.
    normalized = (mel - mel.mean(dim=1, keepdim=True)) / (
        mel.std(dim=1, unbiased=True, keepdim=True) + 1e-5)
    result = torch.zeros((FEATURES, WIDTH), dtype=torch.float32)
    result[:, :valid] = normalized
    masks = []
    sub = valid
    for physical in (601, 301, 151):
        sub = (sub + 1) // 2
        mask = np.zeros(physical, dtype='<f4')
        mask[:sub] = 1
        masks.append(mask)
    return result.numpy(), masks


def create(source, output):
    output.mkdir(parents=True, exist_ok=True)
    manifest = {
        'reference_kind': 'independent_nemo_math_port_not_canonical_checkpoint',
        'canonical_model_checked': False,
        'notes': 'No NeMo .nemo model loaded; not encoder/model qualification.',
        'implementation': 'torch.stft plus librosa.filters.mel',
        'torch': torch.__version__, 'librosa': librosa.__version__,
        'cases': {},
    }
    for case in CASES:
        path = source / f'{case}.pcm.f32le'
        pcm = np.fromfile(path, dtype='<f4')
        if not np.isfinite(pcm).all():
            raise ValueError(f'{case}: nonfinite PCM')
        features, masks = reference_one(pcm)
        features.astype('<f4', copy=False).tofile(output / f'{case}.features.f32le')
        for i, mask in enumerate(masks, start=1):
            mask.tofile(output / f'{case}.mask{i}.f32le')
        manifest['cases'][case] = {
            'samples': int(pcm.size),
            'pcm_sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'valid_frames': int(min(WIDTH - 1, pcm.size // HOP)),
        }
    (output / 'reference-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(manifest, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pcm-dir', required=True, type=Path)
    parser.add_argument('--output-dir', required=True, type=Path)
    args = parser.parse_args()
    if args.pcm_dir.resolve() == args.output_dir.resolve():
        parser.error('Output directory cannot equal PCM directory')
    create(args.pcm_dir, args.output_dir)


if __name__ == '__main__':
    main()
