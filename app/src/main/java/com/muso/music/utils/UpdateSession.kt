package com.muso.music.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-scoped, in-memory record of whether the user tapped "Later" on the
 * in-app update popup during THIS run of the app.
 *
 * Deliberately NOT persisted to DataStore. The required behaviour is that the
 * update popup keeps re-appearing on every fresh app launch until the app is
 * actually updated to the latest GitHub release. The old implementation stored
 * the dismissed version in DataStore ([com.muso.music.constants.UpdateDismissedVersionKey]),
 * which suppressed the popup for that version forever — the opposite of what is
 * wanted.
 *
 * Because this lives only in memory, it is naturally `false` again the moment
 * the process restarts (i.e. the next time the user opens the app), while still
 * letting "Later" hide the popup for the remainder of the current session so it
 * does not re-nag on every configuration change / screen rotation.
 *
 * [updateDismissed] is a Compose snapshot state, so flipping it from the popup's
 * `onDismiss` callback immediately recomposes the caller and removes the popup.
 */
object UpdateSession {
    var updateDismissed: Boolean by mutableStateOf(false)
}
