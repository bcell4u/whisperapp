package com.example.whisperrecorder.npu

import org.json.JSONObject

/** Immutable configuration sent across process boundaries BEFORE Qwen runtime initialization.
 * Do not infer these settings from the worker process's SharedPreferences.
 */
internal data class QwenDecoderRunConfig(
    val threads: Int,
    val fp16EmbeddingTrial: Boolean,
    val cpuArenaDisabled: Boolean
) {
    init { require(threads == 2 || threads == 4) { "Qwen decoder thread count must be 2 or 4" } }
    fun toJson(): String = JSONObject()
        .put("schema", SCHEMA)
        .put("threads", threads)
        .put("fp16_embedding_trial", fp16EmbeddingTrial)
        .put("cpu_arena_disabled", cpuArenaDisabled)
        .toString()

    companion object {
        const val SCHEMA = 1
        fun fromJson(raw: String): QwenDecoderRunConfig {
            require(raw.length in 2..512) { "Qwen config payload length invalid" }
            val doc = JSONObject(raw)
            require(doc.getInt("schema") == SCHEMA) { "Unsupported Qwen config schema" }
            for (key in listOf("fp16_embedding_trial", "cpu_arena_disabled")) {
                require(doc.get(key) is Boolean) { "Qwen config $key must be Boolean" }
            }
            return QwenDecoderRunConfig(doc.getInt("threads"),
                doc.getBoolean("fp16_embedding_trial"), doc.getBoolean("cpu_arena_disabled"))
        }
    }
}
