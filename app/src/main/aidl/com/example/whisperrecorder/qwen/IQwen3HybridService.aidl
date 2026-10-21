package com.example.whisperrecorder.qwen;
import android.os.SharedMemory;
interface IQwen3HybridService {
    String prepareGgufLab(String configJson);
    String encodeGgufLab(in SharedMemory pcm, int sampleCount);
    String finishGgufLab();
    String verifyDecoderFixture();
    void cancelCurrent();
    void closeRuntime();
}
