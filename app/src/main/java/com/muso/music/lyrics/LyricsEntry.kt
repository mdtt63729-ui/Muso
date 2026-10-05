package com.muso.music.lyrics

/**
 * One word of a karaoke (word-by-word) synced lyric line, timed in milliseconds.
 * Only TTML sources (BetterLyrics / SimpMusic lyrics) carry these; LRC lines
 * keep an empty list and render line-synced as before.
 */
data class LyricsWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<LyricsWord> = emptyList(),
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}
