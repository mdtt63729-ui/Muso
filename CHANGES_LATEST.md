# Latest Now Playing artwork + adaptive foreground fix

- Added a single `NowPlayingContentState.thumbnailURL` resolver that prefers the live screen-state artwork and falls back to the current queue track's highest-resolution original thumbnail.
- Updated all Now Playing styles and Apple Music shared player artwork surfaces to use the resolved thumbnail, preventing styles from showing blank/placeholder artwork when the bridge does not populate `thumbnailURL`.
- Removed the forced `ForceDarkContent` wrapper from ArchiveTune styles. Their adaptive shell can now choose black foreground on light artwork/background and white foreground on dark/coloured artwork/background.
- Updated Immersive Extended foreground to use the same artwork-luminance-aware foreground instead of permanently forcing white.
- Existing mini-player, playlist, lyrics, navigation, and immersive changes from the previous project are retained.

Build verification could not reach Kotlin compilation because Gradle 9.5.1 distribution download requires network access to services.gradle.org, which is unavailable in this environment.
