package com.example.whisperrecorder.npu

/** Actual QNN 2.50 context interfaces extracted by qnn-context-binary-utility (SM8850/V81). */
internal object OmiNpuProbeContracts {
    data class Context(val graph: NpuGraphContract, val sha256: String)

    val JOINT = Context(
        NpuGraphContract("omi-joint", "graph_z1sbzhs3", "joint.bin", 3_556_368L,
            listOf(NpuTensorContract("encoder_frame", longArrayOf(1, 1, 1024)),
                NpuTensorContract("predictor_out", longArrayOf(1, 1, 640))),
            listOf(NpuTensorContract("output_0", longArrayOf(1, 1, 1030)))),
        "5de4cb9dce7da6106a05f66136b6d25f77d4408cac49685e1bfc311329d79022"
    )
    val PREDICTOR = Context(
        NpuGraphContract("omi-predictor", "graph_3c5bjib1", "predictor.bin", 13_289_936L,
            listOf(NpuTensorContract("token_embedding", longArrayOf(1, 1, 640)),
                NpuTensorContract("h0", longArrayOf(1, 640)),
                NpuTensorContract("c0", longArrayOf(1, 640)),
                NpuTensorContract("h1", longArrayOf(1, 640)),
                NpuTensorContract("c1", longArrayOf(1, 640))),
            listOf(NpuTensorContract("output_0", longArrayOf(1, 1, 640)),
                NpuTensorContract("output_1", longArrayOf(1, 640)),
                NpuTensorContract("output_2", longArrayOf(1, 640)),
                NpuTensorContract("output_3", longArrayOf(1, 640)),
                NpuTensorContract("output_4", longArrayOf(1, 640)))),
        "387f58e02c1454c0f1900cd71470d0a23b0a1bcbe6fef8bf9d9f32707ec5effd"
    )
    val ENCODER = Context(
        NpuGraphContract("omi-encoder", "graph_ey_zztqn", "encoder.bin", 1_216_129_360L,
            listOf(NpuTensorContract("audio_signal", longArrayOf(1, 128, 1201)),
                NpuTensorContract("subsample_mask_1", longArrayOf(1, 601)),
                NpuTensorContract("subsample_mask_2", longArrayOf(1, 301)),
                NpuTensorContract("subsample_mask_3", longArrayOf(1, 151))),
            listOf(NpuTensorContract("output_0", longArrayOf(1, 1024, 151)))),
        "44522017cf6423a6f8fea29aa86c2256c02c0a4d73cd76aa5ba35d9c4f40d8c6"
    )
    val DECODER = listOf(JOINT, PREDICTOR)
    val ALL = DECODER + ENCODER
}
