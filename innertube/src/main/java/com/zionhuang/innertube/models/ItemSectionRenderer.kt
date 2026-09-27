package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

/** Wraps content renderers found in the sections of podcast show pages. */
@Serializable
data class ItemSectionRenderer(
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val musicMultiRowListItemRenderer: MusicMultiRowListItemRenderer?,
        // YouTube's flattened All-search results: one plain item per section.
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    )
}
