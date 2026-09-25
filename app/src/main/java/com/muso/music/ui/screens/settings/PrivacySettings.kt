package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zionhuang.innertube.YouTube
import com.muso.music.LocalDatabase
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.DisableScreenshotKey
import com.muso.music.constants.PauseSearchHistoryKey
import com.muso.music.constants.UseLoginForBrowse
import com.muso.music.ui.component.DefaultDialog
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val database = LocalDatabase.current
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(key = PauseSearchHistoryKey, defaultValue = false)
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(key = UseLoginForBrowse, defaultValue = false)
    val (disableScreenshot, onDisableScreenshotChange) = rememberPreference(key = DisableScreenshotKey, defaultValue = false)

    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    if (showClearSearchHistoryDialog) {
        DefaultDialog(
            onDismiss = { showClearSearchHistoryDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_search_history_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showClearSearchHistoryDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearSearchHistoryDialog = false
                        database.query {
                            clearSearchHistory()
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState)
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.search_history)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.pause_search_history)) },
            icon = { Icon(painterResource(R.drawable.search_off), null) },
            checked = pauseSearchHistory,
            onCheckedChange = onPauseSearchHistoryChange
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_search_history)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            onClick = { showClearSearchHistoryDialog = true }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.account)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.use_login_for_browse)) },
            description = stringResource(R.string.use_login_for_browse_desc),
            icon = { Icon(painterResource(R.drawable.person), null) },
            checked = useLoginForBrowse,
            onCheckedChange = {
                YouTube.useLoginForBrowse = it
                onUseLoginForBrowseChange(it)
            }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.disable_screenshot)) },
            description = stringResource(R.string.disable_screenshot_desc),
            icon = { Icon(painterResource(R.drawable.screenshot), null) },
            checked = disableScreenshot,
            onCheckedChange = onDisableScreenshotChange
        )
    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
                            Text(stringResource(R.string.privacy))

        },
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
