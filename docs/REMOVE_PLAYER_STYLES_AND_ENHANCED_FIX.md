# Three styles only, the picker finally wired, and the Enhanced blank sheet

Snapshot: **0.5.247**.

## 1. The player-style picker had never reached the player

`NowPlayingScreen` chooses the fullscreen layout from `DataStoreManager.nowPlayingStyle`. That
value was written by exactly one place: a `LaunchedEffect` inside **`MusoSuiteHost`** — a
composable with **no caller anywhere**. So the preference was written by Settings and never read
by the player: every style rendered the same one. That is why changing the setting did nothing.

The mapping now lives in the live player host (`Player.kt`), which already read the preference
and did nothing with it:

```kotlin
LaunchedEffect(playerStyle) {
    styleBridge.nowPlayingStyle.value = when (playerStyle) {
        PlayerStyle.EXPRESSIVE -> NOW_PLAYING_STYLE_M3_EXPRESSIVE
        PlayerStyle.IMMERSIVE  -> NOW_PLAYING_STYLE_APPLE_MUSIC
        else                   -> NOW_PLAYING_STYLE_SPOTIFY   // CLASSIC, "Classic V2"
    }
}
```

## 2. Seven player styles removed

Kept exactly three, matching the settings labels:

| enum | label | renderer |
|---|---|---|
| `PlayerStyle.CLASSIC` | Classic V2 | `NowPlayingContentSpotify` |
| `PlayerStyle.EXPRESSIVE` | M3 Expressive | `NowPlayingContentM3Expressive` |
| `PlayerStyle.IMMERSIVE` | Immersive Nightly | `NowPlayingContentAppleMusic` |

Removed: MODERN, MINIMAL, CINEMATIC, LITTLE, IMMERSIVE_EXTENDED, MATERIAL_EXTENDED, EDITORIAL —
from the enum, the settings labels, the live `when`, the `NOW_PLAYING_STYLE_*` constants, and
`strings.xml`.

**Deleted outright:**
- `ui/screen/player/content/ATPlayerStyles.kt` — **2,379 lines**, all seven designs plus their
  private helpers. Nothing else referenced any of it.
- `ui/player/MusoSuiteHost.kt` — dead, and the only holder of the mapping above.

A stored style that no longer exists is safe: `String?.toEnum` catches the
`IllegalArgumentException` and falls back to its default.

## 3. Enhanced lyrics came up completely blank

`MusoEnhancedLyrics` fetched ArchiveTune's database through `KitRuntimeAccess.database()` inside a
`runCatching`. When that threw, the whole connection became null, `LyricsEnhanced` bailed on its
first line, and the sheet rendered **nothing at all** — no lyrics, not even the loader.

The Enhanced renderer reads only `player`, `mediaMetadata` and `playbackParameters`, so the
database is now **optional** on the bridge: the three database-backed flows emit null and the
local-media extraction is skipped when it is absent, instead of the connection failing.

And a second net: if the connection still cannot be built, `MusoEnhancedLyrics` renders the words
plainly rather than an empty sheet.

## Files

| file | change |
|---|---|
| `com/muso/music/ui/player/Player.kt` | the style mapping, in the live path |
| `com/muso/music/constants/PreferenceKeys.kt` | `PlayerStyle` down to three |
| `com/muso/music/ui/screens/settings/MusoSettingsSections.kt` | labels |
| `.../ui/screen/player/NowPlayingScreen.kt` | seven branches out |
| `.../domain/manager/DataStoreManager.kt` | seven constants out |
| `.../ui/component/ModalBottomSheet.kt` | queue's removed-style special case |
| `res/values/strings.xml` | seven strings out |
| `.../archivetune/playback/PlayerConnection.kt` | database optional on the bridge |
| `com/muso/music/ui/component/MusoEnhancedLyrics.kt` | optional database + plain fallback |
| `ATPlayerStyles.kt`, `MusoSuiteHost.kt` | **deleted** |

## Not fixed in this round

- **The bottom row (info / add-to-playlist / queue) and the fullscreen-lyrics button dropping the
  player to the mini player.** Every one of them opens a second window, and so does the
  three-dot menu — which instead does nothing. That points at the nested sheets, but nothing in
  the sheet, the menu or `NowPlayingScreen` calls a collapse: the only unguarded collapse path in
  the app is the player sheet's own `BackHandler`. A logcat of one tap is what will settle it.
- **The three-dot menu doing nothing.**

## Verified

1,072 `.kt` files parse clean with tree-sitter (the 4 known false positives unchanged; the count
dropped by two with the deleted files). No dangling references to any removed symbol: every one
of the seven composables, the seven constants, the seven enum entries, `immersiveQueue`,
`ATPlayerStyles` and `MusoSuiteHost` greps to zero. Not runtime-verified.
