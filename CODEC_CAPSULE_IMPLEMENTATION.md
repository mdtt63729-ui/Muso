# Codec / Bitrate Capsule Update

Implemented the player codec capsule across Muso's Now Playing styles.

## Behaviour
- The capsule is controlled by the single `showCodecOnPlayer` preference.
- Preference OFF: no codec capsule and no loading UI.
- Preference ON: a compact animated loading capsule appears while the current track's resolved format is unavailable.
- Once resolved, the capsule smoothly reveals the real stream container, codec and bitrate.
- Example: `WEBM • OPUS • 129 kbps` or `MP4 • AAC • 256 kbps`.
- Bitrate is taken from the resolved `FormatEntity.bitrate`; no nominal itag quality is substituted.
- The capsule is shared across Spotify, Material 3 Expressive, Apple Music and the ArchiveTune/Muso player styles, including the immersive/material/editorial variants.
- The setting key was unified so the player and settings screen no longer read different preference keys.

## Build verification
The source was checked for Kotlin parser-level errors with `kotlinc`; no `expecting`, `unclosed`, `missing`, or syntax errors were reported. A full Gradle build could not be completed in this environment because the Gradle 9.5.1 distribution is not cached and `services.gradle.org` is unreachable.

## Preference-store synchronization fix (2026-10-06)

The codec toggle was using the same `ShowCodecOnPlayerKey` name in two different
DataStore implementations: Muso's `settings` store and ArchiveTune's separate
`archivetune_settings` store. That made the toggle appear enabled in Settings while
ArchiveTune-backed Now Playing styles could read a different value.

All codec-toggle reads/writes now use `com.muso.music.utils.rememberPreference`,
which is backed by Muso's single `settings` DataStore. ArchiveTune's other preferences
remain on its own store; only the shared codec preference was unified.
