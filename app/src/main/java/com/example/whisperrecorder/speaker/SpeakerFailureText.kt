package com.example.whisperrecorder.speaker

import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.IdentityHashMap

/** Pure Kotlin formatting, independently testable without Android or model weights. */
object SpeakerFailureText {
    fun summary(error: Throwable): String {
        val type = error.javaClass.simpleName.ifBlank { error.javaClass.name }
        val detail = error.message?.trim().orEmpty()
        return if (error is LinkageError) {
            "Speaker runtime could not load ($type). Open diagnostics for the cause."
        } else if (detail.isBlank()) {
            "Speaker identification failed ($type). Open diagnostics for details."
        } else {
            "Speaker identification failed ($type): ${detail.take(180)}"
        }
    }

    fun exceptionChain(error: Throwable): String {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val output = StringBuilder()
        var current: Throwable? = error
        var depth = 0
        while (current != null && seen.add(current) && depth < 12) {
            if (depth > 0) output.append("Caused by -> ")
            output.append(current.javaClass.name).append(": ")
                .append(current.message ?: "(no message)").append('\n')
            current = current.cause
            depth++
        }
        if (current != null) output.append("Additional causes omitted (cycle or depth limit)\n")
        return output.toString()
    }

    fun stackTrace(error: Throwable): String = StringWriter().use { writer ->
        PrintWriter(writer).use { error.printStackTrace(it) }
        writer.toString().take(24_000)
    }
}
