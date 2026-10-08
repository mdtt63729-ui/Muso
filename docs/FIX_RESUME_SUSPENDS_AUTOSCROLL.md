# Lyrics resume: scrolling by hand suspends the auto-scroll

Snapshot: **0.5.246**. Refines the resume button added in 0.5.245.

## The behaviour

1. The listener drags the list off the line that is singing → the player's own **auto-scroll
   stands down** (it no longer yanks the list back) and the **resume button appears**.
2. The listener returns to the sung line — with the button, or by scrolling back by hand → the
   **button disappears** and the **auto-scroll takes over again**.

## How

`userScrolledAway` is the single flag behind all of it.

```kotlin
val isAwayFromCurrentLine by remember {
    derivedStateOf {
        val visible = listState.layoutInfo.visibleItemsInfo
        visible.isNotEmpty() && visible.none { it.index == renderCurrentLineIndex }
    }
}
var userScrolledAway by remember { mutableStateOf(false) }
LaunchedEffect(isDragging, isAwayFromCurrentLine) {
    if (isDragging) userScrolledAway = true          // only a DRAG arms it
    if (!isAwayFromCurrentLine) userScrolledAway = false
}
```

- **Only a drag arms it.** The auto-scroll itself takes the line off screen for a moment on every
  line change, and that is not the listener asking to go anywhere. Keying on `isDragging` is what
  separates the two, and it is why the button needs no debounce any more (0.5.245 used a 600 ms
  timer for this; the flag is exact).
- **The auto-scroll effect now takes the flag as a key and returns early on it:**

```kotlin
LaunchedEffect(renderCurrentLineIndex, ..., userScrolledAway, isDragging) {
    if (userScrolledAway || isDragging) return@LaunchedEffect
    ...
}
```

  Clearing the flag re-runs the effect, which is exactly how the auto-scroll resumes — no second
  code path.
- The button is shown on `userScrolledAway` alone, so it vanishes on its own the moment the line
  is back, whichever way it got there.

## Files

| file | change |
|---|---|
| `.../ui/component/LyricsView.kt` | `userScrolledAway`; auto-scroll stands down; button keyed on the flag |

## Verified

1,074 `.kt` files parse clean with tree-sitter (the 4 known false positives unchanged); the file
is brace/paren balanced; `isDragging` (485) and `renderCurrentLineIndex` (618) are both defined
before the state block (~645) and before `BoxWithConstraints` (679), so the button and the
auto-scroll both see them. Not runtime-verified — no Android SDK here.
