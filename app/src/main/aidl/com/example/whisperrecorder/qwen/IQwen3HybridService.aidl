package com.example.whisperrecorder.qwen;
import android.os.SharedMemory;
interface IQwen3HybridService {
    String prepare(String configJson);
    String encode(in SharedMemory pcm, int sampleCount);
    String finish();
    void cancelCurrent();
    void closeRuntime();
}
