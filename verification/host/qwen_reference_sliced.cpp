// Independent *bridge* path: standard llama_batch/llama_decode API on a host CPU.
// This is NOT an independent transformer implementation: llama.cpp internals are shared.
// This file intentionally does not include or link the Android native probe source.
#include "llama.h"
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
constexpr int kAudio = 156;
constexpr int kDim = 1024;
constexpr int kVocab = 151936;
constexpr int kCtx = 512;
constexpr std::array<llama_token, 9> kPrefix = {
    151644,8948,198,151645,198,151644,872,198,151669
};
constexpr std::array<llama_token, 6> kSuffix = {
    151670,151645,198,151644,77091,198
};
constexpr std::array<llama_token, 4> kTeacher = {11528,4128,102064,13806};

void require(bool ok, const char * why) { if (!ok) throw std::runtime_error(why); }

std::vector<float> read_features(const std::string & filename) {
    std::ifstream f(filename, std::ios::binary | std::ios::ate);
    require(bool(f), "FEATURE_MISSING");
    require(f.tellg() == std::streampos(kAudio * kDim * sizeof(float)), "FEATURE_SIZE_INVALID");
    f.seekg(0);
    std::vector<float> values(kAudio * kDim);
    f.read(reinterpret_cast<char *>(values.data()), values.size() * sizeof(float));
    require(bool(f), "FEATURE_READ_FAILED");
    for (float v : values) require(std::isfinite(v), "FEATURE_NONFINITE");
    return values;
}

// Caller-owned storage makes llama_batch lifetime and pointer ownership explicit.
struct Batch {
    std::vector<llama_token> tokens;
    std::vector<float> features;
    std::vector<llama_pos> positions;
    std::vector<int32_t> seq_counts;
    std::vector<llama_seq_id> seq_values;
    std::vector<llama_seq_id *> seq_ptrs;
    std::vector<int8_t> outputs;
    llama_batch batch{};

    Batch(size_t n, bool embedded, const float * audio, int start_position,
          bool request_last_logit, bool perturb_audio_axes) {
        require(n > 0 && n <= 256, "INVALID_BATCH_SIZE");
        if (embedded) {
            require(n == kAudio && audio, "INVALID_AUDIO_BATCH");
            features.assign(audio, audio + n * kDim);
            // M-RoPE embeddings require FOUR section-major position planes.
            // Never use llama_batch_init() here: its pos buffer may be only n_tokens.
            positions.resize(4 * n);
            for (size_t axis = 0; axis < 4; ++axis) {
                for (size_t i = 0; i < n; ++i) {
                    positions[axis*n + i] = start_position + int(i) +
                        ((perturb_audio_axes && axis > 0) ? 1 : 0);
                }
            }
        } else {
            tokens.resize(n);
            // Text tokens use the one-axis positions defined by llama.cpp's API.
            positions.resize(n);
            for (size_t i = 0; i < n; ++i) positions[i] = start_position + int(i);
        }
        seq_counts.assign(n, 1);
        seq_values.assign(n, 0);
        seq_ptrs.resize(n);
        outputs.assign(n, 0);
        for (size_t i = 0; i < n; ++i) seq_ptrs[i] = &seq_values[i];
        if (request_last_logit) outputs.back() = 1;
        batch.n_tokens = int32_t(n);
        batch.token = embedded ? nullptr : tokens.data();
        batch.embd = embedded ? features.data() : nullptr;
        batch.pos = positions.data();
        batch.n_seq_id = seq_counts.data();
        batch.seq_id = seq_ptrs.data();
        batch.logits = outputs.data();
    }
};

void decode(llama_context * ctx, Batch & b, const char * error) {
    if (llama_decode(ctx, b.batch) != 0) throw std::runtime_error(error);
}

void write_logits(llama_context * ctx, const std::filesystem::path & path) {
    const float * values = llama_get_logits_ith(ctx, -1);
    require(values != nullptr, "MISSING_LOGITS");
    for (int i = 0; i < kVocab; ++i) require(std::isfinite(values[i]), "NONFINITE_LOGITS");
#if defined(__BYTE_ORDER__) && __BYTE_ORDER__ != __ORDER_LITTLE_ENDIAN__
#error FP32LE exports require a little-endian host
#endif
    std::ofstream out(path, std::ios::binary | std::ios::trunc);
    require(bool(out), "CANNOT_CREATE_OUTPUT");
    out.write(reinterpret_cast<const char *>(values), kVocab * sizeof(float));
    out.flush();
    require(bool(out), "INCOMPLETE_OUTPUT");
}

