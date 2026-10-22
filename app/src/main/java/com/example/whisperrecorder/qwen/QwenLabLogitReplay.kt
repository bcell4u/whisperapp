package com.example.whisperrecorder.qwen

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs

/** Synthetic fixture only. This checks numerical repeatability, NOT official-logit equivalence. */
internal object QwenLabLogitReplay {
    const val VOCAB = 151936
    const val BYTES = VOCAB * 4

    data class Comparison(
        val identical: Boolean,
        val sha256: String,
        val firstDifferentFloat: Int,
        val maxAbsoluteDelta: Double
    )

    fun compare(first: ByteArray, second: ByteArray): Comparison {
        require(first.size == BYTES && second.size == BYTES) { "Logit checkpoint size mismatch" }
        val a = ByteBuffer.wrap(first).order(ByteOrder.LITTLE_ENDIAN)
        val b = ByteBuffer.wrap(second).order(ByteOrder.LITTLE_ENDIAN)
        var firstDifference = -1
        var maxDelta = 0.0
        for (i in 0 until VOCAB) {
            val left = a.float.toDouble()
            val right = b.float.toDouble()
            require(left.isFinite() && right.isFinite()) { "Nonfinite checkpoint logits" }
            if (firstDifference < 0 && java.lang.Float.floatToRawIntBits(left.toFloat()) !=
                java.lang.Float.floatToRawIntBits(right.toFloat())) firstDifference = i
            maxDelta = maxOf(maxDelta, abs(left - right))
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(first)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return Comparison(first.contentEquals(second), digest, firstDifference, maxDelta)
    }

    fun compareFiles(first: File, second: File): Comparison {
        require(first.isFile && second.isFile && first.length() == BYTES.toLong() &&
            second.length() == BYTES.toLong()) { "Missing or truncated logit checkpoint" }
        return compare(first.readBytes(), second.readBytes())
    }
}
