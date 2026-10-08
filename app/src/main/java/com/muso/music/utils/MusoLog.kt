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
 *  - AND (Round 170): every app start and every crash is ALSO written to the
 *    PUBLIC Downloads folder under Downloads/Muso via MediaStore - no
 *    permission is needed for that on Android 10+, and it is visible in any
 *    file manager. This is the guaranteed-visible copy: it works even when the
 *    app crash-loops before the All Files Access prompt could ever appear.
 *    Each crash entry there also carries the tail of the system logcat, so
 *    the exact crash reason is in the file.
 *
 * The handler is installed as the very first thing in Application.onCreate,
 * so even early crashes are captured.
 */
object MusoLog {
    private const val PREFS = "muso_crash_log"
    private const val COUNT_KEY = "crash_count"
    private const val PROMPT_KEY = "storage_prompt_done"
    private const val FILE_PROVIDER_AUTHORITY = "com.muso.music.fileprovider"

    /** Tag for the UI trace. main.txt captures the whole logcat, so this is all that is needed. */
    private const val UI_TAG = "MusoUI"

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

    @Volatile
    private var appContext: Context? = null

    fun init(app: Application) {
        appContext = app
        // The "Muso" folder lives in the app's OWN external directory:
        //   /storage/emulated/0/Android/data/<pkg>/files/Muso
        //
        // Nothing has to be granted to write there, and the system deletes the whole folder
        // when the app is uninstalled - exactly what was asked for. This used to PREFER the
        // public /storage/emulated/0/Muso when All Files Access was granted, and to put up a
        // dialog asking for that access when it was not. Both are gone: that dialog was the
        // only permission this app ever asked the user for, and the app-external folder meets
        // every requirement without it.
        val dir =
            runCatching { File(app.getExternalFilesDir(null), "Muso").apply { mkdirs() } }
                .getOrNull()
                ?.takeIf { canWrite(it) }
        logDir = dir
        usingPublicDir = false
        mirrorDir = dir
        installCrashHandler(app)
        startLogcat(dir)
        // Round 178: whole-app jank needs measuring before it can be fixed.
        FrameJankMonitor.install()
        // Round 179: and the STALL needs naming, not just measuring.
        MainThreadWatchdog.install()

        // Drain the UI trace into Downloads/Muso/ui_log.txt every few seconds. A daemon thread,
        // not a coroutine: this is set up in Application.onCreate and must outlive any scope.
        runCatching {
            Thread {
                while (true) {
                    runCatching { Thread.sleep(4_000L) }
                    runCatching { flushUiLog() }
                }
            }.apply {
                isDaemon = true
                name = "muso-ui-log"
                start()
            }
        }
    }

    /**
     * The screen the user is looking at. The nav host keeps it here so a press can name the
     * screen it happened on - a press log with no screen is nearly useless.
     */
    @Volatile
    var currentScreen: String = "startup"
        private set

    /** A screen became visible. Logged so the UI log reads as a journey, not a list of taps. */
    fun screen(route: String) {
        currentScreen = route
        android.util.Log.i(UI_TAG, "SCREEN  " + route)
        uiLines.add(stamp() + "  SCREEN  " + route)
    }

    /** One UI event (a press, a toggle, a dialog). Lands in main.txt with the screen. */
    fun ui(event: String) {
        android.util.Log.i(UI_TAG, event + "  screen=" + currentScreen)
        uiLines.add(stamp() + "  " + event + "  screen=" + currentScreen)
    }

