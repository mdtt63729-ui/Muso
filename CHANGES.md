## Muso 0.5.237 — fix the app-wide lag (preference getter fell back to disk)
- The two synchronous preference getters in DataStore.kt were written as
  "snapshot?.let { it[key] } ?: runBlocking(Dispatchers.IO) { data.first()[key] }", where
  the elvis binds to the VALUE, not to the snapshot. So every read of an UNSET preference
  (i.e. most reads in the app) - and of a legitimately null one - fell through to a
  blocking DataStore read on the calling thread. That is app-wide main-thread disk I/O:
  the reported lag with or without a song playing.
- Introduced by the 0.5.230 crash-guard rewrite; 0.5.225-0.5.229 were correct. Fixed by
  moving the fallback back onto the snapshot, keeping the ClassCastException guard.
- Docs: added docs/FIX_PREFERENCE_READ_LAG.md.

## Muso 0.5.236 — fix the Home crash (duplicate Lazy key)
- A device run of 0.5.235 crashed on startup, on Home:
  java.lang.IllegalArgumentException: Key "FEmusic_moods_and_genres_category" was already
  used.
- Cause: 0.5.232 keyed the Home mood/genre row with key = { it.endpoint.browseId }, but
  that browseId is a CATEGORY id shared by every entry in the row, so the key was not
  unique. (0.5.235 fixed the COMPILE errors in these keys; this fixes a RUNTIME duplicate.)
- Fix: every key added in 0.5.232/0.5.233 that is not backed by a database primary key is
  reverted - moodAndGenres, the two Home YouTube rows, YouTubeBrowseScreen, the three
  artist menus, ModalBottomSheet listYouTubePlaylist, and ChangelogScreen releases.
  Kept (DB primary key or unique by construction): HomeScreen keepListening,
  AddToPlaylistDialog playlists, ModalBottomSheet listLocalPlaylist and listAction,
  ArchiveTune PlayerMenu (distinctBy), and the two ArchiveTune notAddedList rows.
- Docs: added docs/CRASH_FIX_MOOD_GENRES_KEY.md.

## Muso 0.5.235 — fix the six CI compile errors from the stable-state keys
- CI failed at :app:compileFossReleaseKotlin with six errors, all from the list keys added
  in 0.5.232/0.5.233. Fixed:
  * DownloadUtil: C.PERCENTAGE_UNSET is an Int but percentDownloaded is a Float, so the
    comparison was invalid; it now compares against C.PERCENTAGE_UNSET.toFloat().
  * MediaMetadataMenu / PlayerMenu / YouTubeSongMenu: MediaMetadata.Artist.id is nullable,
    so "key = { it.id }" did not satisfy the non-null key; now "key = { it.id ?: it.name }"
    (the lists are pre-filtered to non-null ids, so the id always wins).
  * ArchiveTune YouTubeAlbumMenu: the element is archivetune Song (: LocalItem), which has
    id, not videoId; key is now { it.id }.
  * ArchiveTune YouTubePlaylistMenu: the element is archivetune models.MediaMetadata,
    which has id: String; key is now { it.id }.
- The same run confirms the rest compiles: it reached :app:compileFossReleaseKotlin, so
  resource processing and KSP passed, and no error came from the Phase 5/8/9/11 work or
  the PlayerSettings LazyColumn pilot.

## Muso 0.5.234 — Phase 9 lazy rendering: pilot on PlayerSettings
- PlayerSettings now uses a LazyColumn instead of Column(verticalScroll): the top/bottom
  padding moved into contentPadding (the behaviour-preserving form), and each of its four
  direct PreferenceGroup children is wrapped in item { }, so groups below the fold no
  longer compose on open. Group-level laziness, piloted on one screen.
- Why a pilot: row-level laziness needs PreferenceGroup (59 call sites) turned into a
  LazyListScope extension plus item-wrapping across 18 host screens - a ~77-site refactor
  of a DSL shared by every settings screen, unverifiable without a compiler. Proven on one
  screen first. See docs/PHASE9_LAZY_RENDERING_PILOT.md.

## Muso 0.5.233 — Phase 4 stable state: list keys
- Keyed 10 more keyless items() calls, each on a value proven unique: the HomeScreen
  YouTube rows and YouTubeBrowseScreen (YTItem.id), ModalBottomSheet listAction
  (QueueItemAction.name), listYouTubePlaylist (PlaylistsResult.browseId) and
  listLocalPlaylist (LocalPlaylistEntity.id), ArchiveTune PlayerMenu
  (name, already distinctBy), YouTubeAlbumMenu/YouTubePlaylistMenu (Song.videoId) and
  ChangelogScreen (ReleaseInfo.tagName). Together with the 6 from 0.5.232 that is 16.
