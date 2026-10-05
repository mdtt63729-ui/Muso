package com.maxrave.domain.repository

import com.maxrave.domain.data.model.lyrics.RomanizationDictionaryState
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Muso integration shim: the suite's LyricsView injects a romanizer repository.
 * Muso does not (yet) ship the romanization service, so this implementation
 * returns null for every line - the suite's contract renders the original
 * line unchanged, exactly as a language without a library behaves upstream.
 */
class NoopLyricsRomanizerRepository : LyricsRomanizerRepository {
    override fun romanize(line: String, enabled: Set<RomanizationLanguage>): String? = null

    override val japaneseDictionaryState: StateFlow<RomanizationDictionaryState> =
        MutableStateFlow(RomanizationDictionaryState.NOT_DOWNLOADED)

    override suspend fun downloadJapaneseDictionary() { }
}
