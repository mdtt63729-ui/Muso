package com.muso.music.suite

/**
 * Holds a reference to Muso's real playback player (owned by the playback
 * service) for the SimpMusic suite, which resolves its "mainPlayer" through
 * Koin to attach video surfaces and subtitle views to the playing player.
 *
 * Set when the service creates its ExoPlayer, cleared when the service is
 * destroyed. The Koin definition in App.kt reads it lazily - the suite only
 * asks for the player once a video surface composes, which happens while
 * playback (and therefore the service) is alive.
 */
object SuitePlayerRegistry {
    @Volatile
    var player: androidx.media3.common.Player? = null
}
