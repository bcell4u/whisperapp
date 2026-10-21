import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appReleaseVersion = requireNotNull(versionProperties.getProperty("releaseVersion"))
val appVersionName = requireNotNull(versionProperties.getProperty("versionName"))
val appVersionCode = requireNotNull(versionProperties.getProperty("versionCode")).toInt()

// Distinct experimental signing; no production key is supplied to the lab CI.
val labStorePath = providers.environmentVariable("WR_LAB_STORE_FILE").orNull.orEmpty()
val labStorePass = providers.environmentVariable("WR_LAB_STORE_PASSWORD").orNull
val labKeyAlias = providers.environmentVariable("WR_LAB_KEY_ALIAS").orNull
val labKeyPass = providers.environmentVariable("WR_LAB_KEY_PASSWORD").orNull
val releaseStoreFile = providers.environmentVariable("WHISPER_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("WHISPER_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("WHISPER_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("WHISPER_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.example.whisperrecorder"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        resValue("string", "app_release_version", "v$appReleaseVersion")
        resValue("string", "app_version_name", appVersionName)
        applicationId = "com.example.whisperrecorder.qwengguflab"
        buildConfigField("boolean", "QWEN_GGUF_LAB", "true")
        minSdk = 29
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DGGML_OPENMP=OFF",
                    "-DGGML_CPU_ARM_ARCH=armv8.2-a+fp16"
                )
            }
        }
    }

    signingConfigs {
        if (labStorePath.isNotBlank()) {
            create("labDebug") {
                storeFile = file(labStorePath)
                storePassword = requireNotNull(labStorePass)
                keyAlias = requireNotNull(labKeyAlias)
                keyPassword = requireNotNull(labKeyPass)
                enableV1Signing = false
                enableV2Signing = true
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = false
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // Optional STABLE lab-only key provided by workflow secret; never production signing.
            if (labStorePath.isNotBlank()) signingConfig = signingConfigs.getByName("labDebug")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += setOf("**/libwrqwengguflab.so")
            // Native ASR modules share the Android C++ runtime. Keep one compatible copy.
            pickFirsts += setOf("**/libc++_shared.so")
            excludes += setOf("**/libOpenCL.so", "**/libOpenCL.so.*")
        }
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // ORT-QNN runtime aligned with the QAIRT 2.50 context build line.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.27.0")
    implementation("com.qualcomm.qti:onnxruntime-android-qnn:2.6.0")
    implementation("com.qualcomm.qti:qnn-runtime:2.50.0")
    implementation(project(":transcribenative"))
    implementation(project(":parakeetnative"))
    // Downloaded and SHA-256 verified by GitHub Actions. Model weights are never bundled.
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    testImplementation("junit:junit:4.13.2")
}
