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

## Video in player (Spotify/SimpMusic style)

The fullscreen player now plays the song's music video, like Spotify's canvas / SimpMusic's
fullscreen player:

- A secondary, muted ExoPlayer renders the lowest-resolution video stream for the current
  song (fetched through the same InnerTube player API used for audio), kept in sync with the
  main audio player: it follows play/pause and re-syncs after seeks (drift check every 2 s).
- The audio pipeline is completely untouched — cache, normalization, queue and equalizer all
  keep working on the audio stream; the video layer is display-only.
- Shown when the player sheet is at least half expanded; falls back to the regular artwork
  thumbnail when the song has no video or the fetch fails.
- Settings -> Player: "Show video in player" toggle (default on).

Files: `ui/player/PlayerVideo.kt` (new), `ui/player/Player.kt`, `ui/screens/settings/PlayerSettings.kt`,
`libs.versions.toml` + `app/build.gradle.kts` (added `media3-ui`).

## SimpMusic-style navigation, new icon, and branding (v0.5.20 final polish)

**Navigation bar** — the bottom bar now uses the SimpMusic tab set: Home, Library, Search.

- Old tabs (Songs / Artists / Albums / Playlists as separate bottom-bar entries) removed from
  the bar; all four now live inside the new combined Library screen as tabs (selection persists).
- The Search entry is an action like Spotify/SimpMusic: tapping it opens the search field
  (same as the search shortcut), so search suggestions and history work exactly as before.
- Default-open-tab setting simplified to Home / Library; old saved values fall back to Home.
- App shortcuts (Songs/Albums/Playlists) now open the Library tab.

**App icon** — every launcher icon replaced with the provided Muso logo (music note +
equalizer bars), generated from the original image at full quality (LANCZOS) for all densities:

- Adaptive icons: white background + black glyph foreground in the safe zone, plus a white
  monochrome layer for Android 13+ themed icons.
- Legacy square + round icons for older launchers.

**App name / branding** — the About screen and Discord settings screens had hard-coded
"InnerTune" text (this is why the app name still showed InnerTune); they now show Muso.
README retitled as well.

