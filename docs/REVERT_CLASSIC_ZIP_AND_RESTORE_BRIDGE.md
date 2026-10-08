# The Classic-NowPlaying zip replace is reverted; the build is green again

Snapshot: **0.5.250**.

## Why the replace could not work

The CI run on 0.5.248 failed with **789 errors** across 26 files. Their shape is the whole story:

| unresolved / error | count |
|---|---|
| `Res` | 241 |
| `simpmusic` | 216 |
| `stringResource` | 180 |
| `resources` | 26 |
| `AudioOutputKind` | 16 |
| `expect`/`actual` — "can be used only in multiplatform projects" | 4 |

That is the **Compose Multiplatform Resources** API (`Res.string.*`,
`org.jetbrains.compose.resources.stringResource`, the generated `Res` class) plus four
`expect`/`actual` declarations. The supplied zip is a **`commonMain` source set of a KMP
project**; this app is a plain Android module that uses `R.string` and has no multiplatform
plugin. Dropping the zip in cannot work without porting its entire resource layer — hundreds of
`Res.*` call sites — and giving the module a multiplatform setup. That is a rewrite, not a fix.

So the tree is back to **0.5.247**, the last version before the replace.

## Two real bugs that the revert exposed, both fixed

The 0.5.248 log also carried errors that had nothing to do with the zip:

### 1. `MusoSuiteBridge` was deleted with its file

0.5.247 removed `MusoSuiteHost.kt` as dead code. The **host composable** was dead, but
`MusoSuiteBridge` — defined in the same file — was not: `MusoNavbarHost` calls it to feed the
suite's shared state, and the glass bottom bar's MiniPlayer reads that state. Deleting the file
took the bridge with it, and `MusoNavbarHost.kt:126` then failed to resolve it.

The bridge (and its three private helpers) is now its own file, `MusoSuiteBridge.kt`, extracted
verbatim from 0.5.246 with one change: the seven removed player styles are dropped from its
style mapping, leaving the three that exist.

### 2. `DarkAppColors` / `LightAppColors` private

Those were only private in the *zip's* `Theme.kt`; the restored `Theme.kt` declares both public,
as `MainActivity` expects.

## What is in this build

- **0.5.247** — three player styles (Classic V2 / M3 Expressive / Immersive Nightly), the style
  picker wired into the live player, seven styles and their dead code removed.
- **The 0.5.249 PRD fix** — the foreground-restore fix is retained: the `MainActivity`
  snap-to-dismissed effect is gone and `BottomSheet.kt`'s two boot guards are gated on
  `wasRestored`, so a fullscreen player comes back fullscreen.
- **`MusoSuiteBridge.kt`** — the bridge restored.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives). Every error pattern
from the 0.5.248 log greps to zero in this tree: no Compose-Resources `Res`, no
`expect`/`actual`, no private `DarkAppColors`/`LightAppColors`, and `MusoSuiteBridge` is defined
again. Not runtime-verified.
