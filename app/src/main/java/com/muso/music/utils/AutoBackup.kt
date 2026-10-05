package com.muso.music.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.muso.music.db.InternalDatabase
import com.muso.music.db.MusicDatabase
import com.muso.music.extensions.div
import com.muso.music.extensions.zipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.zip.ZipEntry

/**
 * SimpMusic-style automatic backups: writes the exact same zip the manual backup creates
 * (settings + database) into the public Downloads folder through MediaStore - no storage
 * permission needed on Android 10+, and the file is immediately visible to the user and
 * to the restore picker. Only the newest few backups are kept.
 */
object AutoBackup {
    private const val SETTINGS_FILENAME = "settings.preferences_pb"
    private const val MAX_BACKUPS = 3
    private const val BACKUP_PREFIX = "Muso_backup_"

    /**
     * @return true when the backup zip was fully written and published.
     */
    suspend fun writeBackup(context: Context, database: MusicDatabase): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.applicationContext.contentResolver
            val name = "${BACKUP_PREFIX}${System.currentTimeMillis()}.zip"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val uri = resolver.insert(collection, values)
                ?: error("Could not create backup file")

            resolver.openOutputStream(uri)?.use { out ->
                out.zipOutputStream().use { zip ->
                    runCatching {
                        (context.applicationContext.filesDir / "datastore" / SETTINGS_FILENAME)
                            .inputStream().buffered().use { input ->
                                zip.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                                input.copyTo(zip)
                            }
                    }
                    runBlocking { database.checkpoint() }
                    FileInputStream(database.openHelper.writableDatabase.path).use { input ->
                        zip.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        input.copyTo(zip)
                    }
                }
            } ?: error("Could not open backup file")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            trimOldBackups(context.applicationContext, collection)
            true
        }.getOrDefault(false)
    }

    /**
     * Best-effort cleanup: keep only the newest [MAX_BACKUPS] automatic backups.
     */
    private fun trimOldBackups(context: Context, collection: Uri) {
        runCatching {
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("${BACKUP_PREFIX}%.zip")
            val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            resolver.query(collection, projection, selection, selectionArgs, sort)?.use { cursor ->
                var index = 0
                while (cursor.moveToNext()) {
                    if (index >= MAX_BACKUPS) {
                        val id = cursor.getLong(0)
                        resolver.delete(Uri.withAppendedPath(collection, id.toString()), null, null)
                    }
                    index++
                }
            }
        }
    }
}
