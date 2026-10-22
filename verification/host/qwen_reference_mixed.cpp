// Independent host test of the Android-style EXTENDED mixed embedding batching API.
// This is NOT the Android program and does NOT independently implement transformer math.
// It deliberately matches the Android 6.6.7 prompt, position map, batch flags and GGUF.
// It is paired with qwen_reference_sliced.cpp (standard llama_decode API) on one host.
#include "llama.h"
#include <array>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
constexpr int kAudio = 156;
constexpr int kDim = 1024;
constexpr int kVocab = 151936;
constexpr std::array<llama_token, 9> kPrefix = {151644,8948,198,151645,198,151644,872,198,151669};
constexpr std::array<llama_token, 6> kSuffix = {151670,151645,198,151644,77091,198};
constexpr std::array<llama_token, 4> kTeacher = {11528,4128,102064,13806};

void require(bool ok, const char * why) { if (!ok) throw std::runtime_error(why); }
std::vector<float> load_features(const char * filename) {
    std::ifstream in(filename, std::ios::binary | std::ios::ate);
    require(bool(in), "FEATURE_MISSING");
    require(in.tellg() == std::streampos(kAudio*kDim*sizeof(float)), "FEATURE_LENGTH_INVALID");
    in.seekg(0);
    std::vector<float> v(kAudio*kDim);
    in.read(reinterpret_cast<char *>(v.data()), v.size()*sizeof(float));
    require(bool(in), "FEATURE_READ_ERROR");
    for (const float x : v) require(std::isfinite(x), "FEATURE_NONFINITE");
    return v;
}
void save_logits(llama_context * ctx, const std::filesystem::path & filename) {
    const float * data = llama_get_logits_ith(ctx, -1);
    require(data != nullptr, "MIXED_LOGITS_MISSING");
    for (int i = 0; i < kVocab; ++i) require(std::isfinite(data[i]), "MIXED_LOGITS_NONFINITE");
#if defined(__BYTE_ORDER__) && __BYTE_ORDER__ != __ORDER_LITTLE_ENDIAN__
#error Verification checkpoint requires little-endian FP32LE
#endif
    std::ofstream out(filename, std::ios::binary | std::ios::trunc);
    require(bool(out), "CANNOT_OPEN_CHECKPOINT");
    out.write(reinterpret_cast<const char *>(data), kVocab*sizeof(float));
    out.flush();
    require(bool(out), "INCOMPLETE_CHECKPOINT");
}
struct MixedBatch {
    llama_batch_ext * value;
    explicit MixedBatch(llama_context * ctx) : value(llama_batch_ext_init(ctx)) {
        require(value != nullptr, "EXTENDED_API_MISSING");
    }
    ~MixedBatch() { if (value) llama_batch_ext_free(value); }
    MixedBatch(const MixedBatch &) = delete;
    MixedBatch & operator=(const MixedBatch &) = delete;
};
void run(llama_context * ctx, const std::vector<float> & audio, const std::filesystem::path & folder) {
    MixedBatch batch(ctx);
    int position = 0;
    auto set_position = [&](int idx) {
        require(idx >= 0, "CANNOT_APPEND_MIXED_ITEM");
        const llama_pos current = position++;
        const llama_pos all_axes[4] = {current,current,current,current};
        require(llama_batch_ext_set_pos(batch.value, idx, all_axes), "MIXED_POSITION_FAILED");
        return idx;
    };
    auto add_token = [&](llama_token token) {
        return set_position(llama_batch_ext_add_token(batch.value, 0, token));
    };
    for (const auto token : kPrefix) add_token(token);
    for (int i = 0; i < kAudio; ++i) {
        llama_embd value{audio.data() + static_cast<size_t>(i)*kDim, 1, kDim};
        set_position(llama_batch_ext_add_embd(batch.value, 0, value));
    }
    int last = -1;
    for (const auto token : kSuffix) last = add_token(token);
    require(position == 171, "PROMPT_LENGTH_CHANGED");
    require(llama_batch_ext_set_output_logits(batch.value, last, true), "MIXED_LOGIT_FLAG_FAILED");
    require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0, "MIXED_PREFILL_FAILED");
    save_logits(ctx, folder / "logits-step-00.f32le");
    for (size_t i = 0; i < kTeacher.size(); ++i) {
        llama_batch_ext_clear(batch.value);
        last = add_token(kTeacher[i]);
        require(llama_batch_ext_set_output_logits(batch.value, last, true), "TEACHER_LOGIT_FLAG_FAILED");
        require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0, "TEACHER_STEP_FAILED");
        char name[64];
        std::snprintf(name, sizeof(name), "logits-step-%02zu.f32le", i+1);
        save_logits(ctx, folder / name);
    }
}
} // namespace

int main(int argc, char ** argv) {
    if (argc != 4) {
        std::fprintf(stderr, "Usage: %s Q8.gguf SYNTHETIC_FEATURES.f32le OUTPUT_DIR\n", argv[0]);
        return 2;
    }
    llama_backend_init();
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    try {
        const auto features = load_features(argv[2]);
        std::filesystem::create_directories(argv[3]);
        auto params = llama_model_default_params();
        params.n_gpu_layers = 0;
        model = llama_model_load_from_file(argv[1], params);
        require(model != nullptr, "MODEL_MISSING");
        require(llama_model_n_embd_inp(model) == kDim, "MODEL_DIM_WRONG");
        require(llama_model_n_layer(model) == 28, "MODEL_LAYER_WRONG");
        require(llama_vocab_n_tokens(llama_model_get_vocab(model)) == kVocab, "MODEL_VOCAB_WRONG");
        auto rope = llama_model_rope_type(model);
        require(rope == LLAMA_ROPE_TYPE_MROPE || rope == LLAMA_ROPE_TYPE_IMROPE, "NOT_MROPE_MODEL");
        auto ctx_params = llama_context_default_params();
        ctx_params.n_ctx = 512;
        ctx_params.n_batch = 256;
        ctx_params.n_ubatch = 256;
        ctx_params.n_threads = 4;
        ctx_params.n_threads_batch = 4;
        ctx = llama_init_from_model(model, ctx_params);
        require(ctx != nullptr, "CONTEXT_INIT_FAILED");
        run(ctx, features, argv[3]);
        llama_free(ctx);
        llama_model_free(model);
        llama_backend_free();
        std::puts("HOST_EXTENDED_MIXED_BATCH_EXECUTED steps=5 model_math_independent=false");
        return 0;
    } catch (const std::exception & e) {
        std::fprintf(stderr, "INCONCLUSIVE_MIXED_HOST: %s\n", e.what());
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
        llama_backend_free();
        return 1;
    }
}
