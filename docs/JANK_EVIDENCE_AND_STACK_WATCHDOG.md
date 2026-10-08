# The logs prove 0.5.256 is running — and they name the real problem

Snapshot: **0.5.257**.

## What the uploaded logs prove

`app_log.txt` and `app_log_1.txt` both read:

```
App start 0.5.256 (263) at 2026-10-08 11:24:11; publicDir=false
```

**`publicDir=false` is written by nothing except the new code.** So the phone is running
0.5.256, and every change from 0.5.247 onwards is live on it. That settles the question the
last few rounds were circling.

## And they show the app is not "laggy" — it stalls

| time | window | frames | fps | janky | worst |
|---|---|---|---|---|---|
| 11:24:21 | 10 s | 445 | 44.4 | 29 | **1385 ms** |
| 11:24:37 | 10 s | 851 | 85.0 | 19 | 249 ms |
| 11:25:03 | 25 s | 995 | 38.3 | 1 | **17725 ms** |
| 11:25:13 | 10 s | 1088 | 108.7 | 13 | 149 ms |
| 11:25:23 | 10 s | 1198 | 119.8 | 1 | 58 ms |
| 11:25:33 | 10 s | 1041 | 103.9 | 11 | 99 ms |
| 11:25:43 | 10 s | 1053 | 105.2 | 10 | 174 ms |
| 11:25:53 | 10 s | 1116 | 111.6 | 4 | 149 ms |

The app runs at **100–120 fps** in the steady state. The felt problem is not a slow renderer —
it is **a few enormous freezes**, one of them **17.7 seconds**. That is a different bug, and it is
much easier to fix, because it has a cause rather than a cost.

## Why the monitor could not name it

`FrameJankMonitor`'s callback runs **on the main thread**, so sampling the main thread from inside
it returns the monitor's own stack. It could only ever report how long the stall was.

## The watchdog

`MainThreadWatchdog` runs on its own thread and reads the main thread's stack every 500 ms. When
the same top frames repeat for about two seconds, the main thread is genuinely stuck, and it
writes that stack to **`Downloads/Muso/jank_stack.txt`** — visible in any file manager:

```
[STUCK ~2s at 2026-10-08 11:25:03.412  screen=player]
    android.database.sqlite.SQLiteConnection.nativeExecuteForCursorWindow(Native Method)
    android.database.sqlite.SQLiteConnection.executeForCursorWindow(SQLiteConnection.java:859)
    ...
```

That line names the blocking call. It is what will close the 17.7-second freeze.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives); `MusoLog.kt` is
brace/paren balanced and `MainThreadWatchdog.install()` is called from `init`, next to the jank
monitor.
