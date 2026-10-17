# WhisperRecorder Android OpenCL discovery shim.
#
# Upstream ggml expects to link its OpenCL backend against an OpenCL library.
# Android's NDK does not provide one, so the transcribe native build creates the
# official Khronos ICD-loader target purely as the link-time ABI surface. The APK
# excludes that loader; at runtime Android resolves DT_NEEDED libOpenCL.so to the
# Qualcomm/vendor implementation declared through <uses-native-library>.
if(NOT WR_OPENCL_HEADERS_DIR OR NOT EXISTS "${WR_OPENCL_HEADERS_DIR}/CL/cl.h")
    set(OpenCL_FOUND FALSE)
    if(OpenCL_FIND_REQUIRED)
        message(FATAL_ERROR "WhisperRecorder OpenCL headers are unavailable")
    endif()
    return()
endif()

if(NOT TARGET OpenCL::OpenCL)
    set(OpenCL_FOUND FALSE)
    if(OpenCL_FIND_REQUIRED)
        message(FATAL_ERROR "WhisperRecorder Khronos OpenCL link target is unavailable")
    endif()
    return()
endif()

set(OpenCL_FOUND TRUE)
set(OpenCL_INCLUDE_DIR "${WR_OPENCL_HEADERS_DIR}")
set(OpenCL_INCLUDE_DIRS "${WR_OPENCL_HEADERS_DIR}")
set(OpenCL_LIBRARIES OpenCL::OpenCL)
set(OpenCL_VERSION_STRING "3.0")

message(STATUS "WhisperRecorder OpenCL: headers=${WR_OPENCL_HEADERS_DIR}; link=OpenCL::OpenCL; runtime=vendor libOpenCL.so")