    /**
     * Every finger-up, wherever it lands. MotionIndication was the intended hook, but
     * MaterialTheme provides its own ripple as LocalIndication and that one wins, so
     * MotionIndication never ran and no press was ever logged. A touch at the Activity level
     * cannot be missed, and it covers buttons that pass indication = null too.
     */
    fun touch(x: Float, y: Float) {
        val where = x.toInt().toString() + "," + y.toInt()
        android.util.Log.i(UI_TAG, "TOUCH " + where + "  screen=" + currentScreen)
        uiLines.add(stamp() + "  TOUCH  " + where + "  screen=" + currentScreen)
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    /**
     * The UI trace, buffered and then written to Downloads/Muso/ui_log.txt.
     *
     * main.txt lives in Android/data/<pkg>/files/Muso, which Android 11+ HIDES from file
     * managers - so a user cannot find the log to send it. Downloads/Muso is visible in any file
     * manager and needs no permission on Android 10+. Buffered, because a MediaStore write on
     * every press would be far too expensive.
     */
    private val uiLines = java.util.concurrent.ConcurrentLinkedQueue<String>()

    private fun flushUiLog() {
        val ctx = appContext ?: return
        if (uiLines.isEmpty()) return
        val sb = StringBuilder()
        while (true) {
            val line = uiLines.poll() ?: break
            sb.append(line).append('\n')
        }
        if (sb.isNotEmpty()) runCatching { appendToDownloads(ctx, "ui_log.txt", sb.toString()) }
    }

    /** Public append used by the frame monitor (Downloads/Muso/<file>). */
    fun appendPublic(fileName: String, text: String) {
        val ctx = appContext ?: return
        runCatching { appendToDownloads(ctx, fileName, text) }
        logDir?.let { runCatching { File(it, fileName).appendText(text) } }
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

        // The guaranteed-visible copy + the logcat tail (the actual crash
        // reason, including anything the system logged about it).
        runCatching { appendToDownloads(app, "crash_log.txt", "\n$report\n----- logcat tail -----\n${logcatTail()}\n") }
    }

    /** Last ~300 logcat lines (the crash reason lives here). */
    private fun logcatTail(): String = runCatching {
        val p = ProcessBuilder("logcat", "-d", "-t", "300", "-v", "time").start()
        p.inputStream.bufferedReader().use { r -> r.readText().take(64_000) }
    }.getOrElse { "logcat unavailable: ${it.message}" }

    /**
     * Appends text to Downloads/Muso/<fileName> through MediaStore (Android
     * 10+, no permission) or a direct file write (older versions). Visible in
     * any file manager, even when the app never gets far enough to ask for
     * All Files Access.
     */
    private fun appendToDownloads(context: Context, fileName: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = android.provider.MediaStore.Downloads.getContentUri(
                android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
            )
            // Find this app's existing entry (we own what we insert).
            // Match on the file NAME only. MediaStore stores RELATIVE_PATH with a trailing
            // slash ("Download/Muso/"), so matching it against "Download/Muso" never found the
            // existing entry - and every append inserted a NEW file instead. The user's log zip
            // had ~100 of them.
            val found = resolver.query(
                collection,
                arrayOf(android.provider.MediaStore.MediaColumns._ID),
                "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(fileName),
                null,
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
            val uri =
                if (found != null) {
                    android.net.Uri.withAppendedPath(collection, found.toString())
                } else {
                    resolver.insert(
                        collection,
                        android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                            put(
                                android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                "${Environment.DIRECTORY_DOWNLOADS}/Muso",
                            )
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        },
                    ) ?: return
                }
            runCatching {
                resolver.openOutputStream(uri, "wa")?.use { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                }
                // Clear the pending flag for freshly inserted entries.
                if (found == null) {
                    resolver.update(
                        uri,
                        android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        },
                        null,
                        null,
                    )
                }
            }
        } else {
            // Legacy: direct write to the public Downloads folder.
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Muso",
            )
            if (canWrite(dir)) {
                File(dir, fileName).appendText(text)
            }
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
        // Round 170: heartbeat that is visible in Files -> Downloads -> Muso.
        // If the user sees app_log.txt there, Application.onCreate HAS run -
        // which narrows down any crash to after that point.
        val ctx = appContext ?: return
        // Off the main thread: never delay the launch.
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            runCatching {
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                appendToDownloads(
                    ctx,
                    "app_log.txt",
                    "App start ${com.muso.music.BuildConfig.VERSION_NAME} " +
                        "(${com.muso.music.BuildConfig.VERSION_CODE}) at $ts; " +
                        "publicDir=$usingPublicDir\n",
                )
            }
        }
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

