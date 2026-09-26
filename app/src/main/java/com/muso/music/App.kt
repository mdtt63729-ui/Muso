package com.muso.music

import android.app.Application
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.SuccessResult
import coil.request.ImageResult
import coil.intercept.Interceptor
import coil.disk.DiskCache
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.YouTubeLocale
import com.zionhuang.kugou.KuGou
import com.muso.music.constants.ContentCountryKey
import com.muso.music.constants.ContentLanguageKey
import com.muso.music.constants.CountryCodeToName
import com.muso.music.constants.InnerTubeCookieKey
import com.muso.music.constants.LanguageCodeToName
import com.muso.music.constants.MaxImageCacheSizeKey
import com.muso.music.constants.ProxyEnabledKey
import com.muso.music.constants.ProxyTypeKey
import com.muso.music.constants.ProxyUrlKey
import com.muso.music.constants.SYSTEM_DEFAULT
import com.muso.music.constants.UseLoginForBrowse
import com.muso.music.constants.VisitorDataKey
import com.muso.music.extensions.toEnum
import com.muso.music.extensions.toInetSocketAddress
import com.muso.music.utils.UpdateCheckWorker
import com.muso.music.utils.dataStore
import com.muso.music.utils.get
import com.muso.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // Crash log capture: writes the stack trace of any uncaught crash to
        // files/crash.log so the next start can show it in-app without adb -
        // the fastest way to pin down the Library-tab crash.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                java.io.File(filesDir, "crash.log").writeText(
                    buildString {
                        appendLine("Muso crash at " + java.time.LocalDateTime.now())
                        appendLine("Thread: " + thread.name)
                        appendLine()
                        append(android.util.Log.getStackTraceString(throwable))
                    }
                )
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Timber.plant(Timber.DebugTree())

        // Update notification: check GitHub for a newer release roughly every
        // 15 minutes (WorkManager's minimum period) even while the app is closed,
        // and post a system notification as soon as one is published. The check
        // is a single tiny API call; Doze/App Standby may defer it, which is fine.
        // KEEP means the schedule survives repeated process starts untouched.
        // Scheduling touches WorkManager's own Room database; running that on the
        // main thread stalls process startup (and with it the splash). WorkManager
        // is thread-safe, so schedule from a background thread instead.
        Thread {
            runCatching {
                WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                    "muso_update_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<UpdateCheckWorker>(15, TimeUnit.MINUTES).build(),
                )
            }
        }.start()

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] == true) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        } ?: YouTube.DEFAULT_VISITOR_DATA
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    YouTube.cookie = cookie
                }
        }
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .components { add(HqThumbnailInterceptor()) }
        .crossfade(true)
        .respectCacheHeaders(false)
        .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes((dataStore[MaxImageCacheSizeKey] ?: 512) * 1024 * 1024L)
                .build()
        )
        .build()
}

/**
 * Upgrades EVERY YouTube art URL to the highest resolution the server has:
 * ultra-high (2160px) first, 1200px second, falling back to the original URL when
 * neither loads. Applies globally - home grid, quick picks, playlist cards and
 * song rows, mini player, full player, lyrics card, artist pages, search results.
 *
 * Two googleusercontent URL shapes are handled:
 *  - "=w###-h###" (song/album/playlist art)  -> "=w2160-h2160-p-l90-rj"
 *  - "=s###" (channel avatars, playlist circles - never upgraded before) -> "=s2160"
 * Video thumbnails (i.ytimg.com) go to maxresdefault, then hq720.
 * Coil still downsamples each image to the view, so memory use does not change.
 */
private class HqThumbnailInterceptor : Interceptor {
    private val lowResPattern = Regex("/(hq|mq|sd)?default")
    private val whPattern = Regex("=w(\\d+)-h(\\d+)[^ ]*$")
    private val sPattern = Regex("=s(\\d+)([^ ]*)$")

    private suspend fun tryLoad(chain: Interceptor.Chain, url: String): ImageResult? =
        runCatching {
            chain.proceed(chain.request.newBuilder().data(url).build())
        }.getOrNull()?.takeIf { it is SuccessResult }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        if (data is String) {
            val isGoogleArt = data.contains("googleusercontent.com/") || data.contains("ggpht.com")
            if (isGoogleArt) {
                val wh = whPattern.find(data)
                if (wh != null) {
                    val width = wh.groupValues[1].toIntOrNull() ?: 0
                    if (0 < width && width < 2160) {
                        for (suffix in listOf(
                            "=w2160-h2160-p-l90-rj",
                            "=w1200-h1200-p-l90-rj",
                        )) {
                            tryLoad(chain, whPattern.replace(data, suffix))?.let { return it }
                        }
                    }
                } else {
                    val sm = sPattern.find(data)
                    if (sm != null) {
                        val size = sm.groupValues[1].toIntOrNull() ?: 0
                        val flags = sm.groupValues[2]
                        if (0 < size && size < 2160) {
                            for (s in listOf(2160, 1200)) {
                                tryLoad(chain, sPattern.replace(data, "=s$s$flags"))?.let { return it }
                            }
                        }
                    }
                }
            }
            if (data.contains("i.ytimg.com/vi/") &&
                !data.contains("/maxresdefault") && !data.contains("/hq720")
            ) {
                for (url in listOf(
                    lowResPattern.replace(data, "/maxresdefault"),
                    lowResPattern.replace(data, "/hq720")
                )) {
                    tryLoad(chain, url)?.let { return it }
                }
            }
        }
        return chain.proceed(chain.request)
    }
}
