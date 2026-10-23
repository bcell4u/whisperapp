// Independent host test of the Android-style EXTENDED mixed embedding batching API.
// This is NOT the Android program and does NOT independently implement transformer math.
// It deliberately matches the Android 6.6.7 prompt, position map, batch flags and GGUF.
// It is paired with qwen_reference_sliced.cpp (standard llama_decode API) on one host.
#include "llama.h"
#include <array>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
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
// Input-kind controls keep the prompt length and M-RoPE position map unchanged.
// "zero": replace original synthetic audio embeddings with all-zero FP32 embeddings.
// "text-filler": replace each of the 156 middle positions with ordinary text token 198.
// They are mathematical controls ONLY, not valid speech-recognition prompts.
void run(llama_context * ctx, const std::vector<float> & audio, const std::filesystem::path & folder,
         const std::string & mode, const std::string & input_kind, int greedy_max, const std::string & pos_mode) {
    const bool cut_after_prefix = mode == "three-stage" || mode == "audio-suffix";
    const bool cut_after_audio = mode == "three-stage" || mode == "prefix-audio";
    MixedBatch batch(ctx);
    int position = 0;
    auto set_position = [&](int idx) {
        require(idx >= 0, "CANNOT_APPEND_MIXED_ITEM");
        const int index = position++;
        // Host-only 6.6.27 M-RoPE axis attribution. All token indices remain
        // strictly increasing along every axis, preserving causal order.
        // Axis-specific probes are NOT official Qwen audio M-RoPE positions.
        llama_pos all_axes[4] = {index,index,index,index};
        const bool suffix = pos_mode.find("pos-suffix-gap11") == 0;
        const bool shift17 = pos_mode.find("pos-plus17") == 0;
        const bool shift1 = pos_mode == "pos-plus1";
        const int delta = suffix ? (index >= 165 ? 11 : 0) : (shift17 ? 17 : (shift1 ? 1 : 0));
        const auto axis_suffix = pos_mode.find("-axis");
        if (axis_suffix == std::string::npos) {
            for (auto & axis : all_axes) axis += delta;
        } else {
            const int axis = pos_mode.back() - '0';
            require(axis >= 0 && axis < 4, "INVALID_POSITION_AXIS");
            all_axes[axis] += delta;
        }
        require(llama_batch_ext_set_pos(batch.value, idx, all_axes), "MIXED_POSITION_FAILED");
        return idx;
    };
    auto add_token = [&](llama_token token) {
        return set_position(llama_batch_ext_add_token(batch.value, 0, token));
    };
    // 6.6.11: same extended API, positions, logits and model across four
    // grouping patterns. Processing preserves the shared KV state.
    // prefix-audio: [prefix+audio] [suffix]; audio-suffix: [prefix] [audio+suffix].
    for (const auto token : kPrefix) add_token(token);
    if (cut_after_prefix) {
        require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0,
                "CHUNKED_PREFIX_FAILED");
        llama_batch_ext_clear(batch.value);
    }
    const std::vector<float> zero_audio(input_kind == "zero" ? size_t(kAudio)*kDim : 0, 0.0f);
    for (int i = 0; i < kAudio; ++i) {
        if (input_kind == "text-filler") {
            // Token 198 is a well-defined ordinary newline token in this GGUF.
            // A text-only middle span isolates generic batching from embedding handling.
            add_token(198);
        } else {
            const float * source = input_kind == "zero" ? zero_audio.data() : audio.data();
            llama_embd value{source + static_cast<size_t>(i)*kDim, 1, kDim};
            set_position(llama_batch_ext_add_embd(batch.value, 0, value));
        }
    }
    if (cut_after_audio) {
        require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0,
                "CHUNKED_AUDIO_FAILED");
        llama_batch_ext_clear(batch.value);
    }
    int last = -1;
    for (const auto token : kSuffix) last = add_token(token);
    require(position == 171, "PROMPT_LENGTH_CHANGED");
    require(llama_batch_ext_set_output_logits(batch.value, last, true), "MIXED_LOGIT_FLAG_FAILED");
    require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0,
            cut_after_audio ? "CHUNKED_SUFFIX_FAILED" : "MIXED_PREFILL_FAILED");
    save_logits(ctx, folder / "logits-step-00.f32le");
    // 6.6.18: Only the synthetic host-control run uses greedy generation.
    // Stop on a model end marker: post-EOS tokens would not reflect a useful ASR continuation.
    if (greedy_max > 0) {
        std::ofstream decisions(folder / "greedy-decisions.json", std::ios::binary | std::ios::trunc);
        require(bool(decisions), "CANNOT_CREATE_GREEDY_DECISIONS");
        decisions << "{\"schema\":1,\"requested_max_tokens\":" << greedy_max << ",\"decisions\":[";
        bool stopped = false;
        for (int i = 0; i < greedy_max; ++i) {
            const float * logits = llama_get_logits_ith(ctx, -1);
            require(logits != nullptr, "GREEDY_LOGITS_MISSING");
            int best = -1;
            double largest = -INFINITY, second = -INFINITY;
            for (int id = 0; id < kVocab; ++id) {
                const double value = logits[id];
                require(std::isfinite(value), "GREEDY_LOGITS_NONFINITE");
                if (value > largest) { second = largest; largest = value; best = id; }
                else if (value > second) second = value;
            }
            require(best >= 0 && std::isfinite(second), "GREEDY_ARGMAX_INVALID");
            if (i) decisions << ',';
            decisions << "{\"token_id\":" << best << ",\"top2_margin\":" << (largest-second) << '}';
            if (best == 151645 || best == 151643) { stopped = true; break; }
            if (i + 1 == greedy_max) break;
            llama_batch_ext_clear(batch.value);
            last = add_token(static_cast<llama_token>(best));
            require(llama_batch_ext_set_output_logits(batch.value, last, true), "GREEDY_LOGIT_FLAG_FAILED");
            require(llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch.value) == 0, "GREEDY_DECODE_FAILED");
        }
        decisions << "],\"stopped_on_end_token\":" << (stopped ? "true" : "false") << "}\n";
        decisions.flush();
        require(bool(decisions), "GREEDY_DECISIONS_INCOMPLETE");
        return;
    }
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
    if (argc != 5 && argc != 7 && argc != 8 && argc != 9 && argc != 10) {
        std::fprintf(stderr, "Usage: %s Q8.gguf FEATURES.f32le OUTPUT_DIR MODE [UBATCH=64|128|160|192|256 THREADS=1|4 [original|zero|text-filler] [auto|flash-off|flash-on] [greedy-24|pos-linear|pos-plus1|pos-plus17|pos-suffix-gap11|pos-plus17-axis0..3|pos-suffix-gap11-axis0..3]]\n", argv[0]);
        return 2;
    }
    const std::string mode = argv[4];
    if (mode != "unified" && mode != "three-stage" && mode != "prefix-audio" && mode != "audio-suffix") return 2;
    const std::string input_kind = argc >= 8 ? argv[7] : "original";
    const std::string attention = argc >= 9 ? argv[8] : "auto";
    const std::string step_arg = argc == 10 ? argv[9] : "";
    const int greedy_max = step_arg == "greedy-24" ? 24 : 0;
    const std::string pos_mode = step_arg.rfind("pos-", 0) == 0 ? step_arg : "pos-linear";
    const bool axis_map = step_arg.size() > 5 &&
        (step_arg.rfind("pos-plus17-axis", 0) == 0 ||
         step_arg.rfind("pos-suffix-gap11-axis", 0) == 0) &&
        step_arg.back() >= '0' && step_arg.back() <= '3' &&
        step_arg.find("-axis") + 6 == step_arg.size();
    if (argc == 10 && greedy_max == 0 && step_arg != "pos-linear" && step_arg != "pos-plus1" &&
        step_arg != "pos-plus17" && step_arg != "pos-suffix-gap11" && !axis_map) return 2;
    if (attention != "auto" && attention != "flash-off" && attention != "flash-on") return 2;
    if (input_kind != "original" && input_kind != "zero" && input_kind != "text-filler") return 2;
    // Explicit allowlist, fail closed on malformed host-test configurations.
    int ubatch = 256;
    int threads = 4;
    if (argc >= 7) {
        const std::string ub = argv[5];
        const std::string th = argv[6];
        if (ub != "64" && ub != "128" && ub != "160" && ub != "192" && ub != "256") return 2;
        if (th != "1" && th != "4") return 2;
        ubatch = std::atoi(argv[5]);
        threads = std::atoi(argv[6]);
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
        ctx_params.n_ubatch = ubatch;
        ctx_params.n_threads = threads;
        ctx_params.n_threads_batch = threads;
        // 6.6.17 host-only attention toggle; existing Android decoder remains untouched.
        if (attention == "flash-off") ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_DISABLED;
        if (attention == "flash-on") ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_ENABLED;
        ctx = llama_init_from_model(model, ctx_params);
        require(ctx != nullptr, "CONTEXT_INIT_FAILED");
        run(ctx, features, argv[3], mode, input_kind, greedy_max, pos_mode);
        llama_free(ctx);
        llama_model_free(model);
        llama_backend_free();
        std::printf("HOST_EXTENDED_BATCH_EXECUTED mode=%s ubatch=%d threads=%d input_kind=%s attention_request=%s steps=%s pos_mode=%s model_math_independent=false\n", mode.c_str(), ubatch, threads, input_kind.c_str(), attention.c_str(), greedy_max ? "greedy_max_24" : "teacher_forced_5", pos_mode.c_str());
        return 0;
    } catch (const std::exception & e) {
        std::fprintf(stderr, "INCONCLUSIVE_MIXED_HOST: %s\n", e.what());
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
        llama_backend_free();
        return 1;
    }
}
