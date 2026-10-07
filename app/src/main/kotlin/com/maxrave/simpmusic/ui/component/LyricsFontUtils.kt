package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import java.lang.Character.UnicodeScript

/**
 * Lyrics can contain scripts that are only partially covered by the app's decorative UI font.
 * Some bundled fonts expose a few Devanagari glyphs but not the complete shaping/fallback set;
 * Compose can then render combining marks with dotted-circle placeholders instead of using Android's
 * proper script fallback. Lyrics must always prefer correctness over the decorative UI font.
 *
 * For Latin-only text we keep the selected Muso typography. For any line/word containing a
 * non-Latin script we switch to Android's SansSerif family, whose platform fallback chain covers
 * Devanagari, Bengali, Arabic, CJK, Tamil, Telugu, Gurmukhi, etc. This also fixes mixed-script
 * lyrics because the complete line is measured with one script-safe family, preventing glyphs from
 * jumping between incompatible font metrics.
 */
internal fun TextStyle.forLyricsText(text: String): TextStyle {
    if (!text.requiresScriptSafeLyricsFont()) return this
    return copy(
        fontFamily = FontFamily.SansSerif,
        // Keep enough vertical font padding for Indic combining marks and other scripts that place
        // glyphs above/below the Latin em box. This prevents matras/Arabic marks from being clipped.
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
}

internal fun String.requiresScriptSafeLyricsFont(): Boolean {
    for (codePoint in codePoints().toArray()) {
        val script = UnicodeScript.of(codePoint)
        when (script) {
            UnicodeScript.COMMON,
            UnicodeScript.INHERITED,
            UnicodeScript.LATIN,
            UnicodeScript.UNKNOWN -> Unit
            else -> return true
        }
    }
    return false
}
