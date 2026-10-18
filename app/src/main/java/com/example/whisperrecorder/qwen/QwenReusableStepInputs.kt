package com.example.whisperrecorder.qwen

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import com.example.whisperrecorder.npu.QwenEmbeddingReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** Experimental, synchronous ORT CPU step input reuse.
 * Direct memory stays owned by this object until BOTH tensors have closed.
 * Each step must complete before overwriting the buffers for the next one.
 * No QNN tensor, output, or KV cache is shared or changed by this helper.
 */
internal class QwenReusableStepInputs(env: OrtEnvironment) : AutoCloseable {
    private val embeddingBytes = ByteBuffer.allocateDirect(QwenEmbeddingReader.WIDTH * 4)
        .order(ByteOrder.nativeOrder())
    private val positionBytes = ByteBuffer.allocateDirect(java.lang.Long.BYTES)
        .order(ByteOrder.nativeOrder())
    private val embeddings: FloatBuffer = embeddingBytes.asFloatBuffer()
    private val positions: LongBuffer = positionBytes.asLongBuffer()
    val embeddingTensor: OnnxTensor = OnnxTensor.createTensor(
        env, embeddings, longArrayOf(1, 1, QwenEmbeddingReader.WIDTH.toLong())
    )
    val positionTensor: OnnxTensor = try {
        OnnxTensor.createTensor(env, positions, longArrayOf(1, 1))
    } catch (t: Throwable) {
        embeddingTensor.close()
        throw t
    }

    fun update(row: FloatArray, position: Long) {
        require(row.size == QwenEmbeddingReader.WIDTH)
        embeddings.position(0)
        embeddings.put(row)
        embeddings.position(0)
        positions.put(0, position)
    }

    override fun close() {
        positionTensor.close()
        embeddingTensor.close()
    }
}
