# Lab v6.6.9 host batch path diagnostic build

- Experimental app versionName `6.6.9-dev`; Android versionCode `66009`; GitHub prerelease tags `v6.6.9-lab.*`.
- Separate Lab applicationId and existing Android signing variable names retained.
- On-device performance/inference is identical to 6.6.7; only version metadata and hosted verification code changed.
- A separate manually dispatched Actions workflow runs the S26 synthetic Q8 decoder reference on a hosted x86 CPU. Its output is diagnostic, **NOT** official PyTorch/ONNX M-RoPE parity.
