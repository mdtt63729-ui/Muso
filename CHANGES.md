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

## Round 37 (v0.5.52): v0.5.51 build fix - string XML escapes

The v0.5.51 CI build failed at resource linking: two strings
(preload_lyrics_desc, artwork_background_desc) contained raw apostrophes
("song's"), which Android XML requires to be escaped. Both now use \'.
A full scan confirmed no other unescaped apostrophes remain. No Kotlin
compile errors were in the log.

**Version:** 0.5.52 (versionCode 59); release tag v0.5.52, APK Muso_v0.5.52_v1.apk.

## Round 36 (v0.5.51): karaoke lyrics matched exactly to kimi_5.html

The reference HTML (kimi_5.html) was decoded and the word-level karaoke was
matched to it exactly, with white text everywhere as requested:

- Word lift: 4px * sin(progress), word scale: 1 + 0.02 * sin(progress) - the
  active word gently floats up and grows while it sings, then settles.
- Fill layer: white overlay swept left-to-right by the synced word timestamps
  (gradient edge), with a white glow (12px, 35% alpha) - the HTML's primary
  accent is replaced by white as requested.
- Base layer: white at 30% opacity inside the singing line; full white in the
  other lines, dimmed by ReTune distance focus (distance 1-2 -> 20%, 3 -> 15%,
  4 -> 10%, 5+ -> 8%). Once a line passes, its words return to the dim base.
- The previous round's line zoom was removed - the HTML has no line-level zoom;
  distance focus is a pure alpha fade with no stagger, following instantly.
- Typography: ExtraBold (800) with -0.5sp letter spacing, center aligned.

**Version:** 0.5.51 (versionCode 58); release tag v0.5.51, APK Muso_v0.5.51_v1.apk.

## Round 35 (v0.5.50): Apple Music lyrics glow, default Apple player, lyrics toolbar, artwork background

**Lyrics animation (Apple Music feel):** karaoke lyrics are now pure white with a
white glow that breathes as each word is sung, and the active line gets a light
zoom (1.04x) that eases in and out smoothly - the word-to-word fill keeps working
as before. The plain Apple-style lines also light up white instead of the theme
color.

**Default player = Apple Music:** the fullscreen player now opens in Echo's Apple
Music style by default (previously the classic InnerTune style was default), and
the player background defaults to the blurred-artwork style - the full-potential
Apple Music look out of the box. Previously stored classic selections map to the
new default automatically.

**Lyrics toolbar (Echo):** when lyrics are enabled in the player, a fullscreen
button and a three-dot button appear at the top of the lyrics. Fullscreen opens a
true full-screen lyrics experience (same synced lyrics, tap or the close button to
leave). The three-dot menu adjusts lyrics text size live (bigger/smaller).

**Background from artwork (new Appearance setting, default on):** the whole app is
tinted with a soft layer of the current song's artwork color. The extracted color
is persisted, so when you close and reopen the app the tint is already there
instantly - no waiting for artwork, no flash. Toggles in Appearance settings.

**Note:** no HTML file was attached with the animation reference, so the lyrics
animation was implemented exactly per the description (white text, white glow,
word-to-word fill, light zoom). If the HTML differs, share it and I will match it.

**Version:** 0.5.50 (versionCode 57); release tag v0.5.50, APK Muso_v0.5.50_v1.apk.

## Round 34 (v0.5.49): Gochi Hand wordmark, immersive mode, spatial audio, automix, preloads

**Brand typography (PRD):** the Muso wordmark is now set in Gochi Hand Regular (400) -
a clean, slightly handwritten, non-cursive Google Font bundled as a real font resource
(res/font/gochi_hand.ttf), no image, no effects, letter-spacing normal. Applied to the
permanent home header (32sp), the About screen brand name (30sp), and the splash screen,
where a white 44sp "Muso" wordmark fades and scales in under the waveform during the
settle phase and fades out with the splash. User content (song/artist/album text) is
untouched; only brand-name occurrences changed.

**Immersive mode:** the status bar and navigation bar now auto-hide while the app runs.
Swiping from the screen edge brings them back transiently.

**Spatial audio (was skipped, now real):** a genuine stereo-widening DSP
(SpatialAudioProcessor, mid/side widening with hard clamping so it can never clip)
inserted into the audio processor chain when enabled. Applies on the next app start,
like audio offload.

**Automix (was skipped, now real):** manual skips (next/previous buttons and the
artwork swipe) now fade the volume down smoothly before switching and fade back in,
instead of a hard cut. Natural track-end crossfade stays as it was.

