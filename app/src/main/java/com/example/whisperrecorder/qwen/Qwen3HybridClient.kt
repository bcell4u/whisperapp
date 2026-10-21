package com.example.whisperrecorder.qwen

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
import com.example.whisperrecorder.npu.QwenDecoderRunConfig
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
    fun prepareGgufLab(config: QwenDecoderRunConfig, overlapSeconds: Int = 0): JSONObject {
        require(overlapSeconds in 0..1)
        val json = JSONObject(config.toJson()).put("overlap_seconds", overlapSeconds).toString()
        return JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.prepareGgufLab(json))
    }
    fun encodeGgufLab(pcm: SharedMemory, count: Int): JSONObject =
        JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.encodeGgufLab(pcm, count))
    fun verifyDecoderFixture(): JSONObject =
        JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.verifyDecoderFixture())
    fun finishGgufLab(): JSONObject =
        JSONObject(requireNotNull(remote) { "Qwen worker disconnected" }.finishGgufLab())
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
                error("Qwen experimental worker could not connect")
            }
            return result
        }
    }
}
