package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.SpotifyAccessTokenKey
import com.muso.music.constants.SpotifyClientIDKey
import com.muso.music.constants.SpotifyDisplayNameKey
import com.muso.music.ui.component.DefaultDialog
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.utils.backToMain

import com.muso.music.utils.SpotifyClient
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.launch


/**
 * SimpMusic's Spotify category: log in with the user's own Spotify Client ID, see the
 * account state and browse the playlists that were found. Playlist import into the
 * Muso library arrives in the next update - this screen never fakes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (spotifyClientId, onSpotifyClientIdChange) = rememberPreference(key = SpotifyClientIDKey, defaultValue = "")
    val spotifyAccessToken by rememberPreference(key = SpotifyAccessTokenKey, defaultValue = "")
    val spotifyDisplayName by rememberPreference(key = SpotifyDisplayNameKey, defaultValue = "")
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val loggedIn = spotifyAccessToken.isNotEmpty()
    val displayName = spotifyDisplayName.ifEmpty { stringResource(R.string.spotify) }

    var showClientIdDialog by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }

    // Playlists shown after login: null = still loading, non-empty = list,
    // empty = error with a Retry button. Never a stuck spinner.
    var playlists by remember { mutableStateOf<List<SpotifyClient.SpotifyPlaylist>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(loggedIn, loadAttempt) {
        playlists = null
        loadFailed = false
        if (loggedIn) {
            val result = SpotifyClient.playlists(context)
            if (result == null) {
                loadFailed = true
            } else {
                playlists = result
            }
        }
    }

    if (showClientIdDialog) {
        var value by rememberSaveable { mutableStateOf(spotifyClientId) }
        DefaultDialog(
            onDismiss = { showClientIdDialog = false },
            content = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.spotify_client_id)) },
                    supportingText = { Text(stringResource(R.string.spotify_client_id_desc)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showClientIdDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSpotifyClientIdChange(value.trim())
                        showClientIdDialog = false
                    },
                ) {
                    Text(stringResource(R.string.set))
                }
            },
        )
    }

    if (showLogOutDialog) {
        DefaultDialog(
            onDismiss = { showLogOutDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.log_out_from_spotify) + "?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showLogOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showLogOutDialog = false
                        scope.launch {
                            SpotifyClient.logOut(context)
                        }
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.spotify),
        )

        if (!loggedIn) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.spotify_client_id)) },
                description = spotifyClientId.ifEmpty { null },
                icon = { Icon(painterResource(R.drawable.security), null) },
                onClick = { showClientIdDialog = true },
            )

            PreferenceEntry(
                title = { Text(stringResource(R.string.log_in_to_spotify)) },
                description = if (spotifyClientId.isEmpty()) stringResource(R.string.spotify_client_id_desc) else null,
                icon = { Icon(painterResource(R.drawable.spotify), null) },
                isEnabled = spotifyClientId.isNotEmpty(),
                onClick = { navController.navigate("spotify_login") },
            )
        } else {
            PreferenceEntry(
                title = { Text(stringResource(R.string.log_out_from_spotify)) },
                description = stringResource(R.string.spotify_logged_in, displayName),
                icon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = { showLogOutDialog = true },
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.spotify_playlists),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(4.dp))

            when {
                playlists != null -> {
                    playlists.orEmpty().forEach { playlist ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.padding(start = 14.dp)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${playlist.trackCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (playlists.orEmpty().isEmpty() && !loadFailed) {
                        Text(
                            text = stringResource(R.string.spotify_load_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.spotify_sync_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }

                loadFailed -> {
                    Text(
                        text = stringResource(R.string.spotify_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    TextButton(
                        onClick = { loadAttempt++ },
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }

                else -> {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
                            Text(stringResource(R.string.spotify))

        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