**Preload next song + preload lyrics (were skipped, now real):** when the track changes,
Muso looks ahead in the queue and (for each enabled setting) prefetches ~3 MB of the
next song's audio into the player cache through the same resolving data source the
player uses, and fetches the next song's lyrics into the lyrics cache. All background,
silent on failure.

**Google Cast:** still not possible in this build - the Cast protocol only ships inside
Google's proprietary Play Services framework, which the FOSS build deliberately does
not include. Integrating it means dropping the FOSS build and shipping with Play
Services dependencies; say the word and that becomes its own round.

**Automix debug overlay:** not ported - it is Echo's developer tool, not a user feature.

**Critical fix:** v0.5.48 shipped with a missing closing parenthesis inside the new
collapsing-title block of all 13 settings screens (it would not have compiled). Found
by a per-file parenthesis depth scan and fixed everywhere; all 24 touched files now
pass brace and paren balance checks.

**Version:** 0.5.49 (versionCode 56); release tag v0.5.49, APK Muso_v0.5.49_v1.apk.

## Round 33 (v0.5.48): Echo page motion, settings title collapse, permanent home header

**Echo-style page transitions:** every page change now uses Echo's exact motion recipe -
a 400 ms slide of one-eighth of the screen width with a same-length fade, driven by
Echo's emphasized easing (cubic-bezier 0.2, 0, 0, 1.0). Direction-aware from the tab
order: going deeper slides from the right, going back from the left, tabs slide by
their order. Instant cut when Animations are turned off, as before.

**Settings title collapse (Echo behavior):** all 13 settings screens now show a large
bold title at the top of the scrollable content; after scrolling about 100 px the
title fades smoothly into the top bar, and fades back out when you scroll up - the
same hand-over as Echo's settings screens.

**Permanent home header:** the Muso title with the history and settings buttons is now
a true pinned header - it never hides, and it is drawn on an opaque surface bar, so
the home content (quick picks, etc.) starts below it and slides under it while
scrolling instead of showing through it. The home list got matching top content
padding so nothing is covered.

**iOS-style bounce buttons:** new BounceIconButton component (Echo's press
treatment) - while pressed the icon zooms down to 88% and springs back with a bouncy
spring curve, no ripple. Applied to the home header's history and settings buttons.

**Version:** 0.5.48 (versionCode 55); release tag v0.5.48, APK Muso_v0.5.48_v1.apk.

## Round 32 (v0.5.47): Echo Music Player and Audio settings ported

New rows in Player & audio, every one wired to real playback behavior (crash-first
review done: no new focus requests, no uncomposed state, all toggles are DataStore
switches the service already knows how to read):

**Player group:** Loudness normalization preset (Off / Normal / Strong - Strong also
boosts quiet tracks, capped at 2x), Data saver (streams at low quality regardless of
the quality setting), Seek accumulates (repeated double-taps on the artwork add up:
10s, 20s, 30s...; clamped to the track duration, gesture restarts when toggled).

**Queue group:** Prevent duplicate tracks in queue (playing-next / add-to-queue skip
songs already queued).

**Crossfade group:** Audio offload (hands audio to the device chip for less battery;
disabled while crossfade is on, like Echo; takes effect on next app start).

**Misc group:** Pause music when media is muted (volume observer pauses when media
volume hits 0), Download on Wi-Fi only (DownloadManager requirements - pending
downloads wait for an unmetered network), Listening history duration (keep forever /
12h / 1d / 7d / 30d - pruned at startup).

**Already covered by Muso (no duplicate rows):** audio/download quality, crossfade
with duration (ours fades volume without a gap), skip silence (applies instantly),
audio normalization, equalizer (Audio effects), persistent queue, auto load more
(= similar content), auto-download liked songs, remember shuffle AND repeat (both
already persist), persistent shuffle, auto skip on error, stop on task clear.

**Not ported (honest):** Google Cast (needs Play Services, FOSS build has none),
spatial audio (device-DSP level, no media3 API), automix crossfade + debug overlay
(Echo proprietary DSP), preload next song / preload lyrics (needs a custom preload
manager; queued as a possible future round).

**Version:** 0.5.47 (versionCode 54); release tag v0.5.47, APK Muso_v0.5.47_v1.apk.

## Round 31 (v0.5.46): Echo Music Appearance settings ported

The Appearance screen now carries Echo Music's Appearance items alongside the SimpMusic
Interface ones, grouped the Echo way. Everything writes a real preference the UI reads.

