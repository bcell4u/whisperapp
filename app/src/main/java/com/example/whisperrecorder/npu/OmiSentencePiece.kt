package com.example.whisperrecorder.npu

import java.io.ByteArrayOutputStream

/** Decode-only reader for NeMo's original SentencePiece ModelProto, without another native runtime. */
internal class OmiSentencePiece private constructor(private val pieces: Array<String>, private val types: IntArray) {
    fun decode(ids: List<Int>): String = buildText(ids).first

    fun timedWords(ids: List<Int>, frames: List<Int>, durationMs: Long): List<NpuTimedWord> {
        require(ids.size == frames.size && durationMs > 0L)
        val (decoded, positions) = buildText(ids, frames)
        val words = Regex("\\S+").findAll(decoded).toList()
        var previousEnd = 0L
        return words.mapIndexed { index, word ->
            val start = maxOf(previousEnd, framesToMs(positions[word.range.first]))
                .coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
            val nextStart = words.getOrNull(index + 1)?.let { framesToMs(positions[it.range.first]) }
            val maxFrame = (word.range.first..word.range.last).maxOf { positions[it] }
            val end = minOf(durationMs, maxOf(start + 20, framesToMs(maxFrame + 1)))
                .let { if (nextStart != null && nextStart > start) minOf(it, nextStart) else it }
                .coerceAtLeast(start + 1L)
            previousEnd = end
            NpuTimedWord(start, end, word.value)
        }
    }

    private fun framesToMs(frame: Int) = frame.toLong() * 80L

    private fun buildText(ids: List<Int>, frames: List<Int>? = null): Pair<String, List<Int>> {
        val out = StringBuilder()
        val positions = ArrayList<Int>()
        val pendingBytes = ByteArrayOutputStream()
        var pendingFrame = 0
        fun append(s: String, frame: Int) {
            out.append(s)
            repeat(s.length) { positions.add(frame) }
        }
        fun flush() {
            if (pendingBytes.size() > 0) {
                append(pendingBytes.toByteArray().toString(Charsets.UTF_8), pendingFrame)
                pendingBytes.reset()
            }
        }
        ids.forEachIndexed { i, id ->
            require(id in pieces.indices) { "Omi SentencePiece token ID out of range: $id" }
            val type = types[id]
            if (type == 3 /* CONTROL */ || type == 5 /* UNUSED */) return@forEachIndexed
            val frame = frames?.get(i) ?: 0
            val piece = pieces[id]
            if (type == 6 /* BYTE */ && piece.length == 6 && piece.startsWith("<0x") && piece.endsWith(">")) {
                if (pendingBytes.size() == 0) pendingFrame = frame
                pendingBytes.write(piece.substring(3, 5).toInt(16))
            } else {
                flush()
                if (type == 2 /* UNKNOWN */) append("�", frame)
                else append(piece.replace("▁", " "), frame)
            }
        }
        flush()
        val trim = if (out.startsWith(" ")) 1 else 0
        return out.substring(trim) to positions.drop(trim)
    }

    companion object {
        /** ModelProto repeated SentencePiece field #1; nested piece=field#1, type=field#3. */
        fun fromModel(bytes: ByteArray): OmiSentencePiece {
            require(bytes.isNotEmpty() && bytes.size <= 2 * 1024 * 1024) { "Invalid Omi tokenizer size ${bytes.size}" }
            val pieces = ArrayList<String>()
            val types = ArrayList<Int>()
            val parser = Reader(bytes)
            while (parser.hasRemaining()) {
                val tag = parser.varint().toInt()
                require(tag != 0) { "Invalid SentencePiece protobuf tag" }
                if (tag ushr 3 == 1 && tag and 7 == 2) {
                    val child = Reader(parser.readBytes())
                    var text = ""
                    var kind = 1
                    while (child.hasRemaining()) {
                        val innerTag = child.varint().toInt()
                        when (innerTag ushr 3) {
                            1 -> { require(innerTag and 7 == 2); text = child.readBytes().toString(Charsets.UTF_8) }
                            3 -> { require(innerTag and 7 == 0); kind = child.varint().toInt() }
                            else -> child.skip(innerTag and 7)
                        }
                    }
                    pieces += text
                    types += kind
                } else parser.skip(tag and 7)
            }
            require(pieces.size == 1024) { "Omi SentencePiece vocabulary count ${pieces.size}, expected 1024" }
            return OmiSentencePiece(pieces.toTypedArray(), types.toIntArray())
        }
    }

    private class Reader(private val data: ByteArray) {
        private var pos = 0
        fun hasRemaining() = pos < data.size
        fun varint(): Long {
            var n = 0L
            for (shift in 0..63 step 7) {
                require(pos < data.size) { "Truncated SentencePiece protobuf varint" }
                val b = data[pos++].toInt() and 255
                n = n or ((b and 127).toLong() shl shift)
                if (b and 128 == 0) return n
            }
            error("SentencePiece protobuf varint exceeds 64 bits")
        }
        fun readBytes(): ByteArray {
            val len = varint().toInt()
            require(len >= 0 && len <= data.size - pos) { "Invalid SentencePiece protobuf field length $len" }
            return data.copyOfRange(pos, pos + len).also { pos += len }
        }
        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> { require(data.size - pos >= 8); pos += 8 }
                2 -> { val len = varint().toInt(); require(len >= 0 && len <= data.size - pos); pos += len }
                5 -> { require(data.size - pos >= 4); pos += 4 }
                else -> error("Unsupported SentencePiece protobuf wire type $wire")
            }
        }
    }
}
