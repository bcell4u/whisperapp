package com.example.whisperrecorder.qwen;

/** Private UI progress only; never carries audio, text, tokens, or paths. */
oneway interface IQwen3ProgressCallback {
    void onWindowDecoded(int completed, int total);
}
