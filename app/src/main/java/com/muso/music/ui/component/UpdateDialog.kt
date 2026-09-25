package com.muso.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.muso.music.R
import com.muso.music.utils.Updater
import com.muso.music.utils.UpdateDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * In-app update flow: shows the new version, downloads the release APK through the system
 * DownloadManager (with progress) and hands the file to the package installer. After the user
 * confirms the install, [com.muso.music.receivers.UpdateReceiver] relaunches the app.
 */
@Composable
fun UpdateDialog(
    version: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var progress by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(UpdateState.IDLE) }

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

    AlertDialog(
        onDismissRequest = {
            if (state != UpdateState.DOWNLOADING) onDismiss()
        },
        title = { Text(stringResource(R.string.update_available)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.update_available_desc, version),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state == UpdateState.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                UpdateState.IDLE, UpdateState.FAILED -> {
                    TextButton(
                        onClick = {
                            state = UpdateState.RESOLVING
                        }
                    ) { Text(stringResource(R.string.update_download)) }
                }
                UpdateState.READY -> {
                    TextButton(
                        onClick = {
                            UpdateDownloader.install(context)
                        }
                    ) { Text(stringResource(R.string.update_install)) }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (state != UpdateState.DOWNLOADING) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
            }
        }
    )

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
}

private enum class UpdateState {
    IDLE, RESOLVING, DOWNLOADING, READY, FAILED
}
