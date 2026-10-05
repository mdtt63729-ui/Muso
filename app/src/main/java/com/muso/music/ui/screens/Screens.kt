package com.muso.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.muso.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconId: Int,
    val route: String,
) {
    object Home : Screens(R.string.home, R.drawable.home, "home")
    object Library : Screens(R.string.library, R.drawable.library_music, "library")
    object Search : Screens(R.string.search, R.drawable.search, "search")
    object Songs : Screens(R.string.songs, R.drawable.music_note, "songs")
    object Artists : Screens(R.string.artists, R.drawable.artist, "artists")
    object Albums : Screens(R.string.albums, R.drawable.album, "albums")
    object Playlists : Screens(R.string.playlists, R.drawable.queue_music, "playlists")

    companion object {
        // SimpMusic-style bottom bar: Home, Library, Search.
        // Songs/Artists/Albums/Playlists live inside the Library screen as tabs.
        val MainScreens = listOf(Home, Library, Search)
    }
}
