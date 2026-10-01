# Round 193 (v0.5.209, code 216) — smooth lyrics + real player codec badge

- Optimized lyrics rendering/scrolling to reduce GPU/composition work and animation contention.
- Player codec capsule now uses the resolved playback format and only displays bitrate when a real bitrate is available.
- Codec capsule can be shown under the player slider for all ArchiveTune player styles when enabled in settings.
- Version bumped from 0.5.208/code 215 to 0.5.209/code 216.

# Round 192 (v0.5.208, code 215) — user's own upgrade, repackaged

The user edited the v0.5.207 tree themselves (offline artwork + lyrics work across
App.kt, DownloadUtil, MusicService, LyricsHelper, DownloadedArtworkRepository,
PreferenceKeys, DataStoreManager, ATPlayerStyles, OnlineSearch* and the innertube
search models). This build takes that tree as-is and only bumps the version:
versionCode 214 -> 215, versionName 0.5.207 -> 0.5.208.

## 0.5.209 — Mini player / onboarding polish
- Removed the onboarding permission-review page from the visible onboarding flow.
- Added four Mini Player Style choices: Minify, Flat, M3 Flex, Classic. M3 Flex uses the Material 3 Flex layout and Classic uses the adapted ArchiveTune Material 3 mini-player implementation.
- Added the new Flat mini-player: Material 3 flat surface, artwork-backed Spotify-like darkened background, persistent album cover, previous/play-pause/next/like controls, and working horizontal swipe navigation.
- Improved mini-player swipe tracking to follow the finger directly and settle once, avoiding per-pointer-event animation lag.
- Improved mini-to-full-player and full-to-mini transitions with a small scale interpolation and a more responsive no-bounce spring.
