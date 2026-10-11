package com.example.whisperrecorder.qwen;
import android.os.SharedMemory;
/** Dedicated experimental worker, separate from Parakeet/Omi NPU service. */
interface IQwen3HybridService {
    String prepare();
    String transcribe(in SharedMemory pcm, int sampleCount);
    void cancelCurrent();
    void closeRuntime();
}
