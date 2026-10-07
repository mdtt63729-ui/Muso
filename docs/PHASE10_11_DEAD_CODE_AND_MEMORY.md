# Muso — Phase 10 completion + Phase 11: Dead code and memory

Snapshot: **0.5.231**.

---

## Part A — dead code removed (Phase 10 remainder + the Phase 5 leftover)

### A1. `com.muso.music.ui.component.Lyrics` deleted — 646 lines

Verified unreferenced before deleting: no call site in any source set (`main`, `foss`,
`full`, `debug`), no import of it, no wildcard import of its package. The six `Lyrics(`
matches elsewhere are the unrelated `com.maxrave.domain.data.model.metadata.Lyrics`
data class.

Its one live symbol, `animateScrollDuration`, moved to `lyrics/LyricsUtils.kt` — its only
consumer — and the import dropped. The file's other top-level names
(`LyricsPreviewTime`, `KaraokeLyricsLine`, `KaraokeWord`, `LyricsMorphLoading`) had no
references either.

### A2. `MiniPlayer.kt`: Flat / M3-Flex render branches removed — 224 lines

After the enum removal in 0.5.223, two constant-false flags (`usePremiumFlatStyle`,
`useM3FlexStyle`) kept the old branches compiling but unreachable. Removed, together with
everything they guarded:

- the M3-Flex content branch and the `M3FlexMiniPlayerContent` composable (125 lines);
- the `FlatMiniPlayerBackground` composable (38 lines) and the overlay that called it;
- the two Flat-only previous / next `IconButton`s;
- the Flat / M3-Flex alternatives for the card shape, card colour, artwork size and
  progress-ring size;
- the two parameters threaded through `MiniPlayerAndroidPlaybackContent`.

`MiniPlayer.kt`: 1,524 → 1,300 lines. Verified afterwards: no dangling reference to any
removed symbol, and the file parses and balances.

**Correction to my own estimate:** I described this as "~600 lines". The actual dead span
is **224 lines**. The earlier figure was wrong.

Total dead code removed this round: **870 lines**.

---

## Part B — Phase 11: memory / lifecycle

### B1. `MusicService` never cancelled its coroutine scopes — FIXED

`scope` (`Dispatchers.Main + Job()`) and `canvasScope` (`Dispatchers.IO + SupervisorJob()`)
are created in `onCreate` and were **never cancelled**. `onDestroy` released the player,
the media session, the volume observer and the audio effects, but left both scopes
running — so queue loading, crossfade and canvas-preload coroutines kept executing against
a destroyed service and held it alive.

Both are now cancelled at the top of `onDestroy`.

### B2. An uncancelled `CoroutineScope` in `MusoSuiteHost` — FIXED

The playback-command sink was installed from a `LaunchedEffect` that created its own
`CoroutineScope(Dispatchers.Main)`. Nothing cancelled it, so queued playback commands
could outlive the composable that installed them. It now uses the effect's own scope
(`this`), which is cancelled with the effect.

### Checked and found clean

Listener and observer pairing in the live layer is correct:

| owner | add | remove |
|---|---|---|
| MusicService (player) | 329, 331 | 1460, 1461 |
| PlayerConnection | 97 | 202 |
| MainActivity | 750 | 752 |
| FullscreenVideoScreen | 185 | 187 |
| Player.kt (codec listener) | 198 | 202 (onDispose) |
| MusicService (volume ContentObserver) | 381 register | 1448 unregister |

`DownloadManager` listeners live for the singleton's lifetime by design.

### Remaining

- `LoginScreen` launches a one-shot account fetch in `GlobalScope` from a `WebViewClient`
  callback — bounded, but it outlives the screen. Fixing it needs a scope threaded into
  the WebView factory.
- `MediaLibrarySessionCallback` holds its own `CoroutineScope + Job` and is not cancelled
  when the service is destroyed.
- `DownloadUtil` creates a second unstructured scope for the Wi-Fi-only observer; it is
  an app-lifetime singleton, so that one is intentional.

---

## Verification debt

Nothing here is runtime-verified — the sandbox has no Android SDK. Static checks only:
1,073 `.kt` files parsed with tree-sitter (one fewer than before, the deleted file; no new
errors, the 4 known false positives unchanged), every edited file parses clean, and each
was bracket-balanced (the one file that reports "unbalanced" is `LyricsUtils.kt`, whose
regex literals contain `[` / `]` — balanced once string literals are stripped, and
tree-sitter reports no error). Per PRD §62 none of this may be marked "fixed" until a
build and a device run confirm it.
