package com.example.whisperrecorder.npu

import android.content.Context

/** Explicit, comparable per-recording CPU decoder A/B profiles. Model weights remain unchanged. */
internal object QwenDecoderPerformancePolicy {
    private const val PREFS = "qwen-decoder-performance"
    private const val KEY = "fast_four_threads"
    private const val KEY_FP16 = "opt_in_verified_k2a_fp16_embeddings"
    private const val KEY_NO_ARENA = "experimental_disable_cpu_arena"
    fun fast(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY, false)
    fun threads(context: Context): Int = if (fast(context)) 4 else 2
    // Retired after the S26 trial showed no performance gain. Keep the persisted
    // key only so upgrades do not revive a previously enabled experiment.
    fun reusableStepInputs(context: Context): Boolean = false
    fun cpuArenaDisabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_NO_ARENA, false)
    fun setCpuArenaDisabled(context: Context, disabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_NO_ARENA, disabled).apply()
    }
    fun snapshot(context: Context): QwenDecoderRunConfig = QwenDecoderRunConfig(
        threads(context), useFp16EmbeddingTrial(context), cpuArenaDisabled(context))
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
