package com.maxrave.data.io

import android.net.Uri
import com.maxrave.simpmusic.ui.component.SuiteRes

/**
 * Reads the raw bytes of a local image for the suite's thumbnail-crop flow.
 * Muso port: plain ContentResolver read on the application context.
 */
suspend fun readLocalImageBytes(uri: String): ByteArray? {
    val context = SuiteRes.context ?: return null
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
    }.getOrNull()
}
