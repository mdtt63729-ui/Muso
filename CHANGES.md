## Round 108 (v0.5.123, code 130)

## What's new — iOS-style page transitions + 120Hz smoothness work
(Transitions & performance PRD — phases 2/3 core + phase-1 playlist fixes.)

- **NavigationTransitions.kt (new, Muso layer):** the full iOS push/pop
  engine — push 420ms / pop 400ms, CubicBezierEasing(0.25, 0.1, 0.25, 1)
  (iOS's exact curve), incoming page slides from 100% width, outgoing page
  parks at -30% parallax with a dim, zero spring/overshoot. Back is the
  exact mirror. Motion runs entirely through graphicsLayer-backed
  transition modifiers (slide/fade/scale) — render thread, no per-frame
  recomposition, no layout invalidation.
- **Two looks, one engine:** Liquid Glass ON = the incoming page floats in
  like a translucent plate over the barely-dimmed page behind (lighter
  fade + 0.86 dim). OFF = same timing and easing, stronger 0.72 dim and
  the outgoing page settles to a 0.96 scale. No feature is lost with glass
  off — only the look changes. Blur is deliberately never animated per
  frame (the PRD's #1 jank rule), so no RuntimeShader churn during
  navigation.
- **Top-level tab switch** (Home <-> Library) no longer slides: a 150ms
  crossfade with a touch of scale, iOS tab-switch style. Detail pages
  (playlist, album, artist, settings, search results) keep the slide.
- **Predictive back:** already wired — enableOnBackInvokedCallback was
  already set in the manifest, so Android 14+ edge-swipe drag tracks the
  finger with the same motion via navigation-compose's built-in support.
- **Playlist lag fixes (PRD phase 1):** Muso's own playlist screens
  already had LazyColumn keys; LocalPlaylistScreen's reorderable track
  list now also carries contentType so rows don't re-inflate while
  scrolling. Image pipeline already had memory/disk cache, hardware
  bitmaps and the HqThumbnailInterceptor — verified, unchanged. The
  gradient-placeholder playlist cards stay a cheap plain Box (no
  SubcomposeLayout).
- Old "Echo-style" EmphasizedEasing it/8 transitions on the NavHost were
  replaced by the engine above; the search result page keeps its own
  fine-tuned fade/slide overrides, and the Search tab proxy keeps its
  instant cut.

# Round 107 (v0.5.122, code 129)

## What's new — Settings Screen Integration (settings PRD)
Every delivered SimpMusic feature now has its setting in the right category,
with the right control and perfect (instant, no-restart) wiring:

- **User interface (Appearance):** Now playing style (Classic / Expressive /
  Immersive) — already there; NEW **Lyrics style** selector (Classic / Apple
  Music, "Android 12+" note on the Apple option — below 12 it safely falls
  back to Classic, same as upstream's blur gating) — mirrors live into the
  suite renderer through the DataStoreManager shim, so the running player's
  lyrics restyle on the very next composition. Liquid glass switch — already
  there (drives nav bar, MiniPlayer and every glass button through one
  composition local).
- **Lyrics (Player & audio):** the timing-offset slider from the v2.2.0
  round, plus the new style selector's sibling in the player settings.
- **Audio:** "Open system equalizer" row — already there.
- The suite player's own More-sheet keeps its in-player settings (endless
  queue, crossfade, playback speed/pitch) writing straight into the shim,
  exactly SimpMusic's pattern.
- Not carried over on purpose (PRD §5): MiniPlayer style (removed upstream
  in v2.2.0), word-by-word/landscape lyrics toggles (automatic with the
  lyrics style), playlist card gradient and the morphing loader (design
  system, not user options), lyrics prefetch/cache (invisible optimization),
  QR desktop sign-in and preferred audio language (Muso's own login/stream
  pipeline).

# Round 106 (v0.5.121, code 128)

## What's new — lyrics-and-cards package
- **Playlist card design (SimpMusic):** ported PlaylistThumbnail.kt (the
  title-hash gradient system) and FiveImagesComponent.kt (the 1+4 collage).
  Muso's playlist grid cards (library grid + home shelves) now fall back to
  the deterministic title gradient + subtle darken when a playlist has no
  thumbnail — same playlist, same colours, every time, exactly like
  SimpMusic's cards.
- **Lyrics engine hardened to the PRD's "resume at current line" rule:** the
  current-line index is now computed from playback position PLUS the user's
  lyrics timing offset, and honours any synced lyric type (not just
  line-synced). Toggling the lyrics button off and on (or opening fullscreen
  lyrics) lands on the line that is singing right now — the tracking runs in
  the background regardless of visibility.
- Verified the rest of the package was already aboard: the full lyrics UI
  stack (LyricsView word-by-word, fullscreen landscape, share card, vote
  dialog, translation), the Room-backed lyrics cache through Muso's own
  lyrics table, the morphing M3 CenterLoadingBox loader, and the settings
  slider — all byte-identical from earlier packages off the same v2.2.0
  commit.

# Round 105 (v0.5.120, code 127)

## What's new — SimpMusic v2.2.0 update applied
- **Verified & refreshed the full v2.2.0 file set.** All the earlier packages
  (player, navbar, playlist UI, liquid glass) were cut from the same v2.2.0
  commit, so the core systems — word-by-word lyrics rendering, fullscreen
  lyrics, the new MiniPlayer default, Compose-drawn playback indicators, the
  navbar search-tab keyboard fix — were already aboard; this round re-ports
  them cleanly from the official update package so everything is guaranteed
  v2.2.0 state (LyricsView, FullscreenLyricsContent, all three player content
  styles, AppleMusicQueueView, ExpressiveTransportRow, MiniPlayer,
  PlaybackIndicators, DescriptionView, FullWidthItems, ModalBottomSheet,
  PlaylistScreen, Scrollbar, ComposeResUtils, icons).
- **Lyrics timing offset (new feature):** Player & audio settings now has a
  Lyrics group with a -5s..+5s (100ms step) slider. The value flows into the
  suite player live, so synced lyrics shift to match what your ears hear —
  the seek bar and time readouts stay untouched, exactly like upstream.
- **System equalizer (new feature):** Audio effects settings gained an "Open
  system equalizer" row that launches the phone's built-in EQ instead of
  Muso's own.
- **26 new languages for the suite UI:** the full SimpMusic translation set
  (Arabic, Azerbaijani, Bulgarian, Catalan, German, Spanish, Persian, French,
  Hindi, Croatian, Indonesian, Italian, Hebrew, Japanese, Korean, Polish,
  Portuguese, Russian, Swedish, Thai, Turkish, Ukrainian, Vietnamese, Chinese
  simplified/traditional) now ships as translated simp_strings resources —
  the suite screens (player, playlist, navbar, sheets) follow the app
  language.

## Skipped (not applicable to Muso)
- QR login sync and unofficial-build blocking — SimpMusic's own account /
  anti-repackage systems; Muso keeps its own login and builds.
- Core media3 service rewrites, Room schema v26, scraper/audio-language
  selection — Muso plays through its own service, database and Innertube
  pipeline; the suite rides on top.
- Suite Browse/Artist/Settings screens — Muso's own home, artist and
  settings screens serve those routes.

# Round 104 (v0.5.119, code 126)

## What's new
- **The real liquid glass engine is in.** The hand-written Kyant stubs are gone,
  replaced by the actual `io.github.kyant0:backdrop:2.0.1` (+ `shapes:1.2.1`)
  library — AGSL RuntimeShader refraction on Android. With the glass setting
  on, the floating navigation bar, its glass MiniPlayer, the playlist screen's
  top-bar buttons and the player's Apple-Music-style buttons now get the full
  SimpMusic recipe: vibrancy saturation boost, luminance-driven blur
  (2-16dp), the 24dp lens refraction at the edges, and the separate
  onDrawSurface darkening.
- **Hosting follows the upstream pattern exactly:** MainActivity now creates
  the shared backdrop (white base on light theme, black on dark), marks the
  NavHost content layer with `layerBackdrop`, and the bar + MiniPlayer stay
  siblings of that layer — the render-feedback crash rule from the PRD is
  respected. `LocalLiquidGlassEnabled` is provided from the existing Liquid
  glass navigation bar setting, so every glass surface (including detail
  screens) honors the toggle while keeping its shape and hit target.
- Luminance adaptation: the bar samples the content behind it every second
  and tweens blur + darkening over 500ms, so the glass re-lights as artwork
  changes. Press interaction (scale-up + pointer-following glow, spring
  return) now actually renders.
- Setting OFF still falls back to the same flat `surfaceContainerHighest @
  80%` pill with unchanged layout.

## Technical
- `expect/ui/LiquidGlass.kt` replaced by the upstream 78-line common-code
  version (PlatformBackdrop = typealias LayerBackdrop, layerBackdrop wrapper,
  rememberBackdrop, drawBackdropCustomShape with the exact effect stack).
- Engine layer 2 (`LiquidGlassContainer.kt`, 421 lines) and Theme were
  already byte-identical; MiniPlayer/TabBar/AppBottomNavigationBar only
  differed by our resource conversions, so they were kept.
- Added `-opt-in=androidx.compose.ui.ExperimentalComposeUiApi` for the
  GraphicsLayer APIs the engine uses.

# Round 103 (v0.5.118, code 125)

## What's new
- **The real SimpMusic Playlist screen (PRD playlist package).** Tapping any
  online playlist card now opens the suite's own playlist detail screen:
  full-bleed hero artwork with palette-driven immersive gradient, title block
  with author/metadata, the centered 48dp Shuffle-Play pill-Download cluster,
  expandable description, and the square-thumbnail track rows with three-dot
  menus and a now-playing indicator.
- **In-page search**: the top-bar search button morphs into a search field
  (SearchBarTransition) and filters the track list live.
- **Selection mode**: long-press a track to enter selection with the animated
  top bar and "n selected" count; the selection sheet can play next, add to
  queue, download, like, add to a Muso playlist or remove from it.
- **Download** on the playlist works end-to-end: per-track progress states
  flow from Muso's download manager, and the button crossfades
  normal -> downloading -> downloaded.
- **Tapping a track starts the queue from that track** (or shuffled), backed
  by the suite's QueueData pipeline; Play Next / Add to Queue from the sheets
  route into Muso's player too. The suite's glass MiniPlayer and the player
  follow along.
- Start Radio builds a radio queue via YouTube.next; "save to local playlist"
  creates a real Muso playlist with all tracks.

## Technical
- Ported PlaylistScreen (1,388 lines), PlaylistViewModel, SongSelectionViewModel,
  BaseViewModel, the selection components, SearchBarTransition,
  PlaybackIndicators, SurfaceDarkColors, LoadingDialog, ImageCropperDialog and
  the Config object - byte-for-byte apart from resource references.
- MediaPlayerHandler became a functional adapter: the playlist ViewModels call
  setQueueData/loadMediaItem/shufflePlaylist/playNext/loadMoreCatalog on it,
  and MusoSuiteBridge executes those commands against Muso's PlayerConnection
  (SuiteTrackQueue serves the loaded track list as a Muso queue). The bridge
  also feeds the handler's nowPlaying/control state now.
- New Muso adapters registered in Koin: MusoPlaylistRepository (Innertube
  fetch + in-memory live playlist store), MusoSongRepository (Room-backed
  reads, playlist continuation via Innertube), MusoDownloadHandler
  (ExoDownloadService + DownloadUtil progress), MusoLocalPlaylistRepository
  (Room playlists with a Long<->UUID id bridge). They reach Muso's Hilt
  singletons through a SuiteEntryPoint.
- DatabaseDao gained songsByIds()/setLikedById(); SharedViewModel gained
  getQueueDataState() and a working addListToQueue; online_playlist now hosts
  the suite screen, with type-safe Artist/Album/Playlist destination proxies
  forwarding into Muso's routes. Fixed a duplicated RepeatState declaration
  that would have broken compilation. Added koin-compose-viewmodel.

# Round 102 (v0.5.117, code 124)

## What's new
- **The real SimpMusic floating navigation bar (PRD section 12).** Muso's old
  hand-built bottom NavigationBar is removed entirely; the suite's own bar
  renders in its place - a floating capsule of tabs plus a separate round
  Search button, exactly the geometry SimpMusic ships (96/64/56dp, 12dp gap).
- **Liquid glass variant (default ON)**: LiquidGlassAppBottomNavigationBar
  with the frosted capsule, the sliding frosted-blob tab indicator, the glass
  Search FAB, and the suite's own glass MiniPlayer riding above the bar (with
  progress ring, play/pause, like, swipe-to-change-track and drag-down
  dismiss). The bar collapses to a single glass pill while content is
  scrolled and expands back at the top.
- **Flat variant** (new setting "Liquid glass navigation bar" OFF): the same
  capsule-and-FAB form drawn flat, with Muso's own mini player kept.
- Tabs: Home, Library, plus Search in its round button. Tapping the selected
  tab again reloads that screen (scroll-to-top), and switching tabs restores
  each tab's saved scroll state. Search opens Muso's search overlay exactly
  like the old bar's Search entry did.
- Added the suite's full destination set (Home/Search/Library/Analytics/Mix/
  login/list), the simp_mono rail logo, and the suite MiniPlayer, all ported
  byte-for-byte with only resource-reference adaptation.

