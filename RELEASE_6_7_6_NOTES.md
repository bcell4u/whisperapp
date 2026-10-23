# WhisperRecorder 6.7.6 — restore skipped prerequisite changes

`releaseVersion=6.7.6`, `versionName=6.7.6`, `versionCode=67006`.

Run `108363851626` failed in source-level audit (3 checks):
- microphone preflight missing `RECORD_AUDIO` guard markers;
- WAV and compressed recorder missing permission guard markers;
- GitHub build missing lint summary / fail-closed gate markers.

All these checks pass on the provided *complete* 6.7.5 ZIP. The most likely
cause is applying the 6.7.5 changed-files ZIP atop a GitHub checkout that
missed the v6.7.3 and/or v6.7.4 updates. The run log does not provide file
contents, so repository drift is inferred, not proven by a byte-for-byte diff.

The v6.7.6 **CHANGED-FILES-ONLY ZIP is intentionally cumulative** for these
prerequisite paths. It includes the canonical `MicrophoneCapabilityProbe.kt`,
`WavPcmRecorder.kt`, `RecordingService.kt`, `.github/workflows/build.yml`, and
`scripts/summarize-android-lint.py`, even though they match 6.7.5 full source.
Also includes incremental 6.7.6 version changes, audit guidance and docs.

Model card download status, auto-select, Qwen encoding/decoding progress,
QNN+llama.cpp, Omi/Parakeet/Whisper, and debug-only automatic prereleases are
preserved. The lint gate remains on. Do not publish if lint fails.

Validation is source-only; GitHub Android build, all lint findings, and S26
transcription / playback remain unverified for this candidate.
