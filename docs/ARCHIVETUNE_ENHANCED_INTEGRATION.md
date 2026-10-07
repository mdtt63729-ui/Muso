# ArchiveTune "Enhanced" lyrics — integration status

Snapshot: **0.5.239**. Source of truth: `github.com/rukamori/ArchiveTune` @
`24459219a6d8353dd40179301c17baf352f6d777`.

## What "Enhanced" is in ArchiveTune

The two lyrics animation styles are ArchiveTune's `LyricsMode` enum:

```kotlin
enum class LyricsMode { V2, ENHANCED }
```

`ENHANCED` renders `moe.rukamori.archivetune.ui.component.LyricsEnhanced`
(`app/.../ui/component/LyricsEnhanced.kt`, 1,105 lines), selected in
`ui/player/LyricsScreen.kt`:

```kotlin
LyricsMode.ENHANCED -> { LyricsEnhanced(lyricsState = …, …) }
```

## What the Muso app already has

`LyricsEnhanced.kt` is **already vendored and upstream-identical**. A full diff against
the upstream file shows exactly two differences, both cosmetic:

| app | upstream |
|---|---|
| `import com.muso.music.R` | `import moe.rukamori.archivetune.R` |
| `playerConnection.mediaMetadata.collectAsStateWithLifecycle()` | `…collectAsState()` |

So the renderer itself needs no porting — it is the same code.

## Why it does not run today

Two reasons, both structural:

1. **No caller.** The only call site is ArchiveTune's own `LyricsScreen`
   (`ui/player/LyricsScreen.kt`), which is reached only from ArchiveTune's own
   `ui/player/Player.kt`. That player has **no caller anywhere in the Muso app**, so the
   whole ArchiveTune player subtree — including `LyricsEnhanced` — is unreachable.
   (This is the same dead-layer finding as Phase 1/6.)
2. **No live `LocalPlayerConnection`.** `LyricsEnhanced` starts with
   `val playerConnection = LocalPlayerConnection.current ?: return` — the *ArchiveTune*
   local. Muso provides that local as **`null`** (`KitSettingsHost`), and it cannot
   provide a real one: ArchiveTune's `PlayerConnection(context, binder: MusicBinder,
   database, scope)` requires **ArchiveTune's own `MusicBinder`**, produced by
   ArchiveTune's own `MusicService`, which is not registered in the manifest and never
   runs.

## What was done in this round

The mechanism that makes ArchiveTune's Enhanced feel fluid is its **playhead**, not the
drawing. `LyricsEnhanced` never reads the player position straight into its playhead —
it *projects* the smoothed position forward by the elapsed frame time and then corrects a
fraction of the drift each frame:

```kotlin
val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000.0 * playbackSpeed
val projectedMs = smoothedMs + elapsedMs
val driftMs = rawPosition - projectedMs
smoothedMs = if (driftMs > 250.0 || driftMs < -250.0) rawPosition
             else projectedMs + (driftMs * 0.08).coerceIn(-2.0, 2.0)
```

Muso's lyrics renderer instead **snapped** `playhead = player.currentPosition` every
frame, which inherits every quantisation step of the player clock — the jitter in the
karaoke wipe. That algorithm has now been ported into `LyricsView.rememberSmoothPlayhead`
**verbatim, with ArchiveTune's own constants** (`SMOOTH_PLAYBACK_MAX_FORWARD_DRIFT_MS`
250.0, `…BACKWARD…` 250.0, `…DRIFT_CORRECTION` 0.08, `…MAX_CORRECTION_PER_FRAME_MS` 2.0),
so the app's lyrics now advance with the same projected/corrected playhead ArchiveTune
uses.

## What a literal renderer swap still needs

To render ArchiveTune's `LyricsEnhanced` itself in the Muso player:

1. **A `PlayerConnection` bridge.** ArchiveTune's class needs a secondary construction
   path that takes a raw `androidx.media3.common.Player` + a metadata flow + the
   ArchiveTune database + a scope, with `service` becoming optional (`service?.` at its
   ~30 uses — the queue/together/canvas operations that have no Muso equivalent).
2. **`LyricsRenderViewModel` wiring** — `hiltViewModel<LyricsRenderViewModel>()` and
   `bind(mediaMetadata.id, durationMs)` to produce the `LyricsRenderScreenState` the
   renderer consumes.
3. **The render hook** — call `LyricsEnhanced(...)` from the Muso lyrics path when the
   style is ENHANCED, in place of the Echo renderer.
4. **The locals** — provide `LocalDatabase`, `LocalMenuState`, `LocalAnimationsDisabled`
   and the bridged `LocalPlayerConnection` around it.

That is a multi-file integration with a real architectural bridge, and none of it can be
compiled here — so it is staged rather than done blind in one shot.

## Verified

`LyricsView.kt` parses clean (tree-sitter) and is brace/paren balanced; the whole tree
still parses with no new errors (the 4 known false positives unchanged). Not
runtime-verified — no Android SDK here.
