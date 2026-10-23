package com.example.whisperrecorder.qwen;

import com.example.whisperrecorder.qwen.IQwen3ProgressCallback;
import android.os.SharedMemory;
interface IQwen3HybridService {
    String prepare(String configJson);
    String encode(in SharedMemory pcm, int sampleCount);
    String finish(IQwen3ProgressCallback callback);
    void cancelCurrent();
    void closeRuntime();
}
