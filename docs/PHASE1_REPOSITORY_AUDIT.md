# Muso — Phase 1: Repository Audit

PRD §63, Phase 1. Snapshot: **0.5.223**. Method: static analysis only (package/file
enumeration, symbol and import tracing, instantiation-site search, manifest
cross-check, file-size measurement). No build was run; line counts and call sites
are exact, runtime reachability is inferred and marked as such.

1,057 Kotlin files, 224,969 lines, one Gradle module (`app`).

---

## Executive summary

One structural cause sits behind most of the PRD's symptoms: **the app ships three
application layers inside a single Gradle module**, and two of them own the same
concerns. Playback, lyrics, the mini-player, settings and downloads each exist two
or three times. Performance work that does not first collapse this duplication will
be spent on code paths the running app may never execute.

- **One registered *audio* player owner, plus separate video players (corrected in Phase 6 — see below).** The manifest registers only
  `com.muso.music.playback.MusicService`. The ArchiveTune layer carries a second
  complete media service (8,681 lines) that is not registered, yet is referenced
  throughout its own layer.
- **The UI is duplicated.** Two mini-players (185 and 1,524 lines), four lyric
  renderers (646 / 1,255 / 1,669 / 2,677 lines), two preference-key files (639 and
  940 lines), two settings screen trees.
- **The layers are entangled, not stacked.** Muso imports ArchiveTune 83 times;
  ArchiveTune imports Muso 173 times. Neither is removable in isolation.
- **Monoliths drive the recomposition and jank risk.** Five files exceed 1,500
  lines; `MusicService` alone publishes more than twenty observable flows.

**Recommendation.** Phase 2 should instrument the live path only. Phase 3 must
decide the fate of the second media service before any playback refactor: the
"single player owner" requirement is already satisfied in the manifest and violated
in the source tree.

---

## 1. Layer map

| Layer | Package root | DI | Files | Lines |
|---|---|---|---|---|
| Muso core | `com.muso.music` | Hilt | 215 | 38,469 |
| SimpMusic suite | `com.maxrave.*` | Koin | 386 | 48,905 |
| ArchiveTune / KIT | `moe.rukamori.archivetune` | Hilt + Koin | 449 | 136,821 |
| Muso suite bridge | `com.muso.music.suite` | Koin | 6 | 749 |

ArchiveTune is 61% of the codebase. The bridge is what lets the two DI frameworks
coexist: `App.kt` publishes Hilt singletons into the Koin graph the suite reads.

## 2. Playback dependency graph

| Component | File (lines) | Role |
|---|---|---|
| MusicService | `com.muso.music.playback` (1,499) | `MediaLibraryService` + `Player.Listener`. Owns the **audio** ExoPlayer, built at line 314. Registered in the manifest. |
| PlayerConnection | `com.muso.music.playback` (199) | Wraps the service player into StateFlows. |
| SuitePlayerRegistry | `com.muso.music.suite` | `@Volatile` global holding the service player; Koin `single<Player>` "mainPlayer" reads it (App.kt 110-117). |
| Queues | `com.muso.music.playback.queues` (6 files) | Empty/List/LocalAlbumRadio/YouTubeAlbumRadio/YouTubeQueue. |
| ArchiveTune MusicService | `moe.rukamori.archivetune.playback` (8,681) | `@AndroidEntryPoint`; injects ResolveAudioStreamUseCase, EqualizerPlaybackController, SponsorBlockPlaybackController, NextStreamPreloader; builds its own player near line 1,154. **Not registered in the manifest.** |

The manifest registers `.playback.MusicService` under namespace `com.muso.music` —
the 1,499-line Muso service. The ArchiveTune service is started only from within its
own layer (`ArchiveTuneMediaNotificationProvider`, widget actions, MusicTogether,
BackupRestore), all of which appear unreachable from the live navigation graph.
Phase 3 must confirm this on a build; if confirmed, ~8,700 lines of player code are
dead.

## 3. Lyrics dependency graph

| Component | File (lines) | Role |
|---|---|---|
| LyricsProviderRegistry | `com.muso.music.lyrics` | Nine providers; order persisted in `LyricsProviderOrderKey`. |
| LyricsHelper | `com.muso.music.lyrics` (105) | Queries all enabled providers in parallel, first non-blank wins; LruCache 3 result-sets / 128 lyrics. |
| LyricsView | `com.maxrave.simpmusic.ui.component` (1,669) | Primary lyric surface. |
| FullscreenLyricsContent | `com.maxrave.simpmusic.ui.component` (1,255) | Fullscreen overlay. |
| AppleMusicLyrics | `com.muso.music.ui.component` (646) | Third renderer. |
| ArchiveTune Lyrics | `moe.rukamori.archivetune.ui.component` (2,677) | Fourth renderer (karaoke). |

Four renderers, one data source. Phase 5 is chiefly about consolidating them.

