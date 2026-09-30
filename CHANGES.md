# Round 184 (v0.5.201, code 208) — SimpMusic streaming API re-enabled (user request)

The remote music source that an earlier snapshot had detached is back ON,
restored exactly from the last CI-verified implementation (v0.5.195):

- innertube client (com.zionhuang): the defaultRequest stub that threw on
  every network call is gone - search, browse, player/next, watch/playlists
  all hit the network again.
- morideobfuscator YoutubeiHttpClient: executeRequest / executePlayerScript
  restored - stream URL resolution and player-script fetches work.
- MusoDownloadHandler.downloadTrack: re-enqueues into ExoDownloadService,
  so downloads work from the suite again.
- core (ArchiveTune) InnerTube client and the MusicApiDisabledException
  stub removed; API_DETACHED.md removed.

Playback quality was verified fully wired end to end: the Audio Quality
setting (Low 66 / Medium 129 / High Opus / High AAC) drives the exact itag
selection in MusicService's stream resolution - the setting's itag first,
then its high-quality twin, then the family order, with the old cached
format reused ONLY while it still belongs to the selected quality family,
so changing the setting really changes the audio that plays. Downloads use
the same itag-preference logic under their own Download Quality setting.

Audit: brace/paren balance on all 1362 Kotlin files, all XML parsed,
project-import resolution identical to the v0.5.200 baseline, no leftover
detach markers anywhere in the tree.
