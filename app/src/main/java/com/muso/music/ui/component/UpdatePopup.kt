package com.muso.music.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muso.music.R
import com.muso.music.utils.Updater
import com.muso.music.utils.UpdateDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Premium Material 3 in-app update popup. Slides up from the bottom of the screen
 * (emphasized M3 easing) when a newer GitHub release exists.
 *
 * Flow:
 * - "Update now": resolves the release's APK URL and downloads it with the system
 *   DownloadManager (progress shown inline), then hands it to the package installer.
 * - "Get it on GitHub": opens the repo's releases page in the browser, for users who
 *   prefer to download the APK themselves.
 * - "Later": slides the popup back down and remembers the dismissed version in
 *   DataStore, so it will NOT appear again until the next release is published.
 */
@Composable
fun UpdatePopup(
    version: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var progress by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(UpdateState.IDLE) }

    // Entry/exit animation: composes hidden and flips visible on the first frame,
    // so AnimatedVisibility plays the slide-up entrance.
    var visible by remember { mutableStateOf(false) }
    var dismissRequested by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Smooth slide-down before actually dismissing (the caller removes us from
    // the composition, so the exit animation has to finish first).
    LaunchedEffect(dismissRequested) {
        if (dismissRequested) {
            visible = false
            delay(350)
            onDismiss()
        }
    }

    // Poll the DownloadManager while the update is coming down.
    LaunchedEffect(state) {
        if (state == UpdateState.DOWNLOADING) {
            while (true) {
                val p = withContext(Dispatchers.IO) {
                    UpdateDownloader.queryProgress(context)
                }
                when {
                    p >= 0 -> progress = p
                    p == -1 -> {
                        state = UpdateState.READY
                        UpdateDownloader.install(context)
                        return@LaunchedEffect
                    }
                    p == -2 -> {
                        state = UpdateState.FAILED
                        Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_SHORT).show()
                        return@LaunchedEffect
                    }
                }
                delay(500)
            }
        }
    }

    // Resolve the release asset and kick off the download.
    LaunchedEffect(state) {
        if (state == UpdateState.RESOLVING) {
            val result = withContext(Dispatchers.IO) {
                Updater.getLatestReleaseAssetUrl()
            }
            result.onSuccess { url ->
                if (withContext(Dispatchers.IO) { UpdateDownloader.startDownload(context, url) }) {
                    progress = 0
                    state = UpdateState.DOWNLOADING
                } else {
                    state = UpdateState.FAILED
                }
            }.onFailure {
                state = UpdateState.FAILED
                Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) +
            slideInVertically(
                animationSpec = tween(350, easing = EmphasizedEasing),
                initialOffsetY = { it },
            ),
        exit = fadeOut(animationSpec = tween(200)) +
            slideOutVertically(
                animationSpec = tween(300, easing = EmphasizedEasing),
                targetOffsetY = { it },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Dim scrim: makes it clearly modal, blocks interaction with the app
                // underneath, but never intercepts back navigation.
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(enabled = state != UpdateState.DOWNLOADING) { /* modal */ },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
                    .widthIn(max = 480.dp)
                    .clickable(enabled = false) { /* consume clicks so the scrim doesn't dismiss */ },
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.update_available),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "v$version",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_available_desc, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (state == UpdateState.DOWNLOADING) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.update_downloading) + " $progress%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = {
                                when (state) {
                                    UpdateState.READY -> UpdateDownloader.install(context)
                                    UpdateState.DOWNLOADING -> Unit
                                    else -> state = UpdateState.RESOLVING
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(
                                    when (state) {
                                        UpdateState.READY -> R.string.update_install
                                        else -> R.string.update_now
                                    },
                                ),
                            )
                        }
                        TextButton(
                            onClick = {
                                if (state != UpdateState.DOWNLOADING) {
                                    dismissRequested = true // slides down, then onDismiss()
                                }
                            },
                        ) {
                            Text(stringResource(R.string.update_later))
                        }
                    }

                    // Manual alternative: jump straight to the GitHub release page.
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(GITHUB_RELEASES_URL)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.update_get_github))
                    }
                }
            }
        }
    }
}

/** M3 emphasized easing, matching the app-wide page transition curve. */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

private const val GITHUB_RELEASES_URL = "https://github.com/mdtt63729-ui/Muso/releases/latest"

private enum class UpdateState { IDLE, RESOLVING, DOWNLOADING, READY, FAILED }
