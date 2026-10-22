# WhisperRecorder Qwen GGUF Lab 6.6.0 — built-in model downloads

## How to use the updated Lab

1. Install the **new APK built from this source** (the earlier 6.6.0 APK will not gain this button by itself).
2. Open **Qwen GGUF Lab**, and tap **Download missing encoder + Q8 GGUF from Hugging Face**. The download begins only after this tap. Budget ~1.2 GB plus working storage and use reliable Wi-Fi.
3. The app fetches `model-manifest.json`, `encoder-qnn-fp16.bin`, `tokenizer.json`, and the two `golden-synthetic-*.raw` fixtures from `drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81`, checks lengths/digests against the manifest and the original Lab verification contract, and installs them under this Lab's private files directory.
4. It fetches only `Qwen3-ASR-0.6B-Q8_0.gguf` from `ggml-org/Qwen3-ASR-0.6B-GGUF` pinned to revision `928ab95`, verifies **804,749,248 bytes** and SHA-256 `bca259818b50ca7c4c05e9bdb35a5dc04fa039653a6d6f3f0f331f96f6aa1971`, and installs it into Lab-private storage. The 214 MB `mmproj` file is intentionally not used by the NPU-feature-injection experiment.
5. Choose lecture audio and tap **TRANSCRIBE**. NPU runtime support and actual Android execution remain device-validation gates.

## If you have the decoder already

The NPU Builder source points to:

`<Builder root>\\workspace\\llama-phone-benchmark\\cache\\models\\Qwen3-ASR-0.6B-Q8_0.gguf`

Transfer that **one GGUF file** to phone storage and choose **Select Q8 GGUF model**. The Lab copies it into Lab-private storage when you run the transcription. If its hash matches the pinned public file, a later automatic download will skip it.

The Builder's cache may have been cleaned or moved; there is no way for this APK to access the Windows directory directly. The source/assets ZIP does not contain either model or private recordings.

## Safety and limitations

- Direct HTTPS from the app to Hugging Face, following HTTPS redirects only. No passwords, authentication tokens, ADB, Windows Builder, audio files or transcripts are uploaded. The model host/CDN will see an IP address as with any download.
- Every large downloaded model file has a SHA-256 check and is committed by rename only after verification. An interrupted/cancelled download deletes the exact app-owned `.part` file and retains the previous verified destination. Retries restart the interrupted file rather than resume it.
- The encoder's large-asset hashes are provided by Hugging Face's `model-manifest.json` over HTTPS; the remote encoder revision is not cryptographically pinned in this prototype. Review/pin a known manifest SHA or commit before promoting this to a production release.
- Downloads require enough private device storage and a compatible SM-S942U Qualcomm QNN runtime, and may fail during Hugging Face/network outages. The manual encoder and GGUF import paths are preserved as fallbacks.
- Both source repositories redistribute Qwen-family weights; retain upstream Apache-2.0 attributions and any applicable model notices.
- This is an offline/source-tested feature, NOT an APK-compiled or phone-verified claim. The release workflow remains push-to-main and emits a Lab prerelease APK only after passing CI checks.
