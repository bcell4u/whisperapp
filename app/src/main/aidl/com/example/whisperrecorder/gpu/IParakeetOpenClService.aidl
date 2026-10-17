package com.example.whisperrecorder.gpu;

import android.os.SharedMemory;

interface IParakeetOpenClService {
    String runtimeDiagnostics();
    long open(String modelPath);
    String transcribe(long handle, in SharedMemory pcmSharedMemory, int sampleCount);
    void cancel(long handle);
    void close(long handle);
}
