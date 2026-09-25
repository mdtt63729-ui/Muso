package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.muso.music.ui.component.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.BuildConfig
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.ui.component.Material3SettingsGroup
import com.muso.music.ui.component.Material3SettingsItem
import com.muso.music.ui.utils.backToMain

/**
 * Echo Music settings home (ported): a big title, a search field that filters the
 * categories live, and the categories as connected rounded card groups with icons and
 * descriptions. Every entry opens the existing, fully working settings page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val updateAvailable = latestVersionName != BuildConfig.VERSION_NAME

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchLower = searchQuery.lowercase()

    data class SettingsPage(
        val title: String,
        val description: String,
        val iconRes: Int,
        val route: String?,
        val url: String? = null,
        val showBadge: Boolean = false,
    )

    val appearanceDesc = stringResource(R.string.settings_desc_appearance)
    val contentDesc = stringResource(R.string.settings_desc_content)
    val playerDesc = stringResource(R.string.settings_desc_player)
    val effectsDesc = stringResource(R.string.settings_desc_audio_effects)
    val storageDesc = stringResource(R.string.settings_desc_storage)
    val backupDesc = stringResource(R.string.settings_desc_backup)
    val discordDesc = stringResource(R.string.settings_desc_discord)
    val privacyDesc = stringResource(R.string.settings_desc_privacy)
    val aboutDesc = stringResource(R.string.settings_desc_about)

    val allPages = listOf(
        SettingsPage(
            title = stringResource(R.string.appearance),
            description = appearanceDesc,
            iconRes = R.drawable.palette,
            route = "settings/appearance",
        ),
        SettingsPage(
            title = stringResource(R.string.content),
            description = contentDesc,
            iconRes = R.drawable.language,
            route = "settings/content",
        ),
        SettingsPage(
            title = stringResource(R.string.player_and_audio),
            description = playerDesc,
            iconRes = R.drawable.play,
            route = "settings/player",
        ),
        SettingsPage(
            title = stringResource(R.string.audio_effects),
            description = effectsDesc,
            iconRes = R.drawable.equalizer,
            route = "settings/audio_effects",
        ),
        SettingsPage(
            title = stringResource(R.string.storage),
            description = storageDesc,
            iconRes = R.drawable.storage,
            route = "settings/storage",
        ),
        SettingsPage(
            title = stringResource(R.string.backup_restore),
            description = backupDesc,
            iconRes = R.drawable.restore,
            route = "settings/backup_restore",
        ),
        SettingsPage(
            title = stringResource(R.string.discord_integration),
            description = discordDesc,
            iconRes = R.drawable.discord,
            route = "settings/discord",
        ),
        SettingsPage(
            title = stringResource(R.string.privacy),
            description = privacyDesc,
            iconRes = R.drawable.security,
            route = "settings/privacy",
        ),
        SettingsPage(
            title = stringResource(R.string.about),
            description = aboutDesc,
            iconRes = R.drawable.info,
            route = "settings/about",
        ),
    )

    val items = allPages
        .filter { it.title.lowercase().contains(searchLower) || it.description.lowercase().contains(searchLower) }
        .map { page ->
            Material3SettingsItem(
                icon = painterResource(page.iconRes),
                title = page.title,
                description = page.description,
                onClick = { navController.navigate(page.route!!) },
            )
        }
        .toMutableList()

    // The update entry floats at the top when a newer release exists (and matches the search).
    val updateTitle = stringResource(R.string.system_update)
    val updateDesc = stringResource(R.string.settings_desc_update)
    if (updateAvailable &&
        (updateTitle.lowercase().contains(searchLower) || updateDesc.lowercase().contains(searchLower))
    ) {
        items.add(
            0,
            Material3SettingsItem(
                icon = painterResource(R.drawable.update),
                title = updateTitle,
                description = if (searchLower.isNotEmpty()) updateDesc else latestVersionName,
                showBadge = true,
                onClick = { uriHandler.openUri("https://github.com/mdtt63729-ui/Muso/releases/latest") },
            )
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
        )

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, onLongClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
            )
        }

        Material3SettingsGroup(items = items)

        Spacer(Modifier.padding(bottom = 24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
