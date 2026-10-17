package com.example.whisperrecorder.npu

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/** GPT-style byte-level BPE decoding for the pinned Qwen3 tokenizer JSON.
 * This is only the output decoder: tokenizer *encoding* is not used by the fixed prompt contract.
 */
internal class Qwen3ByteTokenizer private constructor(private val vocabulary: Map<Int, String>) {
    private val inverseByteAlphabet: Map<Int, Int> = run {
        val bytes = (33..126).toMutableList().apply { addAll(161..172); addAll(174..255) }
        val unicode = bytes.toMutableList()
        var extra = 256
        for (value in 0..255) if (value !in bytes) {
            bytes.add(value)
            unicode.add(extra++)
        }
        unicode.indices.associate { unicode[it] to bytes[it] }
    }
    fun decode(ids: List<Int>): String {
        val output = ByteArrayOutputStream()
        for (id in ids) {
            if (id == 151643 || id == 151645) break
            val token = vocabulary[id] ?: error("Qwen tokenizer vocabulary missing generated token $id")
            if (token.startsWith("<|") && token.endsWith("|>")) continue
            // Qwen's tokenizer.json uses GPT2 ByteLevel symbols (space=U+0120, newline=U+010A).
            for (codepoint in token.codePoints().toArray()) {
                val byte = inverseByteAlphabet[codepoint]
                if (byte != null) output.write(byte)
                else output.write(String(Character.toChars(codepoint)).toByteArray(Charsets.UTF_8))
            }
        }
        return output.toByteArray().toString(Charsets.UTF_8).trim()
    }

    companion object {
        fun load(file: File): Qwen3ByteTokenizer {
            require(file.isFile) { "Missing Qwen tokenizer.json" }
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val vocab = root.getJSONObject("model").getJSONObject("vocab")
            val mapping = HashMap<Int, String>(vocab.length() + 300)
            for (key in vocab.keys()) mapping[vocab.getInt(key)] = key
            root.optJSONArray("added_tokens")?.let { added ->
                for (i in 0 until added.length()) {
                    val entry = added.getJSONObject(i)
                    mapping[entry.getInt("id")] = entry.getString("content")
                }
            }
            require(mapping.size >= 151000) { "Unexpected Qwen3 tokenizer vocabulary" }
            return Qwen3ByteTokenizer(mapping)
        }
    }
}
