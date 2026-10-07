# Muso — Phase 4 stable state: list keys

Snapshot: **0.5.233**. Closes the stable-state item of Phase 4.

## Keyed (16 lists, each on a value proven unique)

| file | list | key |
|---|---|---|
| HomeScreen | `keepListening` | `it.id` (`LocalItem.id`) |
| HomeScreen | `moodAndGenres` | `it.endpoint.browseId` |
| HomeScreen | `it.items` / `section.items` (YouTube rows) | `it.id` (`YTItem.id`, from InnerTube) |
| YouTubeBrowseScreen | `it.items` | `it.id` |
| MediaMetadataMenu / PlayerMenu / YouTubeSongMenu | `artists` | `it.id ?: it.name` (`MediaMetadata.Artist.id` is nullable — see the CI correction below) |
| AddToPlaylistDialog | `playlists` | `it.id` (`PlaylistEntity.id`) |
| ModalBottomSheet | `listAction` | `it.name` (`QueueItemAction`) |
| ModalBottomSheet | `listYouTubePlaylist` | `it.browseId` (`PlaylistsResult.browseId`) |
| ModalBottomSheet | `listLocalPlaylist` | `it.id` (`LocalPlaylistEntity.id`) |
| ArchiveTune PlayerMenu | `splitArtists.distinctBy { it.name }` | `it.name` (unique by construction) |
| ArchiveTune YouTubeAlbumMenu / YouTubePlaylistMenu | `notAddedList` | `it.id` (corrected — see below) |
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


---

## CI correction (0.5.235)

The first CI run against this work failed at `:app:compileFossReleaseKotlin` with six
errors, all from the keys added above. Each was a wrong guess about a field, which is
exactly the failure mode this file warned about — the difference is that the compiler
caught it rather than a device:

| site | error | fix |
|---|---|---|
| `DownloadUtil` ×2 | `Operator '==' cannot be applied to 'Float' and 'Int'` | `C.PERCENTAGE_UNSET` is an `Int` while `percentDownloaded` is a `Float`; compared via `C.PERCENTAGE_UNSET.toFloat()` |
| `MediaMetadataMenu`, `PlayerMenu`, `YouTubeSongMenu` | `Return type mismatch: expected 'Any', actual 'String?'` | `MediaMetadata.Artist.id` is **nullable**; key is now `it.id ?: it.name` (the lists are pre-filtered to non-null ids, so `it.id` always wins) |
| `YouTubeAlbumMenu` | `Unresolved reference 'videoId' on receiver of type 'Song'` | that `Song` is `moe.rukamori.archivetune.db.entities.Song` (`: LocalItem`), which has `id`, not `videoId`; key is now `it.id` |
| `YouTubePlaylistMenu` | `Unresolved reference 'videoId' on receiver of type 'MediaMetadata'` | the list is `moe.rukamori.archivetune.models.MediaMetadata`, which has `id: String`; key is now `it.id` |

The same run also confirms the rest of the work compiles: it reached
`:app:compileFossReleaseKotlin` (so resource processing and KSP across every module
passed), and the only errors reported were these six — nothing from the Phase 5, 8, 9 or
11 changes, and nothing from the PlayerSettings `LazyColumn` pilot.