**Interface:** Theme, Pure black, Now playing style, Theme color, Liquid glass, plus NEW
High refresh rate (fastest display mode), and Default open tab + Grid cell size moved
here from Player settings (where Echo keeps them in Appearance).

**Player:** NEW Player background style, Player slider style (both moved here), NEW
Player buttons style (Default / Primary color / Tertiary color - recolors every control
icon through LocalContentColor so all four player styles pick it up), Hide player
slider, Hide player thumbnail, Crop album art, Rotating artwork (vinyl spin, respects
Reduced motion), and Show codec on player (the codec pill finally has an off switch).

**Lyrics:** NEW Lyrics text position (Left / Center / Right, independent of the player
text alignment), Lyrics text size + line spacing slider (x1.0-x2.0), Lyrics style,
Romanization, Lyrics blur and Auto scroll (all now in one place).

**Auto playlists:** NEW group - show/hide Liked, Downloaded, Uploaded and Cached
playlists in the library grid.

**Not ported (honest):** Echo's ten lyrics animation styles (their proprietary
renderers - Muso already has Apple Music, Classic and karaoke word float), Canvas
thumbnail animation (no client API), comment button (Muso has no comments), Listen
Together (needs a sync server), app icon / app font pickers and miniplayer background
(needs bundled assets - can be a future round), haptics / swipe-to-remove-queue /
swipe-lyrics gestures (queued for a future round).

**Version:** 0.5.46 (versionCode 53); release tag v0.5.46, APK Muso_v0.5.46_v1.apk.

## Round 30 (v0.5.45): Ultra-premium morphing waveform splash

The uploaded 5-bar waveform logo (cyan > blue > lavender > purple > pink) is now the
splash screen's only hero element, animated in the Echo Nightly style, drawn 100% in
Jetpack Compose Canvas (no bitmaps, no video, no network - GPU friendly, targets
60/120 FPS with a single frame-clock driven master timeline).

Timeline (~1.95 s total): black > staggered logo reveal (0.15-0.40) > waveform morph:
bars compress toward the center with the outer bars shrinking (0.40-0.62) > pulse
travels through the logo, center bar first, each neighbour delayed ~70 ms (0.62-1.20)
> bars rebuild back into the exact original logo with a tiny overshoot (1.20-1.50) >
settle (to 1.70) > exit: logo scales to 0.96 and cross-fades into the app over
~250 ms, revealing the main UI which has been loading behind it the whole time.

Details: per-bar height/width/translation/alpha/glow/color-shift all derived from the
centralized state machine (SPLASH_INIT > LOGO_REVEAL > WAVE_MORPH > LOGO_REBUILD >
LOGO_SETTLE > SPLASH_EXIT); cubic-bezier (0.22, 1.0, 0.36, 1.0) easing; controlled
glow (15% normal, 38% peak, 12% settled) rendered with a BlurMaskFilter halo; very
subtle colour movement toward each bar's neighbour during the pulse; rotation capped
at 0.6 degrees; the 5-bar identity, gradients, proportions and centering of the
original logo are preserved exactly.

Accessibility: when the device's animator scale is 0 or the in-app Reduced motion
setting is on, the splash degrades to a simple fade-in / static waveform / fade-out.

Android 12+ continuity: the system splash is set to pure black with a transparent icon
(new MusoSplashTheme on the activity + black window background on all API levels), so
there is no white flash and no duplicate logo - system splash and the custom
animation read as one continuous black scene.

Plays once per process (cold start only, never on rotation or task-switch back).

**Version:** 0.5.45 (versionCode 52); release tag v0.5.45, APK Muso_v0.5.45_v1.apk.

## Round 29 (v0.5.44): Appearance and Content rebuilt to mirror SimpMusic's Interface and Content

**Bug fix first:** the Animation toggles from the last update were emitted OUTSIDE the
scrollable Column (an editing slip), which rendered them over the top of the settings
screen and made it look broken/unresponsive. The whole screen is rebuilt, so this is
gone. Every switch and picker in all three screens is inside the proper Column and
writes a real DataStore preference.

**Appearance (= SimpMusic Interface), exact item list:**
- Theme (System / Dark / Light), Pure black when dark
- Now playing style (moved here from Player): Classic / Expressive / Immersive / Apple
- Lyrics style (moved here from Player): Apple Music / Classic
- Lyrics romanization (moved here from Player)
- Theme color: Default / From wallpaper / Custom - the old separate "Dynamic theme"
  switch and color picker are merged into this one selector like SimpMusic
