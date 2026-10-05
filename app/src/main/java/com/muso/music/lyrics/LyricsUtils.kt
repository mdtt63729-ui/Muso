package com.muso.music.lyrics

import android.text.format.DateUtils
import com.muso.music.ui.component.animateScrollDuration

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\])+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

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
            var mil = milString.toLong()
            if (milString.length == 2) {
                mil *= 10
            }
            val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
            LyricsEntry(time, text)
        }.toList()
    }

    fun findCurrentLineIndex(lines: List<LyricsEntry>, position: Long): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + animateScrollDuration) {
                return index - 1
            }
        }
        return lines.lastIndex
    }
}
