package com.muso.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private const val REPO_RELEASES_URL = "https://api.github.com/repos/mdtt63729-ui/Muso/releases/latest"

    private val client = HttpClient()
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
     * The release's tag (e.g. "v0.5.20" or "v0.5.20-v1"), normalized so it can be compared with
     * BuildConfig.VERSION_NAME: leading "v" stripped, trailing "-vN" iteration dropped.
     */
    suspend fun getLatestVersionName(): Result<String> = fetchRelease().map { json ->
        json.optString("tag_name", json.optString("name", ""))
            .removePrefix("v")
            .substringBefore("-v")
    }

    /** Browser-download URL of the release's first APK asset (Muso_v0.5.20_v1.apk). */
    suspend fun getLatestReleaseAssetUrl(): Result<String> = fetchRelease().map { json ->
        val assets = json.optJSONArray("assets")
        require(!(assets == null || assets.length() == 0)) { "Release has no assets" }
        assets.getJSONObject(0).getString("browser_download_url")
    }
}
