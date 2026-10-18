# WhisperRecorder v6.5.40 — Retired Experimental Controls Cleanup

Built from full v6.5.39 source. The Omi synthetic NPU compatibility card, its separate IPC, temporary QNN probe implementation, and probe-only contract/test subsets have been deleted; **normal Omi NPU model installation, context metadata, loading, and transcription remain unchanged**. This change does not remove Omi support.

The Qwen model card hides optional FP16 embedding tests, encoder probe, private-copy reclaim, and manual temporary-file recovery in a collapsed Advanced section. Historical v1/v2 deletion and Stage I upgrade buttons and their now-unreferenced deletion helpers were retired; model compatibility detection and non-destructive upgrade support remain in package storage. CPU thread and CPU arena controls remain visible because they are the current profiling experiments. Existing user files are never deleted by this patch.

Automated ZIP diagnostics in Whisperapp/Logs, Qwen verified scratch cleanup, and the existing encoder/decoder code are unchanged.

Version 6.5.40 / Android code 65040. The added source verification enforces the retired-interface boundary without removing the Omi production runtime. APK compilation / hardware validation remains pending.

The update ZIP includes SegmentationPolicyTest.kt to repair the exact missing GitHub-repository file reported in workflow logs, even though it is unchanged relative to the v6.5.39 full-source archive.