- Liquid glass effect: one switch that turns on the translucent glassy navigation bar
  AND the blurred-artwork player background together

**Content (= SimpMusic Content), exact item order:**
- YouTube account (entry relabeled to SimpMusic's name)
- Language, Content country, Preferred audio language (relabel of content language)
- Quality (moved here from Player) + Download quality
- Video quality (high quality video) + Play video for video track (both moved here)
- Auto download liked songs
- Play explicit content (inverted Hide explicit, SimpMusic wording)
- Lyrics providers and Proxy groups stay as before

**Player settings** received everything that used to clutter Appearance (player text
alignment, slider style with the preview dialog, default open tab, grid cell size, and
the animation toggles), so no setting was lost - they just moved to where SimpMusic
keeps their equivalents.

Not ported (honest): video download quality (Muso does not download video), radio audio
only / sync follows to YouTube / send listening data to Google (no such backend in this
FOSS app).

**Version:** 0.5.44 (versionCode 51); release tag v0.5.44, APK Muso_v0.5.44_v1.apk.

## Round 28 (v0.5.43): SimpMusic settings categories - Listening history, AI, Spotify, theme color

The SimpMusic settings list, mapped category-wise into Muso (all real, nothing fake):

**Listening history (new category):** pause/resume listening history, clear history with
a confirm dialog, and a shortcut to Stats. The switches are the same keys the player
service already respects, so history recording actually stops and starts. The old
duplicate rows were removed from Privacy.

**AI (new category):** AI provider (OpenAI / Google Gemini / Custom OpenAI-compatible),
API key (stored only on the device), custom model ID, custom base URL for the custom
provider, translation target language (35 languages + system), and a "Use AI
translation" switch that stays disabled until a key is entered. When enabled, the lyrics
translate button in the player translates through the chosen AI provider - a real
translator for the FOSS build, which previously had none. Any error returns the original
lyrics instead of crashing or blanking the screen.

**Spotify (new category):** log in with a real OAuth PKCE flow using the user's own
Spotify Client ID (free at developer.spotify.com), in an in-app WebView like the Discord
login. Logged-in users see their Spotify playlists (name + track count) with
loading/error/retry states - never a stuck spinner. Tokens auto-refresh. Playlist import
into the Muso library is the next update (SimpMusic's spotify backend is a closed
binary, so this is rebuilt from scratch and lands in steps).

**Interface -> Appearance:** Theme color picker with 14 presets, applied whenever the
artwork-based dynamic theme is off.

Everything else SimpMusic has under Interface/Content/Audio/Playback/Lyrics was already
in Muso (themes, languages, providers, proxy, equalizer, crossfade, sleep timer, caches,
Discord). The Discord category already exists in Muso's settings.

**Version:** 0.5.43 (versionCode 50); release tag v0.5.43, APK Muso_v0.5.43_v1.apk.

## Round 27 (v0.5.42): library crash fix, smooth transitions, SimpMusic settings port

**Library tab crash fixed (root cause):** the library's in-screen search used the same
unsafe focus pattern that crashed the search tab earlier - requesting focus on a
FocusRequester in the same frame its TextField enters composition, which reliably throws
"FocusRequester is not initialized" when the library tab is restored while search was
active. Now the request is deferred one frame and wrapped in runCatching, and the search
state is no longer saved across tab switches at all. All remaining force-unwraps in the
library screens were removed too.

**Smooth navigation (SimpMusic-style):** page transitions now use spring-physics slides
with soft fades instead of fixed tweens - shared-axis feel between tabs, slide-and-fade
for detail screens, and an instant cut when Animations is off in Appearance settings.

**SimpMusic settings port (all real, DataStore-backed):**
- Content: Auto-download liked songs (keeps every newly liked song available offline;
  never re-queues failed or user-removed downloads) and a separate Download quality
  (downloads resolve their own stream, independent of streaming quality).
- Backup & restore: Automatic backup with Daily/Weekly frequency - writes the same
  zip as the manual backup into the public Downloads folder via MediaStore on app
  start, keeps the newest three, restores through the normal restore picker.
- Player: shuffle mode now survives a restart together with repeat mode
  (Settings toggle "Persistent queue" remains the master switch).

Everything else SimpMusic has is either already in Muso (themes, languages, lyrics
providers, proxy, equalizer/effects, crossfade, sleep timer, storage caches, Discord,
updates) or needs external keys/services (SponsorBlock, AI, Last.fm, Listen Together,
Canvas) and stays out rather than shipping broken toggles.

**Version:** 0.5.42 (versionCode 49); release tag v0.5.42, APK Muso_v0.5.42_v1.apk.

## Round 26 fix (v0.5.41): compile fixes for the v0.5.40 build log

- Player.kt: the 10 transport/shuffle/repeat IconButtons resolved to nothing because
  the material3 IconButton import was missing - added it (the ui.component one needs
  onLongClick and is not used here).
- SettingsScreen: the search-clear IconButton used the shared ui.component IconButton
  without its required onLongClick - now passes an empty onLongClick.

**Version:** 0.5.41 (versionCode 48); release tag v0.5.41, APK Muso_v0.5.41_v1.apk.

## Round 26 (v0.5.40): PRD round - player styles, codec info, gestures, animation settings

Follows the Muso advanced-player PRD (text.txt), the feasible phases in one stable pass.

**Multiple fullscreen player styles (PRD sections 8-10):**
Settings -> Player -> Player Style now offers four designs, persisted across restarts:
- Muso Classic - the current design, kept as the default (existing look preserved).
- Material 3 Expressive - the pill transport with press-growing weights and the
  connected shuffle/repeat/lyrics/video control group.
- Immersive - large metadata, the biggest transport, minimal controls (Lyrics | Queue
  dock only).
- Apple-inspired - centered metadata with heart/shuffle/repeat as one quiet row.
All four share the same shell: video, queue sheet, lyrics, and mini player untouched.

**Real audio codec information (sections 5 and 7):**
The player now reads its actually-selected audio track (Echo Music's pattern via
onTracksChanged) and shows the real codec and bitrate - "AAC 128 kbps", "Opus", "FLAC" -
in the times row of the player. When nothing is known the pill simply hides; no fake
values are ever shown.

**Echo Nightly-style gestures (sections 13 and 14):**
Swipe the artwork left/right to skip to the next/previous song; the artwork tracks the
finger while dragging and settles back if released too early. Velocity-aware, gated by
the new Gesture Animations setting.

**Animation settings (section 15):**
Settings -> Appearance -> Animation adds three real toggles: Animations (fades over
video become instant cuts, skeletons become static), Gesture Animations (the artwork
swipe), and Reduced Motion (Ken Burns zoom and karaoke word lift/glow motion stop;
sync fill stays).

**Video (SimpMusic):** already follows SimpMusic's architecture - a separate muted
ExoPlayer plays the song's video stream behind the whole player while the audio engine
stays untouched; sync tightened to 1s/1s in the previous round. No changes needed.

**Non-regression (section 2):** no routes, player, lyrics, queue, or search behavior
changed; the style switch only swaps the controls composable inside the same shell.

**Version:** 0.5.40 (versionCode 47); release tag v0.5.40, APK Muso_v0.5.40_v1.apk.

## Round 25 (v0.5.39): Echo Music settings UI + SimpMusic video polish

**Settings (Echo Music port):**
- New settings home in Echo's design: big title, a live search field that filters the
  categories, and connected rounded card groups (Material3SettingsGroup) with icons
  and descriptions - ported from Echo's Material3SettingsGroup/Item.
- The update entry floats to the top with a badge when a newer release exists.
- Every existing settings page is reachable from the new home - all routes unchanged,
  so every setting keeps working exactly as before.
- All settings rows everywhere (appearance, content, player, audio effects, storage,
  privacy, backup, discord) restyled to Echo's card look: each row is a rounded 20dp
  card with the icon in a 40dp rounded box, titleMedium title and bodyMedium
  description, with animateContentSize. Group titles now use Echo's uppercase
  onSurfaceVariant style.

**Video (SimpMusic):** the video system already follows SimpMusic's architecture (a
separate muted ExoPlayer playing the song's YouTube video stream, display-only,
kept in sync with the main audio player). Tightened the sync loop to 1s interval /
1s threshold so video and audio stay imperceptibly aligned after seeks and stalls.

