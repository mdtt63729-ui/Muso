# Muso — Phase 3: Playback Architecture

PRD §63, Phase 3. Snapshot: **0.5.225**. The PRD asks for three things: a single
player owner, asynchronous stream resolution, and isolated state publishing. This
document records what was verified, what was fixed, and what is deliberately left
open.

---

## 1. Single player owner — verified, unchanged

**Correction (Phase 6):** this section originally said exactly one `ExoPlayer.Builder`
call exists. That was wrong — the search missed builder calls split across two lines.
There are six creation sites. Exactly one builds the **audio** player:
`playback/MusicService.kt:314`. The rest build video/canvas players (see
`PHASE6_FULLSCREEN_PLAYER.md`). The manifest still registers exactly one media service,
`.playback.MusicService` (namespace `com.muso.music`).

The ownership contract is therefore already correct in the running app:

- `MusicService` (Muso) owns the player.
- `SuitePlayerRegistry.player` exposes it to the SimpMusic suite.
- `App.kt` binds it into Koin as `single<Player>` "mainPlayer", throwing a clear
  error if asked for before the service starts.

No code change was made. The second media service in the ArchiveTune layer
(`moe.rukamori.archivetune.playback.MusicService`, 8,681 lines) is **not registered**
and cannot be started by the system, but it is still referenced throughout its own
layer. Confirming it is dead needs a build (remove its entry points and compile);
until then it is left alone rather than deleted blind.

## 2. Asynchronous stream resolution — verified; one follow-up noted

Stream resolution runs inside `ResolvingDataSource.Factory { dataSpec -> ... }`
lambdas, which ExoPlayer invokes on **its own loading thread**, not the main thread.
The two `runBlocking(Dispatchers.IO)` calls there (`MusicService.kt:1220, 1267-1268`;
`DownloadUtil.kt:312, 378-379`) therefore block the loader thread, not the UI.

They do still serialise resolution and so lengthen playback start. The lambdas are
non-suspend, so removing the bridge means restructuring the resolve into a
pre-resolved URL cache. That is a behaviour-sensitive change and is left as an
explicit follow-up rather than done blind.

## 3. Isolated state publishing — fixed

### 3a. The lyrics pipeline ran network work on the main thread

`PlayerConnection` is constructed with `MainActivity.lifecycleScope`, i.e.
`Dispatchers.Main`. Its `currentLyrics` flow publishes through a `combine` whose
transform called `TranslationHelper.translate(...)` inline — and the FOSS
implementation of that delegates straight to `AITranslator.translate`, which performs
an HTTP request. The request therefore ran **on the main thread**, on every lyric
change while translation was enabled.

Fixed by wrapping the call in `withContext(Dispatchers.IO)` inside the transform, so
the publishing pipeline no longer performs I/O on its collecting scope.

### 3b. The mini-player published position as composition state

`MusoNavbarHost` ticked `position` / `duration` every 250 ms as composition state and
passed them down as plain values, so the whole mini-player subtree recomposed four
times a second. They are now `() -> Long` providers read in the indicator's draw
phase (see Phase 2, H2).

## 4. Remaining in Phase 3

1. Decide the second media service — delete it, or document why it stays.
2. Convert the loader-thread `runBlocking` resolve into a suspend, pre-resolved path.
3. Route the suite's "mainPlayer" lookup through a typed accessor so the volatile
   global is not read directly.

## Verification debt

None of the above is runtime-verified: the sandbox has no Android SDK, so no build or
device run was possible. Each change is static-verified only (tree-sitter parse of
every `.kt` file plus brace/paren/bracket balance on every edited file). Per PRD §62,
none of these may be marked "fixed" until a build and a device run confirm it.
