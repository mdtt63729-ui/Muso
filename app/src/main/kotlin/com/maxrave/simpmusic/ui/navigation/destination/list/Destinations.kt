package com.maxrave.simpmusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

/** Muso port: the suite's typed destinations, backed by Muso's own routes. */
@Serializable
data class PodcastDestination(
    val podcastId: String,
)

@Serializable
data class BrowseDestination(
    val browseId: String,
)
