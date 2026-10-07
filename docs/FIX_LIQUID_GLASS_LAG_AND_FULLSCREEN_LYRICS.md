# Two fixes: Liquid Glass lag, and the fullscreen lyrics closing itself

Snapshot: **0.5.241**.

## 1. Liquid Glass lag

Two separate costs, both real.

### a) The content layer was recorded whether or not the glass was on

`MainActivity` applied `Modifier.layerBackdrop(glassBackdrop)` to the **whole NavHost** —
the entire app content — unconditionally. That modifier makes the content record itself
into a full-screen `GraphicsLayer` every time it draws: every scroll, every page
transition, every animation. With the Liquid Glass setting **off**, no glass surface
samples that layer at all, so the app was paying the single most expensive part of the
effect for glass it never drew.

The backdrop is now applied only while the setting is on:

```kotlin
.then(if (liquidGlassState.value) Modifier.layerBackdrop(glassBackdrop) else Modifier)
```

`liquidGlassState` is the same `State<Boolean>` the app already derives from
`LiquidGlassNavBarKey` and feeds to `LocalLiquidGlassEnabled`, so the record and the
surfaces that consume it are gated on exactly the same flag — no surface can sample a
layer that was not recorded.

### b) Blur radii cut, and no longer driven by the press progress

Backdrop blur is the expensive part of every glass surface.

| surface | before | after |
|---|---|---|
| `drawInteractiveGlass` (bar capsule, mini-player, pills) | 5–8 dp × 0.8 **+ 1 dp × press** | 4–6 dp × 0.8 |
| tab bar selection blob (`LiquidGlassTabBar`) | 8–16 dp **+ a flat 20 dp** (28–36 dp total) | 5–9 dp + 8 dp (13–17 dp total) |

The blob was by far the most expensive shader in the app and it slides on every drag
frame. The press term is gone from `drawInteractiveGlass` because it re-specified the
effect on every frame of a press for a change that is not visible.

## 2. The fullscreen lyrics closed themselves

`FullscreenLyricsSheet` (in `LyricsView.kt`) tracked its open state as a boolean that
started `false` and was flipped by a `LaunchedEffect`, with a second effect watching it:

```kotlin
var visible by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { visible = true }
LaunchedEffect(visible) { if (!visible) { delay(190L); onDismiss() } }
```

The watcher effect is first composed against `visible = false`, so on any frame where the
opening flip has not landed yet it reads the page as closed and schedules `onDismiss()`
190 ms out. On a device that is busy — which, with the glass lag above, this one was — the
window is missed and the page dismisses itself right after opening. Immersive mode was
unaffected because its sheet is reached through a different path.

It now uses a `MutableTransitionState`, the same pattern the desktop branch of
`NowPlayingScreen` already uses:

```kotlin
val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
fun requestClose() { visibility.targetState = false }
LaunchedEffect(visibility.currentState, visibility.isIdle) {
    if (visibility.isIdle && !visibility.currentState) onDismiss()
}
```

It starts closed with the target already open, so there is no state in which the page
looks closed without having been asked to close — the race is gone rather than narrowed.
Enter and exit animations are unchanged.

## Files

| file | change |
|---|---|
| `com/muso/music/MainActivity.kt` | gate `.layerBackdrop` on the setting |
| `.../ui/component/LiquidGlassContainer.kt` | smaller blur, no press-driven radius |
| `.../ui/component/LiquidGlassTabBar.kt` | blob blur halved |
| `.../ui/component/LyricsView.kt` | `FullscreenLyricsSheet` → `MutableTransitionState` |

## Verified

All 1,074 `.kt` files parse clean with tree-sitter (the 4 known false positives
unchanged). Not runtime-verified — no Android SDK here. The fullscreen fix removes a race
and is the safer of the two; the glass changes are reductions in shader work whose visual
effect is a slightly softer frost.
