# Lyrics — three animation styles, and the smoothness fixes

Snapshot: **0.5.238**.

## 1. The animation styles are now exactly three

`LyricsAnimationStyle` (Muso) had six values — `NONE`, `FADE`, `KARAOKE`, `LYRICS_V2`,
`V2_MODE`, `ENHANCED` — and several of them produced the same visible animation, which is
why picking a style changed nothing. It is now exactly the three asked for, in this order:

| style | what it renders |
|---|---|
| **Enhanced** | the word-by-word glow / focus animation (the Echo renderer) |
| **Immersive** | the Apple-Music lyrics sheet — blurred backdrop, centred lines, dimmed neighbours |
| **None** | static lines, no animation |

- Default is **Enhanced** (was `LYRICS_V2`).
- The settings picker lists the three; `Immersive` is a new label (`lyrics_style_immersive`).
- **Immersive selects the Apple-Music sheet directly**: `LyricsView.appleStyle` is now true
  when the animation style is Immersive, *or* when the suite's lyrics style says so, so the
  choice works even when the player style is not Immersive. The `MusoSuiteHost` bridge that
  writes `dsm.lyricsStyle` also takes the animation style into account, so the Apple-Music
  view's own layout flag agrees.
- The Echo renderer's two `when (style)` dispatches lost the removed branches and gained
  `IMMERSIVE` as a fallback alias of `ENHANCED` (it is not normally reached, because
  Immersive renders through the Apple sheet). A no-op `if (style == KARAOKE) eased else
  eased` was collapsed to `eased`.
- **Migration is automatic and crash-safe**: the preference is read with `toEnum`, which
  falls back to the default when a saved value no longer names an enum entry, so a device
  that had `FADE`/`KARAOKE`/`LYRICS_V2`/`V2_MODE` stored simply comes up on **Enhanced**.
- The ArchiveTune layer has its **own** `LyricsAnimationStyle` (NONE/FADE/GLOW/SLIDE/
  KARAOKE/APPLE) and its own key; it is a different type and was left untouched.

## 2. Smoothness

The lyrics view's cost was in the depth-of-field blur. `appleMusicLyricFocus` applied
`Modifier.blur` to **every** visible line, with the radius driven by
`animateDpAsState(tween(400))` — so every line carried its own `RenderEffect`, and every
line's effect was re-issued on every frame of the 400 ms tween after each line change. On
a GPU that is a per-frame blur pass per visible line.

Three changes:

1. **The blur radius is no longer animated.** The alpha still animates (a float animation,
   no `RenderEffect`), so the focus still glides; the radius is now a plain value, which
   removes the per-frame re-issue.
2. **Only the two lines either side of the sung line are blurred.** Past that distance the
   line is already dimmed to `MIN_LINE_ALPHA`, where the blur is not visible — so those
   lines now rely on the alpha alone. This cuts the number of blurred layers per frame from
   "every visible line" to at most two.
3. **The Lyrics blur setting now works.** It existed in Settings (`LyricsBlurEnabled`, read
   in `musoLyricsRows`) but nothing consumed it — the Apple-Music lines hard-coded
   `blurEnabled = !isDragging`. It is now read and honoured, so turning blur off removes
   the remaining two passes entirely.

## Verified

All edited files parse clean with tree-sitter and are brace/paren balanced. Not
runtime-verified — no Android SDK here. Per PRD §62 this may not be marked "fixed" until a
build and a device run confirm it.
