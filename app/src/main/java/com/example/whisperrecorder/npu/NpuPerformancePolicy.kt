package com.example.whisperrecorder.npu

/** QNN HTP performance policy used by normal NPU transcription. */
internal object NpuPerformancePolicy {
    enum class QnnMode(val qnnValue: String?) {
        PROVIDER_DEFAULT(null),
        BALANCED("balanced")
    }

    data class Profile(
        val id: String,
        val encoderMode: QnnMode,
        val decoderMode: QnnMode
    )

    val PROVIDER_DEFAULT = Profile("provider_default", QnnMode.PROVIDER_DEFAULT, QnnMode.PROVIDER_DEFAULT)
    private val BALANCED_ALL = Profile("balanced", QnnMode.BALANCED, QnnMode.BALANCED)

    /** Preserve the accepted AUTO behavior: provider-default unless Android reports severe thermal load. */
    fun productionProfile(thermalStatus: Int): Profile =
        if (thermalStatus >= 3) BALANCED_ALL else PROVIDER_DEFAULT
}
