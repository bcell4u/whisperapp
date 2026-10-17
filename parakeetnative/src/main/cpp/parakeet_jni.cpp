#include <jni.h>
#include <android/sharedmem_jni.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cstdlib>
#include <cstdint>
#include <algorithm>
#include <cctype>
#include <sstream>
#include <ctime>
#include <stdexcept>
#include <string>

#include "parakeet_capi.h"
#include "ggml-backend.h"

namespace {

thread_local int64_t wr_last_inference_wall_ns = 0;
thread_local int64_t wr_last_inference_cpu_ns = 0;

int64_t wr_clock_ns(clockid_t id) {
    timespec ts{};
    if (clock_gettime(id, &ts) != 0) return 0;
    return static_cast<int64_t>(ts.tv_sec) * 1000000000LL + ts.tv_nsec;
}
void throw_illegal_state(JNIEnv *env, const std::string &message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message.c_str());
}

parakeet_ctx *as_ctx(jlong handle) {
    return reinterpret_cast<parakeet_ctx *>(static_cast<intptr_t>(handle));
}

std::string lower_copy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });
    return value;
}

std::string available_devices() {
    std::ostringstream out;
    for (size_t i = 0; i < ggml_backend_dev_count(); ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        if (!dev) continue;
        if (out.tellp() > 0) out << ", ";
        const char *name = ggml_backend_dev_name(dev);
        out << (name ? name : "?");
    }
    return out.str();
}

std::string resolve_device(const std::string &requested) {
    const std::string want = lower_copy(requested);
    if (want == "cpu") return "cpu";

    for (size_t i = 0; i < ggml_backend_dev_count(); ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        if (!dev) continue;
        const char *name_ptr = ggml_backend_dev_name(dev);
        const std::string name = name_ptr ? name_ptr : "";
        const std::string haystack = lower_copy(name);
        const bool matches =
            (want == "opencl" && haystack.find("opencl") != std::string::npos) ||
            lower_copy(name) == want;
        if (!matches) continue;

        // Validate that ggml can actually initialize the requested device. This
        // prevents parakeet.cpp's normal "explicit device missing -> CPU" fallback
        // from silently falling back to CPU.
        ggml_backend_t probe = ggml_backend_dev_init(dev, nullptr);
        if (!probe) {
            throw std::runtime_error("ggml found " + name + " but could not initialize it");
        }
        ggml_backend_free(probe);
        return name;
    }

    throw std::runtime_error(
        "Requested ggml device '" + requested + "' is unavailable. Registered devices: " +
        available_devices());
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeResolveDevice(
    JNIEnv *env, jobject, jstring requested_backend) {
    if (requested_backend == nullptr) {
        throw_illegal_state(env, "Parakeet backend is missing");
        return nullptr;
    }
    const char *requested_chars = env->GetStringUTFChars(requested_backend, nullptr);
    if (requested_chars == nullptr) return nullptr;
    std::string requested(requested_chars);
    env->ReleaseStringUTFChars(requested_backend, requested_chars);
    try {
        const std::string resolved = resolve_device(requested);
        return env->NewStringUTF(resolved.c_str());
    } catch (const std::exception &e) {
        throw_illegal_state(env, e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeOpen(
    JNIEnv *env, jobject, jstring model_path, jstring device_name) {
    if (model_path == nullptr || device_name == nullptr) {
        throw_illegal_state(env, "Parakeet model path or resolved device is missing");
        return 0;
    }

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    const char *device = env->GetStringUTFChars(device_name, nullptr);
    if (path == nullptr || device == nullptr) {
        if (path != nullptr) env->ReleaseStringUTFChars(model_path, path);
        if (device != nullptr) env->ReleaseStringUTFChars(device_name, device);
        return 0;
    }

    // The device name has already been resolved against ggml's registry and
    // initialization-probed. Use the exact registered name for parakeet.cpp.
    setenv("PARAKEET_DEVICE", device, 1);
    parakeet_ctx *ctx = parakeet_capi_load(path);

    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(device_name, device);

    if (ctx == nullptr) {
        throw_illegal_state(env, "parakeet.cpp could not load the model on the requested device");
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<intptr_t>(ctx));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeTranscribeShared(
    JNIEnv *env, jobject, jlong handle, jobject shared_memory, jint sample_count) {
    parakeet_ctx *ctx = as_ctx(handle);
    if (ctx == nullptr || shared_memory == nullptr || sample_count <= 0) {
        throw_illegal_state(env, "parakeet.cpp backend or shared audio is missing");
        return nullptr;
    }
    const size_t byte_count = static_cast<size_t>(sample_count) * sizeof(float);
    const int fd = ASharedMemory_dupFromJava(env, shared_memory);
    if (fd < 0) {
        throw_illegal_state(env, "Could not duplicate SharedMemory descriptor");
        return nullptr;
    }
    const size_t region_size = ASharedMemory_getSize(fd);
    if (region_size < byte_count) {
        close(fd);
        throw_illegal_state(env, "SharedMemory PCM region is smaller than requested sample count");
        return nullptr;
    }
    void *mapping = mmap(nullptr, byte_count, PROT_READ, MAP_SHARED, fd, 0);
    close(fd);
    if (mapping == MAP_FAILED) {
        throw_illegal_state(env, "Could not map shared PCM into OpenCL worker");
        return nullptr;
    }
    const int clip_lengths[1] = {static_cast<int>(sample_count)};
    const int64_t wall_start_ns = wr_clock_ns(CLOCK_MONOTONIC);
    const int64_t cpu_start_ns = wr_clock_ns(CLOCK_THREAD_CPUTIME_ID);
    char *result = parakeet_capi_transcribe_pcm_batch_json(
        ctx, static_cast<const float *>(mapping), clip_lengths, 1, 16000, 0);
    const int64_t cpu_end_ns = wr_clock_ns(CLOCK_THREAD_CPUTIME_ID);
    const int64_t wall_end_ns = wr_clock_ns(CLOCK_MONOTONIC);
    wr_last_inference_wall_ns = std::max<int64_t>(0, wall_end_ns - wall_start_ns);
    wr_last_inference_cpu_ns = std::max<int64_t>(0, cpu_end_ns - cpu_start_ns);
    munmap(mapping, byte_count);
    if (result == nullptr) {
        const char *detail = parakeet_capi_last_error(ctx);
        throw_illegal_state(env, std::string("parakeet.cpp inference failed: ") +
                                   (detail != nullptr && *detail ? detail : "unknown error"));
        return nullptr;
    }
    jstring out = env->NewStringUTF(result);
    parakeet_capi_free_string(result);
    return out;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeClose(
    JNIEnv *, jobject, jlong handle) {
    parakeet_ctx *ctx = as_ctx(handle);
    if (ctx != nullptr) parakeet_capi_free(ctx);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeLastInferenceWallNs(
    JNIEnv *, jobject) {
    return static_cast<jlong>(wr_last_inference_wall_ns);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_whisperrecorder_advanced_ParakeetCppNativeBridge_nativeLastInferenceCpuNs(
    JNIEnv *, jobject) {
    return static_cast<jlong>(wr_last_inference_cpu_ns);
}
