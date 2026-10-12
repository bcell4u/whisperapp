# WhisperRecorder v6.5.31 — Qwen storage alignment

- Store the durable Qwen package under the existing **user-selected Whisperapp/Models/qwen3-asr-0.6b/** folder via Android Storage Access Framework (SAF), using Stage I/II/III package-specific subdirectories.
- New installs archive verified model files into that folder automatically.
- Settings > Models & Downloads adds **Save existing Qwen to Whisperapp/Models (no download)** for already installed app-private models. It copies original bytes through SAF, checks SHA-256 after each copy, and publishes the READY marker last. No HTTP download is requested.
- The user-visible archive survives normal app upgrades and uninstall if the selected directory remains; an ONNX/QNN-compatible app-private runtime copy is retained because ORT needs local adjacent external-data paths. Restoring from a valid archive is offline and verifies hashes. Expect extra storage use: archiving existing multi-GB weights needs free space for an additional full copy. No automatic deletion of the user-visible archive.
- Current installed Qwen versions and other ASR model pipelines are preserved. Never uninstall the current app during migration.
- Version `6.5.31` / build code `65031`. Android/physical S26 test pending.