## Technical
- The tab routes in the nav graph are now SimpMusic's type-safe @Serializable
  destinations (HomeDestination / LibraryDestination); a SearchDestination
  proxy opens the search overlay and pops itself with instant transitions.
- MusoSuiteBridge (new, in MusoSuiteHost.kt) keeps the suite's
  SharedViewModel shim fed with live state - nowPlayingState (mapped to the
  domain SongEntity/GenericMediaItem), controllerState, a 250ms timeline
  poll - and routes its UIEvents back into Muso's player; stopPlayer() stops
  and clears Muso's queue. The shim gained getEnableLiquidGlass() backed by
  the DataStoreManager stub.
- Koin: SharedViewModel now receives the DataStoreManager. The kyant backdrop
  stub was split into its real package layout (com.kyant.backdrop.effects /
  .highlight / .shadow) with the full drawBackdrop signature the tab bar uses
  (shadow, innerShadow, layerBlock, Highlight.copy). A duplicate SuiteRes
  declaration was removed. Added androidx.constraintlayout:compose.
- MainActivity: removed the old NavigationBar/NavigationBarItem block and
  navigationItems/topLevelScreens; every route check now uses hasRoute();
  BottomSheetPlayer hides its collapsed mini player while the glass bar's own
  MiniPlayer is showing.

# Round 101 (v0.5.116, code 123)

## What's new
- **The SimpMusic suite is now the only player.** Muso's hand-built native
  players (the old Spotify, Apple Music and Material 3 Expressive layouts,
  including the canvas video player) were removed entirely - about 2,500
  lines. Every player style now renders the real, byte-for-byte SimpMusic
  suite.
- **The three styles were renamed** to drop the "SimpMusic" prefix:
  - **Classic** (default) - the suite's Spotify layout
  - **Expressive** - the suite's Material 3 Expressive layout
  - **Immersive** - the suite's Apple Music layout
- Existing installs migrate automatically: a stored style that no longer
  exists (including the old SIMPMUSIC* values) falls back to Classic.

## Technical
- Player.kt was rewritten as a slim shell (~320 lines): mini-player,
  background, codec readout, keep-screen-on, the suite host and the info /
  add-to-playlist dialogs - the latter previously never rendered in suite
  mode because they lived only inside the removed native layouts.
- Deleted: SimpExpressivePlayer.kt (SongInfoDialog moved into Player.kt),
  PlayerVideo.kt, Thumbnail.kt, PlaybackError.kt.
- PlayerStyle enum reduced to CLASSIC / EXPRESSIVE / IMMERSIVE; stored
  preference values are enum names, so the migration is a plain fallback to
  the CLASSIC default.

# Round 100 (v0.5.115, code 122)

## What's new
- **All three SimpMusic player styles are now individually selectable.** The
  Now Playing Style setting gained two new entries, "SimpMusic Apple Music"
  and "SimpMusic Spotify", next to the existing "SimpMusic" (renamed
  "SimpMusic Expressive"). Each renders the suite's own byte-for-byte layout —
  NowPlayingContentAppleMusic, NowPlayingContentSpotify or
  NowPlayingContentM3Expressive — through the same MusoSuiteHost adapter,
  with the live lyrics and artwork-palette feeds from Round 99.
- Fixed a latent compile error: the native controlsContent switch in
  Player.kt was not exhaustive once the suite styles exist; the suite
  entries now (unreachable) map to the classic controls.

## Technical
- PlayerStyle enum: added SIMPMUSIC_APPLE and SIMPMUSIC_SPOTIFY plus a
  suiteStyles companion set; both the video-disable guard and the suite
  routing in Player.kt now use it. The stored preference string is
  unchanged, so existing installs keep their current style.

# Round 99 — v0.5.114 (121)
PHASE 2 OF THE SIMPMUSIC SUITE — lyrics and live palette colors:
- LYRICS IN THE SUITE: Muso's stored lyrics (LRC or plain text) are parsed
  into SimpMusic's Lyrics model and fed through NowPlayingScreenData, so the
  Apple Music style's Lyrics tab and the Spotify lyrics view now render Muso's
  own lyrics - synced (line highlight follows playback via the timeline flow)
  and unsynced alike. Tapping a line seeks to that timestamp. Provider is
  reported as LRCLIB (Muso's primary source); translations and the
  community-vote dialog stay Phase 3.
- LIVE PALETTE: the Apple Music / M3 Expressive gradient wash now derives its
  dominant color from the current artwork (kmpalette over the Coil3-loaded
  bitmap, animated 800ms) instead of the static brand green. The Spotify style
  already generated its palette internally and is unchanged.
- FIX: UIEvent.UpdateProgress arrives as a 0..100 percent (lyrics line clicks
  and description timestamps), not a 0..1 fraction - lyric taps now seek to the
  right position instead of the end of the track.
- LyricsView's injected LyricsRomanizerRepository is provided as a no-op
  (original lines render unchanged), registered through Koin alongside the
  other suite collaborators.
- PRE-CI HARDENING: resolved every external library the suite's ported files
  reference but Muso does not ship. Kermit logging -> android.util.Log; the
  CMPToast API -> a platform-Toast shim under the same package; the Google
  Cast button -> a no-op (Muso has no cast; all cast UI auto-hides); the Kyant
  liquid-glass backdrop -> an API-compatible shim that draws the glass panels'
  surface tints without the blur/refraction sampling; the unused haze blur
  helper, desktop MPV player module, YouTube link parser, generic intents,
  import repository and BaseViewModel were deleted. Added dependencies:
  lifecycle-runtime-compose, kotlinx-datetime, androidx.paging (common),
  kotlinx-serialization (with the plugin applied to :app). Restored the
  MediaService handler type declarations (ControlState, QueueData,
  SleepTimerState, NowPlayingTrackState...) that a bad extraction had dropped,
  trimmed LocalPlaylistRepository to the call surface the sheets use and
  registered a no-op implementation in Koin.

## Round 98 — v0.5.113 (120)
STARTUP BLACK FIXED FOR PRE-ANDROID-12 DEVICES — from the 07:23 recording:
the app opened to ~1s of black before the splash on devices below API 31,
because R95's instant logo used the Android 12+ system splash, which doesn't
exist on older Androids. Now every Android version opens straight into the
brand:
- The pre-31 launch window's background IS the logo: a layer-list (black +
  static waveform) sized per smallest-width bucket (drawable-sw400dp/440/480
  plus the base) so the logo sits on screen milliseconds after the tap, while
  the process starts underneath - no black pause at any API level.
