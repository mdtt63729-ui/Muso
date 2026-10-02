# Muso Settings Audit — v0.5.212

## Fixed

1. Unified Muso and ArchiveTune settings onto one canonical DataStore: `archivetune_settings`.
2. Muso's old `settings` DataStore is now treated as a legacy store and merged once at startup.
3. Started `PreferenceStore` during `Application.onCreate()` so imperative consumers see live preference updates instead of falling back to defaults before the cache starts.
4. Removed the second active `preferencesDataStore` implementation from Muso's DataStore helper; it now delegates to the canonical store.
5. Fixed critical raw-key type collisions:
   - `lyricsTextSize`: unified as Muso Int preference.
   - `crossfadeDuration`: Muso Int remains canonical; ArchiveTune-only legacy float key migrated to the Muso Int key.
   - `customThemeColor`: Muso Int remains canonical; ArchiveTune palette data now uses a separate `archiveTuneCustomThemeColor` key and old values are migrated.
6. Added migration for old ArchiveTune float `lyricsTextSize` values.
7. Existing ArchiveTune `historyDuration` float→int migration remains isolated and intentional.
8. Removed the blocking initial preference read from Muso Compose `rememberPreference`/`rememberEnumPreference`; state now starts from the default and immediately follows the DataStore flow.

## Static audit findings

The settings UI contains several Muso controls that write a preference but have no active consumer in the current source tree:

- `AnimatedArtworkKey`
- `CropAlbumArtKey`
- `HidePlayerSliderKey`
- `PlayerTextAlignmentKey`
- `RotatingArtworkKey`
- `SeekExtraSecondsKey`
- `ShowCachedPlaylistKey`
- `ShowDownloadedPlaylistKey`
- `ShowLikedPlaylistKey`
- `ShowUploadedPlaylistKey`

These were not silently assigned speculative behavior. They should be wired to the intended player/library behavior in a separate feature-specific implementation pass rather than pretending that persistence equals functionality.

## Verification limitation

The Gradle wrapper requires Gradle 9.5.1 from `services.gradle.org`. This environment cannot resolve that host, so an actual Kotlin/Android compilation could not be completed here. ZIP integrity and static source audits were completed.
