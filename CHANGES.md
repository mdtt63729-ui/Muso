# Round 175 (v0.5.192, code 199) — CI fix round for v0.5.191

The v0.5.191 CI log (paste-1-35.md) failed with 50 compile errors, all in
the new Round 174 code plus one latent Round 173 typo:

1. FullscreenLyricsContent.kt — the Round 173 perf comment's last two lines
   were missing their `//` prefix (bare words parsed as code). Fixed.
2. ATPlayerStyles.kt:
   - SimpIcons' icons are extension properties, each needing its own
     import; the file imported only the receiver object. Added all 13
     (SkipPrevious, SkipNext, Favorite, FavoriteBorder, Lyrics, Fullscreen,
   QueueMusic, MoreVert, Pause, PlayArrow, PauseCircle, PlayCircle,
   KeyboardArrowDown).
   - `state.canvasData` -> `state.screenData.canvasData` (two places).
   - Removed the unresolvable `surfaceColorAtAlpha` import.
   - Removed the leftover `trackHeight` argument from one ATSlider call
     (the parameter had been deleted from the definition).
3. KitSettingsHost.kt — `WindowInsets.getLeft/getRight` take a
   LayoutDirection parameter; passed `LayoutDirection.Ltr`.
4. LyricsSettings.kt — `Modifier.height` used without import (an earlier
   substring check matched `heightIn` and skipped adding it). Added.
