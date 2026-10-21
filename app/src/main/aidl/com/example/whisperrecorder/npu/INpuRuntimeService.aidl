package com.example.whisperrecorder.npu;

import android.os.SharedMemory;

/** Persistent ORT-QNN runtime bridge used by normal NPU transcription. */
interface INpuRuntimeService {
    String bootstrap();
    String probeOmiContexts(boolean includeEncoder);
    String prepareRuntime();
    String prepareOmiRuntime();
    String transcribeOmiChunk(in SharedMemory pcm, int sampleCount, long audioDurationMs);
    String transcribeChunk(in SharedMemory pcm, int sampleCount, long audioDurationMs);
    String prepareChunk(in SharedMemory pcm, int sampleCount, long audioDurationMs);
    String transcribePreparedChunk(long preparedId);
    void releasePreparedChunk(long preparedId);
    void cancelCurrent();
    void closeRuntime();
}
