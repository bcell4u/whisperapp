package com.example.whisperrecorder.speaker

/** An expected count is a clustering constraint, not a guarantee of correct voice attribution. */
object SpeakerClusteringPolicy {
    const val AUTO = -1
    const val AUTO_THRESHOLD = 0.75f
    val choices: List<Int> = listOf(AUTO, 1, 2, 3, 4, 5, 6)

    fun normalize(expected: Int): Int = expected.takeIf { it in 1..6 } ?: AUTO

    fun label(expected: Int): String =
        if (normalize(expected) == AUTO) "Automatic" else normalize(expected).toString()

    fun description(expected: Int): String =
        if (normalize(expected) == AUTO) "Automatically estimate speaker count" else
            "Use ${normalize(expected)} expected speaker${if (normalize(expected) == 1) "" else "s"}"
}
