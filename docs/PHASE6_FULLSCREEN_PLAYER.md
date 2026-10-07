# Muso — Phase 6: Fullscreen Player

PRD §63, Phase 6. Snapshot: **0.5.228**. The PRD asks for a state machine, no
recreation, and no mini-player regression.

---

## 0. Correction to the Phase 1 / Phase 3 audits — there are SIX players, not one

The Phase 1 audit reported "exactly one `ExoPlayer.Builder` call in the whole tree".
**That was wrong.** The search pattern did not match builder calls written across two
lines (`ExoPlayer\n    .Builder(...)`). There are six:

| # | Site | Live? | Role |
|---|---|---|---|
| 1 | `playback/MusicService.kt:314` | yes | The audio player. The only one the manifest's media service owns. |
| 2 | `ui/player/FullscreenVideoScreen.kt:119` | yes | Video-only player for the fullscreen video screen. Reached from `NavigationBuilder` route `fullscreen_video`. |
| 3 | `maxrave/media3/ui/MediaPlayerView.kt:184` | yes | The canvas player the now-playing screen attaches to the video surface. |
| 4 | `archivetune/playback/MusicService.kt:1133` | no | Inside the unregistered second media service. |
| 5 | `archivetune/playback/MusicService.kt:2782` | no | Secondary crossfade player, same dead service. |
| 6 | `archivetune/ui/player/CanvasArtworkPlayer.kt:137` | ? | ArchiveTune canvas artwork player; reachability unconfirmed. |

So the app has **one audio player and at least two video/canvas players**. That is a
deliberate design — #2's own comment says "the service stream carries the audio; this
surface is video only" — but it does contradict the PRD's literal "single player
owner", and the earlier audits must not be trusted on this point. `PHASE1` and
`PHASE3` in this repo now carry the correction inline.

## 1. Recreation — the fullscreen video player is rebuilt on every entry (NOT fixed)

`FullscreenVideoScreen` builds its player in a keyless `remember { … }` and releases it
in `onDispose`. `remember` without keys is created once **per composition instance**,
and the screen is a navigation destination, so **each visit to the fullscreen video
constructs and tears down a whole ExoPlayer**. Player construction is expensive
(renderers, decoders, caches), which is exactly the "recreation" the PRD flags.

Fixing it means hoisting the video player to a longer-lived owner (or reusing the
canvas player) — a behavioural change to the video path that cannot be validated
without a device, so it is left as the top Phase 6 follow-up rather than done blind.

## 2. Recomposition — a 10 Hz position loop fed a mini player that is never drawn (FIXED)

`BottomSheetPlayer` ran, unconditionally:

```kotlin
LaunchedEffect(playbackState) {
    if (playbackState == Player.STATE_READY) {
        while (isActive) {
            delay(100)
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }
}
```

`position` and `duration` are used in exactly one place — the collapsed `MiniPlayer`
inside `collapsedContent` — and that block is guarded by `showCollapsedMiniPlayer`,
which is **false at the only call site** (`MainActivity`). So the loop was recomposing
the entire player sheet ten times a second for two values nothing read.

Now gated on `showCollapsedMiniPlayer`, so it starts only when a mini player will
actually consume it.

## 3. Mini-player regression — checked

The mini player the PRD worries about lives in `MusoNavbarHost`, not in this sheet. Its
position/duration were already moved to deferred providers in 0.5.225 (Phase 2, H2), so
the navbar mini player no longer recomposes on the position tick either. Nothing in this
round touches `MusoNavbarHost`.

Also noted: the Muso `ui/player/MiniPlayer.kt` (186 lines) is now reachable only from
the `showCollapsedMiniPlayer` branch, i.e. it is dead in practice — the same situation
as the dead lyric renderer found in Phase 5.

## 4. State machine — assessed, not rebuilt

The open/close state is `BottomSheetState` (`expandSoft` / `collapseSoft` / `isExpanded`
/ `progress`), driven from `MusoNavbarHost` and `MainActivity`. It behaves as a
three-anchor sheet with a soft expand/collapse. No state-machine defect was found that
could be fixed without a device; the recreation issue in §1 is the real one.

## 5. Artwork flash on song change (NOT fixed)

`MusoSuiteHost` sets `bitmapBridge = null` and then re-loads the artwork whenever
`mediaMetadata.id` changes, so the artwork blanks and repaints on every song change.
Keeping the previous bitmap until the new one decodes would remove the flash, but that
is a visible behaviour change and needs a device to judge, so it is left.

## Verification debt

Nothing here is runtime-verified — the sandbox has no Android SDK. Static checks only:
all 1,074 `.kt` files parsed with tree-sitter (no new errors; the 4 known false
positives unchanged) and the edited file brace/paren/bracket balanced. Per PRD §62 the
change in §2 may not be marked "fixed" until a build and a device run confirm it.
