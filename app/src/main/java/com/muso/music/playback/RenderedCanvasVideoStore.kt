package com.muso.music.playback

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Records songs for which a real canvas/video frame was actually rendered.
 * This is deliberately separate from "a video URL exists": a URL can resolve
 * successfully while the decoder never produces a frame.
 */
object RenderedCanvasVideoStore {
    private const val PREFS = "muso_rendered_canvas_videos"
    private const val KEY_MEDIA_IDS = "rendered_media_ids"

    private val _renderedIds = MutableStateFlow<Set<String>>(emptySet())
    val renderedIds: StateFlow<Set<String>> = _renderedIds.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        val ids = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_MEDIA_IDS, emptySet())
            .orEmpty()
        _renderedIds.value = ids.toSet()
    }

    @Synchronized
    fun markRendered(context: Context, mediaId: String) {
        if (mediaId.isBlank()) return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_MEDIA_IDS, emptySet()).orEmpty().toMutableSet()
        if (!ids.add(mediaId)) return
        prefs.edit().putStringSet(KEY_MEDIA_IDS, ids).apply()
        _renderedIds.value = ids.toSet()
    }

    fun wasRendered(mediaId: String): Boolean = _renderedIds.value.contains(mediaId)
}
