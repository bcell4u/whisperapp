package com.example.whisperrecorder.transcribe

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import kotlin.math.ceil

/**
 * Streaming media-audio decoder used by every transcription engine.
 *
 * The source URI remains the source of truth. We decode only its audio track and resample it
 * directly into bounded 16 kHz mono segments, so imported audio and video never require a
 * full-length PCM/WAV copy in app storage or cache.
 */
object PcmDecoder {
    private const val TARGET_SAMPLE_RATE = 16_000

    data class StreamInfo(
        val sampleRate: Int,
        val channelCount: Int,
        val durationMs: Long,
        val audioStartMs: Long,
        val trackMime: String
    )

    data class TimedChunk(
        val samples: FloatArray,
        val startMs: Long,
        val endMs: Long
    )

    fun prepare(context: Context, uri: Uri): PreparedSource = PreparedSource(context, uri)

    class PreparedSource internal constructor(context: Context, uri: Uri) : AutoCloseable {
        internal val extractor = MediaExtractor()
        internal val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Cannot open media")
        internal lateinit var inputFormat: MediaFormat
            private set
        lateinit var info: StreamInfo
            private set
        private var transferred = false
        private var closed = false

        init {
            try {
                extractor.setDataSource(pfd.fileDescriptor)
                val (trackIndex, format) = findAudioTrack(extractor)
                extractor.selectTrack(trackIndex)
                inputFormat = format
                val audioStartMs = extractor.sampleTime.coerceAtLeast(0L) / 1000L
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val durationMs = if (format.containsKey(MediaFormat.KEY_DURATION))
                    format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L) / 1000L else 0L
                require(sampleRate > 0 && channels > 0) { "Invalid media audio format" }
                info = StreamInfo(sampleRate, channels, durationMs, audioStartMs,
                    requireNotNull(format.getString(MediaFormat.KEY_MIME)))
            } catch (t: Throwable) {
                extractor.release(); pfd.close(); closed = true; throw t
            }
        }

        fun open16kSegmentStream(segmentSeconds: Int, overlapSeconds: Int = 0): SegmentStream {
            check(!closed) { "Media source is closed" }
            check(!transferred) { "Media source decoder already opened" }
            transferred = true
            return SegmentStream(this, segmentSeconds, overlapSeconds)
        }

