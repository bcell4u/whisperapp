# Offline speaker diarization

Speaker diarization is an **optional, post-transcription CPU task**. It does not move the QNN Parakeet neural decoder off the NPU. Speaker transcripts are saved separately from regular transcripts, and regular transcript text is not overwritten.

## Models and packaging

- **sherpa-onnx Android runtime v1.13.8:** downloaded and SHA-256 checked in GitHub Actions. Expected AAR digest: `633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`.
- **Pyannote Segmentation 3.0 INT8:** https://huggingface.co/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0 — SHA-256 `d582f4b4c6b48205de7e0643c57df0df5615a3c176189be3fc461e9d18827b5d`.
- **3D-Speaker ERes2Net:** https://huggingface.co/csukuangfj/speaker-embedding-models — SHA-256 `1a331345f04805badbb495c775a6ddffcdd1a732567d5ec8b3d5749e3c7a5e4b`.

The two optional ONNX models are approximately 41 MB total. Their download and the speaker AAR are separate from the primary speech-recognition model downloads. Verify upstream model-specific licenses when redistributing.

## Runtime and native-library separation

sherpa-onnx includes an ONNX Runtime version that conflicts with the ORT/QNN library needed for Parakeet NPU. The CI integration isolates the speaker runtime as `libsherpa_ort.so` by **length-preserving ELF edits**, updating SONAME, DT_NEEDED, and DT_VERNEED references without shifting sections. Do not use a Gradle `pickFirst` workaround for `libonnxruntime.so`; it could break the NPU backend. `scripts/test-sherpa-elf-isolation.sh`, `scripts/isolate-sherpa-onnx.sh`, and APK audits guard this separation.

## Processing and playback

- The CPU foreground speaker service processes bounded 180-second windows with 30-second overlap instead of loading the whole interview into memory. The overlap midpoint splits duplicate regions before attribution.
- Embeddings and speaker evidence are matched across windows to maintain global labels. Automatic speaker count and fixed 1–6 are available; selecting 2 is useful for a two-person interview but cannot guarantee perfect attribution.
- Speaker assignment uses confidence-aware timing overlap. When Parakeet NPU supplies word timing, changes can split a phrase between speakers and playback can follow each word. Otherwise, phrase-timing fallback remains available. Short interruptions, cross-talk, poor acoustics, and model boundary errors can still result in `Unknown speaker` or incorrect speaker words.
- Regular and speaker transcript viewers share word timing where validated. Speaker preview, share, rename, expected speaker count, regeneration, and speaker diagnostics live in a separate speaker-focused submenu.
- Speaker transcripts and their timing metadata are separate from the regular transcript. Deletion should preserve and expose any files left behind by a failed SAF operation.

## Diagnostics and verification

The speaker service records model/runtime errors, Java/native loading information and the exception chain in private per-recording diagnostics. The app exposes copy/share actions; it does not upload these reports automatically. Generic diagnostics also record window counts, fallback timing, speaker cue formatting, and word-sync match coverage without intentionally logging spoken text.

After making native or speaker changes, validate on a physical device: NPU speech recognition, a short single-speaker file, a two-person conversation, a long interview, overlapping speech, regeneration/renaming, and speaker-only file deletion. A green GitHub build and native-symbol audit do not guarantee real-world diarization accuracy.
