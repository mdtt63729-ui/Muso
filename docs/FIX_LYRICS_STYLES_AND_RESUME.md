# Lyrics: the None style, per-style behaviour, and a resume button

Snapshot: **0.5.245**.

## 1. Why None was the laggiest style (and why the picker seemed not to work)

`LyricsView` resolved the renderer with this:

```kotlin
val effectiveEchoLyricsStyle = when {
    !wordByWordEnabled -> NONE
    echoLyricsStyle == IMMERSIVE -> NONE      // drawn by the Apple sheet instead
    else -> ENHANCED
}
```

There was no branch for the picker's own **None**. A stored None fell into the `else`, so
choosing None ran the **Echo word-by-word renderer** — an animated line under a setting that says
"no animation". None was therefore the only style doing the most work, for the one style that
must do none. That is also why every style looked the same: two of the three settings produced
the same renderer.

Fixed:

```kotlin
echoLyricsStyle == NONE -> NONE
```

The three settings now reach three different renderers:

| setting | renderer |
|---|---|
| **Enhanced** | ArchiveTune's `LyricsEnhanced` sheet (returns before the Echo path) |
| **Immersive** | the Apple-Music sheet |
| **None** | the suite's static lines — no animation, no per-frame loop |

A side effect worth stating: with Enhanced taking the early return, the Echo word renderer
(`EchoLyricsLine`) is now reached by no setting. It is left in place, unused.

## 2. Resume to the line that is singing

When the user drags the list away from the sung line, a button now appears at the bottom of the
lyrics area; tapping it returns to the current line.

- "Away" is derived from the list itself: the sung line's index is not among the visible items.
- The button waits **600 ms** before showing. The player's own auto-scroll also takes the line
  off screen for a moment, and that is not the user asking to go anywhere — without the delay the
  button flashed on every line change.
- It scrolls back with **the same call the player's own auto-scroll uses**
  (`animateScrollAndCentralizeItem`, or `animateScrollAndAnchorItemTop(-exposedRowPx)` in the
  Apple style), so the line lands exactly where the animation and the blur expect it.
- It is an overlay inside the same `BoxWithConstraints` as the list, so it does not push the
  lyrics, and it is hidden when there is no sung line.

## Files

| file | change |
|---|---|
| `.../ui/component/LyricsView.kt` | NONE branch; resume button + imports |

## Verified

All 1,074 `.kt` files parse clean with tree-sitter (the 4 known false positives unchanged) and
the file is brace/paren balanced. Not runtime-verified — no Android SDK here.
