package com.muso.music.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.muso.music.MainActivity
import com.muso.music.R

/**
 * System notification that mirrors the in-app update popup: posted whenever a new
 * GitHub release is detected - either right after the app is opened or by the
 * background [UpdateCheckWorker] (every ~15 minutes, even when Muso is closed).
 *
 * Tapping it opens Muso and force-shows the update popup (bypassing a previous
 * "Later" dismissal for that version), with the in-app DownloadManager install
 * one tap away. The notification is only shown ONCE per release version, so it
 * never spams.
 */
object UpdateNotification {
    private const val CHANNEL_ID = "muso_update"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS = "muso_updater"
    private const val KEY_LAST_NOTIFIED = "lastNotifiedVersion"

    /** Set on the launch intent so MainActivity knows to open the update popup. */
    const val EXTRA_SHOW_UPDATE = "muso.show_update"

    /**
     * Posts the update notification for [version], unless this exact version was
     * already notified before (remembered in a tiny SharedPreferences file).
     */
    fun notifyIfNew(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFIED, null) == version) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.update_notification_channel_desc)
                }
            )
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_SHOW_UPDATE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = context.getString(R.string.update_notification_text, version)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putString(KEY_LAST_NOTIFIED, version).apply()
    }
}
