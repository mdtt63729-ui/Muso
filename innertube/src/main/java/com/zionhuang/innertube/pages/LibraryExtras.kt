package com.zionhuang.innertube.pages

import com.zionhuang.innertube.models.Album
import com.zionhuang.innertube.models.Artist
import com.zionhuang.innertube.models.MusicMultiRowListItemRenderer
import com.zionhuang.innertube.models.MusicResponsiveListItemRenderer
import com.zionhuang.innertube.models.MusicTwoRowItemRenderer
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.splitBySeparator
import com.zionhuang.innertube.utils.parseTime

/** A saved podcast show from the user's YouTube Music library. */
data class PodcastShowItem(
    val id: String,
    val title: String,
    val author: String?,
    val thumbnail: String?,
)

/**
 * Parsers for the online library extras: saved podcasts, podcast episodes and uploaded
 * songs (ReTune port). All results are plain [SongItem]s so playback works through the
 * existing online queue path — no database changes.
 */
object LibraryExtras {
    /** Saved podcast show from a two-row grid item. */
    fun podcastShowFromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): PodcastShowItem? {
        return PodcastShowItem(
            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
            author = renderer.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.firstOrNull()?.text,
            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl(),
        )
    }

    /** Podcast episode from a multi-row list item. */
    fun episodeFromMusicMultiRowListItemRenderer(
        renderer: MusicMultiRowListItemRenderer,
        show: PodcastShowItem?,
    ): SongItem? {
        val runs = renderer.subtitle?.runs?.splitBySeparator()
        return SongItem(
            id = renderer.onTap?.watchEndpoint?.videoId ?: return null,
            title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
            artists = listOfNotNull(
                show?.author?.let { Artist(name = it, id = null) }
                    ?: runs?.firstOrNull()?.firstOrNull()?.let {
                        Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                    }
            ),
            album = show?.let { Album(name = it.title, id = it.id) },
            duration = runs?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: show?.thumbnail
                ?: return null,
            endpoint = renderer.onTap.watchEndpoint,
        )
    }

    /**
     * Podcast episode / uploaded song from a responsive list item. Uploaded songs carry
     * their video id at the top level of the renderer, not in playlistItemData.
     */
    fun songFromMusicResponsiveListItemRenderer(
        renderer: MusicResponsiveListItemRenderer,
        show: PodcastShowItem? = null,
    ): SongItem? {
        val id = renderer.videoId
            ?: renderer.playlistItemData?.videoId
            ?: return null
        val secondaryRuns = renderer.flexColumns.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.splitBySeparator()
        return SongItem(
            id = id,
            title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text
                ?: return null,
            artists = show?.author?.let { listOf(Artist(name = it, id = null)) }
                ?: secondaryRuns?.firstOrNull()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }
                ?: return null,
            album = show?.let { Album(name = it.title, id = it.id) },
            duration = renderer.fixedColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text
                ?.parseTime()
                ?: secondaryRuns?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
            thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: show?.thumbnail
                ?: return null,
            endpoint = renderer.overlay
                ?.musicItemThumbnailOverlayRenderer
                ?.content
                ?.musicPlayButtonRenderer
                ?.playNavigationEndpoint
                ?.watchEndpoint,
        )
    }
}
