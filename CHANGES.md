# Round 182 (v0.5.199, code 206) — classic lyrics styles for line-synced lyrics, Lyrics mode removed

User request (ArchiveTune lyrics styles package):
1. The word-by-word style picker (Settings -> Player and audio, lyrics
   section) now drives LINE-synced lyrics too. A line-synced line has no
   word timings, so the whole line is synthesized as one word spanning the
   line's own start/end - the same approach as ArchiveTune's classic
   renderers. KARAOKE fills across the whole line; FADE/GLOW/SLIDE/APPLE
   and the other Echo styles animate the line as a unit. FLARE (needs real
   word timing) and NONE (static look) keep the classic LyricsLineItem.
   RICH_SYNCED lyrics are unchanged.
2. "Lyrics mode" is REMOVED from settings (kit LyricsSettings): the engine
   switch row, the V2/Enhanced dialog, and the V2-only animation-tuning
   entry are gone. Word-by-word is the only lyrics engine; its switch lives
   inside the word-by-word style dialog. LyricsView no longer reads the
   lyrics-mode flag, so no one can get stuck on the static look.

Full-source audit after the edits: brace/paren balance on all 1362 Kotlin
files, all XML parsed, project-import resolution identical to the v0.5.198
baseline (no new unresolved), SimpIcons imports complete, R.string clean.
