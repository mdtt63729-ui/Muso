# Muso — Phase 4 completion + Phase 9 (lazy rendering analysis)

Snapshot: **0.5.232**.

---

## 1. Phase 4 — lifecycle-aware collection now complete (236 / 236)

The last two `collectAsState` sites are migrated:

- `HistoryScreen` — `viewModel.events`
- `LibrarySongsScreen` — `viewModel.allSongs`

**Correction to my own Phase 4 note.** I previously reported these two as *plain Flows*
with non-null element types that would need an explicit type argument. That was wrong.
Both end in `.stateIn(viewModelScope, SharingStarted.Lazily, …)` — they are `StateFlow`s.
My classifier missed it because the `stateIn(` call sat beyond the window it scanned. So
both are plain one-line migrations to `collectAsStateWithLifecycle()`, with the same
return type as before, and **there are now zero `collectAsState` call sites left in the
app.**

## 2. Phase 4 — stable state: six keyless lists now keyed

`items()` calls that lacked a `key` were triaged. Six were keyed, each on a value proven
unique:

| file | list | key |
|---|---|---|
| `HomeScreen` | `keepListening` | `it.id` (`LocalItem.id`, a DB id) |
| `HomeScreen` | `moodAndGenres` | `it.endpoint.browseId` |
| `MediaMetadataMenu` | `artists` | `it.id` (`Artist : LocalItem`) |
| `PlayerMenu` | `artists` | `it.id` |
| `YouTubeSongMenu` | `artists` | `it.id` |
| `AddToPlaylistDialog` | `playlists` | `it.id` (`PlaylistEntity.id`) |

A key was deliberately **not** added elsewhere:

- **Fixed-count placeholder loops** — `items(4)`, `items(8)`, `items(10)` in the shimmer
  components and the account/new-release screens. There is no identity to key on; a
  positional key would be a no-op.
- **Lists whose element type I could not resolve** — the suite's `it.items` /
  `section.items` YouTube rows (the `ytGridItem` parameter type is not declared in that
  file), the `ModalBottomSheet` action/playlist/artist lists, and the ArchiveTune menu
  lists. Guessing a property name here risks a compile error, and guessing a
  *non-unique* one risks a runtime `Key was already used` crash, so they are left for a
  pass that can resolve each element type.

## 3. Phase 9 — lazy rendering: analysed, and deliberately NOT done

The settings rows are **already collected into a list** — `PreferenceGroupScope` builds
`items: List<@Composable () -> Unit>` — but `PreferenceGroup`
(`moe.rukamori.archivetune.ui.component.Preference.kt:1297`) renders that list inside a
`Column { forEachIndexed { … } }`. A plain `Column` is not lazy, and **18 settings
screens** host their `PreferenceGroup`s inside a non-lazy
`Column(Modifier.verticalScroll(rememberScrollState()))`, so every row of every group
composes as soon as the screen opens.

Making it lazy means two coordinated changes:

1. turn `PreferenceGroup` into a `LazyListScope` extension so its rows become real lazy
   items (keeping the First/Middle/Last position logic it computes today), and
2. convert the 18 host screens from `Column(verticalScroll)` to `LazyColumn`.

That is a cross-cutting refactor of a DSL shared by 18 screens. Getting it wrong breaks
every settings screen at once, and there is no compiler here to catch it — so it is left
undone rather than done blind. This is the last open item of Phase 9.

## 4. Phase 11 — `LoginScreen`'s `GlobalScope`: deliberately left

`LoginScreen` launches a one-shot `YouTube.accountInfo()` fetch from a `WebViewClient`
callback via `GlobalScope`. Replacing it with the composition's `rememberCoroutineScope()`
would be a one-line change, but it would **cancel the fetch if the user navigates away
mid-request** — losing the account name / email / handle the login just produced, because
those are written straight into preferences. The work is bounded (one request plus three
preference writes), so the global scope is the correct trade here. Documented, not
changed.

---

## Verification debt

Nothing here is runtime-verified — the sandbox has no Android SDK. Static checks only:
1,073 `.kt` files parsed with tree-sitter (no new errors; the 4 known false positives
unchanged), and all seven edited files parse clean. Per PRD §62 none of this may be marked
"fixed" until a build and a device run confirm it. The stable-state keys in §2 are the
part most worth exercising on a device: open Home, a song menu and the add-to-playlist
dialog and confirm nothing crashes with a duplicate-key error.
