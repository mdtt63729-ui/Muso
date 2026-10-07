package com.muso.music.utils

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.muso.music.extensions.toEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * In-memory mirror of the settings DataStore.
 *
 * The synchronous accessors below used to run `runBlocking(Dispatchers.IO)` on
 * every read, which blocked the calling thread - usually the main thread - until
 * the DataStore finished its file I/O. The mirror is primed once at application
 * start ([primePreferences]) and then kept live by a single collector, so a read
 * is a memory lookup instead of an I/O wait.
 */
private object PreferencesSnapshot {
    private val snapshots =
        java.util.concurrent.ConcurrentHashMap<DataStore<Preferences>, kotlinx.coroutines.flow.MutableStateFlow<Preferences?>>()
    private val started = java.util.concurrent.ConcurrentHashMap.newKeySet<DataStore<Preferences>>()
    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun current(dataStore: DataStore<Preferences>): Preferences? = snapshots[dataStore]?.value

    /**
     * Starts the one live collector for [dataStore] and loads the first snapshot
     * synchronously. Called from Application.onCreate, before any activity or
     * service can read a preference, so the blocking read happens exactly once -
     * at startup - instead of on every accessor call.
     */
    fun prime(dataStore: DataStore<Preferences>) {
        val flow = snapshots.getOrPut(dataStore) { kotlinx.coroutines.flow.MutableStateFlow(null) }
        if (!started.add(dataStore)) return
        scope.launch {
            dataStore.data.collect { flow.value = it }
        }
        runCatching { flow.value = runBlocking(Dispatchers.IO) { dataStore.data.first() } }
    }
}

/** Primes the settings mirror. Call once from Application.onCreate. */
fun primePreferences(context: Context) {
    PreferencesSnapshot.prime(context.dataStore)
}

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    val snapshot = PreferencesSnapshot.current(this)
    return try {
        // The fallback must key off the SNAPSHOT, not off the value. Writing this as
        // `snapshot?.let { it[key] } ?: runBlocking { ... }` made an ABSENT key - and a
        // legitimately null one - fall through to a blocking DataStore read, which is
        // most preference reads in the app. That reintroduced exactly the main-thread
        // I/O this mirror exists to remove.
        if (snapshot != null) {
            snapshot[key]
        } else {
            runBlocking(Dispatchers.IO) { data.first()[key] }
        }
    } catch (e: ClassCastException) {
        // A value stored under this name with a different type - two layers declaring
        // the same key name as different types, see docs/PHASE9_SETTINGS.md - makes the
        // typed read throw. A preference read must never crash the app.
        null
    }
}

fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, defaultValue: T): T {
    val snapshot = PreferencesSnapshot.current(this)
    return try {
        if (snapshot != null) {
            snapshot[key] ?: defaultValue
        } else {
            runBlocking(Dispatchers.IO) { data.first()[key] ?: defaultValue }
        }
    } catch (e: ClassCastException) {
        defaultValue
    }
}

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember {
        context.dataStore.data
            .map { prefs -> runCatching { prefs[key] }.getOrNull() ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(context.dataStore[key] ?: defaultValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialValue = context.dataStore[key].toEnum(defaultValue = defaultValue)
    val state = remember {
        context.dataStore.data
            .map { prefs -> runCatching { prefs[key] }.getOrNull().toEnum(defaultValue = defaultValue) }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value.name
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
