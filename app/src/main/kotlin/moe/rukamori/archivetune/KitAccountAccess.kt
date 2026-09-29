/*
 * ArchiveTune (2026)
 * (c) Rukamori - github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * Muso port: ArchiveTune keeps this on App.Companion; Muso's Application is
 * com.muso.music.App, so the kit's AccountSettings imports it from here.
 */

package moe.rukamori.archivetune

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.utils.clearPlaybackAuthSession
import moe.rukamori.archivetune.utils.clearPlaybackWebAuthSession
import moe.rukamori.archivetune.utils.dataStore
import androidx.datastore.preferences.core.edit

fun forgetAccount(
    context: Context,
    clearWebAuthSession: Boolean = true,
) {
    if (clearWebAuthSession) {
        clearPlaybackWebAuthSession(context)
    }
    CoroutineScope(Dispatchers.IO).launch {
        context.dataStore.edit { settings ->
            settings.clearPlaybackAuthSession()
        }
    }
}