// ---------------------------------------------------------------------------
// Round 178: frame-time monitor. "The whole app lags" cannot be fixed blind -
// every screen the user names (settings, navigation, lyrics) is a different
// composable, and the guilty renderer cannot be found from a description.
// This monitor watches the Choreographer: every 10 seconds it writes one
// line with the measured frame rate, how many frames overran 40 ms and the
// worst frame, to Downloads/Muso/jank_log.txt. The user sends that file and
// the numbers say WHERE the frames go (constant low fps everywhere = a
// global burner like the glass shader; normal 60 fps with spikes on
// navigation = the transitions; 30 fps with everything smooth = the device
// is rendering in power-save).
// ---------------------------------------------------------------------------
/**
 * Round 179: the jank monitor says HOW LONG a stall was; it cannot say WHAT caused it, because a
 * Choreographer callback runs on the main thread itself - sampling the main thread from there
 * returns the monitor's own stack.
 *
 * This watchdog runs on its own thread and reads the main thread's stack every 500 ms. When the
 * same top frames repeat for ~2 s, the main thread is stuck, and the stack it prints is the one
 * that names the blocking call. Written to Downloads/Muso/jank_stack.txt, which is visible in any
 * file manager.
 */
object MainThreadWatchdog {
    @Volatile private var started = false

    fun install() {
        if (started) return
        started = true
        runCatching {
            Thread {
                var lastTop = ""
                var repeats = 0
                while (true) {
                    runCatching { Thread.sleep(500L) }
                    val stack = runCatching {
                        android.os.Looper.getMainLooper().thread.stackTrace
                    }.getOrNull() ?: continue
                    val top = stack.take(8).joinToString("|") { it.toString() }
                    if (top == lastTop) {
                        repeats++
                        if (repeats == 4) {
                            repeats = 0
                            val stamp = java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US,
                            ).format(java.util.Date())
                            val body = stack.take(24).joinToString("\n    ") { it.toString() }
                            val report =
                                "\n[STUCK ~2s at " + stamp + "  screen=" + MusoLog.currentScreen + "]\n    " +
                                    body + "\n"
                            Thread { runCatching { MusoLog.appendPublic("jank_stack.txt", report) } }.start()
                        }
                    } else {
                        lastTop = top
                        repeats = 0
                    }
                }
            }.apply {
                isDaemon = true
                name = "muso-main-watch"
                start()
            }
        }
    }
}

object FrameJankMonitor {
    @Volatile private var started = false

    fun install() {
        if (started) return
        started = true
        runCatching {
            val choreographer = android.view.Choreographer.getInstance()
            var last = 0L
            var frames = 0L
            var janky = 0
            var worstNanos = 0L
            var windowStart = System.currentTimeMillis()
            choreographer.postFrameCallback(object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (last != 0L) {
                        val delta = frameTimeNanos - last
                        frames++
                        if (delta > 40_000_000L) {
                            janky++
                            if (delta > worstNanos) worstNanos = delta
                        }
                        val elapsed = System.currentTimeMillis() - windowStart
                        if (elapsed >= 10_000L) {
                            if (frames > 0) {
                                val fps = frames * 1000f / elapsed
                                val line = "\n[" + java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.US,
                                ).format(java.util.Date()) + "] ${elapsed / 1000}s: $frames frames (~${"%.1f".format(fps)} fps), $janky janky(>40ms), worst ${worstNanos / 1_000_000}ms\n"
                                Thread { runCatching { MusoLog.appendPublic("jank_log.txt", line) } }.start()
                            }
                            frames = 0; janky = 0; worstNanos = 0
                            windowStart = System.currentTimeMillis()
                        }
                    }
                    last = frameTimeNanos
                    choreographer.postFrameCallback(this)
                }
            })
        }
    }
}
