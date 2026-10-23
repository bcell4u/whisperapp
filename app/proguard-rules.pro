# JNI entry points are resolved by fully qualified Java/Kotlin class and method names.
# Keep the native bridge objects stable; the rest of the app remains eligible for R8 shrinking.
-keep class com.example.whisperrecorder.transcribe.WhisperBridge { *; }
-keep class com.example.whisperrecorder.advanced.ParakeetCppNativeBridge { *; }

# ONNX Runtime is used by the isolated :npu ORT-QNN worker.
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# sherpa-onnx Kotlin/JNI passes configs and results through JNI reflection.
-keep class com.k2fsa.sherpa.onnx.** { *; }
