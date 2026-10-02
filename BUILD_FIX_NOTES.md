# Build Fix Notes — 2026-10-02

This patch addresses the Kotlin compilation errors reported by GitHub Actions during `:app:compileFossReleaseKotlin`.

Fixed source-level issues:
- Added missing Compose `@Composable` and `Dp` imports in `MainActivity.kt`.
- Added missing `fillMaxHeight` import and removed delegated-property smart-cast usage in `Items.kt`.
- Fixed `MusoNavbarHost.kt` scope error where `glassOn` was referenced outside the `Crossfade` lambda.
- Added missing `size` import in `SplashScreen.kt`.
- Added the missing `androidx.datastore.core.data` import in Muso and ArchiveTune DataStore helpers.
- Added missing DataStore preference-key imports and `CustomThemeColorKey` import in ArchiveTune `DataStore.kt`.
- Fixed nullable `FormatEntity` access in `LibraryViewModels.kt` and `PodcastViewModels.kt`.
- Added `withStyle` import in `EchoLyricsStyles.kt`.
- Fixed the `LazyListScope.item` implicit-receiver issue around the lyrics footer in `LyricsView.kt`.
- Reworked lyric fallback weight calculation to avoid unsupported `IntStream.sumOf` / `Int.toChar()` usage.

Verification limitation:
- The local environment cannot download Gradle 9.5.1 because network access to services.gradle.org is unavailable.
- Therefore the final `assembleFossRelease` must be verified in GitHub Actions.
