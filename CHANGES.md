# Muso v0.5.20 — Changes (based on InnerTune dev + Echo-Music API stack)

## Streaming / Download (Echo-Music stack)

`innertube` module now uses the Echo-Music measured stream fallback chain:

ANDROID_MUSIC (logged in) → VISIONOS → ANDROID_VR 1.65.10 → ANDROID_VR 1.43.32 → IPADOS → IOS
→ legacy TVHTML5 + Piped URL merge (last resort).

- `innertube/.../models/YouTubeClient.kt` — added `VISIONOS`, `ANDROID_VR_1_65_10`,
  `ANDROID_VR_1_43_32`, `IPADOS` clients (exact Echo-Music values), updated `IOS`/`WEB_REMIX`
  client versions, added `STREAM_FALLBACK_CLIENTS` order.
- `innertube/.../models/Context.kt` — client context gained `osName`, `deviceMake`, `deviceModel`,
  `androidSdkVersion` (required by the new clients).
- `innertube/.../YouTube.kt` — `player()` rewritten to walk the fallback chain and return the
  first response that is OK and carries audio formats. Streaming and downloads both resolve
  through this function, so both benefit.

## Search

Same InnerTube `search` / `get_search_suggestions` endpoints, now sent with the current
Echo-Music `WEB_REMIX` client version.

## Lyrics (Echo-Music multi-provider system)

New provider modules (ported from Echo-Music, self-contained Ktor clients):

| Module | API | Notes |
|---|---|---|
| `youlyplus` | 6 LyricsPlus community servers (`lyricsplus.prjktla.my.id`, ...) | races all servers, remembers the fastest |
| `paxsenixlyrics` | `lyrics.paxsenix.org` | TTML word-level sync via TTMLParser |
| `unison` | `unison.boidu.dev` | TTML → LRC conversion |
| `betterlyrics` | `lyrics-api.boidu.dev` | TTML → LRC |
| `simpmusic` | `api-lyrics.simpmusic.org/v1` (+ `vivi-yt-music-server.onrender.com` fallback) | search by videoId |

App side:

- `LyricsProviderRegistry` with the Echo-Music default order:
  YouLyPlus → Paxsenix → Unison → BetterLyrics → SimpMusic → LrcLib → KuGou → YouTube Subtitle →
  YouTube Music. Order is user-configurable through the `lyricsProviderOrder` preference.
- `LyricsProvider` interface gained an `album` parameter (used by the new providers for better
  matching); LrcLib now also receives the album.
- `LyricsHelper` resolves providers through the registry.
- Settings → Content: on/off switch for each of the five new providers.
- Lyrics UI: active-line color and alpha now animate smoothly (350 ms FastOutSlowIn) instead of
  switching instantly.

## Apple Music lyrics UI (from SimpMusic)

Ported SimpMusic's Apple Music lyrics renderer into the player lyrics view:

- Every line renders at the same large size (26sp) — the active line is separated by focus, not
  scale: it is lit (theme primary color) while the rest of the page is grey.
- Depth of field: lines dim and progressively blur with distance from the sung line. Already-sung
  lines recede faster than upcoming ones (AMLL distance rule). Blur and dimming are animated
  (400 ms) so the focus glides down the page with the song.
- Pre-roll: during an intro (no line sung yet) all lines are uniformly dimmed and NOT blurred.
- Unsynced lyrics render fully lit, no blur.
- On devices below Android 12 (no RenderEffect) the blur gracefully degrades to dimming only.

Files: `ui/component/AppleMusicLyrics.kt` (new), `ui/component/Lyrics.kt` (updated).

## Build / CI

- `versionName` 0.5.20, `versionCode` 27, app name changed to **Muso**.
- `.github/workflows/build.yml` — pushes (and manual dispatch) build the **unsigned FOSS release**
  APK, rename it to `Muso_v0.5.20_v1.apk`, upload it as a workflow artifact and **create a GitHub
  Release with the APK automatically**. No secrets required (`GITHUB_TOKEN` is enough).
- `gradle.properties` — more daemon memory, parallel and cached builds.

## Notes

- The Echo-Music PoToken / cipher engine and its own app UI (Metrolist architecture) are NOT
  part of this port; the stream chain above is the part of the Echo-Music stack that works
  without them.
- License: GPL-3.0 (unchanged, both projects are GPL-3.0).