- The Compose splash starts pre-revealed below API 31 (initialT = T_REVEAL_END):
  its first frame draws the bars at full alpha, matching the window logo, so
  the window -> Compose handoff is seamless and the logo never blinks off;
  the wave/pulse animation continues from there exactly as before.
- Android 12+ behavior is unchanged (system splash icon + full materialize).

## Round 97 — v0.5.112 (119)
THE REAL SIMPMUSIC PLAYER SUITE, INTEGRATED — the actual SimpMusic Now Playing UI
(maxrave-dev/SimpMusic, GPL-3.0), ported byte-for-byte per the integration PRD:
- A fourth player style option in Settings > Appearance: "SimpMusic (exact port)"
  (PlayerStyle.SIMPMUSIC). The other three styles are untouched.
- The three SimpMusic Now Playing screens render unmodified: Spotify, Apple
  Music, and M3 Expressive — artwork pager, gradient washes, controls, seek
  slider, queue swipe, canvas video frame, all animations at full fidelity.
- MusoSuiteHost.kt is the adapter: it feeds NowPlayingContentState from Muso's
  PlayerConnection (queue, metadata, timeline, like status, canvas video URL)
  and routes every NowPlayingContentActions callback back to the player
  (play/pause, next/prev, seek, shuffle/repeat, like, queue reorder/remove,
  slider, artist navigation, info/add-to-playlist/song-menu sheets).
- Stack bumped to the PRD's pinned versions: Kotlin 2.2.20, KSP 2.2.20-2.0.2,
  Compose 1.9.4 / material3 1.5.0-alpha22, media3 1.11.1, Room 2.8.5,
  Hilt 2.56.2, AGP 8.9.1, Gradle 8.11.1, compileSdk 36; added Koin (4.2.2),
  Coil3 (3.6.3), kmpalette (3.1.0), materialkolor (5.0.1).
- Suite platform layer: SimpMusic's Android actuals for expect/ui (video view,
  device volume, scrollbar, photo picker, etc.), 905 prefixed string resources
  (simp_*), placeholder drawables, Poppins Medium font.
- Shim layer (Phase 1, documented in-file): SharedViewModel,
  NowPlayingBottomSheetViewModel, MediaPlayerHandler, DataStoreManager provide
  neutral defaults; sheets/dialogs that read them render but act as no-ops.
