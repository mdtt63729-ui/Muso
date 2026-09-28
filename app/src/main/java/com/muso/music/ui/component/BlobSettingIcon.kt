package com.muso.music.ui.component

/*
 * BlobSettingIcon - the settings icon system (paste-1-31 PRD): every setting
 * icon sits inside a soft, organic, glassmorphic blob container instead of a
 * bare stock Material glyph.
 *
 *  - organic silhouette with a subtle bottom-center notch (reference look)
 *  - dark translucent surface with a very subtle purple/gray tint
 *  - thin semi-transparent border, diffuse soft shadow, inner top highlight
 *  - clean white icon, optically centred, consistent stroke language
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

private val BlobSurface = Color(0xCC1E1B26)
private val BlobBorder = Color(0x26FFFFFF)
private val BlobHighlight = Color(0x1FADB8C8)

/** Organic blob silhouette: an 8-point closed smooth path, bottom point
 *  pushed in - the notch from the reference screenshots. */
private fun blobPath(w: Float, h: Float): Path {
    val cx = w / 2f
    val cy = h / 2f
    val radii = floatArrayOf(0.50f, 0.53f, 0.49f, 0.52f, 0.50f, 0.53f, 0.40f, 0.52f)
    val angles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
    val pts = Array(8) { i ->
        val a = Math.toRadians(angles[i].toDouble())
        Offset(
            cx + (radii[i] * w / 2f * Math.cos(a)).toFloat(),
            cy + (radii[i] * h / 2f * Math.sin(a)).toFloat(),
        )
    }
    val path = Path()
    val mid = { a: Offset, b: Offset -> Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f) }
    val start = mid(pts[7], pts[0])
    path.moveTo(start.x, start.y)
    for (i in 0 until 8) {
        val cur = pts[i]
        val next = pts[(i + 1) % 8]
        val m = mid(cur, next)
        path.cubicTo(cur.x, cur.y, cur.x, cur.y, m.x, m.y)
    }
    path.close()
    return path
}

@Composable
fun BlobSettingIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(44.dp),
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            val path = blobPath(size.width, size.height)
            // Diffuse soft shadow below the blob (native paint, no blur param).
            drawIntoCanvas { c ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(48, 0, 0, 0)
                    setShadowLayer(7f, 0f, 3f, android.graphics.Color.argb(48, 0, 0, 0))
                }
                c.nativeCanvas.drawPath(path.asAndroidPath(), paint)
            }
            drawPath(path, color = BlobSurface)
            // Inner top highlight: a soft arc along the upper edge.
            drawRoundRect(
                color = BlobHighlight,
                topLeft = Offset(size.width * 0.22f, size.height * 0.10f),
                size = Size(size.width * 0.56f, size.height * 0.20f),
                cornerRadius = CornerRadius(size.height * 0.10f, size.height * 0.10f),
            )
            drawPath(path, color = BlobBorder, style = Stroke(width = 1.2f))
        }
        Icon(painter = painter, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}
