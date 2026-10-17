package com.example.whisperrecorder.npu

import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Generates the tiny ONNX EPContext wrapper used by ORT-QNN to load an external QNN context binary.
 * The QNN binary remains external; no model weights are duplicated into this wrapper.
 */
internal object NpuEpContextModel {
    private const val ONNX_FLOAT = 1L
    private const val ATTR_INT = 2L
    private const val ATTR_STRING = 3L

    fun write(contract: NpuGraphContract, contextFile: File, destination: File, qnnSdkVersion: String = "2.50"): File {
        require(contextFile.isFile && contextFile.length() > 0L) { "QNN context file is missing: ${contextFile.path}" }
        require(contextFile.parentFile?.canonicalFile == destination.parentFile?.canonicalFile) {
            "EPContext wrapper and external QNN context must share a directory"
        }
        return writeExternalPath(contract, contextFile.name, destination, qnnSdkVersion)
    }

    /** Writes an EPContext model whose cache path may be absolute (for descriptor/real-path probes). */
    fun writeExternalPath(
        contract: NpuGraphContract,
        cachePath: String,
        destination: File,
        qnnSdkVersion: String = "2.50"
    ): File {
        require(cachePath.isNotBlank()) { "QNN context cache path must not be blank" }
        val node = message {
            contract.inputs.forEach { string(1, it.name) }
            contract.outputs.forEach { string(2, it.name) }
            string(3, contract.graphName)
            string(4, "EPContext")
            bytes(5, attributeString("ep_cache_context", cachePath))
            bytes(5, attributeInt("embed_mode", 0L))
            bytes(5, attributeString("ep_sdk_version", qnnSdkVersion))
            bytes(5, attributeString("source", "Qnn"))
            string(7, "com.microsoft")
        }
        val graph = message {
            bytes(1, node)
            string(2, "qnn-onnx-model")
            contract.inputs.forEach { bytes(11, valueInfo(it)) }
            contract.outputs.forEach { bytes(12, valueInfo(it)) }
        }
        val model = message {
            int64(1, 10L)
            string(2, "WhisperRecorder")
            string(3, "whisperrecorder-npu")
            bytes(7, graph)
            bytes(8, opset("", 21L))
            bytes(8, opset("com.microsoft", 1L))
        }
        destination.parentFile?.mkdirs()
        destination.writeBytes(model)
        return destination
    }

    private fun valueInfo(t: NpuTensorContract): ByteArray = message {
        string(1, t.name)
        bytes(2, message {
            bytes(1, message {
                int64(1, ONNX_FLOAT)
                bytes(2, message {
                    t.shape.forEach { dim -> bytes(1, message { int64(1, dim) }) }
                })
            })
        })
    }

    private fun attributeString(name: String, value: String): ByteArray = message {
        string(1, name)
        string(4, value)
        int64(20, ATTR_STRING)
    }

    private fun attributeInt(name: String, value: Long): ByteArray = message {
        string(1, name)
        int64(3, value)
        int64(20, ATTR_INT)
    }

    private fun opset(domain: String, version: Long): ByteArray = message {
        if (domain.isNotEmpty()) string(1, domain)
        int64(2, version)
    }

    private inline fun message(block: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter().apply(block).toByteArray()

    private class ProtoWriter {
        private val out = ByteArrayOutputStream()
        fun int64(field: Int, value: Long) {
            tag(field, 0); varint(value)
        }
        fun string(field: Int, value: String) = bytes(field, value.toByteArray(Charsets.UTF_8))
        fun bytes(field: Int, value: ByteArray) {
            tag(field, 2); varint(value.size.toLong()); out.write(value)
        }
        private fun tag(field: Int, wire: Int) = varint(((field shl 3) or wire).toLong())
        private fun varint(raw: Long) {
            var value = raw
            while (true) {
                if ((value and -128L) == 0L) { out.write(value.toInt()); return }
                out.write(((value and 127L) or 128L).toInt())
                value = value ushr 7
            }
        }
        fun toByteArray(): ByteArray = out.toByteArray()
    }
}
