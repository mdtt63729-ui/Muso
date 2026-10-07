# Muso — Phase 9 lazy rendering: pilot on PlayerSettings

Snapshot: **0.5.234**. This is the pilot for the last open Phase 9 item, not the full
rollout — see "Why a pilot" below.

## What was wrong

`PreferenceGroup` (`moe.rukamori.archivetune.ui.component.Preference.kt`) collects its
rows into a list — `PreferenceGroupScope.items: List<@Composable () -> Unit>` — but then
renders that list inside a plain `Column { forEachIndexed { … } }`. A plain `Column` is
not lazy, and each host screen wraps its groups in
`Column(Modifier.verticalScroll(rememberScrollState()))`. So opening a settings screen
composes **every row of every group** immediately.

## What the pilot does

`PlayerSettings.kt` (the screen hosting the Muso player, audio-quality, audio-behaviour,
audio-effects, queue and misc rows) now uses a `LazyColumn`:

- the scrolling `Column` became a `LazyColumn`;
- `Modifier.padding(top = topPadding)` and `.padding(bottom = ScreenBottomPadding)`, which
  used to pad the Column's contents, became the LazyColumn's `contentPadding` — the
  behaviour-preserving form (a `padding` modifier would shrink the viewport and clip the
  last row);
- each of the four direct `PreferenceGroup(…) { … }` children is wrapped in `item { … }`,
  so a group now composes only when it scrolls into view.

This is **group-level** laziness: a group's rows still compose together once the group is
on screen, but groups below the fold no longer compose at all.

## Why a pilot, not the full rollout

Row-level laziness would be better, but it is a much larger change than it looks:

- `PreferenceGroup` has **59 call sites**, and to make its rows individual lazy items the
  composable has to become a `LazyListScope` extension — which changes what every one of
  those 59 call sites resolves to, and would collide with the existing composable if both
  stayed in scope;
- **18 settings screens** use the non-lazy `Column + verticalScroll` host, and each one's
  direct children have to be wrapped in `item { … }` individually (their children are not
  uniform — some screens mix `PreferenceGroup`s with plain composables).

That is a ~77-site refactor of a DSL shared by every settings screen. Getting it wrong
breaks all of them at once, and there is no compiler in this environment to catch it. So
the pattern is proven on one screen first: **build 0.5.234, open Player settings, scroll
it, and confirm it looks and behaves as before.** If it does, the same mechanical
transformation rolls out to the other 17 screens next round.

## Verified

`PlayerSettings.kt` parses clean (tree-sitter) and is brace/paren balanced; the whole tree
still parses with no new errors (the 4 known false positives unchanged). The two now-unused
`verticalScroll` / `rememberScrollState` imports were removed.

## Verification debt

Not runtime-verified — no Android SDK here. Per PRD §62 this may not be marked "fixed"
until a build and a device run confirm it.
