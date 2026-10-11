cmake_minimum_required(VERSION 3.22.1)
project(whisper_recorder)

include(FetchContent)
# Reuse pinned checkout trees between CI runs instead of cloning whisper/transcribe each run.
set(FETCHCONTENT_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../.native-deps" CACHE PATH "WhisperRecorder native dependency cache" FORCE)
set(FETCHCONTENT_UPDATES_DISCONNECTED ON CACHE BOOL "Do not re-fetch cached pinned sources" FORCE)
# Android performance: whisper.cpp's own Android example forces an optimized native build even
# inside a Debug APK. Without this, NDK Debug settings can make inference dramatically slower.
set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    # Modern ARM64 phones (including the target Galaxy S26) benefit from FP16 vector arithmetic.
    set(GGML_CPU_ARM_ARCH "armv8.2-a+fp16" CACHE STRING "" FORCE)
endif()

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    whisper
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG v1.8.3
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(whisper)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_include_directories(whisper_jni PRIVATE ${whisper_SOURCE_DIR}/include)
target_link_libraries(whisper_jni PRIVATE whisper android log)
target_compile_options(whisper_jni PRIVATE -O3 -DNDEBUG -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_options(whisper_jni PRIVATE "-Wl,--exclude-libs,ALL")
