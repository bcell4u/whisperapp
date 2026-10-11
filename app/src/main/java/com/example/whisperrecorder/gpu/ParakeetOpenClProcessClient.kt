package com.example.whisperrecorder.gpu

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.SharedMemory
import android.os.SystemClock
import android.system.OsConstants
import org.json.JSONObject
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal data class ParakeetOpenClRecognition(
    val resultJson: String,
    val resolvedDevice: String,
    val nativeWallMs: Long,
    val nativeCpuMs: Long,
    val sharedMemoryPrepMs: Long,
    val ipcRoundTripMs: Long,
    val pcmBytes: Int,
    val readOnlyProtectionApplied: Boolean
)

/** Synchronous SharedMemory client used only from TranscriptionService's IO worker. */
internal class ParakeetOpenClProcessClient private constructor(private val context: Context) : AutoCloseable {
    @Volatile private var remote: IParakeetOpenClService? = null
    @Volatile private var bound = false
    @Volatile private var closed = false
    private val connected = CountDownLatch(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote = IParakeetOpenClService.Stub.asInterface(service)
            connected.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { remote = null }
        override fun onBindingDied(name: ComponentName?) { remote = null; connected.countDown() }
        override fun onNullBinding(name: ComponentName?) { remote = null; connected.countDown() }
    }

    fun runtimeDiagnostics(): String = call("runtimeDiagnostics") { it.runtimeDiagnostics() }
    fun open(modelPath: String): Long = call("open") { it.open(modelPath) }

    fun transcribe(handle: Long, samples: FloatArray): ParakeetOpenClRecognition {
        require(samples.isNotEmpty()) { "Audio segment is empty" }
        val byteCount = Math.multiplyExact(samples.size, Float.SIZE_BYTES)
        val prepStarted = SystemClock.elapsedRealtimeNanos()
        return SharedMemory.create("parakeet-pcm", byteCount).use { shared ->
            // Keep the SharedMemory lifetime around both mapping and binder IPC. A failed
            // mapReadWrite() previously skipped close(), leaking a native file descriptor.
            val mapped = shared.mapReadWrite()
            try {
                mapped.order(ByteOrder.nativeOrder()).asFloatBuffer().put(samples)
            } finally {
                SharedMemory.unmap(mapped)
            }
            val readOnly = runCatching { shared.setProtect(OsConstants.PROT_READ) }.getOrDefault(false)
            val prepMs = elapsedMs(prepStarted)
            try {
                val callStarted = SystemClock.elapsedRealtimeNanos()
                val envelope = JSONObject(call("transcribe") { it.transcribe(handle, shared, samples.size) })
                ParakeetOpenClRecognition(
                    resultJson = envelope.getString("resultJson"),
                    resolvedDevice = envelope.optString("resolvedDevice", "GPUOpenCL"),
                    nativeWallMs = envelope.optLong("nativeWallMs", -1L),
                    nativeCpuMs = envelope.optLong("nativeCpuMs", -1L),
                    sharedMemoryPrepMs = prepMs,
                    ipcRoundTripMs = elapsedMs(callStarted),
                    pcmBytes = byteCount,
                    readOnlyProtectionApplied = readOnly
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw ParakeetOpenClWorkerException(
                    "Parakeet OpenCL transcribe failed sampleCount=${samples.size} pcmBytes=$byteCount: ${t.message ?: t::class.java.simpleName}", t
                )
            }
        }
    }

    fun cancel(handle: Long) {
        val service = remote ?: return
        try { service.cancel(handle) } catch (_: RemoteException) { remote = null }
    }

    fun closeSession(handle: Long) {
        val service = remote ?: return
        try { service.close(handle) } catch (_: RemoteException) { remote = null }
    }

    private inline fun <T> call(operation: String, block: (IParakeetOpenClService) -> T): T {
        check(!closed) { "Parakeet OpenCL worker client is closed" }
        val service = remote ?: throw ParakeetOpenClWorkerException("Parakeet OpenCL worker unavailable during $operation")
        return try {
            block(service)
        } catch (e: DeadObjectException) {
            remote = null
            throw ParakeetOpenClWorkerException("Parakeet OpenCL worker process died during $operation", e)
        } catch (e: RemoteException) {
            remote = null
            throw ParakeetOpenClWorkerException("Parakeet OpenCL IPC failed during $operation: ${e.message ?: e::class.java.simpleName}", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        remote = null
        if (bound) runCatching { context.unbindService(connection) }
        bound = false
    }

    companion object {
        private const val BIND_TIMEOUT_SECONDS = 10L
        fun connect(context: Context): ParakeetOpenClProcessClient {
            check(Looper.myLooper() != Looper.getMainLooper()) { "OpenCL worker must be connected from a background thread" }
            val client = ParakeetOpenClProcessClient(context.applicationContext)
            val intent = Intent(client.context, ParakeetOpenClWorkerService::class.java)
            client.bound = client.context.bindService(intent, client.connection, Context.BIND_AUTO_CREATE)
            if (!client.bound || !client.connected.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS) || client.remote == null) {
                client.close()
                throw ParakeetOpenClWorkerException("OpenCL worker did not become ready within ${BIND_TIMEOUT_SECONDS}s")
            }
            return client
        }

        private fun elapsedMs(startNs: Long): Long = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L
    }
}

internal class ParakeetOpenClWorkerException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
