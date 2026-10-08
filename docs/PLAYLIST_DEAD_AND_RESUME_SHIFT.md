# One root cause behind two reports: the dead playlist and the layout that shifts on restore

Snapshot: **0.5.259**.

## The two reports

1. "Playlist songs do not play; no button and no component in the playlist works."
2. "When I minimise the app and come back, components do not stay where they were. 0.5.240-0.5.244
   were fine."

## The root cause

The player sheet's collapsed bound is built from the window insets:

```kotlin
collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight
```

Insets change when the app goes to the background and returns, so **`collapsedBound` changes**. The
`Animatable` that holds the sheet's position is keyed on the anchor and on the dismissed/expanded
bounds, but **not** on `collapsedBound`:

```kotlin
val animatable = remember(initialValue, dismissedBound, expandedBound) { ... }
```

So when the collapsed bound moves, the Animatable keeps its **old** value. The sheet is then no
longer exactly at `collapsedBound`, and two things follow from that one fact:

1. `isCollapsed` and `isDismissed` are both false, so `progress` is neither 0 nor 1. Everything
   positioned from `progress` — the mini player's fade (`1f - progress*4`) and the navbar's slide —
   lands somewhere else. **That is report 2.**

2. The sheet then composes its **expanded** layer: a `fillMaxSize()` box with a `pointerInput` and
   an alpha near zero. It is invisible, it lies over the whole screen, and it consumes every tap.
   **That is report 1** — buttons do nothing and songs do not play.

The file's own comment already named this failure mode: *"the collapsed touch catcher sitting
misplaced over screen content eating taps"*.

## Why 0.5.244 was fine

Before 0.5.249 the host had:

```kotlin
LaunchedEffect(playerBottomSheetState) { playerBottomSheetState.snapTo(playerBottomSheetState.dismissedBound) }
```

`rememberBottomSheetState` re-creates the state object whenever the bounds change, so that effect
re-anchored the sheet on every inset change and masked this. It was removed in 0.5.249 because it
also forced a restored **FULLSCREEN** player down to **MINI**, which the PRD forbids. Removing it
fixed the PRD bug and exposed this one.

## The fix

Re-anchor to the bound the sheet is actually sitting at, instead of to `dismissedBound`:

```kotlin
LaunchedEffect(collapsedBound, expandedBound) {
    when (previousAnchor) {
        collapsedAnchor -> if (animatable.value != collapsedBound) animatable.snapTo(collapsedBound)
        expandedAnchor -> if (animatable.value != expandedBound) animatable.snapTo(expandedBound)
    }
}
```

A collapsed sheet follows the new collapsed bound; an expanded sheet follows the new expanded
bound; nothing else moves. This also satisfies the PRD directly — a fullscreen player stays
fullscreen across a lifecycle change.

Two supporting changes:

- `progress` returned **1f** (the expanded end) when `expandedBound == collapsedBound`. That is
  backwards: with no room to expand the sheet is collapsed, which is **0f**. Returning 1f faded the
  mini player out and slid the navbar away for that frame.
- The expanded layer now also requires `progress > 0.01f`, so an in-between value can never compose
  an invisible full-screen layer over the app.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives); `BottomSheet.kt` is
brace/paren balanced. No device run is possible here — this is a code-level fix.

## Not fixed

The `⋮` (three-dot) button in the player still does nothing.
