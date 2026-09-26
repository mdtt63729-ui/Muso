package com.muso.music.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.muso.music.BuildConfig

/**
 * Background check for new GitHub releases, scheduled by App.onCreate and run by
 * WorkManager roughly every 15 minutes (WorkManager's minimum period; Doze may
 * defer it, which is fine). When the latest release tag differs from the running
 * version, the update notification is posted - so the user hears about a new APK
 * even while Muso is closed. Failures are swallowed: the next periodic run
 * retries anyway, and there is never anything worth crashing the app over.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val latest = Updater.getLatestVersionName(force = true).getOrNull()
            if (latest != null && latest != BuildConfig.VERSION_NAME) {
                UpdateNotification.notifyIfNew(applicationContext, latest)
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
