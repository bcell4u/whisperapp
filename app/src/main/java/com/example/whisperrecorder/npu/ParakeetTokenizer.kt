package com.example.whisperrecorder.npu

import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Minimal decode-only adapter for the pinned Hugging Face BPE/Metaspace tokenizer. */
internal class ParakeetTokenizer private constructor(
    private val piecesById: Array<String?>,
    private val specialIds: Set<Int>,
    private val replacement: String,
    private val padTokenId: Int
) {
    fun decode(tokenIds: List<Int>, skipSpecialTokens: Boolean = true): String {
        val out = StringBuilder()
        val byteFallback = ByteArrayOutputStream()

        fun flushBytes() {
            if (byteFallback.size() == 0) return
            out.append(byteFallback.toByteArray().toString(Charsets.UTF_8))
            byteFallback.reset()
        }

        tokenIds.forEach { id ->
            if (id == padTokenId) return@forEach
            if (skipSpecialTokens && id in specialIds) return@forEach
            val piece = piecesById.getOrNull(id) ?: return@forEach
            val byte = parseByteFallback(piece)
            if (byte != null) {
                byteFallback.write(byte)
            } else {
                flushBytes()
                out.append(piece)
            }
        }
        flushBytes()

        val decoded = if (replacement.isEmpty()) out.toString() else out.toString().replace(replacement, " ")
        // Metaspace prepend_scheme=always inserts a leading replacement marker for normal text.
        return decoded.removePrefix(" ")
    }

    /** Decoder-frame word starts, approximate ends; emitted frames are 80 ms apart. */
    fun decodeTimedWords(ids: List<Int>, frames: List<Int>, durationMs: Long): List<NpuTimedWord> {
        require(ids.size == frames.size && durationMs > 0)
        val text = StringBuilder()
        val positions = ArrayList<Int>()
        val bytes = ByteArrayOutputStream()
        var byteFrame = 0
        fun append(raw: String, frame: Int) {
            val decoded = raw.replace(replacement, " ")
            text.append(decoded)
            repeat(decoded.length) { positions.add(frame) }
        }
        fun flush() {
            if (bytes.size() > 0) {
                append(bytes.toByteArray().toString(Charsets.UTF_8), byteFrame)
                bytes.reset()
            }
        }
        ids.forEachIndexed { index, id ->
            if (id == padTokenId || id in specialIds) return@forEachIndexed
            val piece = piecesById.getOrNull(id) ?: return@forEachIndexed
            val byte = parseByteFallback(piece)
            if (byte != null) {
                if (bytes.size() == 0) byteFrame = frames[index]
                bytes.write(byte)
            } else {
                flush()
                append(piece, frames[index])
            }
        }
        flush()
        val offset = if (text.startsWith(" ")) 1 else 0
        val decoded = text.substring(offset)
        require(decoded == decode(ids)) { "Tokenizer timing reconstruction differs from transcript" }
        val matches = Regex("\\S+").findAll(decoded).toList()
        var previousEnd = 0L
        return matches.mapIndexed { index, match ->
            val from = match.range.first + offset
            val to = match.range.last + offset
            val start = maxOf(previousEnd, positions[from] * 80L)
            val lastFrame = positions.subList(from, to + 1).maxOrNull() ?: positions[from]
            val nextFrameMs = matches.getOrNull(index + 1)?.let { positions[it.range.first + offset] * 80L }
            val end = maxOf(start + 40L, (lastFrame + 1L) * 80L)
                .let { e -> if (nextFrameMs != null && nextFrameMs > start) minOf(e, nextFrameMs) else e }
                .coerceAtMost(durationMs)
            require(end > start) { "Word timing exhausted available audio" }
            previousEnd = end
            NpuTimedWord(start, end, match.value)
        }
    }

    companion object {
        fun fromJson(text: String): ParakeetTokenizer {
            val root = JSONObject(text)
            val model = root.getJSONObject("model")
            require(model.optString("type") == "BPE") { "Unsupported tokenizer model: ${model.optString("type")}" }
            require(model.optBoolean("byte_fallback", false)) { "Pinned tokenizer must expose BPE byte fallback" }

            val vocab = model.getJSONObject("vocab")
            var maxId = -1
            val pairs = ArrayList<Pair<Int, String>>(vocab.length() + 16)
            val keys = vocab.keys()
            while (keys.hasNext()) {
                val piece = keys.next()
                val id = vocab.getInt(piece)
                maxId = maxOf(maxId, id)
                pairs += id to piece
            }

            // Hugging Face tokenizers can keep special/control tokens outside model.vocab.
            // Parakeet does exactly that for <blank> id 8192, while model.vocab ends at 8191.
            val specialIds = mutableSetOf<Int>()
            val added = root.optJSONArray("added_tokens")
            if (added != null) {
                for (i in 0 until added.length()) {
                    val token = added.getJSONObject(i)
                    val id = token.getInt("id")
                    val content = token.getString("content")
                    maxId = maxOf(maxId, id)
                    pairs += id to content
                    if (token.optBoolean("special", false)) specialIds += id
                }
            }
            require(maxId >= 8192) { "Tokenizer id space is unexpectedly small: maxId=$maxId" }
            val pieces = arrayOfNulls<String>(maxId + 1)
            pairs.forEach { (id, piece) -> pieces[id] = piece }

            val decoder = root.getJSONObject("decoder")
            require(decoder.optString("type") == "Metaspace") { "Unsupported tokenizer decoder: ${decoder.optString("type")}" }
            require(decoder.optString("prepend_scheme") == "always") { "Unexpected Metaspace prepend scheme" }
            val replacement = decoder.optString("replacement", "▁")
            require(replacement.isNotEmpty()) { "Tokenizer Metaspace replacement is empty" }

            return ParakeetTokenizer(
                piecesById = pieces,
                specialIds = specialIds,
                replacement = replacement,
                padTokenId = 2
            )
        }

        private fun parseByteFallback(piece: String): Int? {
            if (piece.length != 6 || !piece.startsWith("<0x") || piece.last() != '>') return null
            return piece.substring(3, 5).toIntOrNull(16)
        }
    }
}

internal data class NpuTimedWord(val startMs: Long, val endMs: Long, val text: String)
