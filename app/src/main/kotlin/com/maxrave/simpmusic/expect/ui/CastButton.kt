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

/** A Cast receiver playback can be handed to. Muso has no Cast, so one is never produced. */
@androidx.compose.runtime.Immutable
data class CastReceiver(
    val id: String,
    val name: String,
    val isConnected: Boolean,
)

/** The receivers on the network, plus the two things a picker does with them. */
@androidx.compose.runtime.Stable
class CastReceivers(
    val receivers: List<CastReceiver>,
    val connect: (id: String) -> Unit,
    val disconnect: () -> Unit,
)

/**
 * Muso port: there is no Cast support, so there are never any receivers - always empty, exactly
 * as [isPlatformCastAvailable] is always false. The output picker shows local devices only.
 */
@Composable
fun rememberCastReceivers(discover: Boolean): CastReceivers =
    androidx.compose.runtime.remember {
        CastReceivers(receivers = emptyList(), connect = {}, disconnect = {})
    }
