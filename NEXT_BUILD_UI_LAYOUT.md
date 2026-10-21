# Pending UI layout refinement (not a release)

**Baseline:** v6.5.12; **proposed subsequent release:** v6.5.13 (65013) when requested.  The source still declares v6.5.12 / 65012. **No Gradle build or Android APK was produced.**

Changes staged on top of the previously deferred ~6.5% active-word font-size effect and one dead-method cleanup:

- Recording submenu, **Transcript** card: replace the full-width **Share transcript** button with a document-and-share icon in the card header. Add an adjacent media-share icon, using an audio-note or video-frame variant according to the recording, so media sharing is available from the regular recording submenu. The actions call the existing `ShareManager` methods; content descriptions name each share action. The transcript-share action is disabled while the transcript text is loading; media sharing is shown only for available media.
- Dedicated speaker submenu, **Speaker transcript** card: add a single transcript-share icon in the upper-right corner. Remove the separate **Share speaker transcript** and **Share audio/video** buttons. Speaker transcript sharing calls the unchanged callback and preserves the saved text attachment behavior.
- Separate **Speaker settings** outlined panel after the preview: expected speaker count, create/regenerate action, rename speaker labels (only after a speaker transcript exists), progress/model help and optionally speaker diagnostics. Diarization processing, name editing, word sync and the player are otherwise unchanged.
- Reuse a single tiny `TranscriptShareIcon` composable and three vector drawables (text, audio, video). No new library, inference path, storage path or service.

## Validation and limitations

- Static source verifier and local XML syntax checks pass. References and share-action placement were checked from source.
- The complete ZIP and cumulative patch were compared against v6.5.12 source.
- **No Gradle compilation, instrumented tests, APK or physical-device UI validation.** In a future build, check button tap targets/content descriptions, chooser MIME/types, narrow-screen title truncation, and panel layout with large accessibility fonts.
- Do not upgrade version metadata or imply v6.5.13 was released until a future build is explicitly requested.
