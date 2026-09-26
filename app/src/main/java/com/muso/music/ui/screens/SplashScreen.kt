package com.muso.music.ui.screens

import com.muso.music.R
import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.muso.music.constants.ReducedMotionKey
import com.muso.music.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Ultra-premium morphing splash (PRD: Echo Nightly-inspired motion, waveform logo hero).
 *
 * The uploaded 5-bar waveform logo is the ONLY element: pure black screen, the logo
 * materializes, a pulse travels through the bars (center first, outer bars respond with
 * a 35 ms-per-bar delay), the bars briefly read as one fluid waveform, then everything
 * reconstructs into the exact original logo, settles, and cross-fades into the app.
 *
 * Everything is drawn in a single Compose Canvas (GPU-accelerated, no bitmaps, no
 * network, no video), and every bar parameter is a pure function of the master clock,
 * driven by one centralized state machine.
 */

// Plays once per process; survives rotation because the process keeps it.
internal var splashAlreadyShown = false

// ---------- Timeline (seconds); ideal total ~1.95 s ----------
private const val T_REVEAL_START = 0.15f
private const val T_REVEAL_END = 0.40f
private const val T_COMPRESS_END = 0.62f
private const val T_PULSE_END = 1.20f
private const val T_REBUILD_END = 1.50f
private const val T_SETTLE_END = 1.70f
private const val T_EXIT_END = 1.95f

/** After the animation completes, the whole overlay fades out over this duration
 * so the home screen appears through a smooth transition, never a hard cut. */
private const val SPLASH_HANDOFF = 0.25f
private const val WAVE_DELAY = 0.035f

// ---------- Original logo geometry (heights relative to the center bar) ----------
private const val BAR_COUNT = 5
private val BAR_HEIGHTS = floatArrayOf(0.42f, 0.68f, 1.00f, 0.72f, 0.52f)

// Per-bar vertical gradients: cyan, blue, lavender, purple, pink (top / bottom).
private val BAR_TOP = intArrayOf(
    0xFF6FE6FF.toInt(), 0xFF6B8DFF.toInt(), 0xFFB49CFF.toInt(), 0xFFE86BFF.toInt(), 0xFFFFA5DE.toInt(),
)
private val BAR_BOTTOM = intArrayOf(
    0xFF1EB6FF.toInt(), 0xFF2E5CFF.toInt(), 0xFF3D46E8.toInt(), 0xFFA24BFF.toInt(), 0xFFF86BC8.toInt(),
)

// ---------- Controlled glow levels ----------
private const val GLOW_NORMAL = 0.15f
private const val GLOW_PULSE = 0.30f
private const val GLOW_PEAK = 0.38f
private const val GLOW_SETTLE = 0.12f

// ---------- Centralized state machine ----------
internal enum class SplashPhase {
    SPLASH_INIT, LOGO_REVEAL, WAVE_MORPH, PEAK_PULSE, LOGO_REBUILD, LOGO_SETTLE, SPLASH_EXIT, APP_READY
}

internal fun phaseOf(t: Float): SplashPhase = when {
    t < T_REVEAL_START -> SplashPhase.SPLASH_INIT
    t < T_REVEAL_END -> SplashPhase.LOGO_REVEAL
    t < T_PULSE_END -> SplashPhase.WAVE_MORPH
    t < T_REBUILD_END -> SplashPhase.LOGO_REBUILD
    t < T_SETTLE_END -> SplashPhase.LOGO_SETTLE
    t < T_EXIT_END -> SplashPhase.SPLASH_EXIT
    else -> SplashPhase.APP_READY
}

private class BarAnim(
    val height: Float,
    val width: Float,
    val dx: Float,
    val alpha: Float,
    val glow: Float,
    val colorShift: Float,
)

private class SplashFrame(
    val logoAlpha: Float,
    val logoScale: Float,
    val rotationDeg: Float,
    val bars: List<BarAnim>,
    val phase: SplashPhase,
)

