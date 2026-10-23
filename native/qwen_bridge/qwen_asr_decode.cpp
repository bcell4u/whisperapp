// Qwen3-ASR 0.6B embedded-feature decoder, Android ARM64 dotprod, llama.cpp v0.6.0.
// Single model load for the entire bounded manifest. Private token IPC is consumed
// within the app worker and never forwarded to diagnostics or application logs.
#include "llama.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <stdexcept>
#include <string>
#include <vector>
#include <chrono>
#include <sys/resource.h>
#include <sys/auxv.h>
#if defined(__aarch64__)
#include <asm/hwcap.h>
#endif
#ifndef AT_HWCAP
#define AT_HWCAP 16
#endif
// Android NDK's <asm/hwcap.h> already defines HWCAP_ASIMDDP as a macro.
// Declare a fallback only when a platform's headers omit the definition.
#ifndef HWCAP_ASIMDDP
#define HWCAP_ASIMDDP (1UL << 20)
#endif
static constexpr int AUDIO_FRAMES = 156, WIDTH = 1024, VOCAB_SIZE = 151936;
static constexpr llama_token PREFIX[] = {151644,8948,198,151645,198,151644,872,198,151669};
static constexpr llama_token SUFFIX[] = {151670,151645,198,151644,77091,198};
static std::vector<float> read_features(const std::string & file) {
    std::ifstream in(file, std::ios::binary | std::ios::ate);
    if (!in || in.tellg() != std::streampos(AUDIO_FRAMES*WIDTH*sizeof(float)))
        throw std::runtime_error("Feature shape invalid");
    std::vector<float> features(AUDIO_FRAMES*WIDTH);
    in.seekg(0);
    in.read(reinterpret_cast<char*>(features.data()), features.size()*sizeof(float));
    if (!in) throw std::runtime_error("Feature read failed");
    for (float value : features) if (!std::isfinite(value)) throw std::runtime_error("Invalid features");
    return features;
}
static int greedy(const float * p) {
    if (!p) throw std::runtime_error("Missing model logits");
    int best = 0;
    for (int i=0; i<VOCAB_SIZE; ++i) {
        if (!std::isfinite(p[i])) throw std::runtime_error("Nonfinite model logits");
        if (p[i] > p[best]) best=i;
    }
    return best;
}
int main(int argc, char **argv) {
    if (argc != 6 || std::string(argv[2]) != "--multi" ||
        std::string(argv[5]) != "--app-private-output" ||
        std::string(argv[4]) != "128") return 2;
    if (!(getauxval(AT_HWCAP) & HWCAP_ASIMDDP)) return 3;
    // Manifest accepts only worker-generated basenames and never absolute paths.
    std::string manifest(argv[3]);
    const auto slash = manifest.find_last_of('/');
    if (slash == std::string::npos) return 4;
    const std::string parent = manifest.substr(0, slash+1);
    std::ifstream list(manifest);
    std::vector<std::string> files;
    std::string name;
    while (std::getline(list,name)) {
        if (name.size() != 13 || name.rfind("feat",0) != 0 ||
            name.substr(7) != ".f32le" || files.size() >= 24 ||
            !std::all_of(name.begin()+4,name.begin()+7,[](char c){return c >= '0' && c <= '9';})) return 4;
        files.push_back(parent+name);
    }
    if (files.empty()) return 4;
    llama_backend_init();
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    llama_batch_ext * batch = nullptr;
    int exit_code=1;
    try {
        const auto load_begin = std::chrono::steady_clock::now();
        auto model_params=llama_model_default_params();
        model_params.n_gpu_layers=0;
        model=llama_model_load_from_file(argv[1],model_params);
        if (!model || llama_model_n_embd_inp(model) != WIDTH ||
            llama_model_n_layer(model) != 28 ||
            llama_vocab_n_tokens(llama_model_get_vocab(model)) != VOCAB_SIZE ||
            (llama_model_rope_type(model) != LLAMA_ROPE_TYPE_MROPE &&
             llama_model_rope_type(model) != LLAMA_ROPE_TYPE_IMROPE))
            throw std::runtime_error("Incompatible decoder model");
        auto ctx_params=llama_context_default_params();
        ctx_params.n_ctx=512; ctx_params.n_batch=256; ctx_params.n_ubatch=256;
        ctx_params.n_threads=4; ctx_params.n_threads_batch=4;
        context=llama_init_from_model(model,ctx_params);
        if (!context) throw std::runtime_error("Context allocation failed");
        batch=llama_batch_ext_init(context);
        if (!batch) throw std::runtime_error("Batch allocation failed");
        const auto load_ms=std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now()-load_begin).count();
        const auto decode_begin=std::chrono::steady_clock::now();
        int total_tokens=0, total_eos=0;
        for (size_t segment=0; segment<files.size(); ++segment) {
            if (segment) {
                llama_memory_clear(llama_get_memory(context),true);
                llama_batch_ext_clear(batch);
            }
            auto audio=read_features(files[segment]);
            int pos=0;
            auto add_token=[&](llama_token token) {
                int idx=llama_batch_ext_add_token(batch,0,token);
                const llama_pos p[4]={pos,pos,pos,pos}; ++pos;
                if (idx < 0 || !llama_batch_ext_set_pos(batch,idx,p))
                    throw std::runtime_error("Prompt token rejected");
                return idx;
            };
            for (auto token: PREFIX) add_token(token);
            for (int i=0; i<AUDIO_FRAMES; ++i) {
                llama_embd e{audio.data()+i*WIDTH,1,WIDTH};
                int idx=llama_batch_ext_add_embd(batch,0,e);
                const llama_pos p[4]={pos,pos,pos,pos}; ++pos;
                if (idx < 0 || !llama_batch_ext_set_pos(batch,idx,p))
                    throw std::runtime_error("Audio token rejected");
            }
            int last=-1;
            for (auto token: SUFFIX) last=add_token(token);
            if (!llama_batch_ext_set_output_logits(batch,last,true) ||
                llama_process(context,LLAMA_PROCESS_TYPE_DECODE,batch) != 0)
                throw std::runtime_error("Prefill failed");
            int token=greedy(llama_get_logits_ith(context,-1));
            std::vector<int> tokens; tokens.reserve(128);
            bool eos=false;
            for (int step=0; step<128; ++step) {
                tokens.push_back(token);
                if (token == 151643 || token == 151645) {eos=true; break;}
                llama_batch_ext_clear(batch);
                int idx=llama_batch_ext_add_token(batch,0,token);
                const llama_pos p[4]={pos,pos,pos,pos}; ++pos;
                if (idx<0 || !llama_batch_ext_set_pos(batch,idx,p) ||
                    !llama_batch_ext_set_output_logits(batch,idx,true) ||
                    llama_process(context,LLAMA_PROCESS_TYPE_DECODE,batch)!=0)
                    throw std::runtime_error("Generation failed");
                token=greedy(llama_get_logits_ith(context,-1));
            }
            total_tokens+=static_cast<int>(tokens.size());
            if (eos) ++total_eos;
            // Worker-private IPC only; never publish this stream or its contents in logs.
            std::printf("WR_APP_PRIVATE_TOKENS={\"segment\":%zu,\"ids\":[",segment);
            for (size_t i=0;i<tokens.size();++i)
                std::printf("%s%d", i ? "," : "",tokens[i]);
            std::printf("]}\n");
            std::printf("WR_BRIDGE_MULTI_METRIC={\"index\":%zu,\"generation_steps\":%zu,\"eos_reached\":%s}\n",
                        segment,tokens.size(),eos ? "true" : "false");
            std::fflush(stdout);
        }
        struct rusage usage{};
        const long rss=getrusage(RUSAGE_SELF,&usage) == 0 ? usage.ru_maxrss : 0;
        const auto elapsed=std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now()-decode_begin).count();
        std::printf("WR_BRIDGE_MULTI_SUMMARY={\"segments\":%zu,\"model_loads\":1,"
                    "\"model_load_ms\":%lld,\"decoder_segments_wall_ms\":%lld,"
                    "\"rss_peak_kb\":%ld,\"decode_threads\":4,\"batch_threads\":4}\n",
                    files.size(),static_cast<long long>(load_ms),static_cast<long long>(elapsed),rss);
        std::fflush(stdout);
        exit_code=total_eos == static_cast<int>(files.size()) ? 0 : 5;
    } catch (...) {
        // Avoid printing private model paths, features, transcript content or token IDs.
        exit_code=6;
    }
    if (batch) llama_batch_ext_free(batch);
    if (context) llama_free(context);
    if (model) llama_model_free(model);
    llama_backend_free();
    return exit_code;
}
