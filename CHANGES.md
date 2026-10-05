## Muso 0.5.216 — playlist header: artwork is now the dominant element
- On the playlist screen the header artwork was only half the screen tall with the
  page-colour scrim running over its full height, so the white background read as
  more prominent than the thumbnail. The artwork is now 60% of the screen tall and
  the scrim spans only its lower ~70%: the top ~30% of the thumbnail stays
  untouched, the fade into the page is a long, even blend, and the thumbnail is the
  highlighted element instead of the white.

## Muso 0.5.215 — full-flavour build fix (missing Cast / Identity deps)
- The `full` flavour's Kotlin did not resolve: its cast player and Google-Drive
  backup import `androidx.media3.cast.*`, `com.google.android.gms.cast.*` and
  `com.google.android.gms.auth.api.identity.*`, but no dependency provided them.
  Added `androidx.media3:media3-cast` (media3 1.11.1), `play-services-auth` 21.5.1
  and `play-services-cast-framework` 21.5.0 to `fullImplementation`.
  (The `foss` flavour — the one CI builds and ships — was never affected.)
- Deeper re-audit pass: every declared dependency version verified to exist in
  Maven Central / Google Maven / the Gradle Plugin Portal; aapt-level resource
  checks (string escaping, format args, resource-name validity, locale qualifiers,
  style parents); manifest `android:exported` on every component; Hilt/Room
  annotation correctness; duplicate top-level declarations; BuildConfig field
  usage; flavour source-set compatibility.

## Muso 0.5.214 — full project re-audit + updater asset fix
- In-app updater now selects the release's **APK** asset by name instead of blindly
  taking `assets[0]`; a release that also carries a `-source.zip` (or any other
  asset) no longer makes the DownloadManager fetch a non-installable file.
- Full A-Z re-audit of every file: version catalog, all `libs.*` / `projects.*`
  references, every internal import, all Android resource references (Kotlin `R.*`
  and XML `@type/name`), the merged manifest's classes, per-module duplicate
  types/resources, `BuildConfig` field usages, flavor (foss/full) source-set
  compatibility, and a syntax parse of all 1340 Kotlin files.

## Muso 0.5.213 — persistent in-app update prompt + build fixes
- The in-app update popup now re-appears on every app launch / foreground until the
  app is actually updated to the latest GitHub release, instead of being suppressed
  permanently for a version the user dismissed with "Later".
- Update detection is now semver-aware, so a release tag carrying a suffix
  (e.g. "v0.5.213-source") still matches the installed version and the popup
  stops correctly once the app has been updated.
- Fixed a version-catalog typo: `compose-foundation` used a literal
  `version = "compose"` instead of `version.ref = "compose"`, which broke
  dependency resolution for `androidx.compose.foundation:foundation`.

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
