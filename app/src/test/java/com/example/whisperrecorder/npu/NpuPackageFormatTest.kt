package com.example.whisperrecorder.npu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NpuPackageFormatTest {
    @Test
    fun safeRelativePath_acceptsCanonicalRepositoryPath() {
        assertEquals("model/encoder.bin", NpuPackageFormat.safeRelativePath("model/encoder.bin"))
    }

    @Test
    fun safeRelativePath_rejectsTraversalAndAbsolutePaths() {
        assertThrows(IllegalArgumentException::class.java) { NpuPackageFormat.safeRelativePath("../encoder.bin") }
        assertThrows(IllegalArgumentException::class.java) { NpuPackageFormat.safeRelativePath("/model/encoder.bin") }
        assertThrows(IllegalArgumentException::class.java) { NpuPackageFormat.safeRelativePath("model\\encoder.bin") }
    }

    @Test
    fun parseChecksums_parsesAndNormalizesDigest() {
        val hash = "ABCDEF" + "0".repeat(58)
        val parsed = NpuPackageFormat.parseChecksums("$hash  model/encoder.bin\n")
        assertEquals(hash.lowercase(), parsed["model/encoder.bin"])
    }

    @Test
    fun parseChecksums_rejectsDuplicatePath() {
        val hash = "a".repeat(64)
        assertThrows(IllegalArgumentException::class.java) {
            NpuPackageFormat.parseChecksums("$hash  model/encoder.bin\n$hash  model/encoder.bin\n")
        }
    }
}
