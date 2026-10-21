package com.example.whisperrecorder.qwen

import android.net.Uri
import android.os.Bundle
import android.os.SharedMemory
import android.system.OsConstants
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.whisperrecorder.BuildConfig
import com.example.whisperrecorder.R
import com.example.whisperrecorder.npu.QwenDecoderRunConfig
import com.example.whisperrecorder.transcribe.PcmDecoder
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Isolated, opt-in Android QNN->GGUF smoke app; no installed production data access.
 * Only the native decoder process receives feature files, all scoped to this app's cache.
 * The full transcript is displayed privately on screen, never in diagnostic logs.
 */
class QwenGgufLabActivity : ComponentActivity() {
    private var modelUri: Uri? = null
    private var audioUri: Uri? = null
    private val chooseEncoderDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) importEncoder("folder") { QwenLabEncoderAssets.importFolder(this, uri) }
    }
    private val chooseEncoderZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importEncoder("ZIP") { QwenLabEncoderAssets.importZip(this, uri) }
    }
    private val chooseSavedModels = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) restoreSavedModels(uri)
    }

    private val chooseReference = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy.get()) importVerificationReference(uri)
    }
    private var activeClient: Qwen3HybridClient? = null
    private val busy = AtomicBoolean(false)
    private val downloadCancelled = AtomicBoolean(false)
    private lateinit var output: TextView
    private lateinit var run: Button
    private lateinit var overlapControl: CheckBox
    private val chooseModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy.get()) {
            modelUri = uri
            showMessage("Q8 GGUF selected. Its contents will be copied only into this Lab app's private storage.")
        }
    }
    private val chooseAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy.get()) {
            audioUri = uri
            showMessage("Audio selected. The Lab will read it through Android's media decoder without modifying it.")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.QWEN_GGUF_LAB) { finish(); return }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 24, 26, 24)
        }
        fun button(title: String, action: () -> Unit) {
            layout.addView(Button(this).apply {
                text = title
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        layout.addView(TextView(this).apply {
            textSize = 20f
            text = "Qwen GGUF Lab v${getString(R.string.app_version_name)} · experimental"
        })
        layout.addView(TextView(this).apply {
            text = "Experimental S26 SM-S942U · genuine QNN/HTP encoder + 4-thread " +
                "Q8_0 llama.cpp dot-product decoder. 12 s chunks, at most 240 s. " +
                "Transcription uses private model copies. After model setup, a verified backup " +
                "can also be saved under Download/SFSU/Whisperapp/Models to survive uninstall."
        })
        layout.addView(TextView(this).apply {
            text = "Model setup: tap Download to fetch the S26 QNN encoder and Q8 GGUF " +
                "directly from Hugging Face (~1.2 GB; Wi-Fi and adequate free storage recommended). " +
                "No download starts without tapping the button. Existing imported files are reused " +
                "when their checksums match. The Windows Builder may already have a Q8 GGUF " +
                "you can transfer and select below. mmproj and ONNX decoder files are NOT required. " +
                "A second saved copy uses about 1.2 GB of additional storage."
        })
        button("Download missing encoder + Q8 GGUF from Hugging Face") { downloadModels() }
        button("Back up verified models to Downloads / SFSU / Whisperapp / Models") { backupModels() }
        button("Restore models from Downloads after reinstall") { chooseSavedModels.launch(null) }
        button("Import extracted QNN encoder folder") { chooseEncoderDirectory.launch(null) }
        button("Or import QNN encoder ZIP") { chooseEncoderZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
        button("Select Q8 GGUF model") { chooseModel.launch(arrayOf("application/octet-stream", "*/*")) }
        button("Select lecture audio (up to 4 min)") { chooseAudio.launch(arrayOf("audio/*", "video/*")) }
        button("VERIFY DECODER (synthetic fixture, no lecture)") { verifyDecoderOnce() }
        button("Import independent reference token JSON (optional)") {
            chooseReference.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        button("Clear reference JSON (return to internal-only test)") {
            if (!busy.get()) {
                File(filesDir, "qwen-gguf-lab-reference.json").delete()
                showMessage("Reference cleared. Verification can still run internal tests, but cannot qualify external token parity.")
            }
        }
        overlapControl = CheckBox(this).apply {
            text = "Experimental: overlap segments by 1 second (may improve words at boundaries; slower)"
            isChecked = true
        }
        layout.addView(overlapControl)
        run = Button(this).apply {
            text = "TRANSCRIBE once with on-device QNN + GGUF"
            setOnClickListener { startOnce() }
        }
        layout.addView(run)
        button("Cancel download / Lab session") {
            downloadCancelled.set(true)
            runCatching { activeClient?.cancelCurrent() }
            showMessage("Cancellation requested. Only Lab-owned temporary files will be removed.")
        }
        output = TextView(this).apply {
            textSize = 15f
            text = "Download the required encoder + Q8 GGUF above, or import your own " +
                "compatible encoder folder/ZIP and Q8 GGUF. Then select lecture audio. " +
                "After downloading models, a verified copy is also saved to Downloads. " +
                "After reinstall, choose Restore models and select the Models subfolder. " +
                "The transcript is not uploaded. Diagnostic logs save under Download/SFSU/Whisperapp/Logs."
            setTextIsSelectable(true)
        }
        layout.addView(output)
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    private fun showMessage(message: String) = runOnUiThread { output.text = message }
    private fun importVerificationReference(uri: Uri) {
        if (!busy.compareAndSet(false, true)) return
        Thread({
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { input ->
                    val limited = input.readNBytes(32_769)
                    require(limited.size in 1..32_768) { "Reference JSON exceeds 32 KiB" }
                    limited
                } ?: error("Reference cannot be opened")
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                require(json.getInt("schema") == 1 &&
                    json.getString("kind") == "qwen3-asr-0.6b-fixture-tokens")
                val ids = json.getJSONArray("token_ids")
                require(ids.length() in 1..128)
                for (i in 0 until ids.length()) {
                    val value = ids.get(i)
                    require(value is Int && value in 0 until 151936) { "Token ID must be an integer" }
                }
                require(json.getString("origin").isNotBlank() &&
                    json.getString("origin").length <= 160)
                for (key in listOf("gguf_sha256", "feature_sha256")) {
                    require(json.getString(key).matches(Regex("[0-9a-f]{64}")))
                }
                val file = File(filesDir, "qwen-gguf-lab-reference.json")
                val staging = File(filesDir, "qwen-gguf-lab-reference.json.part")
                try {
                    staging.writeBytes(bytes)
                    // Rename over only this Lab's own prior reference, not any user file.
                    java.nio.file.Files.move(staging.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                } finally { staging.delete() }
                showMessage("Reference imported into Lab storage. Origin is user-supplied and not independently authenticated. " +
                    "Run VERIFY DECODER to compare exact input/model hashes and generated tokens.")
            } catch (t: Throwable) {
                showMessage("Reference import failed: ${t.javaClass.simpleName}. Previous reference preserved.")
            } finally { busy.set(false) }
        }, "qwen-verify-reference-import").start()
    }

    private fun verifyDecoderOnce() {
        if ((modelUri == null && !privateModel().isFile) || !QwenLabEncoderAssets.present(this)) {
            showMessage("Download/import the encoder + Q8 GGUF first; no lecture is needed for verification.")
            return
        }
        if (!busy.compareAndSet(false, true)) return
        run.isEnabled = false
        overlapControl.isEnabled = false
        downloadCancelled.set(false)
        Thread({
            val started = System.currentTimeMillis()
            var result: JSONObject? = null
            var failure: String? = null
            var phase = "MODEL_IMPORT"
            try {
                modelUri?.let { importModel(it) }
                phase = "PREPARE_QNN"
                Qwen3HybridClient.connect(this).use { client ->
                    activeClient = client
                    val prep = client.prepareGgufLab(QwenDecoderRunConfig(4, false, false, false), 0)
                    check(prep.optBoolean("ok")) { "QNN fixture prepare failed" }
                    phase = "VERIFY_FIXTURE"
                    val response = client.verifyDecoderFixture()
                    if (!response.optBoolean("ok")) {
                        failure = response.optString("error").take(64)
                        error("Worker fixture verification stopped")
                    }
                    result = response
                    phase = "COMPLETE"
                }
                val status = result!!.optString("verification_status")
                showMessage(buildString {
                    append("DECODER FIXTURE VERIFICATION: $status\n\n")
                    append("Identical QNN output: ${result!!.optBoolean("qnn_feature_repeat_identical")}\n")
                    append("Decoder replay: ${result!!.optBoolean("internal_repeat_identical")}\n")
                    append("External reference: ${result!!.optString("reference_status")}\n")
                    append("First divergent token index: ${result!!.optInt("first_divergent_token_index", -1)}\n")
                    append("Generated tokens: ${result!!.optInt("generated_tokens")}\n")
                    append("One decoder model load: ${result!!.optInt("native_loads") == 1}\n")
                    append("Model SHA-256: ${result!!.optString("model_sha256")}\n")
                    append("Feature SHA-256: ${result!!.optString("feature_sha256")}\n")
                    append("Native SHA-256: ${result!!.optString("native_sha256")}\n")
                    append("Synthetic features (not lecture audio): " +
                        "${result!!.optString("synthetic_export_status")}\n")
                    if (result!!.optString("synthetic_export_status") == "SAVED") {
                        append("File: Download/SFSU/Whisperapp/Verification/" +
                            result!!.optString("synthetic_export_filename") + "\n")
                    }
                    append("\n")
                    append("M-RoPE numerical parity: NOT VERIFIED. Identical local outputs do not prove " +
                        "official model equivalence. A supplied token reference establishes token agreement " +
                        "only, not logits or independent reference provenance.")
                })
            } catch (t: Throwable) {
                if (failure == null) failure = t.javaClass.simpleName
                showMessage("Decoder fixture verification INCONCLUSIVE at $phase: $failure. " +
                    "Check the log, verify models and retry only after resolving the failure.")
            } finally {
                activeClient = null
                val log = runCatching { QwenLabVerificationLog.save(this, started, result, failure, phase) }
                runOnUiThread {
                    output.append(log.fold(
                        onSuccess = { "\n\nVerification log: Download/SFSU/Whisperapp/Logs/$it" },
                        onFailure = { "\n\nVerification log save failed (${it.javaClass.simpleName})" }
                    ))
                    run.isEnabled = true
                    overlapControl.isEnabled = true
                }
                busy.set(false)
            }
        }, "qwen-decoder-fixture-verification").start()
    }

    private fun startOnce() {
        val selectedAudio = audioUri
        if (selectedAudio == null || (modelUri == null && !privateModel().isFile) ||
            !QwenLabEncoderAssets.present(this)) {
            showMessage("Download/import the QNN encoder and Q8 GGUF, then choose lecture audio first.")
            return
        }
        if (!busy.compareAndSet(false, true)) return
        run.isEnabled = false
        overlapControl.isEnabled = false
        val overlapSeconds = if (overlapControl.isChecked) 1 else 0
        downloadCancelled.set(false)
        Thread({
            val attemptStartedEpochMs = System.currentTimeMillis()
            val attemptStartedElapsedMs = android.os.SystemClock.elapsedRealtime()
            var phase = QwenLabDiagnosticLog.Phase.IMPORT_MODEL
            var status = QwenLabDiagnosticLog.Status.FAILED
            var errorClass: String? = null
            var completedSegments = 0
            var completedResult: JSONObject? = null
            try {
                // Lab app has its own application ID and private storage. This copy cannot
                // replace a GGUF in the installed production app or a user-owned SAF file.
                modelUri?.let { importModel(it) }
                val started = android.os.SystemClock.elapsedRealtime()
                phase = QwenLabDiagnosticLog.Phase.CONNECT
                Qwen3HybridClient.connect(this).use { client ->
                    activeClient = client
                    phase = QwenLabDiagnosticLog.Phase.PREPARE
                    val config = QwenDecoderRunConfig(4, false, false, false)
                    val prepared = client.prepareGgufLab(config, overlapSeconds)
                    check(prepared.optBoolean("ok")) { "QNN/GGUF prepare failed: ${prepared.optString("error").take(160)}" }
                    val audio = requireNotNull(selectedAudio)
                    phase = QwenLabDiagnosticLog.Phase.DECODE_AUDIO
                    PcmDecoder.prepare(this, audio).use { source ->
                        // Some SAF/media providers omit duration; enforce the actual segment bound below.
                        require(source.info.durationMs <= 0L || source.info.durationMs <= 240_000L) {
                            "Lab accepts at most 240 seconds of audio"
                        }
                        source.open16kSegmentStream(12, overlapSeconds).use { stream ->
                            var segments = 0
                            while (stream.hasNext()) {
                                check(!downloadCancelled.get()) { "Lab cancelled" }
                                check(segments < 24) { "More than 24 segments is not supported" }
                                val timedChunk = stream.next()
                                // Metadata may omit duration. Check actual decoded audio coverage, not
                                // cumulative overlapped samples (which intentionally repeat audio).
                                require(timedChunk.endMs - source.info.audioStartMs <= 240_050L) {
                                    "Lab accepts at most 240 seconds of decoded audio"
                                }
                                val chunk = timedChunk.samples
                                phase = QwenLabDiagnosticLog.Phase.ENCODE_QNN
                                SharedMemory.create("qwen-gguf-lab-pcm", chunk.size * 4).use { shared ->
                                    val writable = shared.mapReadWrite().order(ByteOrder.nativeOrder())
                                    try { writable.asFloatBuffer().put(chunk) }
                                    finally { SharedMemory.unmap(writable) }
                                    require(shared.setProtect(OsConstants.PROT_READ))
                                    val encoded = client.encodeGgufLab(shared, chunk.size)
                                    check(encoded.optBoolean("ok")) {
                                        "QNN segment ${segments + 1} failed: ${encoded.optString("error").take(160)}"
                                    }
                                }
                                segments++
                                completedSegments = segments
                                showMessage("QNN encoded $segments audio segments. Decoder model loads only once at the end.")
                            }
                            require(segments in 2..24)
                        }
                    }
                    check(!downloadCancelled.get()) { "Lab cancelled" }
                    phase = QwenLabDiagnosticLog.Phase.DECODE_GGUF
                    val result: JSONObject = client.finishGgufLab()
                    check(result.optBoolean("ok")) { "Native decoder failed: ${result.optString("error").take(160)}" }
                    completedResult = result
                    status = QwenLabDiagnosticLog.Status.SUCCESS
                    phase = QwenLabDiagnosticLog.Phase.COMPLETE
                    showMessage(buildString {
                        append("ON-DEVICE EXPERIMENTAL QNN + DOTPROD Q8 GGUF\n\n")
                        append("Segments: ${result.optInt("segments")}; model loads: ${result.optInt("model_loads")}\n")
                        append("Overlap: ${result.optInt("overlap_seconds")} s; stitched boundaries: " +
                            "${result.optInt("stitched_boundaries")}; duplicate words removed: " +
                            "${result.optInt("stitched_duplicate_words")}; loop copies removed: " +
                            "${result.optInt("stitched_repetition_copies_removed")}\n")
                        append("Preprocess: ${result.optLong("preprocess_ms")} ms; QNN encode: ${result.optLong("encoder_ms")} ms\n")
                        append("Native model load: ${result.optLong("model_load_ms")} ms; decoder: ${result.optLong("decoder_ms")} ms\n")
                        append("Tokens: ${result.optInt("generated_tokens")}; EOS segments: ${result.optInt("eos_segments")}\n")
                        append("Peak native RSS: ${result.optLong("native_rss_peak_kb")} KiB; " +
                            "native CPU: ${result.optLong("native_cpu_ms")} ms\n")
                        append("Elapsed (incl. media decode and local processing): ${android.os.SystemClock.elapsedRealtime() - started} ms\n")
                        append("M-RoPE equivalence: UNQUALIFIED\n\n")
                        append(result.getString("transcript"))
                    })
                }
            } catch (t: Throwable) {
                status = if (downloadCancelled.get()) QwenLabDiagnosticLog.Status.CANCELLED
                         else QwenLabDiagnosticLog.Status.FAILED
                errorClass = t.javaClass.simpleName
                // Never print model paths, raw audio, private token IDs, or transcripts in errors.
                showMessage("Lab test stopped: ${t.javaClass.simpleName}. " +
                    "Check that the QNN encoder files were imported into this Lab app, the Q8 file is valid, " +
                    "and Android permits executing the bundled ARM64 decoder. No production app data was changed.")
            } finally {
                // Log on success, failure and cancellation; never log transcript or inputs.
                val logResult = runCatching {
                    QwenLabDiagnosticLog.save(this, QwenLabDiagnosticLog.RunReport(
                        attemptStartedEpochMs,
                        android.os.SystemClock.elapsedRealtime() - attemptStartedElapsedMs,
                        status, phase, completedSegments, errorClass, completedResult
                    ))
                }
                runOnUiThread {
                    val note = logResult.fold(
                        onSuccess = { "\n\nDiagnostic log saved: Download/SFSU/Whisperapp/Logs/$it" },
                        onFailure = { "\n\nDiagnostic log could not be saved (${it.javaClass.simpleName})." }
                    )
                    output.append(note)
                }
                activeClient = null
                busy.set(false)
                runOnUiThread { run.isEnabled = true; overlapControl.isEnabled = true }
            }
        }, "qwen-gguf-lab-session").start()
    }
    private fun downloadModels() {
        if (!busy.compareAndSet(false, true)) return
        downloadCancelled.set(false)
        run.isEnabled = false
        Thread({
            try {
                QwenLabModelDownloader.downloadMissing(this,
                    { downloadCancelled.get() || Thread.currentThread().isInterrupted },
                    { message -> showMessage(message) })
                val backup = runCatching {
                    QwenLabPublicModels.backupVerified(this,
                        { downloadCancelled.get() || Thread.currentThread().isInterrupted },
                        { message -> showMessage(message) })
                }
                showMessage(if (backup.isSuccess) {
                    "Models verified and saved to Download/SFSU/Whisperapp/Models. " +
                        "Choose audio and tap TRANSCRIBE."
                } else {
                    "Models verified in private Lab storage, but the Downloads backup failed " +
                        "(${backup.exceptionOrNull()?.javaClass?.simpleName}). " +
                        "Use Back up verified models before uninstalling."
                })
            } catch (_: InterruptedException) {
                showMessage("Model download cancelled. Previously installed models remain available.")
            } catch (_: Throwable) {
                showMessage("Model setup failed. Check Wi-Fi, free storage and Hugging Face " +
                    "availability, then retry. No incomplete model was activated. " +
                    "You can still import existing model files manually.")
            } finally {
                busy.set(false)
                runOnUiThread { run.isEnabled = true; overlapControl.isEnabled = true }
            }
        }, "qwen-gguf-model-download").start()
    }

    private fun backupModels() {
        if (!busy.compareAndSet(false, true)) return
        downloadCancelled.set(false)
        run.isEnabled = false
        Thread({
            try {
                QwenLabPublicModels.backupVerified(this,
                    { downloadCancelled.get() || Thread.currentThread().isInterrupted },
                    { message -> showMessage(message) })
                showMessage("Backup complete: Internal storage/Download/SFSU/Whisperapp/Models. " +
                    "Keep these files when uninstalling the app.")
            } catch (_: InterruptedException) {
                showMessage("Backup cancelled; verified private models remain unchanged.")
            } catch (t: Throwable) {
                showMessage("Model backup failed (${t.javaClass.simpleName}); private models are unchanged. " +
                    "Check free storage (about 1.2 GB), then retry.")
            } finally {
                busy.set(false)
                runOnUiThread { run.isEnabled = true; overlapControl.isEnabled = true }
            }
        }, "qwen-gguf-model-backup").start()
    }

    private fun restoreSavedModels(uri: Uri) {
        if (!busy.compareAndSet(false, true)) return
        downloadCancelled.set(false)
        run.isEnabled = false
        Thread({
            try {
                QwenLabPublicModels.restore(this, uri,
                    { downloadCancelled.get() || Thread.currentThread().isInterrupted },
                    { message -> showMessage(message) })
                showMessage("Models restored from Downloads and verified. " +
                    "Select lecture audio and tap TRANSCRIBE; no download needed.")
            } catch (_: InterruptedException) {
                showMessage("Model restoration cancelled. Existing private model files remain.")
            } catch (t: Throwable) {
                showMessage("Restore failed (${t.javaClass.simpleName}). " +
                    "Select Internal storage/Download/SFSU/Whisperapp/Models " +
                    "containing all six verified model files.")
            } finally {
                busy.set(false)
                runOnUiThread { run.isEnabled = true; overlapControl.isEnabled = true }
            }
        }, "qwen-gguf-model-restore").start()
    }

    private fun importEncoder(kind: String, import: () -> Unit) {
        if (!busy.compareAndSet(false, true)) return
        run.isEnabled = false
        Thread({
            try {
                showMessage("Importing and verifying private QNN encoder assets from $kind...")
                import()
                showMessage("QNN encoder assets imported. Optional source manifest hashes validated when present.")
            } catch (_: Throwable) {
                showMessage("Encoder import failed. Select a compatible 0.6B S26 encoder package " +
                    "with the two golden fixtures; any previously imported model is retained.")
            } finally {
                busy.set(false)
                runOnUiThread { run.isEnabled = true; overlapControl.isEnabled = true }
            }
        }, "qwen-gguf-encoder-import").start()
    }

    private fun privateModel() = File(filesDir, "qwen-gguf-lab-models/decoder-q8.gguf")
    private fun importModel(uri: Uri) {
        val target = privateModel()
        require(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val pending = File(target.parentFile, "decoder-q8.gguf.part")
        try {
            showMessage("Copying selected Q8 GGUF into isolated Lab storage (once)...")
            val copied = contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(pending).use { output -> input.copyTo(output, 1024 * 1024) }
            } ?: error("Could not open selected model")
            require(copied > 128L * 1024 * 1024 && copied < 3L * 1024 * 1024 * 1024) {
                "Q8 GGUF size outside Lab limits"
            }
            require(pending.inputStream().use { String(it.readNBytes(4), Charsets.US_ASCII) } == "GGUF") {
                "Selected file is not a GGUF decoder"
            }
            require(pending.renameTo(target)) { "Could not commit private decoder model" }
            modelUri = null // subsequent tests reuse the private Lab copy; avoid repeated imports
        } finally { if (pending.exists()) pending.delete() }
    }
    override fun onDestroy() {
        downloadCancelled.set(true)
        runCatching { activeClient?.cancelCurrent() }
        super.onDestroy()
    }
}
