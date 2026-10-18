package com.example.whisperrecorder.npu

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class Qwen3EpContextWrapperTest {
    private val contract = NpuGraphContract(
        id = "qwen3_encoder", graphName = "qwen3_asr_encoder",
        artifactPath = "encoder-qnn-fp16.bin", artifactBytes = 4L,
        inputs = listOf(NpuTensorContract("mel", longArrayOf(1,128,1200))),
        outputs = listOf(NpuTensorContract("audio_features", longArrayOf(1,156,1024)))
    )

    @Test fun epContextUsesRelativeBasenameWhenBinaryAndWrapperAreAdjacent() {
        val dir = Files.createTempDirectory("qwen-epcontext-test").toFile()
        try {
            val binary = File(dir, "encoder-qnn-fp16.bin").apply { writeBytes(byteArrayOf(1,2,3,4)) }
            val wrapper = File(dir, "qwen3-encoder-epcontext.onnx")
            NpuEpContextModel.write(contract, binary, wrapper, "2.50")
            val serialized = wrapper.readBytes().toString(Charsets.ISO_8859_1)
            assertTrue(serialized.contains("ep_cache_context"))
            assertTrue(serialized.contains("encoder-qnn-fp16.bin"))
            assertFalse(serialized.contains(binary.absolutePath))
            assertTrue(serialized.contains("embed_mode"))
            assertTrue(serialized.contains("qwen3_asr_encoder"))
            assertTrue(binary.isFile)
        } finally { dir.deleteRecursively() }
    }

    @Test fun epContextRejectsNonadjacentContextBinary() {
        val root = Files.createTempDirectory("qwen-epcontext-mismatch").toFile()
        try {
            val modelDir = File(root, "models").apply { mkdirs() }
            val cacheDir = File(root, "cache").apply { mkdirs() }
            val binary = File(modelDir, "encoder-qnn-fp16.bin").apply { writeBytes(byteArrayOf(1)) }
            val wrapper = File(cacheDir, "qwen3-encoder-epcontext.onnx")
            assertThrows(IllegalArgumentException::class.java) {
                NpuEpContextModel.write(contract, binary, wrapper, "2.50")
            }
            assertFalse(wrapper.exists())
            assertEquals(1L, binary.length())
        } finally { root.deleteRecursively() }
    }
}
