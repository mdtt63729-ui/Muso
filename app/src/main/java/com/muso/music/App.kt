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
 * Upgrades every YouTube thumbnail to the highest resolution variant available:
 * maxresdefault (1280px) first, hq720 second, falling back to the original URL when neither
 * loads. Applies globally — home grid, mini player, full player, search results.
 */
private class HqThumbnailInterceptor : Interceptor {
    private val lowResPattern = Regex("/(hq|mq|sd)?default")

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        if (data is String && data.contains("i.ytimg.com/vi/") &&
            !data.contains("/maxresdefault") && !data.contains("/hq720")
        ) {
            for (url in listOf(
                lowResPattern.replace(data, "/maxresdefault"),
                lowResPattern.replace(data, "/hq720")
            )) {
                try {
                    val result = chain.proceed(chain.request.newBuilder().data(url).build())
                    if (result is SuccessResult) return result
                } catch (_: Exception) {
                }
            }
        }
        return chain.proceed(chain.request)
    }
}
