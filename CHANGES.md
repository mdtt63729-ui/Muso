# Round 174 (v0.5.191, code 198) — ArchiveTune player styles, fullscreen video rewrite, blank-player-at-launch fix

User requests (on top of v0.5.190):

1. ARCHIVETUNE PLAYER STYLES (user request: "add AT's player styles")
   - AT ships ten player designs; Muso had three. All ten now exist:
     Classic (V1, Muso's Spotify style), Expressive (V6, M3), Immersive
     (V7, AppleMusic), plus seven NEW ports in ATPlayerStyles.kt driven by
     the same NowPlayingContentState/Actions contract:
       Modern (V2) - rounded card artwork, left-aligned type
       Minimal (V3) - small art, hairline progress, three-button controls
       Cinematic (V4) - blurred full-bleed backdrop, letterboxed title block
       Little (V5) - tiny artwork, compact centered layout
       Immersive Extended (V8) - dark canvas + codec/explicit/video chips + NEXT UP queue peek
       Material Extended (V9) - M3 tonal buttons + shuffle/repeat FilterChips + huge filled play
       Editorial (V10) - magazine layout, giant display title, offset artwork
   - Every style: artwork pager (swipe = change song), working slider,
     transport controls, like/lyrics/queue/video/more actions, canvas video
     in the artwork frame when a video track plays.
   - Settings row lists all ten (AT's own names).

2. FULLSCREEN VIDEO REWRITE
   - Root cause of "videos don't play properly": the screen reused the
     suite's canvas MediaPlayerView — a 15-second LOOPING segment player.
     The video jumped back to the start every 15s, with double audio and no
     position sync.
   - FullscreenVideoScreen now owns a dedicated player: plays the WHOLE
     video, starts at the service player's position, follows play/pause and
     drift-corrects, MUTED (audio stays with the service stream - single,
     gapless sound), fills the screen edge-to-edge (scale-to-cover, no
     insets, no bars).

3. BLANK FULLSCREEN PLAYER AT APP LAUNCH (fixed)
   - Cause: process death while the fullscreen video route was open left
     that route in the saved NavHost back stack; restoring the app reopened
     a dead video screen - a black, useless fullscreen player.
   - The screen now pops itself out if, after the grace period, there is no
     playable video URL (or no player connection): app launch lands on the
     normal home screen instead.
