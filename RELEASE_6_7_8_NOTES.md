# WhisperRecorder 6.7.8 — real Qwen transcription progress

- Replaced endlessly moving Qwen progress bars in the library and recording details with a real, count-based fixed bar.
- Encoder updates occur after an actual QNN audio window completes.
- Native decoder already emits one per-window metric. The worker now sends only the completed-window count over a private oneway AIDL callback; no audio, transcript or token IDs are sent.
- The bar measures completed work units across **two window passes** (encoding and decoding). It does **not** estimate elapsed-time percentage. It stays stationary while the decoder model loads or a long window is decoding.
- Both the on-screen count and foreground notification reflect decoder-window completions.
- Inference, weights, model download, and legacy production engines remain unchanged.
- Requires a new GitHub Android build and S26 device verification; host static checks are not device proof.
