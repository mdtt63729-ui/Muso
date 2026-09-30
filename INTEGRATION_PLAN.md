# ArchiveTune Settings + Onboarding Integration — Phase Plan

Source: ArchiveTune-Settings-Kit.zip (394 files) + github.com/rukamori/ArchiveTune
(commit 2f48b81, GPL-3.0). PRD: paste-1-33.md.
Goal: ArchiveTune settings UI + onboarding replace the old muso settings UI,
with our kept settings (player styles, video, mini player, lyrics styles,
About page) integrated in. All settings must APPLY to the app.

## Phase 1 — Foundation drop-in (v0.5.182, Round 165)  [STATUS: this build]
- Kit kotlin sources copied as-is to app/src/main/kotlin/moe/rukamori/archivetune/
- di/ (AppModule minus DatabaseProvider clash, NetworkModule,
  LyricsHelperEntryPoint) + utils/DataStore.kt (renamed datastore file
  "archivetune_settings" so it never collides with muso's "settings" store)
  copied from the full repo.
- Room schemas: app/schemas/moe.rukamori.archivetune.db.InternalDatabase/
- Variant files: gms->app/src/full/kotlin, foss->app/src/foss/kotlin,
  automotive parked in app/src/automotive (no automotive source set).
- R fix: `import moe.rukamori.archivetune.R` -> `import com.muso.music.R`.
- Resources: kit drawables (overwrite same-name), poppins font,
  strings dedup-merged into values/archivetune_settings_strings.xml,
  styles -> values/archivetune_styles.xml, values.xml drawables dedup.
- Gradle: + translator (jitpack), markwon x8, accompanist-lyrics ui/core,
  androidsvg.
- GATE: CI must compile this before Phase 2 proceeds.

## Phase 2 — Navigation + hub wiring (v0.5.183, Round 166)  [STATUS: done]
- Kit route block (settings, ~28 routes) swapped into muso's
  navigationBuilder, fully-qualified so old muso screens still compile.
- settings/about -> OUR AboutScreen (kept, user spec).
- settings/account -> muso's LoginScreen (kit AccountSettings needs kit
  HomeViewModel; Phase 5 revisits).
- BuildConfig shim object at moe.rukamori.archivetune.BuildConfig (forwards
  VERSION/DISTRIBUTION from com.muso.music.BuildConfig; safe defaults for
  updater/discord/lastfm/together secrets).
- Old-only routes kept until Phase 3: audio_effects, listening_history, ai,
  spotify, spotify_login, discord/login.

- Copy the settings route block from _wiring_reference/NavigationBuilder.kt
  into our navigationBuilder (settings, settings/appearance, ... ~25 routes).
- Navbar settings entry -> new kit SettingsScreen hub.
- settings/about -> OUR AboutScreen (Manik page) kept.

## Phase 3 — Old settings রিমুভ + ইন্টিগ্রেট  [STATUS: done, v0.5.184]
- MusoSettingsSections.kt (com.muso.music.ui.screens.settings): every kept
  muso setting as PreferenceGroupScope extensions rendered with the KIT's
  Preference UI (kit components, muso DataStore keys — so every row really
  applies): theme (color/dark/pure black/liquid glass x2/high refresh),
  player style (RENAMED: Classic V2 / M3 Expressive / Immersive Nightly),
  mini player, slider (live PlayerSliderByStyle preview dialog), buttons,
  background, codec, hide slider/thumbnail, crop, rotating artwork, video
  (quality + show in player), layout (open tab/grid/auto playlists),
  streaming + download quality (itags), 15 audio-behaviour rows, queue
  rows, misc player rows, full lyrics rows (style, word-by-word, size,
  spacing, position, auto scroll, blur, romanize, offset), 7 lyrics
  providers + preferred provider, content rows (languages, country — with
  live innertube locale apply, account, auto-download, explicit, proxy),
  storage cache sizes, privacy rows, integration links (Spotify/Discord/
  AI/History), muso backup link.
- Kit screens edited (marked /* muso-integration: ... */): Appearance,
  Player, Lyrics, Content, Storage, Privacy, Integration, BackupAndRestore —
  overlapping kit rows commented out, muso-backed rows inserted; dead
  duplicate theme rows (palette/icon/fonts/blur/AOD/swipe) pruned.
- Deleted old muso screens: SettingsScreen, AppearanceSettings,
  ContentSettings, PlayerSettings, StorageSettings, PrivacySettings.
  Kept as muso features (linked from kit UI): AboutScreen, AudioEffects
  (settings/audio_effects), BackupAndRestore (settings/muso_backup),
  DiscordSettings (settings/muso_discord), SpotifySettings, AISettings,
  ListeningHistorySettings, login screens.
