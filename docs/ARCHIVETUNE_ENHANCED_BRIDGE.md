# ArchiveTune ENHANCED lyrics — the bridge (all four steps)

Snapshot: **0.5.240**. Completes the integration started in 0.5.239.

## Result

Selecting the **Enhanced** lyrics animation style now renders ArchiveTune's own
`ui/component/LyricsEnhanced` — its `LyricsMode.ENHANCED` renderer — against Muso's live
player, so the animation is ArchiveTune's own code, not a look-alike.

## Step 1 — the `PlayerConnection` bridge

`moe.rukamori.archivetune.playback.PlayerConnection` had one constructor taking
ArchiveTune's `MusicBinder` (from a `MusicService` that is not registered here). It now has
a **private primary** constructor over the pieces and two secondary constructors:

- `PlayerConnection(context, binder: MusicBinder, database, scope)` — ArchiveTune's own
  playback stack, unchanged behaviour;
- `PlayerConnection(context, player: Player, mediaMetadata: MutableStateFlow<MediaMetadata?>,
  database, scope)` — **the Muso bridge**, wrapping Muso's live player.

`service` became `private val service: MusicService?`, and its 21 uses were handled: the
metadata / network / queue-restore fields now come from constructor parameters, and the
service-only operations (queue control, Together, canvas refetch) are inert when there is no
service (`service?.…`). `canvasNetworkAllowed` degrades to a constant `false` flow.

## Step 2 — `LyricsRenderViewModel` wiring

`MusoEnhancedLyrics` resolves the ViewModel with `hiltViewModel<LyricsRenderViewModel>()`,
collects its `state`, and drives it from the current song:

```kotlin
renderViewModel.bind(mediaId = metadata.id, durationMs = player.duration)
```

## Step 3 — the render hook

`LyricsView` (the suite renderer the Muso player uses) now checks the lyrics animation
style first. When it is **Enhanced**, it renders `MusoEnhancedLyrics` and returns, so
ArchiveTune's sheet replaces the suite's renderer for that style. Immersive and None are
unchanged.

## Step 4 — the locals

`LyricsEnhanced` reads exactly two ArchiveTune locals — `LocalPlayerConnection` and
`LocalAnimationsDisabled` — plus the standard Compose ones. `MusoEnhancedLyrics` provides
both: the bridged connection, and `false` for animations-disabled.

## New / changed files

| file | change |
|---|---|
| `moe/rukamori/archivetune/playback/PlayerConnection.kt` | bridge constructor + optional service |
| `com/muso/music/ui/component/MusoEnhancedLyrics.kt` | **new** — the bridge composable |
| `com/maxrave/simpmusic/ui/component/LyricsView.kt` | Enhanced early-return hook |

## Notes and limits

- The bridge keeps an ArchiveTune-shaped metadata flow in step with Muso's (id, title,
  artists, duration, thumbnail, explicit) — the renderer reads it for the song id and the
  artwork.
- The ArchiveTune database is reached through the existing `KitRuntimeAccess.database()`,
  and only ever touched for LOCAL media ids; a failure there degrades to "no Enhanced
  renderer" rather than taking the player down.
- The `Enhanced` style is the picker's default, so this path is the one most users see;
  switching to Immersive or None returns to the previous renderers.

## Verified

All 1,074 `.kt` files parse clean with tree-sitter (the 4 known false positives unchanged),
and the three touched files are brace/paren balanced. Not runtime-verified — no Android SDK
here — and this is the largest change in the whole series, so it is worth building and
opening a song's lyrics before anything else.
