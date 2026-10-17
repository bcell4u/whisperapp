# Omi v2 / NeMo frontend verification (v6.5.20)

This is a **developer-only, offline numeric investigation**. It does not copy,
download, or ship a patient's audio, a model checkpoint, model logits, or a
transcript. It does not qualify the experimental Omi model for medical use.

## 1. Generate deterministic Kotlin fixtures

Run at the repository root on a machine with a Kotlin/JVM compiler:

```sh
kotlinc app/src/main/java/com/example/whisperrecorder/npu/ParakeetPreprocessor.kt \
  scripts/omi-parity/OmiFixtureGenerator.kt -include-runtime -d /tmp/omi-fixture.jar
java -jar /tmp/omi-fixture.jar /tmp/omi-android-fixtures
```

The cases are silence, impulse, 440 Hz tone and a partial noisy window. These
fixtures use **the actual Android Kotlin frontend code** but run offline.
They do not contain speech.

## 2. Compare with independent NeMo-compatible math (without a checkpoint)

Install NumPy, PyTorch and Librosa in a developer environment and run:

```sh
python3 scripts/omi-parity/reference_features.py \
  --pcm-dir /tmp/omi-android-fixtures \
  --output-dir /tmp/omi-independent-reference
python3 scripts/omi-parity/compare_omi_features.py \
  --android-dir /tmp/omi-android-fixtures \
  --reference-dir /tmp/omi-independent-reference \
  --out /tmp/omi-independent-comparison.json
```

The independent implementation uses NeMo's documented inference math:
preemphasis, symmetric Hann, torch.stft, magnitude-squared, 128-bin Slaney
Librosa Mel filters, log-add 2^-24, and unbiased per-channel normalization.
**Its parameters are assumed from the current Android contract; they have NOT
been read from the original Omi v1 `.nemo` checkpoint.** The reference output
includes a provenance manifest with PCM hashes to detect accidental mismatches.

On the test environment used to prepare this source, the largest valid-feature
difference across the **three non-silence** fixture types was ~0.000145 and
all masks/padding agreed exactly. Float32 normalization of constant silence
creates a roughly -0.191 per-bin difference against the mathematically zero
Android result, illustrating why numerical tolerances need a model-specific
reference, not why the Android algorithm should be changed arbitrarily.

## 3. Later: authoritative canonical reference

Obtain the original Omi Med STT v1 NeMo checkpoint/config. Confirm its
preprocessor configuration first (exact_pad, n_fft, win_size, hop, dithering in
eval mode, normalization, Mel convention, log guard, padding). Run the **same
PCM bytes** with the actual model frontend, export `name.features.f32le`
(channel-major float32 LE, `[128,1201]`) and `name.mask[1-3].f32le` (`601/301/151`)
for each case, and compare using `compare_omi_features.py`. Do not change the
original NeMo pipeline just to make it match Kotlin. Record the checkpoint
revision/hashes. Any fixed numerical acceptance gate must be justified by the
original model or conversion qualification team.

Decoder/encoder parity requires identical intermediate tensors and cannot be
inferred from the frontend comparison or transcription text. Omi Med NPU is
still experimental; earlier compiled encoder numeric qualification failed.

## 4. Inspect a device log without rereading 6,000+ lines

```sh
python3 scripts/omi-parity/analyze_omi_log.py '/path/to/latest-log.txt' \
  --output /tmp/omi-session-summary.json
```

This selects the **last completed `omi_npu` session** even if the export also
includes Parakeet/GPU runs or an incomplete trailing session. It checks chunk
completion, word-sync fallback, zero-duration counts, forced symbol advances,
nonfinite features, nonzero padding, and stop reasons. It never declares WER or
word-alignment acoustic accuracy. Exporting logs may still reveal file names;
review them before sharing.

### Regression gates

Four new Android/JVM unit tests use selected numerical golden probes from the
independent torch/Librosa run, including full and partial windows and complete
mask/padding checks. They run in the existing GitHub CI `testDebugUnitTest`.

Developer script tests:

```sh
python3 -m unittest discover -s scripts/omi-parity -p 'test_tools.py'
```

**APK building and signing:** use the unchanged `.github/workflows/build.yml`
and permanent release signing identity. Source checks do **not** demonstrate a
compiled, signed or installed APK. If the source-only v6.5.20 candidate is
accepted, run GitHub Actions and recheck the 3m lecture as `omi_npu`.
