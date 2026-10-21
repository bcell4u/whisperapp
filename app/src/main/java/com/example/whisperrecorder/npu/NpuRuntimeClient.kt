package com.example.whisperrecorder.npu

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.SharedMemory
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class NpuRuntimeClient private constructor(private val context: Context) : AutoCloseable {
    @Volatile private var remote: INpuRuntimeService? = null
    @Volatile private var bound = false
    @Volatile private var closed = false
    private val connected = CountDownLatch(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote = INpuRuntimeService.Stub.asInterface(service)
            connected.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { remote = null }
        override fun onBindingDied(name: ComponentName?) { remote = null; connected.countDown() }
        override fun onNullBinding(name: ComponentName?) { remote = null; connected.countDown() }
    }

    fun probeOmiContexts(includeEncoder: Boolean): JSONObject =
        JSONObject(call("probeOmiContexts") { it.probeOmiContexts(includeEncoder) })
    fun bootstrap(): JSONObject = JSONObject(call("bootstrap") { it.bootstrap() })
    fun prepareOmiRuntime(): JSONObject = JSONObject(call("prepareOmiRuntime") { it.prepareOmiRuntime() })
    fun transcribeOmiChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): JSONObject =
        JSONObject(call("transcribeOmiChunk") { it.transcribeOmiChunk(pcm, sampleCount, audioDurationMs) })
    fun prepareRuntime(): JSONObject = JSONObject(call("prepareRuntime") { it.prepareRuntime() })
    fun transcribeChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): JSONObject =
        JSONObject(call("transcribeChunk") { it.transcribeChunk(pcm, sampleCount, audioDurationMs) })
    fun prepareChunk(pcm: SharedMemory, sampleCount: Int, audioDurationMs: Long): JSONObject =
        JSONObject(call("prepareChunk") { it.prepareChunk(pcm, sampleCount, audioDurationMs) })
    fun transcribePreparedChunk(preparedId: Long): JSONObject =
        JSONObject(call("transcribePreparedChunk") { it.transcribePreparedChunk(preparedId) })
    fun releasePreparedChunk(preparedId: Long) { remote?.let { runCatching { it.releasePreparedChunk(preparedId) } } }
    fun cancelCurrent() { remote?.let { runCatching { it.cancelCurrent() } } }
    fun closeRuntime() { remote?.let { runCatching { it.closeRuntime() } } }

    private inline fun <T> call(operation: String, block: (INpuRuntimeService) -> T): T {
        check(!closed) { "NPU runtime client is closed" }
        val service = remote ?: throw NpuRuntimeException("NPU worker unavailable during $operation")
        return try { block(service) }
        catch (e: DeadObjectException) {
            remote = null
            throw NpuRuntimeException("NPU worker process died during $operation", e)
        } catch (e: RemoteException) {
            remote = null
            throw NpuRuntimeException("NPU IPC failed during $operation: ${e.message ?: e::class.java.simpleName}", e)
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
        private const val BIND_TIMEOUT_SECONDS = 15L
        fun connect(context: Context): NpuRuntimeClient {
            check(Looper.myLooper() != Looper.getMainLooper()) { "NPU worker must be connected off the main thread" }
            val client = NpuRuntimeClient(context.applicationContext)
            client.bound = client.context.bindService(
                Intent(client.context, NpuRuntimeWorkerService::class.java),
                client.connection,
                Context.BIND_AUTO_CREATE
            )
            if (!client.bound || !client.connected.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS) || client.remote == null) {
                client.close()
                throw NpuRuntimeException("NPU worker did not become ready within ${BIND_TIMEOUT_SECONDS}s")
            }
            return client
        }
    }
}

internal class NpuRuntimeException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
