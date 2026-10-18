package com.example.whisperrecorder.advanced

import com.example.whisperrecorder.transcribe.AsrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAsrCatalogTest {
    @Test
    fun productionParakeetIsSingleNvidiaV3Q8Model() {
        val model = LocalAsrModelManager.byEngine(AsrEngine.PARAKEET)!!
        assertEquals("NVIDIA Parakeet TDT 0.6B v3 Q8_0 — Recommended", model.name)
        assertEquals("tdt-0.6b-v3-q8_0.gguf", model.fileName)
        assertEquals(940_663_680L, model.expectedBytes)
        assertTrue(model.modelInfoUrl.contains("huggingface.co/nvidia/parakeet-tdt-0.6b-v3"))
    }

    @Test
    fun omiIsQ8MedicalModelOnAutomaticOpenClRuntime() {
        val model = LocalAsrModelManager.byEngine(AsrEngine.OMI)!!
        assertEquals("Omi Med STT v1 Q8_0 (0.6B) — Medical", model.name)
        assertEquals("omi-med-stt-v1-q8_0.gguf", model.fileName)
        assertEquals(929_205_888L, model.expectedBytes)
        assertTrue(model.licenseName.contains("OpenCL GPU automatic"))
    }

    @Test
    fun nvidiaSectionContainsOnlyTheProductionParakeetEngine() {
        assertTrue(LocalAsrModelManager.nvidiaParakeetModels.all { it.engine == AsrEngine.PARAKEET })
        assertTrue("parakeet11" !in AsrEngine.ids)
    }

}
