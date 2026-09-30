# Round 193 (v0.5.207) — offline downloads from cache, working Endless queue, All-search fix, ultra-fast loading

User request: (1) cached songs must be addable to Downloads with the network fully off,
(2) the Endless queue switch in the queue section did nothing, (3) songs/lyrics/search
must load ultra fast, (4) search filter "All" returned nothing (Songs worked).

All-search fix (the video's bug):
- Root cause, verified against a LIVE unfiltered music.youtube.com search response: the
  top-result musicCardShelfRenderer no longer carries `header` (or `contents`). The
  model declared `header` as REQUIRED, so kotlinx.serialization failed the ENTIRE
  response deserialization; searchSummary returned a failure, summaryPage stayed null
  and the All tab sat on an endless skeleton while the filtered searches (no card)
  worked.
- MusicCardShelfRenderer: header/contents/buttons made optional; YouTube.searchSummary
  reads the header safely and labels the card section "Top result" (the new card shape
  is parsed by the existing fromMusicCardShelfRenderer - title/subtitle/thumbnail/onTap
  all verified present in the live response).
- OnlineSearchViewModel: on failure the All tab now retries once (1.5s) and then shows
  "no results" instead of an infinite skeleton.

Offline downloads from cache:
- The download resolver's cache check was a 1-byte probe, which (a) let partially
  cached songs fall off the cached span onto a bogus internal URI and fail, and
  (b) was the wrong check for offline use. It now requires the FULL remaining content
  to be cached: a fully cached song resolves without any network request and the
  download runs entirely from the player cache into the download cache - the
  download button works with the network completely off (verified against media3
  1.11.1's DownloadManager data-source chain).

Endless queue (real now):
- The suite's DataStoreManager was a no-op stub: the switch wrote nothing and its
  flow stayed "false" forever. It is now backed by the app's DataStore
  (EndlessQueueKey) and constructed with the app context in Koin.
- MusicService honors it: when fewer than 3 songs remain and the queue has no more
  pages, a radio tail of similar songs is fetched (YouTube.next) and appended
  (deduped against everything ever queued in the session); playback never stops.
  Appended ids reset when playback goes idle or the switch is turned off.

Ultra-fast loading:
- Lyrics: all enabled providers are now queried IN PARALLEL (was: sequential, waiting
  for each failing one); results are taken in the user's priority order, plus a
  128-entry in-memory cache by song id.
- Song starts: next-song audio preload (3 MB) and next-song lyrics preload are ON by
  default now.
- Search: instant-results debounce 450 ms -> 300 ms; the All tab is one request again.

Audit: all 10 modified files parse clean (tree-sitter Kotlin); full-project scan still
shows only the same 5 pre-existing grammar false positives (none touched); media3
1.11.1 / coil 2.6.0 API signatures verified; all new symbols resolve (imports added:
EndlessQueueKey, delay, async/coroutineScope/Dispatchers, edit/dataStore).

# Round 192 (v0.5.207) — offline artwork + offline lyrics for downloaded AND cached songs

User request: songs that get downloaded or cached should also save their thumbnail in high
quality (available with no network) and cache their lyrics so they load and play offline.

Thumbnails:
- DownloadUtil now saves each song's artwork when a download completes, when a song is
  fully pulled into the player cache (cacheSong), and as a one-time backfill at app start
  for every already-downloaded / already-fully-cached song. The song's album art URL is
  registered as an extra alias so offline album grids resolve too.
- DownloadedArtworkRepository upgrades every source URL to the best quality before
  saving (i.ytimg.com -> maxresdefault then hq720; googleusercontent/ggpht =w###-h### /
  =s### -> 2160px then 1200px, original kept as fallback) and exposes
  findDownloadedArtwork() for image loaders.
- App's Coil 2 image loader (the main UI) now installs a Mapper that resolves song
  thumbnail URLs to the saved artwork file BEFORE any network attempt - downloaded and
  cached songs show their thumbnail with zero connectivity. The same mapper + a Coil 3
  port of the high-res interceptor are installed on the coil3 singleton used by the
  embedded SimpMusic player suite.
- Artwork is pruned when a download is removed AND the song is no longer fully cached;
  at every app start artwork is retained for downloads + fully player-cached songs only.

Lyrics:
- Downloads and fully-cached songs get their lyrics fetched and stored in the local DB
  (they previously only traveled with a download, not with the streaming cache).
- Offline bug fix: the per-song lyrics fetch used to write LYRICS_NOT_FOUND while offline,
  permanently marking songs lyric-less. It now skips fetching entirely without a
  connection, and stale NOT_FOUND rows are retried (at most once per 24h) once online -
  with retry timestamps kept in memory so the Room schema stays untouched.

Audit: all 5 modified files parse clean (tree-sitter Kotlin); brace/paren balance across
the modified files verified; coil 2.6.0 / coil3 3.6.3 API usage verified against the
exact library sources; Hilt graph (new constructor dependency + entry-point accessor)
checked for cycles; no other callers of the changed signatures.

# Round 191 (v0.5.207, code 214) — canvas video: no black screen on mini->fullscreen, smooth sheet transitions

User report: with a video (canvas) playing in the player, expanding from the mini player
to the fullscreen player turned the screen black, and the mini <-> fullscreen transition
animation lagged whenever the fullscreen player was showing video.

Root cause: com.maxrave.media3.ui.MediaPlayerView built its OWN ExoPlayer in remember {}
on every composition and released it on every disposal. Expanding the sheet therefore
(a) constructed a new player mid-animation (heavy main-thread work = the jank),
(b) re-prepared and re-buffered the canvas from zero, leaving the video area black until
its first frame rendered (the video is the full-screen backdrop layer, so the whole
screen read as black), and (c) on collapse, ExoPlayer.release() ran on the animation
frames (the lag in the other direction).

Fix: CanvasVideoController - ONE session-scoped canvas player. Attaching a surface binds
it to the already-prepared player (same URL => play(); the presentation state knows the
video size immediately, so the black cover shutter no longer trips); detaching the last
surface only PAUSES it, keeping it prepared and positioned in the canvas cache. New song
URLs swap the media item and restart the single loop controller (previously a per-view
polling effect, now one loop per URL app-wide). All five canvas call sites (Spotify /
Expressive cards / Apple Music backdrops + the AT artwork pager/static art) go through
this automatically. MediaPlayerViewWithSubtitle (music-video fullscreen) is untouched -
it binds the shared playback player as before.

Audit: brace/paren balance across all 1362 Kotlin files, all XML parsed.
