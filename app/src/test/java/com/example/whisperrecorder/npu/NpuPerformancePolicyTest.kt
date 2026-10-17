package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Test

class NpuPerformancePolicyTest {
    @Test fun normalTemperatureUsesProviderDefault() {
        assertEquals(NpuPerformancePolicy.PROVIDER_DEFAULT, NpuPerformancePolicy.productionProfile(0))
        assertEquals(NpuPerformancePolicy.PROVIDER_DEFAULT, NpuPerformancePolicy.productionProfile(2))
    }

    @Test fun severeThermalStatusUsesBalancedMode() {
        assertEquals("balanced", NpuPerformancePolicy.productionProfile(3).id)
        assertEquals("balanced", NpuPerformancePolicy.productionProfile(6).id)
    }
}
