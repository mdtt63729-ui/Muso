package com.muso.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private const val REPO_RELEASES_URL = "https://api.github.com/repos/mdtt63729-ui/Muso/releases/latest"

    private val VERSION_CORE_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")

    // Lazy: the class is first touched by App.onCreate's WorkManager
    // scheduling; the network client must only be built when an actual
    // update check runs, never during process startup.
    private val client by lazy { HttpClient() }
    var lastCheckTime = -1L
        private set
    private var cachedJson: JSONObject? = null

    /**
     * Fetches the latest release JSON from the Muso GitHub repo (and caches it for
     * [getLatestVersionName] / [getLatestReleaseAssetUrl] so the three calls share one request).
     */
    private suspend fun fetchRelease(): Result<JSONObject> = runCatching {
        cachedJson?.let { return@runCatching it }
        val response = client.get(REPO_RELEASES_URL).bodyAsText()
        JSONObject(response).also {
            cachedJson = it
            lastCheckTime = System.currentTimeMillis()
        }
    }

    /**
     * The release's version core (e.g. "0.5.213"), pulled from the tag/name so a
     * suffixed tag like "v0.5.213-source" or "Muso-v0.5.213" still normalizes to a
     * value comparable with BuildConfig.VERSION_NAME.
     */
    internal fun extractVersionCore(raw: String): String =
        VERSION_CORE_REGEX.find(raw)?.value
            ?: raw.removePrefix("v").substringBefore("-v").trim()

    private fun parseVersionCore(value: String): List<Int>? =
        VERSION_CORE_REGEX.find(value)?.groupValues?.drop(1)?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 3 }

    /**
     * True when [latest] is strictly newer than [current]. When both carry a
     * 0.5.x-style core, that core is compared - so a release tag with a suffix
     * (e.g. "v0.5.213-source") still matches the installed version and the update
     * popup correctly stops once the app has actually been updated. Falls back to a
     * plain string comparison when either side is not semver-shaped.
     */
    fun isUpdateAvailable(latest: String, current: String): Boolean {
        val l = parseVersionCore(latest)
        val c = parseVersionCore(current)
        return if (l != null && c != null) {
            when {
                l[0] != c[0] -> l[0] > c[0]
                l[1] != c[1] -> l[1] > c[1]
                else -> l[2] > c[2]
            }
        } else {
            latest.trim() != current.trim()
        }
    }

    /**
     * Latest release version core. [force] skips the in-memory JSON cache so a
     * genuinely fresh GitHub call is made - used by the periodic update worker and
     * by notification taps.
     */
    suspend fun getLatestVersionName(force: Boolean = false): Result<String> {
        if (force) cachedJson = null
        return fetchRelease().map { json ->
            extractVersionCore(json.optString("tag_name", json.optString("name", "")))
        }
    }

    /**
     * Browser-download URL of the release's APK asset (e.g. Muso_v0.5.213_v220.apk).
     *
     * The APK is selected by its name rather than blindly taking assets[0]: a release
     * may also carry a "-source.zip" (or other) asset, and assets[0] is not guaranteed
     * to be the installable one - the DownloadManager would then fetch a non-APK file
     * and the package installer would fail.
     */
    suspend fun getLatestReleaseAssetUrl(): Result<String> = fetchRelease().map { json ->
        val assets = json.optJSONArray("assets")
        require(!(assets == null || assets.length() == 0)) { "Release has no assets" }
        val candidates =
            (0 until assets.length())
                .mapNotNull { i ->
                    assets.optJSONObject(i)?.let { it.optString("name") to it.optString("browser_download_url") }
                }
                .filter { it.second.isNotBlank() }
        val chosen =
            candidates.firstOrNull { it.first.endsWith(".apk", ignoreCase = true) }
                ?: candidates.firstOrNull()
                ?: error("Release has no downloadable asset")
        chosen.second
    }
}
