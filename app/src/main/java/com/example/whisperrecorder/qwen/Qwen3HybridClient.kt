package com.example.whisperrecorder.qwen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import android.os.SharedMemory
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class Qwen3HybridClient private constructor(private val app: Context) : AutoCloseable {
    private val signal = CountDownLatch(1)
    @Volatile private var remote: IQwen3HybridService? = null
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote = IQwen3HybridService.Stub.asInterface(service)
            signal.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { remote = null }
        override fun onBindingDied(name: ComponentName?) { remote = null; signal.countDown() }
        override fun onNullBinding(name: ComponentName?) { remote = null; signal.countDown() }
    }
    fun prepare(overlapSeconds: Int = 1): JSONObject {
        require(overlapSeconds in 0..1)
        val json = JSONObject().put("threads", 4).put("overlap_seconds", overlapSeconds).toString()
        return JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.prepare(json))
    }
    fun encode(pcm: SharedMemory, count: Int): JSONObject =
        JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.encode(pcm, count))
    fun finish(onDecoded: (completed: Int, total: Int) -> Unit): JSONObject {
        val callback = object : IQwen3ProgressCallback.Stub() {
            override fun onWindowDecoded(completed: Int, total: Int) {
                // AIDL callback arrives on a Binder pool thread; state consumer is thread-safe.
                runCatching { onDecoded(completed, total) }
            }
        }
        return JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.finish(callback))
    }
    fun cancelCurrent() { runCatching { remote?.cancelCurrent() } }
    override fun close() {
        runCatching { remote?.closeRuntime() }
        remote = null
        if (bound) runCatching { app.unbindService(connection) }
        bound = false
    }
    companion object {
        fun connect(context: Context): Qwen3HybridClient {
            check(Looper.myLooper() != Looper.getMainLooper()) { "Qwen worker may only start from background thread" }
            val result = Qwen3HybridClient(context.applicationContext)
            result.bound = result.app.bindService(Intent(result.app, Qwen3HybridWorkerService::class.java),
                result.connection, Context.BIND_AUTO_CREATE)
            if (!result.bound || !result.signal.await(20, TimeUnit.SECONDS) || result.remote == null) {
                result.close()
                error("Qwen NPU worker could not connect")
            }
            return result
        }
    }
}
