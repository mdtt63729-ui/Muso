# Round 183 (v0.5.200, code 207) — CI round 182 build errors fixed

The first CI run of the round-182 lyrics work failed with 8 errors in
LyricsView.kt (paste-1-36):

- 968: unresolved 'mutableIntStateOf' (which also cascaded into the
  currentLineIndex comparison errors at 560/620/847). The round-180
  playback-state refactor used it without importing it - import added.
- 536: lyricsOffsetMs passed as Int where the new playback-state builder
  takes Long. Converted at the call site.
- 767/770/771: the round-182 single-word synthesized ParsedRichSyncLine fed
  the line model's STRING timestamps into Long fields. Converted with
  toLongOrNull, matching how the rich-sync parser itself converts (missing
  end time = until the next line).

Also swept every Kotlin file for other missing Compose-runtime imports of
this class (mutable*StateOf / rememberSaveable / getValue) - no further real
gaps (the flagged hits resolve via star imports, the saveable package, or
Kotlin's map delegation).