- 11 keyless items() deliberately remain, each with a stated reason (fixed-count shimmer
  placeholders, a generic ListPreference<T>, a nullable artist id, a String list that can
  hold duplicates, and one false positive inside a comment). See
  docs/PHASE4_STABLE_STATE_KEYS.md.

## Muso 0.5.232 — Phase 4 completed (lifecycle-aware collection + stable state)
- Lifecycle-aware collection is now complete: the last two collectAsState sites
  (HistoryScreen viewModel.events, LibrarySongsScreen viewModel.allSongs) are migrated,
  so there are zero collectAsState call sites left in the app (236/236).
  Correction: I had reported those two as plain Flows needing an explicit type argument.
  They are StateFlows (both end in stateIn(viewModelScope, Lazily, ...)); my earlier
  classifier missed the stateIn because it sat beyond the scanned window.
- Stable state: six keyless items() calls are now keyed on values proven unique -
  HomeScreen keepListening (LocalItem.id) and moodAndGenres (endpoint.browseId), the
  three artist menus (Artist.id), and AddToPlaylistDialog playlists (PlaylistEntity.id).
  Fixed-count shimmer placeholders and lists whose element type could not be resolved
  are deliberately left (a wrong key would be a compile error, a non-unique one a
  runtime duplicate-key crash).
- Phase 9 lazy rendering: analysed and deliberately NOT done. PreferenceGroupScope
  already collects rows into a list, but PreferenceGroup renders them in a plain Column,
  and 18 settings screens host it in a non-lazy Column + verticalScroll. Making it lazy
  means turning PreferenceGroup into a LazyListScope extension and converting all 18
  screens - a cross-cutting refactor that cannot be validated without a build.
- Phase 11: LoginScreen's GlobalScope left as-is on purpose - moving it to the
  composition scope would cancel the account fetch if the user navigates away mid-request.
- Docs: added docs/PHASE4_COMPLETION_AND_PHASE9_LAZY.md.

## Muso 0.5.231 — dead code removed + PRD Phase 11 (memory/lifecycle)
- Deleted app/src/main/java/com/muso/music/ui/component/Lyrics.kt (646 lines). Verified
  unreferenced first: no call site in any source set, no import, no wildcard import. Its
  one live symbol, animateScrollDuration, moved to lyrics/LyricsUtils.kt.
- Removed the Flat / M3-Flex dead render branches from the suite MiniPlayer.kt (224 lines):
  the M3FlexMiniPlayerContent and FlatMiniPlayerBackground composables, the overlay that
  called the latter, the two Flat-only prev/next buttons, the shape/colour/size
  alternatives, and the two parameters threaded through MiniPlayerAndroidPlaybackContent.
  The two constant-false flags are gone. MiniPlayer.kt: 1524 -> 1300 lines.
  (Correction: I had estimated ~600 lines for this; the real span is 224.)
- Phase 11: MusicService.onDestroy now cancels its own coroutine scopes (scope,
  canvasScope). They were created in onCreate and never cancelled, so queue loading,
  crossfade and canvas-preload coroutines kept running against a destroyed service.
- Phase 11: the playback-command sink in MusoSuiteHost created its own uncancelled
  CoroutineScope inside a LaunchedEffect; it now uses the effect's own scope.
- Checked clean: player listener and volume ContentObserver add/remove pairing across
  MusicService, PlayerConnection, MainActivity, FullscreenVideoScreen and Player.kt.
- Docs: added docs/PHASE10_11_DEAD_CODE_AND_MEMORY.md.

