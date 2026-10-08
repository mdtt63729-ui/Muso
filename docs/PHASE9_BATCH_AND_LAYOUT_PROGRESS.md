# Progress on the remaining list

Snapshot: **0.5.251**.

## 1. Layout jump — one real cause fixed, the rest needs a frame-level capture

`BottomSheetState.progress` divided by `(expandedBound - collapsedBound)` with no guard:

```kotlin
1f - (upperBound - value) / (upperBound - collapsedBound)
```

For a frame while a restore is still measuring, `expandedBound == collapsedBound`, so that is
`0/0` — **NaN**. Everything positioned from `progress` (the navbar's slide offset, the mini
player's fade) then took a NaN offset. Now guarded:

```kotlin
val span = upperBound - collapsedBound
if (span.value <= 0f) 1f else 1f - (upperBound - value) / span
```

The supplied recording shows the mini player and the bottom bar overlapping and content sitting
in the wrong place for a moment, which is consistent with a bad offset for a frame or two — but
I cannot tell from 2 fps stills whether that is this NaN, the glass bar's own integrated mini
player (which is designed to sit inside the bar), or something else. A capture at 60 fps around
the single foreground moment would settle it.

## 2. The info / queue / add-to-playlist / fullscreen-lyrics collapse

Not re-verified on a device. The reasoning that it is already cured stands: every one of those
opens a second window, the activity is re-created for it, and the `MainActivity` effect that
snapped the sheet to dismissed on every state re-creation was removed in 0.5.249. That effect
fired on exactly that path.

## 3. Phase 9 — lazy settings screens

Two converted this round, and they show why this is not a mechanical sweep:

| screen | note |
|---|---|
| `ListeningHistorySettings.kt` | flat list — a straight wrap into `item {}` blocks |
| `BackupAndRestore.kt` | flat list **with two `rememberPreference` reads in the middle** |

The second one is the trap. A lazy list composes and recycles its items, so a `remember` inside
one is not a stable home for preference state — those two reads had to be hoisted above the
`LazyColumn` first. Any screen doing that needs the same treatment, which is why the remaining
22 screens (7 Muso-side, 15 kit-side) are best done in small batches with a build between them,
rather than as one blind edit.

## 4. Phase 7 (codec), 12 (testing), 13 (perf verification), 14 (final regression)

Device-bound. Phase 7 asks that the codec pipeline not be UI-only; the player already derives its
codec readout from real media3 track formats (`onTracksChanged` → `tracks.groups[TRACK_TYPE_AUDIO]
.getTrackFormat(0)`) rather than a label, but confirming that it matches what actually plays needs
a device and a file with a known codec.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). Both converted
screens parse clean and are brace/paren balanced, carry the `LazyColumn` import, and have no
dangling `scrollState`/`verticalScroll` reference left behind. Not runtime-verified.
