package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test

class OmiNpuProbeContractsTest {
    @Test fun onlyTwoSmallDecoderContextsAreSelectedByDefault() {
        assertEquals(listOf("omi-joint", "omi-predictor"), OmiNpuProbeContracts.DECODER.map { it.graph.id })
        assertEquals(3, OmiNpuProbeContracts.ALL.size)
        assertFalse(OmiNpuProbeContracts.DECODER.any { it.graph.id == "omi-encoder" })
    }

    @Test fun contractsHaveUniqueQnnNamesAndValidFp32Shapes() {
        val contracts = OmiNpuProbeContracts.ALL
        assertEquals(3, contracts.map { it.graph.graphName }.toSet().size)
        for (context in contracts) {
            assertEquals(64, context.sha256.length)
            assertTrue(context.sha256.all { it in '0'..'9' || it in 'a'..'f' })
            assertTrue(context.graph.artifactBytes > 0)
            val tensors = context.graph.inputs + context.graph.outputs
            assertTrue(tensors.all { it.name.isNotBlank() && it.shape.all { size -> size > 0 } })
            assertEquals(context.graph.inputs.size, context.graph.inputs.map { it.name }.toSet().size)
            assertEquals(context.graph.outputs.size, context.graph.outputs.map { it.name }.toSet().size)
        }
    }

    @Test fun compiledTdtV2JointHasNoProjectorAnd1030Logits() {
        val joint = OmiNpuProbeContracts.JOINT.graph
        assertEquals("graph_z1sbzhs3", joint.graphName)
        assertArrayEquals(longArrayOf(1, 1, 1024), joint.inputs[0].shape)
        assertArrayEquals(longArrayOf(1, 1, 1030), joint.outputs.single().shape)
        assertTrue(OmiNpuProbeContracts.ALL.none { "projector" in it.graph.id })
    }

    @Test fun encoderUsesTwelveSecondFixedInputAndCorrectFrameCount() {
        val encoder = OmiNpuProbeContracts.ENCODER.graph
        assertArrayEquals(longArrayOf(1, 128, 1201), encoder.inputs.first().shape)
        assertArrayEquals(longArrayOf(1, 1024, 151), encoder.outputs.single().shape)
    }
}