// ---------- Easing ----------
private fun pr(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

/** Cubic bezier (0.22, 1.0, 0.36, 1.0) via bisection - smooth Material-style morph curve. */
private fun easeBezier(p: Float): Float {
    if (p <= 0f) return 0f
    if (p >= 1f) return 1f
    val x1 = 0.22f
    val y1 = 1.0f
    val x2 = 0.36f
    val y2 = 1.0f
    fun bx(s: Float) = 3f * (1 - s) * (1 - s) * s * x1 + 3f * (1 - s) * s * s * x2 + s * s * s
    fun by(s: Float) = 3f * (1 - s) * (1 - s) * s * y1 + 3f * (1 - s) * s * s * y2 + s * s * s
    var lo = 0f
    var hi = 1f
    var s = p
    repeat(20) {
        val x = bx(s)
        when {
            abs(x - p) < 0.0005f -> return by(s)
            x < p -> lo = s
            else -> hi = s
        }
        s = (lo + hi) / 2f
    }
    return by(s)
}

/** Pulse envelope for a bar: center first, each step of distance delayed by ~70 ms. */
private fun pulseEnv(t: Float, i: Int): Float {
    val d = abs(i - 2)
    val pw = pr(t, 0.72f + d * 0.07f, T_PULSE_END + d * 0.05f)
    return if (pw <= 0f || pw >= 1f) 0f else sin(pw * PI).toFloat()
}

/** All five bars' parameters as pure functions of the master clock. */
private fun barAt(t: Float, i: Int): BarAnim {
    val d = abs(i - 2)
    val rv = easeBezier(pr(t, T_REVEAL_START + i * WAVE_DELAY * 0.5f, T_REVEAL_END))
    val c = easeBezier(pr(t, T_REVEAL_END, T_COMPRESS_END))
    val env = pulseEnv(t, i)
    val r = easeBezier(pr(t, T_PULSE_END + d * WAVE_DELAY, T_REBUILD_END + d * WAVE_DELAY))

    // Phase B compression: outer bars shrink, center grows slightly, everything pulls
    // a touch toward the middle so the wave reads as one organism.
    val compTarget = when (d) {
        0 -> 1.12f
        1 -> 0.74f
        else -> 0.58f
    }
    val addFactor = when (d) {
        0 -> 0.38f
        1 -> 0.12f
        else -> 0.08f
    }

    // Height: reveal -> compression -> pulse bump -> rebuild (+ tiny overshoot).
    val height = lerp(lerp(0.88f, 1f, rv) * lerp(1f, compTarget, c), 1f, r) +
        env * addFactor +
        sin(r * PI.toFloat()) * 0.03f

    // Width: slightly thicker while compressed / pulsing, back to original on rebuild.
    val width = lerp(0.85f, 1f, rv) *
        lerp(1f, 1.10f, c * (1f - r)) *
        lerp(1f, 1.05f, env * (1f - r))

    // Horizontal: pull toward center while compressed, tiny outward push on pulse.
    val dx = (-0.055f * (i - 2) * c * (1f - r)) + (0.012f * (i - 2) * env)

    // Glow: subtle normally, stronger at the pulse, settling down at the end.
    val glow = lerp(
        GLOW_NORMAL + env * (if (d == 0) GLOW_PEAK - GLOW_NORMAL else GLOW_PULSE - GLOW_NORMAL),
        GLOW_SETTLE,
        r,
    )

    // Very slow color movement toward the neighbour during the pulse.
    val colorShift = env * 0.12f * (1f - r)

    return BarAnim(height, width, dx, rv, glow, colorShift)
}

private fun frameAt(t: Float): SplashFrame {
    val revealGlobal = easeBezier(pr(t, T_REVEAL_START, T_REVEAL_END))
    val exitE = pr(t, T_SETTLE_END, T_EXIT_END)
    val rebuildGlobal = easeBezier(pr(t, T_PULSE_END, T_REBUILD_END))
    val centerEnv = pulseEnv(t, 2)
    return SplashFrame(
        logoAlpha = revealGlobal * (1f - exitE),
        logoScale = lerp(0.88f, 1f, revealGlobal) *
            lerp(1f, 0.96f, exitE) *
            (1f + sin(rebuildGlobal * PI.toFloat()) * 0.02f),
        rotationDeg = 0.6f * centerEnv * (1f - rebuildGlobal),
        bars = (0 until BAR_COUNT).map { barAt(t, it) },
        phase = phaseOf(t),
    )
}

/** Reduced motion: simple fade in, static waveform, fade out. */
private fun reducedFrameAt(t: Float): SplashFrame {
    val alpha = pr(t, 0.05f, 0.35f) * (1f - pr(t, 0.90f, 1.20f))
    return SplashFrame(
        logoAlpha = alpha,
        logoScale = 1f,
        rotationDeg = 0f,
        bars = (0 until BAR_COUNT).map { BarAnim(1f, 1f, 0f, 1f, GLOW_SETTLE * 0.6f, 0f) },
        phase = phaseOf(t),
    )
}

@Composable
internal fun MusoSplash(onFinish: () -> Unit) {
    val context = LocalContext.current
    val (reducedMotionPref) = rememberPreference(ReducedMotionKey, defaultValue = false)
    val animatorScale = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }
    val reduced = reducedMotionPref || animatorScale == 0f

    // On Android 12+ the system splash has ALREADY played the bars rising
    // (windowSplashScreenAnimatedIcon) while the process started - the custom
    // animation continues from there instead of replaying the reveal, so the
    // handoff between the two reads as one continuous animation.
    val initialT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.40f else 0f
    var t by remember { mutableFloatStateOf(initialT) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        val total = if (reduced) 1.20f else T_EXIT_END + SPLASH_HANDOFF
        while (true) {
            withFrameNanos { now ->
                t = initialT + ((now - startNanos) / 1_000_000_000f).coerceAtLeast(0f)
            }
            if (t >= total) break
        }
        onFinish()
    }

    val frame = if (reduced) reducedFrameAt(t) else frameAt(t)

    // Brand wordmark: appears under the waveform during the settle phase with a
    // subtle Material-style fade + scale, then fades out with the splash.
    val wordmarkReveal = if (reduced) pr(t, 0.05f, 0.35f) else easeBezier(pr(t, 1.35f, 1.70f))
    val wordmarkAlpha = wordmarkReveal * (1f - pr(t, T_SETTLE_END, T_EXIT_END))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .graphicsLayer {
                // Handoff: only AFTER the full animation has completed, fade the whole
                // overlay into the home screen beneath over 150ms. Before T_EXIT_END the
                // alpha stays 1 - the app can never be seen early.
                alpha = if (reduced) {
                    1f
                } else {
                    // Eased (iOS-style) reveal: home arrives quickly and settles gently.
                    1f - easeBezier(pr(t, T_EXIT_END, T_EXIT_END + SPLASH_HANDOFF))
                }
            }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWaveform(frame)
        }
        Text(
            text = "Muso",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.gochi_hand)),
            fontWeight = FontWeight.Normal,
            fontSize = 44.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp)
                .graphicsLayer {
                    alpha = wordmarkAlpha
                    val sc = 0.92f + 0.08f * wordmarkReveal
                    scaleX = sc
                    scaleY = sc
                },
        )
    }
}

