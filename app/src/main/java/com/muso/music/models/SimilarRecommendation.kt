package com.muso.music.models

import com.zionhuang.innertube.models.YTItem
import com.muso.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
