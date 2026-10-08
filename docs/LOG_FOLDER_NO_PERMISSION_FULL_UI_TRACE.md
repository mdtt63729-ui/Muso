# The log folder: no permission, and a full UI trace

Snapshot: **0.5.253**.

## The folder

```
/storage/emulated/0/Android/data/com.muso.music/files/Muso/
```

That is the app's **own** external directory. Nothing has to be granted to write there, and the
system **deletes the whole folder when the app is uninstalled** — which is what was asked for.

It used to prefer the public `/storage/emulated/0/Muso` when All Files Access had been granted,
and put up a dialog offering to send the user to that settings page when it had not. **Both are
gone.** That dialog was the only permission this app ever asked the user for; the app-external
folder meets every requirement without it, so there is nothing left to ask for. The
`AlertDialog` and the `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` intent were removed
outright, not just disabled.

The trade-off, stated plainly: the folder sits under `Android/data/`, so it is one level deeper
than `/storage/emulated/0/Muso` would have been. That is the price of asking for no permission —
a folder directly under the storage root, and one that survives uninstall, both require All Files
Access.

## What lands in it

| file | contents |
|---|---|
| `main.txt` | the **whole logcat for this run**, streamed live — every Timber/Log line, every crash, from process start |
| `main_previous.txt` | the previous run's `main.txt` |
| `crash_log.txt` | every crash, appended |
| `crash_log_N.txt` | one file per crash |

`main.txt` is written by streaming this process's own logcat (`logcat -f …`), which needs no
permission on any modern Android — so anything the app logs is captured, from the first line of
`Application.onCreate`.

## The UI trace

Three new lines of evidence, all landing in `main.txt`:

- **Every press.** `MotionIndication` is the app-wide `LocalIndication`, so every plain
  `clickable` and `combinedClickable` in the app passes through it. A press is logged there —
  one place, no call site to touch, and it cannot be missed by a screen that forgot to log.
  ```
  PRESS  screen=player
  ```
- **Every screen.** The nav host logs the destination on each change, so the log reads as a
  journey:
  ```
  SCREEN  library
  ```
- **The app lifecycle.** `onResume` / `onPause` / `onStop` / `onDestroy`, which is where the
  foreground-restore bugs live:
  ```
  LIFECYCLE onResume  screen=player
  ```

A press line therefore reads as *what* and *where*: `PRESS screen=player`.

## Files

| file | change |
|---|---|
| `utils/MusoLog.kt` | folder forced to app-external; `screen()` / `ui()` trace API |
| `ui/animation/MotionIndication.kt` | logs every press |
| `MainActivity.kt` | permission dialog removed; screen logged on nav change |
| `App.kt` | lifecycle logging |

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives); all four touched
files are brace/paren balanced; no reference to the removed dialog or the settings intent remains
anywhere. Not runtime-verified.
