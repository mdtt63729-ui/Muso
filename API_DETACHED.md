# Legacy music API detached

The existing remote music source has been detached from this source snapshot.

Disabled paths:
- Legacy InnerTube search/browse/player/next requests
- Legacy YouTubei stream resolution and player-script network requests
- Legacy remote music download enqueueing

The local database, local playback infrastructure, UI, and lyrics-related providers were left intact so a replacement music provider can be integrated later.

Build verification could not be completed in this environment because the Gradle wrapper attempted to download Gradle 9.5.1 and outbound network access was unavailable.
