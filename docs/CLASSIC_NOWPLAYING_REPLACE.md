# Classic NowPlaying player replaced from the supplied zip

Snapshot: **0.5.248**. Requested as a straight replacement ("1 — full replace").

## What was applied

`SimpMusic-Classic-NowPlaying_1.zip` holds 134 files under
`app/src/commonMain/kotlin/com/maxrave/simpmusic/`. Compared with this app:

| | count |
|---|---|
| identical to the app | 84 |
| differing | 36 |
| new to the app | 12 |

**121 files copied** into `app/src/main/kotlin/com/maxrave/simpmusic/` (12 of them new), replacing
the app's versions.

## What was deliberately NOT copied, and why

**The 11 KMP plumbing files.** The zip ships `expect` **declarations** (it is a `commonMain`
source set); this app is a plain Android module and holds the real implementations:

```
Platform.kt
expect/CopyToClipboard.kt   expect/OpenUrl.kt   expect/ToggleMiniPlayer.kt
expect/ui/CastButton.kt     expect/ui/DeviceVolumeController.kt
expect/ui/MediaPlayerView.kt  expect/ui/PhotoPicker.kt
expect/ui/PlatformBlur.kt   expect/ui/PlatformColorScheme.kt
expect/ui/SaveImagePermission.kt
```

Copying an `expect` declaration into a non-common source set does not compile. Every function
signature in these files was compared against the app's and **all of them match** — the app's own
satisfy the copied code. None of them is the player.

**One addition was required.** The zip's `CastButton.kt` declares three things the app's does not
— `CastReceiver`, `CastReceivers` and `rememberCastReceivers` — and the zip's new
`AppleMusicOutputSheet.kt` uses `rememberCastReceivers`. The app's `CastButton.kt` now declares
all three, with a no-op implementation, matching that file's existing stance (it already reports
`isPlatformCastAvailable() = false` because Muso has no Cast).

## Consequence, stated plainly

This replaces files that carried the fixes of the previous rounds — `LyricsView.kt` (the resume
button, the None-style fix, the Enhanced hook), `FullscreenLyricsContent.kt` (the fullscreen
close guard), `NowPlayingContentState.kt` (true-white text), `LiquidGlassContainer.kt` (the blur
reductions) and `NowPlayingScreen.kt` (the three-style cleanup). Those fixes are **not** in the
zip, so they are gone from those files. This was the chosen option.

The zip is itself a three-style build: its `NowPlayingScreen.kt` branches on
`NOW_PLAYING_STYLE_M3_EXPRESSIVE`, `NOW_PLAYING_STYLE_APPLE_MUSIC` and else → Spotify, and
references none of the seven styles this app removed.

## Verified

1,084 `.kt` files parse clean with tree-sitter, bar the known false positives (`AppModule.kt`,
`Cookies.kt`, `PlaylistViewModel.kt`, `ResolveAudioStreamUseCase.kt`) and the copied
`LyricsOffsetControl.kt`, which is brace-balanced (138 lines, 17/17 braces, 42/42 parens) and
looks like the same class of parser false positive. Not runtime-verified.
