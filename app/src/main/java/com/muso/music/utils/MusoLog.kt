package com.muso.music.utils

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Muso on-device logging (user request):
 *
 *  - A folder named "Muso" is created on the phone. When the public storage
 *    root is writable (All Files Access granted / older Android) it lives at
 *    /storage/emulated/0/Muso; otherwise it falls back to the app's own
 *    external folder (Android/data/com.muso.music/files/Muso) which always
 *    works without any permission.
 *
 *  - crash_log.txt      — every crash appended, one entry per crash
 *  - crash_log_N.txt    — one file per crash (crash_log_1.txt, crash_log_2.txt, ...)
 *  - main.txt           — the app's complete logcat for the current run
 *                         (the biggest file; the previous run is kept as
 *                         main_previous.txt). Crashes appear here too, via
 *                         the system's AndroidRuntime logging.
 *
 *  - Crash files are ALWAYS mirrored into the app-external "Muso" folder so
 *    they can be shared through the in-app crash dialog (FileProvider) even
 *    when the public folder is not available.
 *
 * The handler is installed as the very first thing in Application.onCreate,
 * so even early crashes are captured.
 */
object MusoLog {
    private const val PREFS = "muso_crash_log"
    private const val COUNT_KEY = "crash_count"
    private const val PROMPT_KEY = "storage_prompt_done"
    private const val FILE_PROVIDER_AUTHORITY = "com.muso.music.fileprovider"

    @Volatile
    var logDir: File? = null
        private set

    @Volatile
    var usingPublicDir: Boolean = false
        private set

    /** Always-writable mirror of the crash files (app-external storage). */
    @Volatile
    private var mirrorDir: File? = null

    private var logcatProcess: Process? = null

    fun init(app: Application) {
        val public = File(Environment.getExternalStorageDirectory(), "Muso")
        var dir: File? = null
        if (canWrite(public)) {
            dir = public
            usingPublicDir = true
        }
        if (dir == null) {
            val fallback = File(app.getExternalFilesDir(null), "Muso")
            if (canWrite(fallback)) dir = fallback
        }
        logDir = dir
        mirrorDir = runCatching {
            File(app.getExternalFilesDir(null), "Muso").apply { mkdirs() }
        }.getOrNull()
        installCrashHandler(app)
        startLogcat(dir)
    }

    /** True when the "Muso" folder sits in public storage (user-visible). */
    fun isPublicDirActive(): Boolean = usingPublicDir

    /** One-time prompt flag for the All Files Access request (MainActivity). */
    fun storagePromptDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PROMPT_KEY, false)

    fun markStoragePromptDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(PROMPT_KEY, true).apply()
    }

    private fun canWrite(dir: File): Boolean = runCatching {
        if (!dir.exists() && !dir.mkdirs()) return@runCatching false
        val probe = File(dir, ".probe")
        probe.writeText("ok")
        probe.delete()
        true
    }.getOrDefault(false)

    private fun installCrashHandler(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Never let logging itself crash the crash handler.
            runCatching { writeCrash(app, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(app: Application, thread: Thread, throwable: Throwable) {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(COUNT_KEY, 0) + 1
        prefs.edit().putInt(COUNT_KEY, count).apply()

        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val report = buildString {
            appendLine("==============================================")
            appendLine("MUSO CRASH #$count")
            appendLine("Time: $ts")
            appendLine("App version: ${com.muso.music.BuildConfig.VERSION_NAME} (${com.muso.music.BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("----------------------------------------------")
            appendLine(sw.toString().trimEnd())
            appendLine()
        }

        val targets = listOfNotNull(logDir, mirrorDir)
        for (dir in targets) {
            // One file per crash: crash_log_1.txt, crash_log_2.txt, ...
            runCatching { File(dir, "crash_log_$count.txt").writeText(report) }
            // And the cumulative crash log.
            runCatching { File(dir, "crash_log.txt").appendText("\n$report") }
        }
    }

    /**
     * A share intent (used by the crash dialog) carrying every log file that
     * exists. main.txt is copied into the app-external folder first so the
     * FileProvider can reach it even when the public Muso folder holds the
     * live files. Works with no storage permission at all.
     */
    fun shareLogsIntent(context: Context): Intent {
        val uris = mutableListOf<Uri>()
        val dirs = listOfNotNull(mirrorDir, logDir).distinct()
        val names = mutableListOf("crash_log.txt")
        prefsCrashCount(context).let { n -> if (n > 0) names.add("crash_log_$n.txt") }
        names.add("main.txt")
        for (name in names) {
            // Prefer the newest copy of each file across the folders.
            val candidates = dirs.map { File(it, name) }.filter { it.isFile }
            val file =
                candidates.maxByOrNull { it.lastModified() }
                    ?: continue
            val shareable =
                if (file.parentFile == mirrorDir) {
                    file
                } else {
                    // Copy into the provider-accessible mirror dir.
                    runCatching {
                        val copy = File(mirrorDir, name)
                        file.copyTo(copy, overwrite = true)
                        copy
                    }.getOrNull() ?: file
                }
            runCatching {
                uris.add(
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        FILE_PROVIDER_AUTHORITY,
                        shareable,
                    ),
                )
            }
        }
        val intent =
            if (uris.size > 1) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
            } else if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND).apply { }
            }
        intent.apply {
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Muso ${com.muso.music.BuildConfig.VERSION_NAME} logs",
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Crash log, cumulative log and full app log attached.",
            )
        }
        return intent
    }

    private fun prefsCrashCount(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(COUNT_KEY, 0)

    private fun startLogcat(dir: File?) {
        if (dir == null) return
        runCatching {
            val main = File(dir, "main.txt")
            if (main.exists()) {
                // Keep the previous run's full log as main_previous.txt.
                val prev = File(dir, "main_previous.txt")
                runCatching { prev.delete() }
                runCatching { main.renameTo(prev) }
            }
            // Own-process logcat works without any permission on modern
            // Android; -f streams the whole buffer into the file and keeps
            // appending while the app runs. Rotate at 8 MB keeping one older.
            logcatProcess = ProcessBuilder(
                "logcat",
                "-v", "time",
                "-f", main.absolutePath,
                "-r", "8192",
                "-n", "1",
            ).start()
        }
    }
}
