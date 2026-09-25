package com.muso.music.utils

import android.content.Context
import com.muso.music.constants.SpotifyAccessTokenKey
import com.muso.music.constants.SpotifyClientIDKey
import com.muso.music.constants.SpotifyDisplayNameKey
import com.muso.music.constants.SpotifyRefreshTokenKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Spotify integration (SimpMusic-style, rebuilt for Muso): a real OAuth PKCE login with
 * the user's own Spotify Client ID, followed by playlist listing through the official
 * Web API. Tokens live only in the app's DataStore and are refreshed automatically.
 *
 * Every entry point fails soft with null/empty results instead of throwing, so a
 * network error or an expired session can never crash the settings screen.
 */
object SpotifyClient {

    const val REDIRECT_URI = "http://localhost:8080/muso_callback"
    const val SCOPES = "playlist-read-private playlist-read-collaborative user-read-private"

    /** Kept between the login screen opening the authorize page and the redirect. */
    var pendingCodeVerifier: String = generateCodeVerifier()
        private set

    data class SpotifyPlaylist(
        val id: String,
        val name: String,
        val trackCount: Int,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** S256 code challenge for PKCE. */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun authorizeUrl(clientId: String): String {
        pendingCodeVerifier = generateCodeVerifier()
        return "https://accounts.spotify.com/authorize" +
            "?client_id=$clientId" +
            "&response_type=code" +
            "&redirect_uri=${encode(REDIRECT_URI)}" +
            "&scope=${encode(SCOPES)}" +
            "&code_challenge=${encode(codeChallenge(pendingCodeVerifier))}" +
            "&code_challenge_method=S256"
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    /**
     * Exchanges the OAuth code for tokens and stores them. Returns the display name,
     * or null on failure (wrong client ID, network error, denied permission...).
     */
    suspend fun completeLogin(context: Context, code: String): String? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val clientId = prefs[SpotifyClientIDKey].orEmpty()
        if (clientId.isEmpty()) return@withContext null

        val tokens: Pair<String, String>? = runCatching {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", clientId)
                .add("code_verifier", pendingCodeVerifier)
                .build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string() ?: return@use null)
                val token = json.optString("access_token")
                if (token.isEmpty()) null else token to json.optString("refresh_token")
            }
        }.getOrNull()

        if (tokens == null) return@withContext null

        context.dataStore.edit {
            it[SpotifyAccessTokenKey] = tokens.first
            if (tokens.second.isNotEmpty()) it[SpotifyRefreshTokenKey] = tokens.second
        }

        // The profile call is best-effort: an empty name still means "logged in".
        runCatching { fetchDisplayName(tokens.first) }.getOrNull().orEmpty()
    }

    private fun fetchDisplayName(accessToken: String): String? {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string() ?: return null)
            return json.optString("display_name").ifEmpty { json.optString("id") }
        }
    }

    /**
     * Returns a valid access token, refreshing it first when the stored one was
     * rejected, or null when no session exists / refresh failed.
     */
    suspend fun validAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        var accessToken = prefs[SpotifyAccessTokenKey].orEmpty()
        if (accessToken.isEmpty()) return@withContext null
        val refreshToken = prefs[SpotifyRefreshTokenKey].orEmpty()
        val clientId = prefs[SpotifyClientIDKey].orEmpty()
        if (refreshToken.isEmpty() || clientId.isEmpty()) return@withContext accessToken

        // Probe the token with a cheap call; refresh only when it was rejected.
        if (meSucceeds(accessToken)) return@withContext accessToken

        runCatching {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()
            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val json = JSONObject(response.body?.string() ?: return@runCatching null)
                val newToken = json.optString("access_token")
                if (newToken.isEmpty()) return@runCatching null
                context.dataStore.edit {
                    it[SpotifyAccessTokenKey] = newToken
                    val rotated = json.optString("refresh_token")
                    if (rotated.isNotEmpty()) it[SpotifyRefreshTokenKey] = rotated
                }
                newToken
            }
        }.getOrNull() ?: accessToken
    }

    private fun meSucceeds(accessToken: String): Boolean = runCatching {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)

    /**
     * The user's playlists (id, name, track count), or null when the call failed -
     * the settings screen turns that into an error row with a Retry, never a stuck
     * spinner.
     */
    suspend fun playlists(context: Context): List<SpotifyPlaylist>? = withContext(Dispatchers.IO) {
        val accessToken = validAccessToken(context) ?: return@withContext null
        runCatching {
            val result = mutableListOf<SpotifyPlaylist>()
            var url: String? = "https://api.spotify.com/v1/me/playlists?limit=50"
            while (url != null && result.size < 500) {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val json = JSONObject(response.body?.string() ?: return@runCatching null)
                    val items = json.optJSONArray("items") ?: org.json.JSONArray()
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isEmpty()) continue
                        result += SpotifyPlaylist(
                            id = id,
                            name = item.optString("name"),
                            trackCount = item.optJSONObject("tracks")?.optInt("total") ?: 0,
                        )
                    }
                    url = json.optString("next").takeIf { it.isNotEmpty() }
                }
            }
            result
        }.getOrNull()
    }

    suspend fun logOut(context: Context) {
        context.dataStore.edit {
            it.remove(SpotifyAccessTokenKey)
            it.remove(SpotifyRefreshTokenKey)
            it.remove(SpotifyDisplayNameKey)
        }
    }
}
