package com.example.whisperrecorder.qwen

import android.net.Uri
import android.os.Bundle
import android.os.SharedMemory
import android.system.OsConstants
import android.view.ViewGroup
import android.widget.Button
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

    private var activeClient: Qwen3HybridClient? = null
    private val busy = AtomicBoolean(false)
    private lateinit var output: TextView
    private lateinit var run: Button
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
                "Encoder package and GGUF stay in this app's private storage. " +
                "Native Android executable policy and transcript accuracy need phone validation."
        })
        button("Import extracted QNN encoder folder") { chooseEncoderDirectory.launch(null) }
        button("Or import QNN encoder ZIP") { chooseEncoderZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
        button("Select Q8 GGUF model") { chooseModel.launch(arrayOf("application/octet-stream", "*/*")) }
        button("Select lecture audio (up to 4 min)") { chooseAudio.launch(arrayOf("audio/*", "video/*")) }
        run = Button(this).apply {
            text = "TRANSCRIBE once with on-device QNN + GGUF"
            setOnClickListener { startOnce() }
        }
        layout.addView(run)
        button("Cancel current Lab session") {
            runCatching { activeClient?.cancelCurrent() }
            showMessage("Cancellation requested; worker will clean up its own temporary features.")
        }
        output = TextView(this).apply {
            textSize = 15f
            text = "Import your QNN encoder folder (or ZIP) containing encoder-qnn-fp16.bin, " +
                "tokenizer.json and both golden synthetic fixtures. Then select Q8 GGUF " +
                "and audio. No private model, recording or transcript is exported."
            setTextIsSelectable(true)
        }
        layout.addView(output)
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    private fun showMessage(message: String) = runOnUiThread { output.text = message }
    private fun startOnce() {
        val selectedAudio = audioUri
        if (selectedAudio == null || (modelUri == null && !privateModel().isFile) ||
            !QwenLabEncoderAssets.present(this)) {
            showMessage("Import the QNN encoder files, select a Q8 GGUF, and choose lecture audio first.")
            return
        }
        if (!busy.compareAndSet(false, true)) return
        run.isEnabled = false
        Thread({
            try {
                // Lab app has its own application ID and private storage. This copy cannot
                // replace a GGUF in the installed production app or a user-owned SAF file.
                modelUri?.let { importModel(it) }
                val started = android.os.SystemClock.elapsedRealtime()
                Qwen3HybridClient.connect(this).use { client ->
                    activeClient = client
                    val config = QwenDecoderRunConfig(4, false, false, false)
                    val prepared = client.prepareGgufLab(config)
                    check(prepared.optBoolean("ok")) { "QNN/GGUF prepare failed: ${prepared.optString("error").take(160)}" }
                    val audio = requireNotNull(selectedAudio)
                    PcmDecoder.prepare(this, audio).use { source ->
                        // Some SAF/media providers omit duration; enforce the actual segment bound below.
                        require(source.info.durationMs <= 0L || source.info.durationMs <= 240_000L) {
                            "Lab accepts at most 240 seconds of audio"
                        }
                        source.open16kSegmentStream(12).use { stream ->
                            var segments = 0
                            while (stream.hasNext()) {
                                check(segments < 20) { "More than twenty segments is not supported" }
                                val chunk = stream.next().samples
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
                                showMessage("QNN encoded $segments audio segments. Decoder model loads only once at the end.")
                            }
                            require(segments in 2..20)
                        }
                    }
                    val result: JSONObject = client.finishGgufLab()
                    check(result.optBoolean("ok")) { "Native decoder failed: ${result.optString("error").take(160)}" }
                    showMessage(buildString {
                        append("ON-DEVICE EXPERIMENTAL QNN + DOTPROD Q8 GGUF\n\n")
                        append("Segments: ${result.optInt("segments")}; model loads: ${result.optInt("model_loads")}\n")
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
                // Never print model paths, raw audio, private token IDs, or transcripts in errors.
                showMessage("Lab test stopped: ${t.javaClass.simpleName}. " +
                    "Check that the QNN encoder files were imported into this Lab app, the Q8 file is valid, " +
                    "and Android permits executing the bundled ARM64 decoder. No production app data was changed.")
            } finally {
                activeClient = null
                busy.set(false)
                runOnUiThread { run.isEnabled = true }
            }
        }, "qwen-gguf-lab-session").start()
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
                runOnUiThread { run.isEnabled = true }
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
        runCatching { activeClient?.cancelCurrent() }
        super.onDestroy()
    }
}
