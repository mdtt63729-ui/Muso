package com.muso.music.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the app update through the system DownloadManager and opens the installer.
 *
 * The download lands in the app's external files dir (Download/Muso-update.apk) so no storage
 * permission is needed, and is shared with the installer through the app's FileProvider.
 */
object UpdateDownloader {
    const val UPDATE_FILE_NAME = "Muso-update.apk"

    private var downloadId = -1L

    fun startDownload(context: Context, url: String): Boolean {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            if (downloadId != -1L) {
                dm.remove(downloadId)
            }
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Muso update")
                .setDescription("Downloading Muso update")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, UPDATE_FILE_NAME)
            downloadId = dm.enqueue(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Download progress in percent, or:
     * -1 when finished (or not started), -2 when failed/paused.
     */
    fun queryProgress(context: Context): Int {
        if (downloadId == -1L) return -1
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            dm.query(query).use { cursor ->
                if (!cursor.moveToFirst()) return -1
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> -1
                    DownloadManager.STATUS_FAILED, DownloadManager.STATUS_PAUSED -> -2
                    else -> {
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        if (total > 0) (done * 100 / total).toInt() else 0
                    }
                }
            }
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Opens the system package installer for the downloaded APK. Requires the user to have
     * granted "install unknown apps" for Muso when prompted (Android 8+).
     */
    fun install(context: Context): Boolean {
        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_FILE_NAME)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(context, context.packageName + ".FileProvider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
