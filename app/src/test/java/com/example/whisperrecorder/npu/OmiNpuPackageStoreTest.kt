package com.example.whisperrecorder.npu

import org.junit.Assert.*
import org.junit.Test

class OmiNpuPackageStoreTest {
    @Test fun downloaderIncludesThreeContextsAndTwoHostAssets() {
        val names = OmiNpuPackageStore.ASSETS.keys
        assertEquals(setOf("encoder.bin", "predictor.bin", "joint.bin", "token_embedding.npy", "tokenizer.model"), names)
        assertEquals(1_235_851_188L, OmiNpuPackageStore.totalBytes)
    }

    @Test fun downloadedContextsKeepPublishedMetadataUnchanged() {
        OmiNpuProbeContracts.ALL.forEach { context ->
            val artifact = OmiNpuPackageStore.ASSETS.getValue(context.graph.artifactPath)
            assertEquals(context.graph.artifactBytes, artifact.bytes)
            assertEquals(context.sha256, artifact.sha256)
        }
        OmiNpuPackageStore.ASSETS.values.forEach { artifact ->
            assertEquals(64, artifact.sha256.length)
            assertTrue(artifact.bytes > 0)
        }
    }

    @Test fun experimentalPackageDoesNotReplaceParakeetRuntime() {
        assertNotEquals(NpuPackageStore.DOWNLOAD_ID, OmiNpuPackageStore.DOWNLOAD_ID)
        assertNotEquals(NpuPackageStore.PACKAGE_ID, OmiNpuPackageStore.PACKAGE_ID)
        assertTrue(OmiNpuPackageStore.ARCHIVE_PATH.startsWith("Whisperapp/Models/"))
    }
}
