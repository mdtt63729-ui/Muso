# Fix — app-wide lag from a preference getter falling back to disk

Snapshot: **0.5.237**. This is the cause of the "marattok lag" report (lag with or
without a song playing).

## The bug

`DataStore.kt`, the two synchronous preference getters:

```kotlin
return try {
    snapshot?.let { it[key] } ?: runBlocking(Dispatchers.IO) { data.first()[key] }
} catch (e: ClassCastException) { … }
```

The `?:` binds to the **value**, not to the snapshot. So:

- when the key is **absent** from the mirror — which is true for every preference the user
  has never explicitly set, i.e. **most reads in the app** — `it[key]` is null and the
  expression falls through to `runBlocking(Dispatchers.IO) { data.first()[key] }`, a
  blocking DataStore read on the calling thread;
- the same happens for a key whose stored value is legitimately null.

The mirror (`PreferencesSnapshot`, added in 0.5.225 precisely to remove this I/O) was
therefore bypassed on almost every read, and `preference()` / `enumPreference()` property
delegates are read constantly — in services, view models and composables. That is
app-wide main-thread disk I/O, which is exactly the lag reported.

## When it was introduced

0.5.225 wrote it correctly:

```kotlin
val snapshot = PreferencesSnapshot.current(this)
    ?: return runBlocking(Dispatchers.IO) { data.first()[key] }   // fallback on the SNAPSHOT
return snapshot[key]
```

The **0.5.230 crash-guard rewrite** replaced that with the elvis-on-the-value form above,
which reintroduced the I/O. So 0.5.225–0.5.229 were fine; 0.5.230 onward were not.

## Fix

The fallback is moved back onto the snapshot, with the crash guard kept:

```kotlin
if (snapshot != null) {
    snapshot[key]                      // memory only — an absent key is just null
} else {
    runBlocking(Dispatchers.IO) { data.first()[key] }   // only before the mirror is primed
}
```

Both getters (the nullable one and the one with a default) are fixed. The two flow
mappings inside `rememberPreference` / `rememberEnumPreference` were checked and are
correct — they read the in-memory `prefs` and never touch disk.

## Verified

`DataStore.kt` parses clean and is brace/paren balanced; the whole tree parses with no new
errors (the 4 known false positives unchanged). Not runtime-verified — no Android SDK here
— but this restores the exact 0.5.225 behaviour for reads of unset keys.
