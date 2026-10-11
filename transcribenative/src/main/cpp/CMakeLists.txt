cmake_minimum_required(VERSION 3.22.1)
project(whisper_recorder_transcribe_native LANGUAGES C CXX)

include(FetchContent)
# Reuse pinned checkout trees between CI runs instead of cloning whisper/transcribe each run.
set(FETCHCONTENT_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../.native-deps" CACHE PATH "WhisperRecorder native dependency cache" FORCE)
set(FETCHCONTENT_UPDATES_DISCONNECTED ON CACHE BOOL "Do not re-fetch cached pinned sources" FORCE)
set(FETCHCONTENT_QUIET OFF)

# Pinned transcribe.cpp v0.1.3. It provides Qwen3-ASR and Parakeet GGUF
# decoding behind one stable C API. Models remain download-only.
set(TRANSCRIBE_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_ENABLE_VALIDATION_HOOKS OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_METAL OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_VULKAN OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_CUDA OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_USE_OPENMP OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_USE_SYSTEM_BLAS OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_INSTALL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    transcribe_cpp
    GIT_REPOSITORY https://github.com/handy-computer/transcribe.cpp.git
    GIT_TAG v0.1.3
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(transcribe_cpp)

# transcribe.cpp v0.1.3 uses CMAKE_SOURCE_DIR for its public include path.
# When embedded through FetchContent, CMAKE_SOURCE_DIR is this Android module,
# so add the real fetched include directory explicitly before generation.
target_include_directories(transcribe PUBLIC ${transcribe_cpp_SOURCE_DIR}/include)

add_library(transcribe_cpp_jni SHARED transcribe_jni.cpp)
target_include_directories(transcribe_cpp_jni PRIVATE ${transcribe_cpp_SOURCE_DIR}/include)
target_link_libraries(transcribe_cpp_jni PRIVATE transcribe android log)
target_compile_features(transcribe_cpp_jni PRIVATE cxx_std_17)
target_compile_options(transcribe_cpp_jni PRIVATE -O3 -DNDEBUG -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_options(transcribe_cpp_jni PRIVATE "-Wl,--exclude-libs,ALL")
