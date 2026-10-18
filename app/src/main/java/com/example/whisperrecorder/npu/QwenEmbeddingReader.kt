package com.example.whisperrecorder.npu

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Exact FP16 little-endian embedding lookup for the separately verified Stage K2A candidate.
 * Original v3 manifest continues to require FP32; this is opt-in only and never deletes it.
 */
internal object QwenEmbeddingReader {
    const val VOCAB = 151936
    const val WIDTH = 1024
    const val FP16_BYTES = 311164928L
    const val FP16_SHA256 = "e80150119fa5f7e56e85aed64c3a02d5c78eb7a37cfdcb973d0987316f15bee2"
    const val FP16_FILE = "embed_tokens_fp16.bin"

    /** Binary16 -> binary32 with subnormals, signed zero, infinities and NaNs handled. */
    fun halfToFloat(bits: Int): Float {
        val sign = (bits and 0x8000) shl 16
        val exp = (bits ushr 10) and 31
        val fraction = bits and 1023
        val floatBits = when (exp) {
            0 -> if (fraction == 0) sign else {
                var mantissa = fraction
                var exponent = -14
                while (mantissa and 1024 == 0) { mantissa = mantissa shl 1; exponent-- }
                sign or ((exponent + 127) shl 23) or ((mantissa and 1023) shl 13)
            }
            31 -> sign or 0x7f800000 or (fraction shl 13)
            else -> sign or ((exp - 15 + 127) shl 23) or (fraction shl 13)
        }
        return Float.fromBits(floatBits)
    }

    /** Decode one row without per-token ByteArray/FloatArray/ByteBuffer allocations.
     * This performs the same little-endian conversion as readRow(). The caller owns
     * the scratch buffers and must not share them concurrently between workers.
     */
    fun readRowInto(
        file: RandomAccessFile, token: Int, fp16: Boolean,
        destination: FloatArray, scratch: ByteArray
    ) {
        require(token in 0 until VOCAB)
        require(destination.size == WIDTH)
        val rowBytes = WIDTH * if (fp16) 2 else 4
        require(scratch.size >= rowBytes)
        file.seek(token.toLong() * rowBytes)
        file.readFully(scratch, 0, rowBytes)
        if (fp16) {
            for (i in 0 until WIDTH) {
                val index = i * 2
                destination[i] = halfToFloat((scratch[index].toInt() and 255) or
                    ((scratch[index + 1].toInt() and 255) shl 8))
            }
        } else {
            for (i in 0 until WIDTH) {
                val index = i * 4
                val bits = (scratch[index].toInt() and 255) or
                    ((scratch[index + 1].toInt() and 255) shl 8) or
                    ((scratch[index + 2].toInt() and 255) shl 16) or
                    ((scratch[index + 3].toInt() and 255) shl 24)
                destination[i] = Float.fromBits(bits)
            }
        }
    }

    fun readRow(file: RandomAccessFile, token: Int, fp16: Boolean): FloatArray {
        require(token in 0 until VOCAB)
        val bytesPerElement = if (fp16) 2 else 4
        val bytes = ByteArray(WIDTH * bytesPerElement)
        file.seek(token.toLong() * bytes.size)
        file.readFully(bytes)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(WIDTH) {
            if (fp16) halfToFloat(buffer.short.toInt() and 0xffff) else buffer.float
        }
    }
}
