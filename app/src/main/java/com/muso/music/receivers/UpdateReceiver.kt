package com.muso.music.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Relaunches the app after it was updated: MY_PACKAGE_REPLACED is delivered to the freshly
 * installed package, so starting the launch intent here brings Muso straight back up. On Android
 * versions that block background activity starts, the system installer's "Open" button still
 * works — this receiver simply makes the common case automatic.
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        try {
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        } catch (_: Exception) {
        }
    }
}
