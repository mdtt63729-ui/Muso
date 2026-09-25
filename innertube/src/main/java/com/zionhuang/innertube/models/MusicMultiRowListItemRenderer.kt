package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

/**
 * List item used by podcast show pages for each episode (ReTune port, trimmed to the
 * fields Muso needs: title, subtitle runs, thumbnail and the watch endpoint).
 */
@Serializable
data class MusicMultiRowListItemRenderer(
    val title: Runs?,
    val subtitle: Runs?,
    val thumbnail: ThumbnailRenderer?,
    val onTap: NavigationEndpoint?,
)