**CI fix** — the previous build failed in `android-actions/setup-android@v3` ("Failed to find
package 'tools'"): GitHub's Ubuntu runners already ship the Android SDK, so that step is
removed. The workflow now goes straight from JDK setup to `./gradlew assembleFossRelease`.

## Round 4: build fix, preferred provider setting, perf pass

- **CI fix (again): `./gradlew: Permission denied`** — the wrapper loses its execute bit when the
  source travels through Windows zip tools. The workflow now runs `chmod +x gradlew` before
  building, and the archive itself is packed with the bit set.
- **New setting: Preferred lyrics provider** (Echo-Music). A dropdown in Settings -> Content
  picks which provider is tried first; the rest follow in the default order. Written through the
  existing provider-order key, so no migration.
- **No-lag pass on lyrics**: `LyricsHelper` now reads the provider order with the suspend
  DataStore API instead of a blocking read, so provider resolution can never block the calling
  thread.
- **Scroll perf on the Apple Music lyrics view**: blur layers are now capped to lines within 5
  of the active one — far lines only dim. Fast scrolling no longer creates dozens of
  RenderEffects on low-end devices.

## Round 5: compile fix

- About screen referenced the old `R.drawable.launcher_monochrome` (moved to `mipmap` together
  with the icon rework). Now points at `R.mipmap.launcher_monochrome` — the only compile error
  from the first successful Gradle run.

## Round 6: SimpMusic-style fullscreen video, in-app updater, repo links

**Fullscreen video player** (the big one from the screenshots):

- The video now renders behind the *entire* player screen, SimpMusic-style, not in a small box.
- Tap anywhere on the video and every control (title, slider, buttons) fades out; tap again
  and they fade back in. No layout jump — pure overlay.
- Video quality is 720p by default, with a new Player setting "High quality video (720p)"
  to drop to 360p on slow connections.
- Falls back to the regular artwork layout when a song has no video, exactly as before.

**In-app updater:**

- The daily update check now points at github.com/mdtt63729-ui/Muso (release tag, e.g.
  v0.5.20-v1) instead of the InnerTune repo.
- When a newer release exists, an in-app "Update available" dialog appears: Download shows a
  live progress bar (system DownloadManager, no storage permission needed), then hands the
  APK to the package installer.
- After the update installs, the app relaunches itself (MY_PACKAGE_REPLACED receiver).
- Added REQUEST_INSTALL_PACKAGES permission + external-files provider path.

**Repo links:** every z-huang/InnerTune link in the app (About, Discord screens/settings/RPC,
releases link) now points at the Muso repo.

## Round 7 (v0.5.21): lyrics fixes, immersive video, HQ thumbnails, smoother animations

**Fixed:** Library screen tabs (Songs/Artists/Albums/Playlists) overlapped the status bar —
the TabRow now pads below the status bar.

**Fixed: lyrics not showing.** Tapping the lyrics button now actually switches to lyrics: the
full-screen video layer stands down while lyrics are on (previously it kept covering the
lyrics area). Both UIs coexist — new Player setting **"Lyrics style"**:
- *Apple Music style* (default): big bold type, focus blur/depth effect.
- *Classic (InnerTune)*: the original InnerTune look — smaller type, no blur.

**Immersive video mode:** while the full-screen video plays, the status bar hides and the
navigation bar hides as soon as you tap the controls away; swipe shows them transiently and
everything restores when the sheet collapses.

**Smoother motion:** tab-to-tab and page-to-page transitions are now directional slides with
proper easing (320ms FastOutSlowIn instead of a flat 250ms fade), and the play/pause button
animates with a scale+fade morph.

**Ultra-high-quality thumbnails:** a global Coil interceptor upgrades every YouTube
thumbnail to maxresdefault (1280px), falling back to hq720, falling back to the original —
applies everywhere (home, search, mini player, player).

**Fixed: home categories on first open.** The home/explore feeds now retry automatically
(2 extra attempts with backoff) when the InnerTube request fails transiently instead of
silently staying empty until a manual refresh. No fake categories — only what actually
fetches.

**Version:** 0.5.21 (versionCode 28); release tag v0.5.21, APK Muso_v0.5.21_v1.apk.

## Round 8 (v0.5.22): ReTune-style library system

The library was rebuilt around ReTune's (github.com/Juanoto2012/ReTune) library UX:

- **Filter chips row** (Playlists / Songs / Albums / Artists) at the top of every library
  screen, scrolling away with the content. The old tab row is gone. Tapping the active
  chip again returns to the full library view — exactly like ReTune.
- **Library mix view** (default): everything saved — playlists, albums, artists — in one
  mixed grid or list, sortable (Recent / Name, with the descending toggle), with a
  grid/list view toggle, and an in-library search that also matches songs.
- **Auto playlists**: Liked and Offline (downloaded) sit at the top of the mix grid and
  open a dedicated auto playlist screen with Play / Shuffle / song list and the standard
  long-press song menu.
- All existing sub-screens (Songs/Artists/Albums/Playlists) keep their own filters and
  sort headers, now nested under the top chips row.
- The chip selection and view type are remembered between launches.

Note: ReTune's Podcasts chip and YTM-uploaded/cached auto playlists are not ported — Muso
has no podcast or upload support in its data layer.

**Version:** 0.5.22 (versionCode 29); release tag v0.5.22, APK Muso_v0.5.22_v1.apk.

## Round 9 (v0.5.23): full Material 3 Expressive player port

The player's controls were rebuilt as a faithful port of SimpMusic's M3-Expressive layout
(the piece previously marked as not done):

- **Expressive transport row**: prev / play / next as three pills — prev/next on
  secondaryContainer, play on primary. The pressed pill *grows* x1.15 on a bouncy spring
  while the neighbours shrink proportionally, and the play button's corner radius morphs
  between 22dp (playing) and 34dp (paused). Buffering shows a circular progress instead of
  the icon; unavailable prev/next dim to 40%.
- **Track info row**: title + artists with marquee scrolling, and a 48dp expressive heart
  toggle (fills primaryContainer when liked) on the right.
- **Connected control group** (rounded caps, 48dp slots, 3dp gaps): shuffle | repeat |
  lyrics | video. Lyrics now toggles directly in the player (finally no digging in the
  queue sheet) and video can be switched on/off right there too. Active slots animate to
  primaryContainer.
- All animations use Material springs — no layout jumps, everything animates on top.

**Home first-open hardening:** the home feed retry now also treats a successful-but-empty
response as a failure and retries (3 attempts, backoff), so a flaky first response no
longer leaves the feed empty.

**Version:** 0.5.23 (versionCode 30); release tag v0.5.23, APK Muso_v0.5.23_v1.apk.

## Round 10 (v0.5.24): podcasts, uploaded and cached — the "impossible" list, done

The two items previously marked as not portable are now in, as real (non-fake) features:

**Podcasts (online, ReTune port):**
- New "Podcasts" chip in the library: lists the podcast shows saved on the logged-in
  YouTube Music account (fetched live from YTM).
- Tapping a show opens its episode list; tapping an episode plays it through the online
  queue path (same one search results use).
- Implementation is online-first: no DB migration, no sync jobs — nothing to break.
- Requires being logged in; otherwise the screen says so instead of showing broken UI.

**Uploaded (online):**
- New "Uploaded" auto playlist card in the library mix grid: the account's uploaded
  (privately owned) tracks, fetched live from YTM, tappable to play.

**Cached:**
- New "Cached" auto playlist card: songs fully present in the streaming cache (queried
  from the ExoPlayer cache), playable offline while the cache keeps them.

Under the hood: three new innertube endpoints (saved podcasts, podcast episodes,
uploaded songs) with their own parsing, a new MusicMultiRowListItemRenderer model for
episode lists, and a top-level videoId field on the responsive list item (uploaded
songs carry their id there).

**Version:** 0.5.24 (versionCode 31); release tag v0.5.24, APK Muso_v0.5.24_v1.apk.

## Round 11 (v0.5.25): settings pass over SimpMusic + Echo-Music

Went through the settings screens of both apps and ported everything that maps onto a
feature Muso actually has; four new settings landed in Player settings:

- **Lyrics text size** (Echo): 16-36sp slider driving both lyrics styles; the Apple Music
  blur depth-of-field scales with it (blur radius is em-based).
- **Lyrics blur effect** (Echo): turn the Apple Music-style blur off to keep only the
  dimming falloff — cheaper on older devices, a visual preference in its own right.
- **Auto-scroll lyrics** (Echo): when off, the page stops following the sung line and you
  scroll manually.
- **Keep screen on in player** (Echo): keeps the screen awake whenever the player sheet is
  expanded (reading lyrics, watching video). Managed alongside the immersive-mode window
  flags so nothing leaks after the player closes.

Already covered by Muso from the same two apps: theme mode, dynamic theme, pure black,
audio/download quality, skip silence, audio normalization, auto load more, auto skip next
on error, persistent queue, stop on task clear, video in player + HQ video, lyrics style,
lyrics translation, sleep timer, proxy, Discord presence, backup/restore, auto update
check, default open tab, default library chips.

**Version:** 0.5.25 (versionCode 32); release tag v0.5.25, APK Muso_v0.5.25_v1.apk.

## Round 20 (v0.5.34): fixed the empty workflow file

The GitHub Actions workflow (.github/workflows/build.yml) had been accidentally
truncated to 0 bytes since v0.5.26 by a bad version-bump script (the file was opened
for writing before being read), so no CI build ran for v0.5.26-v0.5.33. Restored the
full workflow from the last good copy (v0.5.25), updated it to v0.5.34 with current
release notes, and validated the YAML. The app code itself was unaffected - only the
workflow file was empty.

**Version:** 0.5.34 (versionCode 41); release tag v0.5.34, APK Muso_v0.5.34_v1.apk.

## Round 19 (v0.5.33): package renamed to com.muso.music

The application id / namespace changed from com.zionhuang.music to com.muso.music.
All source directories moved (main, foss, full source sets), all package declarations
and imports updated (206 files), manifest component names and the FileProvider authority
resolve from the new namespace automatically. The internal library modules
(innertube, kugou, lrclib, betterlyrics, kizzy, material-color-utilities) keep their
internal package names - they are build-time libraries, not part of the app identity.

IMPORTANT: this is a new app identity - the previously installed Muso (old package)
will not receive this as an update; install it as a new app (old data: logins, downloads
in the old app's storage stay with the old app).

**Version:** 0.5.33 (versionCode 40); release tag v0.5.33, APK Muso_v0.5.33_v1.apk.

## Round 18 (v0.5.32): full-codebase audit - 3 compile fixes

Audited every file changed in rounds 12-17 with automated checks (missing/duplicate
string resources, drawable references, import duplicates, brace/paren balance, route
registrations, preference keys, XML validity). Found and fixed 3 real compile errors:

1. MusicService: the crossfade watcher used unqualified STATE_READY and STATE_ENDED but
   only STATE_IDLE was imported - both imports added.
2. MainActivity home header used windowInsetsPadding(WindowInsets.statusBars) without
   importing windowInsetsPadding / statusBars - both imports added.
3. strings.xml had a duplicate <string name="equalizer"> (one pre-existing from the
   player menu, one from round 13's audio effects) - duplicate removed, resource
   linking now succeeds.

Everything else checked clean: all R.string / R.drawable references resolve, no
duplicate imports, all navigated routes are registered, all preference keys defined,
manifest / locales_config / strings XML valid.

**Version:** 0.5.32 (versionCode 39); release tag v0.5.32, APK Muso_v0.5.32_v1.apk.

## Round 17 (v0.5.31): SimpMusic-style home header - search bar and tiles removed

**Home screen redesign:** the search bar, the History tile row and the Stats tile are gone
from the home tab. In their place: a clean header with the app name "Muso", and next to
the settings button (same spot as before, top-right) a small history button. The
playlist shelves now start right under the header.

The search bar itself still exists - it stays on the Library tab and opens full-screen
from the Search tab in the bottom bar, exactly like before. The stats screen remains
reachable through its route; the account/login entry lives in Content settings.

**Version:** 0.5.31 (versionCode 38); release tag v0.5.31, APK Muso_v0.5.31_v1.apk.

## Round 16 (v0.5.30): lyrics romanization + animated artwork

**Lyrics romanization (real, offline):** Player settings gained a "Romanize lyrics"
switch. A new built-in transliteration engine (utils/Romanizer.kt) converts lyrics
written in Bengali, Devanagari, Cyrillic, Greek, Japanese kana (with small-tsu geminate
handling) and Korean hangul (algorithmic Revised Romanization) into Latin letters -
offline, instantly, for both synced and plain lyrics. English/mixed lyrics pass through
untouched. It runs once per lyrics load (remembered), never per frame, so there is no
scroll cost.

**Animated artwork (Ken Burns):** Player settings gained an "Animated artwork" switch -
the player's big artwork slowly breathes (a 15s zoom cycle anchored slightly off-center).
Pure GPU transform on the image layer, so it costs nothing while scrolling or interacting.
This is the app-side animated-artwork effect; YouTube's canvas loop videos are a
separate server-side feature and are not part of this.

**Version:** 0.5.30 (versionCode 37); release tag v0.5.30, APK Muso_v0.5.30_v1.apk.

## Round 15 (v0.5.29): in-app language switch

**In-app language switch (real, all Android versions):** Content settings gained an
"App language" picker with System default plus the 41 languages Muso ships translations
for (including Bengali). Picking one recreates the activity over a locale-configuration
context, so every string in the app follows immediately — no fake locale tricks, works
from Android 6 to the latest. A locales_config.xml was also added so Android 13+ shows
the same languages in the system's per-app language settings.

**Version:** 0.5.29 (versionCode 36); release tag v0.5.29, APK Muso_v0.5.29_v1.apk.

## Round 14 (v0.5.28): crossfade + player background + translucent navigation bar

**Crossfade (real, fade-based):** Player settings gained a Crossfade switch and a 1-12s
duration slider. During the last N seconds of a track the volume eases down, and the next
track fades back in after every transition — including repeat-one. The fade lives inside
the service's existing volume pipeline (playerVolume x normalization x crossfade factor)
so it never fights the volume slider or audio normalization; manual mid-track skips are
unaffected, seeking backwards restores full volume, and stopping playback always resets
the volume to full.

**Player background styles:** Player settings gained a "Player background" selector —
Default, or Blurred artwork: the current song's artwork, heavily blurred, behind the
whole player with a theme-aware scrim (liquid-glass look). Gives way automatically when
the full-screen video background is showing.

**Translucent navigation bar (real):** Appearance settings gained a "Translucent
navigation bar" switch — when on, the bottom bar becomes semi-transparent and the content
scrolls behind it, like SimpMusic's. The bar's slide-away animation is untouched.

**Version:** 0.5.28 (versionCode 35); release tag v0.5.28, APK Muso_v0.5.28_v1.apk.

## Round 13 (v0.5.27): real audio effects + categorized settings

**Audio effects (REAL, not fake toggles):** a new "Audio effects" screen with the
device's actual DSP — Equalizer (device's own bands + preset names, or custom band
sliders), Bass boost, Virtualizer and Reverb — all from android.media.audiofx, applied
to the player's audio session by a new AudioEffectsManager in the playback service.
The settings screen only writes DataStore preferences; the service collects them and
applies them to live effect objects, so the UI can never crash or block playback. Every
audiofx call is wrapped so devices without support degrade to "Equalizer is not
available" instead of crashing. Effects auto re-attach if the audio session changes
(device/route switches). Slider writes go to DataStore (async) — no lag.

**Settings reorganized into categories:** the settings home now groups entries under
primary-colored category headers with icons (Customization / Player and audio /
Storage and backup / Integrations and privacy / About), matching the reference apps'
look.

**Version:** 0.5.27 (versionCode 34); release tag v0.5.27, APK Muso_v0.5.27_v1.apk.

## Round 12 (v0.5.26): thumbnail skeletons + no-reload on re-entry + queue position

**Skeleton shimmer for thumbnails:** every card thumbnail (song rows, grid cards,
playlist covers — including the 4-image collage covers) now shows a soft moving shimmer
while the image downloads and crossfades in when it arrives, instead of a blank gap.
Implemented with rememberAsyncImagePainter (no per-card subcomposition) so scrolling
stays smooth.

**Home does not reload on re-entry:** the home feed (quick picks, recommendations,
account playlists, home/explore pages) is now cached for the lifetime of the app
process. Minimize, back out and reopen — content is there instantly. Only a full close
(process death) refetches. Pull-to-refresh still refetches.

**Last played song / position:** the persistent queue (existing feature) already
restores the last queue and song into the mini player, paused, on app open; this round
the periodic position save went from every 30s to every 10s, so the restored position is
within seconds of where you left off. Requires the "Persistent queue" setting to be on
(Player settings; it is on by default).

**Version:** 0.5.26 (versionCode 33); release tag v0.5.26, APK Muso_v0.5.26_v1.apk.

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
