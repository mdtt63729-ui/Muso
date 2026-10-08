# What the user's log zip proved, and the two bugs it exposed

Snapshot: **0.5.258**.

## The UI trace works — but logged no presses

`ui_log.txt` contains:

```
2026-10-08 11:24:12.097  LIFECYCLE onResume  screen=startup
2026-10-08 11:24:14.107  SCREEN  com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
2026-10-08 11:24:14.334  SCREEN  com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
2026-10-08 11:24:15.582  LIFECYCLE onPause  screen=com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
```

`LIFECYCLE` and `SCREEN` both work. **`PRESS` count: 0.**

## Bug 1: `MotionIndication` never runs

The press hook was placed in `MotionIndication`, on the assumption that it is the app-wide
`LocalIndication`. It is not — `Theme.kt` records it in its own comment: *"MaterialTheme already
provides `material3.ripple()` as LocalIndication"*. The theme's provider sits **inside**
`MainActivity`'s, so it wins, and `MotionIndication` never sees an interaction. That is why not a
single press was logged.

**Fixed** with a hook that cannot be beaten: `MainActivity.dispatchTouchEvent` logs every
finger-up with its coordinates and the current screen.

```kotlin
override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_UP) {
        runCatching { MusoLog.touch(ev.x, ev.y) }
    }
    return super.dispatchTouchEvent(ev)
}
```

It is above every Compose mechanism, so it also covers the buttons that pass `indication = null`.

## Bug 2: ~100 files instead of one

The zip holds about a hundred `.pending-*` files. `appendToDownloads` looked for an existing entry
with

```
RELATIVE_PATH = "Download/Muso" AND DISPLAY_NAME = "jank_log.txt"
```

but MediaStore stores `RELATIVE_PATH` **with a trailing slash** — `"Download/Muso/"` — so the
query never matched and every append inserted a **new** file. Fixed to match on the file name
alone, which is unique to this app.

## Still to come

`jank_stack.txt` was absent, as expected: the `MainThreadWatchdog` that writes it is in 0.5.257,
and the phone was running 0.5.256.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives); `MusoLog.kt` and
`MainActivity.kt` are brace/paren balanced.
