package com.example.whisperrecorder.diagnostics

/** Parse only known numeric Qwen worker counters. Never import raw text, paths, token IDs or audio. */
internal object QwenPerformanceMetrics {
    private val numericFields = mapOf(
        "preprocessMs" to "preprocess_ms", "encoderMs" to "encoder_ms",
        "decoderLoadMs" to "decoder_load_ms", "decoderMs" to "decoder_ms",
        "prefillMs" to "prefill_ms", "stepMs" to "step_ms",
        "decoderSteps" to "decoder_steps", "tokens" to "generated_token_count",
        "embeddingIoMs" to "embedding_io_ms", "argmaxMs" to "argmax_ms",
        "decoderThreads" to "decoder_threads", "workerCpuMs" to "worker_cpu_ms",
        "workerWallMs" to "worker_wall_ms", "workerPssKb" to "worker_pss_kb",
        "thermalStart" to "thermal_start", "thermalEnd" to "thermal_end",
        "encoderGraphCalls" to "encoder_graph_calls",
        "decoderNpuGraphCalls" to "decoder_npu_graph_calls",
        "kvPayloadBytesMax" to "decoder_kv_payload_bytes_max",
        "stepP50Ms" to "decoder_step_p50_ms",
        "stepP95Ms" to "decoder_step_p95_ms",
        "stepMaxMs" to "decoder_step_max_ms"
    )

    fun parse(source: String): Map<String, Long> {
        val found = linkedMapOf<String, Long>()
        for (part in source.split(Regex("\\s+"))) {
            val separator = part.indexOf('=')
            if (separator <= 0) continue
            val field = numericFields[part.substring(0, separator)] ?: continue
            val value = part.substring(separator + 1).toLongOrNull() ?: continue
            if (value >= 0L) found[field] = value
        }
        return found
    }
}
