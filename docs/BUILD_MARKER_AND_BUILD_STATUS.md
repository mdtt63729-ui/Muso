# A visible build marker, and why nothing appeared to change

Snapshot: **0.5.255**.

## The evidence

Every build log sent so far reports a **FAILED** build:

| log | errors |
|---|---|
| 0.5.240 | 88 |
| 0.5.243 | 15 |
| 0.5.248 | 789 |
| 0.5.253 | 9 |

A failed build means CI produces no new artifact, and a release page is not updated — so the APK
being installed stays the one from the last time the build actually passed. That is the most
likely reason none of the changes appear: **the code is not reaching the phone.**

The code itself is present. Checked inside the delivered `Muso-v0.5.254-source.zip`:

| change | in the zip |
|---|---|
| `Player.kt` — the `PlayerStyle` → `nowPlayingStyle` mapping | yes |
| `MusoLog` — the app-external `Muso` folder | yes |
| `MotionIndication` — press logging | yes |
| `AboutScreen` — `LazyColumn` | yes |
| `BottomSheet` — the `wasRestored` guard | yes |
| `MainActivity` — the snap-to-dismissed effect | removed (0 references) |
| `PlayerStyle` — three entries | yes |
| the old All Files Access dialog | removed (0 references) |

## The marker

So that this can be settled in one launch, the app now shows a toast **once per version**:

```
Muso 0.5.255 (code 262)
logs: /storage/emulated/0/Android/data/com.muso.music/files/Muso
```

It is the first thing in `MainActivity.onCreate`. Two things follow from it:

- **If the toast does not appear, the running APK is not built from this source.** There is no
  other reading — the call is unconditional, first, and wrapped in `runCatching` so it cannot
  itself fail the launch.
- **If it does appear, the version and the log folder are both confirmed on screen**, without
  needing a file manager that can reach `Android/data`.

It is shown once per version, so it will not nag after the first launch.

## What is needed to go further

The build log for 0.5.254 (or 0.5.255). If it is green, the APK path is the problem and the
version check above will show it. If it is not green, that log names the exact blocker — and it
is the only thing that does.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). `BuildConfig` needs
no import in `MainActivity` (it is the app's own namespace), and `Toast` was already imported.
