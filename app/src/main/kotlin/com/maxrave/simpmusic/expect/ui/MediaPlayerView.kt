package com.maxrave.simpmusic.expect.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.media3.ui.MediaPlayerView
import com.maxrave.media3.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun MediaPlayerView(
    url: String,
    modifier: Modifier,
    cropToBounds: Boolean = false,
) {
    MediaPlayerView(
        modifier = modifier,
        context = LocalContext.current,
        density = LocalDensity.current,
        url = url,
        screenSize = getScreenSizeInfo(),
        cropToBounds = cropToBounds,
    )
}

// Fully qualified: the media3-ui helper shares this function's name and signature.
@Composable
fun rememberVideoAspectRatio(playerName: String): Float? = com.maxrave.media3.ui.rememberVideoAspectRatio(playerName)

@Composable
fun MediaPlayerViewWithSubtitle(
    modifier: Modifier,
    playerName: String,
    cropToBounds: Boolean = false,
    shouldPip: Boolean,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean,
    isInPipMode: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics?,
    translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
) {
    MediaPlayerViewWithSubtitle(
        playerName = playerName,
        modifier = modifier,
        cropToBounds = cropToBounds,
        shouldShowSubtitle = shouldShowSubtitle,
        shouldPip = shouldPip,
        shouldScaleDownSubtitle = shouldScaleDownSubtitle,
        timelineState = timelineState,
        lyricsData = lyricsData,
        translatedLyricsData = translatedLyricsData,
        context = LocalContext.current,
        activity = (LocalActivity.current as? ComponentActivity)
            ?: (LocalContext.current.findActivity() as? ComponentActivity)
            ?: error("MediaPlayerView requires a ComponentActivity"),
        isInPipMode = isInPipMode,
        mainTextStyle = typo().bodyLarge,
        translatedTextStyle = typo().bodyMedium,
    )
}