private fun DrawScope.drawWaveform(frame: SplashFrame) {
    val logoW = size.minDimension * 0.62f
    val barW = logoW * 0.145f
    val gap = logoW * 0.072f
    val maxH = size.minDimension * 0.52f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val totalW = BAR_COUNT * barW + (BAR_COUNT - 1) * gap
    val startX = cx - totalW / 2f

    rotate(degrees = frame.rotationDeg, pivot = Offset(cx, cy)) {
        scale(scale = frame.logoScale, pivot = Offset(cx, cy)) {
            for (i in 0 until BAR_COUNT) {
                val a = frame.bars[i]
                if (a.alpha <= 0.01f) continue

                val w = barW * a.width
                val h = maxH * BAR_HEIGHTS[i] * a.height
                val x = startX + i * (barW + gap) + (barW - w) / 2f + a.dx * logoW
                val y = cy - h / 2f

                val next = (i + 1) % BAR_COUNT
                val top = lerp(Color(BAR_TOP[i]), Color(BAR_TOP[next]), a.colorShift)
                val bottom = lerp(Color(BAR_BOTTOM[i]), Color(BAR_BOTTOM[next]), a.colorShift)
                val topArgb = top.toArgb()
                val bottomArgb = bottom.toArgb()

                // Controlled glow: the same pill, blurred, at a low alpha.
                if (a.glow > 0.03f && frame.logoAlpha > 0.05f) {
                    drawIntoCanvas { c ->
                        val glowPaint = Paint().apply {
                            isAntiAlias = true
                            shader = LinearGradient(
                                x, y, x, y + h,
                                topArgb, bottomArgb,
                                Shader.TileMode.CLAMP,
                            )
                            maskFilter = BlurMaskFilter(barW * 0.9f, BlurMaskFilter.Blur.NORMAL)
                            alpha = (a.glow * frame.logoAlpha * 255f).toInt().coerceIn(0, 255)
                        }
                        c.nativeCanvas.drawRoundRect(
                            x, y, x + w, y + h,
                            w / 2f, w / 2f,
                            glowPaint,
                        )
                    }
                }

                // Crisp pill with the preserved cyan -> pink gradient.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(top, bottom),
                        startY = y,
                        endY = y + h,
                    ),
                    topLeft = Offset(x, y),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w / 2f, w / 2f),
                    alpha = frame.logoAlpha * a.alpha,
                )
            }
        }
    }
}
