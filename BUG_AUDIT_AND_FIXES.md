# Muso v0.5.218 — Bug Audit & Performance Fixes

## Fixed in this pass

### 1. Home/History/etc. floating action button animation
- The `HideOnScrollFAB` used a vertical slide, so the button entered from the bottom.
- It now enters from the right edge with a spring + scale bounce and a short fade.
- Exit also returns smoothly to the right instead of dropping downward.
- Existing bottom/end window-inset positioning is preserved, so the button remains in the same mini-player-adjacent area.

### 2. Liquid Glass playback jank
The playback path was causing unnecessary UI work while music was playing:
- `MiniPlayer` had three parallel collectors duplicating `nowPlaying`, controller, and timeline state.
- The Android mini-player's playback clock was part of the same composition scope as its Liquid Glass backdrop.
- The shared timeline was being published every 250 ms (4 updates/second), which is unnecessarily frequent for compact playback UI.
- The Classic mini-player used a 100 ms position loop.

Fixes:
- Removed the duplicate collectors and read stable track/controller state directly from the existing StateFlows.
- Moved Android mini-player playback progress/loading into a dedicated child composable. The Liquid Glass surface remains a sibling outside that rapidly changing composition scope.
- Reduced the shared playback snapshot interval from 250 ms to 500 ms. Frame-accurate lyric/playhead code continues to read the real player directly where needed.
- Reduced the Classic mini-player polling interval from 100 ms to 250 ms.
- Reduced the Liquid Glass backdrop blur radius to 80% of the previous value while keeping the same material/shader pipeline, reducing GPU work when multiple glass surfaces are visible.

## Audit scope
- Navigation / bottom bar / floating action button paths
- Mini-player and player-state bridges
- Liquid Glass rendering and interaction primitive
- Playback timeline/state propagation
- Frequent coroutine loops and UI polling in the Muso player path
- Compose state collection around playback UI
- Modified Kotlin delimiter/syntax sanity checks

## Verification
- All modified Kotlin files passed delimiter/balance validation.
- `kotlinc` parsing was run against each modified Kotlin file; no Kotlin parser errors (`expecting`, `unclosed`, etc.) were reported. Full symbol resolution is unavailable outside the Android/Gradle dependency environment.
- Full Gradle build could not be completed in this environment because Gradle 9.5.1 is not cached and `services.gradle.org` is unreachable from the environment.

A real-device/release build should still be run before shipping, especially to validate GPU/frame-time improvements on the target device.
