package com.example.whisperrecorder.speaker

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.whisperrecorder.diagnostics.DiagnosticLogger
import com.example.whisperrecorder.data.RecordingItem
import com.example.whisperrecorder.data.RecordingStore
import com.example.whisperrecorder.data.SafStorage
import com.example.whisperrecorder.data.WorkspaceStorage
import com.example.whisperrecorder.transcribe.PcmDecoder
import com.example.whisperrecorder.transcribe.TranscriptTimelineStore
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.IOException

/** On-demand CPU-only offline pipeline. Never invoked by normal ASR. */
object OfflineSpeakerService {
    private const val WINDOW_SECONDS = 180 // At most 3 minutes of PCM per native invocation.
    private const val OVERLAP_SECONDS = 30 // Shared evidence for identity continuity.

    fun generate(context: Context, item: RecordingItem, timestamps: Boolean, expectedSpeakers: Int, onProgress: (String) -> Unit): Uri {
        val media = item.audioUri ?: error("Recording audio is no longer available")
        val timeline = TranscriptTimelineStore.load(item.transcriptTimingPath)
            ?: error("This transcript has no timing data. Please transcribe again first.")
        require(timeline.cues.isNotEmpty()) { "Transcript has no timestamped speech cues" }
        check(SpeakerModels.verify(context)) { "Speaker models are missing or failed verification" }
        val (segModel, embModel) = SpeakerModels.paths(context)
        val clusterCount = SpeakerClusteringPolicy.normalize(expectedSpeakers)
        val config = OfflineSpeakerDiarizationConfig(
            segmentation = OfflineSpeakerSegmentationModelConfig(
                pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(model = segModel.absolutePath, windowShiftRatio = 0.1f)
            ),
            embedding = SpeakerEmbeddingExtractorConfig(model = embModel.absolutePath),
            clustering = FastClusteringConfig(
                numClusters = clusterCount,
                threshold = SpeakerClusteringPolicy.AUTO_THRESHOLD,
                computeConfidence = clusterCount != 1
            )
        )
        val stitcher = SpeakerWindowStitcher(clusterCount)
        val overlapMerge = SpeakerWindowMerge()
        val intervals = mutableListOf<SpeakerInterval>()
        var windowsProcessed = 0
        // Both neural objects are reused. Neither the whole audio nor a long PCM cache is held.
        val diarizer = OfflineSpeakerDiarization(config = config)
        try {
            require(diarizer.sampleRate() == 16_000) { "Unexpected speaker model sample rate" }
            val extractor = SpeakerEmbeddingExtractor(
                config = SpeakerEmbeddingExtractorConfig(model = embModel.absolutePath)
            )
            try {
                PcmDecoder.prepare(context, Uri.parse(media)).use { prepared ->
                    val totalMs = prepared.info.durationMs
                    prepared.open16kSegmentStream(WINDOW_SECONDS, OVERLAP_SECONDS).use { stream ->
                        while (stream.hasNext()) {
                            val window = stream.next()
                            if (window.samples.size < 16_000) continue
                            windowsProcessed++
                            onProgress("Identifying speakers: window $windowsProcessed" +
                                if (totalMs > 0) " (${(window.startMs * 100 / totalMs).coerceIn(0, 99)}%)…" else "…")
                            val local = diarizer.process(window.samples).map {
                                SpeakerInterval(window.startMs + (it.start * 1000).toLong(),
                                    window.startMs + (it.end * 1000).toLong(), it.speaker, it.confidence)
                            }
                            // Runtimes renumber speakers for every process() call.
                            // Re-extract one voice vector per local identity, then reconcile
                            // it with a rolling cross-window centroid and boundary evidence.
                            val embeddings = local.map { it.speaker }.filter { it >= 0 }.distinct().mapNotNull { localId ->
                                val audio = SpeakerWindowAudio.representative(window.samples, window.startMs, local, localId)
                                    ?: return@mapNotNull null
                                val speakerStream = extractor.createStream()
                                try {
                                    speakerStream.acceptWaveform(audio, 16_000)
                                    speakerStream.inputFinished()
                                    if (extractor.isReady(speakerStream)) {
                                        localId to extractor.compute(speakerStream)
                                    } else null
                                } finally { speakerStream.release() }
                            }.toMap()
                            val mapped = stitcher.map(local, embeddings)
                            intervals += overlapMerge.push(window.startMs, window.endMs, mapped)
                            DiagnosticLogger.append(context, "speaker window=$windowsProcessed " +
                                "startMs=${window.startMs} endMs=${window.endMs} " +
                                "localSpeakers=${local.map { it.speaker }.distinct().size} " +
                                "matchedSpeakers=${mapped.map { it.speaker }.filter { it >= 0 }.distinct().size}")
                        }
                    }
                }
                intervals += overlapMerge.finish()
            } finally { extractor.release() }
        } finally { diarizer.release() }
        require(windowsProcessed > 0) { "Recording is too short for speaker identification" }
        check(intervals.any { it.speaker >= 0 }) {
            "No reliable speaker identities could be distinguished in this audio"
        }
        val cues = timeline.cues.map { SpeakerTextCue(it.startMs, it.endMs, it.text) }
        val labeled = SpeakerTranscript.align(cues, intervals)
        require(labeled.any { it.speaker != null }) { "No speaker intervals aligned with the transcript" }
        // A single phrase-timed fallback must not disable word grouping for the whole recording.
        // Keep the original word cues untouched for playback and speaker attribution.
        val wordCues = timeline.cues.count { cue -> cue.text.none(Char::isWhitespace) }
        val grouped = if (wordCues > 0) SpeakerTranscript.groupWords(labeled) else labeled
        val transcript = SpeakerTranscript.render(grouped, timestamps)
        DiagnosticLogger.append(context, "speaker transcript formatting cues=${labeled.size} " +
            "wordCues=$wordCues groupedLines=${grouped.size} source=mixed-timing-safe")
        // No words, names, audio or private paths enter the exported transcription log.
        val unknown = labeled.count { it.speaker == null }
        val confidences = intervals.map { it.confidence }.filter { it.isFinite() && it in -1f..1f }
        DiagnosticLogger.append(context, "speaker attribution expected=$clusterCount windows=$windowsProcessed intervals=${intervals.size} " +
            "cues=${labeled.size} unknown=$unknown wordCues=$wordCues confidenceAvailable=${confidences.size} " +
            "meanConfidence=${if (confidences.isEmpty()) "unavailable" else "%.3f".format(java.util.Locale.ROOT, confidences.average())}")
        onProgress("Saving speaker transcript…")
        val treeUri = Uri.parse(item.treeUri)
        val folder = WorkspaceStorage.transcriptsDirectory(context, treeUri)
        val originalName = runCatching {
            item.transcriptUri?.let { raw ->
                context.contentResolver.query(Uri.parse(raw), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }
        }.getOrNull() ?: "${item.title}.txt"
        val existing = SafStorage.listDirectory(context, treeUri, folder).map { it.name }.toSet()
        val filename = (1..10_000).asSequence().map { SpeakerTranscript.filename(originalName, it) }
            .firstOrNull { it !in existing } ?: throw IOException("Too many speaker transcripts")
        val dest = SafStorage.createDocumentInDirectory(context, folder, "text/plain", filename)
        try {
            requireNotNull(context.contentResolver.openOutputStream(dest, "w")) { "Cannot write transcript" }
                .bufferedWriter().use { it.write(transcript) }
        } catch (t: Throwable) {
            SafStorage.deleteDocument(context, dest)
            throw t
        }
        runCatching {
            TranscriptTimelineStore.saveSpeaker(context, item.id, timeline)
        }.onFailure { DiagnosticLogger.appendError(context, "speaker word-sync sidecar save failed", it) }
        RecordingStore.setSpeakerTranscript(context, item.id, dest.toString())
        return dest
    }
}
