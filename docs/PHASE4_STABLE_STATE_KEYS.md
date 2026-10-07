# Muso — Phase 4 stable state: list keys

Snapshot: **0.5.233**. Closes the stable-state item of Phase 4.

## Keyed (16 lists, each on a value proven unique)

| file | list | key |
|---|---|---|
| HomeScreen | `keepListening` | `it.id` (`LocalItem.id`) |
| HomeScreen | `moodAndGenres` | `it.endpoint.browseId` |
| HomeScreen | `it.items` / `section.items` (YouTube rows) | `it.id` (`YTItem.id`, from InnerTube) |
| YouTubeBrowseScreen | `it.items` | `it.id` |
| MediaMetadataMenu / PlayerMenu / YouTubeSongMenu | `artists` | `it.id` (`Artist : LocalItem`) |
| AddToPlaylistDialog | `playlists` | `it.id` (`PlaylistEntity.id`) |
| ModalBottomSheet | `listAction` | `it.name` (`QueueItemAction`) |
| ModalBottomSheet | `listYouTubePlaylist` | `it.browseId` (`PlaylistsResult.browseId`) |
| ModalBottomSheet | `listLocalPlaylist` | `it.id` (`LocalPlaylistEntity.id`) |
| ArchiveTune PlayerMenu | `splitArtists.distinctBy { it.name }` | `it.name` (unique by construction) |
| ArchiveTune YouTubeAlbumMenu / YouTubePlaylistMenu | `notAddedList` | `it.videoId` (`Song.videoId`) |
| ArchiveTune ChangelogScreen | `releases` | `it.tagName` (`ReleaseInfo.tagName`) |

## Deliberately NOT keyed (11 remaining, with reasons)

- **`MotionTokens.kt:125`** — not a list; the text `items(` appears inside a comment.
- **Fixed-count placeholder loops** — `items(4)`, `items(8)`, `items(10)` in `Shimmer`,
  `AccountScreen`, `NewReleaseScreen`, `HomeScreen`. There is no identity; a positional
  key would be a no-op.
- **`Preference.kt` — `ListPreference<T>`** — the list is `List<T>` for an unbounded
  generic `T`. A Compose key must be a saveable type, and `T` is not guaranteed to be
  one, so keying it could throw at runtime.
- **`ModalBottomSheet` — `artists`** — the element type here is
  `com.maxrave…searchResult.songs.Artist`, whose `id` is **nullable**; several nulls
  would collide.
- **`AddToPlaylistDialogOnline` — `playlists`** — a `Playlist` wrapper whose unique field
  is `browseId ?: id`; the wrapper's own `id` could not be confirmed from the source.
- **`AddToPlaylistDialogOnline` — `summary.failedItems`** — `List<String>` of titles;
  duplicate titles would collide.

Guessing a property name in these would be a compile error at best, and a non-unique key
a runtime `Key was already used` crash at worst, so they are left rather than guessed.
