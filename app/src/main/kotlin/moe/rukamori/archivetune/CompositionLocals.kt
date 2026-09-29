/*
 * ArchiveTune (2026)
 * (c) Rukamori - github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * Muso port addition: the CompositionLocals that ArchiveTune declares in its
 * MainActivity. Muso's own MainActivity hosts the kit screens, so the locals
 * live here and are provided in com.muso.music.MainActivity around the NavHost.
 */

package moe.rukamori.archivetune

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.playback.DownloadUtil
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.utils.SyncUtils

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

/** Holds the running [Application] so non-composable kit code can reach it. */
object AppInstanceHolder {
    lateinit var application: Application
}

/** Hilt entry point used by the Muso host to fetch kit singletons. */
@InstallIn(SingletonComponent::class)
@EntryPoint
interface KitEntryPoint {
    // Getter names are prefixed with `kit` — muso's own SuiteEntryPoint
    // declares database()/downloadUtil() too, and Hilt merges both entry
    // points into one component where Java cannot overload on return type.
    fun kitDatabase(): MusicDatabase
    fun kitSyncUtils(): SyncUtils
    fun kitDownloadUtil(): DownloadUtil
}

object KitRuntimeAccess {
    private fun entryPoint(): KitEntryPoint =
        EntryPointAccessors.fromApplication(AppInstanceHolder.application, KitEntryPoint::class.java)

    fun database(): MusicDatabase = entryPoint().kitDatabase()
    fun syncUtils(): SyncUtils = entryPoint().kitSyncUtils()
    fun downloadUtil(): DownloadUtil = entryPoint().kitDownloadUtil()
}