## 4. Download dependency graph

| Component | File (lines) | Role |
|---|---|---|
| DownloadHandler (interface) | `com.maxrave.domain.mediaservice.handler` (47) | Contract the suite consumes. |
| MusoDownloadHandler | `com.muso.music.suite` (85) | Sole implementation; bound in App.kt line 119. |
| DownloadUtil / ExoDownloadService | `com.muso.music.playback` | Muso download plumbing. |
| ResumingDownloader / ExternalDownloaderLauncher | `moe.rukamori.archivetune` | ArchiveTune paths; reachability unconfirmed. |
| DownloadedArtworkRepository | `moe.rukamori.archivetune.downloads` | Used by the live Muso DownloadUtil. |

The cleanest of the six graphs. Phase 8 is additive, not de-duplicating.

## 5. Settings dependency graph

| Component | File (lines) | Role |
|---|---|---|
| PreferenceKeys (Muso) | `com.muso.music.constants` (639) | Keys the Muso screens read/write. |
| PreferenceKeys (ArchiveTune) | `moe.rukamori.archivetune.constants` (940) | Second key set; also holds the SliderStyle / mini-player / navigation constants the Muso mini-player imports. |
| MusoSettingsSections | `com.muso.music.ui.screens.settings` (1,725) | Single-file settings monolith. |
| Settings screens | `com.muso.music.ui.screens.settings` (10 files) | Account, AudioEffects, AI, Backup, Discord, Spotify, ListeningHistory, About. |
| ArchiveTune settings | `moe.rukamori.archivetune.ui.screens.settings` | Reached via NavigationBuilder → KitSettingsHost. |

The duplicate is not two rows of one toggle — it is **two key namespaces plus two
screen trees** reached from the same navigation host.

## 6. Player-style graph

| Component | File (lines) | Role |
|---|---|---|
| MiniPlayerStyle | `com.muso.music.constants` | Enum: MINIFY, CLASSIC. Persisted under `MiniPlayerStyleKey`; unknown values fall back to MINIFY. |
| MusoNavbarHost | `com.muso.music.ui.player` (492) | Selects style and height; routes between three renderers. |
| MiniPlayer (Muso) | `com.muso.music.ui.player` (185) | MINIFY renderer. |
| MusoClassicMiniPlayer | `com.muso.music.ui.player.classic` | CLASSIC renderer. |
| MiniPlayer (suite) | `com.maxrave.simpmusic.ui.screen` (1,524) | SimpMusic mini-player, still selectable. |

The enum is clean after 0.5.223, but three renderers remain and the host still
branches between them.

## 7. Navigation graph

| Component | File (lines) | Role |
|---|---|---|
| NavigationBuilder | `com.muso.music.ui.screens` (469) | ~40 composable routes incl. the settings subtree and KitSettingsHost wrappers. |
| Typed destinations | `com.maxrave.simpmusic.ui.navigation.destination` (~30 files) | String-route destinations. |
| NavigationTransitions | `com.muso.music.ui.navigation` | Shared enter/exit animation specs. |
| Bottom bars | `com.maxrave.simpmusic.ui.component` | AppBottomNavigationBar and LiquidGlassAppBottomNavigationBar. |
| MusoNavbarHost | `com.muso.music.ui.player` (492) | Hosts the bottom bar + mini-player and coordinates heights. |

Two route systems sharing one NavHost is why navigation/animation jank is hard to
attribute: a measurement must first say which route system it traversed.

## 8. Cross-cutting findings

1. Three layers, one module — the common ancestor of the playback, recomposition,
   settings and mini-player defects.
2. Playback has a shadow owner (the 8,681-line second service).
3. Five monoliths exceed 1,500 lines: ArchiveTune MusicService (8,681),
   PlayerComponents (5,140), ModalBottomSheet (3,340), ArchiveTune Lyrics (2,677),
   ArchiveTune Player (2,576) — plus MusoSettingsSections (1,725), LyricsView
   (1,669), MainActivity (1,558).
4. Two preference sources make "a canonical setting" ambiguous today.
5. The bridge is a volatile global (`SuitePlayerRegistry`) read lazily by Koin; any
   code asking for "mainPlayer" before the service starts throws.

> **Correction (Phase 6).** This audit originally reported a single `ExoPlayer.Builder`
> site. That was wrong: the search missed builder calls written across two lines.
> There are **six** creation sites — the audio player, a fullscreen-video player, a
> canvas player, and three inside the ArchiveTune layer. See
> `PHASE6_FULLSCREEN_PLAYER.md` for the corrected map.

## 9. Handoff to Phase 2

Instrument only the live path: Muso `MusicService` main-thread work, `PlayerConnection`'s
StateFlow fan-out, `MusoNavbarHost` for mini-player recomposition, and the SimpMusic
now-playing screen for lyric and artwork recomposition. Every hotspot must name the
layer it lives in.
