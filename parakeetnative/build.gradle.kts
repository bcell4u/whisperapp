plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.whisperrecorder.parakeetnative"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 29
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DGGML_OPENMP=OFF",
                    "-DGGML_NATIVE=OFF",
                    "-DGGML_CPU_ARM_ARCH=armv8.2-a+fp16"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            // The Khronos loader is only a link-time ABI surface. Android must resolve
            // the Qualcomm/vendor libOpenCL.so declared by the app manifest.
            excludes += setOf("**/libOpenCL.so", "**/libOpenCL.so.*")
        }
    }
}