## Muso 0.5.230 — PRD Phase 9: settings (duplicate keys + crash guard)
- Duplicate settings: two preference-key files feed the same DataStore, and 76 key names
  were declared in both. Three of them disagreed on TYPE - customThemeColor (int vs
  string), crossfadeDuration (int vs float), lyricsTextSize (int vs float). Since
  Preferences.Key equality is by name, both constants address the same stored value, so
  a read from the other layer cast it to the wrong type and threw ClassCastException.
  customThemeColor was the live one: Muso reads an Int while ArchiveTune's palette
  picker / theme creator (reachable from Muso's settings) write a String - selecting a
  palette and reopening the app crashed it.
- Fixed by giving the ArchiveTune declarations ArchiveTune-scoped key names
  (archivetuneCustomThemeColor / archivetuneCrossfadeDuration / archivetuneLyricsTextSize);
  only the name string changed, so no call site needed editing.
- Also fixed: ArchiveTune's MixSortDescendingKey was declared as "albumSortDescending",
  so the Mix and Album sort toggles were the same stored value. Now "mixSortDescending".
- Crash guard: the typed getters in DataStore.kt (and the two flow mappings inside
  rememberPreference / rememberEnumPreference) now catch ClassCastException and degrade
  to the caller's default, so a stale wrong-typed value can no longer crash a read.
- Docs: added docs/PHASE9_SETTINGS.md.

## Muso 0.5.229 — PRD Phase 8: downloads (async index, throttled progress)
- Startup: DownloadUtil is a @Singleton injected into MainActivity, so its init{} read
  the persisted download index synchronously on the main thread during startup. The
  read, the downloads.value assignment and the artwork-retention pass that depends on
  it now run on cacheScope (Dispatchers.IO). downloads now populates asynchronously;
  every consumer uses the downloads/getDownload flow so the UI is unaffected past the
  first frame.
- Throttled progress: media3 fires onDownloadChanged on every progress tick, and each
  call copied the whole downloads map and emitted a new StateFlow value - recomposing
  every download row and button in the UI. shouldPublishDownload now publishes only a
  new entry, a state change, a non-DOWNLOADING state, or a whole-percent move. State
  changes always publish, so completed/removed side effects are unchanged.
- Cleanup: MusoDownloadHandler.downloads wrapped downloadUtil.downloads in
  flow { collect { emit(...) } } (exactly map); replaced with .map { }.
- Docs: added docs/PHASE8_DOWNLOADS.md.

## Muso 0.5.228 — PRD Phase 6: fullscreen player (+ audit correction)
- CORRECTION: the Phase 1/3 audits claimed a single ExoPlayer.Builder site. That was
  wrong - the search missed builders written across two lines. There are SIX: the audio
  player (MusicService.kt:314), the fullscreen-video player (FullscreenVideoScreen.kt:119),
  the canvas player (MediaPlayerView.kt:184), and three in the ArchiveTune layer. So the
  app runs one audio player plus separate video/canvas players. docs/PHASE1 and
  docs/PHASE3 now carry the correction inline.
- Fullscreen player: BottomSheetPlayer ran a 100 ms position loop unconditionally, feeding
  a collapsed MiniPlayer that is never rendered (showCollapsedMiniPlayer is false at the
  only call site). The loop recomposed the whole player sheet 10x/second for two values
  nothing read. It is now gated on showCollapsedMiniPlayer.
- Documented (not fixed): FullscreenVideoScreen rebuilds its ExoPlayer on every entry
  because the player is created in a keyless remember inside a navigation destination;
  MusoSuiteHost blanks the artwork on every song change (bitmapBridge = null) before
  reloading it.
- Docs: added docs/PHASE6_FULLSCREEN_PLAYER.md.

## Muso 0.5.227 — PRD Phase 5: lyrics (parsing + synchronization)
- Parsing: LyricsUtils only accepted [mm:ss.xx] exactly, so whole classes of LRC files
  were silently dropped line by line — [00:12] (no fraction), [0:12.34] (single-digit
  minute) and the older [00:12:34] colon-fraction form all failed to match and vanished
  from the screen. The patterns now accept 1-2 digit minutes/seconds and an optional
  1-3 digit fraction after '.' or ':', scaled by digit count (a colon fraction reads as
  centiseconds). Validated against 17 sample lines: no regression, 5 new variants parse,
  metadata tags still rejected.
- Synchronization: findCurrentLineIndex was a linear scan called on every 60 Hz position
  tick; it is now a binary search, proven equivalent over 20,000 randomised trials plus
  edge cases. LyricsEntry.compareTo no longer uses (time - other.time).toInt(), which
  could overflow Int and mis-order .sorted().
- Finding: com.muso.music.ui.component.Lyrics (646 lines) is dead — no call sites in any
  source set, and its private helpers are unreferenced. Left in place (deleting a whole
  file is destructive) and documented in docs/PHASE5_LYRICS.md for confirmation.
- Docs: added docs/PHASE5_LYRICS.md.

## Muso 0.5.226 — PRD Phase 4: Compose performance (lifecycle-aware state collection)
- Recomposition isolation: 234 of the 236 collectAsState call sites are now
  collectAsStateWithLifecycle, across 64 files. The receivers were classified first
  (StateFlow vs plain Flow) because only the StateFlow overload is a drop-in — the Flow
  overload takes initialValue: T and returns State<T>, whereas collectAsState() on a
  Flow returns State<T?>. 156 StateFlow sites use the no-arg form; 74 sites that passed
  an explicit initial were renamed to initialValue = ...; 4 sites the extractor could
  not classify (a getStateFlow call and three videoStreamUrl reads) were fixed by hand.
- 2 sites are deliberately left (HistoryScreen viewModel.events and LibrarySongsScreen
  viewModel.allSongs): both are plain Flows with non-null element types, so a null
  initial does not type-check. They need an explicit type argument.
- H7 (StateFlow .value reads in composition) checked and found to be a non-issue in the
  live path: the .value reads there are inside callbacks/effects or are Animatable/
  Dimension/State values, all correct.
- Docs: added docs/PHASE4_COMPOSE_PERFORMANCE.md.

## Muso 0.5.225 — PRD Phase 3: playback architecture (state publishing + blocking reads)
- H1 (DataStore): the synchronous preference getters no longer run runBlocking on every
  read. A process-wide mirror (PreferencesSnapshot) is primed once in App.onCreate via
  primePreferences(), then kept live by a single collector, so every preference read is
  a memory lookup. The blocking read now happens exactly once, at startup.
- H2 (mini player): the position/duration tick no longer drives composition. They travel
  down the mini-player chain as () -> Long providers and are read in the progress
  indicator's draw phase, so the mini-player subtree stops recomposing 4x/second.
- Phase 3 (state publishing): PlayerConnection publishes currentLyrics through a combine
  whose transform called the network translator inline. The connection's scope is
  MainActivity.lifecycleScope (Dispatchers.Main), so the translation HTTP request ran on
  the main thread; it is now wrapped in withContext(Dispatchers.IO).
- Docs: added docs/PHASE3_PLAYBACK_ARCHITECTURE.md; PHASE2 doc updated (H1/H2 marked
  fixed, H4 corrected — the playback runBlocking blocks ExoPlayer's loader thread, not
  the main thread).
- Single player owner verified unchanged: one ExoPlayer.Builder site, one registered
  media service. The second (ArchiveTune) media service stays untouched pending a build.

## Muso 0.5.224 — PRD Phase 1 + Phase 2 audit documents
- Added docs/PHASE1_REPOSITORY_AUDIT.md — the six source graphs (playback, lyrics,
  download, settings, player-style, navigation) for the whole app module.
- Added docs/PHASE2_PERFORMANCE_ROOT_CAUSE.md — the main-thread / blocking /
  recomposition hotspot list, each with file, class, function, root cause, impact,
  fix and verification.
- No runtime code changed in this version; it is the audit baseline for the
  remediation phases that follow.

## Muso 0.5.223 — PRD §13: removed the Flat / M3 Flex style strings
- The now-unused `mini_player_style_flat` and `mini_player_style_m3_flex` strings are
  gone from strings.xml. Nothing referenced them any more after the enum removal, so the
  resource side of the Flat / M3 Flex removal is complete.

## Muso 0.5.222 — PRD §13: Flat and M3 Flex mini-player styles removed
- `MiniPlayerStyle` now has only MINIFY and CLASSIC. FLAT and M3_FLEX are gone from the
  enum, from the settings picker's label map and from the navbar's height map, so they
  can no longer be selected anywhere.
- Migration is automatic and crash-safe: the preference is read with `toEnum`, which
  falls back to the default (MINIFY) when a saved value no longer names an enum entry,
  so a device that had FLAT or M3_FLEX stored simply comes up on MINIFY.
- Note: the two render branches inside MiniPlayer.kt that drew those styles are still
  present but unreachable (their flags are now constant false). Deleting that dead code
  is a follow-up, because removing ~600 lines of layout blind — with no build to check
  it — is not safe.

## Muso 0.5.221 — mini player now fits inline between Home and Search
- The compact Android mini player capsule is drawn in two very different widths: full
  width when it rides above the bar, and only the gap between the Home and Search
  buttons when the bar is collapsed. Its inner row used FIXED widths (a 200dp transport
  cluster + a 300dp track cluster, ~560dp in total), so in the narrow inline slot
  (~224dp on a phone) it overflowed and spilled over the Home and Search buttons.
- The two clusters are now weighted (`weight(1f)` / `weight(1.5f)`) so they share
  whatever width the capsule is actually given, instead of a fixed width that does not
  fit.

## Muso 0.5.220 — home sections no longer overlap / reorder
- Every top-level item in the Home LazyColumn used `Modifier.animateItem()` but had
  NO stable key, so an item's identity was its slot index. As the home sections
  stream in (quick picks, forgotten favourites, keep listening, account playlists,
  similar recommendations, the server sections, new releases, mood & genres, and the
  loading shimmer) the slot at a given index becomes a different section, and
  `animateItem()` animates that slot's content from the old position to the new one -
  which is what made the sections slide over each other and appear in the wrong order.
- Fixed by giving every Home section item a stable `key` (the dynamic loops now use
  `forEachIndexed`), so Compose tracks each section's identity and animates it
  correctly instead of shuffling slot contents.

## Muso 0.5.219 — fix the remaining compile error from the CI log
- `MusicService.kt` line 914: `player.currentMetadata?.mediaType` read a `mediaType`
  field off Muso's own `MediaMetadata` model, which has no such field ("Unresolved
  reference 'mediaType' on receiver of type 'MediaMetadata'"). The media type lives on
  the media3 `MediaMetadata` carried by the current `MediaItem`, so it now reads
  `player.currentMediaItem?.mediaMetadata?.mediaType`.

## Muso 0.5.217 — fix the two compile errors from the CI log
- `HomeViewModel.kt`: inside the cold-start loader the destructured locals
  `quickPicks` / `forgottenFavorites` / `keepListening` shadowed the class's
  `MutableStateFlow` properties, so the `HomeCache(...)` call's `quickPicks.value`
  etc. resolved against the plain `List` locals ("Unresolved reference 'value' on
  receiver of type 'List<Song>'"). Qualified them with `this.` like the rest of the
  function already does.
- `MediaPlayerView.kt`: `player.currentMediaItem?.mediaMetadata?.id` read an `id`
  field that does not exist on `androidx.media3.common.MediaMetadata`. The id lives
  on `MediaItem`, so it now reads `player.currentMediaItem?.mediaId` (the same
  accessor the file already uses elsewhere).

## Muso 0.5.216 — playlist header: artwork is now the dominant element
- On the playlist screen the header artwork was only half the screen tall with the
  page-colour scrim running over its full height, so the white background read as
  more prominent than the thumbnail. The artwork is now 60% of the screen tall and
  the scrim spans only its lower ~70%: the top ~30% of the thumbnail stays
  untouched, the fade into the page is a long, even blend, and the thumbnail is the
  highlighted element instead of the white.

## Muso 0.5.215 — full-flavour build fix (missing Cast / Identity deps)
- The `full` flavour's Kotlin did not resolve: its cast player and Google-Drive
  backup import `androidx.media3.cast.*`, `com.google.android.gms.cast.*` and
  `com.google.android.gms.auth.api.identity.*`, but no dependency provided them.
  Added `androidx.media3:media3-cast` (media3 1.11.1), `play-services-auth` 21.5.1
  and `play-services-cast-framework` 21.5.0 to `fullImplementation`.
  (The `foss` flavour — the one CI builds and ships — was never affected.)
- Deeper re-audit pass: every declared dependency version verified to exist in
  Maven Central / Google Maven / the Gradle Plugin Portal; aapt-level resource
  checks (string escaping, format args, resource-name validity, locale qualifiers,
  style parents); manifest `android:exported` on every component; Hilt/Room
  annotation correctness; duplicate top-level declarations; BuildConfig field
  usage; flavour source-set compatibility.

## Muso 0.5.214 — full project re-audit + updater asset fix
- In-app updater now selects the release's **APK** asset by name instead of blindly
  taking `assets[0]`; a release that also carries a `-source.zip` (or any other
  asset) no longer makes the DownloadManager fetch a non-installable file.
- Full A-Z re-audit of every file: version catalog, all `libs.*` / `projects.*`
  references, every internal import, all Android resource references (Kotlin `R.*`
  and XML `@type/name`), the merged manifest's classes, per-module duplicate
  types/resources, `BuildConfig` field usages, flavor (foss/full) source-set
  compatibility, and a syntax parse of all 1340 Kotlin files.

## Muso 0.5.213 — persistent in-app update prompt + build fixes
- The in-app update popup now re-appears on every app launch / foreground until the
  app is actually updated to the latest GitHub release, instead of being suppressed
  permanently for a version the user dismissed with "Later".
- Update detection is now semver-aware, so a release tag carrying a suffix
  (e.g. "v0.5.213-source") still matches the installed version and the popup
  stops correctly once the app has been updated.
- Fixed a version-catalog typo: `compose-foundation` used a literal
  `version = "compose"` instead of `version.ref = "compose"`, which broke
  dependency resolution for `androidx.compose.foundation:foundation`.

## Muso 0.5.212 — synced lyrics + live download UI
- Frame-synced letter-by-letter lyrics progression shared by Echo animation styles.
- Downloads filter now keeps active/queued/paused items visible and reveals artwork with real download progress.

# Round 195 (v0.5.212, code 219) — smoother navigation/search, playlist loading indicator for lyrics, home logo, immersive lyrics fullscreen

1. Button-to-button (top-level tab) switch and the page motion felt laggy. The tab
   crossfade was 150 ms, which left the incoming screen's first composition - the
   heaviest frame of the switch - inside a blink; it is now 210 ms (still a
   crossfade, never a slide). The search pane also composes one short beat after
   the bar opens instead of in the same frame, so tapping search no longer fights
   the bar's own open animation (and the online screen's network work no longer
   lands on the tap frame).

2. Lyrics loading now uses the PLAYLIST screen's loading indicator (the M3
   contained loading indicator, CenterLoadingBox) in the fullscreen lyrics sheet
   and in the Spotify / Expressive-cards lyrics sections, in place of the old
   spinners - the previous lyrics loading animation is gone.

