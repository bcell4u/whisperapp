# WhisperRecorder v6.5.4 – sherpa ELF version-needs fix

## Evidence

In v6.5.3, Android 17 on an SM-S942U reported `UnsatisfiedLinkError`
(`cannot find "nx_OfflineSpeakerDiarizat" from verneed[0] in DT_NEEDED list`)
when loading `libsherpa-onnx-jni.so`. Both native libraries and the Java
classes were present, and both speaker model files were downloaded.

The previous v6.5.1 build step used `patchelf --replace-needed` to expand
`libonnxruntime.so` into `libonnxruntime_sherpa.so`. This can relocate
ELF dynamic string-table data while version-needs records still refer to
original offsets. The Android dynamic linker interprets a stale `vn_file`
offset as unrelated text and rejects the library.

## Repair

Replace only the dependency name in existing allocated ELF string bytes;
`libsherpa_ort.so` is shorter than `libonnxruntime.so`. Leave original
byte size, offsets, load segments, and ABI untouched. Replace SONAME, needed
and version-needed strings together. Verify every VERNEED dependency
against the updated DT_NEEDED list. Fail CI if any check is violated.

The patch does not modify the Parakeet NPU ORT/QNN library, acoustic model,
transcription logic, or speaker algorithms. The previous 15-minute
diariazation limit is unchanged. A real Android runtime test is required.

## Additional fix

The final transcription notification now says `recording title is ready`
without the untranscribed-segment count. Gap markers and error diagnostics
are retained internally and in the transcript.
