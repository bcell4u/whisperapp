package com.example.whisperrecorder.npu

import android.content.Context

/** Explicit, comparable per-recording CPU decoder A/B profiles. Model weights remain unchanged. */
internal object QwenDecoderPerformancePolicy {
    private const val PREFS = "qwen-decoder-performance"
    private const val KEY = "fast_four_threads"
    fun fast(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY, false)
    fun threads(context: Context): Int = if (fast(context)) 4 else 2
    fun setFast(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY, enabled).apply()
    }
}
