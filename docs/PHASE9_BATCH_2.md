# Phase 9 — batch 2

Snapshot: **0.5.252**.

## Converted this round

| screen | shape | note |
|---|---|---|
| `AboutScreen.kt` | flat | centred column — the alignment moved to `LazyColumn(horizontalAlignment = …)`; each child became an `item {}` |
| `AISettings.kt` | flat | the one `if (aiProvider == CUSTOM_OPENAI) { … }` around a row became an `item {}` |

Both also had their now-unused `rememberScrollState` / `verticalScroll` imports removed and the
`LazyColumn` import added.

That makes **four** settings screens done in total, with the two from the previous round
(`ListeningHistorySettings.kt`, `BackupAndRestore.kt`).

## Why the batch is small

The next screens are not flat, and the difference matters:

| screen | what blocks a mechanical wrap |
|---|---|
| `SpotifySettings.kt` | `playlists.orEmpty().forEach { … }` — one row per playlist, rendered eagerly |
| `AudioEffectsScreen.kt` | `eqInfo.centerFreqs.forEachIndexed { … }` — one row per equalizer band |
| `DiscordSettings.kt`, `MusoSettingsSections.kt` | long, mixed, with nested conditionals |

A `forEach` inside a `Column` composes every row up front; inside a `LazyColumn` it must become
`items(...)`, which means lifting the loop out of the surrounding `if`/`when` into the list's own
scope. That is a restructure per screen, not a wrap — and each one can only be checked by a
build. They are worth doing, and doing one at a time, rather than as a single edit that takes the
whole settings area down with it.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). Both converted
screens parse clean and are brace/paren balanced, carry the `LazyColumn` import, and have no
`rememberScrollState`/`verticalScroll` reference left beyond a comment. Not runtime-verified.

Note: the bodies inside the new `item {}` blocks keep their original 8-space indent rather than
being re-indented to 12. That is cosmetic only — re-indenting by hand is exactly the kind of edit
that silently changes meaning.
