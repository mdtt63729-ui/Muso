# What the 0.5.259 logs show, and why a failed play was invisible

Snapshot: **0.5.260**.

## The logs the user sent

`app_log-2.txt`:

```
App start 0.5.259 (266) at 2026-10-08 12:47:14; publicDir=false
App start 0.5.259 (266) at 2026-10-08 12:50:30; publicDir=false
```

`jank_log-2.txt`, three minutes of steady state: 525-603 frames per 10 s (**52.5-60.2 fps**), 0-10
janky frames, worst **49-215 ms**. Compare the previous build: `445 frames (~44.4 fps), 29 janky,
worst 1385ms`, and a 17 725 ms stall. The stalls are gone.

`ui_log.txt`: 114 `TOUCH`, 56 `PRESS`, 18 `SCREEN`, 3 `LIFECYCLE`. The trace works, and presses are
logged - so `MotionIndication` DOES run. An earlier note of mine claimed it never ran because
`MaterialTheme` provides its own ripple as `LocalIndication`. That was wrong: the earlier log had a
three-second window with no press in it, and I read too much into it. Corrected in the source
comments.

The session: Home -> **online_playlist/{playlistId}** (12:47:28-12:47:39, about twenty taps down
the list) -> Search -> Settings -> Appearance -> Lyrics -> Appearance.

## The watchdog reported nothing useful

Every one of the 14 entries in `jank_stack.txt` has the same stack:

```
android.os.MessageQueue.nativePollOnce(Native Method)
android.os.MessageQueue.nextLegacy
android.os.MessageQueue.next
android.os.Looper.loopOnce
android.os.Looper.loop
android.app.ActivityThread.main
```

That is the **idle** main thread - the Looper waiting for its next message. It is identical on every
sample, so the watchdog's "same top-8 frames four times" test matched it every time. Fixed: a stack
whose top frame is `nativePollOnce` or `MessageQueue.next` is skipped, so only real work is reported.

## Why the songs did not play

`playQueue` runs its body under `SilentHandler`:

```kotlin
val SilentHandler = CoroutineExceptionHandler { _, _ -> }
```

It swallows every exception. So a queue that could not be resolved - network, a dead stream, a
missing PO token - produced **no crash, no toast, and no log**. The user tapped a song and nothing
happened. That is report 1, and it is invisible by construction.

Fixed two ways:

- `playQueue` logs `PLAY-QUEUE <queue class>` on entry, so the next ui_log shows whether a tap
  reached playback at all.
- Its body now runs under `MusoLog.playFailureHandler(applicationContext)` instead of
  `SilentHandler`. A failure is logged as `PLAY-FAIL <exception>: <message>` **and** shown as a
  toast. Whatever the underlying cause turns out to be, the app now says so instead of doing
  nothing.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). No device run here.

## Not fixed

The `⋮` button; the lyrics text overlap seen in the screen recording; Phase 9 (~20 screens).
