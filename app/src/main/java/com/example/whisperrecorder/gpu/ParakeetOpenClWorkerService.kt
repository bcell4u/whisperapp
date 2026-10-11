package com.example.whisperrecorder.gpu

import android.app.Service
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SharedMemory
import com.example.whisperrecorder.advanced.ParakeetCppNativeBridge
import org.json.JSONObject
import java.io.File

/**
 * Production Parakeet-family OpenCL host for NVIDIA v3 and Omi Med Q8_0. The ggml model is cached for 120 s after release;
 * process death remains the hard reclamation boundary for vendor/OpenCL/native allocations.
 */
class ParakeetOpenClWorkerService : Service() {
    private val binder = object : IParakeetOpenClService.Stub() {
        override fun runtimeDiagnostics(): String = cacheSnapshot()

        override fun open(modelPath: String): Long {
            val file = File(modelPath)
            require(file.isFile && file.length() > 0L) { "Parakeet-family model is missing" }
            return acquireModel(file)
        }

        override fun transcribe(handle: Long, pcmSharedMemory: SharedMemory, sampleCount: Int): String {
            require(hasLease(handle)) { "Parakeet OpenCL model lease is not active" }
            val raw = ParakeetCppNativeBridge.nativeTranscribeShared(handle, pcmSharedMemory, sampleCount)
            return JSONObject()
                .put("resultJson", raw)
                .put("resolvedDevice", resolvedDevice)
                .put("nativeWallMs", ParakeetCppNativeBridge.nativeLastInferenceWallNs() / 1_000_000L)
                .put("nativeCpuMs", ParakeetCppNativeBridge.nativeLastInferenceCpuNs() / 1_000_000L)
                .toString()
        }

        override fun cancel(handle: Long) {
            if (hasLease(handle)) {
                // parakeet.cpp v0.5.0 has no in-flight cancellation hook. Kill only :opencl.
                Process.killProcess(Process.myPid())
            }
        }

        override fun close(handle: Long) { releaseModel(handle) }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) evictReleasedModel()
    }

    override fun onDestroy() {
        normalizeLeasesAfterServiceDestroy()
        super.onDestroy()
    }

    companion object {
        private const val MODEL_CACHE_IDLE_MS = 120_000L
        private val lock = Any()
        private val handler = Handler(Looper.getMainLooper())

        private data class ModelKey(val path: String, val bytes: Long, val modified: Long)
        private data class CachedModel(
            val key: ModelKey,
            val handle: Long,
            var leases: Int,
            var hits: Long
        )

        @Volatile private var cached: CachedModel? = null
        @Volatile private var resolvedDevice: String = "GPUOpenCL"

        private val idleEviction = Runnable {
            if (evictReleasedModel()) {
                // A normally-idle bound worker may remain resident. Once the model is gone,
                // end the private process so Android reclaims any remaining driver state.
                Process.killProcess(Process.myPid())
            }
        }

        private fun acquireModel(file: File): Long {
            val key = ModelKey(file.canonicalPath, file.length(), file.lastModified())
            synchronized(lock) {
                handler.removeCallbacks(idleEviction)
                cached?.let { existing ->
                    if (existing.key == key) {
                        existing.leases++
                        existing.hits++
                        return existing.handle
                    }
                    check(existing.leases == 0) { "Cannot replace active Parakeet-family OpenCL model" }
                    runCatching { ParakeetCppNativeBridge.nativeClose(existing.handle) }
                    cached = null
                }
                resolvedDevice = ParakeetCppNativeBridge.nativeResolveDevice("opencl")
                val handle = ParakeetCppNativeBridge.nativeOpen(file.absolutePath, resolvedDevice)
                check(handle != 0L) { "parakeet.cpp could not load model on $resolvedDevice" }
                cached = CachedModel(key, handle, leases = 1, hits = 0L)
                return handle
            }
        }

        private fun releaseModel(handle: Long) {
            synchronized(lock) {
                val existing = cached ?: return
                if (existing.handle != handle || existing.leases <= 0) return
                existing.leases--
                if (existing.leases == 0) {
                    handler.removeCallbacks(idleEviction)
                    handler.postDelayed(idleEviction, MODEL_CACHE_IDLE_MS)
                }
            }
        }

        private fun hasLease(handle: Long): Boolean = synchronized(lock) {
            cached?.let { it.handle == handle && it.leases > 0 } == true
        }

        private fun normalizeLeasesAfterServiceDestroy() {
            synchronized(lock) {
                val existing = cached ?: return
                if (existing.leases > 0) existing.leases = 0
                if (existing.leases == 0) {
                    handler.removeCallbacks(idleEviction)
                    handler.postDelayed(idleEviction, MODEL_CACHE_IDLE_MS)
                }
            }
        }

        private fun evictReleasedModel(): Boolean = synchronized(lock) {
            val existing = cached ?: return false
            if (existing.leases != 0) return false
            handler.removeCallbacks(idleEviction)
            cached = null
            runCatching { ParakeetCppNativeBridge.nativeClose(existing.handle) }
            true
        }

        private fun cacheSnapshot(): String = synchronized(lock) {
            cached?.let {
                "parakeet.cpp 0.5.0 OpenCL device=$resolvedDevice model=${File(it.key.path).name} leases=${it.leases} hits=${it.hits} pssKb=${Debug.getPss()} idleTimeoutMs=$MODEL_CACHE_IDLE_MS"
            } ?: "parakeet.cpp 0.5.0 OpenCL device=$resolvedDevice modelCache=empty pssKb=${Debug.getPss()} idleTimeoutMs=$MODEL_CACHE_IDLE_MS"
        }
    }
}
