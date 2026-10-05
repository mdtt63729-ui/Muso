package com.muso.music.utils

import android.content.Context
import com.muso.music.db.entities.LyricsEntity

object TranslationHelper {
    // FOSS flavor: no Google translate dependency - AI translation (own API key) is the
    // one real engine here; without it the lyrics simply stay in the original language.
    suspend fun translate(context: Context, lyrics: LyricsEntity): LyricsEntity =
        AITranslator.translate(context, lyrics)

    suspend fun clearModels() {}
}
