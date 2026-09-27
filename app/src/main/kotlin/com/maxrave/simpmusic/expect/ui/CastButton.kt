package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Muso port: SimpMusic renders a Google Cast button here. Muso has no cast
 * support, so the button draws nothing and cast is reported unavailable -
 * every layout that gates on isPlatformCastAvailable() hides its cast UI.
 */
@Composable
fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
) { }

fun isPlatformCastAvailable(): Boolean = false
