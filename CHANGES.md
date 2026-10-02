## Muso 0.5.212 — synced lyrics + live download UI
- Frame-synced letter-by-letter lyrics progression shared by Echo animation styles.
- Downloads filter now keeps active/queued/paused items visible and reveals artwork with real download progress.

# Round 195 (v0.5.212, code 219) — smoother navigation/search, playlist loading indicator for lyrics, home logo, immersive lyrics fullscreen

1. Button-to-button (top-level tab) switch and the page motion felt laggy. The tab
   crossfade was 150 ms, which left the incoming screen's first composition - the
   heaviest frame of the switch - inside a blink; it is now 210 ms (still a
   crossfade, never a slide). The search pane also composes one short beat after
   the bar opens instead of in the same frame, so tapping search no longer fights
   the bar's own open animation (and the online screen's network work no longer
   lands on the tap frame).

2. Lyrics loading now uses the PLAYLIST screen's loading indicator (the M3
   contained loading indicator, CenterLoadingBox) in the fullscreen lyrics sheet
   and in the Spotify / Expressive-cards lyrics sections, in place of the old
   spinners - the previous lyrics loading animation is gone.

3. The home header shows the splash mark as the app logo, left of the "Muso"
   wordmark. It is the same animation (MusoLogoMark, extracted from the splash),
   and tapping it replays the materialize seamlessly - the clock is an Animatable
   read only inside the draw lambda, so a replay is draw-only invalidation.

4. Immersive (Apple Music) style lyrics: the page now gives itself the whole
   screen once you stop touching it - the compact header, the provider caption,
   the floating buttons and the transport cluster all slide away after 3 s of no
   interaction (any tap or scroll brings them back). The fullscreen button in
   that tab enters this same state instead of handing over to the shared
   (classic) fullscreen lyrics sheet.

5. Lyrics motion, further: the two scroll helpers jumped instantly to an
   off-screen target line (a far tap, a long interlude) - the one visibly steppy
   moment left in the sheet. Both now animate to it, with the existing centering
   tween finishing the move.

Audit: brace/paren balance across all 1363 Kotlin files, all XML parsed, TOML
parsed, project import resolution identical to the v0.5.211 baseline.
