// One-time Android-native mixed-embedding execution probe for pinned llama.cpp v0.6.0.
// The QNN->GGUF integration is NOT numerically qualified or benchmarked.
// QNN audio features must come from an explicitly consented local test fixture.
#include "llama.h"
#include <algorithm>
#include <chrono>
#include <ctime>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <limits>
#include <stdexcept>
#include <vector>
#include <string>
#include <cctype>
#include <sys/resource.h>
#if defined(WR_CPU_ARM_DOTPROD)
// Optimized binary is ONLY safe on ARM64 hardware with ASIMDDP / dotprod.
// Check before initializing llama.cpp; do not trust the device name alone.
#include <sys/auxv.h>
#ifndef AT_HWCAP
#define AT_HWCAP 16
#endif
static constexpr unsigned long WR_HWCAP_ASIMDDP = 1UL << 20;
#if !defined(__aarch64__) || !defined(__ARM_FEATURE_DOTPROD)
#error WR_CPU_ARM_DOTPROD requires an Android ARM64 compiler with dotprod enabled
#endif
static constexpr char ARM_DOTPROD_PROTOCOL_ID[] = "WR_BRIDGE_PROBE_PROTOCOL_0496_DOTPROD_V1";
#endif

static constexpr int N_AUDIO = 156;
static constexpr int D = 1024;
static constexpr int VOCAB = 151936;
// Deterministic protocol marker: Windows checks ELF contents before ANY phone stage.
// This marker does not qualify M-RoPE or transcript correctness.
static constexpr char PROBE_PROTOCOL_ID[] = "WR_BRIDGE_PROBE_PROTOCOL_0464A_TRACE_V2";
static constexpr char MULTI_PROTOCOL_ID[] = "WR_BRIDGE_PROBE_PROTOCOL_0482_MULTI_V1";
static constexpr char CPU_TUNE_PROTOCOL_ID[] = "WR_BRIDGE_PROBE_PROTOCOL_0488_CPU_TUNE_V1";
static constexpr char DUAL_TUNE_PROTOCOL_ID[] = "WR_BRIDGE_PROBE_PROTOCOL_0492_DUAL_THREAD_V1";
static constexpr char APP_PRIVATE_PROTOCOL_ID[] = "WR_BRIDGE_APP_PRIVATE_PROTOCOL_0001";
// Must appear in the signed APK native ELF and the on-device --probe-info output.
static constexpr char PROMPT_ROLE_PROTOCOL_ID[] = "WR_QWEN_PROMPT_ROLE_8948_872_V1";
// 6.6.7: synthetic-only prefill logit checkpoint, never called from lecture transcription.
static constexpr char SYNTH_LOGITS_PROTOCOL_ID[] = "WR_QWEN_SYNTHETIC_LOGITS_V1";

static std::vector<float> read_audio(const char * name) {
    std::ifstream f(name, std::ios::binary | std::ios::ate);
    if (!f || f.tellg() != std::streampos(N_AUDIO * D * sizeof(float)))
        throw std::runtime_error("Expected exactly 156x1024 Float32 audio values");
    f.seekg(0);
    std::vector<float> x(N_AUDIO * D);
    f.read(reinterpret_cast<char *>(x.data()), x.size() * sizeof(float));
    if (!f) throw std::runtime_error("Audio feature read failed");
    for (float v : x) if (!std::isfinite(v)) throw std::runtime_error("Nonfinite audio feature");
    return x;
}

static int argmax(const float * v, int n) {
    if (!v) throw std::runtime_error("Missing logits: mixed batch output unsupported?");
    int best = 0;
    for (int i = 1; i < n; ++i) if (v[i] > v[best]) best = i;
    if (!std::isfinite(v[best])) throw std::runtime_error("Nonfinite logits");
    return best;
}

