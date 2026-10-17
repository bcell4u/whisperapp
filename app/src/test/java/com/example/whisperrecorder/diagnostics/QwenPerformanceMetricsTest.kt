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
}
