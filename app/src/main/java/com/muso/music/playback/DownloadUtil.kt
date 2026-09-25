package com.muso.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.zionhuang.innertube.YouTube
import com.muso.music.constants.AudioQuality
import com.muso.music.constants.AudioQualityKey
import com.muso.music.constants.DownloadQualityKey
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.FormatEntity
import com.muso.music.di.DownloadCache
import com.muso.music.di.PlayerCache
import com.muso.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.common.Requirements
import com.muso.music.constants.DownloadOnWifiOnlyKey
import com.muso.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec

@Singleton
class DownloadUtil @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)

    // SimpMusic-style separate download quality: downloads can pick a different stream
    // than streaming playback does.
    private val downloadQuality by enumPreference(context, DownloadQualityKey, AudioQuality.AUTO)

    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    private companion object {
        const val PRELOAD_BYTES = 3L * 1024 * 1024
    }

    /**
     * Preload next song (Echo Player and Audio): pulls roughly 3 MB of the song's
     * audio stream into the player cache through the same resolving data source the
     * player uses, so the next track starts instantly. Bounded and best-effort -
     * any failure is swallowed silently.
     */
    fun preloadSong(songId: String) {
        runCatching {
            if (playerCache.isCached(songId, 0, PRELOAD_BYTES)) return
            val dataSource = dataSourceFactory.createDataSource()
            val spec = DataSpec.Builder()
                .setUri("https://muso.internal/preload".toUri())
                .setKey(songId)
                .setPosition(0)
                .setLength(PRELOAD_BYTES)
                .build()
            val buffer = ByteArray(64 * 1024)
            dataSource.open(spec).use { length ->
                var total = 0L
                while (total < PRELOAD_BYTES) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    total += read
                }
            }
        }
    }

    /**
     * One resolving data source factory per quality choice. The streaming factory keeps
     * the URL cache shortcut; the download factory always resolves fresh so the two
     * qualities never fight over one cached URL.
     */
    private fun createDataSourceFactory(quality: () -> AudioQuality, useUrlCache: Boolean) =
        ResolvingDataSource.Factory(
            CacheDataSource.Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .build()
                    )
                )
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            if (useUrlCache) {
                songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                    return@Factory dataSpec.withUri(it.first.toUri())
                }
            }

            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
        val playerResponse = runBlocking(Dispatchers.IO) {
            YouTube.player(mediaId)
        }.getOrThrow()
        if (playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(playerResponse.playabilityStatus.reason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
        }

        val format =
            if (playedFormat != null) {
                playerResponse.streamingData?.adaptiveFormats?.find { it.itag == playedFormat.itag }
            } else {
                playerResponse.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio }
                    ?.maxByOrNull {
                        it.bitrate * when (quality()) {
                            AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                            AudioQuality.HIGH -> 1
                            AudioQuality.LOW -> -1
                        } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
                    }
            }!!.let {
                // Specify range to avoid YouTube's throttling
                it.copy(url = "${it.url}&range=0-${it.contentLength ?: 10000000}")
            }

        database.query {
            upsert(
                FormatEntity(
                    id = mediaId,
                    itag = format.itag,
                    mimeType = format.mimeType.split(";")[0],
                    codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                    bitrate = format.bitrate,
                    sampleRate = format.audioSampleRate,
                    contentLength = format.contentLength!!,
                    loudnessDb = playerResponse.playerConfig?.audioConfig?.loudnessDb
                )
            )
        }

        if (useUrlCache) {
            songUrlCache[mediaId] = format.url!! to playerResponse.streamingData!!.expiresInSeconds * 1000L
        }
        dataSpec.withUri(format.url!!.toUri())
    }

    private val dataSourceFactory = createDataSourceFactory({ audioQuality }, useUrlCache = true)
    private val downloadDataSourceFactory = createDataSourceFactory({ downloadQuality }, useUrlCache = false)
    val downloadNotificationHelper = DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)
    val downloadManager: DownloadManager = DownloadManager(context, databaseProvider, downloadCache, downloadDataSourceFactory, Executor(Runnable::run)).apply {
        maxParallelDownloads = 3
        addListener(
            ExoDownloadService.TerminalStateNotificationHelper(
                context = context,
                notificationHelper = downloadNotificationHelper,
                nextNotificationId = ExoDownloadService.NOTIFICATION_ID + 1
            )
        )
    }
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    fun getDownload(songId: String?): Flow<Download?> = downloads.map { it[songId] }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            set(download.request.id, download)
                        }
                    }
                }
            }
        )

        // Echo Player and Audio: keep downloads waiting for Wi-Fi while enabled.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            context.dataStore.data
                .map { it[DownloadOnWifiOnlyKey] ?: false }
                .distinctUntilChanged()
                .collect { wifiOnly ->
                    downloadManager.setRequirements(
                        Requirements(if (wifiOnly) Requirements.NETWORK_UNMETERED else 0),
                    )
                }
        }
    }
}