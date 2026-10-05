package com.muso.music.utils

/**
 * Offline lyrics romanizer: converts non-Latin scripts in lyrics into Latin letters so a
 * song can be sung along without reading the original script. Pure character mapping -
 * no network, no provider data needed - so it works everywhere instantly and can never
 * block or crash the lyrics screen.
 *
 * Supported scripts: Bengali, Devanagari (Hindi and friends), Cyrillic, Greek, Japanese
 * kana (per-syllable), and Korean hangul (algorithmic Revised Romanization). Anything
 * else passes through untouched, so English and mixed lyrics are unaffected.
 *
 * It is a transliteration, not a linguistically perfect transcription - conjuncts and
 * assimilations are approximated the way most lyrics apps do.
 */
object Romanizer {

    fun romanize(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length + 16)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val cp = c.code
            when {
                cp == 0x09CD || cp == 0x094D -> i++ // Bengali / Devanagari virama: conjunct marker
                cp in 0x0980..0x09FF -> { sb.append(BENGALI[c] ?: c); i++ }
                cp in 0x0900..0x097F -> { sb.append(DEVANAGARI[c] ?: c); i++ }
                cp in 0x0400..0x04FF -> { appendCapitalizing(sb, c, CYRILLIC); i++ }
                cp in 0x0370..0x03FF -> { appendCapitalizing(sb, c, GREEK); i++ }
                cp == 0x30FC -> { sb.append('-'); i++ } // prolonged sound mark
                cp in 0x3040..0x30FF -> {
                    val mapped = KANA[c]
                    if (mapped == null) {
                        sb.append(c)
                    } else if (mapped.isEmpty() && (c == '\u3063' || c == '\u30C3')) {
                        // small tsu doubles the next syllable's first consonant
                        text.getOrNull(i + 1)?.let(KANA::get)?.takeIf { it.isNotEmpty() }?.let { sb.append(it[0]) }
                    } else {
                        sb.append(mapped)
                    }
                    i++
                }
                cp in 0xAC00..0xD7A3 -> { sb.append(hangul(cp)); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    /** Uppercase letters keep their capitalization through the mapping. */
    private fun appendCapitalizing(sb: StringBuilder, c: Char, map: Map<Char, String>) {
        val mapped = map[c.lowercaseChar()]
        when {
            mapped == null -> sb.append(c)
            c.isUpperCase() && mapped.isNotEmpty() -> {
                sb.append(mapped[0].uppercaseChar())
                if (mapped.length > 1) sb.append(mapped, 1, mapped.length)
            }
            else -> sb.append(mapped)
        }
    }

    private fun hangul(cp: Int): String {
        val s = cp - 0xAC00
        val l = s / 588
        val v = (s % 588) / 28
        val t = s % 28
        return CHO[l] + JUNG[v] + if (t > 0) JONG[t] else ""
    }

    // Revised Romanization of Korean: 19 initials, 21 medials, 28 finals.
    private val CHO = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "", "ss",
        "j", "jj", "ch", "k", "t", "p", "h"
    )
    private val JUNG = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo",
        "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )
    private val JONG = arrayOf(
        "", "g", "kk", "gs", "n", "nj", "nh", "d", "r", "rg", "rm", "rb", "rs", "rt",
        "rp", "rh", "m", "b", "bs", "s", "ss", "ng", "j", "c", "k", "t", "p", "h"
    )

    private val BENGALI = mapOf(
        '\u0985' to "o", '\u0986' to "a", '\u0987' to "i", '\u0988' to "ii", '\u0989' to "u",
        '\u098A' to "uu", '\u098B' to "ri", '\u098F' to "e", '\u0990' to "oi", '\u0993' to "o",
        '\u0994' to "ou", '\u0995' to "k", '\u0996' to "kh", '\u0997' to "g", '\u0998' to "gh",
        '\u0999' to "ng", '\u099A' to "ch", '\u099B' to "chh", '\u099C' to "j", '\u099D' to "jh",
        '\u099E' to "ny", '\u099F' to "t", '\u09A0' to "th", '\u09A1' to "d", '\u09A2' to "dh",
        '\u09A3' to "n", '\u09A4' to "t", '\u09A5' to "th", '\u09A6' to "d", '\u09A7' to "dh",
        '\u09A8' to "n", '\u09AA' to "p", '\u09AB' to "ph", '\u09AC' to "b", '\u09AD' to "bh",
        '\u09AE' to "m", '\u09AF' to "y", '\u09B0' to "r", '\u09B2' to "l", '\u09B6' to "sh",
        '\u09B7' to "sh", '\u09B8' to "s", '\u09B9' to "h", '\u09CE' to "t", '\u09B1' to "w",
        '\u09BC' to "", '\u09BE' to "a", '\u09BF' to "i", '\u09C0' to "ii", '\u09C1' to "u",
        '\u09C2' to "uu", '\u09C3' to "ri", '\u09C7' to "e", '\u09C8' to "oi", '\u09CB' to "o",
        '\u09CC' to "ou", '\u0982' to "ng", '\u0983' to "h", '\u0981' to "n", '\u09E6' to "0",
        '\u09E7' to "1", '\u09E8' to "2", '\u09E9' to "3", '\u09EA' to "4", '\u09EB' to "5",
        '\u09EC' to "6", '\u09ED' to "7", '\u09EE' to "8", '\u09EF' to "9",
    )

    private val DEVANAGARI = mapOf(
        '\u0905' to "a", '\u0906' to "aa", '\u0907' to "i", '\u0908' to "ii", '\u0909' to "u",
        '\u090A' to "uu", '\u090B' to "ri", '\u090F' to "e", '\u0910' to "ai", '\u0913' to "o",
        '\u0914' to "au", '\u0915' to "k", '\u0916' to "kh", '\u0917' to "g", '\u0918' to "gh",
        '\u0919' to "ng", '\u091A' to "ch", '\u091B' to "chh", '\u091C' to "j", '\u091D' to "jh",
        '\u091E' to "ny", '\u091F' to "t", '\u0920' to "th", '\u0921' to "d", '\u0922' to "dh",
        '\u0923' to "n", '\u0924' to "t", '\u0925' to "th", '\u0926' to "d", '\u0927' to "dh",
        '\u0928' to "n", '\u092A' to "p", '\u092B' to "ph", '\u092C' to "b", '\u092D' to "bh",
        '\u092E' to "m", '\u092F' to "y", '\u0930' to "r", '\u0932' to "l", '\u0935' to "v",
        '\u0936' to "sh", '\u0937' to "sh", '\u0938' to "s", '\u0939' to "h", '\u093E' to "aa",
        '\u093F' to "i", '\u0940' to "ii", '\u0941' to "u", '\u0942' to "uu", '\u0943' to "ri",
        '\u0947' to "e", '\u0948' to "ai", '\u094B' to "o", '\u094C' to "au", '\u0902' to "n",
        '\u0903' to "h", '\u0901' to "n", '\u0966' to "0", '\u0967' to "1", '\u0968' to "2",
        '\u0969' to "3", '\u096A' to "4", '\u096B' to "5", '\u096C' to "6", '\u096D' to "7",
        '\u096E' to "8", '\u096F' to "9",
    )

    private val CYRILLIC = mapOf(
        '\u0430' to "a", '\u0431' to "b", '\u0432' to "v", '\u0433' to "g", '\u0434' to "d",
        '\u0435' to "e", '\u0451' to "yo", '\u0436' to "zh", '\u0437' to "z", '\u0438' to "i",
        '\u0439' to "y", '\u043A' to "k", '\u043B' to "l", '\u043C' to "m", '\u043D' to "n",
        '\u043E' to "o", '\u043F' to "p", '\u0440' to "r", '\u0441' to "s", '\u0442' to "t",
        '\u0443' to "u", '\u0444' to "f", '\u0445' to "kh", '\u0446' to "ts", '\u0447' to "ch",
        '\u0448' to "sh", '\u0449' to "shch", '\u044A' to "\'", '\u044B' to "y", '\u044C' to "\'",
        '\u044D' to "e", '\u044E' to "yu", '\u044F' to "ya", '\u0454' to "ye", '\u0456' to "i",
        '\u0457' to "yi", '\u0491' to "g", '\u045E' to "u", '\u0452' to "dj", '\u045B' to "ch",
        '\u045F' to "dz", '\u0459' to "lj", '\u045A' to "nj",
    )

    private val GREEK = mapOf(
        '\u03B1' to "a", '\u03B2' to "v", '\u03B3' to "g", '\u03B4' to "d", '\u03B5' to "e",
        '\u03B6' to "z", '\u03B7' to "i", '\u03B8' to "th", '\u03B9' to "i", '\u03BA' to "k",
        '\u03BB' to "l", '\u03BC' to "m", '\u03BD' to "n", '\u03BE' to "x", '\u03BF' to "o",
        '\u03C0' to "p", '\u03C1' to "r", '\u03C3' to "s", '\u03C2' to "s", '\u03C4' to "t",
        '\u03C5' to "y", '\u03C6' to "f", '\u03C7' to "ch", '\u03C8' to "ps", '\u03C9' to "o",
    )

    private val KANA = mapOf(
        '\u3042' to "a", '\u3044' to "i", '\u3046' to "u", '\u3048' to "e", '\u304A' to "o",
        '\u304B' to "ka", '\u304D' to "ki", '\u304F' to "ku", '\u3051' to "ke", '\u3053' to "ko",
        '\u3055' to "sa", '\u3057' to "shi", '\u3059' to "su", '\u305B' to "se", '\u305D' to "so",
        '\u305F' to "ta", '\u3061' to "chi", '\u3064' to "tsu", '\u3066' to "te", '\u3068' to "to",
        '\u306A' to "na", '\u306B' to "ni", '\u306C' to "nu", '\u306D' to "ne", '\u306E' to "no",
        '\u306F' to "ha", '\u3072' to "hi", '\u3075' to "fu", '\u3078' to "he", '\u307B' to "ho",
        '\u307E' to "ma", '\u307F' to "mi", '\u3080' to "mu", '\u3081' to "me", '\u3082' to "mo",
        '\u3084' to "ya", '\u3086' to "yu", '\u3088' to "yo", '\u3089' to "ra", '\u308A' to "ri",
        '\u308B' to "ru", '\u308C' to "re", '\u308D' to "ro", '\u308F' to "wa", '\u3092' to "o",
        '\u3093' to "n", '\u304C' to "ga", '\u304E' to "gi", '\u3050' to "gu", '\u3052' to "ge",
        '\u3054' to "go", '\u3056' to "za", '\u3058' to "ji", '\u305A' to "zu", '\u305C' to "ze",
        '\u305E' to "zo", '\u3060' to "da", '\u3062' to "ji", '\u3065' to "zu", '\u3067' to "de",
        '\u3069' to "do", '\u3070' to "ba", '\u3073' to "bi", '\u3076' to "bu", '\u3079' to "be",
        '\u307C' to "bo", '\u3071' to "pa", '\u3074' to "pi", '\u3077' to "pu", '\u307A' to "pe",
        '\u307D' to "po", '\u30A2' to "a", '\u30A4' to "i", '\u30A6' to "u", '\u30A8' to "e",
        '\u30AA' to "o", '\u30AB' to "ka", '\u30AD' to "ki", '\u30AF' to "ku", '\u30B1' to "ke",
        '\u30B3' to "ko", '\u30B5' to "sa", '\u30B7' to "shi", '\u30B9' to "su", '\u30BB' to "se",
        '\u30BD' to "so", '\u30BF' to "ta", '\u30C1' to "chi", '\u30C4' to "tsu", '\u30C6' to "te",
        '\u30C8' to "to", '\u30CA' to "na", '\u30CB' to "ni", '\u30CC' to "nu", '\u30CD' to "ne",
        '\u30CE' to "no", '\u30CF' to "ha", '\u30D2' to "hi", '\u30D5' to "fu", '\u30D8' to "he",
        '\u30DB' to "ho", '\u30DE' to "ma", '\u30DF' to "mi", '\u30E0' to "mu", '\u30E1' to "me",
        '\u30E2' to "mo", '\u30E4' to "ya", '\u30E6' to "yu", '\u30E8' to "yo", '\u30E9' to "ra",
        '\u30EA' to "ri", '\u30EB' to "ru", '\u30EC' to "re", '\u30ED' to "ro", '\u30EF' to "wa",
        '\u30F2' to "o", '\u30F3' to "n", '\u30AC' to "ga", '\u30AE' to "gi", '\u30B0' to "gu",
        '\u30B2' to "ge", '\u30B4' to "go", '\u30B6' to "za", '\u30B8' to "ji", '\u30BA' to "zu",
        '\u30BC' to "ze", '\u30BE' to "zo", '\u30C0' to "da", '\u30C2' to "ji", '\u30C5' to "zu",
        '\u30C7' to "de", '\u30C9' to "do", '\u30D0' to "ba", '\u30D3' to "bi", '\u30D6' to "bu",
        '\u30D9' to "be", '\u30DC' to "bo", '\u30D1' to "pa", '\u30D4' to "pi", '\u30D7' to "pu",
        '\u30DA' to "pe", '\u30DD' to "po", '\u3063' to "", '\u30C3' to "",
    )
}
