# Build Fix Notes — 2026-10-02

GitHub Actions build reached `:app:compileFossReleaseKotlin` and failed only because both DataStore.kt files imported a non-existent `androidx.datastore.core.data` symbol.

Fix:
- Removed the invalid `import androidx.datastore.core.data` from `app/src/main/java/com/muso/music/utils/DataStore.kt`.
- Removed the invalid `import androidx.datastore.core.data` from `app/src/main/kotlin/moe/rukamori/archivetune/utils/DataStore.kt`.

`DataStore.data` is accessed as the DataStore interface property, so no import for a separate `data` symbol is required.

No other source changes were made in this patch.

Build verification: GitHub Actions must run `./gradlew assembleFossRelease --stacktrace`. Local environment does not have the Gradle 9.5.1 distribution/network required for a full build.
