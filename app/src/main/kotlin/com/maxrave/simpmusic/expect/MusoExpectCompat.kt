package com.maxrave.simpmusic.expect

import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

/**
 * Muso port of SimpMusic's platform-level helpers used by the player suite.
 */
private fun appContext(): Context? = com.maxrave.simpmusic.ui.component.SuiteRes.context

fun copyToClipboard(label: String, text: String) {
    val context = appContext() ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

fun shareUrl(title: String, url: String) {
    val context = appContext() ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_TITLE, title)
    }
    context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun saveImageToDevice(bytes: ByteArray, fileName: String): Boolean {
    val context = appContext() ?: return false
    return try {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SimpMusic",
        )
        if (!dir.exists()) dir.mkdirs()
        File(dir, fileName).writeBytes(bytes)
        true
    } catch (e: Exception) {
        false
    }
}

fun shareImage(bytes: ByteArray, fileName: String, chooserTitle: String): Boolean {
    val context = appContext() ?: return false
    return try {
        val shareDir = File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val file = File(shareDir, fileName).apply { writeBytes(bytes) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
