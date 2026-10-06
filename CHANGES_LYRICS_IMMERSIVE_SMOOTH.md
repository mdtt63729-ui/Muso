# Muso — Lyrics / Immersive Smoothness Fix

## Fixed
- Replaced mobile fullscreen lyrics ModalBottomSheet host with a full-screen Dialog and a single controlled fade/scale transition to remove the double bottom-sheet jump/grow/shrink effect.
- Selecting any concrete word-by-word lyrics animation style now automatically enables the word-by-word engine, so the selected style is actually active.
- Apple visual lyrics no longer bypass the global word-by-word animation style; FADE/KARAOKE/LYRICS_V2/V2_MODE/ENHANCED now render through the same timed renderer.
- Immersive Extended player now determines foreground contrast from the *rendered* artwork surface after its opacity + black scrim, rather than the raw palette seed. This fixes dark text over dark red/orange artwork.
- Immersive Extended now provides an adaptive Material color scheme and LocalForceDarkText so text, icons and Material components consistently switch to black on light rendered backgrounds and white on dark/coloured backgrounds.
- Queue opened from Immersive Extended now forces queue text/icons to white through an adaptive Material scheme and LocalForceDarkText, matching the dark immersive surface.

## Verification
- Gradle compile was attempted with `./gradlew :app:compileDebugKotlin --no-daemon --stacktrace`.
- Compilation could not start because the environment cannot resolve `services.gradle.org` to download Gradle 9.5.1.
