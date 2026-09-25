package com.zionhuang.music.lyrics

/**
 * Registry and ordering for all lyrics providers, ported from Echo-Music.
 *
 * The default order matches Echo-Music: YouLyPlus, Paxsenix, Unison, BetterLyrics, SimpMusic,
 * LrcLib, KuGou, YouTube Subtitle, YouTube Music. The user-configurable order is persisted as
 * a comma-separated string of provider names via [com.zionhuang.music.constants.LyricsProviderOrderKey].
 */
object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "YouLyPlus" to YouLyPlusLyricsProvider,
        "Paxsenix" to PaxSenixLyricsProvider,
        "Unison" to UnisonLyricsProvider,
        "BetterLyrics" to BetterLyricsProvider,
        "SimpMusic" to SimpMusicLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "Kugou" to KuGouLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTubeMusic" to YouTubeLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getDisplayName(name: String): String = when (name) {
        "YouLyPlus" -> "YouLyPlus"
        "Paxsenix" -> "PaxSenix"
        "Unison" -> "Unison"
        "BetterLyrics" -> "Better Lyrics"
        "SimpMusic" -> "SimpMusic"
        "LrcLib" -> "LrcLib"
        "Kugou" -> "KuGou"
        "YouTubeSubtitle" -> "YouTube Subtitle"
        "YouTubeMusic" -> "YouTube Music"
        else -> name
    }

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) return getDefaultProviderOrder()
        return orderString.split(",").map { it.trim() }.filter { it in providerMap }
    }

    fun serializeProviderOrder(providers: List<String>): String =
        providers.filter { it in providerMap }.joinToString(",")

    fun getDefaultProviderOrder(): List<String> = listOf(
        "YouLyPlus",
        "Paxsenix",
        "Unison",
        "BetterLyrics",
        "SimpMusic",
        "LrcLib",
        "Kugou",
        "YouTubeSubtitle",
        "YouTubeMusic",
    )

    fun getOrderedProviders(orderString: String?): List<LyricsProvider> {
        val names = if (orderString.isNullOrBlank()) {
            getDefaultProviderOrder()
        } else {
            deserializeProviderOrder(orderString).ifEmpty { getDefaultProviderOrder() }
        }
        return names.mapNotNull { getProviderByName(it) }
    }
}