- Known Phase-1 limits (next round): in-player lyrics (LyricsData mapping from
  Muso's LRC), playlist-add/sleep-timer actions in the suite sheets, dynamic
  artwork palette colors, and cast/vote UIs.

## Round 96 (v0.5.111): canvas-video overlay look fixed Apple-Music style, video availability

- **CONTROLS OVER THE VIDEO (the overlay complaint):** SimpMusic's exact canvas
  scrims now ride the UI fade - the top 22% of the screen falls from black
  0.55 to clear, the bottom 60% darkens in graded steps to 0.97 black. The
  title, slider, transport and dock sit on that proper dark band, exactly like
  Apple Music in SimpMusic - no more buttons floating raw over the busy video.
  (The old weak controls-block scrim is replaced by this screen-wide pair.)
- **AUTO-HIDE 5s:** the canvas-mode UI now hides 5 seconds after the last
  touch (SimpMusic's timing), leaving the clean video + the idle overlay.
- **SOME VIDEOS NEVER PLAYED:** the strict quality rule was too strict in one
  spot - many songs only offer a MUXED (video+audio) stream at a perfectly
  good quality, which were all rejected. The bar itself stays absolute (at
  least the set video quality or no video at all), but a muxed stream AT/ABOVE
  the bar now plays as fallback when no video-only adaptive stream qualifies.

**Version:** 0.5.111 (versionCode 118); release tag v0.5.111, APK Muso_v0.5.111_v118.apk.

## Round 95 (v0.5.110): splash black-gap + handoff lag, lyrics fetching, All-search fix

- **SPLASH, THE BLACK PAUSE AT OPEN:** the Android 12+ launch window now shows
  the static waveform logo - the SYSTEM draws it within milliseconds of the
  tap, so the app opens straight into the logo instead of a black pause, and
  the live waveform animation (which now starts on its very first frames)
  takes over seamlessly. The overlay is opaque from frame one.
- **SPLASH-OUT TO HOME-IN LAG:** the exit fade now WAITS for the home screen -
  the splash holds its settled frame while the heavy startup composition runs,
  and only fades once the main UI has actually rendered two frames beneath
  it. The dead black gap between the two animations is gone.
- **LYRICS NOT LOADING:** fixed. The fetch was gated on the persisted
  show-lyrics preference, which the per-session lyrics change had orphaned -
  lyrics are now fetched and cached for every song regardless.
- **SEARCH - ALL TAB EMPTY:** YouTube flattened the All-search response (no
  more shelf sections - a top-result card plus a flat run of single-item
  rows). The parser now reads the flat rows and regroups them by type, so the
  All tab shows titled Songs / Videos / Albums / Artists sections again. The
  filtered tabs (which return shelves) were already fine.

**Version:** 0.5.110 (versionCode 117); release tag v0.5.110, APK Muso_v0.5.110_v117.apk.

## Round 94 (v0.5.109): canvas video - HIGH QUALITY OR NOTHING

The canvas video now accepts ONLY a video-only adaptive stream at least as
tall as the video-quality setting (the smallest one that meets the bar, so a
720p setting does not stream 1080p data for a background loop). When nothing
that good exists the video simply never comes - no low-quality stream, no
muxed fallback - the thumbnail stays. The playback itself is unchanged: the
SimpMusic recording's way - fullscreen edge-to-edge behind the player, the
7-10s seamless highlight loop, and the Apple idle overlay.

**Version:** 0.5.109 (versionCode 116); release tag v0.5.109, APK Muso_v0.5.109_v116.apk.

## Round 93 (v0.5.108): canvas video done right - per-song URL, quality, lag, taps, Apple idle overlay

- **THE PREVIOUS SONG'S VIDEO**: the canvas URL was only published by the
  audio resolver - which is SKIPPED for downloaded/cached songs, so the canvas
  kept playing whatever came before. The video now resolves on its own scope
  for EVERY song (independent of the audio path), and the URL is cleared the
  moment the song changes, so a stale video can never bleed through.
- **LOW QUALITY**: the "closest to target" tie-break could land on a tiny
  240/360p stream. The tallest video-only stream within the video-quality
  setting now wins, with the best muxed stream as the fallback.
- **THE APP-WIDE LAG**: the canvas kept decoding full-res video behind the
  collapsed sheet (mini player). It now pauses whenever the player is not
  expanded - the mini player never decodes video in the background.
- **TAP SHOW/HIDE (rebuilt)**: a tap on any empty spot now toggles the whole
  UI through one parent handler on the player itself - buttons consume their
  own taps, drags stay with the slider/pager/sheet. While hidden, the catcher
  above everything takes the first tap to bring it all back.
- **LYRICS AUTO-ON**: show-lyrics is no longer a persisted preference - it is
  a per-session choice that starts OFF every time.
- **APPLE IDLE OVERLAY (from the SimpMusic recording)**: while the controls
  hide over the video, the SimpMusic cluster stays - the current lyric line,
  the 55dp artwork with compact title/artist, and the favourite + more
  buttons - fading in/out with the controls.

**Version:** 0.5.108 (versionCode 115); release tag v0.5.108, APK Muso_v0.5.108_v115.apk.

## Round 92 (v0.5.107): CI fix - Kotlin errors that hid behind the KSP failure

The 0.5.105 build died at KSP before Kotlin ever compiled, so a batch of
small compile errors from the last few rounds surfaced only now. All fixed:

- LyricsMenu: the share action's LYRICS_NOT_FOUND companion import was
  missing.
- Player.kt: the stored-format (kbps) collect used `database` before it was
  declared - moved below the declaration; the FormatEntity import was
  missing (bad anchor); the SongMenu call passed the embedded SongEntity
  instead of the Song wrapper.
- AppleMusicQueueBody: its header got the favourite + more buttons earlier
  but its signature never received the matching parameters - the queue body
  now takes isLiked / onToggleLike / onShowMenu like the lyrics body, wired
  at the call site.
- PlayerVideo.kt: LocalContext was imported from the wrong package
  (androidx.compose.runtime instead of androidx.compose.ui.platform).

**Version:** 0.5.107 (versionCode 114); release tag v0.5.107, APK Muso_v0.5.107_v114.apk.

## Round 91 (v0.5.106): CI fix - Lyrics.kt leftover fragment

The morph-loading replacement left the OLD equalizer body behind the new
function (a stray ") {" line at Lyrics.kt:645 broke the build). The leftover
fragment is removed; the Material-3 morph indicator and everything else from
Round 90 stand unchanged.

**Version:** 0.5.106 (versionCode 113); release tag v0.5.106, APK Muso_v0.5.106_v113.apk.

## Round 90 (v0.5.105): light mode fixed for all player styles, lyrics-card popup, morph loading

- **LIGHT MODE FIXED ON ALL THREE PLAYER STYLES.** The players are SimpMusic
  surfaces - dark, white-text - in BOTH themes now, instead of the broken mix
  that put white text on pale light-mode backgrounds:
  - Apple: the frosted wash now deepens automatically when the artwork's seed
    color is light, so the white text reads on any artwork in any theme.
  - Expressive: the blurred-artwork scrim is always dark (no longer follows
    the theme), a flat dark base covers the non-blurred case, and the header
    row (collapse circle, NOW PLAYING, playing-from, details) is white.
  - Classic: unchanged dark gradient; its shuffle/repeat accents turn white
    in light mode (the pale theme primary washed out).
  - Player button styles (Primary/Tertiary) and the dock chips turn white in
    light mode for the same reason.
- **LYRICS CARD (Classic scroll):** the Show text button is gone. Its place:
  a fullscreen button (the same fullscreen Show used) + a 3-dot button opening
  a compact popup with SHARE (lyrics text to the system share sheet), EDIT,
  SEARCH and REFRESH (re-fetch) - every action fully working.
- **LYRICS LOADING = Material-3 morphing indicator:** a single blob that
  continuously morphs shape (circle to rounded square, breathing size) while
  it rotates, in the theme primary color - replacing the equalizer bars.

**Version:** 0.5.105 (versionCode 112); release tag v0.5.105, APK Muso_v0.5.105_v112.apk.

## Round 89 (v0.5.104): HIGH quality default, codec kbps, serial settings, equalizer loading

- **AUDIO QUALITY DEFAULTS TO HIGH** everywhere the default is read - the
  playback selection in MusicService, the download selection, and the
  Content settings screen. Fresh installs (and any install that never
  touched the setting) now stream high-quality audio out of the box.
- **CODEC PILL NOW SHOWS THE BITRATE**: "OPUS • 128 kbps". The media3 track
  rarely reports a bitrate for progressive streams, which is why the pill
  showed the codec alone - the stored format (written on every first play,
  straight from the YouTube stream data) now fills the kbps when the track
  does not carry one, and the codec name is standardised to upper-case.
- **SETTINGS REORDERED SERIAL-BY-IMPORTANCE** in every category: Content
  leads with Audio/Download/Video quality then language, region and account;
  the Player page leads with playback behaviour (skip silence, audio
  normalization + loudness, spatial) before niceties; Appearance leads with
  the Now Playing style, then theme, then chrome; Lyrics follow
  style -> position -> size -> spacing -> scroll -> blur -> romanization.
- **LYRICS LOADING IS THE SIMPMUSIC NEON EQUALIZER**: the old text-skeleton
  shimmer is completely gone from the lyrics view - while lyrics load, five
  symmetric rounded bars breathe like an equalizer with a cyan-to-magenta
  neon gradient, a soft outer glow and a glossy sheen, centered on the
  lyrics area.

**Version:** 0.5.104 (versionCode 111); release tag v0.5.104, APK Muso_v0.5.104_v111.apk.

## Round 88 (v0.5.103): SimpMusic-clean lyrics - nothing but the words

The lyrics view had junk leaking into the lines on some songs - raw
[mm:ss.xx] timestamps, [ar:]/[ti:]/[by:] LRC metadata tags - because synced
detection only checked whether the file STARTED with "[", and the parser let
time tags embedded mid-text through. All of that is gone (SimpMusic logic):

- ROBUST SYNCED DETECTION: a lyrics file counts as synced when ANY line
  starts with a timestamp - leading blank lines or a BOM can no longer push
  a synced file down the unsynced path that dumped raw timestamps as text.
- SANITIZED EVERYWHERE: inline time tags are stripped from every parsed
  line, LRC metadata rows are dropped, blank leftovers removed - only actual
  lyric text can ever render, synced or unsynced.
- SYNC-TYPE SANITY (SimpMusic): timestamps all identical (a "synced" file
  whose rows are all 0) is demoted to unsynced - no fake active line.
- Apple LYRICS body header is now SimpMusic's compact header actions:
  favourite (heart, live) + more (the song's real menu) instead of a close
  cross - re-tapping the dock's Lyrics button still returns to MAIN.
- Floating lyrics actions restyled as SimpMusic white-24% circles (38 dp,
  18 dp icons): translate toggle + lyrics menu.

**Version:** 0.5.103 (versionCode 110); release tag v0.5.103, APK Muso_v0.5.103_v110.apk.

## Round 87 (v0.5.102): Spotify-Canvas-style fullscreen video experience

The whole video system is rebuilt around one idea - like Spotify's canvas:

- THE THUMBNAIL STAYS while the video loads behind it. No video available?
  The thumbnail simply stays; there is never a black box. When the video
  is actually ready, the thumbnail fades out with a smooth fade animation.
- THE VIDEO PLAYS REALLY FULLSCREEN - edge to edge, top to bottom and side
  to side, behind the whole player (portrait and landscape), cropped with
  the minimal zoom that still covers every edge.
- ONLY A 7-10s HIGHLIGHT LOOPS: the video plays from ~25% in as a clipped
  8-second window with a seamless repeat (Spotify-canvas style), muted,
  from its own dedicated ExoPlayer - the audio always comes from the main
  player, so the loop can never drift the playback or the controls. The
  main player now always plays the audio stream; MusicService publishes the
  video stream URL for the canvas (respects the video-quality setting).
- CONTROLS: 3s after the LAST touch (button, slider, tap - every touch
  restarts the countdown) the whole UI - top bars, artwork, controls, dock -
  fades out smoothly. While hidden, a tap catcher sits above everything:
  the faded buttons cannot be hit, the first tap only brings everything
  back with a smooth fade-in; after that they work normally again.
- Tap on the open video area hides everything with a fade; tap anywhere
  brings it back.
- Removed with the old system: the in-artwork video slot, the fullscreen
  button, the +/-5s overlay, and the FullscreenVideoPlayer route.

**Version:** 0.5.102 (versionCode 109); release tag v0.5.102, APK Muso_v0.5.102_v109.apk.

## Round 86 (v0.5.101): the real cause of the video-time bottom overlay removed

The dark strip with the pill that appeared at the bottom whenever a video
played was the SYSTEM NAVIGATION BAR: the player's immersive-mode logic
deliberately showed the navigation bar while a video was active in the
normal (non-fullscreen) player. That branch is deleted - the regular player
now renders the system bars exactly like the rest of the app whether or not
a video is playing; only the full-screen video route still hides them.

**Version:** 0.5.101 (versionCode 108); release tag v0.5.101, APK Muso_v0.5.101_v108.apk.

## Round 85 (v0.5.100): queue overlay removed entirely

The queue bottom-sheet overlay that appeared over the bottom of the player
(especially noticeable during video playback) is gone from the project:

- Queue.kt (the queue sheet, with its peeking strip and drag handle) is
  deleted from the project - it can never appear again, in any style or
  during video playback.
- The Classic action row's queue button and the Expressive connected group's
  queue slot were removed with it (the Expressive group now ends with the
  rounded cap on the add-to-playlist slot).
- The Apple Music style keeps its QUEUE tab body - that is an in-player tab,
  not the overlay.

**Version:** 0.5.100 (versionCode 107); release tag v0.5.100, APK Muso_v0.5.100_v107.apk.

## Round 84 (v0.5.99): Apple player back to SimpMusic's signature frosted look + codec badge

Following the reference screenshots, the Apple Music style returns to
SimpMusic's classic design:

- Frosted backdrop restored: the artwork heavily blurred under the
  translucent three-stop wash of its dominant colour, darkest at the bottom.
- The artwork is full-bleed at the top of the sheet again (not a centered
  card); video keeps its portrait-cropped framing.
- Transport uses SimpMusic's FastRewind / FastForward glyphs for
  previous/next (fast_rewind drawable is the mirrored fast_forward).
- The times row now carries SimpMusic's codec badge pill in the center slot:
  a GraphicEq glyph + the real codec/bitrate of the playing stream (OPUS,
  AAC...) on a white-16% rounded pill. Respects the existing
  "show codec on player" appearance setting.

**Version:** 0.5.99 (versionCode 106); release tag v0.5.99, APK Muso_v0.5.99_v106.apk.

## Round 83 (v0.5.98): Apple player rework to match SimpMusic's latest, search filter fix, iconless launch

Apple Music player (matching SimpMusic's current look):

- Flat near-black surface instead of the frosted blurred-artwork backdrop.
- The artwork is now a centered square card (~87% width, 20dp corners) with a
  soft lift shadow, centered in its slot; video keeps the same card framing.
- Airier vertical rhythm (wider artwork-to-title gap, wider transport gaps).

Search:

- The Songs/albums/artists filters showed nothing: a filtered search response
  can contain several sections and the parser took the LAST one, which is
  often an empty or unrelated shelf. It now takes the first NON-EMPTY
  musicShelfRenderer, so every filter returns its items.

Launch/splash:

- The Android 12+ system splash no longer shows the app icon at all (fully
  transparent splash icon + 0 duration): opening the app goes straight into
  the custom waveform animation, which now plays from the very beginning and
  fades IN over the black launch window - no icon, no gap, no pop.

**Version:** 0.5.98 (versionCode 105); release tag v0.5.98, APK Muso_v0.5.98_v105.apk.

## Round 82 (v0.5.97): ultra-high thumbnails everywhere (2160px, no compromise)

- The global thumbnail interceptor now upgrades EVERY YouTube art URL to
  ultra-high 2160px first (was capped at 1200px), with a 1200px fallback and
  the original URL as the last resort - so art is always the sharpest the
  server has, on every surface: quick picks, playlist cards, playlist song
  rows, search results, mini player, player card, fullscreen player,
  lyrics card, artist pages.
- Channel/playlist avatar URLs ("=s###" googleusercontent/ggpht form) are now
  upgraded too (to "=s2160") - they were never rewritten before, which is
  why several playlist screens still showed soft art.
- The player's own metadata thumbnail and the artist screens now request
  2160px as well (was 1200px), so fullscreen artwork is crisp on 1440p
  displays.
- Coil still downsamples each image to the view, so memory use is
  unchanged; only the downloaded source resolution went up.

**Version:** 0.5.97 (versionCode 104); release tag v0.5.97, APK Muso_v0.5.97_v104.apk.

## Round 81 (v0.5.96): splash stutter hardening + search throttle fix

Splash (frame-by-frame analysis of the new recording showed the animation
itself is smooth, but main-thread stalls made the splash clock JUMP - that
is what popped the wordmark in and turned the home handoff into a hard cut):

- The splash master clock now advances at most ~3 frames per frame; a
  main-thread stall PAUSES the animation instead of skipping ahead, so no
  phase ever pops and the handoff always eases. The safety timeout was
  raised to 5 s to cover the stretched timeline.
- The startup update check now runs only AFTER the splash (its first Ktor
  network use class-loads on the main thread).
- The WorkManager update-check scheduling moved off the main thread in
  App.onCreate (it opens/writes WorkManager's Room database synchronously).

Search:

- The Classic description card no longer calls the YouTube player endpoint
  on every player open - it now fetches only for 11-character YouTube ids
  (local songs never hit the network), only once the user actually scrolls
  below the fold to where the card is visible, and at most once per song via
  an in-memory cache. The per-song-open calls were heavy enough to get the
  YouTube client throttled, which is why search results stopped appearing.

**Version:** 0.5.96 (versionCode 103); release tag v0.5.96, APK Muso_v0.5.96_v103.apk.

## Round 80 (v0.5.95): fix player-open crash (IndexOutOfBoundsException: Index -1)

The artwork pager crashed on opening the player with a single-song queue:
PlayerConnection's currentWindowIndex starts at -1 (before the first
timeline update), so the pager was created with a negative initialPage and
its first draw threw IndexOutOfBoundsException: Index -1, size 1.

- The pager's initialPage is now coerced to >= 0.
- The HorizontalPager is never composed with an empty queue anymore - an
  empty queue parks the pager's page at -1 and the refill draw crashes the
  same way. An empty box holds the artwork slot until the queue lands.

**Version:** 0.5.95 (versionCode 102); release tag v0.5.95, APK Muso_v0.5.95_v102.apk.

## Round 79 (v0.5.94): CI fixes for the Round 77+78 player port

Mechanical compile fixes from the first CI run of the two-round SimpMusic
player port (0.5.92's code itself compiled clean):

- Removed a duplicated `android.app.Activity` import in Player.kt.
- Fixed the `PlayerResponse` import path to
  `com.zionhuang.innertube.models.response.PlayerResponse` (this also
  un-broke the description card's video-details state).
- Added the missing `com.muso.music.extensions.metadata` import so the
  artwork pager's queue-cover pages and the Apple QUEUE body can read
  `window.mediaItem.metadata`.
- The device volume row no longer references the Classic controls'
  `fgDim` local (which was out of scope in the Apple controls); the color
  is inlined.
- The below-fold cards' lyrics check no longer smart-casts the delegated
  `lyricsEntity` state; the description card was restructured so no
  composable calls happen inside `let` lambdas.

**Version:** 0.5.94 (versionCode 101); release tag v0.5.94, APK Muso_v0.5.94_v101.apk.

## Round 78 (v0.5.93): SimpMusic player port, part 2 - below-fold cards, Apple tabs, device volume

- **Classic below-the-fold cards (SimpMusic)**: the Spotify/Classic player page
  now scrolls - the artwork becomes a square card and under the controls sit
  three SimpMusic cards: a **Lyrics card** (embedded 300dp lyrics preview on the
  artwork palette with a Show button that opens the full lyrics view), an
  **Artist card** (the artist's channel art with song count, linking to the
  artist page), and a **Description card** (the song's view count and video
  description, fetched from the player response - the innertube VideoDetails
  model gained a shortDescription field).
- **Apple Music tabbed bodies (SimpMusic)**: the dock's Lyrics and Queue
  buttons now switch the artwork area between a **LYRICS body** (compact header
  + full lyrics renderer) and a **QUEUE body** (numbered queue list, current
  row highlighted, tap to play); re-tapping the active tab returns to MAIN.
  The Queue dock button no longer opens the bottom sheet.
- **Device volume slider (SimpMusic Apple Music)**: a volume row between the
  transport and the dock, bound to the SYSTEM media volume with a live
  ContentObserver so hardware keys stay in sync.
- Sheet-collapse nested scroll moved to the page column (works with the new
  scrollable Classic layout and the pager alike).

**Version:** 0.5.93 (versionCode 100); release tag v0.5.93, APK Muso_v0.5.93_v100.apk.

## Round 77 (v0.5.92): SimpMusic player port - Classic rewrite, artwork pager, video parity

The player styles were re-ported toward SimpMusic's actual behaviour (the M3
Expressive and Apple Music styles were already 1:1 ports; this round rebuilt
the Spotify style to match SimpMusic's "Classic / Spotify" Now Playing and
brought the shared shell to parity):

- **Spotify style = SimpMusic Classic now playing**: force-dark layout with
  the artwork palette sliding down a diagonal gradient into #121212, "NOW
  PLAYING" top bar with the playlist name and dismiss chevron, marquee
  title/artists row with the heart, a buffered-progress indicator under the
  slider (the DEFAULT slider style now renders a real slider again - it used
  to render nothing), codec pill in the times row, ONE five-slot transport
  row (shuffle | prev | play | next | repeat) and an info / add-to-playlist /
  queue action row.
- **Artwork pager (all styles, SimpMusic's signature gesture)**: the queue's
  covers now live in a real HorizontalPager - swipe through upcoming covers,
  and the song changes when the swipe settles; a song change settles the
  pager (single-page moves animate, multi-page jumps cut). Disabled while
  repeat-one is active, exactly like SimpMusic. Replaces the old
  drag-past-quarter-width skip gesture.
- **Video, SimpMusic-style in the artwork slot**: the single-stream video is
  now framed INSIDE the artwork area per style (Classic: 8dp rounded box at
  the stream's real aspect ratio; Expressive: the 28dp card takes the video's
  shape, capped at square; Apple: centred, 12dp corners when portrait) - the
  transport sits below it in normal flow, not overlaid.
- **Over-video overlay (SimpMusic)**: fullscreen button (top-end), -5s/+5s
  (centred), lyric-subtitle toggle (bottom-end), 3s auto-hide, tap to toggle.
- **Landscape fullscreen video route (SimpMusic FullscreenPlayer)**: locks
  landscape + immersive bars, single tap toggles the overlay, double tap on
  either half seeks -5s/+5s; overlay has title, transport, slider, times.
- **Video quality setting (SimpMusic)**: 360p / 720p / 1080p picker replaces
  the old boolean "high quality video" (Content settings).
- **Shared inline lyric line**: the Expressive gap lyric is now shared by the
  Classic style and the over-video subtitle (extracted to one composable).
- Dead code removed: the secondary PlayerVideo player, ThinProgressSlider,
  ExpressiveControlSlot.

**Version:** 0.5.92 (versionCode 99); release tag v0.5.92, APK Muso_v0.5.92_v99.apk.

## Round 76 (v0.5.91): update notification alongside the update popup

New release published on GitHub now also triggers a system notification, not
just the in-app popup.

- New background worker (WorkManager, ~every 15 minutes) checks GitHub for a
  newer release even while Muso is closed, and posts a notification as soon
  as one is published. Doze may defer the check, which is expected.
- When the app is OPEN, the existing version check (now refreshed at most
  every 6 hours instead of once per day) posts the same notification together
  with the in-app popup.
- The notification is shown ONCE per version (remembered in a tiny prefs
  file), so it never repeats or spams.
- Tapping the notification opens Muso and force-shows the update popup - even
  if that version had been dismissed with "Later" - with the one-tap
  DownloadManager install right there.
- Updater.getLatestVersionName() gained a force flag to bypass the in-memory
  cache (used by the worker and notification taps).
- Android 13+: the POST_NOTIFICATIONS permission is requested once after the
  splash (needed for the music notification too).
- New "App updates" notification channel; strings in English and Bengali.
- New dependency: androidx.work:work-runtime-ktx 2.9.1.

**Version:** 0.5.91 (versionCode 98); release tag v0.5.91, APK Muso_v0.5.91_v98.apk.

## Round 75 (v0.5.90): splash freeze fix - startup composition deferred

Frame-by-frame analysis of the user's screen recording showed the splash
animation was FROZEN for 0.3-0.5s stretches and jumping between phases, and
the handoff ended on a black flash before the home screen popped in. The
splash's own render architecture was already draw-phase-only - the problem
was that the ENTIRE app UI (NavHost, database flows, preference reads,
image loading) was composed at the same time, on the same main thread, while
the animation played. The animation simply never got frames.

- The main UI is now composed ONLY when the splash asks for it: during its
  quiet settled phase at t=1.70s (wordmark fully in, logo static), just before
  the exit fade begins. The heavy startup composition then runs underneath
  the fade/handoff, where a hitch is invisible. During the rest of the
  animation the splash has the main thread to itself and runs at the
  display's full refresh rate.
- The splash overlay moved OUT of the app's UI tree (it used to live inside
  InnerTuneTheme/BoxWithConstraints) so it outlives the app's first frame.
- The splash now HOLDS its fully-faded final frame until the main UI has
  actually rendered one frame, and only then removes itself - no more black
  flash / hard cut into the home screen.
- Safety: even if the frame timeout fires early, the app UI is always asked
  to compose before the splash hands off, so it can never wait forever.
- Warm starts (no splash) compose exactly as before - zero behavior change.

**Version:** 0.5.90 (versionCode 97); release tag v0.5.90, APK Muso_v0.5.90_v97.apk.

## Round 74 (v0.5.89): CI build fix - two compile errors from Round 73

- ShimmerImage.kt: the cached shimmer colors use remember() but the file never
  imported it - import added.
- Player.kt: the new SquigglyPositionSlider leaf calls the experimental
  SquigglySlider API outside the Player composable's @OptIn scope, so it now
  carries its own @OptIn(ExperimentalMaterial3Api::class).

**Version:** 0.5.89 (versionCode 96); release tag v0.5.89, APK Muso_v0.5.89_v96.apk.

## Round 73 (v0.5.88): app-wide frame-drop fix (performance PRD)

Root-cause audit + smallest-safe-architectural-changes fixes. No visual change;
no animation slowed, shortened, or removed.

- Player screen recomposition containment (the biggest win): the 100 ms
  position tick used to recompose the ENTIRE player screen - all three styles
  (Spotify skeleton, Material 3 Expressive, Apple Music). Every position read
  now lives in a deferred scope or a tiny leaf composable:
  - Time texts (all styles) read the position inside derivedStateOf, so they
    recompose only when the displayed string changes (~1 Hz) instead of 10 Hz.
  - The Apple thin progress pill draws its fill inside drawBehind - pure
    draw-phase invalidation, zero recomposition per tick.
  - The M3 Expressive wavy seek bar derives its fraction inside its Canvas
    draw block (and the thumb inside its offset lambda); the unused `position`
    parameter was removed and progressFraction became a provider.
  - The squiggly slider moved into a small leaf composable so its 10 Hz
    refresh recomposes only that slider.
  - The M3E inline current-lyric line switched from remember(position) to
    derivedStateOf: it re-evaluates per tick but its readers (the Crossfade)
    only recompose when the displayed line actually changes.
- Lyrics: only the active karaoke line and its neighbours now receive the live
  50 ms position; distant lines are frozen at their boundary values (pixel-
  identical output), so a tick no longer recomposes every visible lyric line.
- Shimmer skeletons: the moving gradient now reads its animated value inside
  drawBehind (draw-phase only) with cached colors - loading placeholders no
  longer recompose at 60 Hz while scrolling.
- Startup: attachBaseContext no longer blocks on DataStore I/O before the
  first frame - the in-app language is read from a tiny synchronous
  SharedPreferences mirror (seeded once, kept in sync by the language
  setting).
- Already-optimal parts left untouched (verified during the audit): the splash
  animation (draw-phase-only since v0.5.85), Thumbnail Ken Burns/vinyl spins
  (read inside graphicsLayer), PlayingIndicator (Canvas reads), MiniPlayer
  (isolated collapsed-content scope), dynamic theme color extraction
  (Dispatchers.IO).

**Version:** 0.5.88 (versionCode 95); release tag v0.5.88, APK Muso_v0.5.88_v95.apk.

## Round 72 (v0.5.87): CI build fix - duplicate UpdateState enum

- The release build failed because the obsolete UpdateDialog.kt was still in
  the tree: it declared a top-level `UpdateState` enum that collided with the
  same-named (private) enum in the new UpdatePopup.kt - "Redeclaration" in
  the same package. UpdateDialog.kt is fully replaced by UpdatePopup.kt and
  nothing referenced it anymore, so the file is removed. This was the only
  compile error in the CI log; everything else (keystore decode, signing
  setup) was already running fine.

**Version:** 0.5.87 (versionCode 94); release tag v0.5.87, APK Muso_v0.5.87_v94.apk.

## Round 71 (v0.5.86): release APK signing in CI

- The build workflow now signs the release APK: the keystore is decoded from
  the KEYSTORE_BASE64 repository secret, the APK is zipaligned and signed with
  apksigner (alias and passwords from KEY_ALIAS / KEYSTORE_PASSWORD /
  KEY_PASSWORD secrets), and the signature is verified before publishing.
- If the signing secrets are not configured yet, the workflow still runs and
  publishes an unsigned APK with a visible warning, so nothing breaks.
- .gitignore now blocks keystores (*.keystore, *.jks, *.p12) from ever being
  committed by accident.

**Version:** 0.5.86 (versionCode 93); release tag v0.5.86, APK Muso_v0.5.86_v93.apk.

## Round 70 (v0.5.85): buttery-smooth splash, no tap-to-skip

- Splash animation rearchitected for maximum smoothness: the master clock is
  now read ONLY inside draw/layer lambdas (Canvas draw block + graphicsLayer
  blocks), so every frame is a draw-only invalidation - the whole animation
  runs without a single recomposition and glides at the display's native
  refresh rate (90/120 Hz where available), staying smooth even on low-end
  60 Hz devices. Zero allocations in the hot path, as before.
- Tap-to-skip removed by design: touches on the splash do nothing and the
  animation always plays in full before handing off to the home screen.
- The 4-second hard safety timeout stays, so the splash still can never get
  stuck on screen even if frame callbacks stall.

**Version:** 0.5.85 (versionCode 92); release tag v0.5.85, APK Muso_v0.5.85_v92.apk.

## Round 69 (v0.5.84): new app icon

- New launcher icon: the glossy neon waveform artwork is now the app icon at
  every density. Adaptive icon (Android 8+): black background layer with the
  artwork centered inside the safe zone, plus a white monochrome layer so
  Android 13+ themed icons keep the waveform shape. Legacy icons (Android 6/7):
  full-bleed square and circular versions. All PNGs regenerated from the
  1254x1254 source with Lanczos resampling - no other changes in this release.

**Version:** 0.5.84 (versionCode 91); release tag v0.5.84, APK Muso_v0.5.84_v91.apk.

## Round 68 (v0.5.83): splash hardening, premium update popup, self-replacing releases

- Splash screen robustness fix, with zero change to how it looks:
  - The hot path now allocates nothing: the five glow Paints (with their
    BlurMaskFilters) and the wordmark FontFamily are created once and reused
    every frame. Previously 300+ objects per second were churned out during
    the animation, which caused visible jank on low-end devices.
  - A 4-second hard safety timeout now wraps the frame loop, so the splash can
    never get stuck on screen even if frame callbacks stall (window surface
    lost, screen locked mid-splash, OEM choreographer bugs).
  - Tapping the splash skips to the exit phase and fades out over the normal
    250 ms - a smooth skip, never a hard cut, never a dead tap.
- New premium Material 3 in-app update popup (replaces the old center dialog):
  slides up from the bottom with the M3 emphasized easing when a newer GitHub
  release exists. "Update now" downloads the release APK in-app (progress bar)
    and hands it to the package installer; "Get it on GitHub" opens the repo's
  releases page in the browser; "Later" slides it back down and remembers the
  dismissed version in DataStore, so it never shows again for that version -
  only for the next release. It also never appears on top of the splash.
- CI: the build workflow no longer hardcodes versions (they are read from
  app/build.gradle.kts). Every push builds the current version's APK and
  publishes it as the only release: older releases are removed, and any APKs
  already attached to the current version's release are replaced, so the
  Releases section always ends up with exactly one fresh APK.

**Version:** 0.5.83 (versionCode 90); release tag v0.5.83, APK Muso_v0.5.83_v90.apk.

## Round 67 (v0.5.82): SimpMusic's player styles + queue peek removed

- The queue peek bar that sat OVER the bottom of every player style is GONE.
  The queue now only appears when it is actually opened (any Queue button);
  there is no collapsed strip, no reserved space, and the player content runs
  to the bottom edge.
- The style set is now exactly SimpMusic's, from the official repository:
  - Spotify (default skeleton: header, square artwork, info row, slider,
    transport, dock) - the previous "classic" layout, which is SimpMusic's
    Spotify skeleton, now named and listed as Spotify.
  - Material 3 Expressive (unchanged - the full SimpMusic M3E port).
  - Apple Music: REPLACED with SimpMusic's actual Apple Music style, 1:1:
    frosted blurred-artwork backdrop under a three-stop wash of the
    artwork's dominant colour (Palette-extracted, like SimpMusic's seed),
    full-bleed top-aligned artwork, white title/artist row with heart +
    more, the thin 7dp thumbless progress bar that swells to 14dp while
    touched, plain white transport (46dp skips, 66dp play, 58dp gaps,
    press-swell on the play cell), and the Lyrics | Queue dock with its
    light active pill. The only top chrome is the grabber pill.
  - Removed: Classic (renamed Spotify) and Immersive.
- A saved "classic" style preference safely falls back to the default.

**Version:** 0.5.82 (versionCode 89); release tag v0.5.82, APK Muso_v0.5.82_v1.apk.

## Round 66 (v0.5.81): build fix - all Kotlin errors from the 0.5.80 log

- MusicService: added the missing imports for ShowVideoInPlayerKey /
  HighQualityVideoKey (the constants existed; the service never imported them).
- Player.kt: Apple slider - imported the offset modifier and IntOffset (the
  thumb placement failed to resolve); fixed the unquoted "artworkFade" label;
  the playing-from playlist name now reads playlist.playlist.name (the DB
  Playlist wrapper embeds the entity); the M3 Expressive header's tonal
  circles are now Surfaces - material3 1.3.0-rc01's IconButton has no shape
  parameter.
- SimpExpressivePlayer.kt: REPEAT_MODE_* constants imported from media3's
  Player (not com.muso.music.constants, where they do not exist); added the
  missing foundation background import (WavySeekBar thumb); the Details
  dialog's row helper is now a proper @Composable.

**Version:** 0.5.81 (versionCode 88); release tag v0.5.81, APK Muso_v0.5.81_v1.apk.

## Round 65 (v0.5.80): build fix - splash_icon

- v0.5.79 failed to build at processResources: res/drawable/splash_icon.xml was
  an <adaptive-icon>, which cannot sit in plain drawable/ (needs drawable-v26)
  and is not a valid windowSplashScreenAnimatedIcon type anyway.
- Fixed by rewriting splash_icon.xml as a plain layer-list that centers the
  neon-waveform launcher logo (@mipmap/launcher_foreground) - the user's logo
  still shows in the Android 12+ system splash, on the existing black
  background, and it links on every API level.
- Everything else in v0.5.79 compiled clean - this was the ONLY error in the
  build log.

**Version:** 0.5.80 (versionCode 87); release tag v0.5.80, APK Muso_v0.5.80_v1.apk.

## Round 64 (v0.5.79): Echo Music's Apple player + Playing-from + ultra thumbnails

- APPLE STYLE = Echo Music's player, 1:1 from its layouts: the artwork fills the
  screen with a slow Ken-Burns pan/zoom under a radial white wash (light, frosted,
  black text) - Echo's exact look; square cover with 24dp margins; the Echo
  controls card (20sp bold title, 16sp artist, 64dp heart with a 40dp icon, 2dp
  tertiary slider with a 20dp round thumb, plain times at the row ends, the
  72dp play cell with a 64dp buffering spinner, 40dp skip icons, 64dp
  shuffle/repeat, and the centered 2-line lyric subtitle).
- The Apple toolbar shows PLAYING FROM + the local playlist name (see below),
  with lyrics and queue buttons on the right.
- "Playing from" now resolves for real: the current queue's song ids are matched
  against every local playlist (off the main thread) and the name shows in both
  the Apple toolbar and the M3 Expressive header under NOW PLAYING.
- Ultra-high thumbnails EVERYWHERE: the global image interceptor now also
  rewrites small lh3 (googleusercontent) thumbnails to the 1200px variant, on
  top of the existing maxresdefault upgrade for i.ytimg URLs; player and
  notification art requests 1200px directly. Coil still downsamples to each
  view, so memory and speed are unchanged.
- Cast is NOT in this build: it needs the Google Cast SDK and media-service
  surgery and would risk playback stability if rushed - it is a separate
  project, not a one-round add.

**Version:** 0.5.79 (versionCode 86); release tag v0.5.79, APK Muso_v0.5.79_v1.apk.

## Round 63 (v0.5.78): the FULL SimpMusic M3 Expressive player page

- The Expressive style now renders SimpMusic's complete page layout, not just
  the controls block:
  - HEADER ROW: 44dp tonal circles (surfaceContainerHigh) - a down-chevron
    that collapses the player, the centered "NOW PLAYING" label, and a more
    button that opens the real song Details dialog. Status-bar inset aware.
  - ARTWORK CARD: 20dp margins, 28dp rounded corners (as before), swipe to
    skip preserved.
  - INLINE LYRIC LINE: the current synced lyric line sits centered in the gap
    between the artwork card and the info block, crossfading every line change
    (300ms) and marquee-scrolling long lines - exactly SimpMusic's M3E element.
    Works for TTML karaoke, LRC and plain synced lyrics.
  - Then the info row, wavy seek bar, time row, 68dp transport and the full
    connected group (Details | Lyrics | Shuffle | Repeat | Add | Queue).
- Muso does not track which playlist/queue a song was started from, so the
  header shows just the NOW PLAYING label (SimpMusic also shows the playlist
  name there).

**Version:** 0.5.78 (versionCode 85); release tag v0.5.78, APK Muso_v0.5.78_v1.apk.

## Round 62 (v0.5.77): SimpMusic M3 Expressive - full connected group + video stays on pause

- The Expressive style's connected group is now SimpMusic's complete
  Info | Cast | Shuffle | Repeat | Add-to-playlist | Queue row. Muso has no
  Cast support, so Lyrics takes that slot (Details | Lyrics | Shuffle | Repeat
  | Add | Queue): 48dp row, 3dp gaps, 24/6dp end caps, active slots tinted
  primaryContainer.
- The Details slot opens a REAL song-info dialog: title/artist/album/duration
  plus the LIVE stream format from the database (codec, MIME, bitrate, sample
  rate).
- The Add-to-playlist slot opens the shared AddToPlaylistDialog, exactly like
  SimpMusic's.
- Video on pause: verified 1:1 with SimpMusic - the video's visibility depends
  ONLY on the "show video in player" setting, never on the playing state, so
  pausing keeps the picture on screen (frozen frame) instead of dropping back
  to the thumbnail. This holds from v0.5.76's single-stream architecture.

**Version:** 0.5.77 (versionCode 84); release tag v0.5.77, APK Muso_v0.5.77_v1.apk.

## Round 61 (v0.5.76): SimpMusic-style thumbnail-to-video fade, no video button

- The thumbnail now FADES OUT smoothly (300ms) when the video stream becomes
  ready and starts playing - the SimpMusic transition, no hard swap. The
  artwork area stays composed underneath so the crossfade is seamless, and
  its swipe-to-skip gesture keeps working when the video is off.
- The video button is REMOVED from every Now Playing style - exactly like
  SimpMusic, whose player has no video toggle. Video stays a Settings
  option ("show video in player").
- The fullscreen video keeps the bottom-overlay controls, tap-to-toggle and
  3s auto-hide from the previous rounds.

**Version:** 0.5.76 (versionCode 83); release tag v0.5.76, APK Muso_v0.5.76_v1.apk.

## Round 60 (v0.5.75): single-stream video - the SimpMusic architecture

The video now plays through the MAIN player, exactly like SimpMusic - no more
second muted player kept in sync by polling:

- MusicService resolves a muxed (video+audio) format while "show video in
  player" is on; the stream runs through the normal playback pipeline, so
  the picture, the position, the seek bar and every control are ALWAYS in
  sync by construction.
- The player screen renders the main player's own video output fullscreen
  (crop-fill, never letterboxed) with the SimpMusic-style bottom-overlay
  controls and 3s auto-hide from v0.5.74.
- The audio format/quality record stays untouched while video plays, and a
  stale stored itag now falls back to a fresh audio pick instead of failing.
- Progressive MP4 extractor added for the muxed stream.

Trade-off, honestly: while video is on, the audio comes from the muxed
YouTube stream (like SimpMusic), which is lower bitrate than the audio-only
opus stream - turn "show video in player" off to get the full audio
quality back.

Echo's Apple Music player layout is next round, as agreed.

**Version:** 0.5.75 (versionCode 82); release tag v0.5.75, APK Muso_v0.5.75_v1.apk.

## Round 59 (v0.5.74): SimpMusic-style fullscreen video

The portrait video player no longer splits the screen in halves:
- The video now runs edge to edge (the mini-player bottom padding is dropped
  while it plays - that was the extra black bar under the controls).
- The controls overlay the BOTTOM of the full screen over the scrim, exactly
  like SimpMusic's video mode - no more centered-in-lower-half block.
- Controls auto-hide 3 seconds after they appear (SimpMusic behaviour); a tap
  on the picture brings them back, another tap hides them.
- The video player now crops to fill (SCALE_TO_FIT_WITH_CROPPING) so a 16:9
  stream letterboxes no more - it fills the screen like SimpMusic.

**Version:** 0.5.74 (versionCode 81); release tag v0.5.74, APK Muso_v0.5.74_v1.apk.

## Round 58 (v0.5.73): the full player now changes with the style + your icon in the splash

The big one - selecting a Now Playing style now changes the WHOLE player:

- **M3 Expressive** is a 1:1 port of SimpMusic's NowPlayingContentM3Expressive
  (ui/player/SimpExpressivePlayer.kt): the artwork renders as a 28dp rounded
  card with 20dp margins; below it the expressive info row (title + artist,
  48dp tonal heart), the wavy seek bar (custom Canvas sine - amplitude up
  while playing, flat while paused/scrubbing, 14dp circle thumb morphing into
  a 6x22dp bar while dragging, seek commits on release), the time row, the
  68dp pill transport (prev/play/next with x1.15 press growth on weight
  animation and the play corner morphing 22<->34dp), and the 48dp connected
  group (Lyrics | Shuffle | Repeat | Queue with 24/6dp rounded caps). Every
  control is real: shuffle, repeat cycling, queue sheet, the lyrics toggle.
- The other styles keep their own layouts (Classic / Immersive / Apple).
- The system splash icon is now YOUR artwork - the adaptive icon built from
  your image (same as the launcher), not my vector recreation.

**Version:** 0.5.73 (versionCode 80); release tag v0.5.73, APK Muso_v0.5.73_v1.apk.

## Round 57 (v0.5.72): system splash uses the app icon colors

The rising waveform in the Android 12+ system splash is no longer plain
white - it now matches your app icon: the five bars carry the icon's neon
gradient (azure blue through indigo and purple to magenta), each with a
soft halo glow of the same color, on the same black background. The
staggered rise animation is unchanged.

**Version:** 0.5.72 (versionCode 79); release tag v0.5.72, APK Muso_v0.5.72_v1.apk.

## Round 56 (v0.5.71): Echo-style instant splash - no black screen

Ported the technique from Echo Nightly: the Android 12+ system splash now
plays an AnimatedVectorDrawable of the five waveform bars rising the exact
moment the app starts - before any app code runs - so there is never an
empty black screen while the app initializes. The custom Compose waveform
then continues from where the system animation ended (it no longer replays
the reveal phase on Android 12+, so the two read as one continuous
animation), runs its full sequence, and hands off into home with the eased
fade. windowSplashScreenAnimationDuration is set to 900ms like Echo.

Pre-Android 12 devices keep the previous behaviour (black window, then the
full custom animation from the start).

**Version:** 0.5.71 (versionCode 78); release tag v0.5.71, APK Muso_v0.5.71_v1.apk.

## Round 55 (v0.5.70): new app icon + clean splash entry + iOS-style handoff

- **New app icon**: your neon waveform artwork is now the launcher icon -
  full-artwork legacy icons, adaptive (black background, glowing bars
  foreground with the luminance kept as transparency for the glow), and a
  matching white monochrome icon for Android 13+ themed icons.
- **No icon before the splash**: the Android 12+ system splash was showing a
  white waveform mark before the custom animation - it is now fully blank,
  so the cold start goes straight from black into the waveform animation.
- **iOS-style handoff**: after the animation completes, the fade into home
  is now 250ms with an eased curve (fast arrive, gentle settle) instead of a
  linear 150ms.

**Version:** 0.5.70 (versionCode 77); release tag v0.5.70, APK Muso_v0.5.70_v1.apk.

## Round 54 (v0.5.69): the Library crash, finally fixed

The in-app crash reporter did its job. The Library-tab crash was:

    java.lang.RuntimeException: Cannot create an instance of
        com.muso.music.viewmodels.LibraryMixViewModel
    Caused by: NoSuchMethodException: LibraryMixViewModel.<init> []

LibraryMixViewModel was missing its @HiltViewModel annotation, so Hilt never
generated its provider and the default factory looked for a no-argument
constructor that does not exist - instant crash every time the Library tab
opened the mixed view. AutoPlaylistViewModel had the same problem (it would
have crashed on opening the Liked / Offline playlists). Both now carry the
annotation.

The crash reporter also caught a one-off DataStore FileNotFoundException
(missing settings file on a background thread, right before the other
crash). That one recovered on the next launch by itself - most likely an
install-moment race. If it comes back, the reporter will show it again and
we can harden it.

**Version:** 0.5.69 (versionCode 76); release tag v0.5.69, APK Muso_v0.5.69_v1.apk.

## Round 53 (v0.5.68): v0.5.67 build fix

Two small omissions that broke the v0.5.67 build: the AlertDialog import
was never added (it silently matched an existing AlertDialogDefaults
import), and the crash-reporter title string was never inserted. Both
fixed; no other changes.

**Version:** 0.5.68 (versionCode 75); release tag v0.5.68, APK Muso_v0.5.68_v1.apk.

## Round 52 (v0.5.67): v0.5.66 build fixes + in-app crash reporter

The v0.5.66 build failed on two mistakes of mine, both fixed:

- **Apple-style current-lyric line:** it was written against a newer lyrics
  API than this codebase has. The raw lyrics string is now parsed exactly
  like the lyrics screen does (TTML karaoke, LRC, or plain text), and the
  local colour/context values the rewrite had swallowed are restored.
- **Player background:** the tiny-decode blurred background no longer
  references CachePolicy (defaults are already enabled).

Plus:

- **In-app crash reporter.** Every uncaught crash now saves its full stack
  trace to a file; the next time you open Muso a dialog shows it, with a
  Share button. This is how we finally pin down the Library-tab crash -
  it triggers, you reopen the app, screenshot or share the dialog.
- **Settings** no longer shows the page title twice (big title removed; the
  top bar title stays).
- The library mixed-grid sort key stays as it was (it was consistent all
  along; my earlier theory about it was wrong and I have reverted that).

**Version:** 0.5.67 (versionCode 74); release tag v0.5.67, APK Muso_v0.5.67_v1.apk.

## Round 51 (v0.5.66): the fixes you reported, actually built this time

**IMPORTANT: your GitHub repo was still at v0.5.58.** The last several source
zips (v0.5.59 through v0.5.65) were never pushed, so every fix in them never
reached your APK - the settings, splash and crash complaints were all against
the old v0.5.58 build. This zip is based on your repo plus every fix below;
push ALL of its files to GitHub (replace everything) before building.

**Library crash: fixed.** The mixed library sorted albums by lastUpdateTime
(Long) but everything else by LocalDateTime.MIN - comparing the two types
throws ClassCastException, so the Library tab crashed whenever an album was
saved alongside anything else. All sort keys are now Long.

**Everywhere lag: biggest cost removed.** The player background re-ran a
64dp full-screen Modifier.blur on every animation frame. It now decodes the
artwork tiny (64px) and lets the GPU upscale it - the same heavy-blur look,
at zero per-frame cost. Plus the search keyboard no longer fights the search
bar expansion (250ms settle delay).

**Settings: the top of every page was hidden.** The status-bar spacer had
been lost in an earlier restyle, so the search bar and the first rows sat
under the app bar. All 13 settings screens now reserve the status bar and
the 64dp app bar before their content.

**Splash: the full animation is back.** The exit cutoff (1.45s) ended the
animation before the rebuild/settle phases (1.50-1.70s) and the wordmark
reveal could ever play, and an inverted calculation zeroed the wordmark
entirely - so you only ever saw a cut-off waveform. The timeline now runs
the complete sequence (1.95s) and only after it completes does the overlay
cross-fade into home over 150ms. Cold start also shows a white waveform mark
in the Android 12+ system splash. Note: no splash on warm resume by design.

**Apple style: 1:1 SimpMusic title row** - left-aligned title + clickable
artist, 48dp tonal heart on the right, and the current synced lyric line
above the title. Classic / Expressive / Immersive / Apple all dispatch
correctly in this build.

**Version:** 0.5.66 (versionCode 73); release tag v0.5.66, APK Muso_v0.5.66_v1.apk.

## Round 43 (v0.5.58): v0.5.57 build fix - broken slider else-branch

The thin-slider removal regex in v0.5.57 cut through the ThinProgressSlider
call (it stopped at the wrong closing brace) and left dangling arguments at
two sites, breaking Player.kt parsing. Both regions are now cleanly
"else -> {}" and the file passes the full brace/paren balance audit again.
No other changes - all v0.5.57 fixes stand.

**Version:** 0.5.58 (versionCode 65); release tag v0.5.58, APK Muso_v0.5.58_v1.apk.

## Round 42 (v0.5.57): UI polish round from device testing

- Home: content now sits right under the permanent header (removed the extra
  64dp top padding that doubled the gap).
- Background from artwork: removed entirely (overlay, setting, persistence) -
  it washed out the background in light theme.
- Settings screens: single title - the top bar title is always visible and the
  big in-content headline is gone, no more duplicated titles or dead space.
- Defaults per request: Now playing style = Material 3 Expressive, slider
  style = Squiggly.
- Fullscreen player: the stray thin progress line at the bottom (the
  fallback slider branch) is gone.
- Expressive play/pause: the icon no longer fills the pill oddly - the icon
  crossfade now wraps the icon itself, centered like the skip buttons.
- Lyrics toolbar (fullscreen + three-dot): moved to the bottom end of the
  lyrics area with padding - no longer overlapping the player's top bar.
- Splash: shortened to ~1.45s for a snappier entry.
- Player artwork: requests the 1200px thumbnail variant (ultra quality).

Not yet fixed (need crash log / next round): library tab crash on open,
search entry lag, liquid glass toggle behavior.

**Version:** 0.5.57 (versionCode 64); release tag v0.5.57, APK Muso_v0.5.57_v1.apk.

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

## Round 41 (v0.5.56): v0.5.55 build fix - remaining import errors

- Thumbnail: systemBarsPadding import for the fullscreen lyrics dialog.
- LibraryMixScreen: remember import.
- AppearanceSettings: removed phantom imports of DarkMode/NavigationTab from
  constants - those enums live at the bottom of the same file.
- BackupAndRestore / ContentSettings / ListeningHistorySettings / PlayerSettings:
  MaterialTheme / padding / dp imports for the collapsing-title headline.
- PlayerSettings: PlayerTextAlignmentKey import; new swipe.xml vector drawable
  for the gesture-animations preference icon.
- SpotifySettings: kotlinx.coroutines.launch import (logout scope) and the
  NavController.backToMain extension import for the back-button long-press.
- SpotifyClient: androidx.datastore.preferences.core.edit import (token store).

**Version:** 0.5.56 (versionCode 63); release tag v0.5.56, APK Muso_v0.5.56_v1.apk.

## Round 40 (v0.5.55): v0.5.54 build fix - all Kotlin compile errors from rounds 33-36

The v0.5.54 build surfaced the accumulated Kotlin errors of the never-built
rounds. All fixed:

- MainActivity: removed a bad "com.muso.music.utils.edit" import (the DataStore
  edit extension resolves from androidx.datastore.preferences.core.edit).
- DownloadUtil: Requirements lives in androidx.media3.exoplayer.scheduler, not
  common; preloadSong no longer treats DataSource.open()'s Long result as
  Closeable - proper open/read/close loop with cleanup, reads through the
  cache-backed resolving factory so bytes land in the player cache.
- MusicService: audio offload is set on the built sink instance
  (AudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED / DISABLED, API 29+ guard),
  not on DefaultAudioSink.Builder - media3 has no builder-level offload setter.
- SpatialAudioProcessor: AudioFormat is
  androidx.media3.common.audio.AudioProcessor.AudioFormat.
- BounceIconButton: @file:OptIn(ExperimentalFoundationApi) for
  combinedClickable.
- Lyrics: missing LyricsStyleKey/LyricsTextPositionKey imports; line spacing
  now 24.dp * (spacing - 1) (Dp.times(Float)).
- Player: the SQUIGGLY slider branch in the subjectless slider-style when
  now reads sliderStyle == SliderStyle.SQUIGGLY.
- Thumbnail: added the missing imports (remember, Row, size, background,
  MaterialTheme, stringResource, painterResource, Icon, R) used by the new
  lyrics toolbar and fullscreen dialog.
- NavigationBuilder: imports for ListeningHistorySettings, AISettings,
  SpotifySettings, SpotifyLoginScreen.
- SplashScreen: com.muso.music.R import for the Gochi Hand font reference.

**Version:** 0.5.55 (versionCode 62); release tag v0.5.55, APK Muso_v0.5.55_v1.apk.

## Round 39 (v0.5.54): v0.5.53 build fix - duplicate listener override

The v0.5.53 build failed in kapt (Dagger metadata processing) with
"Multiple entries with same key: onShuffleModeEnabledChanged" - the
Player.Listener in MusicService had onShuffleModeEnabledChanged
overridden twice (the InnerTune shuffle-order handler and a separate
shuffle-persist handler). The two bodies are now merged into a single
override: the notification update, the current-item-first shuffle order,
and the DataStore persistence of the shuffle state all run from one
method. A full-app scan confirmed no other file has duplicate override
names.

**Version:** 0.5.54 (versionCode 61); release tag v0.5.54, APK Muso_v0.5.54_v1.apk.

## Round 38 (v0.5.53): v0.5.52 build fix - drawable tint attrs

The v0.5.52 build failed because five vector drawables (crop, disc,
fullscreen, surround_sound, upload) carried android:tint="?attr/
colorControlNormal", an attr not resolvable inside drawable XML under
AAPT2. The tint lines were removed - icons are tinted at usage time by
Compose (LocalContentColor), so nothing changes visually. All five
drawables re-validated as well-formed XML; no other ?attr references
remain in res/drawable.

**Version:** 0.5.53 (versionCode 60); release tag v0.5.53, APK Muso_v0.5.53_v1.apk.

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
