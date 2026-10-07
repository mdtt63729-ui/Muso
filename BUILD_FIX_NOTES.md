# Muso v0.5.218 — Foss Release Build Fix

The GitHub Actions log for `assembleFossRelease` failed during `:app:compileFossReleaseKotlin`.
The fixes in this version address every compiler diagnostic reported in that log:

- MainActivity: fixed delegated Boolean being accessed with `.value`.
- Muso MusicService: replaced unavailable Media3 `MediaMetadata.isPodcast` access with the podcast media type.
- Muso MusicService: made endless-queue de-duplication set mutable where later code calls `add`.
- ArchiveTune MusicService: made infinite-queue de-duplication set mutable where later code calls `add`.
- MusoNavbarHost: restored missing `Modifier.size` import.
- LyricsFontUtils: replaced Java `IntStream.forEach` non-local return with a Kotlin loop.
- LyricsView: restored missing Compose state/animation and coroutine-delay imports used by fullscreen lyrics animation.
- ModalBottomSheet: restored `CompositionLocalProvider` import.
- MiniPlayer: uses `timelineState.loading` instead of an undefined `loading` variable.
- NowPlayingContentState: restored the Compose state delegate import for `State<Float>`.
- ATPlayerStyles: restored the `Color.compositeOver` extension import.

The codec preference unification and codec capsule changes from the previous project version are retained.

Full Gradle compilation could not be rerun in this environment because Gradle 9.5.1 is not cached and
network access to `services.gradle.org` is unavailable. The source was patched directly against the exact
compiler diagnostics from the supplied GitHub Actions log.
