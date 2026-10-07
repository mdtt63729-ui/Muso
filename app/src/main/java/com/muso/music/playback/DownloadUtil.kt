package com.muso.music.playback

import android.content.Context
import java.io.FileOutputStream
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
import com.muso.music.constants.itagPreference
import com.muso.music.constants.DownloadQualityKey
import com.muso.music.constants.VideoQuality
import com.muso.music.constants.VideoQualityKey
import com.muso.music.db.MusicDatabase
import com.muso.music.db.entities.FormatEntity
import com.muso.music.db.entities.LyricsEntity
import com.muso.music.di.DownloadCache
import com.muso.music.di.PlayerCache
import com.muso.music.models.toMediaMetadata
import com.muso.music.utils.enumPreference
import moe.rukamori.archivetune.storage.StorageFolderKind
import moe.rukamori.archivetune.storage.StorageLocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.exoplayer.scheduler.Requirements
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
    private val lyricsHelper: com.muso.music.lyrics.LyricsHelper,
    private val artworkRepository: moe.rukamori.archivetune.downloads.DownloadedArtworkRepository,
) {
    private val appContext = context
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.HIGH_OPUS)

    // SimpMusic-style separate download quality: downloads can pick a different stream
    // than streaming playback does.
    private val downloadQuality by enumPreference(context, DownloadQualityKey, AudioQuality.MEDIUM)
    private val videoQuality by enumPreference(context, VideoQualityKey, VideoQuality.Q720)
    private val videoDownloadJobs = ConcurrentHashMap.newKeySet<String>()

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
            try {
                dataSource.open(spec)
                var total = 0L
                while (total < PRELOAD_BYTES) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    if (read > 0) total += read
                }
            } finally {
                runCatching { dataSource.close() }
            }
        }
    }

    /**
     * One resolving data source factory per quality choice. The streaming factory keeps
     * the URL cache shortcut; the download factory always resolves fresh so the two
     * qualities never fight over one cached URL.
     */
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cachingSongs = ConcurrentHashMap.newKeySet<String>()

    /**
     * Full-song cache-on-play (user request): pulls the ENTIRE audio stream of
     * [songId] into the player cache through the SAME resolving data source
     * the player streams with, so the cached bytes are byte-for-byte what
     * playback itself reads (same itag, same key). After this completes the
     * song is playable offline and appears in the cache. Songs already fully
     * cached or already downloaded are skipped; one job per song at a time.
     */
    fun cacheSong(songId: String) {
        if (alreadyFullyCached(songId) || !cachingSongs.add(songId)) {
            return
        }
        cacheScope.launch {
            try {
                runCatching {
                    val dataSource = dataSourceFactory.createDataSource()
                    val spec = DataSpec.Builder()
                        .setUri("https://muso.internal/cache".toUri())
                        .setKey(songId)
                        .setPosition(0)
                        .build()
                    val buffer = ByteArray(64 * 1024)
                    try {
                        dataSource.open(spec)
                        while (true) {
                            val read = dataSource.read(buffer, 0, buffer.size)
                            if (read == C.RESULT_END_OF_INPUT) break
                        }
                    } finally {
                        runCatching { dataSource.close() }
                    }
                }
                // Offline extras (user request): once the audio is fully
                // cached, the song's high-quality thumbnail and its lyrics are
                // saved too, so a cached song is completely usable offline.
                runCatching { cacheArtworkForSong(songId) }
                runCatching { fetchLyricsForSong(songId) }
            } finally {
                cachingSongs.remove(songId)
            }
        }
    }

    private fun deleteDownloadedCanvasVideo(songId: String) {
        runCatching {
            val directory = StorageLocationRepository.cacheDirectory(appContext, StorageFolderKind.DOWNLOADS)
                .resolve("canvas")
            directory.listFiles()
                ?.filter { it.isFile && it.nameWithoutExtension == songId }
                ?.forEach { it.delete() }
        }
    }

    /**
     * If this song has actually rendered a canvas/video frame, download the same
     * video alongside the audio download. The video is written into the user's
     * configured Downloads/canvas directory and is later preferred offline, so
     * playback never needs the network for that visual.
     *
     * This is opt-in through an explicit song download and is therefore separate
     * from the normal streaming cache: merely watching a video does not create a
     * permanent video download.
     */
    fun downloadRenderedCanvasVideo(songId: String) {
        if (songId.isBlank() || !RenderedCanvasVideoStore.wasRendered(songId)) return
        if (!videoDownloadJobs.add(songId)) return
        cacheScope.launch(Dispatchers.IO) {
            try {
                downloadRenderedCanvasVideoInternal(songId)
            } finally {
                videoDownloadJobs.remove(songId)
            }
        }
    }

    private suspend fun downloadRenderedCanvasVideoInternal(songId: String) {
        if (!RenderedCanvasVideoStore.wasRendered(songId)) return
        val directory = StorageLocationRepository.cacheDirectory(appContext, StorageFolderKind.DOWNLOADS)
            .resolve("canvas")
        directory.mkdirs()

        val existing = directory.listFiles()
            ?.firstOrNull { it.isFile && it.nameWithoutExtension == songId && it.length() > 0L }
        if (existing != null) return

        val response = YouTube.player(songId).getOrNull() ?: return
        val streamingData = response.streamingData ?: return
        val targetHeight = when (videoQuality) {
            VideoQuality.Q360 -> 360
            VideoQuality.Q720 -> 720
            VideoQuality.Q1080 -> 1080
        }
        val adaptive = streamingData.adaptiveFormats.orEmpty()
            .filter { !it.url.isNullOrBlank() && !it.isAudio && (it.height ?: 0) >= targetHeight }
        val muxed = streamingData.formats.orEmpty()
            .filter { !it.url.isNullOrBlank() && (it.height ?: 0) >= targetHeight }
        val format = adaptive.minByOrNull { it.height ?: Int.MAX_VALUE }
            ?: muxed.minByOrNull { it.height ?: Int.MAX_VALUE }
            ?: adaptive.maxByOrNull { it.height ?: 0 }
            ?: muxed.maxByOrNull { it.height ?: 0 }
            ?: return
        val url = format.url ?: return
        val mime = format.mimeType.substringBefore(';').lowercase()
        val extension = if (mime.contains("webm")) "webm" else "mp4"
        val target = directory.resolve("$songId.$extension")
        val partial = directory.resolve(".$songId.$extension.part")

        val client = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { responseBody ->
            if (!responseBody.isSuccessful) return
            val body = responseBody.body ?: return
            partial.delete()
            body.byteStream().use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
        }
        if (partial.length() <= 0L) {
            partial.delete()
            return
        }
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            partial.delete()
        }
    }

    /**
     * Cache artwork using the thumbnail URL supplied by the caller. This is intentionally
     * independent of the Room song row: SimpMusic-side download requests can be created before
     * the main Muso database has a corresponding SongEntity. In that case waiting for the DB
     * would produce a perfectly downloaded audio file with no offline artwork.
     */
    suspend fun cacheArtworkForDownload(songId: String, thumbnailUrl: String?) {
        runCatching {
            val directUrl = thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
            if (directUrl != null) {
                artworkRepository.cache(
                    mediaId = songId,
                    sourceUrls = listOf(directUrl),
                )
            } else {
                cacheArtworkForSong(songId)
            }
        }
    }

    private suspend fun cacheArtworkForSong(songId: String) {
        runCatching {
            val song = database.song(songId).first() ?: return
            artworkRepository.cache(
                mediaId = songId,
                sourceUrls = listOfNotNull(
                    song.song.thumbnailUrl,
                    song.album?.thumbnailUrl,
                    "https://i.ytimg.com/vi/$songId/maxresdefault.jpg",
                ),
            )
        }
    }

    /**
     * Round 193: whole-file cache probe that actually fires. The old guard used
     * Long.MAX_VALUE alone, which no real (finite) cache span satisfies - so
     * every play of an already fully-cached or downloaded song re-pulled the
     * whole stream. The recorded content length gives a real span to probe
     * (whole-file first, then first-byte + last-byte), with the unbounded probe
     * kept as a fallback.
     */
    private fun alreadyFullyCached(songId: String): Boolean {
        if (downloadCache.isCached(songId, 0, Long.MAX_VALUE) ||
            playerCache.isCached(songId, 0, Long.MAX_VALUE)
        ) {
            return true
        }
        val recordedLength =
            runCatching {
                runBlocking(Dispatchers.IO) { database.format(songId).first() }
            }.getOrNull()?.contentLength ?: -1L
        if (recordedLength <= 0L) return false
        return downloadCache.isCached(songId, 0, recordedLength) ||
            playerCache.isCached(songId, 0, recordedLength) ||
            (
                downloadCache.isCached(songId, 0, 1L) &&
                    downloadCache.isCached(songId, recordedLength - 1L, 1L)
                ) ||
            (
                playerCache.isCached(songId, 0, 1L) &&
                    playerCache.isCached(songId, recordedLength - 1L, 1L)
                )
    }

    /**
     * Lyrics travel with the download (user request): once a download completes,
     * its lyrics are fetched and stored in the local database, so an offline
     * song carries offline lyrics too. Best-effort and silent on failure.
     */
    private suspend fun fetchLyricsForSong(songId: String) {
        runCatching {
            // Offline: skip entirely. Writing LYRICS_NOT_FOUND while offline
            // would permanently mark a song as lyric-less even once the
            // network returns.
            if (!com.muso.music.utils.isInternetAvailable(appContext)) return
            val song = database.song(songId).first() ?: return
            if (database.lyrics(songId).first() == null) {
                val lyrics = lyricsHelper.getLyrics(song.toMediaMetadata())
                database.query { upsert(LyricsEntity(songId, lyrics)) }
            }
        }
    }

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
            // Offline downloads (user request): a song that is FULLY cached in
            // the player cache must resolve without any network at all, so the
            // check has to cover the entire remaining content. The old 1-byte
            // probe accepted partially cached songs too, which then fell off
            // the cached span mid-download onto a bogus internal URI and
            // failed; and a full check also lets fully cached songs download
            // with the network completely off.
            val length = if (dataSpec.length >= 0) dataSpec.length else Long.MAX_VALUE

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
            YouTube.player(
                mediaId,
                requireHighQuality = downloadQuality == AudioQuality.HIGH_OPUS ||
                    downloadQuality == AudioQuality.HIGH_AAC,
            )
        }.getOrThrow()
        if (playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(playerResponse.playabilityStatus.reason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
        }

        val format =
            if (playedFormat != null && downloadQuality.itagPreference().contains(playedFormat.itag)) {
                playerResponse.streamingData?.adaptiveFormats?.find { it.itag == playedFormat.itag }
            } else {
                playerResponse.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio }
                    ?.let { audio ->
                        // SimpMusic quality system: exact itag with the
                        // high-quality twin fallback, highest bitrate last.
                        quality().itagPreference()
                            .firstNotNullOfOrNull { wantedItag ->
                                audio.find { it.itag == wantedItag }
                            }
                            ?: audio.maxByOrNull { it.bitrate }
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

    /**
     * media3 reports download progress several times a second per active download.
     * Only republish when something the UI actually shows has moved - the state, or
     * the whole-percent progress. Every other tick is a duplicate that would copy the
     * whole map and recompose every download row and button.
     */
    private fun shouldPublishDownload(previous: Download?, next: Download): Boolean {
        if (previous == null) return true
        if (previous.state != next.state) return true
        if (next.state != Download.STATE_DOWNLOADING) return true
        val oldPercent = previous.percentDownloaded
        val newPercent = next.percentDownloaded
        val unset = androidx.media3.common.C.PERCENTAGE_UNSET.toFloat()
        if (oldPercent == unset || newPercent == unset) {
            return true
        }
        return oldPercent.toInt() != newPercent.toInt()
    }

    init {
        // Reading the persisted download index touches the download-index database.
        // DownloadUtil is a @Singleton injected into MainActivity, so this init ran on
        // the main thread and blocked startup. The read - and the artwork retention
        // that depends on its result - now happen on cacheScope.
        cacheScope.launch {
            val result = mutableMapOf<String, Download>()
            runCatching {
                val cursor = downloadManager.downloadIndex.getDownloads()
                while (cursor.moveToNext()) {
                    result[cursor.download.request.id] = cursor.download
                }
            }
            downloads.value = result

            // Offline artwork (user request): keep a high-quality thumbnail on
            // disk for every downloaded OR fully player-cached song so thumbnails
            // survive with no network. Existing files are kept as-is, so this only
            // performs actual downloads for songs that don't have artwork yet.
            runCatching {
                val downloadedIds = result.keys
                // Failure-safe: if the player cache cannot be listed, skip the
                // retention pass entirely instead of pruning cached songs' artwork.
                val fullyCachedIds = runCatching {
                    playerCache.keys.filter { id -> playerCache.isCached(id, 0, Long.MAX_VALUE) }
                }.getOrNull()
                if (fullyCachedIds != null) {
                    runCatching {
                        artworkRepository.retainForDownloads(downloadedIds + fullyCachedIds)
                    }
                }
                (downloadedIds.filter { id -> result[id]?.state == Download.STATE_COMPLETED } + (fullyCachedIds ?: emptyList()))
                    .distinct()
                    .forEach { songId ->
                        runCatching { cacheArtworkForSong(songId) }
                    }
            }
        }
        cacheScope.launch {
            RenderedCanvasVideoStore.renderedIds.collect { renderedIds ->
                renderedIds.forEach { songId ->
                    if (downloads.value[songId]?.state == Download.STATE_COMPLETED) {
                        downloadRenderedCanvasVideo(songId)
                    }
                }
            }
        }
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    if (!shouldPublishDownload(downloads.value[download.request.id], download)) return
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            set(download.request.id, download)
                        }
                    }
                    if (download.state == Download.STATE_COMPLETED) {
                        // Offline extras: artwork, lyrics, and (only when this song
                        // really rendered a video) the rendered video travel with
                        // the downloaded song.
                        downloadRenderedCanvasVideo(download.request.id)
                        cacheScope.launch {
                            cacheArtworkForSong(download.request.id)
                            fetchLyricsForSong(download.request.id)
                        }
                    }
                }

                override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                    deleteDownloadedCanvasVideo(download.request.id)
                    downloads.update { map ->
                        map - download.request.id
                    }
                    // Keep the artwork when the audio is still fully present in
                    // the player cache - only drop it when the song is gone for
                    // good.
                    cacheScope.launch {
                        val stillFullyCached =
                            runCatching { playerCache.isCached(download.request.id, 0, Long.MAX_VALUE) }.getOrDefault(false)
                        if (!stillFullyCached) {
                            runCatching { artworkRepository.remove(download.request.id) }
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