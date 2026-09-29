/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune

/*
 * BuildConfig shim for the ArchiveTune settings kit (Muso port, Phase 2).
 * The kit's screens expect `moe.rukamori.archivetune.BuildConfig`; in this
 * module the generated one is `com.muso.music.BuildConfig`, so this object
 * forwards the fields that exist here and supplies safe values for the rest.
 */
object BuildConfig {
    val DEBUG: Boolean = com.muso.music.BuildConfig.DEBUG
    val VERSION_NAME: String = com.muso.music.BuildConfig.VERSION_NAME
    val VERSION_CODE: Int = com.muso.music.BuildConfig.VERSION_CODE
    val DISTRIBUTION: String = com.muso.music.BuildConfig.DISTRIBUTION

    // Runtime device info (logcat / debug screens).
    val DEVICE: String = android.os.Build.DEVICE
    val ARCHITECTURE: String = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    // No in-app updater, leakcanary or nightly channel in Muso builds.
    const val UPDATER_AVAILABLE = false
    const val LEAK_CANARY_TOGGLE_AVAILABLE = false
    const val IS_NIGHTLY_BUILD = false

    // Muso carries no Discord / Last.fm / Music Together build secrets; the
    // Discord RPC and Music Together sections stay disabled until Phase 5.
    const val DISCORD_APPLICATION_ID = ""
    const val DISCORD_APPLICATION_ID_LONG = 0L
    const val DISCORD_REDIRECT_SCHEME = ""
    const val LASTFM_API_KEY = ""
    const val LASTFM_SECRET = ""
    const val TOGETHER_BEARER_TOKEN = ""

    const val GITHUB_OWNER = "rukamori"
    const val GITHUB_REPO = "ArchiveTune"
    const val RELEASE_GITHUB_OWNER = "rukamori"
    const val RELEASE_GITHUB_REPO = "ArchiveTune"
}
