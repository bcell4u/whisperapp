package com.example.whisperrecorder.npu

internal data class NpuTensorContract(val name: String, val shape: LongArray)
internal data class NpuGraphContract(
    val id: String,
    val graphName: String,
    val artifactPath: String,
    val artifactBytes: Long,
    val inputs: List<NpuTensorContract>,
    val outputs: List<NpuTensorContract>
)

internal object NpuGraphContracts {
    val ENCODER = NpuGraphContract(
        id = "encoder",
        graphName = "fastconformer_encoder_host_mask_v2",
        artifactPath = "model/encoder.bin",
        artifactBytes = 1_249_989_056L,
        inputs = listOf(
            NpuTensorContract("audio_signal", longArrayOf(1, 128, 4001)),
            NpuTensorContract("subsample_mask_1", longArrayOf(1, 2001)),
            NpuTensorContract("subsample_mask_2", longArrayOf(1, 1001)),
            NpuTensorContract("subsample_mask_3", longArrayOf(1, 501))
        ),
        outputs = listOf(NpuTensorContract("outputs", longArrayOf(1, 1024, 501)))
    )

    val PROJECTOR = NpuGraphContract(
        id = "projector",
        graphName = "encoder_projector_500f_v1",
        artifactPath = "model/encoder_projector.bin",
        artifactBytes = 1_397_536L,
        inputs = listOf(NpuTensorContract("encoder_features", longArrayOf(1, 1024, 500))),
        outputs = listOf(NpuTensorContract("projected", longArrayOf(1, 640, 500)))
    )

    val PREDICTOR = NpuGraphContract(
        id = "predictor",
        graphName = "tdt_predictor_core_embedding_v3_split_state",
        artifactPath = "model/predictor.bin",
        artifactBytes = 14_113_408L,
        inputs = listOf(
            NpuTensorContract("token_embedding", longArrayOf(1, 1, 640)),
            NpuTensorContract("h0", longArrayOf(1, 640)),
            NpuTensorContract("c0", longArrayOf(1, 640)),
            NpuTensorContract("h1", longArrayOf(1, 640)),
            NpuTensorContract("c1", longArrayOf(1, 640))
        ),
        outputs = listOf(
            NpuTensorContract("predictor_out", longArrayOf(1, 1, 640)),
            NpuTensorContract("h0_out", longArrayOf(1, 640)),
            NpuTensorContract("c0_out", longArrayOf(1, 640)),
            NpuTensorContract("h1_out", longArrayOf(1, 640)),
            NpuTensorContract("c1_out", longArrayOf(1, 640))
        )
    )

    val JOINT = NpuGraphContract(
        id = "joint",
        graphName = "tdt_joint_relu_head_v1",
        artifactPath = "model/joint.bin",
        artifactBytes = 10_716_008L,
        inputs = listOf(
            NpuTensorContract("encoder_projected_frame", longArrayOf(1, 1, 640)),
            NpuTensorContract("predictor_out", longArrayOf(1, 1, 640))
        ),
        outputs = listOf(NpuTensorContract("tdt_logits", longArrayOf(1, 1, 8198)))
    )

    val ALL = listOf(ENCODER, PROJECTOR, PREDICTOR, JOINT)
}

/** Peak magnitude in the zero-padded frames of a channel-major [1, 1024, 501] encoder output. */
internal fun encoderPaddedMaxAbs(encoded: FloatArray, validEncodedFrames: Int): Float {
    val shape = NpuGraphContracts.ENCODER.outputs.single().shape
    val channels = shape[1].toInt()
    val frames = shape[2].toInt()
    require(encoded.size == channels * frames) {
        "Encoder output size mismatch: expected=${channels * frames}, actual=${encoded.size}"
    }
    require(validEncodedFrames in 0..frames) {
        "Invalid valid encoded frame count: $validEncodedFrames"
    }
    var peak = 0f
    for (channel in 0 until channels) {
        val end = (channel + 1) * frames
        for (i in channel * frames + validEncodedFrames until end) {
            peak = maxOf(peak, kotlin.math.abs(encoded[i]))
        }
    }
    return peak
}
