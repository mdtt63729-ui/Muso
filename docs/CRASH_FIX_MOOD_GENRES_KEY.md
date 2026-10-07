# Crash fix — duplicate Lazy key on the Home mood/genre row

Snapshot: **0.5.236**.

## The crash

A device run of 0.5.235 produced:

```
FATAL EXCEPTION: main
java.lang.IllegalArgumentException: Key "FEmusic_moods_and_genres_category" was already used.
  If you are using LazyColumn/Row please make sure you provide a unique key for each item.
```

The app died on Home, on startup.

## Cause

In 0.5.232 I keyed the Home `moodAndGenres` row with
`key = { it.endpoint.browseId }`. `FEmusic_moods_and_genres_category` is a **category**
id — every entry in that row carries the same one — so the key was not unique and Compose
threw. This is exactly the failure mode `docs/PHASE4_STABLE_STATE_KEYS.md` warned about
(a non-unique key is a runtime crash), and it is why that document's own rule should have
been applied: **only a database primary key is guaranteed unique.**

## Fix

Every key added in 0.5.232/0.5.233 that was **not** backed by a database primary key is
reverted, restoring those lists to their previous (working) identity:

| file | list | was | now |
|---|---|---|---|
| HomeScreen | `moodAndGenres` | `it.endpoint.browseId` | no key (crashed) |
| HomeScreen | `it.items`, `section.items` | `it.id` (YTItem) | no key |
| YouTubeBrowseScreen | `it.items` | `it.id` (YTItem) | no key |
| MediaMetadataMenu / PlayerMenu / YouTubeSongMenu | `artists` | `it.id ?: it.name` | no key |
| ModalBottomSheet | `listYouTubePlaylist` | `it.browseId` | no key |
| ChangelogScreen | `releases` | `it.tagName` | no key |

Kept, because each is a database primary key or unique by construction:

- `HomeScreen` `keepListening` — `LocalItem.id`
- `AddToPlaylistDialog` `playlists` — `PlaylistEntity.id`
- `ModalBottomSheet` `listLocalPlaylist` — `LocalPlaylistEntity.id`
- `ModalBottomSheet` `listAction` — `QueueItemAction.name` (three distinct enum constants in one literal list)
- ArchiveTune `PlayerMenu` — `it.name` on a list already `distinctBy { it.name }`
- ArchiveTune `YouTubeAlbumMenu` / `YouTubePlaylistMenu` `notAddedList` — `Song.id`

## Verified

All 1,073 `.kt` files parse clean with tree-sitter (no new errors; the 4 known false
positives unchanged) and the edited files are brace/paren balanced. Not runtime-verified —
no Android SDK here — but this restores the exact identity behaviour the app had before
0.5.232 on those lists.
