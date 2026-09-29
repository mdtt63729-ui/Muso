package com.muso.music.utils

import android.app.Application
import android.content.Context
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
 */
object MusoLog {
    private const val PREFS = "muso_crash_log"
    private const val COUNT_KEY = "crash_count"
    private const val PROMPT_KEY = "storage_prompt_done"

    @Volatile
    var logDir: File? = null
        private set

    @Volatile
    var usingPublicDir: Boolean = false
        private set

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
        val dir = logDir ?: return
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

        // One file per crash: crash_log_1.txt, crash_log_2.txt, ...
        runCatching { File(dir, "crash_log_$count.txt").writeText(report) }
        // And the cumulative crash log.
        runCatching { File(dir, "crash_log.txt").appendText("\n$report") }
    }

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
