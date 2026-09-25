package com.muso.music.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.AutoBackupKey
import com.muso.music.constants.AutoBackupFrequency
import com.muso.music.constants.AutoBackupFrequencyKey
import com.muso.music.ui.component.EnumListPreference
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.utils.backToMain
import com.muso.music.viewmodels.BackupRestoreViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            viewModel.backup(context, uri)
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.restore(context, uri)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Text(
            text = stringResource(R.string.backup_restore),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.action_backup)) },
            icon = { Icon(painterResource(R.drawable.backup), null) },
            onClick = {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                backupLauncher.launch("${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
            }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.action_restore)) },
            icon = { Icon(painterResource(R.drawable.restore), null) },
            onClick = {
                restoreLauncher.launch(arrayOf("application/octet-stream"))
            }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.auto_backup)
        )

        val (autoBackup, onAutoBackupChange) = rememberPreference(key = AutoBackupKey, defaultValue = false)
        val (backupFrequency, onBackupFrequencyChange) = rememberEnumPreference(key = AutoBackupFrequencyKey, defaultValue = AutoBackupFrequency.DAILY)

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_backup)) },
            description = stringResource(R.string.auto_backup_desc),
            icon = { Icon(painterResource(R.drawable.update), null) },
            checked = autoBackup,
            onCheckedChange = onAutoBackupChange
        )

        if (autoBackup) {
            EnumListPreference(
                title = { Text(stringResource(R.string.backup_frequency)) },
                icon = { Icon(painterResource(R.drawable.update), null) },
                selectedValue = backupFrequency,
                onValueSelected = onBackupFrequencyChange,
                valueText = {
                    when (it) {
                        AutoBackupFrequency.DAILY -> stringResource(R.string.daily)
                        AutoBackupFrequency.WEEKLY -> stringResource(R.string.weekly)
                    }
                }
            )
        }
    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
            androidx.compose.animation.AnimatedVisibility(
                visible = scrollState.value > 100,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
            ) {
                Text(stringResource(R.string.backup_restore))
            }
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