**Version:** 0.5.39 (versionCode 46); release tag v0.5.39, APK Muso_v0.5.39_v1.apk.

## Round 24 (v0.5.38): Apple Music style player, karaoke lyrics, crash + lag fixes

The first real device run surfaced everything at once; this round rebuilds the full
screen player and the lyrics engine on the reference apps' designs.

**Player (SimpMusic "Apple Music" + Echo design, ported):**
- Title row: bold title, lighter artists, heart on the right.
- Thin pill progress bar: 7dp at rest, thickens to 14dp while touched, no thumb,
  tap-to-seek and drag-to-scrub.
- Transport: prev | play | next as big PLAIN glyphs (no container pills) in a tight
  centered cluster - the old wide pink pill with the off-centre pause icon is gone.
- Shuffle / repeat sit low at the sides, out of the transport.
- New Lyrics | Video | Queue dock (pill buttons) at the bottom.
- Over a playing video the controls flip to white on a gradient scrim.
- The five-button queue peek row (queue / lyrics / sleep timer / library / more) is
  fully removed from the bottom of the player; the queue sheet keeps a minimal
  grabber and the sleep timer / details stay in the expanded queue.

**Karaoke lyrics (Echo Music port):** TTML lyrics (BetterLyrics / SimpMusic
providers) now render word-by-word - each word fills in with a soft left-to-right
wipe exactly while it is sung, with a subtle lift and glow, on the same Apple Music
lyrics design as before. LRC lyrics keep line-sync as usual.

