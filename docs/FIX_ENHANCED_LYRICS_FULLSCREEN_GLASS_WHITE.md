# Four fixes: Enhanced lyrics, fullscreen lyrics, Minify+Liquid Glass, text colour

Snapshot: **0.5.244**.

## 1. Enhanced lyrics showed only the loader

The Enhanced renderer got its lyrics from ArchiveTune's `LyricsRenderViewModel`, which runs
`PrepareLyricsUseCase` — and that reads `database.lyrics(mediaId)`, **ArchiveTune's own lyrics
table**. That table is empty in this app: Muso stores lyrics in its own database. So the
ViewModel could only ever emit `Loading`, which is exactly the spinner the Enhanced style showed
while every other style showed lyrics.

`MusoEnhancedLyrics` no longer uses that ViewModel. The suite already hands its parsed lyrics to
`LyricsView`, so they are mapped straight into the renderer's own model
(`PreparedLyrics` / `PreparedLyricsLine` / `PreparedLyricsTrack`):

- word-synced when any line carries syllables, otherwise line-synced, and `PLAIN` when the
  source says the lyrics are unsynced;
- the line's span is divided evenly between its syllables for word timing;
- `textSizeSp` comes from the app's own lyrics text size, so the size the user picked is kept;
- the renderer's own romanizer is left off, because Muso romanizes separately.

Absent lyrics now mean `Loading` (still fetching) or `Empty` (fetch concluded without any) —
the same two states, just driven by this app's data.

## 2. The fullscreen lyrics page closed the moment it opened

Every player style except immersive draws a header in the fullscreen lyrics page — the artwork
and the title/artist block — and **that header is tappable-to-dismiss**. It also sits exactly
where the button that opens the page sits in those styles. So the opening gesture could land on
the header and shut the page instantly. Immersive draws a different header, which is why it was
the one style that worked.

The header tap, the back press and the dialog's own dismiss all route through
`FullscreenLyricsSheet.requestClose`, so one guard there covers every path: a close request that
arrives while the page is still opening is ignored.

## 3. Minify + Liquid Glass drew no mini player

With Liquid Glass on, it is `LiquidGlassAppBottomNavigationBar` that draws the mini player, and
it decided whether to draw its integrated pill from the **SimpMusic bridge's** `nowPlayingState`.
That bridge is asynchronous and can stay empty (or permanently stale after a process restore) —
the same fault the comment on `App.kt`'s copy of this rule already records, and the reason
`MusoNavbarHost` deliberately stops gating the other mini-player variants on it.

The pill is now drawn when **either** source has a track, and the bar is handed the live
`PlayerConnection` so the pill's title and artwork come from the real queue instead of the bridge
that may be empty.

## 4. The title / artist / time text was never true white

`dynamicTextColor` forced HSV value to `0.96` — 96% brightness, about `#F5F5F5`, not white — and
every label drawn on top of it was then dimmed again by alpha:

| label | before | after |
|---|---|---|
| `dynamicTextColor` | `hsv[2] = 0.96f` | `hsv[2] = 1f` |
| artist name | `.copy(alpha = 0.72f)` | full colour |
| elapsed + total time | `.copy(alpha = 0.78f)` | full colour |
| `secondaryForeground` | `foreground.copy(alpha = 0.72f)` | `foreground` |

## Files

| file | change |
|---|---|
| `com/muso/music/ui/component/MusoEnhancedLyrics.kt` | app lyrics → render model; no ViewModel |
| `.../ui/component/LyricsView.kt` | passes `lyricsData` in; close guard |
| `.../ui/component/LiquidGlassAppBottomNavigationBar.kt` | live-player fallback + connection |
| `com/muso/music/ui/player/MusoNavbarHost.kt` | passes the connection |
| `.../ui/screen/player/content/ATPlayerStyles.kt` | true-white foreground |

## Verified

All 1,074 `.kt` files parse clean with tree-sitter (the 4 known false positives unchanged), the
five touched files are brace/paren balanced, and every symbol the new mapper names was checked to
exist at the path it is imported from. Not runtime-verified — no Android SDK here.
