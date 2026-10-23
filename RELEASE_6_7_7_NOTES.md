# WhisperRecorder 6.7.7 — fix Android lint errors

Version 6.7.7 / versionCode 67007.

GitHub Actions run `108367174862` passed the production and Qwen source gates,
compiled the ARM64 native decoder, ran JVM tests, built and audited the debug
APK, but lint failed with five errors and nine warnings. The lint gate blocked
publication. The improved log exposed all issue locations.

**Corrections:**

1. `MainActivity.kt`: collect three runtime StateFlows as observable Compose
   StateFlows directly, not with `collectAsState(initial = Runtime.state.value...)`.
2. `MainActivity.kt`: likewise observe recording state for library updates.
3. `TranscriptionService.kt`: remove the unintended extra indent on the
   progress update after `val progress = ...`. The emitted state is unchanged.
4. Add two strict source checks so those lint defects are rejected before
   spending time on Android/NDK compilation.

Retains Qwen3-ASR 0.6B QNN+llama.cpp, model download progress, automatic model
selection, Qwen encoding progress and all other installed production engines.
No lint checks were disabled; no experimental diagnostics were reintroduced.

**Warning review (not release blockers):** `OldTargetApi`, dependency update
suggestions, `ChromeOsAbiSupport`, `WakelockTimeout`, `ObsoleteSdkInt`,
`VectorPath`, and two `MonochromeLauncherIcon` warnings. Versions, ABI
architecture, and visual assets were not changed speculatively.

Local static checks and package reconstruction are not equivalent to a real
6.7.7 Android lint pass or a Snapdragon phone test.
