# Muso — Phase 9: Settings

PRD §63, Phase 9. Snapshot: **0.5.230**. The PRD asks for duplicate-settings removal,
a canonical preference source, and lazy rendering.

---

## 1. Duplicate settings — found and fixed

Two preference-key files feed the **same** DataStore (`"settings"`): Muso's
(155 keys) and ArchiveTune's (348). **76 key names were declared in both.** Duplicates
of the *same* type are harmless — `Preferences.Key` equality is by name, so two
constants for one name are interchangeable. But three pairs disagreed on **type**, and
that is a crash:

| key name | Muso | ArchiveTune | consequence |
|---|---|---|---|
| `customThemeColor` | `intPreferencesKey` | `stringPreferencesKey` | crash |
| `crossfadeDuration` | `intPreferencesKey` | `floatPreferencesKey` | crash |
| `lyricsTextSize` | `intPreferencesKey` | `floatPreferencesKey` | crash |

`Preferences.get(key)` is `preferencesMap[key] as T?`, and the map is keyed by name
alone — so reading a value written by the other layer casts it to the wrong type and
throws `ClassCastException`.

**`customThemeColor` is the live one.** Muso writes and reads it as an `Int`
(`MainActivity`, the settings slider). ArchiveTune's palette picker and theme creator —
both reachable from Muso's own settings through `KitSettingsHost` — write and read it as
a `String`. Selecting a palette and then reopening the app crashed it.

**Fix:** the ArchiveTune declarations for these three keys now use ArchiveTune-scoped
key names (`archivetuneCustomThemeColor`, `archivetuneCrossfadeDuration`,
`archivetuneLyricsTextSize`), so no two layers share a key with incompatible types.
Only the key *name string* changed — every call site references the Kotlin constant, so
no other file needed editing. ArchiveTune-layer values stored under the old names are
orphaned once and fall back to their defaults.

**Also fixed:** ArchiveTune's `MixSortDescendingKey` was declared with the wrong name
string — `"albumSortDescending"` — so the Mix sort toggle and the Album sort toggle were
literally the same stored value. It is now `"mixSortDescending"`. No live use of the
ArchiveTune constant was found, so this was latent rather than user-visible.

## 2. Canonical source and crash-hardening — FIXED

Renaming alone would not have rescued a user who already had a `String` stored under
`customThemeColor`: Muso's `Int` read would still throw. The typed getters in
`DataStore.kt` now catch `ClassCastException` and degrade to "absent" (the caller's
default). The two flow mappings inside `rememberPreference` / `rememberEnumPreference`
are guarded the same way, since a mismatch there would throw inside the flow instead.
A preference read can no longer crash the app because of a stored type mismatch.

This is the "canonical source" work in the only form that is safe without a build: one
type per key, and a read path that cannot be broken by a stale value.

## 3. Lazy rendering — NOT done

`MusoSettingsSections` is a 1,725-line file whose ~166 preference reads all sit in a
single **non-lazy** `Column` with `verticalScroll`, so every settings row composes
eagerly when the screen opens. Converting it to a `LazyColumn` means restructuring the
section builders; left as the remaining Phase 9 item.

## Verified after the change

A full scan of every `*PreferencesKey(` declaration in the app now finds exactly **one**
key name carrying more than one type: `historyDuration` in ArchiveTune — and that one is
a deliberate legacy migration (the float key is read, converted to the int key, then
removed, in ArchiveTune's `DataStore.kt`). No accidental collisions remain.

## Verification debt

Nothing here is runtime-verified — the sandbox has no Android SDK. Static checks only:
all 1,074 `.kt` files parsed with tree-sitter (no new errors; the 4 known false
positives unchanged) and both edited files parse clean and are brace/paren/bracket
balanced. The duplicate/type analysis is textual, so it is exact for declarations; it
does not prove which of the colliding keys had been written most recently on a real
device. Per PRD §62 none of this may be marked "fixed" until a build and a device run
confirm it.
