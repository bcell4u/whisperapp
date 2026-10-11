package com.example.whisperrecorder.transcribe

/** Choose the newest word timing only when it matches the saved speaker text. No audio is reprocessed. */
object SpeakerWordSync {
    data class Choice(val useRegular: Boolean, val speakerMapped: Int, val regularMapped: Int,
        val regularWordMapped: Int, val textWords: Int) {
        val source: String get() = if (useRegular) "regular-words" else "speaker-sidecar"
    }

    fun choose(text: String, speaker: List<TranscriptCue>, regular: List<TranscriptCue>): Choice {
        if (text.isBlank()) return Choice(false, 0, 0, 0, 0)
        val textWords = TranscriptCueMapper.playableWordCount(text)
        val oldRanges = TranscriptCueMapper.map(text, speaker, skipSpeakerHeadings = true)
        val oldWords = oldRanges.count { isWord(speaker[it.cueIndex]) }
        // Do not parse the second large timeline when the speaker file is already word-precise.
        if (oldWords >= 8 && oldWords * 100 >= textWords * 85) {
            return Choice(false, oldRanges.size, 0, 0, textWords)
        }
        val newRanges = TranscriptCueMapper.map(text, regular, skipSpeakerHeadings = true)
        val newWords = newRanges.count { isWord(regular[it.cueIndex]) }
        val useRegular = newWords >= 8 && newWords * 100 >= textWords * 80 &&
            newWords > oldWords + 5 && newWords > oldWords * 6 / 5
        return Choice(useRegular, oldRanges.size, newRanges.size, newWords, textWords)
    }

    private fun isWord(cue: TranscriptCue): Boolean = cue.text.isNotBlank() && cue.text.none(Char::isWhitespace)
}
