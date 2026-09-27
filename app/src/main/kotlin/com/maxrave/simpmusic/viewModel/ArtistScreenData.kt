package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.model.browse.artist.Albums
import com.maxrave.domain.data.model.browse.artist.Related
import com.maxrave.domain.data.model.browse.artist.ResultSingle
import com.maxrave.domain.data.model.browse.artist.Singles
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.data.model.browse.album.Track

/**
 * Muso port: the artist-screen payload SimpMusic's AllExt builder produces.
 * The suite's sheets only read a few fields when navigating.
 */
data class ArtistScreenData(
    val title: String,
    val imageUrl: String?,
    val subscribers: String?,
    val playCount: String?,
    val isChannel: Boolean,
    val channelId: String?,
    val radioParam: String?,
    val shuffleParam: String?,
    val description: String?,
    val listSongParam: String?,
    val popularSongs: List<Track> = emptyList(),
    val singles: Singles? = null,
    val albums: Albums? = null,
    val video: ArtistBrowse.Videos? = null,
    val singlesAlbum: Albums? = null,
    val related: Related? = null,
    val featuredOn: List<Any> = emptyList(),
)
