package com.muso.music.ui.component

/*
 * Settings icon system (OpenTune reference, screenshots 580851-580853):
 * every setting icon sits in a uniform CIRCULAR charcoal container that is a
 * shade lighter than the row background, with an off-white outlined glyph -
 * and every setting row carries its OWN icon, distinct from every other row.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/** Charcoal circle, one step lighter than the row. */
private val SettingIconContainer = Color(0xFF2C2C31)
/** Off-white glyph. */
private val SettingIconTint = Color(0xFFE8E8EA)

@Composable
fun BlobSettingIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    tint: Color = SettingIconTint,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SettingIconContainer),
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
