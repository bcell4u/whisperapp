package com.example.whisperrecorder.qwen

import java.security.MessageDigest

/**
 * Comparison policy is Android-independent for deterministic unit testing.
 * Matching two invocations of the SAME implementation proves repeatability,
 * not parity with official Qwen or correct four-axis M-RoPE.
 * External token agreement is a narrow token-level comparison, not logits parity.
 */
internal object QwenLabDecoderVerifier {
    const val SCHEMA = 1
    const val EOS_A = 151643
    const val EOS_B = 151645
    private val HEX64 = Regex("[0-9a-f]{64}")

    data class ExternalReference(
        val modelSha256: String,
        val featureSha256: String,
        val tokenIds: List<Int>,
        val origin: String
    )

    data class Outcome(
        val status: String,
        val internallyIdentical: Boolean,
        val eosBoth: Boolean,
        val firstDifferentToken: Int,
        val tokenCount: Int,
        val referenceStatus: String
    )

    /** The first unequal position (zero based), including unequal sequence lengths. */
    fun firstMismatch(a: List<Int>, b: List<Int>): Int {
        val common = minOf(a.size, b.size)
        for (i in 0 until common) if (a[i] != b[i]) return i
        return if (a.size == b.size) -1 else common
    }

    /** Tiny in-process positive AND negative controls for decision-policy regressions.
     * This tests the comparator only and NEVER asserts that the actual model is correct.
     */
    fun selfTestGate(): Boolean {
        val ids = listOf(42, EOS_B)
        val model = "a".repeat(64)
        val feature = "b".repeat(64)
        val good = ExternalReference(model, feature, ids, "synthetic-unit-test")
        return compare(ids, ids, model, feature, good).status == "PASS_REFERENCE_TOKENS" &&
            compare(ids, ids, model, feature, null).status == "PASS_INTERNAL_ONLY" &&
            compare(ids, ids, model, feature,
                good.copy(tokenIds = listOf(43, EOS_B))).status == "FAIL_REFERENCE_TOKENS" &&
            compare(ids, ids, model, feature,
                good.copy(modelSha256 = "c".repeat(64))).status == "INCONCLUSIVE_REFERENCE_INPUT_MISMATCH" &&
            compare(ids, listOf(99, EOS_B), model, feature, good).status == "FAIL_INTERNAL_REPLAY"
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun compare(
        first: List<Int>, second: List<Int>, modelSha: String, featureSha: String,
        reference: ExternalReference?
    ): Outcome {
        require(modelSha.matches(HEX64) && featureSha.matches(HEX64))
        require(first.size in 1..128 && second.size in 1..128)
        require(first.all { it in 0 until 151936 } && second.all { it in 0 until 151936 })
        val repeatIndex = firstMismatch(first, second)
        val eosBoth = first.last() in listOf(EOS_A, EOS_B) &&
            second.last() in listOf(EOS_A, EOS_B)
        if (repeatIndex != -1) return Outcome(
            "FAIL_INTERNAL_REPLAY", false, eosBoth, repeatIndex, first.size, "NOT_EVALUATED")
        if (!eosBoth) return Outcome(
            "INCONCLUSIVE_NO_EOS", true, false, -1, first.size, "NOT_EVALUATED")
        if (reference == null) return Outcome(
            "PASS_INTERNAL_ONLY", true, true, -1, first.size, "MISSING")
        if (!reference.modelSha256.matches(HEX64) || !reference.featureSha256.matches(HEX64) ||
            reference.origin.isBlank() || reference.origin.length > 160 ||
            reference.tokenIds.size !in 1..128 ||
            reference.tokenIds.any { it !in 0 until 151936 }) {
            return Outcome("INCONCLUSIVE_BAD_REFERENCE", true, true, -1, first.size, "INVALID")
        }
        if (reference.modelSha256 != modelSha || reference.featureSha256 != featureSha) {
            return Outcome("INCONCLUSIVE_REFERENCE_INPUT_MISMATCH", true, true, -1, first.size,
                "MODEL_OR_FEATURE_SHA_MISMATCH")
        }
        val mismatch = firstMismatch(first, reference.tokenIds)
        return if (mismatch == -1) Outcome(
            "PASS_REFERENCE_TOKENS", true, true, -1, first.size, "MATCH")
        else Outcome("FAIL_REFERENCE_TOKENS", true, true, mismatch, first.size, "DIVERGED")
    }
}
