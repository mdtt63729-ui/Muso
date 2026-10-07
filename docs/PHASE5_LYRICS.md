# Muso — Phase 5: Lyrics

PRD §63, Phase 5. Snapshot: **0.5.227**. The PRD asks for provider architecture,
parsing, synchronization, animation and the fullscreen overlay.

---

## 1. Provider architecture — already in place (unchanged)

Nine providers behind `LyricsProviderRegistry` (YouLyPlus, Paxsenix, Unison,
BetterLyrics, SimpMusic, LrcLib, KuGou, YouTubeSubtitle, YouTubeMusic), ordered by the
user through `LyricsProviderOrderKey`. `LyricsHelper` queries every enabled provider
**in parallel** and takes the first non-blank result in priority order, with an
LruCache (3 result-sets / 128 resolved lyrics). Nothing needed changing.

## 2. Parsing — FIXED: three LRC variants were silently dropped

`LyricsUtils.LINE_REGEX` / `TIME_REGEX` demanded `[mm:ss.xx]` exactly — two-digit
minutes, two-digit seconds, and a 2–3 digit fraction after a dot. A line using any
other common LRC spelling was **not matched at all**, so `parseLine` returned null and
the line disappeared from the lyrics:

| variant | example | before | after |
|---|---|---|---|
| no fraction | `[00:12]Hello` | dropped | parsed (0 ms) |
| single-digit minute | `[0:12.34]Hello` | dropped | parsed (340 ms) |
| colon fraction (older LRC) | `[00:12:34]Hello` | dropped | parsed (340 ms) |
| tenths | `[1:02.5]Short ms` | dropped | parsed (500 ms) |

The new patterns accept 1–2 digit minutes/seconds and an optional 1–3 digit fraction
after `.` or `:`. The fraction is scaled by its digit count (tenths / hundredths /
thousandths) and a colon fraction reads as centiseconds, which is what the older LRC
format means.

Validated against 17 sample lines: every line that parsed before parses **identically**
after; the five new variants above now parse; metadata tags (`[ar:]`, `[ti:]`, `[by:]`,
`[offset:]`), tag-only lines and out-of-range timestamps are still rejected.

## 3. Synchronization — FIXED

- `findCurrentLineIndex` was a **linear scan** called on every position tick — and the
  lyric view samples position at 60 Hz. It is now a binary search over the sorted list.
  Equivalence proven: 20,000 randomised trials plus edge cases (empty list, before the
  first line, past the last line) give identical results to the old scan.
- `LyricsEntry.compareTo` used `(time - other.time).toInt()`. For far-apart timestamps
  that difference overflows `Int` and the comparison reports the wrong ordering, which
  makes `.sorted()` unreliable. Now `time.compareTo(other.time)`.

## 4. Animation — NOT DONE, one inconsistency flagged

The lyric view samples position every **16 ms (60 Hz)** while the item comment in the
same file describes "the live **50 ms** karaoke position". One of the two is stale.
Changing the tick changes visible karaoke smoothness, so it needs a device to judge;
left as-is and flagged.

## 5. Fullscreen overlay — NOT DONE

The overlay is `FullscreenLyricsContent` (1,255 lines). Not refactored.

## 6. Renderer consolidation — a FINDING, not done

Four lyric renderers exist, and **one of them is dead**:

- `com.muso.music.ui.component.Lyrics` (646 lines) has **no call sites in any source
  set** (`main`, `foss`, `full`, `debug`). Its private helpers `KaraokeLyricsLine`,
  `KaraokeWord` and `LyricsMorphLoading`, and `LyricsPreviewTime`, are likewise
  unreferenced. Only `animateScrollDuration` is live — it is used by `LyricsUtils`.

Deleting a whole file is destructive, and it would break the build if any reference
exists that a text search cannot see, so the file is **left in place** and flagged here
for confirmation. Recommended next step: delete it and move `animateScrollDuration`
into `LyricsUtils`.

The three live renderers are the suite's `LyricsView` (1,669 lines),
`FullscreenLyricsContent` (1,255) and `AppleMusicLyricsView`. Collapsing them onto one
is the remaining Phase 5 work.

## Verification debt

Nothing here is runtime-verified — the sandbox has no Android SDK. Static checks only:
all 1,074 `.kt` files parsed with tree-sitter (no new errors; the 4 known false
positives unchanged), the two edited files parse clean, and the new regexes were
exercised against 17 sample lines and the binary search against 20,000 randomised
trials before being written. Per PRD §62 none of this may be marked "fixed" until a
build and a device run confirm it.