3. The home header shows the splash mark as the app logo, left of the "Muso"
   wordmark. It is the same animation (MusoLogoMark, extracted from the splash),
   and tapping it replays the materialize seamlessly - the clock is an Animatable
   read only inside the draw lambda, so a replay is draw-only invalidation.

4. Immersive (Apple Music) style lyrics: the page now gives itself the whole
   screen once you stop touching it - the compact header, the provider caption,
   the floating buttons and the transport cluster all slide away after 3 s of no
   interaction (any tap or scroll brings them back). The fullscreen button in
   that tab enters this same state instead of handing over to the shared
   (classic) fullscreen lyrics sheet.

5. Lyrics motion, further: the two scroll helpers jumped instantly to an
   off-screen target line (a far tap, a long interlude) - the one visibly steppy
   moment left in the sheet. Both now animate to it, with the existing centering
   tween finishing the move.

Audit: brace/paren balance across all 1363 Kotlin files, all XML parsed, TOML
parsed, project import resolution identical to the v0.5.211 baseline.

## Muso playlist loading fix — 2026-10-06
- Fixed online playlist taps that could end in the misleading `Error: Empty response` snackbar/screen.
- Normalized `VL`/`vl` playlist IDs before loading.
- Added a YouTube Music queue/`next` endpoint fallback when the strict playlist parser fails.
- Fallback playlists still populate title, thumbnail, artist label, tracks and continuation so the playlist page remains usable.
- Replaced the generic `Empty response` fallback text with a meaningful retry message when both online and local loading fail.

## Lyrics international-script rendering fix

- Added `LyricsFontUtils` with script-aware font selection for lyric text.
- Latin-only lyrics continue using the selected Muso UI font.
- Any non-Latin lyric line/word now uses Android `SansSerif` platform fallback so Devanagari, Bengali, Arabic, CJK, Tamil, Telugu, Gurmukhi and other scripts render with complete glyph coverage instead of dotted-circle/missing-mark placeholders.
- Enabled font padding for script-safe lyric text to avoid clipping of Indic/Arabic combining marks.
- Applied the fix to Classic lyrics, Rich Sync word-by-word lyrics, Apple Music lyrics, and Media3 video lyric subtitles.

## Lyrics animation synchronization fix (2026-10-06)
- Fixed rich-sync word end timing: when providers supply word start timestamps only, each word now ends at the next word's start instead of the entire line end.
- Fixed grapheme segmentation for letter-by-letter animation using Unicode character boundaries, preventing Devanagari/Arabic combining marks and emoji sequences from being split incorrectly.
- Character animation now remains inside the exact word timing window, keeping word-by-word and letter-by-letter sweeps synchronized with the playback clock.
- Preserved frame-smooth player-position interpolation for the unified Echo lyrics renderer.