        override fun close() {
            if (closed || transferred) return
            closed = true
            extractor.release(); pfd.close()
        }
    }

    fun countSegments(
        totalSourceFrames: Long,
        sampleRate: Int,
        segmentSeconds: Int,
        overlapSeconds: Int = 0
    ): Int {
        require(sampleRate > 0) { "Invalid sample rate" }
        require(segmentSeconds > 0) { "Segment duration must be positive" }
        require(overlapSeconds >= 0 && overlapSeconds < segmentSeconds) { "Invalid segment overlap" }
        if (totalSourceFrames <= 0L) return 0
        val segmentFrames = sampleRate.toLong() * segmentSeconds.toLong()
        val strideFrames = sampleRate.toLong() * (segmentSeconds - overlapSeconds).toLong()
        if (totalSourceFrames <= segmentFrames) return 1
        val remainingAfterFirst = totalSourceFrames - segmentFrames
        return (1L + (remainingAfterFirst + strideFrames - 1L) / strideFrames)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun countSegmentsByDuration(
        durationMs: Long,
        segmentSeconds: Int,
        overlapSeconds: Int = 0
    ): Int {
        val estimatedFrames = ((durationMs.coerceAtLeast(0L) * TARGET_SAMPLE_RATE) + 999L) / 1000L
        return countSegments(estimatedFrames, TARGET_SAMPLE_RATE, segmentSeconds, overlapSeconds)
    }

    /** Closable lazy decoder that takes ownership of a prepared media source. */
    class SegmentStream internal constructor(
        source: PreparedSource,
        segmentSeconds: Int,
        overlapSeconds: Int
    ) : Iterator<TimedChunk>, AutoCloseable {
        private val segmentSamples = Math.multiplyExact(segmentSeconds, TARGET_SAMPLE_RATE)
        private val strideSamples = Math.multiplyExact(segmentSeconds - overlapSeconds, TARGET_SAMPLE_RATE)
        private val accumulator: SegmentAccumulator
        private val ready = ArrayDeque<TimedChunk>()
        private val extractor = source.extractor
        private val pfd: ParcelFileDescriptor = source.pfd
        private var codec: MediaCodec? = null
        private var rawPcm = false
        private var inputDone = false
        private var finished = false
        private var closed = false
        private var channels = 0
        private var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        private var sourceSampleRate = 0
        private val resampler: StreamingMonoResampler
        private val codecInfo = MediaCodec.BufferInfo()
        private val rawScratch = ByteBuffer.allocateDirect(256 * 1024).order(ByteOrder.LITTLE_ENDIAN)

        init {
            require(segmentSeconds > 0) { "Segment duration must be positive" }
            require(overlapSeconds >= 0 && overlapSeconds < segmentSeconds) { "Invalid segment overlap" }
            try {
                val inputFormat = source.inputFormat
                accumulator = SegmentAccumulator(segmentSamples, strideSamples, source.info.audioStartMs)
                sourceSampleRate = source.info.sampleRate
                channels = source.info.channelCount
                if (inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcmEncoding = inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                resampler = StreamingMonoResampler(sourceSampleRate)

                val mime = source.info.trackMime
                rawPcm = mime == MediaFormat.MIMETYPE_AUDIO_RAW || mime == "audio/raw"
                if (!rawPcm) {
                    val createdCodec = MediaCodec.createDecoderByType(mime)
                    codec = createdCodec
                    createdCodec.configure(inputFormat, null, null, 0)
                    createdCodec.start()
                }
            } catch (t: Throwable) {
                close()
                throw t
            }
        }

        override fun hasNext(): Boolean {
            check(!closed) { "Audio stream is closed" }
            while (ready.isEmpty() && !finished) pump()
            return ready.isNotEmpty()
        }

        override fun next(): TimedChunk {
            if (!hasNext()) throw NoSuchElementException()
            return ready.removeFirst()
        }

        private fun pump() {
            if (rawPcm) {
                rawScratch.clear()
                val size = extractor.readSampleData(rawScratch, 0)
                if (size < 0) {
                    finishDecoding()
                    return
                }
                rawScratch.position(0)
                rawScratch.limit(size)
                processDecoded(rawScratch.slice().order(ByteOrder.LITTLE_ENDIAN), pcmEncoding, channels)
                extractor.advance()
                return
            }

            val activeCodec = requireNotNull(codec)
            while (ready.isEmpty() && !finished) {
                if (!inputDone) {
                    val inputIndex = activeCodec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = requireNotNull(activeCodec.getInputBuffer(inputIndex))
                        input.clear()
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            activeCodec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            activeCodec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = activeCodec.dequeueOutputBuffer(codecInfo, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> applyOutputFormat(activeCodec.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (codecInfo.size > 0) {
                            val output = requireNotNull(activeCodec.getOutputBuffer(outputIndex))
                            output.position(codecInfo.offset)
                            output.limit(codecInfo.offset + codecInfo.size)
                            processDecoded(output.slice().order(ByteOrder.LITTLE_ENDIAN), pcmEncoding, channels)
                        }
                        val reachedEnd = codecInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        activeCodec.releaseOutputBuffer(outputIndex, false)
                        if (reachedEnd) finishDecoding()
                    }
                }
            }
        }

        private fun applyOutputFormat(format: MediaFormat) {
            val newRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val newChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            require(newRate > 0 && newChannels > 0) { "Invalid decoded audio format" }
            if (resampler.hasConsumedFrames && newRate != sourceSampleRate) {
                error("Decoder changed sample rate during media")
            }
            sourceSampleRate = newRate
            channels = newChannels
            resampler.updateSampleRate(newRate)
            if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                pcmEncoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            }
        }

        private fun processDecoded(buffer: ByteBuffer, encoding: Int, channelCount: Int) {
            val outputSize = resampler.process(buffer, encoding, channelCount)
            if (outputSize > 0) accumulator.append(resampler.outputBuffer, outputSize)
            while (accumulator.hasFullSegment()) ready.addLast(accumulator.takeFullSegment())
        }

        private fun finishDecoding() {
            if (finished) return
            accumulator.takeTailIfNeeded()?.let(ready::addLast)
            finished = true
        }

        override fun close() {
            if (closed) return
            closed = true
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            codec = null
            extractor.release()
            pfd.close()
            ready.clear()
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Pair<Int, MediaFormat> {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                return index to format
            }
        }
        error("No readable audio track found")
    }

    /** Stateful linear resampler so codec-buffer boundaries do not accumulate timing drift. */
    private class StreamingMonoResampler(initialSampleRate: Int) {
        private var sourceSampleRate = initialSampleRate
        private var sourceFrameIndex = 0L
        private var nextOutputSourcePosition = 0.0
        private var previousSample = 0f
        private var hasPrevious = false

        val hasConsumedFrames: Boolean get() = sourceFrameIndex > 0L

        fun updateSampleRate(value: Int) {
            require(value > 0) { "Invalid decoded sample rate" }
            sourceSampleRate = value
        }

        var outputBuffer = FloatArray(4096)
            private set

        fun process(buffer: ByteBuffer, encoding: Int, channels: Int): Int {
            require(channels > 0) { "Invalid decoded channel count" }
            val bytesPerSample = when (encoding) {
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_FLOAT -> 4
                AudioFormat.ENCODING_PCM_8BIT -> 1
                else -> error("Unsupported decoded PCM encoding: $encoding")
            }
            val frameCount = buffer.remaining() / (bytesPerSample * channels)
            if (frameCount <= 0) return 0

            val expected = ceil(frameCount.toDouble() * TARGET_SAMPLE_RATE.toDouble() / sourceSampleRate.toDouble())
                .toInt().coerceAtLeast(1) + 4
            if (outputBuffer.size < expected) outputBuffer = outputBuffer.copyOf(expected)
            var outSize = 0
            val ratio = sourceSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()

            fun append(value: Float) {
                if (outSize == outputBuffer.size) outputBuffer = outputBuffer.copyOf(outputBuffer.size * 2)
                outputBuffer[outSize++] = value.coerceIn(-1f, 1f)
            }

            for (frame in 0 until frameCount) {
                var sum = 0f
                for (channel in 0 until channels) {
                    val sampleIndex = frame * channels + channel
                    sum += when (encoding) {
                        AudioFormat.ENCODING_PCM_16BIT -> buffer.getShort(sampleIndex * 2) / 32768f
                        AudioFormat.ENCODING_PCM_FLOAT -> buffer.getFloat(sampleIndex * 4)
                        AudioFormat.ENCODING_PCM_8BIT -> ((buffer.get(sampleIndex).toInt() and 0xff) - 128) / 128f
                        else -> 0f
                    }
                }
                val current = (sum / channels.toFloat()).coerceIn(-1f, 1f)
                val currentIndex = sourceFrameIndex.toDouble()
                if (!hasPrevious) {
                    previousSample = current
                    hasPrevious = true
                    if (nextOutputSourcePosition <= 0.0) {
                        append(current)
                        nextOutputSourcePosition += ratio
                    }
                } else {
                    val previousIndex = currentIndex - 1.0
                    while (nextOutputSourcePosition <= currentIndex + 1e-9) {
                        val fraction = (nextOutputSourcePosition - previousIndex).coerceIn(0.0, 1.0).toFloat()
                        append(previousSample + (current - previousSample) * fraction)
                        nextOutputSourcePosition += ratio
                    }
                    previousSample = current
                }
                sourceFrameIndex++
            }
            return outSize
        }
    }

    private class SegmentAccumulator(
        private val segmentSamples: Int,
        private val strideSamples: Int,
        private val timelineOffsetMs: Long
    ) {
        private var buffer = FloatArray(segmentSamples.coerceAtMost(32 * 1024).coerceAtLeast(1))
        private var size = 0
        private var startSample = 0L
        private var totalSamples = 0L
        private var lastYieldEndSample = 0L

        fun append(samples: FloatArray, length: Int = samples.size) {
            if (length <= 0) return
            require(length <= samples.size) { "Invalid PCM append length" }
            ensureCapacity(size + length)
            samples.copyInto(buffer, destinationOffset = size, startIndex = 0, endIndex = length)
            size += length
            totalSamples += length.toLong()
        }

        fun hasFullSegment(): Boolean = size >= segmentSamples

        fun takeFullSegment(): TimedChunk {
            check(size >= segmentSamples)
            val samples = buffer.copyOfRange(0, segmentSamples)
            val chunkStart = startSample
            val chunkEnd = chunkStart + segmentSamples.toLong()
            lastYieldEndSample = chunkEnd
            discard(strideSamples)
            return TimedChunk(
                samples = samples,
                startMs = timelineOffsetMs + chunkStart * 1000L / TARGET_SAMPLE_RATE,
                endMs = timelineOffsetMs + chunkEnd * 1000L / TARGET_SAMPLE_RATE
            )
        }

        fun takeTailIfNeeded(): TimedChunk? {
            if (size <= 0 || totalSamples <= lastYieldEndSample) return null
            val samples = buffer.copyOf(size)
            val chunkStart = startSample
            val chunkEnd = chunkStart + size.toLong()
            lastYieldEndSample = chunkEnd
            return TimedChunk(
                samples = samples,
                startMs = timelineOffsetMs + chunkStart * 1000L / TARGET_SAMPLE_RATE,
                endMs = timelineOffsetMs + chunkEnd * 1000L / TARGET_SAMPLE_RATE
            )
        }

        private fun discard(count: Int) {
            val used = count.coerceAtMost(size)
            val remaining = size - used
            if (remaining > 0) buffer.copyInto(buffer, destinationOffset = 0, startIndex = used, endIndex = size)
            size = remaining
            startSample += used.toLong()
        }

        private fun ensureCapacity(required: Int) {
            if (required <= buffer.size) return
            // Do not let geometric growth overshoot a multi-minute segment by tens of MiB.
            // Once we approach the segment ceiling, grow only as much as the current append needs.
            val doubledWithinSegment = (buffer.size.toLong() * 2L)
                .coerceAtMost(segmentSamples.toLong())
                .toInt()
            val capacity = maxOf(required, doubledWithinSegment)
            buffer = buffer.copyOf(capacity)
        }
    }
}