- Enums DarkMode/NavigationTab/PlayerTextAlignment + THEME_COLORS moved
  from deleted AppearanceSettings into MusoSettingsSections.kt (same
  package, so MainActivity/Player imports keep resolving).

## Phase 4 — Onboarding splash-এর পরে  [STATUS: done, v0.5.184]
- Kit OnboardingRoute + OnboardingViewModel wired in MainActivity inside
  BoxWithConstraints (navController in scope), overlaying the main UI
  right after the splash hands off; completion stored in the kit DB
  (shows once per install); login page reuses muso's "login" route.

## Phase 5 — Apply-mapping  [STATUS: done, v0.5.184]
- Strategy: instead of a kit→muso DataStore mapper, every overlapping
  setting is rendered as a muso-backed row inside the kit UI (Option B) —
  the row writes the muso key the app already reads, so application is
  immediate and exact. Content language/country additionally apply the
  innertube locale live (same side-effect the kit row had).
- Kit-DB-backed screens (stats, hidden playlists) remain kit-DB-backed
  and empty until a future data migration — known limitation.

## Phase 6 — Polish + ফাইনাল verify  [STATUS: done, v0.5.184]
- Brace/paren balance checked on every edited file; icon + string
  existence verified; all navigation routes audited.
 — Old settings removal + kept settings integration
- Remove old muso settings screens + routes; keep our About page.
- Integrate into kit UI: player style (RENAME: classic->"Classic V2",
  expressive->"M3 Expressive", immersive->"Immersive Nightly"), mini player
  style, slider style, word-by-word + suite lyrics settings, video settings,
  quality pickers, codec badge toggle.

## Phase 4 — Onboarding after splash (MainActivity gate).

## Phase 5 — Apply mapping (kit keys <-> muso behavior, DB-backed screens).

## Phase 6 — Polish + full verification.

## Round 168 addendum — kit completeness + logging (v0.5.185)
- 21 safe skipped kit files added; 10 strings/plurals merged; 12 kit files
  unaddable (deps outside the kit zip — documented in CHANGES Round 168).
- MusoLog (com.muso.music.utils.MusoLog): Muso folder with crash_log.txt,
  crash_log_N.txt (one per crash), main.txt (full run logcat); App.onCreate
  init; one-time All Files Access prompt in MainActivity; manifest perms.

## Round 169 addendum — startup immunity (v0.5.186)
- Kit CompositionLocals moved OUT of MainActivity's startup provider into
  moe.rukamori.archivetune.KitSettingsHost, wrapping all 28 kit routes in
  NavigationBuilder. Kit DB/SyncUtils/DownloadUtil construct lazily on
  settings open; failures render a fallback screen, never a startup crash.
- R8 blanket keep for the kit in proguard-rules.pro.
- MusoLog: crash files mirrored to the app-external Muso folder; crash
  dialog share button sends full log files via FileProvider
  (com.muso.music.fileprovider, res/xml/file_paths.xml).

## Round 170 addendum — startup hardening II (v0.5.187)
- Onboarding: no longer composed during startup; MainActivity injects kit
  OnboardingRepository and reads the flag once (guarded) after the main UI
  renders, then overlays OnboardingRoute.
- MusoLog: crash entries + logcat tail + app-start heartbeats are mirrored to
  Downloads/Muso via MediaStore (permission-free, visible in any file
  manager). app_log.txt heartbeat proves whether App.onCreate runs.

## Round 171 addendum — ktor alignment (v0.5.188) - THE startup crash fix
- Root cause: translator (jitpack) pulls ktor-client-core 3.0.1; Gradle
  newest-wins made core 3.0.1 + okhttp engine 2.3.12; ktor 3 removed
  io.ktor.client.plugins.HttpTimeout -> NoClassDefFoundError at every launch
  (Updater object's HttpClient in App.onCreate).
- Fix: allprojects resolutionStrategy forces every io.ktor:* artifact to
  2.3.12; Updater's client is lazy. Kit screens/DI/R8 keeps from earlier
  rounds unchanged.
- LESSON: when adding a library that transitively bumps a shared dependency
  (especially ktor 2->3), check the resolved versions - Gradle picks the
  newest, not the direct one.

## Round 172 addendum — ktor 2.x API adaptation (v0.5.189)
- TogetherClient/TogetherOnlineHost WebSockets install: the ktor-3 property
  name -> ktor-2 `pingInterval`, value unchanged (25_000 ms).
