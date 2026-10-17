#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include "transcribe.h"

namespace {

struct SessionHandle {
    transcribe_session *session = nullptr;
    std::atomic<bool> abort_requested{false};
};

void throw_illegal_state(JNIEnv *env, const std::string &message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message.c_str());
}

void throw_cancellation(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/util/concurrent/CancellationException");
    if (cls != nullptr) env->ThrowNew(cls, message);
}

SessionHandle *as_handle(jlong handle) {
    return reinterpret_cast<SessionHandle *>(static_cast<intptr_t>(handle));
}

transcribe_session *as_session(jlong handle) {
    auto *holder = as_handle(handle);
    return holder == nullptr ? nullptr : holder->session;
}

bool abort_callback(void *user_data) {
    auto *holder = static_cast<SessionHandle *>(user_data);
    return holder != nullptr && holder->abort_requested.load(std::memory_order_relaxed);
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
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeOpen(
    JNIEnv *env, jobject, jstring model_path, jint threads) {
    if (model_path == nullptr) {
        throw_illegal_state(env, "Model path is missing");
        return 0;
    }
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) return 0;

    transcribe_model_load_params load_params;
    transcribe_model_load_params_init(&load_params);
    load_params.backend = TRANSCRIBE_BACKEND_CPU;
    load_params.gpu_device = 0;

    transcribe_session_params session_params;
    transcribe_session_params_init(&session_params);
    session_params.n_threads = threads > 0 ? threads : 0;

    transcribe_session *session = nullptr;
    const transcribe_status status = transcribe_open(path, &load_params, &session_params, &session);
    env->ReleaseStringUTFChars(model_path, path);
    if (status != TRANSCRIBE_OK || session == nullptr) {
        throw_illegal_state(env, std::string("transcribe.cpp could not load model: ") + transcribe_status_string(status));
        return 0;
    }

    auto *holder = new SessionHandle();
    holder->session = session;
    transcribe_set_abort_callback(session, abort_callback, holder);
    return static_cast<jlong>(reinterpret_cast<intptr_t>(holder));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeCancel(
    JNIEnv *, jobject, jlong handle) {
    auto *holder = as_handle(handle);
    if (holder != nullptr) holder->abort_requested.store(true, std::memory_order_relaxed);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeTranscribe(
    JNIEnv *env, jobject, jlong handle, jfloatArray samples, jstring language_hint) {
    auto *holder = as_handle(handle);
    transcribe_session *session = holder == nullptr ? nullptr : holder->session;
    if (session == nullptr || samples == nullptr) {
        throw_illegal_state(env, "transcribe.cpp session or audio is missing");
        return nullptr;
    }
    const jsize n = env->GetArrayLength(samples);
    if (n <= 0) {
        throw_illegal_state(env, "Audio segment is empty");
        return nullptr;
    }
    // Copy once and release all JVM array access before starting long native inference.
    // GetFloatArrayElements may pin the Java heap for the entire model run.
    std::vector<jfloat> pcm(static_cast<size_t>(n));
    env->GetFloatArrayRegion(samples, 0, n, pcm.data());
    if (env->ExceptionCheck()) return nullptr;

    holder->abort_requested.store(false, std::memory_order_relaxed);
    transcribe_run_params params;
    transcribe_run_params_init(&params);
    params.task = TRANSCRIBE_TASK_TRANSCRIBE;
    // Ask each model family for the richest timestamp granularity it supports. The app consumes
    // segment rows for synchronized transcript playback and falls back to chunk timing if needed.
    params.timestamps = TRANSCRIBE_TIMESTAMPS_AUTO;
    std::string language_storage;
    if (language_hint != nullptr) {
        const char *language_chars = env->GetStringUTFChars(language_hint, nullptr);
        if (language_chars != nullptr) {
            language_storage.assign(language_chars);
            env->ReleaseStringUTFChars(language_hint, language_chars);
        }
    }
    params.language = (!language_storage.empty() && language_storage != "auto") ? language_storage.c_str() : nullptr;
    params.target_language = nullptr;

    const transcribe_status status = transcribe_run(session, pcm.data(), static_cast<int>(n), &params);
    if (status == TRANSCRIBE_ERR_ABORTED || holder->abort_requested.load(std::memory_order_relaxed)) {
        throw_cancellation(env, "transcribe.cpp transcription stopped");
        return nullptr;
    }
    if (status != TRANSCRIBE_OK) {
        throw_illegal_state(env, std::string("transcribe.cpp inference failed: ") + transcribe_status_string(status));
        return nullptr;
    }
    const char *text = transcribe_full_text(session);
    return env->NewStringUTF(text != nullptr ? text : "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeTimingJson(
    JNIEnv *env, jobject, jlong handle) {
    transcribe_session *session = as_session(handle);
    if (session == nullptr) return env->NewStringUTF("[]");

    struct TimingRow {
        int64_t start_ms;
        int64_t end_ms;
        std::string text;
    };

    std::vector<TimingRow> rows;
    const transcribe_timestamp_kind returned_kind = transcribe_returned_timestamp_kind(session);

    // Parakeet's AUTO timestamp mode returns TOKEN precision and also exposes a clean word table.
    // Prefer those real word boundaries so synchronized playback can build Omi-style short phrases
    // instead of highlighting Parakeet's single whole-clip native segment.
    if (returned_kind >= TRANSCRIBE_TIMESTAMPS_WORD) {
        const int n_words = transcribe_n_words(session);
        rows.reserve(static_cast<size_t>(n_words));
        for (int i = 0; i < n_words; ++i) {
            transcribe_word word;
            transcribe_word_init(&word);
            if (transcribe_get_word(session, i, &word) != TRANSCRIBE_OK || word.text == nullptr || word.text[0] == '\0') continue;
            rows.push_back(TimingRow{word.t0_ms, word.t1_ms, word.text});
        }
    }

    // Families such as Qwen3 expose segment timing only. Keep those native boundaries intact rather
    // than inventing word timing. This is also the fallback if a finer-grained table is unavailable.
    if (rows.empty()) {
        const int n_segments = transcribe_n_segments(session);
        rows.reserve(static_cast<size_t>(n_segments));
        for (int i = 0; i < n_segments; ++i) {
            transcribe_segment segment;
            transcribe_segment_init(&segment);
            if (transcribe_get_segment(session, i, &segment) != TRANSCRIBE_OK || segment.text == nullptr || segment.text[0] == '\0') continue;
            rows.push_back(TimingRow{segment.t0_ms, segment.t1_ms, segment.text});
        }
    }

    std::string json = "[";
    bool first = true;
    for (size_t i = 0; i < rows.size(); ++i) {
        auto &row = rows[i];
        if (row.start_ms < 0) continue;
        // A TDT token can legally have zero duration. Keep its word in the timeline by extending a
        // zero-width word to the next real word boundary (or one millisecond as a final fallback).
        if (row.end_ms <= row.start_ms) {
            const int64_t next_start = (i + 1 < rows.size()) ? rows[i + 1].start_ms : row.start_ms + 1;
            row.end_ms = next_start > row.start_ms ? next_start : row.start_ms + 1;
        }
        if (!first) json.push_back(',');
        first = false;
        json += "{\"startMs\":" + std::to_string(row.start_ms) + ",\"endMs\":" + std::to_string(row.end_ms) + ",\"text\":";
        append_json_string(json, row.text.c_str());
        json.push_back('}');
    }
    json.push_back(']');
    return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeTokenCount(
    JNIEnv *, jobject, jlong handle) {
    transcribe_session *session = as_session(handle);
    return session == nullptr ? 0 : static_cast<jint>(transcribe_n_tokens(session));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeDetectedLanguage(
    JNIEnv *env, jobject, jlong handle) {
    transcribe_session *session = as_session(handle);
    const char *language = session == nullptr ? "" : transcribe_detected_language(session);
    return env->NewStringUTF(language != nullptr ? language : "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeVersion(
    JNIEnv *env, jobject) {
    const char *version = transcribe_version();
    return env->NewStringUTF(version != nullptr ? version : "unknown");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_whisperrecorder_advanced_TranscribeCppBridge_nativeClose(
    JNIEnv *, jobject, jlong handle) {
    auto *holder = as_handle(handle);
    if (holder == nullptr) return;
    holder->abort_requested.store(true, std::memory_order_relaxed);
    if (holder->session != nullptr) transcribe_session_free(holder->session);
    holder->session = nullptr;
    delete holder;
}
