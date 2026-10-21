package com.example.whisperrecorder.qwen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenLabDecoderVerifierTest {
    private val model = "a".repeat(64)
    private val feature = "b".repeat(64)
    private val ids = listOf(101, 102, QwenLabDecoderVerifier.EOS_B)
    private fun reference(tokens: List<Int> = ids, m: String = model, f: String = feature) =
        QwenLabDecoderVerifier.ExternalReference(m, f, tokens, "independent-run-labelled-by-operator")
    @Test fun repeatedOutputWithoutReferenceIsNotCalledParity() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature, null)
        assertEquals("PASS_INTERNAL_ONLY", o.status)
        assertEquals("MISSING", o.referenceStatus)
    }
    @Test fun matchingExternalTokensReceiveQualifiedTokenOnlyPass() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature, reference())
        assertEquals("PASS_REFERENCE_TOKENS", o.status)
        assertEquals(-1, o.firstDifferentToken)
    }
    @Test fun externalDivergenceCapturesFirstIndexWithoutLeakingTokens() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature,
            reference(listOf(101, 999, QwenLabDecoderVerifier.EOS_B)))
        assertEquals("FAIL_REFERENCE_TOKENS", o.status)
        assertEquals(1, o.firstDifferentToken)
    }
    @Test fun unequalLengthsReportCorrectIndex() {
        assertEquals(2, QwenLabDecoderVerifier.firstMismatch(ids, ids.take(2)))
    }
    @Test fun replayMismatchCannotProduceExternalPass() {
        val o = QwenLabDecoderVerifier.compare(ids, listOf(101, 103, QwenLabDecoderVerifier.EOS_B),
            model, feature, reference())
        assertEquals("FAIL_INTERNAL_REPLAY", o.status)
        assertFalse(o.internallyIdentical)
        assertEquals("NOT_EVALUATED", o.referenceStatus)
    }
    @Test fun noEosIsInconclusiveRatherThanFalsePass() {
        val o = QwenLabDecoderVerifier.compare(listOf(101, 102), listOf(101, 102), model, feature, reference())
        assertEquals("INCONCLUSIVE_NO_EOS", o.status)
        assertFalse(o.eosBoth)
    }
    @Test fun wrongModelCannotBeDeclaredParity() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature, reference(m="c".repeat(64)))
        assertEquals("INCONCLUSIVE_REFERENCE_INPUT_MISMATCH", o.status)
    }
    @Test fun wrongFeatureCannotBeDeclaredParity() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature, reference(f="c".repeat(64)))
        assertEquals("INCONCLUSIVE_REFERENCE_INPUT_MISMATCH", o.status)
    }
    @Test fun invalidReferenceCannotBeDeclaredParity() {
        val o = QwenLabDecoderVerifier.compare(ids, ids, model, feature, reference(tokens=listOf(151936)))
        assertEquals("INCONCLUSIVE_BAD_REFERENCE", o.status)
    }
    @Test fun bundledComparatorPositiveAndNegativeControlsPass() {
        assertTrue(QwenLabDecoderVerifier.selfTestGate())
    }
    @Test fun hashHelperUsesCorrectSha256Encoding() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            QwenLabDecoderVerifier.sha256("abc".toByteArray()))
    }
}