void run_one(llama_context * ctx, const std::vector<float> & features,
             const std::filesystem::path & destination, bool axis_perturb) {
    int pos = 0;
    Batch before(kPrefix.size(), false, nullptr, pos, false, false);
    std::copy(kPrefix.begin(), kPrefix.end(), before.tokens.begin());
    decode(ctx, before, "PREFIX_DECODE_FAILED");
    pos += int(kPrefix.size());

    Batch audio(kAudio, true, features.data(), pos, false, axis_perturb);
    require(audio.positions.size() == kAudio * 4, "MROPE_POS_PLANE_SIZE_WRONG");
    require(audio.positions[0] == pos &&
            audio.positions[kAudio] == pos + (axis_perturb ? 1 : 0),
            "MROPE_POS_PLANE_LAYOUT_WRONG");
    decode(ctx, audio, "AUDIO_DECODE_FAILED");
    pos += kAudio;

    Batch after(kSuffix.size(), false, nullptr, pos, true, false);
    std::copy(kSuffix.begin(), kSuffix.end(), after.tokens.begin());
    decode(ctx, after, "SUFFIX_DECODE_FAILED");
    pos += int(kSuffix.size());
    write_logits(ctx, destination / "logits-step-00.f32le");

    // Fixed teacher-forced tokens: compare logits beyond prefill even if synthetic
    // greedy generation immediately reaches EOS. IDs are public synthetic constants.
    for (size_t i = 0; i < kTeacher.size(); ++i) {
        Batch next(1, false, nullptr, pos++, true, false);
        next.tokens[0] = kTeacher[i];
        decode(ctx, next, "TEACHER_FORCED_DECODE_FAILED");
        char name[64];
        std::snprintf(name, sizeof(name), "logits-step-%02zu.f32le", i + 1);
        write_logits(ctx, destination / name);
    }
}
} // namespace

int main(int argc, char ** argv) {
    if (argc != 5) {
        std::fprintf(stderr, "Usage: %s MODEL.gguf FEATURES.f32le OUTPUT_DIR linear4|audio-axis-shift\n", argv[0]);
        return 2;
    }
    const bool perturb = std::string(argv[4]) == "audio-axis-shift";
    if (!perturb && std::string(argv[4]) != "linear4") return 2;
    llama_backend_init();
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    try {
        auto features = read_features(argv[2]);
        std::filesystem::create_directories(argv[3]);
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        model = llama_model_load_from_file(argv[1], mp);
        require(model != nullptr, "MODEL_LOAD_FAILED");
        require(llama_model_n_embd_inp(model) == kDim, "MODEL_DIM_MISMATCH");
        require(llama_vocab_n_tokens(llama_model_get_vocab(model)) == kVocab, "MODEL_VOCAB_MISMATCH");
        require(llama_model_n_layer(model) == 28, "MODEL_LAYER_MISMATCH");
        const auto rope = llama_model_rope_type(model);
        require(rope == LLAMA_ROPE_TYPE_MROPE || rope == LLAMA_ROPE_TYPE_IMROPE,
                "MODEL_NOT_MROPE");
        auto cp = llama_context_default_params();
        cp.n_ctx = kCtx;
        cp.n_batch = 256;
        cp.n_ubatch = 256;
        cp.n_threads = 4;
        cp.n_threads_batch = 4;
        ctx = llama_init_from_model(model, cp);
        require(ctx != nullptr, "CONTEXT_INIT_FAILED");
        run_one(ctx, features, argv[3], perturb);
        llama_free(ctx);
        llama_model_free(model);
        llama_backend_free();
        std::printf("REFERENCE_BRIDGE_EXECUTED mode=%s steps=5 status=DIAGNOSTIC_ONLY\n", argv[4]);
        return 0;
    } catch (const std::exception & e) {
        std::fprintf(stderr, "REFERENCE_INCONCLUSIVE: %s\n", e.what());
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
        llama_backend_free();
        return 1;
    }
}
