# Still broken: the re-anchor was too narrow, and a sheet trace to settle it

Snapshot: **0.5.261**.

The user reports, on the latest APK, that nothing is fixed: songs in the playlist still do not
play, no button there works, and components still do not stay in place when the app is minimised
and restored. My previous two diagnoses were wrong, and this note records what is being done
differently.

## What the 0.5.259 ui_log actually rules out

On `online_playlist/{playlistId}` the log has **both** `TOUCH` and `PRESS` lines, paired, about
twenty of them between 12:47:30 and 12:47:39. `PRESS` is written from `MotionIndication`'s
`PressInteraction.Press` handler, which only runs when a `clickable`'s interaction source sees a
press. So on that session the taps were **reaching the clickables** - a full-screen overlay eating
them is not what happened there. That weakens my "invisible layer eats every tap" story for the
playlist, and points bug 1 back at the action itself: `playQueue` failing silently (see
docs/PLAYBACK_FAILURE_WAS_SILENT.md, which 0.5.260 now reports and surfaces).

## The re-anchor was too narrow

The first version only handled two cases:

```kotlin
when (previousAnchor) {
    collapsedAnchor -> if (animatable.value != collapsedBound) animatable.snapTo(collapsedBound)
    expandedAnchor -> if (animatable.value != expandedBound) animatable.snapTo(expandedBound)
}
```

`previousAnchor` is only updated by `collapse()`, `expand()` and `dismiss()` - never by a drag,
which goes through the `DraggableState` and touches nothing. So the value can be left strictly
**between** anchors, and then neither branch runs: the sheet stays mid-range, `isCollapsed` and
`isDismissed` are both false, `progress` is neither 0 nor 1, and the expanded layer composes
invisibly over the whole app. The fix now snaps to the **nearest anchor**, whatever the value was:

```kotlin
LaunchedEffect(collapsedBound, expandedBound) {
    val v = animatable.value
    val nearest = listOf(dismissedBound, collapsedBound, expandedBound)
        .minBy { kotlin.math.abs((it - v).value) }
    if (nearest != v) animatable.snapTo(nearest)
}
```

After any bounds change the sheet rests exactly on an anchor.

## A trace that will settle it

`MainActivity` now writes one `SHEET ...` line to `ui_log.txt` per screen change:

```
SHEET progress=0% collapsed=true dismissed=false expanded=false value=88.0dp
```

That single line says whether the player sheet is covering the screen. If `ui_log.txt` from the
playlist shows a non-zero `progress`, or `collapsed=false`, the sheet is the cause and the numbers
name it. If it shows `progress=0% collapsed=true`, the sheet is innocent and the playlist problem is
in the action - and the `PLAY-QUEUE` / `PLAY-FAIL` lines added in 0.5.260 will say which.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). No device run here.

## Honest position

I have mis-diagnosed this twice from source alone, and I am not going to claim a third fix is
certain. What I need is one of: a screen recording of the playlist screen while a song is tapped,
or the `ui_log.txt` from 0.5.261 taken on that screen. Either one turns this from guesswork into a
fact.