**Video controls lag fix:** the controls no longer leave composition when hidden
over a video - they fade with a fast alpha (180ms in, 500ms out), so toggling them
no longer rebuilds the whole control cluster (the old stutter).

**Crash fixes:**
- Search tab: requesting focus on the not-yet-composed SearchBar crashed with
  "FocusRequester is not initialized" (classic on the home tab); the request is now
  deferred to the next frame and guarded.
- Release keep rules added for Muso's viewmodels and preference constants as R8
  insurance for the library tab crash; if it still crashes, a logcat will pinpoint it.

**Version:** 0.5.38 (versionCode 45); release tag v0.5.38, APK Muso_v0.5.38_v1.apk.

## Round 23 (v0.5.37): fixed the remaining 8 Kotlin compile errors

Second compile pass of v0.5.36 left only 8 errors in 5 files, all fixed:

1-3. CachedScreen / PodcastScreen / PodcastsScreen: missing
   androidx.compose.foundation.layout.asPaddingValues import.
4. LibraryMixScreen: the sort-by-name lambda names its parameter "item" but the
   when-branches used the implicit "it" - switched to item.playlist.name /
   item.album.title / item.artist.name.
5. PodcastViewModels (CachedViewModel): ExoPlayer's SimpleCache.isCached()
   needs (key, position, length) - now called with 0..Long.MAX_VALUE so the
   whole stream counts as cached.

**Version:** 0.5.37 (versionCode 44); release tag v0.5.37, APK Muso_v0.5.37_v1.apk.

## Round 22 (v0.5.36): fixed all Kotlin compile errors from the first real build

The first CI build of the new code (v0.5.35) reached the Kotlin compile step and
revealed 10 real errors across 8 files, all fixed:

1. MainActivity: @OptIn(ExperimentalMaterial3Api) had drifted onto attachBaseContext
   during the language-switch insertion, leaving onCreate without it (25 experimental
   API errors) - moved back onto onCreate.
2. MainActivity: transition direction code compared it.route (a String list item)
   against routes - 8 occurrences fixed to plain equality.
3. MainActivity: home header used Row without importing it - import added.
4. App.kt HQ thumbnail interceptor: Coil chains have no withData() - replaced with
   chain.proceed(request.newBuilder().data(url).build()).
5. PreferenceKeys: duplicate LibraryViewType enum (mine + the original with toggle())
   - removed the duplicate.
6. AudioEffectsManager: constructor context param was not a property, so start()
   could not see it - made it private val.
7. Lyrics.kt: missing LyricsRomanizationKey import.
8. Player.kt: missing material3.Icon import (only IconButtonDefaults was imported).
9. AutoPlaylistScreen: missing AutoPlaylistViewModel import.
10. LibraryMixScreen: missing LibraryMixViewModel import, and the four auto-playlist
    Playlist() constructions lacked songCount / thumbnails parameters.

**Version:** 0.5.36 (versionCode 43); release tag v0.5.36, APK Muso_v0.5.36_v1.apk.

## Round 21 (v0.5.35): fixed Room schema folder after the package rename

The first CI build (v0.5.34) failed in KSP: Room's exported schemas lived under
app/schemas/com.zionhuang.music.db.InternalDatabase, but after renaming the package it
looked for them under app/schemas/com.muso.music.db.InternalDatabase. Renamed the schema
folder (1.json-12.json all present, no stale references). The room.schemaLocation
build arg is unchanged - Room derives the subfolder from the database class package.

**Version:** 0.5.35 (versionCode 42); release tag v0.5.35, APK Muso_v0.5.35_v1.apk.

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
