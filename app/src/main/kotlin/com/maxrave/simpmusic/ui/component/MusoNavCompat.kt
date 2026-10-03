package com.maxrave.simpmusic.ui.component

import androidx.navigation.NavHostController
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination

/**
 * Muso port: the suite navigates with SimpMusic's typed destination objects;
 * these overloads translate them to Muso's own routes. They live in this
 * package on purpose - the suite's sheets resolve them without an import.
 */
fun NavHostController.navigate(destination: ArtistDestination) = navigate("artist/${destination.channelId}")

fun NavHostController.navigate(destination: AlbumDestination) = navigate("album/${destination.browseId}")

fun NavHostController.navigate(destination: PlaylistDestination) = navigate("playlist/${destination.playlistId}")

fun NavHostController.navigate(destination: PodcastDestination) = navigate("artist/${destination.podcastId}")
