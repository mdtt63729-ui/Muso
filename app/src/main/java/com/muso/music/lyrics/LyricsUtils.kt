package com.muso.music.lyrics

import android.text.format.DateUtils

/**
 * Scroll-animation lead time (ms) used when picking the line being sung.
 *
 * Moved here from the retired ui/component/Lyrics.kt renderer: this is the only
 * symbol from that file anything still used.
 */
const val animateScrollDuration = 300L

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d{1,2}:\\d{1,2}(?:[.:]\\d{1,3})?\\])+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]".toRegex()

    // LRC metadata tags - [ar:], [ti:], [by:], [re:], [ve:], [offset:], [length:]...
    // They are file headers, never lyrics: always dropped.
    private val METADATA_LINE_REGEX = "^\\[[a-zA-Z#]+:.+\\]$".toRegex()

    /**
     * True when ANY line starts with an [mm:ss.xx] tag. This replaces the old
     * "the file starts with [" detection, which misread files with a leading
     * blank line or BOM as unsynced and then dumped raw timestamps into the
     * visible lyrics.
     */
    fun hasTimestampedLines(lyrics: String): Boolean =
        lyrics.lines().any { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("[") && TIME_REGEX.containsMatchIn(trimmed)
        }

    /**
     * Plain-lyrics cleanup (SimpMusic): metadata-tag lines are dropped, any
     * inline [mm:ss.xx] time tags still left inside the text are stripped, and
     * blank leftovers are removed - so nothing but the actual lyric text ever
     * reaches the screen.
     */
    fun sanitizeUnsynced(lyrics: String): List<String> =
        lyrics.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                when {
                    METADATA_LINE_REGEX.matches(line) -> null
                    else -> TIME_REGEX.replace(line, "").trim().takeIf { it.isNotEmpty() }
                }
            }

    fun parseLyrics(lyrics: String): List<LyricsEntry> =
        lyrics.lines()
            .flatMap { line -> parseLine(line).orEmpty() }
            .filter { it.text.isNotBlank() }
            .sorted()

    private fun parseLine(line: String): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        // Strip any time tags embedded mid-text (badly merged LRC exports) so
        // timestamps can never leak into the visible lyric line.
        val text = TIME_REGEX.replace(matchResult.groupValues[3], "").trim()
        if (text.isEmpty()) {
            return null
        }
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults.map { timeMatchResult ->
            val min = timeMatchResult.groupValues[1].toLong()
            val sec = timeMatchResult.groupValues[2].toLong()
            val milString = timeMatchResult.groupValues[3]
            // The fraction is optional in LRC and may be written with 1-3 digits
            // (tenths / hundredths / thousandths); the older format also separates
            // it with a colon instead of a dot. All of those used to be dropped.
            val mil = when (milString.length) {
                0 -> 0L
                1 -> milString.toLong() * 100
                2 -> milString.toLong() * 10
                else -> milString.toLong()
            }
            val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
            LyricsEntry(time, text)
        }.toList()
    }

    /**
     * Index of the line being sung at [position], or -1 before the first line.
     * [lines] must be sorted by time, which [parseLyrics] guarantees.
     *
     * Binary search: this is called on every playback-position tick, so the old
     * linear scan cost O(lines) per tick.
     */
    fun findCurrentLineIndex(lines: List<LyricsEntry>, position: Long): Int {
        val target = position + animateScrollDuration
        var lo = 0
        var hi = lines.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].time >= target) hi = mid else lo = mid + 1
        }
        return if (lo == lines.size) lines.lastIndex else lo - 1
    }
}
