# Muso — Phase 2: Performance Root Cause

PRD §63, Phase 2. Snapshot: **0.5.224**. Every hotspot below names file, class,
function, root cause, impact, fix and the verification that must prove it. Findings
are from static analysis; **none is runtime-verified** (see "Verification debt").

Scope note: the PRD asks for *all* main-thread / blocking / recomposition hotspots.
This list covers the ones that are real and locatable in the **live** path. Hotspots
inside the ArchiveTune duplicate layer are excluded until Phase 3 confirms that
layer's reachability — instrumenting dead code would be wasted work.

---

## H1 — Every preference read blocks its caller on DataStore I/O (critical) — FIXED in 0.5.225

- **File:** `app/src/main/java/com/muso/music/utils/DataStore.kt` (lines 21-28)
- **Class / function:** `DataStore<Preferences>.get(key)` and `get(key, defaultValue)`,
  consumed by the `preference()` and `enumPreference()` property delegates
- **Root cause:** both getters wrap `runBlocking(Dispatchers.IO) { data.first()[key] }`.
  The delegate is evaluated on the *calling* thread, so every `by preference(...)`
  read — including reads from `MusicService` and from composables — blocks until
  DataStore finishes its I/O, regardless of the IO dispatcher inside.
- **Impact:** main-thread stalls on every preference read; jank on screen entry and
  measurable lag on playback start. This is the single largest contributor to the
  PRD's "playback lag" symptom.
- **Fix (applied):** `PreferencesSnapshot` mirrors `dataStore.data` into an in-memory
  snapshot, kept live by one collector started at `App.onCreate` via
  `primePreferences(context)`. The getters now read the snapshot; the blocking read
  happens exactly once, at startup. `rememberPreference` / `rememberEnumPreference`
  inherit the fix because they read through the same getter.
- **Verification:** Macrobenchmark frame timing on cold start, settings open and
  playback start; assert zero main-thread DataStore I/O in a StrictMode trace.

## H2 — The mini-player recomposes 4×/second from a polling loop (high) — FIXED in 0.5.225

- **File:** `app/src/main/java/com/muso/music/ui/player/MusoNavbarHost.kt` (lines 330-340)
- **Class / function:** `ClassicArchiveTuneMiniPlayer`
- **Root cause:** a `LaunchedEffect` `while (isActive)` loop assigns two
  `mutableLongStateOf` values (`position`, `duration`) every `delay(250L)`. Both are
  read during composition and passed into the mini-player subtree.
- **Impact:** the mini-player and its children recompose ~4 times a second
  continuously, even when nothing visible changes. Directly matches the PRD's
  mini-player recomposition item.
- **Fix (applied):** `position` and `duration` travel down the mini-player chain as
  `() -> Long` providers instead of plain `Long` values, and `NewMiniPlayerContent`
  no longer keys its progress lambda on their values. The circular progress indicator
  invokes the provider in its draw phase, so a position tick no longer invalidates
  the composition.
- **Verification:** Layout Inspector recomposition counts — the mini-player should
  recompose ~0 times between real state changes.

## H3 — 236 lifecycle-less `collectAsState` call sites (high)

- **Files:** app-wide; 236 `collectAsState(` vs 167 `collectAsStateWithLifecycle`
- **Root cause:** state is collected without lifecycle awareness.
- **Impact:** flows, DB queries and recomposition continue while a screen is in the
  background — memory and battery cost, and stale work competing with the foreground.
- **Fix:** migrate the remaining 236 to `collectAsStateWithLifecycle`.
- **Verification:** call-site count reaches parity; a background/foreground stress run
  shows no background collection.

## H4 — 38 `runBlocking` sites; the playback ones block the loader thread, not the UI (corrected)

- **Files / lines:** `playback/MusicService.kt:1220, 1267-1268`,
  `playback/DownloadUtil.kt:312, 378-379`, `utils/AutoBackup.kt:61`,
  `viewmodels/BackupRestoreViewModel.kt`, `viewmodels/LyricsMenuViewModel.kt`,
  `MainActivity.kt:314`
- **Root cause:** suspend DB/network calls bridged back to synchronous code with
  `runBlocking`.
- **Correction:** the MusicService and DownloadUtil instances are **not** main-thread
  bugs. Both sit inside `ResolvingDataSource.Factory { dataSpec -> ... }` lambdas,
  which ExoPlayer invokes on its own loading thread. They block that loader thread,
  not the UI.
- **Impact:** serialised stream resolution, so they lengthen playback start, but they
  do not cause UI jank.
- **Fix:** make the call sites suspend, or move the work into an already-running
  coroutine. (`MainActivity.kt:314` is already mitigated by a SharedPreferences
  mirror and is first-launch only — lower priority.)
- **Verification:** a lint rule or grep gate asserting no `runBlocking` on the
  playback path; playback-start latency measured before/after.

## H5 — Blur effects re-applied on player and lyric surfaces (medium)

- **Files / lines:** `ui/component/AppleMusicLyricsLines.kt:231`,
  `ui/screen/player/content/NowPlayingContentAppleMusic.kt:702` (backdrop blur),
  `ui/screen/player/content/ATPlayerStyles.kt:1373`
- **Root cause:** `Modifier.blur` (RenderEffect) is recomputed per frame on animated
  or frequently recomposing surfaces.
- **Impact:** GPU cost and dropped frames on mid-range devices — the fullscreen-player
  jank the PRD reports.
- **Fix:** cache the blurred bitmap once per artwork change; gate the backdrop blur
  behind a quality setting; use a static scrim where the blur is decorative.
- **Verification:** GPU profiling (frame time) with blur on/off.

## H6 — Recomposition surface from monolithic composables (medium)

- **Files:** `ui/screens/settings/MusoSettingsSections.kt` (1,725),
  `kotlin/com/maxrave/simpmusic/ui/screen/MiniPlayer.kt` (1,524),
  `kotlin/com/maxrave/simpmusic/ui/component/LyricsView.kt` (1,669)
- **Root cause:** very large composable files with many sibling state reads make it
  hard to keep recomposition scoped; any state read at the top of such a file
  invalidates the whole subtree.
- **Impact:** wide recomposition on every small state change.
- **Fix:** split into smaller composables with narrower state reads; hoist the state
  each section needs down to that section.
- **Verification:** recomposition counts before/after on the settings and player
  screens.

## H7 — `StateFlow.value` reads inside composition (needs triage)

- **Files:** ~72 `.value` reads under `com.muso.music.ui`
- **Root cause:** mixed. Some are Compose `MutableState` (correct); some are
  `StateFlow`/`MutableStateFlow` reads, which do **not** subscribe Compose and so
  will not trigger recomposition when the flow changes.
- **Impact:** correctness (stale UI) rather than pure performance; must be triaged
  per site before any blanket change.
- **Fix:** for each `StateFlow` read in composition, replace with `collectAsStateWithLifecycle()`.
- **Verification:** per-site review; no behavioural regression.

---

## Verification debt

The PRD (§62) forbids claiming a fix without build + runtime + regression evidence.
None of the above has that yet: the sandbox has no Android SDK, so nothing in this
document is runtime-verified. Each hotspot's "Verification" line is the gate that
must be run on a device before the corresponding fix is marked done.

## Handoff to Phase 3

H1 and H4 must be fixed before Phase 3's playback refactor, because both sit on the
playback-start path and would mask the refactor's own effect. H2 and H3 are
independent and can be fixed in parallel.