int main(int argc, char ** argv) {
#if defined(WR_CPU_ARM_DOTPROD)
    // A CPU feature mismatch must fail cleanly before any model/context work.
    // This is checked even for --probe-info in the phone-side preflight.
    if ((getauxval(AT_HWCAP) & WR_HWCAP_ASIMDDP) == 0) {
        std::fprintf(stderr, "INCONCLUSIVE: ARM_DOTPROD_CPU_NOT_SUPPORTED\n");
        return 4;
    }
#endif
    if (argc == 2 && std::string(argv[1]) == "--probe-info") {
#if defined(WR_CPU_ARM_DOTPROD)
        std::printf("WR_BRIDGE_PROBE_INFO={\"protocol\":\"%s\",\"llama_commit\":\"d812350\",\"position_kind\":\"provisional_linear_4d\",\"multi_protocol\":\"%s\",\"cpu_tune_protocol\":\"%s\",\"dual_tune_protocol\":\"%s\",\"cpu_target\":\"arm64_dotprod\",\"cpu_target_protocol\":\"%s\",\"app_private_protocol\":\"%s\",\"prompt_role_protocol\":\"%s\",\"synthetic_logits_protocol\":\"%s\"}\n", PROBE_PROTOCOL_ID, MULTI_PROTOCOL_ID, CPU_TUNE_PROTOCOL_ID, DUAL_TUNE_PROTOCOL_ID, ARM_DOTPROD_PROTOCOL_ID, APP_PRIVATE_PROTOCOL_ID, PROMPT_ROLE_PROTOCOL_ID, SYNTH_LOGITS_PROTOCOL_ID);
#else
        std::printf("WR_BRIDGE_PROBE_INFO={\"protocol\":\"%s\",\"llama_commit\":\"d812350\",\"position_kind\":\"provisional_linear_4d\",\"multi_protocol\":\"%s\",\"cpu_tune_protocol\":\"%s\",\"dual_tune_protocol\":\"%s\",\"cpu_target\":\"baseline_arm64\",\"prompt_role_protocol\":\"%s\",\"synthetic_logits_protocol\":\"%s\"}\n", PROBE_PROTOCOL_ID, MULTI_PROTOCOL_ID, CPU_TUNE_PROTOCOL_ID, DUAL_TUNE_PROTOCOL_ID, PROMPT_ROLE_PROTOCOL_ID, SYNTH_LOGITS_PROTOCOL_ID);
#endif
        return 0;
    }
    if (argc < 3 || argc > 9) {
        std::fprintf(stderr, "Usage: %s decoder-q8.gguf encoder-output.f32le [steps <= 128] [--private-trace]\n", argv[0]);
        return 2;
    }
    // --multi uses a bounded, strictly named manifest of feature filenames in
    // the same Builder-owned phone directory. Only one model/context load.
    const bool synthetic_logits = argc == 8 && std::string(argv[2]) == "--multi" &&
        std::string(argv[5]) == "--app-private-trace" &&
        std::string(argv[6]) == "--synthetic-logits" && std::string(argv[7]) == "fixture";
    const bool app_private = (argc == 6 || synthetic_logits) && std::string(argv[2]) == "--multi" &&
        std::string(argv[5]) == "--app-private-trace";
    const bool dual_tuned = argc == 9 && std::string(argv[2]) == "--multi" &&
        std::string(argv[5]) == "--decode-threads" && std::string(argv[7]) == "--batch-threads";
    const bool tuned = (argc == 7 || dual_tuned) && std::string(argv[2]) == "--multi" &&
        std::string(argv[5]) == "--decode-threads";
    const bool multi = (argc == 5 || tuned || app_private) && std::string(argv[2]) == "--multi";
    const int decode_threads = tuned ? std::atoi(argv[6]) : 4;
    const int batch_threads = dual_tuned ? std::atoi(argv[8]) : 4;
    const int n_steps = multi ? std::atoi(argv[4]) : argc >= 4 ? std::atoi(argv[3]) : 48;
    // Never emit private transcript-bearing token IDs during the multi benchmark.
    const bool private_trace = !multi && argc == 5 && std::string(argv[4]) == "--private-trace";
    if ((argc == 5 && !private_trace && !multi) ||
        (argc != 5 && argc != 6 && argc != 7 && argc != 8 && argc != 9 && argc > 4) ||
        (argc == 6 && !app_private) || (argc == 8 && !synthetic_logits) || (argc == 7 && !tuned) || (argc == 9 && !dual_tuned) ||
        (tuned && decode_threads != 2 && decode_threads != 3 && decode_threads != 4) ||
        (dual_tuned && batch_threads != 2 && batch_threads != 3 && batch_threads != 4)) return 2;
    if (n_steps <= 0 || n_steps > 128) return 2;
    llama_backend_init();
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    llama_batch_ext * batch = nullptr;
    int code = 1;
    try {
        std::vector<std::string> feature_paths;
        if (multi) {
            std::ifstream list(argv[3]);
            if (!list) throw std::runtime_error("MULTI_MANIFEST_UNAVAILABLE");
            std::string line;
            const std::string manifest(argv[3]);
            const size_t slash = manifest.find_last_of('/');
            const std::string directory = slash == std::string::npos ? "" : manifest.substr(0, slash + 1);
            while (std::getline(list, line)) {
                if (line.empty() || line.size() > 50 || feature_paths.size() >= 24 ||
                    line.find("..") != std::string::npos ||
                    !std::all_of(line.begin(), line.end(), [](unsigned char c) {
                        return std::isalnum(c) || c == '.' || c == '-' || c == '_';
                    })) throw std::runtime_error("MULTI_MANIFEST_INVALID");
                feature_paths.push_back(directory + line);
            }
            if (feature_paths.size() < 2) throw std::runtime_error("MULTI_SEGMENTS_MISSING");
        } else {
            feature_paths.emplace_back(argv[2]);
        }
        using clock = std::chrono::steady_clock;
        const auto model_load_begin = clock::now();
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        model = llama_model_load_from_file(argv[1], mp);
        if (!model) throw std::runtime_error("Cannot load Qwen GGUF model");
        if (llama_model_n_embd_inp(model) != D || llama_model_n_layer(model) != 28 ||
            llama_vocab_n_tokens(llama_model_get_vocab(model)) != VOCAB)
            throw std::runtime_error("Model metadata does not match the 0.6B decoder contract");
        if (llama_model_rope_type(model) != LLAMA_ROPE_TYPE_MROPE &&
            llama_model_rope_type(model) != LLAMA_ROPE_TYPE_IMROPE)
            throw std::runtime_error("GGUF does not expose Qwen3VL M-RoPE");
        auto cp = llama_context_default_params();
        cp.n_ctx = 512;
        cp.n_batch = 256;
        cp.n_ubatch = 256;
        cp.n_threads = decode_threads;
        cp.n_threads_batch = batch_threads;
        ctx = llama_init_from_model(model, cp);
        if (!ctx) throw std::runtime_error("Could not initialize decoder context");
        batch = llama_batch_ext_init(ctx);
        if (!batch) throw std::runtime_error("No extended batch API available");
        const auto model_load_ms = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now() - model_load_begin).count();
        const auto segments_begin = clock::now();
        const std::clock_t cpu_begin = std::clock();
        for (size_t segment = 0; segment < feature_paths.size(); ++segment) {
            if (segment) {
                llama_memory_clear(llama_get_memory(ctx), true);
                llama_batch_ext_clear(batch);
            }
            auto audio = read_audio(feature_paths[segment].c_str());
        // Official Qwen chat roles: system=8948, user=872. The older
        // 9125/882 literals encoded different words and were not valid roles.
        // This affects prompt construction only; the fast decoder is unchanged.
        constexpr llama_token prefix[] = {151644,8948,198,151645,198,151644,872,198,151669};
        constexpr llama_token suffix[] = {151670,151645,198,151644,77091,198};
        int pos = 0;
        // Four-axis values actually supplied to llama.cpp. This is still the
        // provisional mapping and is NOT certified as Qwen3-ASR M-RoPE.
        std::vector<int> supplied_positions;
        if (private_trace) supplied_positions.reserve(171 * 4);
        auto add_token = [&](int tok) {
            const int idx = llama_batch_ext_add_token(batch, 0, tok);
            if (idx < 0) throw std::runtime_error("Adding token failed");
            const llama_pos current = pos++;
            const llama_pos p[4] = {current,current,current,current};
            if (!llama_batch_ext_set_pos(batch, idx, p)) throw std::runtime_error("Token pos failed");
            if (private_trace) for (int axis=0;axis<4;++axis) supplied_positions.push_back(p[axis]);
            return idx;
        };
        for (llama_token token : prefix) add_token(token);
        for (int i=0;i<N_AUDIO;++i) {
            llama_embd e{audio.data() + i*D, 1, D};
            const int idx = llama_batch_ext_add_embd(batch, 0, e);
            if (idx < 0) throw std::runtime_error("Adding QNN embedding failed");
            // Qwen3VL uses M-RoPE positions. Linear [t,t,t,t] is only a provisional
            // candidate. Must be compared against upstream ASR mtmd and ONNX logits.
            const llama_pos p[4] = {pos,pos,pos,pos};
            if (!llama_batch_ext_set_pos(batch, idx, p)) throw std::runtime_error("Audio pos failed");
            if (private_trace) for (int axis=0;axis<4;++axis) supplied_positions.push_back(p[axis]);
            ++pos;
        }
        int last = -1;
        for (llama_token token : suffix) last = add_token(token);
        if (!llama_batch_ext_set_output_logits(batch, last, true))
            throw std::runtime_error("Cannot request last-prompt logits");
        auto started = clock::now();
        if (llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch) != 0)
            throw std::runtime_error("Mixed-batch prefill rejected");
        const auto prefill = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now()-started).count();
        const float * prefill_logits = llama_get_logits_ith(ctx, -1);
        if (synthetic_logits) {
            if (!prefill_logits) throw std::runtime_error("Missing synthetic prefill logits");
            // The Android worker invokes this option exclusively for the bundled
            // synthetic fixture. Keep the checkpoint in its own private scratch
            // folder. The normal lecture path does not write or export logits.
            for (int i = 0; i < VOCAB; ++i) {
                if (!std::isfinite(prefill_logits[i]))
                    throw std::runtime_error("Nonfinite synthetic prefill logits");
            }
#if defined(__BYTE_ORDER__) && __BYTE_ORDER__ != __ORDER_LITTLE_ENDIAN__
#error Synthetic checkpoint FP32LE requires a little-endian Android CPU
#endif
            const std::string first = feature_paths[segment];
            const size_t slash = first.find_last_of('/');
            if (slash == std::string::npos)
                throw std::runtime_error("Synthetic checkpoint directory missing");
            const std::string filename = first.substr(0, slash + 1) +
                "qwen-prefill-logits-" + std::to_string(segment) + ".f32le";
            std::ofstream out(filename, std::ios::binary | std::ios::trunc);
            if (!out) throw std::runtime_error("Cannot create synthetic logits");
            out.write(reinterpret_cast<const char *>(prefill_logits),
                      static_cast<std::streamsize>(VOCAB * sizeof(float)));
            out.flush();
            if (!out) throw std::runtime_error("Cannot write synthetic logits");
        }
        int token = argmax(prefill_logits, VOCAB);
        uint64_t token_hash = 14695981039346656037ULL; // non-cryptographic, diagnostics only
        std::vector<long long> step_times;
        int generated = 0;
        bool eos_reached = false;
        std::vector<int> generated_token_ids;
        if (private_trace || app_private) generated_token_ids.reserve(n_steps);
        const auto begin_steps = clock::now();
        for (int step=0;step<n_steps;step++) {
            // Hash token IDs without printing potentially identifying text or token values.
            for (int shift = 0; shift < 32; shift += 8) {
                token_hash ^= static_cast<uint8_t>(static_cast<uint32_t>(token) >> shift);
                token_hash *= 1099511628211ULL;
            }
            ++generated;
            if (private_trace || app_private) generated_token_ids.push_back(token);
            if (token == 151643 || token == 151645) { eos_reached = true; break; }
            llama_batch_ext_clear(batch);
            const int idx = llama_batch_ext_add_token(batch, 0, token);
            const llama_pos current = pos++;
            const llama_pos p[4] = {current,current,current,current};
            if (idx < 0 || !llama_batch_ext_set_pos(batch, idx, p) ||
                !llama_batch_ext_set_output_logits(batch, idx, true))
                throw std::runtime_error("Generation input rejected");
            auto t = clock::now();
            if (llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch) != 0)
                throw std::runtime_error("Decoder step failed");
            const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now()-t).count();
            step_times.push_back(ms);
            token = argmax(llama_get_logits_ith(ctx, -1), VOCAB);
        }
        std::sort(step_times.begin(), step_times.end());
        auto percentile = [&](double fraction) -> long long {
            if (step_times.empty()) return 0;
            return step_times[std::min(step_times.size()-1, (size_t)std::ceil(fraction*step_times.size())-1)];
        };
        const auto generation_ms = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now()-begin_steps).count();
        if (multi) {
            struct rusage resource{};
            const long long rss_kb = getrusage(RUSAGE_SELF, &resource) == 0 ?
                (long long)resource.ru_maxrss : 0;
            std::printf("WR_BRIDGE_MULTI_METRIC={\"index\":%zu,\"prefill_ms\":%lld,"
                        "\"generation_ms\":%lld,\"generation_steps\":%d,"
                        "\"eos_reached\":%s,\"rss_peak_kb\":%lld,"
                        "\"token_hash_fnv64\":\"%016llx\"}\n",
                        segment, (long long)prefill, (long long)generation_ms,
                        generated, eos_reached ? "true" : "false", rss_kb,
                        (unsigned long long)token_hash);
            if (app_private) {
                // PRIVATE: consumed strictly inside the isolated Android worker.
                // Never include these token IDs in automatic benchmark reports.
                std::printf("WR_APP_PRIVATE_TOKENS={\"segment\":%zu,\"ids\":[", segment);
                for (size_t i=0;i<generated_token_ids.size();++i) {
                    if (i) std::printf(",");
                    std::printf("%d",generated_token_ids[i]);
                }
                std::printf("]}\n");
            }
            std::fflush(stdout);
            continue;
        }
        // Only this bounded, numeric line is retained in the automatic diagnostic ZIP.
        std::printf("WR_BRIDGE_METRIC={\"status\":\"native_mixed_batch_executed_unqualified\","
                    "\"prompt_positions\":%d,\"prefill_ms\":%lld,\"generation_ms\":%lld,"
                    "\"generation_steps\":%d,\"eos_reached\":%s,"
                    "\"step_p50_ms\":%lld,\"step_p95_ms\":%lld,"
                    "\"token_hash_fnv64\":\"%016llx\",\"mrope_parity_verified\":false}\n",
                    pos, (long long)prefill, (long long)generation_ms, generated,
                    eos_reached ? "true" : "false", percentile(.50), percentile(.95),
                    (unsigned long long)token_hash);
        if (private_trace) {
            // PRIVATE output: the Windows runner consumes this in memory, compares
            // it to ONNX, and NEVER includes the tokens/positions in its ZIP.
            // Do not turn this flag on when running the probe unmediated.
            std::printf("WR_BRIDGE_PRIVATE_TRACE={\"schema\":1,\"position_kind\":\"provisional_linear_4d\",\"prompt_positions\":[");
            for (size_t i=0;i<supplied_positions.size();i+=4) {
                if (i) std::printf(",");
                std::printf("[%d,%d,%d,%d]", supplied_positions[i],supplied_positions[i+1],supplied_positions[i+2],supplied_positions[i+3]);
            }
            std::printf("],\"token_ids\":[");
            for (size_t i=0;i<generated_token_ids.size();++i) {
                if (i) std::printf(",");
                std::printf("%d",generated_token_ids[i]);
            }
            std::printf("]}\n");
        }
        }
        if (multi) {
            struct rusage resource{};
            const long long rss_kb = getrusage(RUSAGE_SELF, &resource) == 0 ?
                (long long)resource.ru_maxrss : 0;
            const auto total_ms = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now() - segments_begin).count();
            if (tuned || app_private) {
                const std::clock_t cpu_end = std::clock();
                const long long cpu_ms = cpu_begin != (std::clock_t)-1 && cpu_end != (std::clock_t)-1 && cpu_end >= cpu_begin
                    ? (long long)(1000.0 * double(cpu_end-cpu_begin) / double(CLOCKS_PER_SEC)) : 0;
                // Process CPU time sums work across threads. It is NOT electrical
                // energy, a calibrated power reading, or a thermal-throttle proof.
                std::printf("WR_BRIDGE_MULTI_SUMMARY={\"segments\":%zu,\"model_loads\":1,"
                            "\"model_load_ms\":%lld,\"decoder_segments_wall_ms\":%lld,"
                            "\"rss_peak_kb\":%lld,\"mrope_parity_verified\":false,"
                            "\"decode_threads\":%d,\"batch_threads\":%d,\"process_cpu_ms\":%lld}\n",
                            feature_paths.size(), (long long)model_load_ms,
                            (long long)total_ms, rss_kb, decode_threads, batch_threads, cpu_ms);
            } else {
                std::printf("WR_BRIDGE_MULTI_SUMMARY={\"segments\":%zu,\"model_loads\":1,"
                            "\"model_load_ms\":%lld,\"decoder_segments_wall_ms\":%lld,"
                            "\"rss_peak_kb\":%lld,\"mrope_parity_verified\":false}\n",
                            feature_paths.size(), (long long)model_load_ms,
                            (long long)total_ms, rss_kb);
            }
        }
        code = 0;
    } catch (const std::exception & ex) {
        std::fprintf(stderr,"INCONCLUSIVE: %s\n",ex.what());
    }
    if (batch) llama_batch_ext_free(batch);
    if (ctx) llama_free(ctx);
    if (model) llama_model_free(model);
    llama_backend_free();
    return code;
}
