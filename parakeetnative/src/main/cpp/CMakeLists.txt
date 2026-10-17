cmake_minimum_required(VERSION 3.22.1)
project(whisper_recorder_parakeet_native LANGUAGES C CXX)

include(FetchContent)
set(FETCHCONTENT_QUIET OFF)
set(FETCHCONTENT_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../.native-deps" CACHE PATH "WhisperRecorder native dependency cache" FORCE)
set(FETCHCONTENT_UPDATES_DISCONNECTED ON CACHE BOOL "Do not refetch pinned native dependencies when cached" FORCE)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set(CMAKE_C_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
    set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
    message(STATUS "parakeetnative: using ccache at ${CCACHE_PROGRAM}")
endif()

# Android does not ship OpenCL headers/import libraries in the NDK. Build the
# Khronos ICD loader only as a link-time ABI surface. It is excluded from the
# APK; Android resolves libOpenCL.so to Qualcomm's vendor runtime on-device.
set(OPENCL_HEADERS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(OPENCL_HEADERS_BUILD_CXX_TESTS OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    opencl_headers
    GIT_REPOSITORY https://github.com/KhronosGroup/OpenCL-Headers.git
    GIT_TAG v2026.05.29
    GIT_SHALLOW TRUE
)
FetchContent_GetProperties(opencl_headers)
if(NOT opencl_headers_POPULATED)
    FetchContent_Populate(opencl_headers)
endif()
set(WR_OPENCL_HEADERS_DIR "${opencl_headers_SOURCE_DIR}" CACHE PATH "WhisperRecorder OpenCL headers" FORCE)

set(OPENCL_ICD_LOADER_HEADERS_DIR "${opencl_headers_SOURCE_DIR}" CACHE PATH "OpenCL ICD loader headers" FORCE)
set(OPENCL_ICD_LOADER_BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(OPENCL_ICD_LOADER_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(ENABLE_OPENCL_LAYERS OFF CACHE BOOL "" FORCE)
set(ENABLE_OPENCL_LAYERINFO OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    opencl_icd_loader
    GIT_REPOSITORY https://github.com/KhronosGroup/OpenCL-ICD-Loader.git
    GIT_TAG v2026.05.29
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(opencl_icd_loader)
if(NOT TARGET OpenCL OR NOT TARGET OpenCL::OpenCL)
    message(FATAL_ERROR "Khronos OpenCL link target was not created")
endif()
set_property(TARGET OpenCL PROPERTY VERSION)
set_property(TARGET OpenCL PROPERTY SOVERSION)
set_target_properties(OpenCL PROPERTIES OUTPUT_NAME OpenCL)

# Keep the production parakeet.cpp stack self-contained in one JNI library.
# NVIDIA v3 and Omi Med Q8_0 share this exact v0.5.0 + OpenCL runtime; the Omi
# adapter patch is metadata-gated and is inert for ordinary NVIDIA checkpoints.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_STATIC ON CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
list(PREPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/../../../../cmake")

# v6.0 production runtime is OpenCL-only. Vulkan was slower and stalled on-device;
# rejected K-quant experiments are not part of the production native path.
set(PARAKEET_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(PARAKEET_BUILD_CLI OFF CACHE BOOL "" FORCE)
set(PARAKEET_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(PARAKEET_SHARED OFF CACHE BOOL "" FORCE)
set(PARAKEET_GGML_CUDA OFF CACHE BOOL "" FORCE)
set(PARAKEET_GGML_METAL OFF CACHE BOOL "" FORCE)
set(PARAKEET_GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(PARAKEET_GGML_HIP OFF CACHE BOOL "" FORCE)
set(PARAKEET_WITH_CED OFF CACHE BOOL "" FORCE)
set(PARAKEET_WITH_VOICEDETECT OFF CACHE BOOL "" FORCE)
set(PARAKEET_VERSION "0.5.0" CACHE STRING "" FORCE)

set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL ON CACHE BOOL "" FORCE)
set(GGML_OPENCL_PROFILING OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL_EMBED_KERNELS ON CACHE BOOL "" FORCE)
set(GGML_OPENCL_USE_ADRENO_KERNELS ON CACHE BOOL "" FORCE)
set(GGML_OPENCL_TARGET_VERSION "300" CACHE STRING "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE ON CACHE BOOL "" FORCE)

FetchContent_Declare(
    parakeet_cpp
    GIT_REPOSITORY https://github.com/mudler/parakeet.cpp.git
    GIT_TAG v0.5.0
    GIT_SHALLOW FALSE
    GIT_SUBMODULES_RECURSE TRUE
    PATCH_COMMAND ${CMAKE_COMMAND} -E chdir <SOURCE_DIR> git apply --check --whitespace=nowarn ${CMAKE_CURRENT_SOURCE_DIR}/omi_adapter_runtime.patch
    COMMAND ${CMAKE_COMMAND} -E chdir <SOURCE_DIR> git apply --whitespace=nowarn ${CMAKE_CURRENT_SOURCE_DIR}/omi_adapter_runtime.patch
    COMMAND ${CMAKE_COMMAND} -E chdir <SOURCE_DIR> bash scripts/apply_ggml_patches.sh
)
FetchContent_MakeAvailable(parakeet_cpp)

target_include_directories(parakeet PRIVATE ${parakeet_cpp_SOURCE_DIR}/third_party)

add_library(parakeet_jni SHARED parakeet_jni.cpp)
target_include_directories(parakeet_jni PRIVATE
    ${parakeet_cpp_SOURCE_DIR}/include
    ${parakeet_cpp_SOURCE_DIR}/src
)
find_library(ANDROID_LOG_LIB log REQUIRED)
find_library(ANDROID_NATIVE_LIB android REQUIRED)
target_link_libraries(parakeet_jni PRIVATE
    parakeet
    OpenCL::OpenCL
    ${ANDROID_NATIVE_LIB}
    ${ANDROID_LOG_LIB}
)
target_compile_features(parakeet_jni PRIVATE cxx_std_17)
target_compile_options(parakeet_jni PRIVATE
    -O3 -DNDEBUG -ffunction-sections -fdata-sections
    -fvisibility=hidden -fvisibility-inlines-hidden
)
target_link_options(parakeet_jni PRIVATE "-Wl,--gc-sections" "-Wl,--exclude-libs,ALL")
