#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <atomic>
#include "whisper.h"

namespace {

struct WhisperHandle {
    whisper_context *ctx = nullptr;
    std::atomic<bool> abort_requested{false};
};

WhisperHandle *as_handle(jlong handle) {
    return reinterpret_cast<WhisperHandle *>(handle);
}

bool whisper_abort_callback(void *user_data) {
    auto *holder = static_cast<WhisperHandle *>(user_data);
    return holder != nullptr && holder->abort_requested.load(std::memory_order_relaxed);
}

void throw_cancellation(JNIEnv *env, const char *message) {
    jclass ex = env->FindClass("java/util/concurrent/CancellationException");
    if (ex) env->ThrowNew(ex, message);
}

void append_json_string(std::string &out, const char *value) {
    out.push_back('"');
    if (value != nullptr) {
        for (const unsigned char *p = reinterpret_cast<const unsigned char *>(value); *p; ++p) {
            switch (*p) {
                case '"': out += "\\\""; break;
                case '\\': out += "\\\\"; break;
                case '\b': out += "\\b"; break;
                case '\f': out += "\\f"; break;
                case '\n': out += "\\n"; break;
                case '\r': out += "\\r"; break;
                case '\t': out += "\\t"; break;
                default:
                    if (*p < 0x20) {
                        static const char hex[] = "0123456789abcdef";
                        out += "\\u00";
                        out.push_back(hex[(*p >> 4) & 0x0f]);
                        out.push_back(hex[*p & 0x0f]);
                    } else {
                        out.push_back(static_cast<char>(*p));
                    }
            }
        }
    }
    out.push_back('"');
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_createContext(
        JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (ctx == nullptr) return 0;
    auto *holder = new WhisperHandle();
    holder->ctx = ctx;
    return reinterpret_cast<jlong>(holder);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_freeContext(
        JNIEnv *, jobject, jlong handle) {
    auto *holder = as_handle(handle);
    if (holder == nullptr) return;
    holder->abort_requested.store(true, std::memory_order_relaxed);
    if (holder->ctx) whisper_free(holder->ctx);
    holder->ctx = nullptr;
    delete holder;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_cancel(
        JNIEnv *, jobject, jlong handle) {
    auto *holder = as_handle(handle);
    if (holder) holder->abort_requested.store(true, std::memory_order_relaxed);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_transcribe(
        JNIEnv *env, jobject, jlong handle, jfloatArray samplesArray, jstring language, jint threads) {
    auto *holder = as_handle(handle);
    auto *ctx = holder != nullptr ? holder->ctx : nullptr;
    if (!ctx) {
        jclass ex = env->FindClass("java/lang/IllegalStateException");
        if (ex) env->ThrowNew(ex, "Whisper model context is unavailable");
        return nullptr;
    }

    holder->abort_requested.store(false, std::memory_order_relaxed);
    const jsize n = env->GetArrayLength(samplesArray);
    std::vector<float> samples(static_cast<size_t>(n));
    env->GetFloatArrayRegion(samplesArray, 0, n, samples.data());

    const char *lang = env->GetStringUTFChars(language, nullptr);
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.no_context = true;
    // Keep segment timestamps and enable token-level timing so synchronized playback follows real
    // speech timing rather than proportionally dividing a long Whisper segment.
    params.no_timestamps = false;
    params.token_timestamps = true;
    params.single_segment = false;
    params.language = lang;
    params.n_threads = std::max(1, static_cast<int>(threads));
    params.abort_callback = whisper_abort_callback;
    params.abort_callback_user_data = holder;

    const int rc = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));
    env->ReleaseStringUTFChars(language, lang);
    if (rc != 0) {
        if (holder->abort_requested.load(std::memory_order_relaxed)) {
            throw_cancellation(env, "Whisper transcription stopped");
        } else {
            jclass ex = env->FindClass("java/lang/IllegalStateException");
            if (ex) {
                std::string message = "whisper_full failed with code " + std::to_string(rc);
                env->ThrowNew(ex, message.c_str());
            }
        }
        return nullptr;
    }

    std::string result;
    const int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text) result += text;
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_timingJson(
        JNIEnv *env, jobject, jlong handle) {
    auto *holder = as_handle(handle);
    auto *ctx = holder != nullptr ? holder->ctx : nullptr;
    if (!ctx) return env->NewStringUTF("[]");

    // Token timing is emitted as timed words. Whisper's BPE token text generally starts a new word
    // with leading whitespace; punctuation/subword pieces without leading whitespace are attached to
    // the current word. Special tokens live at/above EOT and are excluded.
    struct TimedWord {
        int64_t start_ms = -1;
        int64_t end_ms = -1;
        std::string text;
    };
    std::vector<TimedWord> words;
    const whisper_token eot = whisper_token_eot(ctx);
    const int segments = whisper_full_n_segments(ctx);

    for (int i = 0; i < segments; ++i) {
        TimedWord current;
        auto flush = [&]() {
            size_t first = current.text.find_first_not_of(" \t\r\n");
            size_t last = current.text.find_last_not_of(" \t\r\n");
            if (first != std::string::npos && last != std::string::npos && current.end_ms > current.start_ms) {
                current.text = current.text.substr(first, last - first + 1);
                words.push_back(current);
            }
            current = TimedWord{};
        };

        const int n_tokens = whisper_full_n_tokens(ctx, i);
        for (int j = 0; j < n_tokens; ++j) {
            const whisper_token id = whisper_full_get_token_id(ctx, i, j);
            if (id >= eot) continue;
            const char *piece_c = whisper_full_get_token_text(ctx, i, j);
            if (piece_c == nullptr || *piece_c == '\0') continue;
            const whisper_token_data data = whisper_full_get_token_data(ctx, i, j);
            if (data.t1 <= data.t0) continue;

            const std::string piece(piece_c);
            const bool starts_word = !piece.empty() &&
                (piece.front() == ' ' || piece.front() == '\t' || piece.front() == '\n' || piece.front() == '\r');
            if (starts_word && !current.text.empty()) flush();

            if (current.start_ms < 0) current.start_ms = data.t0 * 10;
            current.end_ms = std::max(current.end_ms, data.t1 * 10);
            current.text += piece;
        }
        flush();
    }

    // If a model/run yields no usable token timing, preserve Whisper's real segment timing rather
    // than inventing token positions. Kotlin will use these native segment spans as-is.
    if (words.empty()) {
        for (int i = 0; i < segments; ++i) {
            const char *text = whisper_full_get_segment_text(ctx, i);
            const int64_t t0 = whisper_full_get_segment_t0(ctx, i) * 10;
            const int64_t t1 = whisper_full_get_segment_t1(ctx, i) * 10;
            if (text == nullptr || *text == '\0' || t1 <= t0) continue;
            TimedWord row;
            row.start_ms = t0;
            row.end_ms = t1;
            row.text = text;
            words.push_back(std::move(row));
        }
    }

    std::string json = "[";
    bool first = true;
    for (const auto &word : words) {
        if (word.text.empty() || word.end_ms <= word.start_ms) continue;
        if (!first) json.push_back(',');
        first = false;
        json += "{\"startMs\":" + std::to_string(word.start_ms) + ",\"endMs\":" + std::to_string(word.end_ms) + ",\"text\":";
        append_json_string(json, word.text.c_str());
        json.push_back('}');
    }
    json.push_back(']');
    return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_transcribe_WhisperBridge_systemInfo(
        JNIEnv *env, jobject) {
    const char *info = whisper_print_system_info();
    return env->NewStringUTF(info ? info : "");
}
