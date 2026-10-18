package com.example.whisperrecorder.npu

import android.content.Context

/** Explicit, comparable per-recording CPU decoder A/B profiles. Model weights remain unchanged. */
internal object QwenDecoderPerformancePolicy {
    private const val PREFS = "qwen-decoder-performance"
    private const val KEY = "fast_four_threads"
    private const val KEY_REUSE = "experimental_reusable_step_inputs"
    private const val KEY_FP16 = "opt_in_verified_k2a_fp16_embeddings"
    fun fast(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY, false)
    fun threads(context: Context): Int = if (fast(context)) 4 else 2
    fun reusableStepInputs(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REUSE, false)
    fun setReusableStepInputs(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REUSE, enabled).apply()
    }
    fun useFp16EmbeddingTrial(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_FP16, false)
    fun setFp16EmbeddingTrial(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_FP16, enabled).apply()
    }
    fun setFast(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY, enabled).apply()
    }
}
