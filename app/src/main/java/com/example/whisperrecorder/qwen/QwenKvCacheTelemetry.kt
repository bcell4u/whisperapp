package com.example.whisperrecorder.qwen

/** Tracks *logical* tensor payload presented to the decoder, not CPU copies,
 * heap residency, NPU transfer or allocation. Numeric-only privacy-safe counters.
 * Samples are per-token and reset for each audio segment.
 */
internal class QwenKvCacheTelemetry {
    var firstBytes: Long = 0L; private set
    var lastBytes: Long = 0L; private set
    var logicalBytesSum: Long = 0L; private set
    var samples: Int = 0; private set
    var growthSteps: Int = 0; private set
    var shrinkSteps: Int = 0; private set

    fun observe(logicalPayloadBytes: Long) {
        require(logicalPayloadBytes >= 0L) { "KV payload cannot be negative" }
        if (samples == 0) firstBytes = logicalPayloadBytes
        else if (logicalPayloadBytes > lastBytes) growthSteps++
        else if (logicalPayloadBytes < lastBytes) shrinkSteps++
        lastBytes = logicalPayloadBytes
        logicalBytesSum = if (logicalBytesSum > Long.MAX_VALUE - logicalPayloadBytes)
            Long.MAX_VALUE else logicalBytesSum + logicalPayloadBytes
        samples++
    }
}
