package com.example.whisperrecorder.diagnostics

import org.junit.Assert.*
import org.junit.Test

class QwenPerformanceMetricsTest {
    @Test fun acceptedMetricsAreNumericAndWhitelisted() {
        val m = QwenPerformanceMetrics.parse("prefillMs=812 stepP95Ms=72 decoderSteps=17 workerPssKb=5900000 decoderNpuGraphCalls=0 qwenStorage=saf-proc-fd token=151643 /sdcard/x=99")
        assertEquals(812L, m["prefill_ms"])
        assertEquals(72L, m["decoder_step_p95_ms"])
        assertEquals(5900000L, m["worker_pss_kb"])
        assertEquals(0L, m["decoder_npu_graph_calls"])
        assertFalse(m.containsKey("token"))
        assertFalse(m.containsKey("qwenStorage"))
        assertEquals(5, m.size)
    }

    @Test fun rejectsNegativeNonNumericAndUnrecognizedFields() {
        val m = QwenPerformanceMetrics.parse("decoderSteps=-4 prefillMs=not_a_number thermalEnd=5 userFile=/home/name prompt=secret")
        assertEquals(mapOf("thermal_end" to 5L), m)
    }
    @Test fun acceptsNewFirstStepAndMemoryCountersWithoutImportingPrivateValues() {
        val metrics = QwenPerformanceMetrics.parse(
            "firstStepMs=1551 slowStepsOver250Ms=2 pssBeforeDecoderKb=12345 " +
                "pssAfterInitKb=900000 pssAfterStepKb=1900000 prefillPssKb=2100000 " +
                "reusableStepInputsActive=1 transcript=PRIVATE path=/data/user/0/file"
        )
        assertEquals(1551L, metrics["decoder_first_step_ms"])
        assertEquals(2L, metrics["decoder_slow_steps_over_250ms"])
        assertEquals(1900000L, metrics["decoder_pss_after_step_kb"])
        assertEquals(1L, metrics["decoder_reusable_step_inputs"])
        assertEquals(7, metrics.size)
    }

}
