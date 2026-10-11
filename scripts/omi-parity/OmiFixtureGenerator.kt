package com.example.whisperrecorder.npu

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Deterministic, NON-SPEECH offline fixtures; run on desktop JVM, not in the app. */
object OmiFixtureGenerator {
    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: OmiFixtureGenerator <output-directory>" }
        val dir = File(args[0]).apply { mkdirs() }
        val config = ParakeetPreprocessor.Config(16_000, 128, 160, 512, 400, 0.97f, 192_000, 1201)
        val processor = ParakeetPreprocessor(config)
        val stimuli = listOf(
            "silence_1s" to FloatArray(16_000),
            "impulse_1s" to FloatArray(16_000) { if (it == 8_123) 0.8f else 0f },
            "tone_12s" to FloatArray(192_000) { i -> (0.1 * sin(2 * PI * 440 * i / 16_000)).toFloat() },
            "noise_partial" to Random(20261010).let { rng -> FloatArray(51_237) { (rng.nextDouble() * 0.02 - 0.01).toFloat() } }
        )
        for ((name, pcm) in stimuli) {
            val result = processor.extract(pcm)
            save(File(dir, "$name.pcm.f32le"), pcm)
            save(File(dir, "$name.features.f32le"), result.audioSignal)
            save(File(dir, "$name.mask1.f32le"), result.subsampleMask1)
            save(File(dir, "$name.mask2.f32le"), result.subsampleMask2)
            save(File(dir, "$name.mask3.f32le"), result.subsampleMask3)
            println("fixture=$name samples=${pcm.size} validMelFrames=${result.validFeatureFrames} encodedFrames=${result.validEncodedFrames}")
        }
    }

    private fun save(file: File, values: FloatArray) {
        val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        file.writeBytes(buffer.array())
    }
}
