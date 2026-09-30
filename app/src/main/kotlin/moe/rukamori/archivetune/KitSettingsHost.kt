/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * Muso port addition: the kit's CompositionLocals are provided HERE, around
 * each kit screen reached from NavigationBuilder — NOT in MainActivity's
 * startup composition. App startup therefore never constructs the kit
 * database, SyncUtils or DownloadUtil. If the kit object graph fails to
 * build, the settings screen shows a fallback message instead of
 * crash-looping the whole app (and the failure lands in the Muso log folder).
 */

package moe.rukamori.archivetune

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muso.music.R

@Composable
fun KitSettingsHost(content: @Composable () -> Unit) {
    // Constructed lazily here — the first time a kit screen is actually
    // opened — and never during app startup.
    val database = remember { runCatching { KitRuntimeAccess.database() } }
    val syncUtils = remember { runCatching { KitRuntimeAccess.syncUtils() } }
    val downloadUtil = remember { runCatching { KitRuntimeAccess.downloadUtil() } }

    val failure =
        database.exceptionOrNull()
            ?: syncUtils.exceptionOrNull()
            ?: downloadUtil.exceptionOrNull()
    if (failure != null) {
        // The kit graph could not be built. The rest of the app keeps
        // working; show what went wrong (it is also in main.txt).
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.kit_settings_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    failure.stackTraceToString().take(2000),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        return
    }

    // Round 173: the kit screens were getting zero insets, so their content
    // started UNDER the status bar and their trailing TopAppBar, and ended
    // under muso's floating navbar/miniplayer - settings rows were invisible
    // and the screens looked broken/unscrollable. Give them the REAL insets:
    // horizontal = system bars, top = status bar + 64dp top app bar, bottom =
    // muso's player-aware bottom (navbar + miniplayer).
    val density = androidx.compose.ui.platform.LocalDensity.current
    val musoInsets = com.muso.music.LocalPlayerAwareWindowInsets.current
    val kitInsets =
        with(density) {
            WindowInsets(
                left = WindowInsets.navigationBars.getLeft(density).toDp(),
                top = WindowInsets.statusBars.getTop(density).toDp() + 64.dp,
                right = WindowInsets.navigationBars.getRight(density).toDp(),
                bottom = musoInsets.getBottom(density).toDp(),
            )
        }

    CompositionLocalProvider(
        LocalPlayerAwareWindowInsets provides kitInsets,
        LocalPlayerConnection provides null,
        LocalAnimationsDisabled provides false,
        LocalDatabase provides database.getOrThrow(),
        LocalSyncUtils provides syncUtils.getOrThrow(),
        LocalDownloadUtil provides downloadUtil.getOrThrow(),
    ) {
        content()
    }
}
