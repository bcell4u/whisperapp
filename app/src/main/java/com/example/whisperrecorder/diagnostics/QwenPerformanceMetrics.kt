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
        "stepMaxMs" to "decoder_step_max_ms",
        "firstStepMs" to "decoder_first_step_ms",
        "slowStepsOver250Ms" to "decoder_slow_steps_over_250ms",
        "prefillPssKb" to "decoder_prefill_pss_kb",
        "pssBeforeDecoderKb" to "decoder_pss_before_load_kb",
        "pssAfterInitKb" to "decoder_pss_after_init_kb",
        "pssAfterStepKb" to "decoder_pss_after_step_kb",
        "reusableStepInputsActive" to "decoder_reusable_step_inputs",
        "cpuArenaDisabledActive" to "decoder_cpu_arena_disabled",
        "prepackingDisabledActive" to "decoder_prepacking_disabled",
        "kvFirstBytes" to "decoder_kv_payload_bytes_first",
        "kvLastBytes" to "decoder_kv_payload_bytes_last",
        "kvLogicalBytesSum" to "decoder_kv_logical_input_bytes_sum",
        "kvSamples" to "decoder_kv_input_samples",
        "kvGrowthSteps" to "decoder_kv_growth_steps",
        "kvShrinkSteps" to "decoder_kv_shrink_steps",
        "configSchema" to "decoder_config_schema",
        "pssInitDeltaKb" to "decoder_pss_init_delta_kb",
        "pssStepDeltaKb" to "decoder_pss_step_delta_kb",
        "initSessionLoadMs" to "decoder_init_session_load_ms",
        "stepSessionLoadMs" to "decoder_step_session_load_ms",
        "beforeNativePssKb" to "decoder_before_native_pss_kb",
        "initNativePssKb" to "decoder_init_native_pss_kb",
        "stepNativePssKb" to "decoder_step_native_pss_kb",
        "beforeOtherPssKb" to "decoder_before_other_pss_kb",
        "initOtherPssKb" to "decoder_init_other_pss_kb",
        "stepOtherPssKb" to "decoder_step_other_pss_kb",
        "beforeDalvikPssKb" to "decoder_before_dalvik_pss_kb",
        "initDalvikPssKb" to "decoder_init_dalvik_pss_kb",
        "stepDalvikPssKb" to "decoder_step_dalvik_pss_kb",
        "stepPrivateDirtyKb" to "decoder_step_private_dirty_kb"
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
